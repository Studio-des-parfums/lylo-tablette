# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.lylo.tablet.**$$serializer { *; }
-keepclassmembers class com.lylo.tablet.** { *** Companion; }
-keepclasseswithmembers class com.lylo.tablet.** { kotlinx.serialization.KSerializer serializer(...); }

# LiveKit / WebRTC
-keep class org.webrtc.** { *; }
-keep class io.livekit.android.** { *; }
