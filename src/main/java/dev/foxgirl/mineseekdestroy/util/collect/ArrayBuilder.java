package dev.foxgirl.mineseekdestroy.util.collect;

import java.util.Arrays;

class ArrayBuilder<E> {

    private E[] elements;
    private int size;

    @SuppressWarnings("unchecked")
    ArrayBuilder(int capacity) {
        this.elements = (E[]) new Object[capacity];
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    void add(E element) {
        var elements = this.elements;
        if (elements.length <= size) {
            elements = this.elements = Arrays.copyOf(elements, Math.max(elements.length << 1, 8));
        }
        elements[size++] = element;
    }

    final E[] collect() {
        return elements.length == size ? elements : Arrays.copyOf(elements, size);
    }

    final <T> T[] collect(Class<T[]> clazz) {
        return Arrays.copyOf(elements, size, clazz);
    }

}
