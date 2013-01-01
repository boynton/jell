package org.jelled.core;

public class Singleton {
    String token;
    Singleton(String s) { token = s; }
    public String toString() {
        return token;
    }
}
