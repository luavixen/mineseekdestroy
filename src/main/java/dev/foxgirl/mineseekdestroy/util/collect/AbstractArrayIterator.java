package dev.foxgirl.mineseekdestroy.util.collect;

import java.util.ListIterator;

abstract class AbstractArrayIterator<E, T> implements ListIterator<T> {

    private final E[] elements;
    private final int length;
    private int index;

    protected AbstractArrayIterator(E[] elements, int index) {
        this.elements = elements;
        this.length = elements.length;
        this.index = index;
    }
    protected AbstractArrayIterator(E[] elements, int length, int index) {
        this.elements = elements;
        this.length = length;
        this.index = index;
    }

    @Override public final boolean hasNext() { return index != length; }
    @Override public final boolean hasPrevious() { return index != 0; }

    @Override public final int nextIndex() { return index; }
    @Override public final int previousIndex() { return index - 1; }

    protected final E getNext() {
        E element = elements[index]; index++;
        return element;
    }
    protected final E getPrevious() {
        E element = elements[index]; index--;
        return element;
    }

    @Override
    public final void remove() {
        throw new UnsupportedOperationException();
    }
    @Override
    public final void set(T element) {
        throw new UnsupportedOperationException();
    }
    @Override
    public final void add(T element) {
        throw new UnsupportedOperationException();
    }

}
