# ==============================================================================
# Monkify Android ProGuard / R8 Rules (Release Hardening)
# ==============================================================================

# Atributos generales de reflexión, anotaciones y tipos genéricos
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, Exceptions

# Gson & Data Transfer Objects (DTOs)
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}
-keep class com.k11000.monkify.data.remote.dto.** { *; }
-keep class com.k11000.monkify.domain.model.** { *; }
-keep class com.k11000.monkify.data.local.model.** { *; }
-keep class com.k11000.monkify.data.local.entity.** { *; }

# Room SQLite Database
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase$Callback
-dontwarn androidx.room.paging.**

# Retrofit 2 & OkHttp 3
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Media3 & ExoPlayer
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.ui.** { *; }
-dontwarn androidx.media3.**

# Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**

# Rive Animation Runtime (JNI / C++ Native Bindings)
-keep class app.rive.runtime.kotlin.** { *; }
-keepclassmembers class app.rive.runtime.kotlin.** {
    native <methods>;
    <fields>;
}
-dontwarn app.rive.runtime.kotlin.**

# Hilt & Dependency Injection
-keep class * extends androidx.activity.ComponentActivity
-keep class * extends androidx.fragment.app.Fragment
-keep class * extends androidx.lifecycle.ViewModel
-dontwarn dagger.hilt.**

# Google Play Services (Location Provider)
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.location.**

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
