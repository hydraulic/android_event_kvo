package com.yy.base.event.kvo;

import static com.yy.base.event.kvo.Kvo.KVO_LOG_TAG;

import com.yy.base.event.core.EventAction;
import com.yy.base.event.core.EventReceiver;
import com.yy.base.event.core.EventReceiverList;
import com.yy.base.event.kvo.helper.KvoHelper.KvoField;
import com.yy.base.logger.MLog;

/**
 * kvo没有用event Dispatcher对于sticky处理的数据结构, 但是使用了sticky的机制
 * <p>
 * 为什么不直接使用sticky呢，因为对于sticky来说，每个event都会存最后一份，但是在kvo里，
 * 每个对象都是一个Dispatcher，这样的内存会大很多
 */
public class KvoEventReceiverList extends EventReceiverList {

    private final KvoSource mKvoSource;

    KvoEventReceiverList(EventAction eventAction, KvoSource source) {
        super(eventAction);

        mKvoSource = source;
    }

    @Override
    protected void onAddBinding(EventReceiver eventReceiver) {
        KvoField kvoField = mKvoSource.declaredKvoField(mEventAction.getAction());

        if (kvoField == null) {
            return;
        }

        try {
            Object currentValue = kvoField.field.get(mKvoSource);

            // 这个通知虽然不是一个标准意义上的sticky Notify
            // 但是还是需要用这个标志位来标识此次通知
            KvoEventIntent kvoEventIntent = KvoEventIntent.build(mKvoSource,
                    kvoField.fieldAnnotation.name(), true);

            //TODO 这里没有把旧的值设置成null
            kvoEventIntent.setOldValue(currentValue);
            kvoEventIntent.setNewValue(currentValue);

            invokeEventToReceiver(kvoEventIntent, eventReceiver);
        } catch (IllegalAccessException e) {
            MLog.error(KVO_LOG_TAG, "get field value failed : " + e.toString());
        }
    }
}
