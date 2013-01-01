package org.jelled.core;
import java.util.Iterator;
import java.util.ArrayList;

public class LList extends ASequence {

    Object first;
    ISequence rest;

    public LList(Object f, ISequence r) {
        first = f;
        rest = r;
    }

    public static LList create(Object... v) {
        LList result = null;
        for (int i=v.length-1; i>=0; i--)
            result = new LList(v[i], result);
        return result;
    }

    public static LList fromArrayList(ArrayList lst) {
        LList result = null;
        for (int i=lst.size()-1; i>=0; i--)
            result = new LList(lst.get(i), result);
        return result;
    }

    static class EmptyList extends LList {
        private EmptyList() { super(null, null); }
        public String toString() { return "()"; }
        public Object first() { return null; }
        public ISequence next() { return null; }
        public int knownCount() { return 0; }
        public int count() { return 0; }
        public ISequence cons(Object o) { return new LList(o, null); }
        public ISequence seq() { return null; }
        public ISequence conj(Object o) { return cons(o); }
        public ICollection empty() { return this; }
        public ISequence rest() { return this; }
    }
    public static LList EMPTY = new EmptyList();

    public ICollection empty() {
        return EMPTY;
    }

    public Object first() {
        return first;
    }

    public ISequence next() {
        return rest;
    }

    public ICollection conj(Object o) {
        return new LList(o, this);
    }

    public ISequence cons(Object o) {
        return new LList(o, this);
    }

    public ISequence rest() {
        return rest;
    }

}
