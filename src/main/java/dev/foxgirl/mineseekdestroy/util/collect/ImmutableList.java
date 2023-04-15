package dev.foxgirl.mineseekdestroy.util.collect;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.function.UnaryOperator;

public final class ImmutableList<E> extends ImmutableCollection<E> implements List<E>, RandomAccess, Serializable {

    @SuppressWarnings("unchecked")
    public static <E> ImmutableList<E> of() {
        return (ImmutableList<E>) EMPTY_LIST;
    }

    @SuppressWarnings("unchecked")
    public static <E> ImmutableList<E> of(E... elements) {
        if (elements.length == 0) {
            return (ImmutableList<E>) EMPTY_LIST;
        } else {
            return new ImmutableList<>(toElements(elements));
        }
    }

    @SuppressWarnings("unchecked")
    public static <E> ImmutableList<E> of(Collection<? extends E> collection) {
        if (collection.isEmpty()) {
            return (ImmutableList<E>) EMPTY_LIST;
        } else {
            return new ImmutableList<>(toElements(collection.toArray()));
        }
    }

    public static <E> ImmutableList<E> copyOf(E[] elements) {
        return of(elements);
    }
    public static <E> ImmutableList<E> copyOf(Collection<? extends E> collection) {
        return of(collection);
    }

    @SuppressWarnings("unchecked")
    public static <E> ImmutableList<E> wrap(E[] elements) {
        if (elements.length == 0) {
            return (ImmutableList<E>) EMPTY_LIST;
        } else {
            return new ImmutableList<>(elements);
        }
    }

    public static final class Builder<E> extends ArrayBuilder<E> {

        public Builder() {
            super(10);
        }

        public Builder(int capacity) {
            super(capacity);
        }

        public ImmutableList<E> build() {
            return ImmutableList.wrap(collect());
        }

        @Override
        public void add(E element) {
            super.add(element);
        }

    }

    public static <E> Builder<E> builder() {
        return new Builder<>();
    }
    public static <E> Builder<E> builder(int capacity) {
        return new Builder<>(capacity);
    }

    @Serial
    private static final long serialVersionUID = 137674928028660L;

    private static final Object[] EMPTY_ARRAY = new Object[0];
    private static final ImmutableList<?> EMPTY_LIST = new ImmutableList<>(EMPTY_ARRAY);

    private final E[] elements;

    private ImmutableList(E[] elements) {
        this.elements = elements;
    }

    @Override
    public Object @NotNull [] toArray() {
        return elements.clone();
    }

    @Override
    public <T> T @NotNull [] toArray(T @NotNull [] array) {
        return toArray(array, elements);
    }

    @Override public int size() { return elements.length; }
    @Override public boolean isEmpty() { return elements.length == 0; }

    @Override
    public E get(int index) {
        return elements[index];
    }

    @Override
    public boolean contains(Object value) {
        for (Object element : elements) {
            if (Objects.equals(element, value)) {
                return true;
            }
        }
        return false;
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
    public int indexOf(Object value) {
        for (int i = 0, size = size(); i < size; i++) {
            if (Objects.equals(elements[i], value)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object value) {
        for (int i = size() - 1; i >= 0; i--) {
            if (Objects.equals(elements[i], value)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public @NotNull Iterator<E> iterator() {
        return listIterator();
    }

    @Override
    public @NotNull ListIterator<E> listIterator() {
        return new ArrayIterator<>(elements, 0);
    }

    @Override
    public @NotNull ListIterator<E> listIterator(int index) {
        if (index < 0 || index > size()) {
            throw new IndexOutOfBoundsException();
        }
        return new ArrayIterator<>(elements, index);
    }

    @Override
    public @NotNull List<E> subList(int from, int to) {
        return new ImmutableList<>(Arrays.copyOfRange(elements, from, to));
    }

    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }
    @Override
    public E remove(int index) {
        throw new UnsupportedOperationException();
    }
    @Override
    public boolean addAll(int index, @NotNull Collection<? extends E> collection) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void sort(Comparator<? super E> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;

        if (!(other instanceof List<?>)) return false;

        ListIterator<E> iter1 = listIterator();
        ListIterator<?> iter2 = ((List<?>) other).listIterator();

        while (iter1.hasNext() && iter2.hasNext()) {
            if (!Objects.equals(iter1.next(), iter2.hasNext())) {
                return false;
            }
        }

        return !(iter1.hasNext() || iter2.hasNext());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(elements);
    }

    @Override
    public String toString() {
        return Arrays.toString(elements);
    }

}
