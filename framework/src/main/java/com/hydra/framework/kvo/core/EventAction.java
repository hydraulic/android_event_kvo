package com.hydra.framework.kvo.core;

import androidx.annotation.NonNull;

import com.hydra.framework.utils.JFlagUtil;
import com.hydra.framework.utils.StringUtils;

/**
 * Created by Hydra.
 * 保证在一个dispatcher内的action唯一
 */
public class EventAction {

    //EventAction的flag范围是 1 ~ 1<<15 一共15个标志位，其他继承自EventAction的要从 1<<16 开始
    public static final int EVENTACTION_FLAG_STICKY = 1;

    @NonNull
    private final Object mAction;

    private final int mFlag;

    public EventAction(@NonNull Object action) {
        this(action, 0);
    }

    public EventAction(@NonNull Object action, int flag) {
        mAction = action;
        mFlag = flag;
    }

    @SuppressWarnings("unchecked")
    public <T> T getAction() {
        return (T) mAction;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        return getAction().equals(((EventAction) o).getAction());
    }

    @NonNull
    @Override
    public String toString() {
        return StringUtils.combineStr(getClass(), "-", mFlag, "-", mAction);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public int flag() {
        return mFlag;
    }

    public boolean isSticky() {
        return JFlagUtil.isFlag(mFlag, EVENTACTION_FLAG_STICKY);
    }
}
