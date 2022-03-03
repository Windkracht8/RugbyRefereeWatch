-keepattributes InnerClasses,SourceFile,LineNumberTable

-keep class **.R -keep class **.R$* { <fields>; }
-keep class com.samsung.** { *; }
-keepclassmembers class com.samsung.** { *; }
-keepclassmembernames class com.samsung.** { *; }
-keepclasseswithmembernames class com.samsung.** { *; }
-keepclassmembers class com.samsung.** implements java.io.Serializable { *; }

-keep class com.windkracht8.rugbyrefereewatch.** { *; }
-keepclassmembers class com.windkracht8.rugbyrefereewatch.** { *; }
