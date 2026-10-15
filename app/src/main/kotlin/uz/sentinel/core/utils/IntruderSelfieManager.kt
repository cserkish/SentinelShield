package uz.sentinel.core.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import uz.sentinel.data.local.db.SentinelDatabase
import uz.sentinel.data.local.entity.IntruderPhotoEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.security.KeyStore
import java.util.UUID
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class IntruderSelfieManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: SentinelDatabase
) {
    private val tag = "IntruderSelfieManager"
    private val keyAlias = "sentinel_intruder_key"
    private val androidKeyStore = "AndroidKeyStore"
    private val gcmTagLength = 128
    private val executor = Executors.newSingleThreadExecutor()

    suspend fun capture(packageName: String) = withContext(Dispatchers.IO) {
        try {
            val hasFrontCamera = context.packageManager
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
            if (!hasFrontCamera) return@withContext

            // Verify CAMERA runtime permission before invoking CameraX
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(tag, "CAMERA permission not granted; skipping intruder capture")
                return@withContext
            }

            ensureKeyExists()

            val imageBytes = captureImageBytes() ?: return@withContext
            if (imageBytes.isEmpty()) return@withContext
            val encrypted = encryptBytes(imageBytes)

            val intruderDir = File(context.filesDir, "intruder").apply { mkdirs() }
            val fileName = "intruder_${UUID.randomUUID()}.enc"
            val file = File(intruderDir, fileName)

            FileOutputStream(file).use { fos ->
                val ivLenBytes = ByteBuffer.allocate(4).putInt(encrypted.iv.size).array()
                fos.write(ivLenBytes)
                fos.write(encrypted.iv)
                fos.write(encrypted.ciphertext)
            }

            val appName = try {
                context.packageManager.getApplicationLabel(
                    context.packageManager.getApplicationInfo(packageName, 0)
                ).toString()
            } catch (e: Exception) {
                packageName
            }

            val entity = IntruderPhotoEntity(
                targetAppPackage = packageName,
                targetAppName = appName,
                encryptedPhotoPath = file.absolutePath,
                timestamp = System.currentTimeMillis()
            )
            database.intruderPhotoDao().insertPhoto(entity)

            Log.d(tag, "Intruder captured for $packageName -> ${file.name}")
        } catch (e: Exception) {
            Log.e(tag, "Failed to capture intruder", e)
        }
    }

    private suspend fun captureImageBytes(): ByteArray? = suspendCancellableCoroutine { cont ->
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = try {
                cameraProviderFuture.get()
            } catch (e: Exception) {
                if (cont.isActive) cont.resumeWithException(e)
                return@addListener
            }

            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()
            val lifecycleOwner = TempLifecycleOwner()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageCapture)

                imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val bytes = try {
                            imageProxyToJpegBytes(image)
                        } catch (e: Exception) {
                            null
                        } finally {
                            image.close()
                        }
                        // CRITICAL FIX: unbindAll() and lifecycleOwner.destroy() MUST run on Main Thread!
                        mainExecutor.execute {
                            runCatching { cameraProvider.unbindAll() }
                            runCatching { lifecycleOwner.destroy() }
                            if (cont.isActive) {
                                cont.resume(bytes)
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        mainExecutor.execute {
                            runCatching { cameraProvider.unbindAll() }
                            runCatching { lifecycleOwner.destroy() }
                            if (cont.isActive) {
                                cont.resumeWithException(exception)
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                runCatching { cameraProvider.unbindAll() }
                runCatching { lifecycleOwner.destroy() }
                if (cont.isActive) {
                    cont.resumeWithException(e)
                }
            }
        }, mainExecutor)
    }

    private fun imageProxyToJpegBytes(image: ImageProxy): ByteArray {
        return try {
            val bitmap = image.toBitmap()
            ByteArrayOutputStream().use { baos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                baos.toByteArray()
            }
        } catch (e: Exception) {
            val buffer: ByteBuffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            bytes
        }
    }

    private fun ensureKeyExists() {
        val keyStore = KeyStore.getInstance(androidKeyStore).apply { load(null) }
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGen = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                androidKeyStore
            )
            val spec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(androidKeyStore).apply { load(null) }
        return (keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry).secretKey
    }

    private data class EncryptedData(val iv: ByteArray, val ciphertext: ByteArray)

    private fun encryptBytes(data: ByteArray): EncryptedData {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(data)
        return EncryptedData(iv, ciphertext)
    }

    fun decryptFile(file: File): ByteArray {
        val rawBytes = file.readBytes()
        val ivLen = ByteBuffer.wrap(rawBytes.copyOfRange(0, 4)).int
        val iv = rawBytes.copyOfRange(4, 4 + ivLen)
        val ciphertext = rawBytes.copyOfRange(4 + ivLen, rawBytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(gcmTagLength, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        return cipher.doFinal(ciphertext)
    }
}
