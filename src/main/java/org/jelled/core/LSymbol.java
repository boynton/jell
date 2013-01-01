package org.jelled.core;

//Symbols are symbolic tokens which take on a value in a particular environment.
public class LSymbol {
    
    public static Object UNBOUND = new Singleton("<unbound>");

    public final String name;
    LNamespace namespace;
    Object value = UNBOUND;

    public LSymbol(LNamespace ns, String s) {
        this.namespace = ns;
        this.name = s;
    }

    public String toString() {
        //ideally, it would display the package only if it isn't the current package
        //return "#'" + namespace + "/" + name;
        return name;
    }

}
    
