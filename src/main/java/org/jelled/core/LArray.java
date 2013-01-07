package org.jelled.core;
import java.util.Iterator;

public class LArray implements ISequence {

    public static LArray EMPTY_ARRAY = new LArray(new Object[0], 0, 0);

    private final int offset;
    private final int count;
    private final Object [] values;

    LArray(Object [] values, int offset, int count) {
        this.values = values;
        this.offset = offset;
        this.count = count;
    }

    public static LArray create(Object... v) {
        return new LArray(v, 0, v.length);
    }

    public int knownCount() {
        return count;
    }

    public int count() {
        return count;
    }

    public Object ref(int idx) {
        if (idx < 0 || idx > count)
            throw new LError("Array index out of range: " + idx);
        return values[offset+idx];
    }
    public ISequence seq() {
        if (count == 0)
            return null;
        return this;
    }

    public ICollection empty() {
        return EMPTY_ARRAY;
    }

    public Object first() {
        if (count == 0)
            return null;
        return values[offset];
    }
    public ISequence next() {
        if (count <= 1)
            return null;
        return new LArray(values, offset+1, count-1);
    }

    public ICollection conj(Object o) {
        Object [] v = new Object[count+1];
        if (count > 0)
            System.arraycopy(values, offset, v, 0, count);
        v[count] = o;
        return new LArray(v, 0, count+1);
    }

    public ISequence cons(Object o) {
        return new LList(o, this);
    }

    private class ArrayIterator implements Iterator {
        int i, end;
        ArrayIterator() {
            i = offset;
            end = offset+count;
        }
        public boolean hasNext() {
            return i < end;
        }
        public Object next() {
            return values[offset + i++];
        }
        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
        
    }
    public Iterator iterator() {
        return new ArrayIterator();
    }

    public String toString() {
        if (count == 0)
            return "[]";
        else {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            int last = offset+count;
            for (int i=offset; i<last; i++) {
                Object v = values[i];
                if (first) {
                    first = false;
                    sb.append('[');
                } else {
                    sb.append(' ');
                }
                sb.append(LWriter.toString(v));
            }
            sb.append(']');
            return sb.toString();
        }
    }
}
