import org.junit.Test;
import static org.junit.Assert.*;
import static org.jelled.Ell.*;
import java.io.FileReader;

public class TestTypes {

    @Test
    public void testNil() {
        assertFalse(NIL == null);

        assertTrue(isNil(NIL));
        assertFalse(isBoolean(NIL));
        assertFalse(isNumber(NIL));
        assertFalse(isString(NIL));
        assertFalse(isSymbol(NIL));
        assertFalse(isKeyword(NIL));
        assertFalse(isList(NIL));
        assertFalse(isVector(NIL));
        assertFalse(isMap(NIL));
        assertFalse(isChannel(NIL));
        assertFalse(isFunction(NIL));

        assertFalse(isBoolean(NIL));
        assertTrue(equal(NIL, NIL));
        assertFalse(isFalse(NIL)); //NIL (or '()) is *not* boolean false.
        assertTrue(isTrue(NIL));
    }

    @Test
    public void testBoolean() {

        assertTrue(isBoolean(FALSE));
        assertFalse(isNil(TRUE));
        assertTrue(isBoolean(TRUE));
        assertFalse(isNumber(TRUE));
        assertFalse(isString(TRUE));
        assertFalse(isSymbol(TRUE));
        assertFalse(isKeyword(TRUE));
        assertFalse(isList(TRUE));
        assertFalse(isVector(TRUE));
        assertFalse(isMap(TRUE));
        assertFalse(isChannel(TRUE));
        assertFalse(isFunction(TRUE));

        assertTrue(equal(TRUE, TRUE));
        assertFalse(equal(TRUE, FALSE));
        assertTrue(equal(FALSE, FALSE));
        assertFalse(equal(FALSE, TRUE));
        assertTrue(isFalse(FALSE));
        assertTrue(isTrue(TRUE));
        assertFalse(isFalse(TRUE));
        assertFalse(isTrue(FALSE));
        var b1 = bool(true);
        assertTrue(b1 == TRUE);
        var b2 = bool(false);
        assertTrue(b2 == FALSE);
        assertTrue(booleanValue(b1) == true);
        assertTrue(booleanValue(b2) == false);
        assertTrue(b1.hashCode() == TRUE.hashCode());
        assertTrue(b2.hashCode() != TRUE.hashCode());
    }

    @Test
    public void testNumber() {
        var n1 = number(23);
        var n2 = number(57.0);
        var n3 = number(7.5);

        assertTrue(isTrue(n1)); //numbers are boolean true

        assertFalse(isNil(n1));
        assertFalse(isBoolean(n1));
        assertTrue(isNumber(n1));
        assertFalse(isString(n1));
        assertFalse(isSymbol(n1));
        assertFalse(isKeyword(n1));
        assertFalse(isList(n1));
        assertFalse(isVector(n1));
        assertFalse(isMap(n1));
        assertFalse(isChannel(n1));
        assertFalse(isFunction(n1));

        assertTrue(intValue(n1) == 23);
        assertTrue(longValue(n1) == 23L);
        assertTrue(doubleValue(n1) == 23.0);
        assertTrue(equal(n2, number(57)));
        assertTrue(equal(n1, number(23)));
        assertFalse(equal(n1, number(54)));

    }

    @Test
    public void testString() {

        var s1 = string("hello");

        assertFalse(isBoolean(s1));
        assertFalse(isNil(s1));
        assertFalse(isBoolean(s1));
        assertFalse(isNumber(s1));
        assertTrue(isString(s1));
        assertFalse(isSymbol(s1));
        assertFalse(isKeyword(s1));
        assertFalse(isList(s1));
        assertFalse(isVector(s1));
        assertFalse(isMap(s1));
        assertFalse(isChannel(s1));
        assertFalse(isFunction(s1));

        assertTrue(isTrue(s1));
        assertFalse(isFalse(s1));

        assertTrue(length(s1) == 5);

        var s2 = string("hello");
        assertFalse(s1 == s2);
        assertTrue(equal(s1, s2));
        assertTrue(s1.hashCode() == s2.hashCode());
        var s3 = string("hello there");
        assertFalse(s1 == s3);
        assertFalse(equal(s1, s3));
        assertFalse(s1.hashCode() == s3.hashCode());

    }

