package net.filemaid.util;

import java.util.AbstractSet;
import java.util.BitSet;
import java.util.Iterator;
import java.util.stream.IntStream;

public class IntSet
extends AbstractSet<Integer> {
    private final BitSet bits;
    private final int min;
    private final int max;

    private IntSet(int n, int n3, IntStream intStream) {
        this.min = n;
        this.max = n3;
        this.bits = new BitSet(n3 - n);
        intStream.map(n2 -> n2 - n).forEach(this.bits::set);
    }

    public boolean contains(int n) {
        return n >= this.min && this.bits.get(n - this.min);
    }

    @Override
    public boolean contains(Object object) {
        if (object instanceof Number) {
            int n = ((Number)object).intValue();
            return this.contains(n);
        }
        return false;
    }

    public int min() {
        return this.min;
    }

    public int max() {
        return this.max;
    }

    @Override
    public Iterator<Integer> iterator() {
        return this.bits.stream().mapToObj(n -> this.min + n).iterator();
    }

    @Override
    public int size() {
        return this.bits.cardinality();
    }

    public static IntSet of(BitSet bitSet) {
        if (bitSet.isEmpty()) {
            return new IntSet(0, 0, IntStream.empty());
        }
        int n = bitSet.nextSetBit(0);
        int n2 = bitSet.previousSetBit(bitSet.length() - 1);
        return new IntSet(n, n2, bitSet.stream());
    }

    public static IntSet of(int ... nArray) {
        int n = IntStream.of(nArray).min().orElse(0);
        int n2 = IntStream.of(nArray).max().orElse(0);
        return new IntSet(n, n2, IntStream.of(nArray));
    }

    public static IntSet of(int n, int n2) {
        return new IntSet(n, n2, IntStream.of(n, n2));
    }
}

