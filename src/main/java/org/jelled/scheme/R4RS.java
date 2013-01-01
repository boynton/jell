package org.jelled.scheme;
import org.jelled.core.*;
import java.io.Reader;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;

public class R4RS implements ILanguage {
    
    public static LNamespace SCHEME_NS = LNamespace.forName("scheme");
    
    public static void main(String [] args) {
        try {
            new R4RS().run(args);
        } catch (Throwable th) {
            th.printStackTrace();
            System.out.println("[exiting]");
        }
    }
    
    private final LVM vm;

    public R4RS() {
        LEnvironment env = new LEnvironment(SCHEME_NS);
        vm = new LVM(env, this);
    }

    public void run(String [] args) {
        vm.run(args);
    }


    public void init(LVM vm) {
        //System.out.println("[defining R4RS primitives]");
        vm.defPrimitives(getClass());
    }

    public Object read(Reader in) {
        return new ScmReader(in, vm.environment()).decode();
    }

    public void write(Object obj, Writer out, boolean forRead) {
        new ScmWriter(out, vm.environment()).encode(obj, forRead);
    }

    public Object expand(Object expr) {
        return new ScmExpander(vm).expand(expr);
    }

    public LCode compile(Object expr) {
        return new LCompiler(vm).compile(expr);
    }

    /* ------------------------- Primitives provided to the vm --------------------- */

    public Object primitive_newline() {
        //this actually takes an optional port argument
        //currentOutputPort().newline();
        vm.println("");
        return null;
    }

    public Object primitive_display(Object o) {
        //currentOutputPort().display(o);
        vm.print(o);
        return null;
    }

}
