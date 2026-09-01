package net.filemaid.util;

import java.util.AbstractList;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

public class FunctionList<E>
extends AbstractList<E> {
    private final IntFunction<E> get;
    private final IntSupplier size;

    public FunctionList(IntFunction<E> intFunction, IntSupplier intSupplier) {
        this.get = intFunction;
        this.size = intSupplier;
    }

    @Override
    public E get(int n) {
        return this.get.apply(n);
    }

    @Override
    public int size() {
        return this.size.getAsInt();
    }

    public static <S, E> List<E> of(List<S> list, Function<S, E> function) {
        return new FunctionList<E>(n -> function.apply(list.get(n)), list::size);
    }

    public static <S, E> List<E> of(IntFunction<E> intFunction, int n) {
        return new FunctionList<E>(intFunction, () -> n);
    }
}

