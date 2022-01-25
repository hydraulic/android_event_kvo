package com.hydra.framework.kvo.core;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
 */
public class EventDispatcher {

    public transient static final String EVENT_LOG_TAG = "FrameWork_Event";

    private final transient HashMap<EventAction, EventReceiverList> mConnections = new HashMap<>();

    private transient final ReentrantReadWriteLock mReadWriteLock = new ReentrantReadWriteLock();

    public void addBinding(@NonNull final EventAction eventAction, @NonNull final EventReceiver receiver) {
        mReadWriteLock.readLock().lock();

        EventReceiverList receiverList = mConnections.get(eventAction);

        if (receiverList == null) {
            mReadWriteLock.readLock().unlock();
            mReadWriteLock.writeLock().lock();

            //double check
            receiverList = mConnections.get(eventAction);

            if (receiverList == null) {
                receiverList = buildEventReceiverList(eventAction);
                mConnections.put(eventAction, receiverList);
            }

            mReadWriteLock.readLock().lock();   //锁降级
            mReadWriteLock.writeLock().unlock();
        }

        receiverList.add(receiver); //操作要在读锁内

        mReadWriteLock.readLock().unlock();

        onAddBinding(eventAction, receiver, receiverList);
    }

    protected EventReceiverList buildEventReceiverList(final EventAction eventAction) {
        return new EventReceiverList(eventAction);
    }

    protected void onAddBinding(@NonNull EventAction eventAction, @NonNull EventReceiver receiver,
                                @NonNull EventReceiverList receiverList) {
    }

    public void removeBinding(@NonNull final EventAction eventAction, @NonNull final EventReceiver receiver) {
        mReadWriteLock.readLock().lock();

        EventReceiverList receiverList = mConnections.get(eventAction);

        if (receiverList != null) {
            mReadWriteLock.readLock().unlock();

            receiverList.remove(receiver);

            if (!eventAction.isSticky() && receiverList.size() == 0) {
                mReadWriteLock.writeLock().lock();

                receiverList.onTrim(mConnections);

                mReadWriteLock.writeLock().unlock();
            }

            onRemoveBinding(eventAction, receiver, receiverList);
        } else {
            mReadWriteLock.readLock().unlock();
        }
    }

    protected void onRemoveBinding(@NonNull EventAction eventAction, @NonNull EventReceiver receiver,
                                   @NonNull EventReceiverList receiverList) {
    }

    public void notifyEvent(@NonNull final EventIntent eventIntent) {
        EventAction eventAction = eventIntent.eventAction();

        mReadWriteLock.readLock().lock();

        EventReceiverList receiverList = mConnections.get(eventAction);

        mReadWriteLock.readLock().unlock();

        if (receiverList != null) {
            receiverList.invokeToReceivers(eventIntent);
        } else {
            if (eventIntent.eventAction().isSticky()) {
                try {
                    mReadWriteLock.writeLock().lock();

                    receiverList = mConnections.get(eventAction);

                    //double check
                    if (receiverList == null) {
                        receiverList = buildEventReceiverList(eventAction);
                        mConnections.put(eventAction, receiverList);
                    }

                    receiverList.invokeToReceivers(eventIntent);
                } finally {
                    mReadWriteLock.writeLock().unlock();
                }
            }
        }
    }

    public boolean hasConnections() {
        mReadWriteLock.readLock().lock();

        try {
            return !mConnections.isEmpty();
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }
}
