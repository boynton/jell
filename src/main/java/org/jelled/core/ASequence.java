package org.jelled.core;
import java.util.Iterator;

//an abstract sequence building block. Sequences always have at least one element
abstract public class ASequence implements ISequence {

    abstract public ICollection empty();
    abstract public ICollection conj(Object o);
    abstract public Object first();
    abstract public ISequence next();
    abstract public ISequence cons(Object o);

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(Value.toString(first()));
        ISequence o = next();
        while (o != null) {
            sb.append(" ");
            sb.append(Value.toString(o.first()));
            o = o.next();
        }
        sb.append(")");
        return sb.toString();
    }

    public int knownCount() {
        return -1;
    }

    public int count() {
        int k = knownCount();
        if (k >= 0)
            return k;
        ISequence r = next();
        if (r == null)
            return 1;
        else {
            int i = 1;
            while (r != null) {
                k = r.knownCount();
                if (k >= 0)
                    return i + k;
                i++;
                r = r.next();
            }
            return i;
        }
    }

    public ISequence seq() {
        return this;
    }

    public Iterator iterator() {
        return new SequenceIterator(this);
    }
    
}
