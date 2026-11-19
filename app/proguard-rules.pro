# Project ProGuard / R8 Rules for Church Connect

# Preserve stack trace line numbers and reflection metadata
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes *Annotation*

# Preserve Firestore data models used with DocumentSnapshot.toObject()
-keep class com.example.core.model.** { *; }
-keepclassmembers class com.example.core.model.** {
    public <init>(...);
    <fields>;
    <methods>;
}

# Preserve Kotlin enums serialized to Firestore
-keepclassmembers enum com.example.core.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
