package org.jelled.core;

public interface ICollection {

    /**
     * @returns -1 if the count cannot be returned in constant time, else return the count.
     */
    int knownCount();

    /**
     * @returns the count of items in the array as a non-negative integer
     */
    int count();

    /**
     * @returns an empty collection of the same type
     */
    ICollection empty();

    /**
     * Returns a collection with the object added ("conj"oined)
     */
    ICollection conj(Object o);

    /**
     * @returns a sequence of the elements in the collection, or null if it is empty
     */
    ISequence seq();
}

