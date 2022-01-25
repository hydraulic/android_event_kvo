package com.hydra.framework.kvo;

import androidx.annotation.NonNull;

import com.hydra.framework.kvo.core.EventIntent;
import com.hydra.framework.kvo.core.EventReceiver;
import com.hydra.framework.kvo.core.IEventThread;
import com.hydra.framework.kvo.helper.KvoHelper;
import com.hydra.framework.thread.ThreadBus;
import com.hydra.framework.utils.JFlagUtil;

import java.lang.reflect.Method;

public class KvoEventReceiver extends EventReceiver {

    KvoEventReceiver(Object target, Method entry, IEventThread thread, int priority, int flag) {
        super(target, entry, thread, priority, flag);
    }

    @Override
    protected void scheduleInvoke(@NonNull Object targetObj, EventIntent eventIntent) {
        if (JFlagUtil.isFlag(eventIntent.eventAction().flag(),
                KvoHelper.KVO_EVENT_ACTION_FLAG_FORCE_SYNC) && thread != null) {
            throw new RuntimeException("you are using a data notify only can sync notify, " +
                    "don't use thread=XXX in your receiver method: " + toString());
        }

        super.scheduleInvoke(targetObj, eventIntent);
    }

    @Override
    protected void doInvoke(Object target, EventIntent eventIntent) {
        if (JFlagUtil.isFlag(eventIntent.eventAction().flag(),
                KvoHelper.KVO_EVENT_ACTION_FLAG_FORCE_MAIN) && !ThreadBus.isMainThread()) {
            throw new RuntimeException("you are using a data notify only can notify in main, " +
                    "don't receive in other thread: " + toString());
        }

        super.doInvoke(target, eventIntent);
    }
}
