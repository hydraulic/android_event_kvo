package com.hydra.framework.kvo.core.helper;

import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hydra.framework.kvo.core.IEventThread;
import com.hydra.framework.thread.ThreadBus;

/**
 * 辅助类，默认框架不带线程，只带线程接口IEventThread
 * 所以给个默认线程的简单实现
 */
public class DefaultEventThreadWrapper {

    @NonNull
    private static final SparseArray<IEventThread> sEventThreadMap = new SparseArray<>();

    static {
        for (int i = ThreadBus.Main; i < ThreadBus.Inherent_Thread_Index; i++) {
            final int idx = i;

            sEventThreadMap.put(i, r -> ThreadBus.post(idx, r));
        }
    }

    @Nullable
    public static IEventThread thread(int id) {
        return sEventThreadMap.get(id);
    }
}
