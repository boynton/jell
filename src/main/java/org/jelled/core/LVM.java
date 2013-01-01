package org.jelled.core;

import java.util.HashMap;
import java.io.Reader;
import java.io.FileReader;
import java.io.File;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.annotation.Annotation;

public class LVM {
    
    public static final Object UNBOUND = LSymbol.UNBOUND;
    public static final Object EOS = LReader.EOS;
    //    public static final Object VOID = new Singleton("<void>");
    //    public static final Object DOT = new Singleton(".");
    
    /* basic instructions */
    public static final int OPCODE_CALL = 1;
    public static final int OPCODE_TAILCALL = 2;
    public static final int OPCODE_RETURN = 3;
    public static final int OPCODE_JUMPFALSE = 4;
    public static final int OPCODE_JUMP = 5;
    public static final int OPCODE_LOCAL = 6;
    public static final int OPCODE_CONSTANT = 7;
    public static final int OPCODE_POP = 8;
    public static final int OPCODE_CLOSURE = 9;
    public static final int OPCODE_GLOBAL = 10;
    public static final int OPCODE_DEF_GLOBAL = 11;
    public static final int OPCODE_SET_GLOBAL = 12;
    public static final int OPCODE_SET_LOCAL = 13;
    public static final int OPCODE_CALLCC = 14;
    //public static final int OPCODE_APPLY = 15;
    public static final int OPCODE_PRIMITIVE =16;
    public static final int OPCODE_GLOBALTEST = 17;
    
    ILanguage language;
    Frame currentFrame;
    LEnvironment environment;
    
    Object constants[] = new Object[1000];
    int nConstants = 0;
    Object stack[] = new Object[1000];
    
    //the following are maintained in the object per call to execute(), which is not reentrant.
    int sp;
    int [] ops;
    int pc;

    
    Writer systemOutput = new BufferedWriter(new OutputStreamWriter(System.out));
    Writer currentOutput = systemOutput;

    public LVM() {
        this(new LEnvironment(LNamespace.forName("ell.core")));
    }

    public LVM(LEnvironment env) {
        this(env, new CoreLanguage());
    }

    static public LVM debugInstance = null;

    public LVM(LEnvironment env, ILanguage lang) {
        this.language = lang;
        this.environment = env;
        language.init(this);
        debugInstance = this;
    }

    public ILanguage language() {
        return language;
    }

    public LEnvironment environment() {
        return environment;
    }

    public LSymbol intern(String name) {
        return environment.intern(name);
    }

    Object error(String msg) {
        throw new LError(msg);
        //never returns
    }

    public Object read(Reader in) {
        return language.read(in);
    }

    public void write(Object o, Writer out) {
        language.write(o, out, true);
    }

    public void print(Object o, Writer out) {
        language.write(o, out, false);
    }

    public void print(Object o) {
        print(o, currentOutput);
    }

    public void println(Object o, Writer out) {
        language.write(o, out, false);
        try {
            out.write("\n");
            out.flush();
        } catch (IOException e ) { }
    }

    public void println(Object o) {
        println(o, currentOutput);
    }

    public Object macroexpand(Object expr) {
        return language.expand(expr);
    }

    public LCode compile(Object expr) {
        return language.compile(expr);
    }

    /**
     * parse, compile, and execute sequence code objects in the specified file.
     * @param file the file to load
     * @returns the result of the execution of the last expression in the file
     */
    public Object load(File file) {
        Object result = null;
        try {
            FileReader r = new FileReader(file);
            try {
                Object expr = read(r);
                while (expr != EOS) {
                    System.out.println("EXPR: " + expr);
                    Object expanded = macroexpand(expr);
                    System.out.println("EXPANDED: " + expanded);
                    LCode thunk = compile(expanded);
                    System.out.println("COMPILED: " + thunk);
                    result = execute(thunk);
                    if (result instanceof Exception) {
                        println("*** " + result);
                        break;
                    } else if (result != null)
                        println(result);
                    expr = read(r);
                }
            } finally {
                r.close();
            }
            return result;
        } catch (IOException e) {
            return error("Cannot load: " + e);
        }
    }

    public Object load(String filename) {
        return load(new File(filename));
    }

    public void run(String [] args) {
        for (String filename : args) {
            load(filename);
        }
    }

    private final Object localRef(int i, int j) {
        Frame f = currentFrame;
        while (i > 0) {
            f = f.locals;
            i--;
        }
        return f.elements[j];
    }

    private final void localSet(int i, int j) {
        Frame f = currentFrame;
        while (i > 0) {
            f = f.locals;
            i--;
        }
        f.elements[j] = stack[sp];
    }
    
