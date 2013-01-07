package org.jelled.core;

public abstract class AMap implements IMap {

    protected AMap() { }

    abstract public ICollection empty();

    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        sb.append("{");
        ISequence seq = seq();
        if (seq != null) {
            for (Object o : seq) {
                LArray a = (LArray)o;
                Object k = a.ref(0);
                Object v = a.ref(1);
                if (first)
                    first = false;
                else
                    sb.append(' ');
                sb.append(LWriter.toString(k));
                sb.append(" ");
                sb.append(LWriter.toString(v));
            }
        }
        sb.append('}');
        return sb.toString();
    }

    abstract protected LArray find(Object key);

    public boolean has(Object key) {
        LArray b = find(key);
        return b != null;
    }

    public Object get(Object key) {
        LArray b = find(key);
        if (b == null)
            return null;
        return b.ref(1);
    }

    abstract public IMap put(Object key, Object val);

    abstract public IMap remove(Object key);

    abstract public ISequence seq();

    public ICollection conj(Object binding) {
        LArray b = (LArray)binding;
        return put(b.ref(0), b.ref(1));
    }

    public int knownCount() {
        return -1;
    }

    public int count() {
        return 0;
    }


}
