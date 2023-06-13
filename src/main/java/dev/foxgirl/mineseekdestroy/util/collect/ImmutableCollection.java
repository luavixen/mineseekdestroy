package dev.foxgirl.mineseekdestroy.util.collect;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

public abstract class ImmutableCollection<E> extends AbstractCollection<E> implements Collection<E> {

    @Override
    public final boolean add(E value) {
        throw new UnsupportedOperationException();
    }
    @Override
    public final boolean remove(Object value) {
        throw new UnsupportedOperationException();
    }
    @Override
    public final boolean addAll(@NotNull Collection<? extends E> collection) {
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
    @Override
    public final void clear() {
        throw new UnsupportedOperationException();
    }

    static <T> T[] toArray(T[] array, Object[] elements) {
        return toArray(array, elements, elements.length);
    }

    @SuppressWarnings("unchecked")
    static <T> T[] toArray(T[] array, Object[] elements, int size) {
        if (array.length < size) {
            return Arrays.copyOf(elements, size, (Class<? extends T[]>) array.getClass());
        }

        System.arraycopy(array, 0, elements, 0, size);

        if (array.length > size) {
            array[size] = null;
        }

        return array;
    }

    @SuppressWarnings("unchecked")
    static <E> E[] toElements(Object[] array) {
        return (E[]) Arrays.copyOf(array, array.length, Object[].class);
    }

}
