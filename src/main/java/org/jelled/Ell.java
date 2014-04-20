package org.jelled;

public class Ell extends Runtime {

    public static void main(String [] args) {
        if (args.length > 0) {
            runModule(args[0], new EllPrimitives());
        } else {
            println("REPL NYI. Provide a filename");
        }
    }

    public static class EllPrimitives {
        LModule env;

        private EllPrimitives() { }

		public void init(LModule mod) {
			this.env = mod;
			mod.definePrimitives(this);
		}
        public var prim_println(var [] s, int sp, int argc) {
			StringBuilder sb = new StringBuilder();
			int end = sp + argc;
			while (sp < end)
				sb.append(s[sp++]);
            System.out.println(sb.toString());
            return NIL;
        }
        public var primitive_list(var obj1, var obj2) {
            return list(obj1, obj2);
        }
        public var primitive_quotient(var obj1, var obj2) {
            return number(longValue(obj1) / longValue(obj2));
        }
        public var primitive_modulo(var n1, var n2) {
            //FIX: this is actually the remainder, not modulo
            //return number(longValue(n1) % longValue(n2));
            return number(doubleValue(n1) % doubleValue(n2));
        }
        public var primitive_remainder(var n1, var n2) {
            return number(longValue(n1) % longValue(n2));
        }
        public var operator_plus(var n1, var n2) {
            return number(longValue(n1) + longValue(n2));
        }
        public var xprimop_plus(var [] s, int sp, int argc) {
			double n = 0;
			int end = sp + argc;
			while (sp < end)
				n += doubleValue(s[sp++]);
			return number(n);
		}
        public var operator_minus(var n1, var n2) {
            return number(longValue(n1) - longValue(n2));
        }
        public var xprimop_minus(var [] s, int sp, int argc) {
			if (argc < 1) throw error("wrong number of args");			
			int end = sp + argc;
			double n = doubleValue(s[sp++]);
			while (sp < end)
				n -= doubleValue(s[sp++]);
			return number(n);
        }
        public var operator_multiply(var n1, var n2) {
            return number(longValue(n1) * longValue(n2));
        }
        public var xprimop_multiply(var [] s, int sp, int argc) {
			double n = 1;
			int end = sp + argc;
			while (sp < end)
				n *= doubleValue(s[sp++]);
			return number(n);
        }
        public var primitive_mul(var obj1, var obj2) {
            return number(doubleValue(obj1) * doubleValue(obj2));
        }
        public var primitive_div(var obj1, var obj2) {
            return number(doubleValue(obj1) / doubleValue(obj2));
        }
        public var operator_lt(var obj1, var obj2) {
            return (doubleValue(obj1) < doubleValue(obj2))? obj1 : NIL;
            //return (longValue(obj1) < longValue(obj2))? obj1 : NIL;
        }
        public var operator_le(var obj1, var obj2) {
            return (doubleValue(obj1) <= doubleValue(obj2))? obj1 : NIL;
            //return (longValue(obj1) <= longValue(obj2))? obj1 : NIL;
        }
        public var operator_eq(var obj1, var obj2) {
            return equal(obj1, obj2)? TRUE : FALSE;
        }
        public var operator_ge(var obj1, var obj2) {
            return (doubleValue(obj1) >= doubleValue(obj2))? obj1 : NIL;
        }
        public var operator_gt(var obj1, var obj2) {
            return (doubleValue(obj1) > doubleValue(obj2))? obj1 : NIL;
        }
        public var primitive_identical_p(var obj1, var obj2) {
            return bool(obj1 == obj2);
        }
        public var primitive_make_vector(var size, var init) {
            int n = intValue(size);
            return makeVector(n, init);
        }
        public var primitive_vector_set_bang(var vec, var idx, var val) {
            int i = intValue(idx);
            vectorSet(vec, idx, val);
            return NIL;
        }
        public var primitive_vector_ref(var vec, var idx) {
            int i = intValue(idx);
            return vectorRef(vec, idx);
        }
        public var primitive_zero_p(var obj) {
            return (isNumber(obj) && doubleValue(obj) == 0)? TRUE : FALSE;
        }
        public var primitive_number_to_string(var obj) {
            return string(theNumber(obj).toString());
        }
        public var primitive_string_length(var obj) {
            return number(length(obj));
        }
        public var primitive_display(var obj) { //should be primN
            if (isString(obj))
                System.out.print(asString(obj).stringValue());
            else
                System.out.print(obj);
            return NIL;
        }
        public var primitive_newline() { //should be primN
            System.out.println("");
            return NIL;
        }
        public var primitive_use(var sym) {
            String filename = symbolName(sym);
            LCode thunk = asCode(loadModule(filename, new EllPrimitives()));
            var result = exec(thunk);
            var exports = thunk.module.getExports();
            while (exports != NIL) {
                LSymbol export = asSymbol(car(exports));
                var val = thunk.module.global(export);
                env.setGlobal(export, val);
                exports = cdr(exports);
            }
            return sym;
        }
    }
}