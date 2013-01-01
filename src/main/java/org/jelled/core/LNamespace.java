package org.jelled.core;
import java.util.Collection;
import java.util.HashMap;

public class LNamespace {
    
    static HashMap<String,LNamespace> namespaces = new HashMap<String,LNamespace>();

    public static LNamespace forName(String name) {
        LNamespace ns = namespaces.get(name);
        if (ns == null) {
            ns = new LNamespace(name);
            namespaces.put(name, ns);
        }
        return ns;
    }

    String name;
    HashMap<String,LSymbol> symbols = new HashMap<String,LSymbol>();
    
    LNamespace(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public LSymbol intern(String name) {
        LSymbol sym = symbols.get(name);
        if (sym == null) {
            sym = new LSymbol(this, name);
            symbols.put(name, sym);
        }
        return sym;
    }
    
    public boolean exists(String name) {
        return symbols.containsKey(name);
    }
    
    public Object get(String name) {
        return symbols.get(name);
    }
    
    public void remove(String name) {
        symbols.remove(name);
    }

    public Collection<LSymbol> symbols() {
        return symbols.values();
    }
    
}
