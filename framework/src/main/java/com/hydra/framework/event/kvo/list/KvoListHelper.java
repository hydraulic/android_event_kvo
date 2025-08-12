package com.yy.base.event.kvo.list;

import static com.yy.base.event.kvo.list.KvoListHelper.KvoListChangeType.KvoEventArg_Type_Insert;
import static com.yy.base.event.kvo.list.KvoListHelper.KvoListChangeType.KvoEventArg_Type_Reload;
import static com.yy.base.event.kvo.list.KvoListHelper.KvoListChangeType.KvoEventArg_Type_Remove;
import static com.yy.base.event.kvo.list.KvoListHelper.KvoListChangeType.KvoEventArg_Type_Replace;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yy.base.event.kvo.KvoEventIntent;
import com.yy.base.event.kvo.KvoSource;

import java.util.List;

/**
 * Created by Hydra.
 */
public class KvoListHelper {

    public static class NSRange {
        public int position;
        public int length;

        public NSRange(int position, int length) {
            this.length = length;
            this.position = position;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof NSRange)) {
                return false;
            }

            NSRange other = (NSRange) obj;

            return other.position == position && other.length == length;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @NonNull
        @Override
        public String toString() {
            return "NSRange : " + position + "_" + length;
        }
    }

    public enum KvoListChangeType {
        KvoEventArg_Type_Insert,
        KvoEventArg_Type_Remove,
        KvoEventArg_Type_Replace,
        KvoEventArg_Type_Move,
        KvoEventArg_Type_Reload;
    }

    //list变更的种类，#KvoListChangeType
    public static final String KvoList_EventArg_Key_Type = "KvoList_EventArg_Key_Type";
    //list变更的范围，#NSRange
    public static final String KvoList_EventArg_Key_Range = "KvoList_EventArg_Key_Range";

    public static <T> void notifyInsert(KvoSource source, String name, List<T> list, int location, int length) {
        notifyKvoArrayChange(source, name, list, KvoEventArg_Type_Insert, new NSRange(location, length));
    }

    public static <T> void notifyRangeRemove(KvoSource source, String name, List<T> list, int location, int length) {
        notifyKvoArrayChange(source, name, list, KvoEventArg_Type_Remove, new NSRange(location, length));
    }

    public static <T> void notifyMove(KvoSource source, String name, List<T> list, int oldLocation, int newLocation) {
        notifyKvoArrayChange(source, name, list, KvoListChangeType.KvoEventArg_Type_Move, new NSRange(oldLocation,
                newLocation - oldLocation));
    }

    public static <T> void notifyRangeReplace(KvoSource source, String name, List<T> list, int location, int length) {
        notifyKvoArrayChange(source, name, list, KvoEventArg_Type_Replace, new NSRange(location, length));
    }

    public static <T> void notifyReload(KvoSource source, String name, List<T> list) {
        notifyKvoArrayChange(source, name, list, KvoEventArg_Type_Reload, new NSRange(0, list.size()));
    }

    private static <T> void notifyKvoArrayChange(KvoSource source, String name, List<T> list,
                                                 KvoListChangeType type, NSRange range) {

        KvoEventIntent kvoEventIntent = KvoEventIntent.build(source, name);

        //TODO 这里没有把旧的值设置成null
        kvoEventIntent.setOldValue(list);
        kvoEventIntent.setNewValue(list);

        kvoEventIntent.putArg(KvoList_EventArg_Key_Range, range);
        kvoEventIntent.putArg(KvoList_EventArg_Key_Type, type);

        source.notifyEvent(kvoEventIntent);
    }

    /**
     * 绑定时的通知，range是null，所以给一个默认值
     */
    public static NSRange getNotifyRange(KvoEventIntent intent) {
        NSRange range = intent.getArgWithKey(KvoList_EventArg_Key_Range);

        return range == null ? new NSRange(0, 0) : range;
    }

    /**
     * 绑定时的通知，type是null，所以给一个默认值
     */
    @NonNull
    public static KvoListChangeType getNotifyType(KvoEventIntent intent) {
        KvoListChangeType type = intent.getArgWithKey(KvoList_EventArg_Key_Type);

        return type == null ? KvoEventArg_Type_Reload : type;
    }
}
