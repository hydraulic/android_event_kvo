package com.hydra.framework.event.core;

import androidx.annotation.NonNull;
import java.util.HashMap;

/**
 * Created by Hydra.
 * <p>
 * 这个结构是一个经典的cache存取的结构，但是比一般的cache结构多了一点
 * 就是receiverList.add(receiver) 和 mConnections.remove(eventAction) 这两步操作的顺序
 * 核心问题是：receiverList的内部操作会影响到外部mConnections行为
 * <p>
 * 有三种做法：
 * 1、完全不管，只对mConnections的读写加锁，这样就会出现多线程同时对一个action(即使是不同Receiver也会出现)
 * 进行add和remove时，取出了一个receiverList，进行addReceiver操作后，这个receiverList被remove了，
 * 这样就会发现add了一个Receiver，但是怎么都收不到通知
 * <p>
 * 2、把add和remove整个全部加锁，保证两个函数流程的原子性和完全互斥，但是代价太大了，一个action的操作要锁住所有
 * <p>
 * 3、对receiverList.add(receiver)加入读锁，保证不阻塞读流程，当receiverList.size() == 0 时，
 * 进入mConnections.remove(mEventAction)流程，调用receiverList.onTrim(mConnections)，同时加写锁
 * 和receiverList内部的锁，保证外部操作和内部操作两方面的原子性
 * <p>
 * --------------------------------------------------------------------
 * 所以目前采用第三种方案，即 不保证add和remove两个函数流程完全的原子性，
 * 也即 同一个action的同一个Receiver的add和remove操作，如果不在一个线程中，只能靠调用者去保证时序
 * 但是在移动端几乎没有地方需要高并发地进行这种操作，而且同一个Receiver的业务方，保证时序是比较简单的
 * ---------------------------------------------------------------------
 * 更新，去掉读写锁的写法，转为synchronize，因为在addBinding后的一次通知里，可能会有二次绑定，造成同一线程的读写锁重入死锁，
 * 如果针对这种情形去设计一个更复杂的锁，感觉性价比不高，这个复杂的点就在于是一个map，可能有其他的key造成重入，就用synchronize吧
 */
public class EventDispatcher {

    public static final String EVENT_LOG_TAG = "FrameWork_Event";

    private transient final Object mLock = new Object();

    private final transient HashMap<EventAction, EventReceiverList> mConnections = new HashMap<>();

    public void addBinding(@NonNull final EventAction eventAction, @NonNull final EventReceiver receiver) {
        EventReceiverList receiverList;

        synchronized (mLock) {
            receiverList = mConnections.get(eventAction);

            if (receiverList == null) {
                receiverList = buildEventReceiverList(eventAction);
                mConnections.put(eventAction, receiverList);
            }

            receiverList.add(receiver); //操作要在锁内，为了保证sticky(比如kvo绑定时的那次)通知的时序是最早的
        }
    }

    protected EventReceiverList buildEventReceiverList(final EventAction eventAction) {
        return new EventReceiverList(eventAction);
    }

    public void removeBinding(@NonNull final EventAction eventAction, @NonNull final EventReceiver receiver) {
        EventReceiverList receiverList;

        synchronized (mLock) {
            receiverList = mConnections.get(eventAction);
        }

        if (receiverList == null) {
            return;
        }

        receiverList.remove(receiver);

        //在这里判断size，不在下面的二次get加锁里面判断是因为trim操作是一个不要求很严格的操作
        if (eventAction.isSticky() || receiverList.size() > 0) {
            return;
        }

        synchronized (mLock) {
            receiverList = mConnections.get(eventAction);

            if (receiverList != null) {
                receiverList.onTrim(mConnections);
            }
        }
    }

    public void notifyEvent(@NonNull final EventIntent eventIntent) {
        EventAction eventAction = eventIntent.eventAction();

        EventReceiverList receiverList;

        synchronized (mLock) {
            receiverList = mConnections.get(eventAction);
        }

        if (receiverList != null) {
            receiverList.invokeToReceivers(eventIntent);
            return;
        }

        //虽然receiverList是空的，但是因为是sticky的，为了把最后一次的intent记录在ReceiverList中
        if (!eventAction.isSticky()) {
            return;
        }

        synchronized (mLock) {
            receiverList = mConnections.get(eventAction);

            if (receiverList == null) {
                receiverList = buildEventReceiverList(eventAction);

                mConnections.put(eventAction, receiverList);

                //invoke nothing, only for save the last sticky intent
                receiverList.invokeToReceivers(eventIntent);
            }
        }
    }

    public boolean hasConnections() {
        synchronized (mLock) {
            return !mConnections.isEmpty();
        }
    }
}
