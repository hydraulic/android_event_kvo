package com.yy.base.event.kvo.map;

import android.os.Build.VERSION_CODES;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.yy.base.event.kvo.KvoEventIntent;
import com.yy.base.event.kvo.KvoSource;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * KvoHashMap的通知没有像kvoList那样分的很清晰，因为HashMap的结构要比list要复杂许多；
 * 具体体现在：
 * 1、键值对的空值处理上，因为HashMap的key value都是允许空值的，并且空值会占据size，这样在变动时比较难以判断，
 *   这个key/value是空值，还是说没有这个键值对；
 * 2、size判断的不清晰，如对于putAll、retainAll、removeAll这种操作，hashmap并不是一个有序的集合，
 *   在判断变动范围和具体的变动值上会很难，如果封装一层额外操作，会对性能有较大影响，更不用说更复杂的compute操作；
 * 3、HashMap的使用比List要少的多，且List的几个操作，正好是对应了界面上RecyclerView的变更和局部刷新，
 *   但是HashMap一般并不是供界面列表使用，通知者或者接收者也一般并不关心具体的变更值；
 *
 *  综上所述，在HashMap的通知变更上，并未提供像KvoList如此精细的变更通知；
 *
 *  PS：如果想提供更加精细的变更通知，最好的方式就是把HashMap的源码copy出来，
 *  直接在源码层面加上变更通知的代码，而不是封装一层；
 */
public class KvoHashMap<K, V> implements Map<K, V> {

    private final HashMap<K, V> mHashMap;

    public final KvoSource source;
    public final String name;

    public KvoHashMap(KvoSource source, String name) {
        this.source = source;
        this.name = name;

        mHashMap = new HashMap<>();
    }

    public KvoHashMap(KvoSource source, String name, int initialCapacity) {
        this.source = source;
        this.name = name;

        mHashMap = new HashMap<>(initialCapacity);
    }

    public KvoHashMap(KvoSource source, String name, int initialCapacity, float loadFactor) {
        this.source = source;
        this.name = name;

        mHashMap = new HashMap<>(initialCapacity, loadFactor);
    }

    public KvoHashMap(KvoSource source, String name, Map<? extends K, ? extends V> m) {
        this.source = source;
        this.name = name;

        mHashMap = new HashMap<>(m);
    }

    @Override
    public int size() {
        return mHashMap.size();
    }

    @Override
    public boolean isEmpty() {
        return mHashMap.isEmpty();
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
        return mHashMap.containsKey(key);
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
        return mHashMap.containsValue(value);
    }

    @Nullable
    @Override
    public V get(@Nullable Object key) {
        return mHashMap.get(key);
    }

    @RequiresApi(api = VERSION_CODES.N)
    @Nullable
    @Override
    public V getOrDefault(@Nullable Object key, @Nullable V defaultValue) {
        return mHashMap.getOrDefault(key, defaultValue);
    }

    @RequiresApi(api = VERSION_CODES.N)
    @Override
    public void forEach(@NonNull BiConsumer<? super K, ? super V> action) {
        mHashMap.forEach(action);
    }

    @Override
    public void clear() {
        mHashMap.clear();

        notifyChange();
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        V oldValue = mHashMap.put(key, value);

        notifyChange();

        return oldValue;
    }

    @Nullable
    @Override
    public V remove(@Nullable Object key) {
        V value = mHashMap.remove(key);

        notifyChange();

        return value;
    }

    @RequiresApi(api = VERSION_CODES.N)
    @Override
    public boolean remove(@Nullable Object key, @Nullable Object value) {
        boolean success = mHashMap.remove(key, value);

        notifyChange();

        return success;
    }

    @RequiresApi(api = VERSION_CODES.N)
    @Nullable
    @Override
    public V replace(K key, V value) {
        V oldValue = mHashMap.replace(key, value);

        notifyChange();

        return oldValue;
    }

    @RequiresApi(api = VERSION_CODES.N)
    @Override
    public boolean replace(K key, @Nullable V oldValue, V newValue) {
        boolean success = mHashMap.replace(key, oldValue, newValue);

        notifyChange();

        return success;
    }

    @NonNull
    @Override
    public Set<K> keySet() {
        return new KvoHashKeySet<>(mHashMap.keySet(), this);
    }

    @NonNull
    @Override
    public Collection<V> values() {
        return mHashMap.values();
    }

    @NonNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return new KvoHashEntrySet<>(mHashMap.entrySet(), this);
    }

    @RequiresApi(api = VERSION_CODES.N)
    @Nullable
    @Override
    public V merge(K key, @NonNull V value, @NonNull BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        V result = mHashMap.merge(key, value, remappingFunction);

        notifyChange();

        return result;
    }

    @Override
    public void putAll(@NonNull Map<? extends K, ? extends V> m) {
        mHashMap.putAll(m);

        notifyChange();
    }

    @RequiresApi(api = VERSION_CODES.N)
    @Override
    public void replaceAll(@NonNull BiFunction<? super K, ? super V, ? extends V> function) {
        mHashMap.replaceAll(function);

        notifyChange();
    }

    @RequiresApi(api = VERSION_CODES.N)
    @Nullable
    @Override
    public V putIfAbsent(K key, V value) {
        V oldValue = mHashMap.putIfAbsent(key, value);

        notifyChange();

        return oldValue;
    }

    @RequiresApi(api = VERSION_CODES.N)
    @Nullable
    @Override
    public V computeIfAbsent(K key, @NonNull Function<? super K, ? extends V> mappingFunction) {
        V value = mHashMap.computeIfAbsent(key, mappingFunction);

        notifyChange();

        return value;
    }

    @RequiresApi(api = VERSION_CODES.N)
    @Nullable
    @Override
    public V computeIfPresent(K key, @NonNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        V value = mHashMap.computeIfPresent(key, remappingFunction);

        notifyChange();

        return value;
    }

    @RequiresApi(api = VERSION_CODES.N)
    @Nullable
    @Override
    public V compute(K key, @NonNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        V value = mHashMap.compute(key, remappingFunction);

        notifyChange();

        return value;
    }

    void notifyChange() {
        KvoEventIntent kvoEventIntent = KvoEventIntent.build(source, name);

        //TODO 这里没有把旧的值设置成null
        kvoEventIntent.setOldValue(this);
        kvoEventIntent.setNewValue(this);

        source.notifyEvent(kvoEventIntent);
    }
}
