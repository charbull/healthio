# Healthio Proguard Rules

# Keep Compose internal classes
-keepclassmembers class * extends androidx.compose.ui.node.ModifierNodeElement {
    <init>(...);
}

# Keep Room generated classes
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep GSON models (essential for FoodAnalysis JSON parsing)
-keep class com.healthio.core.ai.FoodAnalysis { *; }
-keep class com.healthio.core.database.** { *; }

# Google API Client rules
-keep class com.google.api.services.sheets.** { *; }
-keep class com.google.api.client.** { *; }
-dontwarn com.google.api.client.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.apache.http.**
-dontwarn com.google.errorprone.annotations.**

# Health Connect
-keep class androidx.health.connect.** { *; }
