package com.hydra.framework.kvo.map;

import android.os.Build.VERSION_CODES;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

class KvoHashEntrySet<K, V> extends AbstractSet<Entry<K, V>> {

    private final Set<Entry<K,V>> mSet;
    private final KvoHashMap<K, V> mKvoHashMap;

    public KvoHashEntrySet(Set<Entry<K,V>> set, KvoHashMap<K, V> kvoHashMap) {
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
    public Iterator<Entry<K, V>> iterator() {
        return new KvoHashEntryIterator<>(mSet.iterator(), mKvoHashMap);
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
    public Spliterator<Entry<K, V>> spliterator() {
        return mSet.spliterator();
    }

    @RequiresApi(api = VERSION_CODES.N)
    @Override
    public void forEach(@NonNull Consumer<? super Entry<K, V>> action) {
        mSet.forEach(action);
    }
}
