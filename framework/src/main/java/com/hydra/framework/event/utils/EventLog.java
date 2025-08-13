package com.hydra.framework.event.utils;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by Hydra.
 * 外部日志接口
 */
public class EventLog {

    public static void error(@NonNull String tag, @Nullable Object msgObj) {
        Log.e(tag, msgObj == null ? "null" : msgObj.toString());
    }

    public static void warn(@NonNull String tag, @Nullable Object msgObj) {
        Log.w(tag, msgObj == null ? "null" : msgObj.toString());
    }

    public static void info(@NonNull String tag, @Nullable Object msgObj) {
        Log.i(tag, msgObj == null ? "null" : msgObj.toString());
    }

    public static void debug(@NonNull String tag, @Nullable Object msgObj) {
        Log.d(tag, msgObj == null ? "null" : msgObj.toString());
    }

    public static void verbose(@NonNull String tag, @Nullable Object msgObj) {
        Log.v(tag, msgObj == null ? "null" : msgObj.toString());
    }
}
