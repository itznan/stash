-keepclassmembers class * extends androidx.room.RoomDatabase {
    <methods>;
}
-keep class * extends androidx.room.RoomDatabase
-keep class com.nan.stash.data.local.** { *; }
-keep class com.nan.stash.data.model.** { *; }
-keepattributes *Annotation*
-dontwarn androidx.datastore.**
-dontwarn androidx.biometric.**
-keep class androidx.biometric.** { *; }
