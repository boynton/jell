package ell;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.lang.reflect.*;

public class Runtime extends Notation {

    static final int LITERAL_OPCODE = 1;
    static final int LOCAL_OPCODE = 2;
    static final int JUMPFALSE_OPCODE = 3;
    static final int JUMP_OPCODE = 4;
    static final int TAILCALL_OPCODE = 5;
    static final int CALL_OPCODE = 6;
    static final int RETURN_OPCODE = 7;
    static final int CLOSURE_OPCODE = 8;
    static final int POP_OPCODE = 9;
    static final int GLOBAL_OPCODE = 10;
    static final int DEFGLOBAL_OPCODE = 11;
    static final int SETLOCAL_OPCODE = 12;
    static final int NULL_OPCODE = 13;
    static final int CAR_OPCODE = 14;
    static final int CDR_OPCODE = 15;
    static final int ADD_OPCODE = 16;
    static final int MUL_OPCODE = 17;
    static final int USE_OPCODE = 18;

    static final LSymbol SYM_LITERAL = LSymbol.intern("literal");
    static final LSymbol SYM_LOCAL = LSymbol.intern("local");
    static final LSymbol SYM_JUMPFALSE = LSymbol.intern("jumpfalse");
    static final LSymbol SYM_JUMP = LSymbol.intern("jump");
    static final LSymbol SYM_TAILCALL = LSymbol.intern("tailcall");
    static final LSymbol SYM_CALL = LSymbol.intern("call");
    static final LSymbol SYM_RETURN = LSymbol.intern("return");
    static final LSymbol SYM_CLOSURE = LSymbol.intern("closure");
    static final LSymbol SYM_POP = LSymbol.intern("pop");
    static final LSymbol SYM_GLOBAL = LSymbol.intern("global");
    static final LSymbol SYM_DEFGLOBAL = LSymbol.intern("defglobal");
    static final LSymbol SYM_FUNC = LSymbol.intern("function");
    static final LSymbol SYM_SETLOCAL = LSymbol.intern("setlocal");
    static final LSymbol SYM_USE = LSymbol.intern("use");

    static final LSymbol SYM_CAR = LSymbol.intern("car");
    static final LSymbol SYM_CDR = LSymbol.intern("cdr");
    static final LSymbol SYM_NULL = LSymbol.intern("null");
    static final LSymbol SYM_ADD = LSymbol.intern("add");
    static final LSymbol SYM_MUL = LSymbol.intern("mul");

    static final LSymbol SYM_FUNCTION = intern("function");
    static final LSymbol SYM_MODULE = intern("module");

    static boolean verbose = false;

