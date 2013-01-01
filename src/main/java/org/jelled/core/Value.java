package org.jelled.core;

public class Value {
    
    public static String toString(Object val) {
        if (val == null)
            return "nil";
        else if (val instanceof String)
            return "\"" + val + "\""; //fix
        else {
            return val.toString();
        }
    }
    
}
