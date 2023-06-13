package dev.foxgirl.mineseekdestroy.util.collect;

final class ArrayIterator<E> extends AbstractArrayIterator<E, E> {

    ArrayIterator(E[] elements, int index) {
        super(elements, index);
    }
    ArrayIterator(E[] elements, int length, int index) {
        super(elements, length, index);
    }

    @Override public E next() { return getNext(); }
    @Override public E previous() { return getPrevious(); }

}
