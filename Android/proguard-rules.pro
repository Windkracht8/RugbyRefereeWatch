-keep,allowshrinking,allowoptimization class com.garmin.android.connectiq.IQDevice
-keep,allowshrinking,allowoptimization class com.garmin.android.connectiq.IQApp
-assumenosideeffects class android.util.Log{
    public static *** d(...);
    public static *** v(...);
}
