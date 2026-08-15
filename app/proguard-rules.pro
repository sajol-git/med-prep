# Add project specific ProGuard rules here.

# Keep all of our application classes to prevent any class/method stripping
-keep class com.example.** { *; }
-keep interface com.example.** { *; }

# Keep Room database classes and annotations
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Dao
-keep class androidx.room.Room { *; }
-dontwarn androidx.room.**
-dontwarn androidx.sqlite.db.**

# Keep Moshi / JSON serialization classes
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# Keep Firebase classes (if used in cloud sync/auth)
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep Jetpack Compose classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Kotlin reflection metadata
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-dontwarn kotlin.reflect.**

# OkHttp optional dependencies
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.bouncycastle.provider.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
