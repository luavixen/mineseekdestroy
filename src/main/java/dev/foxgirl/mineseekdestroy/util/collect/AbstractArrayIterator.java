package dev.foxgirl.mineseekdestroy.util.collect;

import java.util.ListIterator;

abstract class AbstractArrayIterator<E, T> implements ListIterator<T> {

    private final E[] elements;
    private int index;

    protected AbstractArrayIterator(E[] elements, int index) {
        this.elements = elements;
        this.index = index;
    }

    @Override public boolean hasNext() { return index != elements.length; }
    @Override public boolean hasPrevious() { return index != 0; }

    @Override public int nextIndex() { return index; }
    @Override public int previousIndex() { return index - 1; }

    protected E getNext() {
        E element = elements[index]; index++;
        return element;
    }
    protected E getPrevious() {
        E element = elements[index]; index--;
        return element;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
    @Override
    public void set(T element) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void add(T element) {
        throw new UnsupportedOperationException();
    }

}
