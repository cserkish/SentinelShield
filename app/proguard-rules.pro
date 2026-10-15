# ============================================================
# SentinelShield — Optimized ProGuard / R8 Rules
# ============================================================

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses, RuntimeVisibleAnnotations

# Kotlin & Coroutines
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Lazy { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# Kotlin Serialization
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class uz.sentinel.**$$serializer { *; }
-keepclassmembers class uz.sentinel.** { *** Companion; }
-keepclasseswithmembers class uz.sentinel.** { kotlinx.serialization.KSerializer serializer(...); }

# Hilt / Dagger
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.Module class * { *; }
-keep @dagger.hilt.migration.DisableInstallInCheck class * { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}
-keep class **_HiltComponents* { *; }
-keep class **_MembersInjector { *; }
-keep class **_Factory { *; }
-dontwarn dagger.**
-dontwarn javax.annotation.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { abstract *; }
-keepclassmembers @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Dao interface * { *; }
-keep class **_Impl { *; }
-keep class **_Impl$* { *; }
-dontwarn androidx.room.**

# Jetpack Compose (Do NOT -keep all of androidx.compose.material.icons.** so R8 strips unused icons)
-keep @androidx.compose.runtime.Stable class * { *; }
-keep @androidx.compose.runtime.Immutable class * { *; }
-dontwarn androidx.compose.**

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keepclassmembers class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-dontwarn androidx.work.**

# CameraX & Biometric & DataStore
-dontwarn androidx.camera.**
-dontwarn androidx.biometric.**
-dontwarn androidx.datastore.**

# SentinelShield Entities & EntryPoints
-keep class uz.sentinel.data.local.entity.** { *; }
-keep class uz.sentinel.service.** { *; }
