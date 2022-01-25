package com.hydra.framework.kvo.helper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hydra.framework.kvo.Kvo;
import com.hydra.framework.kvo.KvoSource;

import java.util.HashMap;
import java.util.Map.Entry;

/**
 * 一个帮助类，为了方便解决 一次/多次绑定 同一类型 的 同一对象或者不同对象 时的便利性，不用解绑再绑定；
 * 还有就是可以无脑一键解绑，不用在最终解绑时再依次拿一遍所有的Source对象
 *
 * 每个key下一个独立的KvoSource，single Bind 顾名思义，就是对同一个key下的KvoSource，如果是singleBind，就会解绑之前的
 *
 * 1、绑定/解绑 同一类型 的 同一对象时，
 *   比如在列表里的item，列表滑动时会不断复用更新同一类型的数据，使用singleBindSourceTo(KvoSource)；
 *   这时每个对象对应的 key = source.getClass().name
 *
 * 2、绑定/解绑 同一类型 的 不同对象 到 不同的方法 时，比如PKGamePlayer里有自己和其他人的连麦状态，
 *   使用singleBindSourceWithFlag(KvoSource, flag)，同时在receiver的接收方法注解里，对不同的对象加入不同的flag；
 *   这时每个对象对应的 key = source.getClass().name + "_" + flag；
 *   对应解绑：clearKvoConnectionWithFlag(KvoSource, flag)
 *
 * 3、绑定/解绑 同一类型 的 不同对象 到 同一个方法 时，可以自定义key，使用singleBindSourceTo(key, source)
 *   比如想绑定一个列表里的所有item到同一个方法上；对应解绑 clearKvoConnection(key)
 *
 * 解绑所有时，无脑调用clearAllKvoConnections就可以了
 */
public class KvoBinder {

    private final Object mTarget;

    private final HashMap<String, KvoSource> mKvoSources = new HashMap<>();

    public KvoBinder(Object target) {
        mTarget = target;
    }

    /**
     * 直接以source的 class.name 当作key
     */
    public boolean singleBindSourceTo(@Nullable KvoSource source) {
        if (source == null) {
            return false;
        }

        return singleBindSourceTo(source.getClass().getName(), source);
    }

    public boolean singleBindSourceTo(@NonNull String key, @Nullable KvoSource source) {
        if (source == null) {
            return false;
        }

        synchronized (this) {
            KvoSource oldSource = mKvoSources.get(key);

            //注意，这里用了==而没有用equals
            if (oldSource == source) {
                return false;
            }

            if (oldSource != null) {
                Kvo.autoUnbindingFrom(oldSource, mTarget);
            }

            Kvo.autoBindingTo(source, mTarget);

            mKvoSources.put(key, source);

            return true;
        }
    }

    /**
     * 这个方法是为了：一个receiver对象，多次 绑定 同一个类型 的 多个对象 时使用
     */
    public boolean singleBindSourceWithFlag(@Nullable KvoSource source, int flag) {
        if (source == null) {
            return false;
        }

        return singleBindSourceWithFlag(source.getClass().getName() + "_" + flag, source, flag);
    }

    /**
     * 用于多对多绑定，一般不会用到这个，除非你知道自己在干啥；
     * 可能用到的地方，receiver有继承关系并且父子类绑定了同一个类型，
     */
    public boolean singleBindSourceWithFlag(@NonNull String key, @Nullable KvoSource source, int flag) {
        if (source == null) {
            return false;
        }

        synchronized (this) {
            KvoSource oldSource = mKvoSources.get(key);

            //注意，这里用了==而没有用equals
            if (oldSource == source) {
                return false;
            }

            if (oldSource != null) {
                // 只 解绑 指定flag的，其实可以无脑全部解绑的，不过为了逻辑的一致性，就这么做也没问题
                Kvo.autoUnbindingFrom(oldSource, mTarget, flag);
            }

            Kvo.autoBindingTo(source, mTarget, flag);

            mKvoSources.put(key, source);

            return true;
        }
    }

    /**
     * 一般用于带flag的绑定，即同类型，多对象，唯一绑定
     */
    public void clearKvoConnectionWithFlag(@Nullable KvoSource source, int flag) {
        if (source == null) {
            return;
        }

        synchronized (this) {
            // 只 解绑 指定flag的，其实可以无脑全部解绑的，不过为了逻辑的一致性，就这么做也没问题
            Kvo.autoUnbindingFrom(source, mTarget, flag);

            //注意：这里的key是按照 source.getClass().name + "_" + flag 规则来的
            mKvoSources.remove(source.getClass().getName() + "_" + flag);
        }
    }

    public synchronized void clearKvoConnection(String key) {
        KvoSource source = mKvoSources.remove(key);

        if (source != null) {
            Kvo.autoUnbindingFrom(source, mTarget);
        }
    }

    public synchronized void clearAllKvoConnections() {
        if (mKvoSources.size() <= 0) {
            return;
        }

        for (Entry<String, KvoSource> entry : mKvoSources.entrySet()) {
            KvoSource value = entry.getValue();
            if (value != null) {
                Kvo.autoUnbindingFrom(value, mTarget);
            }
        }

        mKvoSources.clear();
    }
}