    void funcall(Object fun, int argc, int savedPc) {
        if (fun instanceof Closure) {
            Closure closure = (Closure)fun;
            Frame f = new Frame();
            f.previous = currentFrame;
            f.pc = savedPc;
            f.ops = ops;
            f.locals = closure.frame;
            if (closure.code.argc >= 0) {
                if (argc != closure.code.argc)
                    error("Wrong number of args (" + argc + ") to " + closure);
                f.elements = new Object[argc];
                if (argc > 0) {
                    System.arraycopy(stack, sp, f.elements, 0, argc);
                    sp += argc;
                }
            } else { //rest args
                int nMinArgc = -closure.code.argc - 1;
                if (argc < nMinArgc)
                    error("Wrong number of args (" + argc + ") to " + closure);
                f.elements = new Object[nMinArgc + 1];
                System.arraycopy(stack, sp, f.elements, 0, nMinArgc);
                sp += argc;
				int nRest = argc - nMinArgc;
				int j = sp;
				LList lstRest = null;
				while (nRest-- > 0)
					lstRest = new LList(stack[--j], lstRest);
                f.elements[nMinArgc] = lstRest;
            }
            ops = closure.code.ops;
            pc = 0;
            currentFrame = f;
        } else if (fun instanceof Primitive) {
            Primitive nProc = (Primitive)fun;
            Object o = callPrimitive(nProc.method, argc);
            sp = sp + argc - 1;
            stack[sp] = o;
            pc = savedPc;
        } else {
            error("Unhandled case in funcall");
        }
    }

    void tailcall(Object fun, int argc) {
        if (fun instanceof Closure) {
            Closure closure = (Closure)fun;
            Frame f = new Frame();
            f.previous = currentFrame.previous;
            f.pc = currentFrame.pc;
            f.ops = currentFrame.ops;
            f.locals = closure.frame;
            if (closure.code.argc >= 0) {
                if (argc != closure.code.argc)
                    error("Wrong number of args (" + argc + ") to " + closure);
                f.elements = new Object[argc];
                System.arraycopy(stack, sp, f.elements, 0, argc);
                sp += argc;
            } else {
                int nMinArgc = -closure.code.argc - 1;
                if (argc < nMinArgc)
                    error("Wrong number of args (" + argc + ") to " + closure);
                f.elements = new Object[nMinArgc + 1];
                System.arraycopy(stack, sp, f.elements, 0, nMinArgc);
                sp += argc;
				int nRest = argc - nMinArgc;
				int j = sp;
				LList lstRest = null;
				while (nRest-- > 0)
					lstRest = new LList(stack[--j], lstRest);
                f.elements[nMinArgc] = lstRest;
            }
            ops = closure.code.ops;
            pc = 0;
            currentFrame = f;
        } else if (fun instanceof Primitive) {
            Primitive nProc = (Primitive)fun;
            Object o = callPrimitive(nProc.method, argc);
            sp = sp + argc - 1;
            stack[sp] = o;
            pc = currentFrame.pc;
            ops = currentFrame.ops;
            currentFrame = currentFrame.previous;
        } else
            error("Unhandled case in tailcall: " + fun);
    }
    
    private final Object getGlobal(int i) {
        LSymbol sym = (LSymbol)constants[i];
        Object val = sym.value;
        if (val == UNBOUND)
            error("Unbound variable: " + sym);
        return val;
    }

    private final boolean isUnbound(String s) {
        LSymbol sym = intern(s);
        return (sym.value == UNBOUND);
    }

    public void define(String s, Object val) {
        LSymbol sym = intern(s);
        sym.value = val;
    }

    private final void defGlobal(int i) {
        LSymbol sym = (LSymbol)constants[i];
        sym.value = stack[sp];
    }

    private final void setGlobal(int i) {
        LSymbol sym = (LSymbol)constants[i];
        if (sym.value == UNBOUND)
            error("Unbound variable: " + sym);
        sym.value = stack[sp];
    }