    @Test
        public void testSymbol() {
        var sym1 = intern("symbol1");
        assertFalse(isNil(sym1));
        assertFalse(isBoolean(sym1));
        assertFalse(isNumber(sym1));
        assertFalse(isString(sym1));
        assertTrue(isSymbol(sym1));
        assertFalse(isKeyword(sym1));
        assertFalse(isList(sym1));
        assertFalse(isVector(sym1));
        assertFalse(isMap(sym1));
        assertFalse(isChannel(sym1));
        assertFalse(isFunction(sym1));

        assertTrue(isTrue(sym1));

        var sym2 = intern("symbol1");
        assertTrue(sym1 == sym2);
        assertTrue(equal(sym1, sym2));
        var sym3 = intern("symbol2");
        assertFalse(sym1 == sym3);
        assertFalse(equal(sym1, sym3));

        var key1 = keyword("symbol1");
        assertFalse(isBoolean(key1));
        assertFalse(isNil(key1));
        assertFalse(isBoolean(key1));
        assertFalse(isNumber(key1));
        assertFalse(isString(key1));
        assertFalse(isSymbol(key1));
        assertTrue(isKeyword(key1));
        assertFalse(isList(key1));
        assertFalse(isVector(key1));
        assertFalse(isMap(key1));
        assertFalse(isChannel(key1));
        assertFalse(isFunction(key1));

        assertTrue(isTrue(key1));

        assertFalse(key1 == sym1);
        assertFalse(equal(key1, sym1));
        assertTrue(keywordSymbol(key1) == sym1);

    }


    @Test
        public void testList() {
        var sym = intern("foo");
        var lst = list(string("one"), number(2), sym);

        assertFalse(isBoolean(lst));
        assertFalse(isNil(lst));
        assertFalse(isBoolean(lst));
        assertFalse(isNumber(lst));
        assertFalse(isString(lst));
        assertFalse(isSymbol(lst));
        assertFalse(isKeyword(lst));
        assertTrue(isList(lst));
        assertFalse(isVector(lst));
        assertFalse(isMap(lst));
        assertFalse(isChannel(lst));
        assertFalse(isFunction(lst));

        assertTrue(length(lst) == 3);
        assertTrue(isString(car(lst)));
        assertTrue(isList(cdr(lst)));
        assertTrue(isNumber(cadr(lst)));
        assertTrue(isList(cddr(lst)));
        assertTrue(isSymbol(caddr(lst)));
        assertTrue(isNil(cdddr(lst)));
        var lst1 = list(sym);
        var lst2 = cons(sym, NIL);
        assertFalse(lst1 == lst2);
        assertTrue(equal(lst1, lst2));

    }

    @Test
        public void testVector() {
        var sym = intern("foo");
        var vec = vector(string("one"), number(2), sym);

        assertFalse(isBoolean(vec));
        assertFalse(isNil(vec));
        assertFalse(isBoolean(vec));
        assertFalse(isNumber(vec));
        assertFalse(isString(vec));
        assertFalse(isSymbol(vec));
        assertFalse(isKeyword(vec));
        assertFalse(isList(vec));
        assertTrue(isVector(vec));
        assertFalse(isMap(vec));
        assertFalse(isChannel(vec));
        assertFalse(isFunction(vec));
        assertTrue(length(vec) == 3);

        var vec25 = makeVector(25, string("Hi"));
        assertTrue(length(vec25) == 25);

        var vec1 = vector(sym);
        var vec2 = vector(sym);
        assertFalse(vec1 == vec2);
        assertTrue(equal(vec1, vec2));

    }
    @Test
        public void testMap() {
        var map = map(intern("one"), number(1), intern("two"), number(2), intern("three"), number(3));

        assertFalse(isBoolean(map));
        assertFalse(isNil(map));
        assertFalse(isBoolean(map));
        assertFalse(isNumber(map));
        assertFalse(isString(map));
        assertFalse(isSymbol(map));
        assertFalse(isKeyword(map));
        assertFalse(isList(map));
        assertFalse(isVector(map));
        assertTrue(isMap(map));
        assertFalse(isChannel(map));
        assertFalse(isFunction(map));
        println("length: " + length(map));
        assertTrue(length(map) == 3);
        assertTrue(equal(get(map, intern("two")), number(2)));
        assertTrue(equal(get(map, intern("twozzz")), NIL));



    }


}