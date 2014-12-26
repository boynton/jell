package org.jelled;
import static org.jelled.Runtime.*;
import java.util.HashMap;
import java.io.File;

public class Compiler {

    static final var SYM_LAP = intern("lap");
    static final var SYM_BEGIN = intern("begin");
    static final var SYM_QUOTE = intern("quote");
    static final var SYM_FUN = intern("lambda");
    static final var SYM_DEF = intern("define");
    static final var SYM_IF = intern("if");
    static final var SYM_SET = intern("set!");

    static final var SYM_CAR = intern("car");
    static final var SYM_CDR = intern("cdr");
    static final var SYM_SET_CDR_BANG = intern("set-cdr!");

    static final var SYM_NULLP = intern("null?");
    static final var SYM_ADD = intern("+");
    static final var SYM_MUL = intern("*");

    LModule module;

    Compiler(LModule module) {
        this.module = module;
    }

    public var compile(var expr) {
        LCode code = new LCode(asModule(module), 0);
        var env = NIL;
        if (isList(expr)) {
            if (car(expr) == SYM_LAP) {
                code.loadOps(cdr(expr));
                return code;
            }
        }
        compileExpr(code, env, expr, false, false);
        code.emitReturn();
        return code;
    }


    private void compileExpr(LCode code, var env, var expr, boolean bTail, boolean bIgnoreResult) {
        if (isSymbol(expr)) {
            int [] loc = {0, 0};
            if (calculateLocation(loc, expr, env)) {
                code.emitLocal(loc[0], loc[1]);
            } else {
                code.emitGlobal(expr);
            }
            if (bIgnoreResult) code.emitPop();
            else if (bTail) code.emitReturn();
        } else if (isList(expr)) {
            LList lst = asList(expr);
            int len = length(lst);
            if (len == 0)
                error("Cannot compile empty list: " + expr); //if it was nil, on the other hand, we could
            var fn = car(lst);
            if (SYM_QUOTE == fn) {
                if (len != 2)
                    error("Syntax error: " + expr);
                if (!bIgnoreResult) {
                    code.emitLiteral(cadr(lst));
                    if (bTail) code.emitReturn();
                }
            } else if (SYM_BEGIN == fn) { //the begin special form
                compileSequence(code, env, cdr(lst), bTail, bIgnoreResult);
            } else if (SYM_LAP == fn) { //the lap special form -> already compiled code
                code.loadOps(cdr(lst));
            } else if (SYM_IF == fn) {
                if (len == 3 || len == 4)
                    compileIfElse(code, env, cadr(lst), caddr(lst), cdddr(lst), bTail, bIgnoreResult);
                else
                    error("Syntax error: " + expr);
            } else if (SYM_DEF == fn) {
                if (len != 3)
                    error("syntax error: " + expr);
                var tmp = cadr(lst);
                if (!isSymbol(tmp))
                    error("syntax error: " + expr);
                LSymbol sym = asSymbol(tmp);
                //scope so we pick up "context" from current global def for syntax errors? sModuleName = lst.second()
                compileExpr(code, env, caddr(lst), false, false);
                //sModuleName = sOldModuleName;
                //check if we are at the outermost lexical frame!!!!!
                code.emitDefGlobal(sym);
                if (bIgnoreResult) code.emitPop();
                else if (bTail) code.emitReturn();
            } else if (SYM_FUN == fn) { //create a closure
                if (len < 3)
                    error("syntax error for function: " + expr);
                var body = cddr(lst);
                var args = cadr(lst);
                if (args != NIL && !isList(args))
                    error("invalid function formal argument list: " + args);
                compileLambda(code, env, args, body, bTail, bIgnoreResult);
            } else if (SYM_SET == fn) {
                int [] loc = {0, 0};
                var tmp = cadr(lst);
                if (!isSymbol(tmp))
                    error("syntax error: " + expr);
                LSymbol sym = asSymbol(tmp);
                compileExpr(code, env, caddr(lst), false, false);
                if (calculateLocation(loc, sym, env)) {
                    code.emitSetLocal(loc[0], loc[1]);
                } else {
                    code.emitDefGlobal(cadr(expr)); //should be setglobal
                }
                if (bIgnoreResult) code.emitPop();
                else if (bTail) code.emitReturn();
            } else {
                compileFuncall(code, env, fn, cdr(lst), bTail, bIgnoreResult);
            }
        } else {
            if (!bIgnoreResult) {
                code.emitLiteral(expr);
                if (bTail)
                    code.emitReturn();
            }
        }
    }

