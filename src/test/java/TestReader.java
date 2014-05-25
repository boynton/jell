import org.junit.Test;
import static org.junit.Assert.*;
import static org.jelled.Ell.*;
import static org.jelled.Runtime.*;
import java.io.FileReader;

public class TestReader {

    @Test
    public void testEllDN() {
        try {
            var elldn = file_contents(file("./src/test/resources/test1.elldn"));
            var data = read(elldn);
            var sdata = write(data);
            var data2 = read(sdata);
            boolean b = equal(data, data2);
            assertTrue(b);
        } catch (error e) {
            System.out.println("fail!");
            e.printStackTrace();
        }
    }

    @Test
    public void testJson() {
        try {
            var json = file_contents(file("./src/test/resources/test1.json"));
            var data = read(json);
            var sdata = write(data);
            var data2 = read(sdata);
            assertTrue(equal(data, data2));
        } catch (error e) {
            System.out.println("fail!");
            e.printStackTrace();
        }
    }

    @Test
    public void testTypes() {
        try {
            var data = read(file_contents(file("./src/test/resources/test1.elldn")));
            println(data);
            var v = get(data, "nothing");
            assertTrue(v == NIL);
            v = get(data, keyword("k"));
            assertTrue(type(v) == SYM_KEYWORD);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testReadWriteString() {
        var d1 = map(intern("x"), "symbol", keyword("y"), "keyword", string("z"), "string");
        var sdata = write(d1);
        var d2 = read(sdata);
        assertTrue(equal(d1, d2));
    }

    @Test
    public void testReadWrite() {
        try {
            println("begin testReadWrite");
            var channel = open(file("./src/test/resources/test1.elldn"), READ);
            try {
                var data = read(channel);
                assertTrue(isNil(get(data, string("ary"))));
                assertTrue(isNil(get(data, intern("ary"))));
                assertTrue(!isNil(get(data, keyword("ary"))));
                var bad_data = read(channel);
                println(bad_data);
                var sdata = write(data);
                println(sdata);
                var data2 = read(sdata);
                assertTrue(equal(data, data2));
                try {
                    read(string(""));
                    fail("reading an empty string should throw an error");
                } catch (error e) {
                }
                var outchan = open(file("/tmp/test1.elldn"), WRITE);
                write(data, outchan);
                close(outchan);
            } catch (Throwable e) {
                e.printStackTrace();
                fail("Whoops: " + e);
            } finally {
                println("closing...");
                close(channel);
            }
            println("end testReadWrite");
        } catch (error e) {
            e.printStackTrace();
        }
    }

}