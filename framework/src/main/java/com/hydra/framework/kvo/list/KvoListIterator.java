package com.hydra.framework.kvo.list;

import com.hydra.framework.kvo.KvoSource;

import java.util.ListIterator;

public class KvoListIterator<T> implements ListIterator<T> {

    private final KvoSource mKvoSource;
    private final String mName;
    private final ListIterator<T> mIterator;
    private final KvoList<T> mKvoList;

    private int mLastOperation = 0;

    public KvoListIterator(KvoSource kvoSource, String name, KvoList<T> list) {
        this(kvoSource, name, list, 0);
    }

    public KvoListIterator(KvoSource kvoSource, String name, KvoList<T> list, int index) {
        this.mKvoSource = kvoSource;
        this.mKvoList = list;
        this.mIterator = list.wrappedList().listIterator(index);
        this.mName = name;
    }

    @Override
    public boolean hasNext() {
        return mIterator.hasNext();
    }

    @Override
    public T next() {
        mLastOperation = 1;

        return mIterator.next();
    }

    @Override
    public boolean hasPrevious() {
        return mIterator.hasPrevious();
    }

    @Override
    public T previous() {
        mLastOperation = 2;

        return mIterator.previous();
    }

    @Override
    public int nextIndex() {
        return mIterator.nextIndex();
    }

    @Override
    public int previousIndex() {
        return mIterator.previousIndex();
    }

    @Override
    public void remove() {
        mIterator.remove();

        KvoListHelper.notifyRangeRemove(mKvoSource, mName, mKvoList, mIterator.nextIndex(), 1);
    }

    @Override
    public void set(T t) {
        mIterator.set(t);

        KvoListHelper.notifyRangeReplace(mKvoSource, mName, mKvoList, mLastOperation == 1 ?
                mIterator.previousIndex() : mIterator.nextIndex(), 1);
    }

    @Override
    public void add(T t) {
        mIterator.add(t);

        KvoListHelper.notifyInsert(mKvoSource, mName, mKvoList, mIterator.previousIndex(), 1);
    }
}
