package com.yy.base.event.kvo;

import com.yy.base.event.core.EventReceiver;
import com.yy.base.thread.ThreadBus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface KvoMethodAnnotation {
    String name();
    Class<? extends KvoSource> sourceClass();
    int thread() default ThreadBus.Sync;  //如果不指定线程，就在派发线程中同步调用
    int priority() default EventReceiver.DEFAULT_EVENT_RECEIVER_PRIORITY;
    int flag() default 0;
}
