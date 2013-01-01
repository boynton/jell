package org.jelled.core;

public interface IMap extends ICollection {
    public boolean has(Object key);
    public Object get(Object key);
    public IMap put(Object key, Object val);
    public IMap remove(Object key);
}