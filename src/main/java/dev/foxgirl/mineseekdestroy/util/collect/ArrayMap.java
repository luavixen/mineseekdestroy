package dev.foxgirl.mineseekdestroy.util.collect;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class ArrayMap<K, V> implements Map<K, V> {

    private static final class Entry<K, V> implements Map.Entry<K, V> {
        private final K key;
        private V value;

        private Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override public K getKey() { return key; }
        @Override public V getValue() { return value; }
        @Override public String toString() { return key + "=" + value; }

        @Override
        public V setValue(V newValue) {
            V oldValue = this.value;
            this.value = newValue;
            return oldValue;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value) ^ Objects.hashCode(key);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            return obj instanceof Map.Entry<?, ?> entry
                && Objects.equals(key, entry.getKey())
                && Objects.equals(value, entry.getValue());
        }
    }

    private static abstract class WrappedIterator<K, V> {
        protected final Iterator<Entry<K, V>> delegate;

        private WrappedIterator(Iterator<Entry<K, V>> delegate) {
            this.delegate = delegate;
        }

        public boolean hasNext() { return delegate.hasNext(); }
        public void remove() { delegate.remove(); }
    }

    private static final class KeyIterator<K, V> extends WrappedIterator<K, V> implements Iterator<K> {
        private KeyIterator(Iterator<Entry<K, V>> delegate) {
            super(delegate);
        }

        @Override public K next() { return delegate.next().getKey(); }
    }

    private static final class ValueIterator<K, V> extends WrappedIterator<K, V> implements Iterator<V> {
        private ValueIterator(Iterator<Entry<K, V>> delegate) {
            super(delegate);
        }

        @Override public V next() { return delegate.next().getValue(); }
    }

    private final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public @NotNull Iterator<Map.Entry<K, V>> iterator() {
            return (Iterator) entries.iterator();
        }

        @Override public int size() { return ArrayMap.this.size(); }
        @Override public void clear() { ArrayMap.this.clear(); }
    }

    private final class KeySet extends AbstractSet<K> {
        @Override public @NotNull Iterator<K> iterator() { return new KeyIterator<>(entries.iterator()); }

        @Override public int size() { return ArrayMap.this.size(); }
        @Override public void clear() { ArrayMap.this.clear(); }
    }

    private final class Values extends AbstractCollection<V> {
        @Override public @NotNull Iterator<V> iterator() { return new ValueIterator<>(entries.iterator()); }

        @Override public int size() { return ArrayMap.this.size(); }
        @Override public void clear() { ArrayMap.this.clear(); }
    }

    private final ArrayList<Entry<K, V>> entries;

    public ArrayMap() {
        entries = new ArrayList<>();
    }

    public ArrayMap(int capacity) {
        entries = new ArrayList<>(capacity);
    }

    public ArrayMap(@NotNull Map<? extends K, ? extends V> map) {
        this(map.size());
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            putUnsafe(entry.getKey(), entry.getValue());
        }
    }

    private Entry<K, V> findByKey(Object key) {
        for (var entry : entries) {
            if (Objects.equals(entry.key, key)) return entry;
        }
        return null;
    }
    private Entry<K, V> findByValue(Object value) {
        for (var entry : entries) {
            if (Objects.equals(entry.value, value)) return entry;
        }
        return null;
    }

    public final void putUnsafe(K key, V value) {
        entries.add(new Entry<>(key, value));
    }

    @Override public boolean containsKey(Object key) { return findByKey(key) != null; }
    @Override public boolean containsValue(Object value) { return findByValue(value) != null; }

    @Override
    public V get(Object key) {
        var entry = findByKey(key);
        return entry != null ? entry.value : null;
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        var entry = findByKey(key);
        return entry != null ? entry.value : defaultValue;
    }

    @Override
    public V put(K key, V value) {
        var entry = findByKey(key);
        if (entry != null) {
            return entry.setValue(value);
        }
        putUnsafe(key, value);
        return null;
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public V remove(Object key) {
        var iter = entries.iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            if (Objects.equals(entry.key, key)) {
                iter.remove();
                return entry.value;
            }
        }
        return null;
    }

    @Override
    public boolean remove(Object key, Object value) {
        return entries.remove(new Entry<>(key, value));
    }

    @Override public int size() { return entries.size(); }
    @Override public boolean isEmpty() { return entries.isEmpty(); }

    @Override public void clear() { entries.clear(); }

    @Override public @NotNull Set<Map.@NotNull Entry<K, V>> entrySet() { return new EntrySet(); }
    @Override public @NotNull Set<K> keySet() { return new KeySet();  }
    @Override public @NotNull Collection<V> values() { return new Values(); }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof Map<?, ?> map) {
            if (map.size() != size()) return false;
            for (Map.Entry<?, ?> entry2 : map.entrySet()) {
                var entry1 = findByKey(entry2.getKey());
                if (entry1 == null || !Objects.equals(entry1.value, entry2.getValue())) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        if (isEmpty()) return "{}";

        var iter = entries.iterator();
        var builder = new StringBuilder();

        builder.append('{');
        for (;;) {
            Entry<K, V> entry = iter.next();
            K key = entry.key;
            V value = entry.value;
            builder.append(key == this ? "<this>" : key);
            builder.append('=');
            builder.append(value == this ? "<this>" : value);
            if (iter.hasNext()) {
                builder.append(',');
                builder.append(' ');
            } else {
                builder.append('}');
                return builder.toString();
            }
        }
    }
}
