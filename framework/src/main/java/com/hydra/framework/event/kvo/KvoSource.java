package com.yy.base.event.kvo;

import static com.yy.base.event.kvo.Kvo.KVO_LOG_TAG;
import static com.yy.base.event.kvo.helper.KvoHelper.kvoFieldsContainerFor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yy.base.FrameworkRuntimeContext;
import com.yy.base.event.core.EventAction;
import com.yy.base.event.core.EventDispatcher;
import com.yy.base.event.core.EventReceiverList;
import com.yy.base.event.kvo.helper.KvoHelper.KvoField;
import com.yy.base.logger.MLog;
import com.yy.base.utils.FrameworkSettingFlagKeys;
import com.yy.base.utils.SettingFlags;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * Created by Hydra.
 * KvoSource在绑定时，是不会校验绑定的key是否存在，因为默认receiver中接收通知的函数都是有KvoMethodAnnotation的
 * 这里是不支持绑定一个没有KvoMethodAnnotation的函数的，即使传了函数名，也会绑定不成功
 */
public class KvoSource extends EventDispatcher {

    private transient final HashMap<String, KvoField> mKvoValues = kvoFieldsContainerFor(getClass());

    /**
     * 手动通知, force notify once
     */
    public void notifyKvoEvent(@NonNull String key) {
        KvoField kvoField = declaredKvoField(key);

        if (kvoField == null) {
            return;
        }

        try {
            Object currentValue = kvoField.field.get(this);

            KvoEventIntent kvoEventIntent = KvoEventIntent.build(this, key);

            //这里是手动去通知，oldValue和newValue用一样的
            kvoEventIntent.setOldValue(currentValue);
            kvoEventIntent.setNewValue(currentValue);

            notifyEvent(kvoEventIntent);
        } catch (IllegalAccessException e) {
            MLog.error(KVO_LOG_TAG, "get field value failed : " + e.toString());
        }
    }

    /**
     * 手动通知, force notify once
     */
    public void notifyKvoEvent(@NonNull String key, @Nullable Object newValue) {
        KvoField kvoField = declaredKvoField(key);

        if (kvoField == null) {
            return;
        }

        try {
            Field targetField = kvoField.field;

            Object oldValue = targetField.get(this);

            targetField.set(this, newValue);

            KvoEventIntent kvoEventIntent = KvoEventIntent.build(this, key);
            kvoEventIntent.setOldValue(oldValue);
            kvoEventIntent.setNewValue(newValue);

            notifyEvent(kvoEventIntent);
        } catch (IllegalAccessException e) {
            MLog.error(KVO_LOG_TAG, "get field value failed : " + e.toString());
        }
    }

    @Override
    protected EventReceiverList buildEventReceiverList(EventAction eventAction) {
        return new KvoEventReceiverList(eventAction, this);
    }

    public KvoField declaredKvoField(String key) {
        return mKvoValues.get(key);
    }

    public void setValue(@NonNull String key, @Nullable Object newValue) {
        Object oldValue = null;

        try {
            KvoField kvoField = declaredKvoField(key);

            if (kvoField == null) {
                return;
            }

            Field targetField = kvoField.field;

            oldValue = targetField.get(this);

            if (!(oldValue == null ? newValue == null : oldValue.equals(newValue))) {
                targetField.set(this, newValue);

                KvoEventIntent kvoEventIntent = KvoEventIntent.build(this, key);
                kvoEventIntent.setOldValue(oldValue);
                kvoEventIntent.setNewValue(newValue);

                notifyEvent(kvoEventIntent);
            }
        } catch (Exception e) {
            MLog.error(KVO_LOG_TAG, "notify kvo event failed:", e);

            if (FrameworkRuntimeContext.sIsDebuggable) {
                boolean switchOn = SettingFlags.getBoolean(FrameworkSettingFlagKeys.ENV_PAGE_AUTO_SWITCH,
                        false);
                boolean switchOnVoice =
                        SettingFlags.getBoolean(FrameworkSettingFlagKeys.ENV_PAGE_AUTO_SWITCH_VOICE,
                                false);
                if (!switchOn && !switchOnVoice) {
                    throw new RuntimeException("exception when setValue, key: " + key +
                            ", oldValue" + (oldValue == null ? "null" : oldValue.toString()) +
                            ", newValue" +
                            newValue + ", error: " + e.toString(), e);
                }
            }
        }
    }
}
