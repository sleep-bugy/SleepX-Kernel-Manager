# ===================================================================
# == ATURAN PROGUARD WAJIB UNTUK APLIKASI XTRA KERNEL MANAGER (v3) ==
# ===================================================================

# --- Aturan Umum untuk Kotlin ---
-keep,allowobfuscation,allowshrinking class kotlin.Metadata
-keepclassmembers class kotlin.coroutines.jvm.internal.BaseContinuationImpl {
    private java.lang.Object L$0;
    private java.lang.Object L$1;
    private int label;
}

# --- Aturan Wajib untuk Kotlinx Serialization ---
# Peringatan "The rule matches no class members" akan muncul jika Anda belum punya
# class @Serializable, ini aman untuk diabaikan.
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}
-keep class * implements kotlinx.serialization.KSerializer {
    <fields>;
    <methods>;
}
-keepclassmembers class **$$serializer {
    <methods>;
    <fields>;
}

# --- Aturan Wajib untuk Hilt / Dagger ---
-keep class hilt_aggregated_deps.** {*;}
-keep class dagger.hilt.internal.aggregatedroot.codegen.** {*;}
-keep class dagger.hilt.android.internal.modules.** {*;}
-keep class dagger.hilt.android.internal.builders.** {*;}
-keep class dagger.hilt.android.internal.lifecycle.** {*;}
-keep class dagger.hilt.android.internal.managers.** {*;}
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper {*;}
-keep class * implements dagger.hilt.internal.GeneratedComponent {*;}
-keep class * implements dagger.hilt.internal.GeneratedEntryPoint {*;}
-keep class * implements dagger.hilt.internal.GeneratedComponentManager {*;}
-keep class * implements dagger.hilt.internal.GeneratedComponentManagerHolder {*;}
# -keep class * implements dagger.hilt.internal.GeneratedEarlyEntryPoint {*;}
# --- Aturan SPESIFIK untuk melindungi semua data class dari Firebase ---
# GANTI "id.xms.xtrakernelmanager.model" dengan package Anda yang benar!
# Melindungi data class UpdateInfo yang digunakan oleh Firebase
-keep class id.xms.xtrakernelmanager.model.UpdateInfo { *; }
-keep class id.xms.xtrakernelmanager.data.model.** { *; }

# --- Tambahan: Melindungi semua class dan adapter Firebase ---
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.protobuf.** { *; }
-keep class com.google.common.** { *; }
-keep class com.firebase.** { *; }
-keep class com.google.api.** { *; }
-keep class com.google.type.** { *; }
-keep class com.google.datastore.** { *; }
-keep class com.google.auth.** { *; }
-keep class com.google.gson.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keep class com.squareup.moshi.** { *; }
# --- Untuk Firebase Realtime Database Serialization ---
-keepclassmembers class * {
    @com.google.firebase.database.PropertyName <fields>;
    @com.google.firebase.database.PropertyName <methods>;
}
-keepclassmembers class * {
    @com.google.firebase.database.Exclude <fields>;
    @com.google.firebase.database.Exclude <methods>;
}

# Mengatasi error R8: "Missing class" yang disebabkan oleh library Guava
-dontwarn com.google.common.reflect.**

# --- Aturan untuk Library Jaringan (dipakai oleh Coil, dll.) ---
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }
-keep interface okio.** { *; }

# ===================================================================