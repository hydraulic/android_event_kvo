package com.hydra.framework.kvo.map;

import android.os.Build.VERSION_CODES;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.Iterator;
import java.util.function.Consumer;

@SuppressWarnings("rawtypes")
class KvoHashKeyIterator<T> implements Iterator<T> {

    private final Iterator<T> mIterator;
    private final KvoHashMap mKvoHashMap;

    public KvoHashKeyIterator(Iterator<T> iterator, KvoHashMap kvoHashMap) {
        mIterator = iterator;
        mKvoHashMap = kvoHashMap;
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

        mKvoHashMap.notifyChange();
    }

    @RequiresApi(api = VERSION_CODES.N)
    @Override
    public void forEachRemaining(@NonNull Consumer<? super T> action) {
        mIterator.forEachRemaining(action);
    }
}