    static class IntVector {
        int [] elements;
        int count;
        public IntVector(int initialCapacity) {
            this.elements = new int[initialCapacity];
            this.count = 0;
        }
        public void add(int val) {
            if (count == elements.length) {
                int newcap = count * 2;
                int [] tmp = new int[newcap];
                System.arraycopy(elements, 0, tmp, 0, count);
                elements = tmp;
            }
            elements[count++] = val;
        }
        public int size() {
            return count;
        }
        public int getInt(int idx) {
            return elements[idx];
        }
        public void setInt(int idx, int val) {
            elements[idx] = val;
        }
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            if (count > 0) {
                sb.append(elements[0]);
                for (int i=1; i<count; i++) {
                    sb.append(" ");
                    sb.append(elements[i]);
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }

    static class LFunction extends var {
        LSymbol type() { return SYM_FUNCTION; }
    }
    public static boolean isFunction(var v) { return v instanceof LFunction; }

    static class LCode extends LFunction {
        IntVector ops;
        LModule module;
        var rest;
        int argc;
        public LCode(LModule module, int argc) {
            this(module, argc, NIL);
        }
        public LCode(LModule module, int argc, var rest) {
            this.argc = argc;
            this.rest = rest;
            this.module = module;
            this.ops = new IntVector(64);
        }
        LCode loadOps(var ops) {
            while (ops != NIL) {
                var instr = car(ops);
                var op = car(instr);
                if (op == SYM_CLOSURE) {
                    var lstFunc = cadr(instr);
                    if (asSymbol(car(lstFunc)) != SYM_FUNC)
                        error("Bad argument for a closure: " + lstFunc);
                    lstFunc = cdr(lstFunc);
                    int ac = intValue(car(lstFunc));
                    LCode fun = new LCode(module, ac).loadOps(cdr(lstFunc));
                    emitClosure(fun);
                } else if (op == SYM_FUNC) {
                    //what is this for?
                } else if (op == SYM_LITERAL) {
                    emitLiteral(cadr(instr));
                } else if (op == SYM_LOCAL) {
                    emitLocal(intValue(cadr(instr)), intValue(caddr(instr)));
                } else if (op == SYM_SETLOCAL) {
                    emitSetLocal(intValue(cadr(instr)), intValue(caddr(instr)));
                } else if (op == SYM_GLOBAL) {
                    emitGlobal(cadr(instr));
                } else if (op == SYM_JUMP) {
                    emitJump(intValue(cadr(instr)));
                } else if (op == SYM_JUMPFALSE) {
                    emitJumpFalse(intValue(cadr(instr)));
                } else if (op == SYM_CALL) {
                    emitCall(intValue(cadr(instr)));
                } else if (op == SYM_TAILCALL) {
                    emitTailCall(intValue(cadr(instr)));
                } else if (op == SYM_RETURN) {
                    emitReturn();
                } else if (op == SYM_POP) {
                    emitPop();
                } else if (op == SYM_DEFGLOBAL) {
                    emitDefGlobal(cadr(instr));
                } else if (op == SYM_USE) {
                    emitUse(cadr(instr));
                } else if (op == SYM_CAR) {
                    emitCar();
                } else if (op == SYM_CDR) {
                    emitCdr();
                } else if (op == SYM_NULL) {
                    emitNull();
                } else if (op == SYM_ADD) {
                    emitAdd();
                } else if (op == SYM_MUL) {
                    emitMul();
                } else {
                    error("Unknown instruction: " + op);
                }
                ops = cdr(ops);
            }
            return this;
        }
        public LCode emitLiteral(String str) {
            return emitLiteral(string(str));
        }
        public LCode emitLiteral(var obj) {
            ops.add(LITERAL_OPCODE);
            ops.add(module.putConstant(obj));
            return this;
        }
        public LCode emitClosure(LCode code) {
            ops.add(CLOSURE_OPCODE);
            ops.add(module.putConstant(code));
            return this;
        }
        public LCode emitLocal(int i, int j) {
            ops.add(LOCAL_OPCODE);
            ops.add(i);
            ops.add(j);
            return this;
        }
        public LCode emitSetLocal(int i, int j) {
            ops.add(SETLOCAL_OPCODE);
            ops.add(i);
            ops.add(j);
            return this;
        }
        public LCode emitGlobal(var sym) {
            if (!isSymbol(sym))
                error("emitGlobal: not a symbol: " + sym);
            ops.add(GLOBAL_OPCODE);
            ops.add(module.putConstant(sym));
            return this;
        }
        public LCode emitDefGlobal(var sym) {
            if (!isSymbol(sym))
                error("emitDefGlobal: not a symbol: " + sym);
            ops.add(DEFGLOBAL_OPCODE);
            ops.add(module.putConstant(sym));
            return this;
        }
        public LCode emitCall(int argc) {
            ops.add(CALL_OPCODE);
            ops.add(argc);
            return this;
        }
        public LCode emitTailCall(int argc) {
            ops.add(TAILCALL_OPCODE);
            ops.add(argc);
            return this;
        }
        public int emitJumpFalse(int offset) {
            ops.add(JUMPFALSE_OPCODE);
            int loc = ops.size();
            ops.add(offset);
            return loc;
        }
        public int emitJump(int offset) {
            ops.add(JUMP_OPCODE);
            int loc = ops.size();
            ops.add(offset);
            return loc;
        }
        public LCode setJumpLocation(int loc) {
            ops.setInt(loc, ops.size() - loc + 1);
            return this;
        }
        public LCode emitReturn() {
            ops.add(RETURN_OPCODE);
            return this;
        }
        public LCode emitPop() {
            ops.add(POP_OPCODE);
            return this;
        }
        public LCode emitUse(var sym) {
            if (!isSymbol(sym))
                error("emitUse: not a symbol: " + sym);
            ops.add(USE_OPCODE);
            ops.add(module.putConstant(sym));
            return this;
        }
        
        public LCode emitCar() {
            ops.add(CAR_OPCODE);
            return this;
        }
        public LCode emitCdr() {
            ops.add(CDR_OPCODE);
            return this;
        }
        public LCode emitNull() {
            ops.add(NULL_OPCODE);
            return this;
        }

        public LCode emitAdd() {
            ops.add(ADD_OPCODE);
            return this;
        }
        public LCode emitMul() {
            ops.add(MUL_OPCODE);
            return this;
        }

        public int decompile(StringBuilder sb, int offset) {
            switch (ops.getInt(offset)) {
            case LITERAL_OPCODE:
                sb.append(" (" + SYM_LITERAL + " " + write(module.getConstant(ops.getInt(offset+1))) + ")");
                return offset + 2;
            case GLOBAL_OPCODE:
                sb.append(" (" + SYM_GLOBAL + " " + module.getConstant(ops.getInt(offset+1)) + ")");
                return offset + 2;
            case DEFGLOBAL_OPCODE:
                sb.append(" (" + SYM_DEFGLOBAL + " " + module.getConstant(ops.getInt(offset+1)) + ")");
                return offset + 2;
            case JUMP_OPCODE:
                sb.append(" (" + SYM_JUMP + " " + ops.getInt(offset+1) + ")");
                return offset + 2;
            case JUMPFALSE_OPCODE:
                sb.append(" (" + SYM_JUMPFALSE + " " + ops.getInt(offset+1) + ")");
                return offset + 2;
            case CALL_OPCODE:
                sb.append(" (" + SYM_CALL + " " + ops.getInt(offset+1) + ")");
                return offset + 2;
            case TAILCALL_OPCODE:
                sb.append(" (" + SYM_TAILCALL + " " + ops.getInt(offset+1) + ")");
                return offset + 2;
            case RETURN_OPCODE:
                sb.append(" (" + SYM_RETURN + ")");
                return offset + 1;
            case POP_OPCODE:
                sb.append(" (" + SYM_POP + ")");
                return offset + 1;
            case LOCAL_OPCODE:
                sb.append(" (" + SYM_LOCAL + " " + ops.getInt(offset+1) + " " + ops.getInt(offset+2) + ")");
                return offset + 3;
            case CLOSURE_OPCODE:
                sb.append(" (" + SYM_CLOSURE + " " + module.getConstant(ops.getInt(offset+1)) + ")");
                return offset + 2;
            case SETLOCAL_OPCODE:
            	sb.append(" (" + SYM_SETLOCAL + " " + ops.getInt(offset+1) + " " + ops.getInt(offset+2) + ")");
                return offset + 3;
            case USE_OPCODE:
                sb.append(" (" + SYM_USE + " " + module.getConstant(ops.getInt(offset+1)) + ")");
                return offset + 2;
            case NULL_OPCODE:
                sb.append(" (" + SYM_NULL + ")");
                return offset + 1;
            case CAR_OPCODE:
                sb.append(" (" + SYM_CAR + ")");
                return offset + 1;
            case CDR_OPCODE:
                sb.append(" (" + SYM_CDR + ")");
                return offset + 1;
            case ADD_OPCODE:
                sb.append(" (" + SYM_ADD + ")");
                return offset + 1;
            case MUL_OPCODE:
                sb.append(" (" + SYM_MUL + ")");
                return offset + 1;
            default:
                sb.append("?");
                System.out.println("FIX ME: " + ops.getInt(offset));
                return -1;
            }
        }
        public String toString() {
            StringBuilder sb = new StringBuilder();
            int max = ops.size();
            int i = 0;
            sb.append("(" + SYM_FUNC + " ");
            sb.append(argc);
            //            while (i < max) {
            //                i = decompile(sb, i);
            //            }
            sb.append(" " + ops);
            sb.append(")");
            return sb.toString();
        }
    }
    public static boolean isCode(var code) { return code instanceof LCode; }
    public static LCode asCode(var code) { if (!isCode(code)) error("not executable", code); return (LCode)code; }

    public static class LModule extends var {
        String name;
        var exports;
        HashMap<var,var> globals;
        HashMap<var,Integer> constantsMap;
        var [] constants;
        Class<?> primitives;
        LModule(String name, Class<?> primitives) {
            this.name = name;
            this.primitives = primitives;
            this.exports = NIL;
            this.globals = new HashMap<var,var>();
            this.globals.put(TRUE, TRUE);
            this.globals.put(FALSE, NIL);
            this.constantsMap = new HashMap<var,Integer>();
            this.constants = new var[10];
        }
        LSymbol type() { return SYM_MODULE; }
        public int putConstant(var val) {
            Integer i = constantsMap.get(val);
            if (i == null) {
                i = constantsMap.size();
                constantsMap.put(val, i);
                if (i >= constants.length) {
                    var [] tmp = new var [i * 2];
                    System.arraycopy(constants, 0, tmp, 0, i);
                    constants = tmp;
                }
                constants[i] = val;
            }
            return i;
        }
        //public String toString() { return "<module " + name + " " + hashCode() + ">"; }
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("<module ");
            sb.append(name);
            sb.append(", constants:[");
            boolean first = true;
            int max = constantsMap.size();
            for (int i=0; i<max; i++) {
                var obj = constants[i];
                if (first) { first = false; } else { sb.append(" "); }
                sb.append(obj);
            }
            sb.append("]>");
            return sb.toString();
        }
        var getExports() {
            return exports;
        }
        var getConstant(int idx) {
            return constants[idx];
        }
        var globalValue(var sym) {
            var val = globals.get(sym);
            if (val == null)
                error("Unbound variable: " + sym);
            return val;
        }
        var global(var sym) {
            return globals.get(sym);
        }
        var global(String name) {
            return global(intern(name));
        }
        var setGlobal(LSymbol sym, var value) {
            return globals.put(sym, value);
        }
        var setGlobal(String name, var value) {
            return setGlobal(intern(name), value);
        }
        public static String operatorName(String javaName) {
            if ("eq".equals(javaName))
                return "=";
            else if ("le".equals(javaName))
                return "<=";
            else if ("lt".equals(javaName))
                return "<";
            else if ("ge".equals(javaName))
                return ">=";
            else if ("gt".equals(javaName))
                return ">";
            else if ("plus".equals(javaName))
                return "+";
            else if ("minus".equals(javaName))
                return "-";
            else if ("multiply".equals(javaName))
                return "*";
            else if ("divide".equals(javaName))
                return "/";
            throw error("Unrecognized operator name in primitive: " + javaName);
        }
        public static String primitiveName(String javaName) {
            if (javaName.endsWith("_p"))
                javaName = javaName.substring(0, javaName.length()-2) + "?";
            else if (javaName.endsWith("_bang"))
                javaName = javaName.substring(0, javaName.length()-5) + "!";
            else {
                int i = javaName.indexOf("_to_");
                if (i >= 0)
                    javaName = javaName.substring(0, i) + "->" + javaName.substring(i+4);
            }
            return javaName.replace("_","-");
        }

        static String ellName(String javaName) {
            if (javaName.startsWith("operator_"))
                return operatorName(javaName.substring(9));
            else if (javaName.startsWith("primitive_"))
                return primitiveName(javaName.substring(10));
            else if (javaName.startsWith("primop_"))
                return operatorName(javaName.substring(7));
            else if (javaName.startsWith("prim_"))
                return primitiveName(javaName.substring(5));
            return javaName;
        }

        public void definePrimitives(Object o) {
            Class<?> c = o.getClass();
            //bug: multiple arity methods get redefined, only the last is defined.
            for (Method method : c.getDeclaredMethods()) {
                String name = method.getName();
                int mod = method.getModifiers();
                if (((mod & Modifier.PUBLIC) == Modifier.PUBLIC) && ((mod & Modifier.STATIC) != Modifier.STATIC)) {
                    String pname = null;
                    if (name.startsWith("operator_"))
                        pname = operatorName(name.substring(9));
                    else if (name.startsWith("primitive_"))
                        pname = primitiveName(name.substring(10));
                    if (pname != null) {
                        LSymbol sym = intern(pname);
                        var val = global(sym);
                        if (val != null)
                            println("*** Warning: redefining " + pname);
                        //check the argument signature. Define "primitiveN" differently than "primitive0" .. "primitive3"
                        setGlobal(sym, new LPrimitive(pname, method, o));
                    } else {
                        if (name.startsWith("primop_"))
                            pname = operatorName(name.substring(7));
                        else if (name.startsWith("prim_"))
                            pname = primitiveName(name.substring(5));
                        if (pname != null) {
                            LSymbol sym = intern(pname);
                            var val = global(sym);
                            if (val != null)
                                println("*** Warning: redefining " + pname);
                            //check the argument signature. Define "primitiveN" differently than "primitive0" .. "primitive3"
                            setGlobal(sym, new LPrimitiveN(pname, method, o));
                        }
                    }
                }
            }
        }
    }

