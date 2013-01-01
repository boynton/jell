package org.jelled.core;

public class LArrayMap extends AMap {

    public static final LArrayMap EMPTY_ARRAYMAP = new LArrayMap();

    LArray [] bindings;
    int count;

    private LArrayMap(LArray [] bindings, int count) {
        this.bindings = bindings;
        this.count = count;
    }

    public LArrayMap() {
        this(new LArray [4], 0);
    }

    protected LArray find(Object key) {
        int i = indexOf(key);
        if (i < 0)
            return null;
        else
            return (LArray)bindings[i];
    }

    private int indexOf(Object key) {
        int i = count;
        while (i > 0) {
            LArray b = bindings[--i];
            if (b.ref(0).equals(key))
                return i;
        }
        return -1;
    }

    public IMap put(Object key, Object val) {
        //expensive, compared to LTransientArrayMap
        int i = indexOf(key);
        if (i < 0) {
            LArray [] newBindings = new LArray[count+1];
            System.arraycopy(bindings, 0, newBindings, 0, count);
            newBindings[count] = LArray.create(key, val);
            return new LArrayMap(newBindings, count+1);
        } else {
            LArray [] newBindings = new LArray[count];
            if (i > 0)
                System.arraycopy(bindings, 0, newBindings, 0, i);
            newBindings[i] = LArray.create(key, val);
            if (i+1 < bindings.length)
                System.arraycopy(bindings, i+1, newBindings, i+1, bindings.length-i-1);
            return new LArrayMap(newBindings, count);
        }
    }

    public IMap remove(Object key) {
        throw new LError("NYI");
    }
    public ISequence seq() {
        if (count == 0)
            return null;
        return
            new LArray(bindings, 0, count);
    }
    public int knownCount() {
        return count;
    }
    public int count() {
        return count;
    }
    public ICollection empty() {
        return EMPTY_ARRAYMAP;
    }
}