package com.yy.base.event.kvo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yy.base.event.core.EventAction;
import com.yy.base.event.core.EventIntent;
import com.yy.base.event.core.EventSender;

/**
 * Created by Hydra.
 * 其实这里也可以直接继承EventBundle
 */
public class KvoEventIntent extends EventIntent {

    private static final String OLD_VALUE_KEY = "kvo_old_value";
    private static final String NEW_VALUE_KEY = "kvo_new_value";
    private static final String FROM_KEY = "kvo_from";

    public static KvoEventIntent build(KvoSource source, String name) {
        return build(source, name, false);
    }

    public static KvoEventIntent build(KvoSource source, String name, boolean isStickyNotify) {
        EventSender eventSender = new EventSender(source);
        EventAction eventAction = source.declaredKvoField(name).eventAction;

        KvoEventIntent kvoEventIntent = new KvoEventIntent(eventSender, eventAction, isStickyNotify);

        kvoEventIntent.setKvoSource(source);

        return kvoEventIntent;
    }

    public KvoEventIntent(EventSender sender, EventAction eventAction, boolean isStickyNotify) {
        super(sender, eventAction, isStickyNotify);
    }

    public void setKvoSource(KvoSource kvoSource) {
        putArg(FROM_KEY, kvoSource);
    }

    public void setOldValue(Object oldValue) {
        putArg(OLD_VALUE_KEY, oldValue);
    }

    public void setNewValue(Object newValue) {
        putArg(NEW_VALUE_KEY, newValue);
    }

    @NonNull
    public <T extends KvoSource> T source() {
        return getArgWithKey(FROM_KEY);
    }

    @Nullable
    public <T> T newValue() {
        return getArgWithKey(NEW_VALUE_KEY);
    }

    @Nullable
    public <T> T oldValue() {
        return getArgWithKey(OLD_VALUE_KEY);
    }

    @NonNull
    public <T> T caseNewValue(@NonNull T def) {
        T value = newValue();

        return value == null ? def : value;
    }
}