    public static LModule module(String name, Class<?> primitivesClass) {
        LModule module = new LModule(name, primitivesClass);
        if (primitivesClass != null) {
            try {
                Object primitives = primitivesClass.newInstance();
                java.lang.reflect.Method meth = primitivesClass.getMethod("init", LModule.class);
                if (meth != null)
                    meth.invoke(primitives, module);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return module;
    }

    public static boolean isModule(var mod) { return mod instanceof LModule; }
    public static LModule asModule(var mod) { if (!isModule(mod)) error("not a module", mod); return (LModule)mod; }

    public static var exec(var thunk) {
        ArrayList<LSymbol> defs = new ArrayList<LSymbol>();
        LVM vm = new LVM();
        LCode code = asCode(thunk);
        var result = vm.exec(code, defs);
        List<var> lst = new ArrayList<var>();
        if (defs.size() > 0) {
            HashSet<LSymbol> syms = new HashSet<LSymbol>();
            for (LSymbol s : defs) {
                if (!syms.contains(s)) {
                    syms.add(s);
                    lst.add(s);
                }
            }
            code.module.exports = makeList(lst); //fix this. This assumes the *only* execution in a module is this one.
        } else
            code.module.exports = NIL;
        return result;
    }

    //-----

    private static final class Frame {
        Frame previous;
        int pc;
        int ops[];
        Frame locals;
        var elements[];
        LModule module;
        var constants[];
        @Override public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("** Frame " + this.hashCode() + ":\n");
            Frame f = this;
            while (f != null) {
                for (var v : elements) {
                    sb.append("  ");
                    if (v instanceof LCode)
                        sb.append(v.getClass().getName());
                    else
                        sb.append(v);
                }
                sb.append("\n");
                f = f.previous;
            }
            return sb.toString();
        }
    }

    static final class LClosure extends LFunction {
        LCode code;
        Frame frame;
        LClosure(LCode c, Frame f) {
            code = c;
            frame = f;
        }
        LClosure(var c, Frame f) {
            this((LCode)c, f);
        }

        @Override public String toString() {
            return "<closure: " + code + ">";
        }
    }

    static class LPrimitive extends LFunction {
        Object impl;
        Method method;
        String name;

        LPrimitive(String name, Method method, Object impl) {
            this.name = name;
            this.method = method;
            this.impl = impl;
        }

        public String toString() {
            return "<primitive " + name + ">";
        }

        public var call(var [] params, int offset, int count) {
            try {
                switch (count) {
                case 0:
                    return (var)method.invoke(impl);
                case 1:
                    return (var)method.invoke(impl, params[offset]);
                case 2:
                    return (var)method.invoke(impl, params[offset], params[offset+1]);
                case 3:
                    return (var)method.invoke(impl, params[offset], params[offset+1], params[offset+2]);
                case 4:
                    return (var)method.invoke(impl, params[offset], params[offset+1], params[offset+2], params[offset+3]);
                case 5:
                    return (var)method.invoke(impl, params[offset], params[offset+1], params[offset+2], params[offset+3], params[offset+4]);
                default:
                    error("too many arguments: " + count);
                    return null;
                }
            } catch (error e) {
                throw e;
            } catch (Exception e) {
                throw error("while executing " + LModule.ellName(method.getName()) + " : " + e.getMessage());
            }
        }
    }

    static class LPrimitiveN extends LPrimitive {
        LPrimitiveN(String name, Method method, Object impl) {
            super(name, method, impl);
        }
        public var call(var [] params, int offset, int count) {
            try {
                return (var)method.invoke(impl, params, offset, count);
            } catch (error e) {
                throw e;
            } catch (Exception e) {
                throw error("while executing " + LModule.ellName(method.getName()) + " : " + e.getMessage());
            }
        }
    }

    static class LVM {

        var stack[]; //the stack. It is shared with all vm activity to pass data around
        int sp; //the pointer into the stack. Starts at the top of the stack (it is a push-down stack)

        int [] ops; //the current ops to execute out of
        int pc; //the program counter
        Frame environment; //the local variable frame and previous frame state
        var [] constants; //the constant pool for current ops to use
        LModule module; //the module the constants and symboltable use.

        List<LSymbol> defs; //this is for capturing new global defs when loading a file, for module mgt purposes. Nothing else.

        LVM() {
            stack = new var[16000];
        }

        //        var exec(LCode code) {
        //            return exec(code, null);
        //        }
        String showArray(var [] ary) {
            String s = "[";
            if (ary != null) {
                boolean first = true;
                for (var v : ary) {
                    if (first) { first = false; } else { s += " "; }
                    s += v;
                }
            }
            s += "]";
            return s;
        }
		
        String showEnv(Frame f) {
            String s = "";
            while (true) {
                s += showArray(f.elements);
                if (f.locals == null) {
                    break;
                }
                f = f.locals;
            }
            return s;
        }
        String showStack(var [] stack, int sp) {
            int end = stack.length;
            String s = "[";
            while (sp < end) {
                s = s + " " + stack[sp++];
            }
            return s + " ]";
        }
        String showOps(int [] ops) {
            String s = "[";
            for (int i : ops) {
                s = s + " " + i;
            }
            return s + " ]";
        }
		
        var exec(LCode code, List<LSymbol> collectDefs) {
            var tmp;
            double tmpnum;
            boolean trace = false;
            if (trace) System.err.println("------------------ BEGIN EXECUTION of " + code.module);
            defs = collectDefs;
            sp = stack.length;

            environment = new Frame();
            environment.previous = null;
            environment.ops = null;
            environment.pc = 0;
            environment.locals = null;
            environment.elements = null;
            environment.module = null;
            environment.constants = null;

            module = code.module;
            constants = module.constants;
            ops = code.ops.elements;
            if (trace) System.out.println(" ops: " + showOps(ops));
            pc = 0;
            while (true) {
                try {
                    while (true) {
                        switch (ops[pc]) {
                        case LITERAL_OPCODE:
                            if (trace) System.err.println("const\t" + constants[ops[pc+1]]);
                            stack[--sp] = constants[ops[pc+1]];
                            pc += 2;
                            break;
                        case GLOBAL_OPCODE:
                            if (trace) System.err.println("glob\t" + constants[ops[pc+1]]);
                            stack[--sp] = module.globals.get(constants[ops[pc+1]]);
                            if (stack[sp] == null)
                                error("Unbound variable: " + constants[ops[pc+1]]);

                            pc += 2;
                            break;
                        case DEFGLOBAL_OPCODE:
                            if (trace) System.err.println("defglob\t" + constants[ops[pc+1]]);
                            defGlobal(ops[pc+1], stack[sp]);
                            pc += 2;
                            break;
                        case CALL_OPCODE:
                            if (trace) System.err.println("call\t" + ops[pc+1]);
                            //check_stack();
                            funcall(stack[sp++], ops[pc+1], pc+2);
                            break;
                        case TAILCALL_OPCODE:
                            if (trace) System.err.println("tcall\t" + ops[pc+1]);
                            tailcall(stack[sp++], ops[pc+1]);
                            break;
                        case RETURN_OPCODE:
                            if (trace) System.err.println("ret");
                            if (environment.previous == null) {
                                //if (trace) System.err.println("------------------ END EXECUTION of " + code.module);
                                return stack[sp];
                            }
                            ops = environment.ops;
                            pc = environment.pc;
                            module = environment.module;
                            //constants = environment.constants;
                            constants = module.constants;
                            environment = environment.previous;
                            break;
                        case LOCAL_OPCODE:
                            if (trace) System.err.println("getloc\t" + ops[pc+1] + " " + ops[pc+2]);
                            {
                                Frame tmpEnv = environment;
                                int i = ops[pc+1];
                                while (i > 0) {
                                    tmpEnv = tmpEnv.locals;
                                    i--;
                                }
                                stack[--sp] = tmpEnv.elements[ops[pc+2]];
                            }
                            //if (trace) System.out.println(pc + "\t" + sp + "\t  => " + stack[sp]);
                            pc += 3;
                            break;
                        case SETLOCAL_OPCODE:
                            if (trace) System.err.println("setloc\t" + ops[pc+1] + " " + ops[pc+2]);
                            {
                                Frame tmpEnv = environment;
                                int i = ops[pc+1];
                                while (i > 0) {
                                    tmpEnv = tmpEnv.locals;
                                    i--;
                                }
                                tmpEnv.elements[ops[pc+2]] = stack[sp];
                            }
                            pc += 3;
                            break;
                        case POP_OPCODE:
                            if (trace) System.err.println("pop");
                            sp++;
                            pc += 1;
                            break;
                        case CLOSURE_OPCODE:
                            if (trace) System.err.println("closure\t" + constants[ops[pc+1]]);
                            stack[--sp] = closure(ops[pc+1], environment);
                            pc += 2;
                            break;
                        case JUMPFALSE_OPCODE:
                            if (trace) System.err.println("fjmp\t" + ops[pc+1]);
                            if (stack[sp++] == FALSE)
                                pc += ops[pc+1];
                            else
                                pc += 2;
                            break;
                        case JUMP_OPCODE:
                            if (trace) System.err.println("jmp\t" + ops[pc+1]);
                            pc += ops[pc+1];
                            break;
                        case USE_OPCODE:
                            if (trace) System.err.println("use\t" + constants[ops[pc+1]]);
                            if (trace) System.err.println(" -> pc before: " + pc + ", ops:" + showOps(ops));
                            useModule(constants[ops[pc+1]]);
                            if (trace) System.err.println(" -> pc after: " + pc + ", ops:" + showOps(ops));
                            pc += 2;
                            break;
                        case CAR_OPCODE:
                            if (trace) System.err.println("car");
                            stack[sp] = car(stack[sp]);
                            pc += 1;
                            break;
                        case CDR_OPCODE:
                            if (trace) System.err.println("cdr");
                            stack[sp] = cdr(stack[sp]);
                            pc += 1;
                            break;
                        case NULL_OPCODE:
                            if (trace) System.err.println("null");
                            stack[sp] = (stack[sp] == NIL)? TRUE : FALSE;
                            pc += 1;
                            break;
                        case ADD_OPCODE:
                            if (trace) System.err.println("add");
                            tmpnum = doubleValue(stack[sp++]);
                            stack[sp] = number(tmpnum + doubleValue(stack[sp]));
                            pc += 1;
                            break;
                        case MUL_OPCODE:
                            if (trace) System.err.println("mul");
                            tmpnum = doubleValue(stack[sp++]);
                            stack[sp] = number(tmpnum * doubleValue(stack[sp]));
                            pc += 1;
                            break;
                        default:
                            throw error("Bad instruction: " + ops[pc]);
                        }
                    }
                } catch (Exception e) {
                    //if the restart continuation is bound, then jump to it.
                    //else
                    //e.printStackTrace();
                    System.err.println("*** " + e);
                    //if (trace) System.err.println("------------------ ABORT EXECUTION");
                    return null;
                }
            }
        }

        void funcall(var fun, int argc, int savedPc) {
            if (fun instanceof LClosure) {
                LClosure closure = (LClosure)fun;
                Frame f = new Frame();
                f.previous = environment;
                f.pc = savedPc;
                f.ops = ops;
                f.module = module;
                f.constants = constants;
                f.locals = closure.frame;
                if (closure.code.argc >= 0) {
                    if (argc != closure.code.argc)
                        error("Wrong number of args (" + argc + ") to " + closure);
                    f.elements = new var[argc];
                    if (argc > 0) {
                        System.arraycopy(stack, sp, f.elements, 0, argc);
                        sp += argc;
                    }
                } else { //rest args
                    int nMinArgc = -closure.code.argc - 1;
                    if (argc < nMinArgc)
                        error("Wrong number of args (" + argc + ") to " + closure);
                    f.elements = new var[nMinArgc + 1];
                    System.arraycopy(stack, sp, f.elements, 0, nMinArgc);
                    sp += argc;
                    int nRest = argc - nMinArgc;
                    int j = sp;
                    var lstRest = NIL;
                    while (nRest-- > 0)
                        lstRest = cons(stack[--j], lstRest);
                    f.elements[nMinArgc] = lstRest;
                }
                environment = f;
                ops = closure.code.ops.elements;
                module = closure.code.module;
                constants = module.constants;
                pc = 0;
            } else if (fun instanceof LPrimitive) {
                var o = ((LPrimitive)fun).call(stack, sp, argc);
                sp = sp + argc - 1;
                stack[sp] = o;
                pc = savedPc;
            } else if (fun instanceof LKeyword) {
                if (argc != 1)
                    error("wrong number of arguments to keyword (must be 1)");
                stack[sp] = get(stack[sp], asKeyword(fun));
            } else {
                System.out.println("fun is a " + fun);
                error("Unhandled case in funcall");
            }
        }

        final void tailcall(var fun, int argc) {
            if (fun instanceof LClosure) {
                LClosure closure = (LClosure)fun;
                Frame newEnv = new Frame();
                newEnv.previous = environment.previous;
                newEnv.pc = environment.pc;
                newEnv.ops = environment.ops;
                newEnv.module = environment.module;
                newEnv.constants = environment.constants;
                newEnv.locals = closure.frame;
                if (closure.code.argc >= 0) {
                    if (argc != closure.code.argc)
                        error("Wrong number of args (" + argc + ") to " + fun);
                    newEnv.elements = new var[argc];
                    System.arraycopy(stack, sp, newEnv.elements, 0, argc);
                    sp += argc;
                } else {
                    int nMinArgc = -closure.code.argc - 1;
                    if (argc < nMinArgc)
                        error("Wrong number of args (" + argc + ") to " + fun);
                    newEnv.elements = new var[nMinArgc + 1];
                    System.arraycopy(stack, sp, newEnv.elements, 0, nMinArgc);
                    sp += nMinArgc;
                    var lstRest = NIL;
                    int j = sp;
                    while (argc-- > nMinArgc)
                        lstRest = cons(stack[--j], lstRest);
                    newEnv.elements[nMinArgc] = lstRest;
                }
                ops = closure.code.ops.elements;
                module = closure.code.module;
                constants = module.constants;
                pc = 0;
                environment = newEnv;
            } else if (fun instanceof LPrimitive) {
                LPrimitive nProc = (LPrimitive)fun;
                var o = ((LPrimitive)fun).call(stack, sp, argc);
                sp = sp + argc - 1;
                stack[sp] = o;
                pc = environment.pc;
                ops = environment.ops;
                module = environment.module;
                constants = environment.constants;
                environment = environment.previous;
            } else if (fun instanceof LKeyword) {
                if (argc != 1)
                    error("wrong number of arguments to keyword (must be 1)");
                stack[sp] = get(stack[sp], asKeyword(fun));
                pc = environment.pc;
                ops = environment.ops;
                module = environment.module;
                constants = environment.constants;
                environment = environment.previous;
            } else
                error("Unhandled case in tailcall: " + fun);
        }

        private final var closure(int i, Frame env) {
            //could optimise by having a dedicated LCode constant pool, only LCode are candidates at compile time.
            return new LClosure((LCode)constants[i], env);
        }

        private final var getGlobal(int i) {
            var sym = constants[i];
            //var val = module.global(sym);
            var val = module.globals.get(sym);
            if (val == null)
                error("Unbound variable: " + sym);
            return val;
        }

        private final void defGlobal(int i, var val) {
            LSymbol sym = (LSymbol)constants[i];
            module.setGlobal(sym, val);
            if (defs != null)
                defs.add(sym);
        }

        private final void setGlobal(int i, var val) {
            LSymbol sym = (LSymbol)constants[i];
            var prev = module.global(sym);
            if (prev == null)
                error("Unbound variable: " + sym);
            module.setGlobal(sym, val);
        }

        private void useModule(var sym) {
            String filename = symbolName(sym);
            LCode thunk = asCode(loadModule(filename, module.primitives));
            var result = Runtime.exec(thunk);
            var exports = thunk.module.getExports();
            while (exports != NIL) {
                LSymbol export = asSymbol(car(exports));
                var val = thunk.module.global(export);
                module.setGlobal(export, val);
                exports = cdr(exports);
            }
        }

    }

    public static var runModule(String name, Class<?> primitives) {
        var code = loadModule(name, primitives);
        if (verbose) println("; begin execution");
        var result = exec(code);
        if (verbose) {
            if (result != null) {
                println("; => " + result);
            }
        }
        return result;
    }

    public static var loadModule(String name, Class<?> primitives) {
        var f = (name.indexOf('.') > 0)? file(name) : findModule(name);
        if (f == NIL)
            error("module not found: " + name);
        return loadModule(name, f, primitives);
    }

    static String [] getPath() {
        String spath = System.getenv("ELL_PATH");
        if (spath == null) spath = ".:src/main/ell";
        return spath.split(":");
    }

    public static var findModule(String moduleName) {
        String name = (moduleName.endsWith(".ell") || moduleName.endsWith(".lap"))? moduleName : moduleName + ".ell";
        if (name.startsWith("..") || name.startsWith("/")) {
            return file(name);
        }
        String [] path = getPath();
        for (String dirname : path) {
            File dir = new File(dirname);
            if (dir.exists()) {
                for (String f : dir.list()) {
                    if (name.equals(f))
                        return file(new File(dir, f).toString());
                }
            }
        }
        return null;
    }

    public static var loadModule(String moduleName, var f, Class<?> primitives) {
        if (verbose) println("; loadModule: " + moduleName + " from " + f);
        LModule module = module(moduleName, primitives);
        var channel = open(f, READ);
        var source = list(intern("begin"));
        var expr = read(channel);
        while (expr != EOI) {
            source = concat(source, list(expr));
            expr = read(channel);
        }
        close(channel);
        if (verbose) println("; read: " + write(source));
        var code = new Compiler(module).compile(source);
        if (verbose) {
            println("; compiled to: " + write(code));
            println("; module: " + module);
        }
        return code;
    }

}
