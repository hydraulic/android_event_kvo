package com.yy.base.event.kvo.list;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yy.base.event.kvo.KvoFieldAnnotation;
import com.yy.base.event.kvo.KvoSource;
import com.yy.base.logger.MLog;

import java.util.List;

import common.Page;

/**
 * Created by Hydra.
 *
 * 对常用的分页数据的封装
 *
 * 数据状态 -- 分两部分
 * 1) 列表实体数据状态，即 datas
 * 2) 分页状态，当前的offset、total、limit、snapshot，其中由 offset 和 total 来决定是否还要加载更多
 *
 * TODO 加载状态 -- 即 isLoading
 * 本来预想的设计是：由界面用户的操作(action)触发开始；由一次请求的完成来结束，即数据状态的更新，来结束loading状态；
 * 但加载状态目前没有办法做到非常完备的描述，因为对于分页列表来说，
 * 加载状态有两种：refresh和loadMore，这两种状态由用户在界面上触发，并且有可能共存，即两种请求同时存在；
 * 两种加载状态同时存在时，当数据response返回后，对数据状态的更新也需要业务调用方来做一些逻辑判断
 * 此时对于加载状态的更新，也同时依赖于调用方的外部判断，这样更新逻辑的复杂度会飙升，对接口调用的理解成本太大
 */
public class KvoPageList<T> extends KvoSource {

    private static final String TAG = "KvoPageList";

    private static final int SNAPSHOT_CHANGE_STRATEGY = 0;  //snapshot变化时的处理策略

    public static final String kvo_datas = "datas";
    @KvoFieldAnnotation(name = kvo_datas)
    @NonNull
    public final KvoList<T> datas = new KvoList<>(this, kvo_datas);

    public static final String kvo_hasMore = "hasMore";
    @KvoFieldAnnotation(name = kvo_hasMore)
    public boolean hasMore = false;

    public static final String kvo_offset = "offset";
    @KvoFieldAnnotation(name = kvo_offset)
    public long offset = 0L;

    public static final String kvo_limit = "limit";
    @KvoFieldAnnotation(name = kvo_limit)
    public long limit = 0L;

    public static final String kvo_snapshot = "snapshot";
    @KvoFieldAnnotation(name = kvo_snapshot)
    public long snapshot = 0L;

    public static final String kvo_total = "total";
    @KvoFieldAnnotation(name = kvo_total)
    public long total = 0L;

    @MainThread
    public void reset() {
        datas.clear();

        setValue(kvo_hasMore, false);
        setValue(kvo_limit, 0L);
        setValue(kvo_offset, 0L);
        setValue(kvo_total, 0L);
        setValue(kvo_snapshot, 0L);
    }

    //协议分页设计：https://git.duowan.com/wuerping1/share/blob/master/page-snapshot.md
    //
    //由于服务器对此种分页的结构设计，所以做各种判断时，千万不能用list的size来进行判断或者比较，这个是最不准的
    //
    // reqOffset 是请求时的offset，不一定和当前的offset相同，因为在请求时，数据的状态还没有变
    @MainThread
    public void combineList(List<T> resList, @Nullable Page reqPage, @NonNull Page resPage) {
        long reqOffset = 0;
        if (reqPage != null) {
            reqOffset = reqPage.offset;
        }
        combineList(resList, resPage.snap, resPage.limit, resPage.total, reqOffset, resPage.offset);
    }

    @MainThread
    public void combineList(List<T> resList, long resSnapshot, long resLimit,
                            long resTotal, long reqOffset, long resOffset) {
        MLog.debug(TAG, "current list size: " + datas.size() + ", snapshot: " +
                this.snapshot + ", offset: " + this.offset + ", limit: " + this.limit +
                ", total: " + this.total);
        MLog.debug(TAG, "new list size: " + resList.size() + ", snapshot: " +
                resSnapshot + ", startOffset: " + reqOffset +  ", newOffset: " + resOffset +
                ", limit: " + resLimit + ", total: " + resTotal);

        // 代表是refresh操作
        if (reqOffset == 0) {
            datas.set(resList);

            setValue(kvo_limit, resLimit);
            setValue(kvo_snapshot, resSnapshot);
            setValue(kvo_total, resTotal);
            setValue(kvo_offset, resOffset);

            setValue(kvo_hasMore, resTotal > resOffset);

            return;
        }

        if (this.snapshot == resSnapshot) {
            // 同一个快照下的处理
            if (this.offset <= resOffset) {
                //关于limit的问题，本来limit是实际返回回来的数量，但是现在有些协议把传过去的limit直接返回回来了
                //例如最后一页只有7个，但是请求的limit是10，本来limit应该返回7，但是返回了10
                //所以这里用了 >= 来判断
                if (this.offset + resLimit >= resOffset || this.offset + resLimit >= resTotal) {
                    datas.addAll(resList);

                    setValue(kvo_limit, resLimit);
                    setValue(kvo_total, resTotal);
                    setValue(kvo_offset, resOffset);

                    setValue(kvo_hasMore, resTotal > resOffset);
                } else {
                    //对于不连续的分页，放弃此次数据
                    MLog.error(TAG, "no continue datas in page list");
                }
            } else {
                MLog.error(TAG, "local offset is bigger than remote");
            }

            return;
        }

        MLog.error(TAG, "old snapshot is expired");

        //  快照失效的情况，也可以有不同的策略
        //  1、对于严格排序列表，比如榜单，这里要清空原数据，然后重新请求第一页，
        //  2、如果是帖子列表这种，是允许重复的，就可以拼接上；
        if (SNAPSHOT_CHANGE_STRATEGY == 0) {
            datas.set(resList);
        } else {
            datas.addAll(resList);
        }

        setValue(kvo_snapshot, resSnapshot);
        setValue(kvo_limit, resLimit);
        setValue(kvo_total, resTotal);
        setValue(kvo_offset, resOffset);

        setValue(kvo_hasMore, resTotal > resOffset);
    }
}
