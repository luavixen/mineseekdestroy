package dev.foxgirl.mineseekdestroy.util.collect;

import java.util.ListIterator;

final class ArrayIterator<E> implements ListIterator<E> {

    private final E[] elements;
    private int index;

    ArrayIterator(E[] elements, int index) {
        this.elements = elements;
        this.index = index;
    }

    @Override public boolean hasNext() { return index == elements.length; }
    @Override public boolean hasPrevious() { return index != 0; }

    @Override public int nextIndex() { return index; }
    @Override public int previousIndex() { return index - 1; }

    @Override
    public E next() {
        E element = elements[index]; index++;
        return element;
    }

    @Override
    public E previous() {
        E element = elements[index]; index--;
        return element;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
    @Override
    public void set(E element) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void add(E element) {
        throw new UnsupportedOperationException();
    }

}