    //FIX: figure out argument binding mode. This affects how a new frame is built
    // (x y) -> {argc: 2} -> we know in advance how many args
    // (x & z) ...) -> {argc: 1 rest: z} -> we know in advance how many args in frame, but must bind list
    // (x [y z]) ...) -> {argc: 1 rest: [y z]} - at runtime, bind extra args if present into frame
    // (x {y: 23 z: 57}) -> {argc: 1 rest: {y: 23 z:57}} - at runtime, parse keyargs, bind into frame
    private void compileLambda(LCode callingCode, var env, var args, var body, boolean bTail, boolean bIgnoreResult) {
        int argc = 0;
        var rest = NIL;
        var tmp = args;
        //to do: deal with rest, optional, and keywords arguments
        while (tmp != NIL) {
            if (!isSymbol(car(tmp)))
                error("Formal argument is not a symbol: " + car(tmp) + " -> " + tmp.getClass());
            argc++;
            tmp = cdr(tmp);
        }
        var newEnv = cons(args, env);
        LCode code = new LCode(module, argc, rest);
        compileSequence(code, newEnv, body, true, false);
        if (!bIgnoreResult) {
            callingCode.emitClosure(code);
            if (bTail)
                callingCode.emitReturn();
        }
    }

    private boolean compilePrimopCall(LCode code, var fn, int argc, boolean bTail, boolean bIgnoreResult) {
        boolean b = false;
        if (equal(SYM_CAR, fn) && argc == 1) {
            code.emitCar();
            b = true;
        }
        else if (equal(SYM_CDR, fn) && argc == 1) {
            code.emitCdr();
            b = true;
        }
        else if (equal(SYM_NULLP, fn) && argc == 1) {
            code.emitNullP();
            b = true;
        }
        else if (equal(SYM_ADD, fn) && argc == 2) {
            code.emitAdd();
            b = true;
        }
/*
        else if (equal(SYM_MUL, fn) && argc == 2) {
            code.emitMul();
            b = true;
        }
*/
        if (b) {
            if (bTail)
                code.emitReturn();
            else if (bIgnoreResult)
                code.emitPop();
        }
        return b;
    }

    private void compileFuncall(LCode code, var env, var fn, var args, boolean bTail, boolean bIgnoreResult) {
        int argc = length(args);
        if (argc < 0)
            error("bad funcall: (" + fn + " " + args);
        compileArgs(code, env, args);
        if (compilePrimopCall(code, fn, argc, bTail, bIgnoreResult)) return;
        compileExpr(code, env, fn, false, false);
        if (bTail) {
            if (false) {
                code.emitCall(argc);
                code.emitReturn();
            } else {
                code.emitTailCall(argc);
            }
        } else {
            code.emitCall(argc);
            if (bIgnoreResult)
                code.emitPop();
        }
    }

    private void compileArgs(LCode code, var env, var args) {
        if (args != NIL) {
            compileArgs(code, env, cdr(args));
            compileExpr(code, env, car(args), false, false);
        }
    }

    private void compileSequence(LCode code, var env, var exprs, boolean bTail, boolean bIgnoreResult) {
        if (exprs != NIL) {
            while (cdr(exprs) != NIL) {
                compileExpr(code, env, car(exprs), false, true);
                exprs = cdr(exprs);
            }
            compileExpr(code, env, car(exprs), bTail, bIgnoreResult);
        }
    }

    private void compileIfElse(LCode code, var env, var predicate, var consequent, var antecedentOptional, boolean bTail, boolean bIgnoreResult) {
        var antecedent = (antecedentOptional == NIL)? NIL : car(antecedentOptional);
        compileExpr(code, env, predicate, false, false);
        int loc1 = code.emitJumpFalse(0); //returns the location just *after* the jump. setJumpLocation knows this.
        compileExpr(code, env, consequent, bTail, bIgnoreResult);
        int loc2 = bTail? 0 : code.emitJump(0);
        code.setJumpLocation(loc1);
        compileExpr(code, env, antecedent, bTail, bIgnoreResult);
        if (!bTail)
            code.setJumpLocation(loc2);
    }

    private boolean calculateLocation(int [] loc, var sym, var env) {
        int i = 0;
        while (env != NIL) {
            int j = 0;
            var e = car(env);
            while (e != NIL) {
                LList ee = asList(e);
                if (car(ee) == sym) {
                    loc[0] = i;
                    loc[1] = j;
                    return true;
                }
                j++;
                e = cdr(ee);
            }
            if (e == sym) { //a "rest" argument
                loc[0] = i;
                loc[1] = j;
                return true;
            }
            i++;
            env = cdr(env);
        }
        return false;
    }

}
