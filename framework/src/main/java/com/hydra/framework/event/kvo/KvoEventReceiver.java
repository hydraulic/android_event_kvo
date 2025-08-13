package com.hydra.framework.event.kvo;

import android.nfc.Tag;
import androidx.annotation.NonNull;

import com.hydra.framework.event.core.EventIntent;
import com.hydra.framework.event.core.EventReceiver;
import com.hydra.framework.event.core.IEventThread;
import com.hydra.framework.event.kvo.helper.KvoHelper;
import com.hydra.framework.event.utils.EventLog;
import com.hydra.framework.event.utils.EventUtils;
import com.hydra.framework.thread.ThreadBus;
import com.hydra.framework.utils.JFlagUtil;
import java.lang.reflect.Method;

public class KvoEventReceiver extends EventReceiver {

    private static final String TAG = "KvoEventReceiver";

    KvoEventReceiver(Object target, Method entry, IEventThread thread, int priority, int flag) {
        super(target, entry, thread, priority, flag);
    }

    @Override
    protected void scheduleInvoke(@NonNull Object targetObj, EventIntent eventIntent) {
        if (JFlagUtil.isFlag(eventIntent.eventAction().flag(),
                KvoHelper.KVO_EVENT_ACTION_FLAG_FORCE_SYNC) && thread != null) {
            if (EventUtils.sIsDebuggable) {
                throw new RuntimeException("you are using a data notify only can sync notify, " +
                        "don't use thread=XXX in your receiver method: " + toString());
            } else {
                EventLog.error(TAG, "scheduleInvoke error don't use thread=XXX in your receiver method");
            }
        }

        super.scheduleInvoke(targetObj, eventIntent);
    }

    @Override
    protected void doInvoke(Object target, EventIntent eventIntent) {
        if (EventUtils.sIsDebuggable) {
            if (JFlagUtil.isFlag(eventIntent.eventAction().flag(),
                    KvoHelper.KVO_EVENT_ACTION_FLAG_FORCE_MAIN) && !ThreadBus.isMainThread()) {
                throw new RuntimeException("you are using a data notify only can notify in main, " +
                        "don't receive in other thread: " + toString());
            }
        }

        super.doInvoke(target, eventIntent);
    }
}
