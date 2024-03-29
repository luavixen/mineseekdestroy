package dev.foxgirl.mineseekdestroy.util.collect;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

class ArrayBuilder<E> extends AbstractCollection<E> {

    private E[] elements;
    private int size;

    @SuppressWarnings("unchecked")
    ArrayBuilder(int capacity) {
        this.elements = (E[]) new Object[capacity];
    }

    final E[] collect() {
        return elements.length == size ? elements : Arrays.copyOf(elements, size);
    }

    final <T> T[] collect(Class<T[]> clazz) {
        return Arrays.copyOf(elements, size, clazz);
    }

    @Override
    public final int size() {
        return size;
    }

    private void ensureCapacityFor(int needed) {
        int length = elements.length;
        int free = length - size;
        if (free < needed) {
            int lengthMinimum = free + needed;
            int lengthDoubled = Math.max(length << 1, 8);
            this.elements = Arrays.copyOf(elements, Math.max(lengthMinimum, lengthDoubled));
        }
    }

    @Override
    public final boolean add(E element) {
        ensureCapacityFor(1);
        elements[size++] = element;
        return true;
    }

    @Override
    public final boolean addAll(@NotNull Collection<? extends E> collection) {
        var iterator = collection.iterator();
        if (iterator.hasNext()) {
            ensureCapacityFor(collection.size());
            do { add(iterator.next()); } while (iterator.hasNext());
            return true;
        }
        return false;
    }

    @Override
    public final @NotNull Iterator<E> iterator() {
        return new ArrayIterator<>(elements, size, 0);
    }

    @Override
    public final Object @NotNull [] toArray() {
        return Arrays.copyOf(elements, size);
    }
    @Override
    public final <T> T @NotNull [] toArray(T @NotNull [] array) {
        return ImmutableCollection.toArray(array, elements, size);
    }

    @Override
    public final void clear() {
        throw new UnsupportedOperationException();
    }
    @Override
    public final boolean remove(Object value) {
        throw new UnsupportedOperationException();
    }
    @Override
    public final boolean removeAll(@NotNull Collection<?> collection) {
        throw new UnsupportedOperationException();
    }
    @Override
    public final boolean removeIf(@NotNull Predicate<? super E> filter) {
        throw new UnsupportedOperationException();
    }
    @Override
    public final boolean retainAll(@NotNull Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

}
