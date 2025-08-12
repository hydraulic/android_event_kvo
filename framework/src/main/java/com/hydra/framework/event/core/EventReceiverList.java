package com.hydra.framework.event.core;

import com.yy.base.logger.MLog;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;


import static com.yy.base.event.core.EventDispatcher.EVENT_LOG_TAG;

/**
 * Created by Hydra.
 */
public class EventReceiverList {

    public static final int DEFAULT_RECEIVER_COUNT_THRESHOLD = 60;

    protected final TreeSet<EventReceiver> mReceivers;

    protected final EventAction mEventAction;

    private volatile EventIntent mLastStickyIntent;  //保证可见性

    public EventReceiverList(EventAction eventAction) {
        mReceivers = new TreeSet<>(EventReceiver.sEventReceiverComparator);

        mEventAction = eventAction;
    }

    /**
     * 为什么要把sticky的通知放到每个receiver list内部，而不是放到Dispatcher里呢
     * 因为要保证add Receiver的操作和notify sticky操作的原子性
     * 如果在Dispatcher内来做这个事，可以保证，但是锁的开销太大，会锁住所有事件的add和remove
     * 所有最好的方法是在每个Receiver list内部去做
     */
    public synchronized void add(EventReceiver eventReceiver) {
        boolean preExist = !mReceivers.add(eventReceiver);

        //event receiver的hashcode是通过里面的string的hash，理论上在同一个事件派发域内是不会重复的
        if (preExist) {
            MLog.warn(EVENT_LOG_TAG,
                    "add event destination warning, destination already exist : " + eventReceiver.toString());
            //不走下面的流程了
            return;
        }

        if (mEventAction.isSticky() && mLastStickyIntent != null) {
            invokeEventToReceiver(mLastStickyIntent, eventReceiver);
        }

        onAddBinding(eventReceiver);

        removeUnusedReceivers();
    }

    public void invokeToReceivers(EventIntent eventIntent) {
        TreeSet<EventReceiver> copiedReceivers;

        synchronized (this) {
            copiedReceivers = new TreeSet<>(mReceivers);

            //为什么这个操作放到这里，不放到通知完之后，因为通知的列表是一个copy列表，不用加锁
            //但是如果在for循环和refreshsticky操作之间，有一个Receiver被add了，这个时候通知的就是旧的sticky intent了
            //refresh后的新sticky intent永远不会被通知到
            if (mEventAction.isSticky()) {
                refreshStickyEventIntent(eventIntent);
            }
        }

        for (EventReceiver receiver : copiedReceivers) {
            invokeEventToReceiver(eventIntent, receiver);

            if (eventIntent.haveDone()) {
                break;
            }
        }
    }

    private void refreshStickyEventIntent(EventIntent eventIntent) {
        //a copy but sticky flag set true
        EventIntent stickyIntent = new EventIntent(eventIntent.eventSender(), mEventAction, true);
        stickyIntent.putBundle(eventIntent.eventBundle());

        mLastStickyIntent = stickyIntent;
    }

    public void invokeEventToReceiver(EventIntent eventIntent, EventReceiver receiver) {
        if (!receiver.invoke(eventIntent)) {
            remove(receiver);
        }
    }

    private void removeUnusedReceivers() {
        int size = size();

        if (size > DEFAULT_RECEIVER_COUNT_THRESHOLD) {
            MLog.warn(EVENT_LOG_TAG, "too many connections: " + size + " add to: " + mEventAction);

            Iterator<EventReceiver> iterator = mReceivers.iterator();

            while (iterator.hasNext()) {
                EventReceiver receiver = iterator.next();

                if (receiver == null || !receiver.isValid()) {
                    iterator.remove();
                }
            }
        }
    }

    public synchronized boolean remove(EventReceiver object) {
        return mReceivers.remove(object);
    }

    //double lock
    synchronized void onTrim(HashMap<EventAction, EventReceiverList> connections) {
        if (size() == 0) {
            connections.remove(mEventAction);
        }
    }

    public synchronized int size() {
        return mReceivers.size();
    }

    public EventIntent getLastStickyIntent() {
        return mLastStickyIntent;
    }

    protected void onAddBinding(EventReceiver receiver) {
    }
}