    public Object execute(LCode code) {
        currentFrame = new Frame();
        currentFrame.previous = null;
        currentFrame.ops = null;
        currentFrame.pc = 0;
        currentFrame.locals = null;
        currentFrame.elements = null;
        sp = stack.length;
        ops = code.ops;
        pc = 0;

        while (true) {
            try {
                while (true) {
                    switch (ops[pc]) {
                         
                    case OPCODE_LOCAL:
                        stack[--sp] = localRef(ops[pc+1], ops[pc+2]);
                        pc += 3;
                        break;

                    case OPCODE_PRIMITIVE:
                        callPrimitive(ops[pc+1], ops[pc+2]);
                        pc += 3;
                        break;
                        
                    case OPCODE_JUMPFALSE:
                        if (stack[sp++] == Boolean.FALSE)
                            pc += ops[pc+1];
                        else
                            pc += 2;
                        break;
                        
                    case OPCODE_JUMP:
                        pc += ops[pc+1];
                        break;
                        
                    case OPCODE_TAILCALL: //optimize this
                        tailcall(stack[sp++], ops[pc+1]);
                        break;
                        
                    case OPCODE_CALL: //optimize this
                        //check_stack();
                        funcall(stack[sp++], ops[pc+1], pc+2);
                        break;
                        
                    case OPCODE_RETURN:
                        ops = currentFrame.ops;
                        pc = currentFrame.pc;
                        currentFrame = currentFrame.previous;
                        if (currentFrame == null)
                            return stack[sp];
                        break;
                        
                    case OPCODE_CLOSURE:
                        stack[--sp] = new Closure(constants[ops[pc+1]], currentFrame);
                        pc += 2;
                        break;

                    case OPCODE_GLOBAL:
                        stack[--sp] = getGlobal(ops[pc+1]);
                        pc += 2;
                        break;
                        
                    case OPCODE_CONSTANT:
                        stack[--sp] = constants[ops[pc+1]];
                        pc += 2;
                        break;
                        
                    case OPCODE_POP:
                        sp++;
                        pc += 1;
                        break;
                        
                    case OPCODE_SET_LOCAL:
						localSet(ops[pc+1], ops[pc+2]);
						/*
                        {
                            Frame f = currentFrame;
                            int i = ops[pc+1];
                            while (i > 0) {
                                f = f.locals;
                                i--;
                            }
                            f.elements[ops[pc+2]] = stack[sp];
                        }
*/
                        pc += 3;
                        break;
                        
                    case OPCODE_DEF_GLOBAL:
                        defGlobal(ops[pc+1]);
                        pc += 2;
                        break;
                        
                    case OPCODE_SET_GLOBAL:
                        setGlobal(ops[pc+1]);
                        pc += 2;
                        break;
                    }
                }
            } catch (ClassCastException e) {
                String s = e.getMessage();
                System.out.println("\n*** bad argument type: " + s);
                return null;
            } catch (Exception e) {
                //if the restart continuation is bound, then jump to it.
                //else
                //e.printStackTrace();
                //                System.out.println("\n*** " + e);
                return e;
            }
        }
    }

	//constants the code refers to must be registered here in the global constant pool.
    public int registerConstant(Object obj) {
        int n = 0;
        while (n < nConstants) {
            if (constants[n] == obj)
                return n;
            else
                n++;
        }
        if (nConstants == constants.length) {
            Object newConstants[] = new Object[nConstants * 2];
            System.arraycopy(constants, 0, newConstants, 0, nConstants);
            constants = newConstants;
        }
        constants[nConstants++] = obj;
        return nConstants-1;
    }
    
    
    private static class Frame {
        Frame previous;
        int pc;
        int ops[];
        Frame locals;
        Object elements[];
    }
    
    public static class Closure {
        LCode code;
        Frame frame;
        Closure(Object c, Frame f) {
            code = (LCode)c;
            frame = f;
        }
    }
    
    public static class Primitive {
        Method method;
        String name;
        
        Primitive(String name, Method method) {
            this.name = name;
            this.method = method;
        }
        public String toString() {
            return "<primitive " + name + ">";
        }
    }

    //DONE fixed-argc primitives work. the programmer must not duplicate names, these are all defined in the global namespace.
    //DO fix "optional" arguments, i.e. minArgc, maxArgc. I.e. if min=3, max=5, then (fun 1 2 3 4 5) -> (fun 1 2 3 . 4 5)
    //note: this is the advantage of passing in the raw stack -- the primitive can intepret it however it wants. But, the code is uglier.
    public void defPrimitives(Class<?> c) {
        //System.out.println("defPrimitives for " + c);
        //bug: multiple arity methods get redefined, only the last is defined.
        for (Method method : c.getDeclaredMethods()) {
            String name = method.getName();
            if (name.startsWith("primitive_")) {
                String pname = name.substring("primitive_".length());
                LSymbol sym = intern(pname);
                if (sym.value != UNBOUND)
                    System.out.println("*** Warning: redefining " + pname);
                //check the argument signature. Define "primitiveN" differently than "primitive0" .. "primitive3"
                define(pname, new Primitive(pname, method));
            }
        }
        
    }

    protected Object callPrimitive(int i, int argc) {
        Primitive p = (Primitive)constants[i];
        return callPrimitive(p.method, argc);
    }

    protected Object callPrimitive(Method method, int argc) {
        try {
            switch (argc) {
            case 0:
                return method.invoke(language);
            case 1:
                return method.invoke(language, stack[sp]);
            case 2:
                return method.invoke(language, stack[sp], stack[sp+1]);
            case 3:
                return method.invoke(language, stack[sp], stack[sp+1], stack[sp+2]);
            case 4:
                return method.invoke(language, stack[sp], stack[sp+1], stack[sp+2], stack[sp+3]);
            case 5:
                return method.invoke(language, stack[sp], stack[sp+1], stack[sp+2], stack[sp+3], stack[sp+4]);
            default:
                error("too many arguments: " + argc);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            error("Primitive error: " + e.getMessage());
            return null;
        }
    }

}
