package org.jelled.core;
//
//note: this compiler generates offsets into a constant pool that is defined by the VM instance.
// that is, it is tightly coupled with the VM instance. Same goes for symboltable.
// Would be nice to have the code not use statics, except for obvious constants like OPCODES.
//
public class LCompiler  {
    
    final LVM vm; //just to register constants and to get opcodes
    final LEnvironment env;

    final LSymbol DOT;
    final LSymbol AMP;

    final LSymbol SYM_QUOTE;
    final LSymbol SYM_FN;
    final LSymbol SYM_DO;
    final LSymbol SYM_IF;
    final LSymbol SYM_DEF;
    final LSymbol SYM_SET;

    public LCompiler(LVM vm) {
        this.vm = vm;
        this.env = vm.environment();
        this.DOT = env.SYM_DOT;
        this.AMP = env.SYM_AMP;
        this.SYM_QUOTE = env.SYM_QUOTE;
        this.SYM_FN = env.SYM_FN;
        this.SYM_DO = env.SYM_DO;
        this.SYM_IF = env.SYM_IF;
        this.SYM_DEF = env.SYM_DEF;
        this.SYM_SET = env.SYM_SET;
    }

    int constantIndex(Object c) {
        //this is the one tight coupling between the vm and the compiler.
        return vm.registerConstant(c);
    }

    String moduleName;
    int opArray[];
    int opCount;

    void syntaxError(Object expr) {
        error("Syntax error: " + expr);
    }

    void error(Object expr) {
        throw new LError("Error: " + expr);
    }
    
    private final Object first(ISequence seq) {
        return seq.first();
    }
    private final Object second(ISequence seq) {
        return seq.next().first(); //! NPE if the sequence doesn't have at least 2 elements
    }
    private final Object third(ISequence seq) {
        return seq.next().next().first(); //! NPE if the sequence doesn't have at least 3 elements
    }
    private final ISequence rest(ISequence seq) {
        if (seq == null)
            return null;
        return seq.next();
    }

    void encode(int data) {
        if (opCount == opArray.length) {
            int p[] = new int[2 * opArray.length];
            System.arraycopy(opArray, 0, p, 0, opArray.length);
            opArray = p;
        }
        opArray[opCount++] = data;
    }

    /*        
    int calculateLocation(LSymbol sym, ISequence frame) {
        int result = _calculateLocation(sym, frame);
        System.out.println("calculateLocation for " + sym + ": " + frame + " => " + result);
        return result;
    }
    */
    int calculateLocation(LSymbol sym, ISequence frame) {
        int i = 0;
        while (frame != null) {
            int j = 0;
            ISequence e = (ISequence)frame.first();
            while (e != null) {
                Object o = e.first();
                if (o == sym)
                    return ((i << 16) + (j & 65535));
                if (o == DOT || o == AMP) {
                    e = e.next();
                    o = e.first();
                }
                j++;
                e = e.next();
            }
            if (e == sym) //a "rest" argument
                return (i << 16) + (j & 65535);
            i++;
            frame = frame.next();
        }
        return -1;
    }
        
    void compileObject(Object obj) {
        int i = constantIndex(obj);
        encode(vm.OPCODE_CONSTANT);
        encode(i);
    }
        
    void compileSequence(ISequence exprs, ISequence env, boolean bTail, boolean bIgnoreResult) {
        if (exprs != null) {
            ISequence next = exprs.next();
            while (next != null) {
                compileExpr(exprs.first(), env, false, true);
                exprs = next;
                next = exprs.next();
            }
            compileExpr(exprs.first(), env, bTail, bIgnoreResult);
        }
    }
        
    int calculateArgc(Object args) {
        int i = 0;
        Object rest = null;
        if (args == null)
            return 0;
        else if (args instanceof LSymbol)
            return -1;
        else {
            ISequence argseq = (ISequence)args;
            while (argseq != null) {
                Object arg = argseq.first();
                if (arg instanceof LSymbol) {
                    if (DOT == arg || AMP == arg) {
                        rest = second(argseq);
                        if (!(rest instanceof LSymbol))
                            error("Invalid argument list: " + args);
                        argseq = null;
                        break;
                    }
                } else {
                    error("Formal argument not a symbol: " + arg);
                }
                i++;
                argseq = argseq.next();
            }
            if (argseq == null)
                return i;
            else //a symbol for the "rest" argument
                return -i-1;
        }
    }
        
    void compileLambda(Object args, ISequence body, ISequence frame, boolean bTail, boolean bIgnoreResult) {
        int oldOps[] = opArray;
        int oldCount = opCount;
        int argc = calculateArgc(args);
        LList newFrame = new LList(args, frame);
        opArray = new int[100];
        opCount = 0;
        compileSequence(body, newFrame, true, false);
        LCode newProc = new LCode(opArray, opCount, moduleName, argc);
        opArray = oldOps;
        opCount = oldCount;
        if (!bIgnoreResult) {
            int i = constantIndex(newProc);
            encode(vm.OPCODE_CLOSURE);
            encode(i);
            if (bTail) encode(vm.OPCODE_RETURN);
        }
    }
        
