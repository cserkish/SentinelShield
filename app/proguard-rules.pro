# ============================================================
# SentinelShield — ProGuard Rules
# ============================================================

# --- Keep line number info for crash reporting ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================================
# Kotlin
# ============================================================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Lazy { *; }
-dontwarn kotlin.**
-dontnote kotlin.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class uz.sentinel.**$$serializer { *; }
-keepclassmembers class uz.sentinel.** {
    *** Companion;
}
-keepclasseswithmembers class uz.sentinel.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ============================================================
# Hilt / Dagger
# ============================================================
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.Module class * { *; }
-keep @dagger.hilt.migration.DisableInstallInCheck class * { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}
-keepclassmembers class * {
    @javax.inject.Inject <fields>;
}
-dontwarn dagger.**
-dontwarn javax.annotation.**

# Hilt generated components
-keep class **_HiltComponents* { *; }
-keep class **_MembersInjector { *; }
-keep class **_Factory { *; }

# ============================================================
# Room
# ============================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract *;
}
-keepclassmembers @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.Query *;
    @androidx.room.Insert *;
    @androidx.room.Delete *;
    @androidx.room.Update *;
    @androidx.room.Transaction *;
}
-dontwarn androidx.room.**

# Room — keep generated _Impl classes
-keep class **_Impl { *; }
-keep class **_Impl$* { *; }

# ============================================================
# Jetpack Compose
# ============================================================
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Compose runtime
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.animation.** { *; }

# Stable / Immutable annotations used by Compose compiler
-keepattributes RuntimeVisibleAnnotations
-keep @androidx.compose.runtime.Stable class * { *; }
-keep @androidx.compose.runtime.Immutable class * { *; }

# ============================================================
# DataStore
# ============================================================
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ============================================================
# WorkManager
# ============================================================
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keepclassmembers class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-dontwarn androidx.work.**

# ============================================================
# CameraX
# ============================================================
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ============================================================
# Biometric
# ============================================================
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# ============================================================
# SentinelShield — Application classes
# ============================================================
-keep class uz.sentinel.** { *; }
-keepclassmembers class uz.sentinel.** { *; }

# Keep all entity fields (Room needs them)
-keepclassmembers class uz.sentinel.data.local.entity.** {
    <fields>;
    <init>(...);
}

# ============================================================
# Android / General
# ============================================================
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Enum
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Suppress known benign warnings
-dontwarn sun.misc.**
-dontwarn java.lang.instrument.**
-dontwarn org.slf4j.**
