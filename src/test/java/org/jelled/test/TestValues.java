package org.jelled.test;
import org.jelled.core.*;
import junit.framework.TestCase;
import java.io.StringReader;
import java.io.FileReader;
import java.io.FileWriter;

public class TestValues extends TestCase {

    private LList cons(Object o1, Object o2) {
        return new LList(o1, (ISequence)o2);
    }

    private LList list() { return null; }
    private LList list(Object o1) { return cons(o1, null); }
    private LList list(Object o1, Object o2) { return cons(o1, cons(o2, null)); }
    private LList list(Object o1, Object o2, Object o3) { return cons(o1, cons(o2, cons(o3, null))); }

    private int count(ICollection p) {
        return p.count();
    }

    public void testLists() {
        ISequence lst0 = list();
        System.out.println("lst0 = " + lst0);
        ISequence lst1 = list(1);
        System.out.println("lst1 = " + lst1);
        ISequence lst2 = list(1,2);
        System.out.println("lst2 = " + lst2);
        LList lst3 = list(1,2,3);
        System.out.println("lst3 = " + lst3);
        System.out.println("length(lst3) = " + count(lst3));
        ISequence s1 = lst3.cons(4);
        System.out.println("cons(4, a3) = " + s1);
        ICollection c1 = lst3.conj(4);
        System.out.println("conj(4, a3) = " + c1);
    }

    public void testArrays() {
        LArray a0 = LArray.create();
        System.out.println("a0 = " + a0);
        LArray a1 = LArray.create(1);
        System.out.println("a1 = " + a1);
        LArray a2 = LArray.create(1,2);
        System.out.println("a2 = " + a2);
        LArray a3 = LArray.create(1,2,3);
        System.out.println("a3 = " + a3);
        System.out.println("length(a3) = " + count(a3));
        ISequence s1 = a3.cons(4);
        System.out.println("cons(4, a3) = " + s1);
        ICollection c1 = a3.conj(4);
        System.out.println("conj(4, a3) = " + c1);
    }

    public void testMaps() {
        IMap m0 = new LArrayMap();
        System.out.println("m0 = " + m0);
        IMap m1 = m0.put("foo", 23).put("bar", 57);
        System.out.println("m1 = " + m1);
        System.out.println("m1 knownCount = " + m1.knownCount());
        System.out.println("m1.has('foo') = " + m1.has("foo"));
        System.out.println("m1.get('foo') = " + m1.get("foo"));
        IMap m2 = m1.put("foo", "twenty-three");
        System.out.println("m2= " + m2);
    }

    public void testReader() {
        LEnvironment env = new LEnvironment();
        StringReader sr = new StringReader("(one 2 \"three\")");
        LReader reader = new LReader(sr, env);
        Object o = reader.decode();
        System.out.println("read this: " + o);
        assertTrue("Expected a list", (o instanceof LList));
        LList lst = (LList)o;
        assertTrue("Expected a list of length three", lst.count() == 3);
        assertTrue("Expected a symbol in the user package as the first list element", (lst.first() instanceof LSymbol));
        assertTrue("Expected a Number as the second list element", (lst.rest().first() instanceof Number));
        assertTrue("Expected a Number as the second list element", (lst.next().next().first() instanceof String));
    }

    public void testWriter() {
    }

    public void testReadWrite() {
        LEnvironment env = new LEnvironment();
        try {
            boolean pass1 = true;
            FileReader fr = pass1? new FileReader("tests/benchx.scm") : new FileReader("/tmp/pass1.txt");
            FileWriter fw = pass1? new FileWriter("/tmp/pass1.txt") : new FileWriter("/tmp/pass2.txt");
            LReader reader = new LReader(fr, env);
            LWriter writer = new LWriter(fw, env);
            //enable r4rs reader macros, i.e. #t is true, etc.
            Object o = reader.decode();
            while (o != LReader.EOS) {
                writer.encode(o);
                fw.write("\n");
                o = reader.decode();
            }
            fw.close();
            fr.close();
        } catch (Exception e) {
            fail("cannot read file: " + e);
        }
    }
}