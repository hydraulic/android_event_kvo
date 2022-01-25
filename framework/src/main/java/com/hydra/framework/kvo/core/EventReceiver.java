package com.hydra.framework.kvo.core;

import static com.hydra.framework.kvo.core.EventDispatcher.EVENT_LOG_TAG;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hydra.framework.utils.StringUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Comparator;

/**
 * Created by Hydra.
 */
public class EventReceiver {

    public static final int DEFAULT_EVENT_RECEIVER_PRIORITY = 0;

    static Comparator<EventReceiver> sEventReceiverComparator = (lhs, rhs) -> {
        if (lhs.mReceiverHashCode == rhs.mReceiverHashCode) {
            return 0;
        }

        int priorityResult = Integer.compare(rhs.priority, lhs.priority);

        return priorityResult > 0 ? 1 : (priorityResult < 0 ? -1 :
                Integer.compare(rhs.mReceiverHashCode, lhs.mReceiverHashCode));
    };

    @Nullable
    protected final IEventThread thread;  //if null 同步调用
    protected final int priority;
    protected final int flag;
    protected final WeakReference<Object> target;
    protected final Method entry;
    protected final int mReceiverHashCode;

    public EventReceiver(Object target, Method entry, @Nullable IEventThread thread, int priority, int flag) {
        this.target = new WeakReference<>(target);
        this.entry = entry;
        this.thread = thread;
        this.flag = flag;
        this.priority = priority;

        mReceiverHashCode = StringUtils.combineStr(target.getClass(), entry, target.hashCode()).hashCode();
    }

    public boolean invoke(final EventIntent eventIntent) {
        final Object targetObj = target.get();

        if (targetObj == null) {
            Log.i(EVENT_LOG_TAG, "invoke failed target has been recycled, method is : "
                    + entry.toGenericString());
            return false;
        }

        scheduleInvoke(targetObj, eventIntent);

        return true;
    }

    protected void scheduleInvoke(@NonNull Object targetObj, final EventIntent eventIntent) {
        if (thread != null) {
            thread.post(() -> doInvoke(targetObj, eventIntent));
        } else {
            doInvoke(targetObj, eventIntent);
        }
    }

    protected void doInvoke(Object target, EventIntent eventIntent) {
        try {
            entry.invoke(target, eventIntent);
        } catch (Throwable e) {
            Log.e(EVENT_LOG_TAG, "invoke failed target error : " + Log.getStackTraceString(e) +
                    " cause : " + e.getCause() + " method : " + entry.toString());

            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EventReceiver)) {
            return false;
        }

        EventReceiver other = (EventReceiver) obj;

        return other.mReceiverHashCode == mReceiverHashCode && other.entry == entry
                && thread == other.thread && priority == other.priority
                && flag == other.flag;
    }

    /**
     * 因为这里保存的是软引用，所以target有可能被回收
     */
    public boolean isValid() {
        return target.get() != null;
    }

    @Override
    public int hashCode() {
        return mReceiverHashCode;
    }

    @NonNull
    @Override
    public String toString() {
        return StringUtils.safeCombineStr("target : ", target.get(), " entry : ", entry,
                " thread : ", thread, " priority : ", priority, " mReceiverHashCode : ", mReceiverHashCode);
    }
}
