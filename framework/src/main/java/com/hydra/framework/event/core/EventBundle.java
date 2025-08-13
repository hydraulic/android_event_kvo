package com.hydra.framework.event.core;

import android.nfc.Tag;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.hydra.framework.event.utils.EventLog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Hydra.
 * 如果后续需要支持跨进程调用，需要添加序列化支持
 */
@SuppressWarnings("unchecked")
public class EventBundle {

    private static final String TAG = "EventBundle";

    public static final int EVENT_BUNDLE_FLAG_NONE = 0;

    private int mFlag;  //for extend

    private List<Object> mAllArgs; //all args, include named and no-key args
    private Map<String, Object> mKeyArgs; //args which has a key

    public EventBundle() {
        mAllArgs = new ArrayList<>();
        mKeyArgs = new HashMap<>();

        mFlag = EVENT_BUNDLE_FLAG_NONE;
    }

    public EventBundle(EventBundle other) {
        this();

        putAll(other);
    }

    @NonNull
    public List<Object> allArgs() {
        return mAllArgs;
    }

    @NonNull
    public Map<String, Object> keyArgs() {
        return mKeyArgs;
    }

    @Nullable
    public <T> T getArgWithKey(String key) {
        try {
            return (T) mKeyArgs.get(key);
        } catch (ClassCastException e) {
            EventLog.error(TAG, "EventBundle getArgWithKey failed : " + e.toString());
        }

        return null;
    }

    public boolean hasArg(String key) {
        return mKeyArgs.containsKey(key);
    }

    @Nullable
    public <T> T getArgWithIndex(int index) {
        if (mAllArgs.size() > index && index >= 0) {
            try {
                return (T) mAllArgs.get(index);
            } catch (ClassCastException e) {
                EventLog.error(TAG, "EventBundle getArgWithIndex failed : " + e.toString());
            }
        }
        return null;
    }

    //add an arg, maybe has key or no-key
    public void addArg(Object arg) {
        mAllArgs.add(arg);
    }

    /**
     * add some args with no key
     */
    public void addArgs(Object... args) {
        Collections.addAll(mAllArgs, args);
    }

    /**
     * put a arg with key, with no duplicated
     */
    public void putArg(@NonNull String key, @Nullable Object arg) {
        if (mKeyArgs.put(key, arg) == null) {
            addArg(arg);
        }
    }

    public void putAll(@NonNull EventBundle other) {
        mAllArgs.addAll(other.mAllArgs);
        mKeyArgs.putAll(other.mKeyArgs);

        mFlag |= other.mFlag;
    }
}
