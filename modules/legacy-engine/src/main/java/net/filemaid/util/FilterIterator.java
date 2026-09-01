package net.filemaid.util;

import java.util.Iterator;

public abstract class FilterIterator<S, T>
implements Iterator<T> {
    private final Iterator<S> sourceIterator;
    private T current = null;

    public FilterIterator(Iterable<S> iterable) {
        this(iterable.iterator());
    }

    public FilterIterator(Iterator<S> iterator) {
        this.sourceIterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return this.peekNext(false) != null;
    }

    @Override
    public T next() {
        try {
            T t = this.peekNext(true);
            return t;
        }
        finally {
            this.current = null;
        }
    }

    private T peekNext(boolean bl) {
        while (this.current == null && (bl || this.sourceIterator.hasNext())) {
            this.current = this.filter(this.sourceIterator.next());
        }
        return this.current;
    }

    protected abstract T filter(S var1);

    @Override
    public void remove() {
        this.sourceIterator.remove();
    }
}

