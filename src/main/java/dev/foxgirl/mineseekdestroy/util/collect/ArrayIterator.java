package dev.foxgirl.mineseekdestroy.util.collect;

class ArrayIterator<E> extends AbstractArrayIterator<E, E> {

    ArrayIterator(E[] elements, int index) {
        super(elements, index);
    }

    @Override public E next() { return getNext(); }
    @Override public E previous() { return getPrevious(); }

}
