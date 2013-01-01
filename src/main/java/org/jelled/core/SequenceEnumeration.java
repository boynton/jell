package org.jelled.core;
import java.util.Enumeration;

public class SequenceEnumeration implements Enumeration {
    ISequence seq;
    public SequenceEnumeration(ISequence seq) {
        this.seq = seq;
    }
    public boolean hasMoreElements() {
        return seq != null;
    }
    public Object nextElement() {
        Object v = seq.first();
        seq = seq.next();
        return v;
    }
}
