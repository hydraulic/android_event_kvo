package com.hydra.framework.kvo.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hydra.framework.utils.JFlagUtil;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Hydra.
 * 可以后续加入Action/Scheme等过滤条件，相应的dispatcher也要更复杂
 * 如果需要支持跨进程调用，添加序列化支持
 * EventIntent和EventBundle是模仿系统的Intent和Bundle来写的
 */
public class EventIntent {

    // TODO: hydra add more flag
    public static final int EVENT_INTENT_DONE_FLAG = 1;

    private final EventAction mEventAction;
    private final EventSender mSender;

    private final AtomicInteger mFlag;

    /**
     * 此次事件是否是sticky类型的通知
     */
    private final boolean mIsStickyNotify;

    private EventBundle mBundle;

    public EventIntent(@Nullable EventSender sender, @NonNull EventAction eventAction, boolean isStickyNotify) {
        mBundle = new EventBundle();

        mSender = sender;
        mEventAction = eventAction;

        mFlag = new AtomicInteger(0);

        mIsStickyNotify = isStickyNotify;
    }

    @NonNull
    public EventAction eventAction() {
        return mEventAction;
    }

    @Nullable
    public EventSender eventSender() {
        return mSender;
    }

    @NonNull
    public EventBundle eventBundle() {
        return mBundle;
    }

    public boolean isStickyNotify() {
        return mIsStickyNotify;
    }

    @Nullable
    public <T> T getArgWithIndex(int index) {
        return mBundle.getArgWithIndex(index);
    }

    public void putBundle(@NonNull EventBundle eventBundle) {
        mBundle.putAll(eventBundle);
    }

    // convenience for allArgs
    @Nullable
    public <T> T arg0() {
        return getArgWithIndex(0);
    }

    @Nullable
    public <T> T arg1() {
        return getArgWithIndex(1);
    }

    @Nullable
    public <T> T arg2() {
        return getArgWithIndex(2);
    }

    public void putArg(@NonNull String name, @Nullable Object arg) {
        mBundle.putArg(name, arg);
    }

    public <T> T getArgWithKey(@NonNull String key) {
        return mBundle.getArgWithKey(key);
    }

    public void addArgs(@NonNull Object... args) {
        mBundle.addArgs(args);
    }

    public void addFlag(int newFlag) {
        int current;
        int nextFlag;

        do {
            current = mFlag.get();

            nextFlag = current | newFlag;
        } while (!mFlag.compareAndSet(current, nextFlag));
    }

    public int flag() {
        return mFlag.get();
    }

    public void done() {
        addFlag(EVENT_INTENT_DONE_FLAG);
    }

    /**
     * TODO 对于接受者不在同一线程中的情况，done的标志位还需要再加一些处理
     * 目前这里是和eventbus一样的写法，但是我觉得这样写不太好
     * 可以后面加入专门的interceptor来同步回调拦截
     */
    public boolean haveDone() {
        return JFlagUtil.isFlag(mFlag.get(), EVENT_INTENT_DONE_FLAG);
    }
}
