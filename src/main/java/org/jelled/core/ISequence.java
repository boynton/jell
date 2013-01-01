package org.jelled.core;
import java.util.Iterator;

public interface ISequence extends ICollection, Iterable {

    /**
     * @returns the first object in the sequence, or null if the sequence is empty
     */
    public Object first();

    /**
     * @returns returns the rest of the sequence if it exists, null otherwise
     */
    public ISequence next();

    /**
     * @returns a new sequence that has the object as its first element
     */
    public ISequence cons(Object o);

    public Iterator iterator();
}
