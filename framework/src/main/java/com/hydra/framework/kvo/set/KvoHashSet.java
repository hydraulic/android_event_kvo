package com.hydra.framework.kvo.set;

import android.os.Build.VERSION_CODES;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.hydra.framework.kvo.KvoEventIntent;
import com.hydra.framework.kvo.KvoSource;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

public class KvoHashSet<E> implements Set<E> {

    private final HashSet<E> mHashSet;

    public final KvoSource source;
    public final String name;

    public KvoHashSet(KvoSource source, String name) {
        this.source = source;
        this.name = name;

        mHashSet = new HashSet<>();
    }

    public KvoHashSet(KvoSource source, String name, Collection<? extends E> c) {
        this.source = source;
        this.name = name;

        mHashSet = new HashSet<>(c);
    }

    public KvoHashSet(KvoSource source, String name, int initialCapacity, float loadFactor) {
        this.source = source;
        this.name = name;

        mHashSet = new HashSet<>(initialCapacity, loadFactor);
    }

    public KvoHashSet(KvoSource source, String name, int initialCapacity) {
        this.source = source;
        this.name = name;

        mHashSet = new HashSet<>(initialCapacity);
    }

    @Override
    public int size() {
        return mHashSet.size();
    }

    @Override
    public boolean isEmpty() {
        return mHashSet.isEmpty();
    }

    @Override
    public boolean contains(@Nullable Object o) {
        return mHashSet.contains(o);
    }

    @NonNull
    @Override
    public Iterator<E> iterator() {
        return new KvoHashSetIterator<>(mHashSet.iterator(), this);
    }

    @NonNull
    @Override
    public Object[] toArray() {
        return mHashSet.toArray();
    }

    @NonNull
    @Override
    public <T> T[] toArray(@NonNull T[] a) {
        return mHashSet.toArray(a);
    }

    @Override
    public boolean add(E e) {
        boolean result = mHashSet.add(e);

        notifyChange();

        return result;
    }

    @Override
    public boolean remove(@Nullable Object o) {
        boolean result = mHashSet.remove(o);

        notifyChange();

        return result;
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        return mHashSet.containsAll(c);
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends E> c) {
        boolean result = mHashSet.addAll(c);

        notifyChange();

        return result;
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        boolean result = mHashSet.retainAll(c);

        notifyChange();

        return result;
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        boolean result = mHashSet.removeAll(c);

        notifyChange();

        return result;
    }

    @Override
    public void clear() {
        mHashSet.clear();

        notifyChange();
    }

    @RequiresApi(api = VERSION_CODES.N)
    @NonNull
    @Override
    public Spliterator<E> spliterator() {
        return mHashSet.spliterator();
    }

    @RequiresApi(api = VERSION_CODES.N)
    @Override
    public void forEach(@NonNull Consumer<? super E> action) {
        mHashSet.forEach(action);
    }

    void notifyChange() {
        KvoEventIntent kvoEventIntent = KvoEventIntent.build(source, name);

        //TODO 这里没有把旧的值设置成null
        kvoEventIntent.setOldValue(this);
        kvoEventIntent.setNewValue(this);

        source.notifyEvent(kvoEventIntent);
    }
}
