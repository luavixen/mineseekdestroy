package dev.foxgirl.mineseekdestroy.util.collect;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;

public final class ImmutableMap<K, V> implements Map<K, V> {

    @SuppressWarnings("unchecked")
    public static <K, V> @NotNull ImmutableMap<K, V> of() {
        return (ImmutableMap<K, V>) EMPTY;
    }

    public static <K, V> @NotNull ImmutableMap<K, V> of(@NotNull Map<? extends K, ? extends V> map) {
        Objects.requireNonNull(map, "Argument 'map'");

        if (map.isEmpty()) {
            return of();
        } else {
            return new ImmutableMap<>(new AbstractCollection<>() {
                @Override
                public int size() {
                    return map.size();
                }

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return new Iterator<>() {
                        private final Iterator<? extends Map.Entry<? extends K, ? extends V>> iterator = map.entrySet().iterator();

                        @Override
                        public boolean hasNext() {
                            return iterator.hasNext();
                        }

                        @Override
                        public Entry<K, V> next() {
                            Map.Entry<? extends K, ? extends V> entry = iterator.next();
                            return new Entry<>(entry.getKey(), entry.getValue());
                        }
                    };
                }
            });
        }
    }

    public static final class Builder<K, V> extends ArrayBuilder<Entry<K, V>> {
        private Builder() {
            super(16);
        }

        private Builder(int capacity) {
            super(capacity);
        }

        public @NotNull ImmutableMap<K, V> build() {
            if (isEmpty()) {
                return of();
            } else {
                return new ImmutableMap<>(this);
            }
        }

        public @NotNull Builder<K, V> put(K key, V value) {
            add(new Entry<>(key, value));
            return this;
        }
    }

    public static <K, V> @NotNull Builder<K, V> builder() {
        return new Builder<>();
    }
    public static <K, V> @NotNull Builder<K, V> builder(int capacity) {
        return new Builder<>(capacity);
    }

    private static final class Entry<K, V> implements Map.Entry<K, V> {
        private final int hash;
        private final K key;
        private final V value;

        private Entry<K, V> next;

        private Entry(K key, V value) {
            this.hash = hash(key);
            this.key = key;
            this.value = value;
        }

        @Override public K getKey() { return key; }
        @Override public V getValue() { return value; }
        @Override public String toString() { return key + "=" + value; }

        @Override
        public V setValue(V newValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            return Objects.hash(key) ^ Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            return obj instanceof Map.Entry<?, ?> entry
                && Objects.equals(key, entry.getKey())
                && Objects.equals(value, entry.getValue());
        }

        private boolean matches(int hash, Object key) {
            return this.hash == hash && Objects.equals(this.key, key);
        }
    }

    private static <K, V> Entry<K, V> traverse(Entry<K, V> entry, int hash, Object key) {
        while (entry != null) {
            if (entry.matches(hash, key)) return entry;
            entry = entry.next;
        }
        return null;
    }

    private static final class KeyIterator<K, V> extends AbstractArrayIterator<Entry<K, V>, K> {
        private KeyIterator(Entry<K, V>[] elements) {
            super(elements, 0);
        }

        @Override public K next() { return getNext().getKey(); }
        @Override public K previous() { return getPrevious().getKey(); }
    }

    private static final class ValueIterator<K, V> extends AbstractArrayIterator<Entry<K, V>, V> {
        private ValueIterator(Entry<K, V>[] elements) {
            super(elements, 0);
        }

        @Override public V next() { return getNext().getValue(); }
        @Override public V previous() { return getPrevious().getValue(); }
    }

    private abstract class BaseSet<E> extends ImmutableCollection<E> implements Set<E> {
        @Override
        public final int size() {
            return ImmutableMap.this.size();
        }

        @Override
        public final boolean equals(Object other) {
            if (other == this) return true;

            if (!(other instanceof Set<?>)) return false;

            var collection = (Collection<?>) other;

            try {
                return size() == collection.size() && containsAll(collection);
            } catch (ClassCastException | NullPointerException ignored) {
                return false;
            }
        }

        @Override
        public final int hashCode() {
            int hash = 0;
            for (E element : this) {
                if (element != null) {
                    hash += element.hashCode();
                }
            }
            return hash;
        }
    }

    private final class EntrySet extends BaseSet<Map.Entry<K, V>> {
        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new ArrayIterator<>(elements, 0);
        }

        @Override
        public boolean contains(Object entry) {
            return entry instanceof Map.Entry<?, ?>
                && containsEntry((Map.Entry<?, ?>) entry);
        }

        @Override
        public Object[] toArray() {
            return elements.clone();
        }
        @Override
        public <T> T[] toArray(T[] array) {
            return toArray(array, elements);
        }
    }

    private final class KeySet extends BaseSet<K> {
        @Override
        public Iterator<K> iterator() {
            return new KeyIterator<>(elements);
        }

        @Override
        public boolean contains(Object key) {
            return containsKey(key);
        }
    }

    private static final class Values<K, V> extends ImmutableCollection<V> {
        private final Entry<K, V>[] values;

        private Values(Entry<K, V>[] values) {
            this.values = values;
        }

        @Override public int size() { return values.length; }
        @Override public Iterator<V> iterator() { return new ValueIterator<>(values); }
    }

    //
    // Since many common hashCode implementations produce values that are
    // suboptimal for use in hash tables, we use Fibonacci hashing (a kind of
    // multiplicative hashing) to scramble the result and provide good
    // distribution of hash values.
    // See: https://en.wikipedia.org/wiki/Hash_function#Fibonacci_hashing
    //
    // Note the use of the "shift" field instead of using the length of the
    // underlying "nodes" array directly with modulo. Since we are using
    // multiplicative hashing the upper bits of any given hash are very well
    // distributed while the lower bits may still be suboptimal, so by ensuring
    // the underlying array's length is a power of two and then applying
    // `hash >>> (32 - n)` where `nodes.length == 1 << n` we get great
    // distribution with little work.
    //

    private static final int CAPACITY_MIN = 4;
    private static final int CAPACITY_MAX = 1 << 30;

    static int shift(int capacity) {
        // Clamp capacity into the range 4..2^30
        if (capacity < CAPACITY_MIN) capacity = CAPACITY_MIN;
        else if (capacity > CAPACITY_MAX) capacity = CAPACITY_MAX;
        // Calculate the largest shift that will contain the requested capacity
        return Integer.numberOfLeadingZeros(capacity - 1);
    }

    static int hash(Object key) {
        // 0x9E3779B9 or 2654435769 is ⌊2^32/ϕ⌋, where ϕ is the golden ratio
        return key != null
            ? (key.hashCode() * 0x9E3779B9)
            : 0;
    }

    private final Entry<K, V>[] entries;
    private final int shift;

    private final Entry<K, V>[] elements;

    private transient EntrySet entrySet;
    private transient KeySet keySet;
    private transient Values<K, V> values;

    private static final ImmutableMap<?, ?> EMPTY = new ImmutableMap<>();

    private ImmutableMap() {
        this(ImmutableList.of());
    }

    @SuppressWarnings("unchecked")
    private ImmutableMap(Collection<Entry<K, V>> collection) {
        int count = collection.size();
        var elements = new ArrayBuilder<Entry<K, V>>(count);

        int shift = shift(count);
        var entries = (Entry<K, V>[]) new Entry[1 << (32 - shift)];

        for (Entry<K, V> entry : collection) {
            int hash = entry.hash;
            int index = hash >>> shift;

            var entryHead = entries[index];
            var entryMatching = traverse(entryHead, hash, entry.key);
            if (entryMatching != null) continue;

            entry.next = entryHead;
            entries[index] = entry;

            elements.add(entry);
        }

        this.entries = entries;
        this.shift = shift;

        this.elements = elements.collect(Entry[].class);
    }

    private Entry<K, V> findEntry(Object key) {
        int hash = hash(key);
        return traverse(entries[hash >>> shift], hash, key);
    }

    private Entry<K, V> findEntryValue(Object value) {
        for (var entry : elements) {
            if (Objects.equals(entry.value, value)) return entry;
        }
        return null;
    }

    private boolean containsEntry(Map.Entry<?, ?> entryOther) {
        var entryThis = findEntry(entryOther.getKey());
        return entryThis != null && Objects.equals(entryThis.value, entryOther.getValue());
    }

    @Override
    public int size() {
        return elements.length;
    }
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return findEntry(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        return findEntryValue(value) != null;
    }

    @Override
    public V get(Object key) {
        var entry = findEntry(key);
        return entry != null ? entry.value : null;
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        var entry = findEntry(key);
        return entry != null ? entry.value : defaultValue;
    }

    @Override
    public @NotNull Set<Map.@NotNull Entry<K, V>> entrySet() {
        var entrySet = this.entrySet;
        if (entrySet == null) {
            entrySet = this.entrySet = new EntrySet();
        }
        return entrySet;
    }
    @Override
    public @NotNull Set<K> keySet() {
        var keySet = this.keySet;
        if (keySet == null) {
            keySet = this.keySet = new KeySet();
        }
        return keySet;
    }
    @Override
    public @NotNull Collection<V> values() {
        var values = this.values;
        if (values == null) {
            values = this.values = new Values<>(elements);
        }
        return values;
    }

    @Override
    public @Nullable V put(K key, V value) {
        throw new UnsupportedOperationException();
    }
    @Override
    public @Nullable V remove(Object key) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        throw new UnsupportedOperationException();
    }
    @Override
    public @Nullable V putIfAbsent(K key, V value) {
        throw new UnsupportedOperationException();
    }
    @Override
    public boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException();
    }
    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        throw new UnsupportedOperationException();
    }
    @Override
    public @Nullable V replace(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        return entrySet().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof Map<?, ?> map) {
            if (map.size() != size()) return false;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!containsEntry(entry)) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        if (isEmpty()) return "{}";

        var iterator = new ArrayIterator<>(elements, 0);
        var builder = new StringBuilder();

        builder.append('{');
        for (;;) {
            Entry<K, V> entry = iterator.next();
            K key = entry.key;
            V value = entry.value;
            builder.append(key == this ? "<this>" : key);
            builder.append('=');
            builder.append(value == this ? "<this>" : value);
            if (iterator.hasNext()) {
                builder.append(',');
                builder.append(' ');
            } else {
                builder.append('}');
                return builder.toString();
            }
        }
    }

}
