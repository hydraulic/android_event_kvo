package com.hydra.framework.event.fw;

import com.hydra.framework.event.core.EventReceiver;
import com.hydra.framework.thread.ThreadBus;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface FWEventAnnotation {
    FWEventActionKey name();

    int priority() default EventReceiver.DEFAULT_EVENT_RECEIVER_PRIORITY;

    //如果不指定线程，就在派发线程中同步调用，相当于registerReceiver中的scheduler handler
    int thread() default ThreadBus.Sync;

    int flag() default 0;
}
