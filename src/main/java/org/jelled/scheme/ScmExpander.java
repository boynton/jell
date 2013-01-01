package org.jelled.scheme;
import org.jelled.core.*;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;

public class ScmExpander {

    private final LVM vm;
    
    public ScmExpander(LVM vm) {
        this.vm = vm;
    }
    
    private ISequence cons(Object o, ISequence s) {
        return new LList(o, s);
    }

    private Object car(ISequence lst) {
        return lst.first();
    }
    private ISequence cdr(ISequence lst) {
        return lst.next();
    }
    private Object cadr(ISequence lst) {
        ISequence rest = lst.next();
        if (rest == null)
            return null;
        return rest.first();
    }
    private ISequence cddr(ISequence lst) {
        ISequence rest = lst.next();
        if (rest == null)
            return null;
        return rest.next();
    }
    private Object caddr(ISequence lst) {
        return car(cddr(lst));
    }
    private ISequence cdddr(ISequence lst) {
        return cdr(cddr(lst));
    }
    
    public Object expand(Object expr) {
        if (expr instanceof LList) {
            LList lst = (LList)expr;
            Object sym = car(lst);
            if (sym instanceof LSymbol) {
                String name = ((LSymbol)sym).name;
                if (name.equals("define"))
                    return LList.create(vm.environment().SYM_DEF, (LSymbol)cadr(lst), expand(caddr(lst)));
                else if (name.equals("lambda"))
                    return cons(vm.environment().SYM_FN, cons(expandFunArgs(cadr(lst)), expandSequence(cddr(lst))));
                else if (name.equals("begin"))
                    return cons(vm.environment().SYM_DO, expandSequence(cdr(lst)));
            }
            return expandSequence(lst);
        }
        return expr;
    }

    private ISequence expandSequence(ISequence expr) {
        int len = expr.count();
        if (len > 0) {
            ArrayList<Object> al = new ArrayList<Object>();
            for (Object o : expr) {
                al.add(expand(o));
            }
            return LList.fromArrayList(al);
        }
        return expr;
    }

    private Object expandFunArgs(Object args) {
        return args;
    }

	//expand dotted pairs into lists with a dot symbol in them
	//and make then length primitive check for a dot in the second-to-last position, throw an error.
	//also fix cdr and set-cdr! to be aware of the dot.
	
}

