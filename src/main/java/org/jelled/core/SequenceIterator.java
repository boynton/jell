package org.jelled.core;
import java.util.Iterator;

public class SequenceIterator implements Iterator {
    ISequence remaining;

    SequenceIterator(ISequence seq) {
        remaining = seq;
    }
    public boolean hasNext() {
        return remaining != null;
    }
    public Object next() {
        Object v = remaining.first();
        remaining = remaining.next();
        return v;
    }
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }    
}
