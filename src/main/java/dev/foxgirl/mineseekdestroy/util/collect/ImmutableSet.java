package dev.foxgirl.mineseekdestroy.util.collect;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class ImmutableSet<E> extends ImmutableCollection<E> implements Set<E> {

    @SuppressWarnings("unchecked")
    public static <E> @NotNull ImmutableSet<E> of() {
        return (ImmutableSet<E>) EMPTY;
    }

    @SafeVarargs
    public static <E> @NotNull ImmutableSet<E> of(E @NotNull ... elements) {
        Objects.requireNonNull(elements, "Argument 'elements'");

        if (elements.length == 0) {
            return of();
        } else {
            return new ImmutableSet<>(ImmutableList.wrap(elements));
        }
    }

    public static <E> @NotNull ImmutableSet<E> of(@NotNull Collection<? extends E> collection) {
        Objects.requireNonNull(collection, "Argument 'collection'");

        if (collection.isEmpty()) {
            return of();
        } else {
            return new ImmutableSet<>(collection);
        }
    }

    public static <E> @NotNull ImmutableSet<E> copyOf(E @NotNull [] elements) {
        return of(elements);
    }
    public static <E> @NotNull ImmutableSet<E> copyOf(@NotNull Collection<? extends E> collection) {
        return of(collection);
    }

    public static final class Builder<E> extends ArrayBuilder<E> {
        private Builder() {
            super(16);
        }

        private Builder(int capacity) {
            super(capacity);
        }

        public @NotNull ImmutableSet<E> build() {
            return of(this);
        }

        public @NotNull Builder<E> put(E element) {
            add(element);
            return this;
        }
    }

    public static <E> @NotNull Builder<E> builder() {
        return new Builder<>();
    }
    public static <E> @NotNull Builder<E> builder(int capacity) {
        return new Builder<>(capacity);
    }

    private static final class Entry<E> {
        private final int hash;
        private final E value;

        private final Entry<E> next;

        private Entry(int hash, E value, Entry<E> next) {
            this.hash = hash;
            this.value = value;
            this.next = next;
        }

        private boolean matches(int hash, Object value) {
            return this.hash == hash && Objects.equals(this.value, value);
        }
    }

    private static <E> Entry<E> traverse(Entry<E> entry, int hash, Object value) {
        while (entry != null) {
            if (entry.matches(hash, value)) return entry;
            entry = entry.next;
        }
        return null;
    }

    private static final ImmutableSet<?> EMPTY = new ImmutableSet<>();

    private final Entry<E>[] entries;
    private final int shift;

    private final E[] elements;

    private ImmutableSet() {
        this(ImmutableList.of());
    }

    @SuppressWarnings("unchecked")
    private ImmutableSet(Collection<? extends E> collection) {
        int count = collection.size();
        var elements = new ArrayBuilder<E>(count);

        int shift = ImmutableMap.shift(count);
        var entries = (Entry<E>[]) new Entry[1 << (32 - shift)];

        for (E element : collection) {
            int hash = ImmutableMap.hash(element);
            int index = hash >>> shift;

            var entryHead = entries[index];
            var entryMatching = traverse(entryHead, hash, element);
            if (entryMatching != null) continue;

            entries[index] = new Entry<>(hash, element, entryHead);

            elements.add(element);
        }

        this.entries = entries;
        this.shift = shift;

        this.elements = elements.collect();
    }

    @Override
    public int size() {
        return elements.length;
    }

    @Override
    public boolean contains(Object value) {
        int hash = ImmutableMap.hash(value);
        var entry = traverse(entries[hash >>> shift], hash, value);
        return entry != null;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> collection) {
        for (Object value : collection) {
            if (!contains(value)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NotNull Iterator<E> iterator() {
        return new ArrayIterator<>(elements, 0);
    }

    @Override
    public Object @NotNull [] toArray() {
        return elements.clone();
    }

    @Override
    public <T> T @NotNull [] toArray(T @NotNull [] array) {
        return toArray(array, elements);
    }

    @Override
    public boolean equals(Object other) {
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
    public int hashCode() {
        int hash = 0;
        for (E element : elements) {
            if (element != null) {
                hash += element.hashCode();
            }
        }
        return hash;
    }

    @Override
    public String toString() {
        return Arrays.toString(elements);
    }

}
