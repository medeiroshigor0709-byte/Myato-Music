# Keep source file and line numbers for readable crash diagnostics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Required attributes for Kotlin, Kotlinx Serialization, and Room reflection/annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# App entry points declared in AndroidManifest.xml
-keep class com.example.MyatoApplication { *; }
-keep class com.example.MainActivity { *; }
-keep class com.example.core.audio.MyatoPlaybackService { *; }
-keep class com.example.widgets.** { *; }
-keep class com.example.core.common.BackupWorker { *; }

# Room entities, DAOs, database, and generated implementations
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class com.example.core.database.** { *; }
-keep @androidx.room.Dao interface com.example.core.database.** { *; }

# Kotlinx Serialization (navigation routes, domain models, theme JSON, and backup DTOs)
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <1>$* {
    <fields>;
    <methods>;
}
-keep class com.example.presentation.navigation.MyatoRoute** { *; }
-keep class com.example.domain.models.** { *; }
-keep class com.example.core.storage.MyatoBackupBundle { *; }
-keep class com.example.core.storage.Backup*Dto { *; }

# Media3 / ExoPlayer & AndroidX WorkManager stability
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-dontwarn org.jetbrains.annotations.**
-dontwarn javax.annotation.**

