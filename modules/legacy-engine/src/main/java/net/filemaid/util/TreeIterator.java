package net.filemaid.util;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;

public abstract class TreeIterator<T>
implements Iterator<T> {
    private final Deque<Iterator<T>> recursionStack = new ArrayDeque<Iterator<T>>();

    public TreeIterator(T ... TArray) {
        this.recursionStack.push(Arrays.asList(TArray).iterator());
    }

    protected abstract Iterator<T> children(T var1);

    @Override
    public boolean hasNext() {
        return this.currentIterator().hasNext();
    }

    @Override
    public T next() {
        T t = this.currentIterator().next();
        Iterator<T> iterator = this.children(t);
        if (iterator != null && iterator.hasNext()) {
            this.recursionStack.push(iterator);
        }
        return t;
    }

    private Iterator<T> currentIterator() {
        Iterator<T> iterator = this.recursionStack.peek();
        if (iterator.hasNext() || this.recursionStack.size() <= 1) {
            return iterator;
        }
        this.recursionStack.pop();
        return this.currentIterator();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}

