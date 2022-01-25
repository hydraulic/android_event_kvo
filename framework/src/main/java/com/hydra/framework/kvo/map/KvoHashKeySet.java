package com.hydra.framework.kvo.map;

import android.os.Build.VERSION_CODES;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

class KvoHashKeySet<K> extends AbstractSet<K> {

    private final Set<K> mSet;
    private final KvoHashMap<K, Object> mKvoHashMap;

    public KvoHashKeySet(Set<K> set, KvoHashMap kvoHashMap) {
        mKvoHashMap = kvoHashMap;
        mSet = set;
    }

    @Override
    public int size() {
        return mSet.size();
    }

    @Override
    public void clear() {
        mSet.clear();

        mKvoHashMap.notifyChange();
    }

    @NonNull
    @Override
    public Iterator<K> iterator() {
        return new KvoHashKeyIterator<>(mSet.iterator(), mKvoHashMap);
    }

    @Override
    public boolean contains(@Nullable Object o) {
        return mSet.contains(o);
    }

    @Override
    public boolean remove(@Nullable Object o) {
        boolean result = mSet.remove(o);

        mKvoHashMap.notifyChange();

        return result;
    }

    @RequiresApi(api = VERSION_CODES.N)
    @NonNull
    @Override
    public Spliterator<K> spliterator() {
        return mSet.spliterator();
    }

    @RequiresApi(api = VERSION_CODES.N)
    @Override
    public void forEach(@NonNull Consumer<? super K> action) {
        mSet.forEach(action);
    }
}
