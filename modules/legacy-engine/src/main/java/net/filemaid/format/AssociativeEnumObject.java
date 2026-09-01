package net.filemaid.format;

import groovy.lang.GroovyObjectSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.filemaid.util.RegularExpressions;

public class AssociativeEnumObject
extends GroovyObjectSupport
implements List<Object> {
    private final Map<?, ?> values;

    public AssociativeEnumObject(Map<?, ?> map) {
        this.values = map;
    }

    protected String definingKey(Object object) {
        return RegularExpressions.NON_WORD.matcher(object.toString()).replaceAll("").toLowerCase(Locale.ROOT);
    }

    public Object getProperty(String string) {
        return this.getValue(this.definingKey(string));
    }

    private Object getValue(String string) {
        return this.values.keySet().stream().filter(object -> this.definingKey(object).equals(string)).findFirst().map(this.values::get).orElse(null);
    }

    public void setProperty(String string, Object object) {
        throw new UnsupportedOperationException(string);
    }

    public String toString() {
        return this.values.toString();
    }

    public Set<?> keySet() {
        return this.values.keySet();
    }

    public List<Object> toList() {
        return new ArrayList<Object>(this.values.values());
    }

    @Override
    public Iterator<Object> iterator() {
        return this.toList().iterator();
    }

    @Override
    public Object get(int n) {
        return this.toList().get(n);
    }

    @Override
    public List<Object> subList(int n, int n2) {
        return this.toList().subList(n, n2);
    }

    @Override
    public int size() {
        return this.values.size();
    }

    @Override
    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    @Override
    public boolean contains(Object object) {
        return this.values.values().contains(object);
    }

    @Override
    public Object[] toArray() {
        return this.values.values().toArray();
    }

    @Override
    public <T> T[] toArray(T[] TArray) {
        return this.values.values().toArray(TArray);
    }

    @Override
    public boolean add(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return this.values.values().containsAll(collection);
    }

    @Override
    public boolean addAll(Collection<? extends Object> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int n, Collection<? extends Object> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object set(int n, Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int n, Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(int n) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(Object object) {
        return this.toList().indexOf(object);
    }

    @Override
    public int lastIndexOf(Object object) {
        return this.toList().lastIndexOf(object);
    }

    @Override
    public ListIterator<Object> listIterator() {
        return this.toList().listIterator();
    }

    @Override
    public ListIterator<Object> listIterator(int n) {
        return this.toList().listIterator(n);
    }
}

