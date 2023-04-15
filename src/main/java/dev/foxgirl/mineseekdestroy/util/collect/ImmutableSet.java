package dev.foxgirl.mineseekdestroy.util.collect;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public final class ImmutableSet<E> extends ImmutableCollection<E> implements Set<E>, Serializable {

    @SuppressWarnings("unchecked")
    public static <E> ImmutableSet<E> of() {
        return (ImmutableSet<E>) EMPTY;
    }

    @SuppressWarnings("unchecked")
    public static <E> ImmutableSet<E> of(E... elements) {
        if (elements.length == 0) {
            return (ImmutableSet<E>) EMPTY;
        } else {
            return new ImmutableSet<>(ImmutableList.wrap(elements));
        }
    }

    @SuppressWarnings("unchecked")
    public static <E> ImmutableSet<E> of(Collection<? extends E> collection) {
        if (collection.isEmpty()) {
            return (ImmutableSet<E>) EMPTY;
        } else {
            return new ImmutableSet<>(collection);
        }
    }

    public static <E> ImmutableSet<E> copyOf(E[] elements) {
        return of(elements);
    }
    public static <E> ImmutableSet<E> copyOf(Collection<? extends E> collection) {
        return of(collection);
    }

    public static final class Builder<E> extends ArrayBuilder<E> {

        public Builder() {
            super(16);
        }

        public Builder(int capacity) {
            super(capacity);
        }

        public ImmutableSet<E> build() {
            return of(collect());
        }

        @Override
        public void add(E element) {
            super.add(element);
        }

    }

    public static <E> ImmutableList.Builder<E> builder() {
        return new ImmutableList.Builder<>();
    }
    public static <E> ImmutableList.Builder<E> builder(int capacity) {
        return new ImmutableList.Builder<>(capacity);
    }

    @Serial
    private static final long serialVersionUID = 2913121814384335849L;

    private static final class Node<E> {

        private final E value;
        private final int hash;

        private final Node<E> next;

        private Node(E value, int hash, Node<E> next) {
            this.value = value;
            this.hash = hash;
            this.next = next;
        }

        private boolean matches(Object value, int hash) {
            return this.hash == hash && Objects.equals(this.value, value);
        }

    }

    private static <E> Node<E> traverse(Node<E> node, Object value, int hash) {
        while (node != null) {
            if (node.matches(value, hash)) return node;
            node = node.next;
        }
        return null;
    }

    //
    // Since many common hashCode implementations produce values that are
    // suboptimal for use in hash tables, we use Fibonacci hashing (a kind of
    // multiplicative hashing) to scramble the result and provide good
    // distribution of hash values.
    // See: https://en.wikipedia.org/wiki/Hash_function#Fibonacci_hashing
    //
    // Note the use of the "shift" field instead of using the length of the
    // underlying "nodes" array directly with modulo. Since we are using
    // multiplicative hashing the upper bits of any given hash are very well
    // distributed while the lower bits may still be suboptimal, so by ensuring
    // the underlying array's length is a power of two and then applying
    // `hash >>> (32 - n)` where `nodes.length == 1 << n` we get great
    // distribution with little work.
    //

    private static final int CAPACITY_MIN = 4;
    private static final int CAPACITY_MAX = 1 << 30;

    private static int shift(int capacity) {
        // Clamp capacity into the range 4..2^30
        if (capacity < CAPACITY_MIN) capacity = CAPACITY_MIN;
        else if (capacity > CAPACITY_MAX) capacity = CAPACITY_MAX;
        // Calculate the largest shift that will contain the requested capacity
        return Integer.numberOfLeadingZeros(capacity - 1);
    }

    private static int hash(Object key) {
        // 0x9E3779B9 or 2654435769 is ⌊2^32/ϕ⌋, where ϕ is the golden ratio
        return key != null
            ? (key.hashCode() * 0x9E3779B9)
            : 0;
    }

    private static final ImmutableSet<?> EMPTY = new ImmutableSet<>();

    private final Node<E>[] nodes;
    private final E[] elements;
    private final int shift;

    @SuppressWarnings("unchecked")
    private ImmutableSet() {
        this.nodes =
            (Node<E>[]) new Node[0];
        this.elements =
            (E[]) new Object[0];
        this.shift = 33;
    }

    @SuppressWarnings("unchecked")
    private ImmutableSet(Collection<? extends E> collection) {
        int count = collection.size();
        var elements = new ArrayBuilder<E>(count);

        int shift = shift(count);
        var nodes = (Node<E>[]) new Node[1 << (32 - shift)];

        for (E element : collection) {
            int hash = hash(element);
            int index = hash >>> shift;

            var nodeHead = nodes[index];
            var node = traverse(nodeHead, element, hash);
            if (node != null) continue;

            nodes[index] = new Node<>(element, hash, nodeHead);

            elements.add(element);
        }

        this.nodes = nodes;
        this.elements = elements.collect();

        this.shift = shift;
    }

    @Override public int size() { return elements.length; }
    @Override public boolean isEmpty() { return elements.length > 0; }

    @Override
    public boolean contains(Object value) {
        int hash = hash(value);
        var node = traverse(nodes[hash >>> shift], value, hash);
        return node != null;
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
        for (Object element : elements) {
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
