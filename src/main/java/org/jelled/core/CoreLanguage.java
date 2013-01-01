package org.jelled.core;
import java.io.Reader;
import java.io.Writer;

public class CoreLanguage implements ILanguage {

    private LVM vm;
    
    public static void main(String [] args) {
        try {
            new LVM().run(args);
        } catch (Throwable th) {
            th.printStackTrace();
            System.out.println("[exiting]");
        }
    }

    public void init(LVM vm) {
        this.vm = vm;
        //System.out.println("[defining CoreLanguage primitives]");
        vm.defPrimitives(getClass());
    }

    public Object read(Reader in) {
        return new LReader(in, vm.environment()).decode();
    }

    public void write(Object o, Writer out, boolean forRead) {
        new LWriter(out, vm.environment()).encode(o, forRead);
    }

    public Object expand(Object expr) {
        return expr;
    }

    public LCode compile(Object expr) {
        return new LCompiler(vm).compile(expr);
    }

    /* ------------------------- Language Primitives ------------------------- */
    
    public Object primitive_println(String s) {
        //look up *out* in the environment, redirect to that.
        System.out.println(s);
        return null;
    }

}
