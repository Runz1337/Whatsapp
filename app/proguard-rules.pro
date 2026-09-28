# Room entities are constructed reflectively by the generated implementations.
-keep class com.familyguardian.app.data.models.** { *; }

# Firestore DTOs are deserialised by field name via reflection.
-keep class com.familyguardian.app.data.remote.** { *; }

# Keep enum valueOf/values used for the supervision event and role decoding.
-keepclassmembers enum com.familyguardian.app.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# The notification listener is instantiated by the system from the manifest.
-keep class com.familyguardian.app.capture.WhatsAppNotificationListener { *; }

-dontwarn com.google.firebase.**
