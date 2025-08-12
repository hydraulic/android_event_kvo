package com.yy.base.event.kvo.list;

import androidx.annotation.NonNull;

import com.yy.base.event.kvo.KvoSource;
import com.yy.base.event.kvo.list.KvoListHelper.NSRange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by Hydra.
 * <p>
 * kvoList的使用要有个注意的地方：
 * 我们大多数的列表都是给界面用的，给界面用的数据，只能在主线程更新
 * 但是有个点，就是绑定时的那一次通知是无法控制的，所以KvoList如果是给界面使用，一定要在主线程绑定
 *
 * 本来KvoList的通知 通常 来说需要两个限制：
 * 1、在主线程
 * 2、同步通知
 *
 * 目前在框架层已经对这两个点做了限制，同步通知的特殊处理在这几个地方：
 * 1、KvoHelper里加入了一个 强制同步更新 的标志位，作为Kvo的EventAction
 * 3、在KvoEventReceiver里，判断了这个标志位，如果是带有 强制同步更新 标志位的Action，会强制同步更新
 */
public class KvoList<T> implements List<T> {

    private List<T> mList;

    private final KvoSource mSource;

    private final String mName;

    public KvoList(KvoSource source, String name) {
        this(source, name, null);
    }

    /**
     * @param list 默认是ArrayList，不会对传入的list做深拷贝
     */
    public KvoList(KvoSource source, String name, List<T> list) {
        mSource = source;
        mName = name;

        mList = (list == null) ? new ArrayList<>() : list;
    }

    /**
     * 不会对传入的list做深拷贝，考虑到wrapped list可能会用不同类型的list
     */
    public void setWrappedList(List<T> list) {
        mList = list;

        KvoListHelper.notifyReload(mSource, mName, this);
    }

    public List<T> wrappedList() {
        return mList;
    }

    public String name() {
        return mName;
    }

    public KvoSource source() {
        return mSource;
    }

    @Override
    public void add(int location, T object) {
        mList.add(location, object);

        KvoListHelper.notifyInsert(mSource, mName, this, location, 1);
    }

    @Override
    public boolean add(T object) {
        int oldSize = size();

        boolean result = mList.add(object);

        if (result) {
            KvoListHelper.notifyInsert(mSource, mName, this, oldSize, 1);
        }

        return result;
    }

    @Override
    public boolean addAll(int location, Collection<? extends T> collection) {
        boolean result = mList.addAll(location, collection);

        if (result) {
            KvoListHelper.notifyInsert(mSource, mName, this, location, collection.size());
        }

        return result;
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        int oldSize = size();

        boolean result = mList.addAll(collection);

        if (result) {
            KvoListHelper.notifyInsert(mSource, mName, this, oldSize, collection.size());
        }

        return result;
    }

    @Override
    public void clear() {
        int preSize = size();

        mList.clear();

        KvoListHelper.notifyRangeRemove(mSource, mName, this, 0, preSize);
    }

    @Override
    public T remove(int location) {
        T t = mList.remove(location);

        KvoListHelper.notifyRangeRemove(mSource, mName, this, location, 1);

        return t;
    }

    @Override
    public boolean remove(Object object) {
        int index = mList.indexOf(object);

        if (index >= 0) {
            mList.remove(index);

            KvoListHelper.notifyRangeRemove(mSource, mName, this, index, 1);
        }

        return index >= 0;
    }

    /**
     * 调用者自己判断index范围
     */
    public void removeRange(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > size() || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException("invalid index fromIndex: " +
                    fromIndex + " toIndex: " + toIndex);
        }

        mList.subList(fromIndex, toIndex).clear();

        KvoListHelper.notifyRangeRemove(mSource, mName, this, fromIndex,
                toIndex - fromIndex);
    }

    /**
     * 需要fromIndex
     * 非原子操作
     */
    public void rangeReplace(int fromIndex, int toIndex, @NonNull Collection<? extends T> collection) {
        if (fromIndex < 0 || toIndex > size() || fromIndex > toIndex ||
                toIndex + 1 - fromIndex != collection.size()) {
            throw new IndexOutOfBoundsException("invalid index fromIndex: " + fromIndex +
                    " toIndex: " + toIndex + " collectionSize : " + collection.size());
        }

        List<T> subList = mList.subList(fromIndex, toIndex);

        subList.clear();
        subList.addAll(collection);

        KvoListHelper.notifyRangeReplace(mSource, mName, this, fromIndex,
                toIndex - fromIndex);
    }

    /**
     * clear && addAll, 非原子操作
     */
    public void set(@NonNull Collection<? extends T> newCollection) {
        mList.clear();
        mList.addAll(newCollection);

        KvoListHelper.notifyReload(mSource, mName, this);
    }

    @Override
    public T set(int location, T object) {
        T t = mList.set(location, object);

        KvoListHelper.notifyRangeReplace(mSource, mName, this, location, 1);

        return t;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return doCollectionOperation(collection, true);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return doCollectionOperation(collection, false);
    }

    private boolean doCollectionOperation(Collection<?> collection, boolean positive) {
        boolean result = false;

        Iterator<T> it = mList.iterator();

        NSRange currentRange = null;
        int i = 0;
        int currentDeleteLength = 0;

        while (it.hasNext()) {
            T t = it.next();

            if (collection.contains(t) == positive) {
                it.remove();

                if (currentRange == null) {
                    currentRange = new NSRange(i, 1);
                } else {
                    currentRange.length++;
                }

                result = true;
            } else if (currentRange != null) {
                KvoListHelper.notifyRangeRemove(mSource, mName, this, currentRange.position -
                        currentDeleteLength, currentRange.length);

                currentDeleteLength += currentRange.length;

                currentRange = null;
            }

            i++;
        }

        if (currentRange != null) {
            KvoListHelper.notifyRangeRemove(mSource, mName, this, currentRange.position -
                    currentDeleteLength, currentRange.length);
        }

        return result;
    }

    /**
     * 调用者自己判断index范围
     * 非原子操作
     */
    public void move(int fromIndex, int toIndex) {
        if (fromIndex != toIndex) {
            T t = mList.remove(fromIndex);

            mList.add(toIndex, t);

            KvoListHelper.notifyMove(mSource, mName, this, fromIndex, toIndex);
        }
    }

    @Override
    public List<T> subList(int start, int end) {
        return mList.subList(start, end);
    }

    @Override
    public int size() {
        return mList.size();
    }

    @Override
    public Object[] toArray() {
        return mList.toArray();
    }

    @Override
    public <E> E[] toArray(E[] array) {
        return mList.toArray(array);
    }

    @Override
    public boolean isEmpty() {
        return mList.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        return new KvoListIterator<>(mSource, mName, this);
    }

    @Override
    public int lastIndexOf(Object object) {
        return mList.lastIndexOf(object);
    }

    @Override
    public ListIterator<T> listIterator() {
        return new KvoListIterator<>(mSource, mName, this);
    }

    @Override
    public ListIterator<T> listIterator(int location) {
        return new KvoListIterator<>(mSource, mName, this, location);
    }

    @Override
    public boolean contains(Object object) {
        return mList.contains(object);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return mList.containsAll(collection);
    }

    @Override
    public T get(int location) {
        return mList.get(location);
    }

    @Override
    public int indexOf(Object object) {
        return mList.indexOf(object);
    }
}
