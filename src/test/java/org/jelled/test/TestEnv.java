package org.jelled.test;
import org.jelled.core.*;

import junit.framework.TestCase;

/**
 * Tests functionality of the Environment
 */
public class TestEnv extends TestCase {
    
    //fail("message")
    //assertTrue("message", boolean)
    
    public void testDefaultEnv() {
        LNamespace user = LNamespace.forName("user");
    	LEnvironment env = new LEnvironment(user);
        //should have java.lang symbols by default
        LNamespace defaultNamespace = env.currentNamespace();
        assertTrue("Environment default namespace is not what we initialized with", user == defaultNamespace);
        env = new LEnvironment();
        assertTrue("Environment default namespace is not expected 'user'", user == env.currentNamespace());
        LNamespace foo = LNamespace.forName("foo");
        LNamespace tmp = env.setCurrentNamespace(foo);
        assertTrue("Environment.setCurrentNamespace didn't return previous", tmp == user);
        assertTrue("Environment.setCurrentNamespace didn't take", env.currentNamespace() == foo);
    }

    public void testInternEnv() {
        System.out.println("------------ testInternEnv");
    	LEnvironment env = new LEnvironment();
        LSymbol foo = env.intern("foo");
        LSymbol foo2 = env.intern("foo");
        assertTrue("Environment.intern return different symbol for same string", foo == foo2);
        LNamespace ns2 = LNamespace.forName("blah");
        LSymbol foo3 = ns2.intern("foo");
        try {
            LSymbol foo4 = env.intern(foo3);
            assertTrue("Environment.intern from different package should not have succeeded", false);
        } catch (Exception e) {
            //expected
            System.out.println("Expected error: " + e);
        }
        try {
            LSymbol bar = ns2.intern("bar");
            LSymbol bar2 = env.intern(bar);
            assertTrue("cannot intern across packages", bar == bar2);
        } catch (Exception e) {
            fail("cannot intern across packages: " + e);
        }
        System.out.println("env after: " + env.symbols());
    }

    public void testCrossNamespaceIntern() {
        System.out.println("------------ testCrossNamespaceIntern");
    	LEnvironment env = new LEnvironment();
        LNamespace ns2 = LNamespace.forName("blah");
        LSymbol bar = ns2.intern("bar");
        LSymbol bar2 = env.getSymbol("bar");
        System.out.println("bar2 = " + bar2);
        System.out.println("env before: " + env.symbols());
        env.refer(ns2);
        System.out.println("env after: " + env.symbols());
        LSymbol bar3 = env.getSymbol("bar");
        System.out.println("bar3 = " + bar3);
    }
}