    void compileIfElse(Object pred, Object consequent, ISequence antecedent, ISequence frame, boolean bTail, boolean bIgnoreResult) {
        int loc1, loc2=0;
        int i;
        Object ant = (antecedent != null)? antecedent.first() : null;
        compileExpr(pred, frame, false, false);
        encode(vm.OPCODE_JUMPFALSE);
        loc1 = opCount;
        encode(0);
        compileExpr(consequent, frame, bTail, bIgnoreResult);
        if (!bTail) {
            encode(vm.OPCODE_JUMP);
            loc2 = opCount;
            encode(0);
        }
        opArray[loc1] = opCount - loc1 + 1;
            
        compileExpr(ant, frame, bTail, bIgnoreResult);
        if (!bTail)
            opArray[loc2] = opCount - loc2 + 1;
    }
        
    void compileArgs(ISequence args, ISequence frame) {
        if (args != null) {
            compileArgs(args.next(), frame);
            compileExpr(args.first(), frame, false, false);
        }
    }
        
    void compileFuncall(Object fun, ISequence args, ISequence frame, boolean bTail, boolean bIgnoreResult) {
        int argc = length(args);
        if (argc < 0)
            syntaxError(new LList(fun, args));
        compileArgs(args, frame);
        compileExpr(fun, frame, false, false);
        if (bTail) {
            encode(vm.OPCODE_TAILCALL);
            encode(argc);
        } else {
            encode(vm.OPCODE_CALL);
            encode(argc);
            if (bIgnoreResult)
                encode(vm.OPCODE_POP);
        }
    }
        
    int length(ISequence seq) {
        if (seq == null)
            return 0;
        return seq.count();
    }
	
    void compileExpr(Object expr, ISequence frame, boolean bTail, boolean bIgnoreResult) {
        if (expr instanceof LSymbol) {
            if (!bIgnoreResult) {
                int i = calculateLocation((LSymbol)expr, frame);
                if (i != -1) {
                    encode(vm.OPCODE_LOCAL);
                    encode(i >> 16);
                    encode(i & 65535);
                } else {
                    i = constantIndex(expr);
                    encode(vm.OPCODE_GLOBAL);
                    encode(i);
                }
                if (bTail) encode(vm.OPCODE_RETURN);
            }
        } else if (expr instanceof LList) {
            LList lst = (LList)expr;
            int n = length(lst);
            if (n == 0) {
                compileObject(expr);
            } else {
                Object fun = lst.first();
                if (fun == SYM_QUOTE) {
                    if (n == 2) {
                        if (!bIgnoreResult) {
                            compileObject(second(lst));
                            if (bTail) encode(vm.OPCODE_RETURN);
                        }
                    } else
                        syntaxError(expr);
                } else if (fun == SYM_DO) {
                    compileSequence(lst.rest(), frame, bTail, bIgnoreResult);
                } else if (fun == SYM_IF) {
                    if (n == 3 || n == 4)
                        compileIfElse(second(lst), third(lst), rest(rest(rest(lst))), frame, bTail, bIgnoreResult);
                    else
                        syntaxError(expr);
                } else if (fun == SYM_DEF) {
                    if (n == 3) {
                        int i = constantIndex(second(lst));
                        String sOldModuleName = moduleName;
                        moduleName = second(lst).toString();
                        compileExpr(third(lst), frame, false, false);
                        moduleName = sOldModuleName;
                        encode(vm.OPCODE_DEF_GLOBAL);
                        encode(i);
                        if (bIgnoreResult)
                            encode(vm.OPCODE_POP);
                        else if (bTail)
                            encode(vm.OPCODE_RETURN);
                    } else
                        syntaxError(expr);
                } else if (fun == SYM_SET) {
                    if (n == 3) {
                        compileExpr(third(lst), frame, false, false);
                        int i = calculateLocation((LSymbol)second(lst), frame);
                        if (i != -1) {
                            encode(vm.OPCODE_SET_LOCAL);
                            encode(i >> 16);
                            encode(i & 65535);
                        } else {
                            i = constantIndex(second(lst));
                            encode(vm.OPCODE_SET_GLOBAL);
                            encode(i);
                        }
                        if (bIgnoreResult)
                            encode(vm.OPCODE_POP);
                        else if (bTail)
                            encode(vm.OPCODE_RETURN);
                    } else
                        syntaxError(expr);
                } else if (fun == SYM_FN) {
                    if (n >= 3) {
                        Object arglist = second(lst);
                        ISequence body = rest(rest(lst));
                        compileLambda(arglist, body, frame, bTail, bIgnoreResult);
                    } else
                        syntaxError(expr);
                } else {  /* a function call */
                    compileFuncall(fun, lst.rest(), frame, bTail, bIgnoreResult);
                }
            }
        } else { /*	 a literal */
            if (!bIgnoreResult) {
                compileObject(expr);
                if (bTail) encode(vm.OPCODE_RETURN);
            }
        }
    }
        
    public LCode compile(Object expr) {
        moduleName = null;
        opArray = new int[100];
        opCount = 0;
        compileExpr(expr, null, false, false);
        encode(vm.OPCODE_RETURN);
        LCode tmp = new LCode(opArray, opCount, moduleName, 0);
        opArray = null;
        return tmp;
    }

}
