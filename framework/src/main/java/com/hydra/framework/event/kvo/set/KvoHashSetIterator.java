package com.yy.base.event.kvo.set;

import android.os.Build.VERSION_CODES;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.Iterator;
import java.util.function.Consumer;

class KvoHashSetIterator<T> implements Iterator<T> {

    private final Iterator<T> mIterator;
    private final KvoHashSet<T> mKvoHashSet;

    public KvoHashSetIterator(Iterator<T> iterator, KvoHashSet<T> kvoHashSet) {
        mIterator = iterator;
        mKvoHashSet = kvoHashSet;
    }

    @Override
    public boolean hasNext() {
        return mIterator.hasNext();
    }

    @Override
    public T next() {
        return mIterator.next();
    }

    @Override
    public void remove() {
        mIterator.remove();

        mKvoHashSet.notifyChange();
    }

    @RequiresApi(api = VERSION_CODES.N)
    @Override
    public void forEachRemaining(@NonNull Consumer<? super T> action) {
        mIterator.forEachRemaining(action);
    }
}
