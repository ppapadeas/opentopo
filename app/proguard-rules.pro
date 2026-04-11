# Add project specific ProGuard rules here.

# MapLibre
-keep class org.maplibre.** { *; }
-keep class com.mapbox.** { *; }

# USB Serial
-keep class com.hoho.android.usbserial.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }

# Coroutines
-keepnames class kotlinx.coroutines.** { *; }
