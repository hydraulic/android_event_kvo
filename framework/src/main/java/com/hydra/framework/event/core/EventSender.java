package com.hydra.framework.event.core;

import java.lang.ref.WeakReference;

/**
 * Created by Hydra.
 * 记录下来发送事件的sender，参考ProcessRecord
 * 这个Sender只是一个弱记录，对于sticky的event来说，保存Sender Object有可能会内存泄漏
 * 放置的位置也不一定放在EventIntent中，也可以添加更多信息如pid uid之类的
 */
public class EventSender {

    private final Class<?> mSenderCls;

    private final WeakReference<Object> mSenderObject;  //防止内存泄漏

    private final int mSenderObjectHashCode;

    public EventSender(Object senderObject) {
        mSenderObject = new WeakReference<>(senderObject);

        mSenderCls = senderObject.getClass();

        mSenderObjectHashCode = senderObject.hashCode();
    }

    public Object senderObject() {
        return mSenderObject.get();
    }

    public Class<?> senderClass() {
        return mSenderCls;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EventSender)) {
            return false;
        }

        EventSender other = (EventSender) o;

        Object thisObj = mSenderObject.get();
        Object otherObj = other.mSenderObject.get();

        return other.mSenderCls == this.mSenderCls &&
                (thisObj != null && thisObj.equals(otherObj));
    }

    @Override
    public int hashCode() {
        return mSenderObjectHashCode;
    }
}
