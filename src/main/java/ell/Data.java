package ell;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class Data {
    public static final String NIL_NAME = "nil";
    public static final LSymbol SYM_NIL = intern(NIL_NAME); //scheme: no
    public static final LSymbol SYM_BOOLEAN = intern(LBoolean.typeName);
    public static final LSymbol SYM_STRING = intern(LString.typeName);
    public static final LSymbol SYM_NUMBER = intern(LNumber.typeName);
    public static final LSymbol SYM_SYMBOL = intern(LSymbol.typeName);
    public static final LSymbol SYM_KEYWORD = intern(LKeyword.typeName);
    public static final LSymbol SYM_LIST = intern(LList.typeName);
    public static final LSymbol SYM_VECTOR = intern(LVector.typeName);
    public static final LSymbol SYM_MAP = intern(LMap.typeName);

    public static abstract class var {
        protected var() { }
        public boolean equals(Object other) { return this == other; }
        public String toString() { return "<" + type().name + ">"; }
        public int hashCode() { return super.hashCode(); }
        abstract LSymbol type();
    }

    @SuppressWarnings("unchecked")
    static var asVar(Object o) {
        if (o == null) return NIL;
        if (o instanceof var) return (var)o;
        if (o instanceof String) return string((String)o);
        if (o instanceof Number) return number((Number)o);
        if (o instanceof Boolean) return bool((Boolean)o);
        if (o instanceof List) return makeList((List<var>)o);
        if (o instanceof Map) return makeMap((Map<var,var>)o);
        throw error("Cannot conver to var: " + o.getClass().getName());
    }
    static var asVar(boolean b) { return bool(b); }
    static var asVar(int n) { return number(n); }
    static var asVar(long n) { return number(n); }
    static var asVar(double n) { return number(n); }

    static <T> T typeCast(var obj, Class<T> type, String name) {
        if (type.isInstance(obj))
            return type.cast(obj);
        throw error("not a " + name, obj);
    }

    public static LSymbol type(var data) { return data.type(); }

    public static boolean equal(var o1, var o2) {
        if (o1 == o2) return true;
        else if (o1 == null || o2 == null) return false;
        else if (o1.type() != o2.type()) return false; //otherwise it isn't commutative
        else return o1.equals(o2);
    }

    public static String toString(Object o) {
        if (o == null) return "null";
        return o.toString();
    }
    public static void println(Object s) {
        System.out.println(Data.toString(s));
    }
    public static void print(Object s) {
        System.out.print(Data.toString(s));
    }

    static final class LSpecial extends var {
        private final LSymbol type;
        private final String name;
        private LSpecial(String type, String name) { this.type = intern(type); this.name = name; }
        public String toString() { return name; }
        final LSymbol type() { return type; }
    }
    public static final var NIL = new LSpecial(NIL_NAME, NIL_NAME);
    public static boolean isNil(var obj) { return obj == NIL; }

    public static final LSpecial EOI = new LSpecial("end-of-input", "<end-of-input>");
    public static boolean isEndOfInput(var obj) { return obj == EOI; }

    public static final var UNDEFINED = new LSpecial("undefined", "<undefined>");
    public static boolean isUndefined(var obj) { return obj == UNDEFINED; }

    public static class error extends RuntimeException {
        private final var obj;
        public error(String msg, var obj) {
            super(msg);
            this.obj = obj;
        }
        public error(String msg) {
            super(msg);
            obj = NIL;
        }
        public String toString() {
            String s = "Error: " + getMessage();
            if (obj != NIL)
                s = s + ": " + obj;
            return s;
        };
        public var getObject() { return obj; }
    }
    public static RuntimeException error(String msg) { throw new error(msg); }
    public static RuntimeException error(String msg, var obj) { throw new error(msg, obj); }

    private static abstract class LData extends var {
        private LData() { }
    }
    public static boolean isData(var obj) { return obj instanceof LData; }
    public static LData asData(var obj) { return (LData)obj; }
    public static LData theData(var obj) { return typeCast(obj, LData.class, "data"); }

    public static class LBoolean extends LData {
        static final String typeName = "boolean";
        final boolean value;
        private LBoolean(boolean b) { this.value = b; }
        LSymbol type() { return SYM_BOOLEAN; }
        public int hashCode() { return value ? 1231 : 1237; }
        public String toString() { return value? "true" : "false"; }
        public boolean equals(Object another) {
            if (another instanceof LBoolean)
                return value == booleanValue((LBoolean)another);
            return false;
        }
    }
    public static final LBoolean TRUE = new LBoolean(true);
    public static final LBoolean FALSE = new LBoolean(false);
    public static var bool(boolean b) { return b? TRUE : FALSE; }
    public static boolean isBoolean(var d) { return d instanceof LBoolean; }
    public static final boolean isFalse(var d) { return d == FALSE; }
    public static boolean isTrue(var d) { return d != FALSE; }
    public static boolean booleanValue(var d) { return isTrue(d); }

    public static String escapeString(String s) {
        StringBuilder sb = new StringBuilder();
        int max = s.length();
        sb.append('"');
        for (int i=0; i<max; i++) {
            char c = s.charAt(i);
            switch (c) {
            case '\\': sb.append("\\"); break;
            case '"': sb.append("\\\""); break;
            case '\n': sb.append("\\n"); break;
            case '\t': sb.append("\\t"); break;
            case '\r': sb.append("\\r"); break;
            case '\b': sb.append("\\b"); break;
            case '\f': sb.append("\\f"); break;
            default:
                if (c >= '~' || c < ' ') {
                    String hex = Integer.toHexString(c);
                    while (hex.length() < 4) hex = "0" + hex;
                    sb.append("\\u");
                    sb.append(hex);
                } else
                    sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    public static class LString extends LData {
        static final String typeName = "string";
        private final String value;
        private LString(String s) { this.value = s; }
        LSymbol type() { return SYM_STRING; }
        private int length() { return value.length(); }
        public String toString() { return value; } //this is "display", not "write"
        public int hashCode() { return value.hashCode(); }
        public String stringValue() { return value; }
        public boolean equals(Object o) {
            if (o instanceof LString)
                return value.equals(((LString)o).value);
            else
                return false;
        }
    }
    public static var string(String s) { return new LString(s); }
    public static boolean isString(var v) { return v instanceof LString; }
    public static LString asString(var v) { return (LString)v; }
    public static LString theString(var obj) { return typeCast(obj, LString.class, LString.typeName); }
    public static String stringValue(var v) { return asString(v).value; }

    private final static String SYMBOL_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_!?+-*/=<>";

    public static class LSymbol extends LData {
        static final String typeName = "symbol";
        static ConcurrentHashMap<String,LSymbol> symbols = new ConcurrentHashMap<String,LSymbol>();
        static boolean isValid(String name) {
            int len = name.length();
            //FIXME: a regex to do this faster
            for (int i=0; i<len; i++)
                if (SYMBOL_CHARS.indexOf(name.charAt(i)) < 0) return false;
            return true;
        }

        LSymbol type() { return SYM_SYMBOL; }
        static LSymbol intern(String name) {
            LSymbol sym = symbols.get(name);
            if (sym == null) {
                if (!LSymbol.isValid(name))
                    error("Invalid symbol: " + name);
                sym = new LSymbol(name);
                symbols.putIfAbsent(name, sym); //unusual race condition handled here
            }
            return sym;
        }
        private final String name;
        LSymbol(String n) { this.name = n; }
        public String name() { return name; }
        public String toString() { return name; }
        public boolean equals(Object o) {
            return this == o;
        }
    }

    public static boolean isSymbol(var obj) { return obj instanceof LSymbol; }
    public static LSymbol asSymbol(var obj) { return (LSymbol)obj; }
    public static LSymbol theSymbol(var obj) { return typeCast(obj, LSymbol.class, LSymbol.typeName); }
    public static LSymbol intern(String name) { return LSymbol.intern(name); }
    public static String symbolName(var sym) { return theSymbol(sym).name; }

    static class LNumber extends LData {
        static final String typeName = "number";
        private final double value;
        private LNumber(double v) { this.value = v; }
        LSymbol type() { return SYM_NUMBER; }
        public String toString() {
            //FIXME
            String s = String.format("%f", value); //prevent scientific notation
            if (s.indexOf('.') >= 0) {
                while (s.endsWith("0")) //trim trailing zeros
                    s = s.substring(0, s.length()-1);
                if (s.endsWith(".")) //omit a trailing .
                    s = s.substring(0, s.length()-1);
            }
            //Infinity? NaN?
            return s;
        }
        int asInt() { return (int)value; }
        long asLong() { return (long)value; }
        double asDouble() { return value; }
        public int hashCode() { return new Double(value).hashCode(); }
        public boolean equals(Object o) {
            if (o instanceof LNumber) {
                if (value == ((LNumber)o).value)
                    return true;
            }
            return false;
        }
    }

    public static LNumber number(Number n) { return new LNumber(n.doubleValue()); }
    public static LNumber number(int n) { return new LNumber(n); }
    public static LNumber number(long n) { return new LNumber(n); }
    public static LNumber number(double n) { return new LNumber(n); }
    public static boolean isNumber(var obj) { return obj instanceof LNumber; }
    public static LNumber asNumber(var obj) { return (LNumber)obj; }
    public static LNumber theNumber(var obj) { return typeCast(obj, LNumber.class, "number"); }
    public static int intValue(var obj) { return (int)theNumber(obj).value; }
    public static long longValue(var obj) { return (long)theNumber(obj).value; }
    public static double doubleValue(var obj) { return theNumber(obj).value; }

    protected static class LList extends LData {
        static final String typeName = "list";
        var car;
        var cdr;
        LList(var car, var cdr) {
            this.car = car;
            this.cdr = cdr;
        }
        LSymbol type() { return SYM_LIST; }
        int length() {
            var me = this;
            int count = 0;
            while (me != NIL) {
                count++;
                me = cdr(me);
            }
            return count;
        }
        public boolean equals(Object o) {
            if (o == this) return true;
            if (o instanceof LList) {
                var me = this;
                var other = (var)o;
                while (me != NIL && other != NIL) {
                    if (!equal(car(me), car(other)))
                        return false;
                    me = cdr(me);
                    other = cdr(other);
                }
                return me == other;
            }
            return false;
        }
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            var tmp = this;
            while (tmp != NIL && isList(tmp)) {
                if (tmp != this)
                    sb.append(" ");
                sb.append(Data.toString(car(tmp)));
                tmp = cdr(tmp);
            }
            if (tmp != NIL) {
                sb.append(" . ");
                sb.append(Data.toString(tmp));
            }
            sb.append(")");
            return sb.toString();
        }
    }
    public static boolean isList(var obj) { return obj instanceof LList; }
    public static LList asList(var obj) { return (LList)obj; }
    public static LList theList(var obj) { return typeCast(obj, LList.class, LList.typeName); }
    public static LList cons(var d1, var d2) { return new LList(d1, d2); }
    public static var car(var obj) { return theList(obj).car; }
    public static var cdr(var obj) { return obj == NIL? NIL : theList(obj).cdr; }

    public static var list(Object... v) {
        var lst = NIL;
        for (int i=v.length-1; i >= 0; i--) {
            lst = cons(asVar(v[i]), lst);
        }
        return lst;
    }
    public static var makeList(List<var> lst) {
        return makeList(lst, NIL);
    }
    public static var makeList(List<var> lst, var rest) {
        int idx = lst.size();
        var result = rest;
        while (--idx >= 0) {
            result = cons(lst.get(idx), result);
        }
        return result;
    }
    public static var cadr(var lst) { return car(cdr(lst)); }
    public static var cddr(var lst) { return cdr(cdr(lst)); }
    public static var caddr(var lst) { return car(cdr(cdr(lst))); }
    public static var cdddr(var lst) { return cdr(cdr(cdr(lst))); }

    public static var reverse(var lst) {
        var rev = NIL;
        while (lst != NIL) {
            rev = cons(car(lst), rev);
            lst = cdr(lst);
        }
        return rev;
    }
    public static var concat(var self, var lst) {
        var rev = reverse(self);
        while (rev != NIL) {
            lst = cons(car(rev), lst);
            rev = cdr(rev);
        }
        return lst;
    }

    protected static class LVector extends LData {
        static final String typeName = "vector";
        private final var [] elements;
        private final int offset;
        private final int count;
        LSymbol type() { return SYM_VECTOR; }
        LVector(var [] elements) {
            this(elements, elements.length);
        }
        LVector(var [] elements, int count) {
            this(elements, count, 0);
        }
        LVector(var [] elements, int count, int offset) {
            this.elements = elements;
            this.offset = offset;
            this.count = count;
        }
        int length() { return count; }
        var ref(int idx) { return elements[idx+offset]; }
        void set(int idx, var val) { elements[idx+offset] = val; }
        var slice(int start, int end) {
            return new LVector(elements, end-start, offset+start);
        }
        public boolean equals(Object o) {
            if (o instanceof LVector) {
                LVector v = (LVector)o;
                if (v.count == count) {
                    for (int i=0; i<count; i++) {
                        if (!equal(elements[offset+i], v.ref(i)))
                            return false;
                    }
                    return true;
                }
            }
            return false;
        }
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            int end = offset + count;
            for (int i=offset; i<end; i++) {
                if (i != offset)
                    sb.append(" ");
                sb.append(Data.toString(elements[i]));
            }
            sb.append("]");
            return sb.toString();
        }
    }
    public static boolean isVector(var d) { return d instanceof LVector; }
    public static LVector asVector(var d) { return (LVector)d; }
    public static LVector theVector(var obj) { return typeCast(obj, LVector.class, "vector"); }
    public static LVector makeVector(int size, var init) {
        var [] d = new var[size];
        for (int i=0; i<size; i++)
            d[i] = init;
        return new LVector(d);
    }
    public static var vector(Object... ary) {
        int max = ary.length;
        var [] d = new var[max];
        for (int i=0; i<max; i++)
            d[i] = asVar(ary[i]);
        return new LVector(d);
    }
    public static LVector makeVector(List<var> lst) {
        int max = lst.size();
        var [] d = new var[max];
        for (int i=0; i<max; i++)
            d[i] = lst.get(i);
        return new LVector(d);
    }
    public static var vectorRef(var vec, var idx) {
        return asVector(vec).ref(intValue(idx));
    }
    public static void vectorSet(var vec, var idx, var val) {
        asVector(vec).set(intValue(idx), val);
    }

    static class LMap extends LData {
        static final String typeName = "map";
        var [] bindings;
        int count;
        LMap() {
            this(4);
        }
        LMap(int cap) {
            this.bindings = new var[2*cap];
            this.count = 0;
        }
        LMap(Map<var,var> map) {
            this(Math.max(map.size(), 4));
            int i = 0;
            for (Map.Entry<var, var> e : map.entrySet()) {
                bindings[i++] = e.getKey();
                bindings[i++] = e.getValue();
            }
            this.count = map.size() * 2;
        }
        LSymbol type() { return SYM_MAP; }
        int find(var key) {
            for (int i=0; i<count; i+=2) {
                if (key.equals(bindings[i]))
                    return i;
            }
            return -1;
        }
        void put(var key, var val) {
            int i = find(key);
            if (i < 0) {
                if (count == bindings.length) {
                    int newcap = count*2;
                    var [] nbindings = new var[newcap];
                    System.arraycopy(bindings, 0, nbindings, 0, count);
                    bindings = nbindings;
                }
                bindings[count] = key;
                bindings[count+1] = val;
                count += 2;
            } else {
                bindings[i+1] = val;
            }
        }
        int length() { return count / 2; }
        public boolean equals(Object another) {
            if (another instanceof LMap) {
                LMap other = (LMap)another;
                if (other.length() != length()) return false;
                for (int i=0; i<count; i+=2) {
                    var key = bindings[i];
                    int j = other.find(key);
                    if (j < 0) return false;
                    if (!equal(bindings[i+1], other.bindings[j+1])) return false;
                }
                return true;
            }
            return false;
        }
        boolean has(var key) {
            return find(key) >= 0;
        }
        var keyRef(int i) {
            return bindings[i*2];
        }
        var valueRef(int i) {
            return bindings[i*2+1];
        }
        var get(var key) {
            int i = find(key);
            if (i < 0) return NIL;
            return bindings[i+1];
        }
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean first = true;
            for (int i=0; i<count; i+=2) {
                if (i > 0)
                    sb.append(" ");
                var k = bindings[i];
                sb.append(k);
                sb.append(" ");
                sb.append(bindings[i+1]);
            }
            sb.append("}");
            return sb.toString();
        }
    }
    public static var map(Object... kv) {
        if (kv.length % 2 != 0)
            throw error("Bad arguments to map, must be in pairs");
        LMap m = new LMap();
        for (int i=0; i<kv.length; i+=2) {
            m.put(asVar(kv[i]), asVar(kv[i+1]));
        }
        return m;
    }
    public static var makeMap(Map<var,var> m) { return new LMap(m); }
    public static boolean isMap(var data) { return data instanceof LMap; }
    public static LMap asMap(var data) { return (LMap)data; }
    public static LMap theMap(var obj) { return typeCast(obj, LMap.class, "map"); }

    public static var get(var obj, Object key) {
        return get(obj, asVar(key));
    }
    public static var get(var obj, var key) {
        //this may be a generic function, but is just map for now
        if (isMap(obj)) {
            return asMap(obj).get(key);
            //} else if (isMap(obj)) {
            //    return asMap(obj).get(key);
        } else
            throw error("Cannot get from " + obj);
    }

    public static class LKeyword extends LData {
        static final String typeName = "keyword";
        private final LSymbol sym;
        private LKeyword(LSymbol sym) {
            this.sym = sym;
        }
        LSymbol type() { return SYM_KEYWORD; }
        public boolean equals(Object o) {
            if (o == this) return true;
            if (o instanceof LKeyword)
                return ((LKeyword)o).sym == sym;
            return false;
        }
        public String toString() { return sym.toString() + ":"; }
    }
    public static boolean isKeyword(var o) { return o instanceof LKeyword; }
    public static LKeyword asKeyword(var o) { return (LKeyword)o; }
    public static LKeyword theKeyword(var obj) { return typeCast(obj, LKeyword.class, "keyword"); }
    public static var keywordSymbol(var o) { return ((LKeyword)o).sym; }
    public static var keyword(String s) {
        return keyword(intern(s));
    }
    public static var keyword(var sym) {
        if (isSymbol(sym))
            return new LKeyword(asSymbol(sym));
        //else if (isString(sym))
        //    return new LKeyword(intern(stringValue(sym)));
        else
            throw error("Not a valid argument to keyword: " + sym);
    }

    public static int length(var v) {
        if (isNil(v)) return 0;
        if (isVector(v)) return asVector(v).length();
        if (isString(v)) return asString(v).length();
        if (isList(v)) return asList(v).length();
        if (isMap(v)) return asMap(v).length();
        throw error("Bad argument for length function", v);
    }

}
