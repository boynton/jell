package org.jelled;
import java.util.List;
import java.util.HashMap;
import java.io.IOException;
import java.io.File;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringReader;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.io.UnsupportedEncodingException;

public class Notation extends Data {

    public static final var READ = intern("read");
    public static final var WRITE = intern("write");
    static final LSymbol SYM_FILE = intern("file");
    static final LSymbol SYM_CHANNEL = intern("channel");

    static class LFile extends var {
        java.io.File handle;
        LFile(String path) {
            this.handle = new File(path);
        }
        LSymbol type() { return SYM_FILE; }
        boolean exists() { return handle.exists(); }
        public String toString() { return "<file " + handle + ">"; }
    }
    public static var file(String path) {
        return new LFile(path);
    }
    public static boolean isFile(var obj) {
        return obj instanceof LFile;
    }
    static LFile asFile(var obj) {
        if (!(obj instanceof LFile))
            error("not a file: " + obj);
        return (LFile)obj;
    }

    public static var open(var file, var direction) {
        if (isFile(file)) {
            LFile f = asFile(file);
            if (direction == READ) {
                return openReader(f);
            } else if (direction == WRITE) {
                return openWriter(f);
            } else
                error("bad direction: " + direction);
        }
        //else if string then readstring
        //else if url then readurl
        throw error("Cannot open: " + file + " for " + direction);
    }

    private static var openReader(var obj) {
        if (isFile(obj)) {
            LFile f = asFile(obj);
            if (!f.exists())
                error("file not found: " + obj);
            try {
                return new LReaderChannel(new FileReader(f.handle));
            } catch (Exception e) {
                throw error("Cannot read file: " + e.getMessage());
            }
        } else {
            throw error("Cannot open for reading: " + obj);
        }
    }

    //all at once
    public static var file_contents(var vfile) {
        LFile file = asFile(vfile);
        try {
            File f = file.handle;
            byte [] buf = new byte[(int)f.length()];
            try (FileInputStream fis = new FileInputStream(f)) {
                    fis.read(buf);
                    return string(new String(buf, "UTF-8"));
                }
        } catch (IOException e) {
            throw error("cannot read file " + file);
        }
    }

    static LChannel readableChannel(var chan) {
        if (isString(chan))
            return new LReaderChannel(new StringReader(stringValue(chan)));
        if (isFile(chan))
            return new LReaderChannel(new StringReader(stringValue(file_contents(asFile(chan)))));
        if (isReadable(chan))
            return (LReaderChannel)chan;
        throw error("Cannot read from " + chan);
    }

    public static var read(var source) {
        boolean allOrNothing = isString(source);
        LChannel chan = readableChannel(source);
        var val = chan.read();
        if (allOrNothing && val == EOI)
            throw error("read: unexpected end of input");
        return val;
    }

    public static void close(var channel) {
        if (!isChannel(channel))
            error("Cannot close: " + channel);
        asChannel(channel).close();
    }

    static abstract class LChannel extends var implements AutoCloseable {
        boolean readable() { return false; }
        boolean writable() { return false; }
        int getChar() { throw error("Cannot read channel"); }
        var read() { throw error("Cannot read channel"); }
        void putChar(int ch) { throw error("cannot write channel"); }
        void write(var data) { throw error("cannot write channel"); }
        void flush() { throw error("cannot flush channel"); }
        public void close() { }
        LSymbol type() { return SYM_CHANNEL; }
        public String toString() { return "<channel>"; }
    }
    public static boolean isChannel(var chan) {
        return chan instanceof LChannel;
    }
    static LChannel asChannel(var chan) {
        return (LChannel)chan;
    }
    public static boolean isReadable(var chan) {
        if (isChannel(chan))
            return ((LChannel)chan).readable();
        return false;
    }
    public static boolean isWritable(var chan) {
        if (isChannel(chan))
            return ((LChannel)chan).writable();
        return false;
    }

    public static class LReaderChannel extends LChannel {
        Reader raw;
        BufferedReader in;
        int lastc;
        final LSymbol SYM_QUOTE;
        final LSymbol SYM_QUASIQUOTE;
        final LSymbol SYM_UNQUOTE;
        final LSymbol SYM_UNQUOTE_SPLICING;
        final LSymbol SYM_TRUE;
        final LSymbol SYM_FALSE;
        final LSymbol SYM_NIL;
        final LSymbol SYM_NULL;

        @Override public String toString() { return "<channel: " + raw + ">"; }
        public LReaderChannel(Reader in) {
            this.raw = in;
            this.in = new BufferedReader(in);
            this.lastc = -1;
            SYM_QUOTE = LSymbol.intern("quote");
            SYM_QUASIQUOTE = LSymbol.intern("quasiquote");
            SYM_UNQUOTE = LSymbol.intern("unquote");
            SYM_UNQUOTE_SPLICING = LSymbol.intern("unquote-splicing");
            SYM_TRUE = LSymbol.intern("true");
            SYM_FALSE = LSymbol.intern("false");
            SYM_NULL = LSymbol.intern("null");
            SYM_NIL = LSymbol.intern("nil");
        }

        @Override
        boolean readable() { return true; }

        @Override
        public void close() {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    error("Cannot cleanly close: " + e);
                }
            }
        }

        int getChar() {
            try {
                if (lastc != -1) {
                    int c = lastc;
                    lastc = -1;
                    return c;
                } else {
                    return in.read();
                }
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }
        }

        void ungetChar(int n) {
            lastc = n;
        }

        final char SINGLE_QUOTE = '\'';
        final String WHITESPACE = " \n\r\t,";
        final String DELIMITER = "()[]{}\";";
        final String ALPHA = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final String NUMERIC = "0123456789";
        final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        @Override
        var read() {
            int c = getChar();
            while (c != -1) {
                if (WHITESPACE.indexOf(c) >= 0) {
                    c = getChar();
                    continue;
                } else if (c == ';') {
                    if (!decodeComment())
                        break;
                    else {
                        c = getChar();
                        continue;
                    }
                } else if (c == '(') {
                    return decodeList();
                } else if (c == ')') {
                    error("LReader: unexpected ')'");
                } else if (c == '[') {
                    return decodeVector();
                } else if (c == ']') {
                    error("LReader: unexpected ']'");
                } else if (c == '{') {
                    return decodeMap();
                } else if (c == '}') {
                    error("LReader: unexpected '}'");
                } else if (c == '"') {
                    return decodeString();
                } else if (c == SINGLE_QUOTE) {
                    var o = read();
                    if (o != EOI) {
                        if (o == NIL)
                            return NIL;
                        return list(SYM_QUOTE, o);
                    }
                    break;
                } else if (c == '#') {
                    return decodeSharp();
                } else {
                    var obj;
                    String atom = decodeAtom(c);
                    boolean colon = false;
                    if (atom.endsWith(":")) {
                        colon = true;
                        atom = atom.substring(0, atom.length()-1);
                    } else if (atom.startsWith(":")) {
                        colon = true;
                        atom = atom.substring(atom.length()-1);
                        if (atom.length() == 0) continue; //standalone colons are whitespace
                    }
                    try {
                        double d = Double.parseDouble(atom);
                        return number(d); //colons next to numbers are whitespace
                    } catch (NumberFormatException e) {
                    }
                    if (colon) {
                        if (atom.length() == 0) {
                            c = getChar();
                            continue; //standalone colons are whitespace
                        }
                        var k = keyword(LSymbol.intern(atom));
                        return k;
                    }
                    LSymbol sym = LSymbol.intern(atom);
                    if (true) {
                        //hmm. This is a bit like evaluation. to avoid this, could use #t, #f, and '() like scheme does.
                        if (sym == SYM_TRUE)
                            return TRUE;
                        else if (sym == SYM_FALSE)
                            return FALSE;
                        else if (sym == SYM_NIL || sym == SYM_NULL)
                            return NIL;
                    }
                    return sym;
                }
            }
            return EOI;
        }

        String bufToString(byte [] b) {
            try {
                return new String(b, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                error("Cannot encode string: " + e);
                return null;
            }
        }
        String bufToSubstring(byte [] b, int offset, int len) {
            try {
                return new String(b, offset, len, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                error("Cannot encode string: " + e);
                return null;
            }
        }

        var decodeSharp() {
            int c = getChar();
            if (c == '\\') {
                String token = decodeAtom(0);
                //FIXME: implement Characters if I want to support Scheme
                if ("space".equals(token))
                    return string(" ");
                else if ("newline".equals(token))
                    return string("\n");
                else if (token.length() == 1) {
                    return string(token);
                } else
                    error("Bad character literal: #\\" + token);
            } else {
                ungetChar(c);
            }
            String atom = decodeAtom(0);
            if ("t".equals(atom)) return TRUE; //scheme
            if ("f".equals(atom)) return FALSE; //scheme
            //scheme vectors are #(...)
            //#( is a lambda in clojure
            throw error("LReader: bad # entity: " + atom);
        }

        var decodeString() {
            boolean escape = false;
            int n;
            char c, c2;
            char [] cbuf = new char[4];
            StringBuilder buf = new StringBuilder();
            while ((n = getChar()) != -1) {
                c = (char)n;
                if (escape) {
                    escape = false;
                    switch (c) {
                    case '\\':
                    case '/':
                    case '"':
                        buf.append(c);
                        break;
                    case 'e':
                        buf.append((char)27);
                        break;
                    case 'n':
                        buf.append('\n');
                        break;
                    case 't':
                        buf.append('\t');
                        break;
                    case 'f':
                        buf.append('\f');
                        break;
                    case 'b':
                        buf.append('\b');
                        break;
                    case 'r':
                        buf.append('\r');
                        break;
                    case 'u':
                    case 'U':

                        cbuf[0] = (char)getChar();
                        cbuf[1] = (char)getChar();
                        cbuf[2] = (char)getChar();
                        cbuf[3] = (char)getChar();
                        String hex = new String(cbuf);
                        try {
                            int i = Integer.parseInt(hex, 16);
                            buf.append((char)i);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            error("Bad unicode escape: " + hex);
                        }
                        break;
                    case 'C':
                    case 'c':
                        c2 = (char)getChar();
                        if (c2 == '-') {
                            c2 = (char)getChar();
                            if (c2 >= 'a' && c2 <= 'z') c2 -= (char)('a' - 'A');
                            if (c2 > '@' && c2 <= 'Z') {
                                buf.append((char)((int)c2 - (int)'@'));
                                break;
                            }
                        }
                        /* fall through to error case */
                    default:
                        error("read: bad escape character in string: \\" + (char)c);
                    }
                } else if (c == '"')
                    break;
                else if (c == '\\')
                    escape = true;
                else {
                    escape = false;
                    buf.append(c);
                }
            }
            if (n == -1)
                return EOI;
            else
                return string(buf.toString());
        }

        boolean decodeComment() {
            //to do: actually return the comment to stash in the metadata
            int c = getChar();
            while (c != -1) {
                if (c == '\n')
                    return true;
                c = getChar();
            }
            return false;
        }

        boolean isWhiteSpace(int c) {
            return WHITESPACE.indexOf((char)c) >= 0;
        }
        boolean isDelimiter(int c) {
            return DELIMITER.indexOf((char)c) >= 0;
        }

        boolean isAlpha(int c) {
            return ALPHA.indexOf((char)c) >= 0;
        }

        boolean isNumeric(int c) {
            return NUMERIC.indexOf((char)c) >= 0;
        }

        boolean isAlphaNumeric(int c) {
            return ALPHANUMERIC.indexOf((char)c) >= 0;
        }

        var decodeList() {
            var element;
            int c;
            c = getChar();
            ArrayList<var> lst = new ArrayList<var>();
            var rest = UNDEFINED;
            while (c != -1) {
                if (isWhiteSpace((char)c)) {
                    c = getChar();
                    continue;
                }
                if (c == ';') {
                    if (!decodeComment()) {
                        error("LReader: unterminated list");
                    } else {
                        c = getChar();
                        continue;
                    }
                }
                if (c == '.') {
                    if (rest != UNDEFINED)
                        error("LReader: dotted pair");
                    c = getChar();
                    if (isWhiteSpace((char)c)) {
                        rest = read();
                        if (rest == EOI)
                            error("LReader: unterminated list");
                        c = getChar();
                        break;
                    }
                }
                if (c == ')') {
                    break;
                }
                ungetChar(c);
                element = read();
                if (element == EOI) {
                    error("LReader: unterminated list");
                } else {
                    lst.add(element);
                }
                c = getChar();
            }
            if (c == -1)
                error("LReader: unterminated list: " + lst);
            if (rest != UNDEFINED)
                return makeList(lst, rest);
            return makeList(lst);
        }

        var decodeVector() {
            var element;
            int c;
            c = getChar();
            ArrayList<var> lst = new ArrayList<var>();
            while (c != -1) {
                if (isWhiteSpace((char)c)) {
                    c = getChar();
                    continue;
                }
                if (c == ';') {
                    if (!decodeComment()) {
                        error("LReader: unterminated vector");
                    } else {
                        c = getChar();
                        continue;
                    }
                }
                if (c == ']') {
                    break;
                }
                ungetChar(c);
                element = read();
                if (element == EOI) {
                    error("LReader: unterminated vector");
                } else {
                    lst.add(element);
                }
                c = getChar();
            }
            var result = makeVector(lst);
            if (c == -1)
                error("LReader: unterminated vector: " + result);
            return result;
        }

        var decodeMap() {
            var key, value;
            int c;
            c = getChar();
            LMap map = new LMap();
            while (c != -1) {
                if (isWhiteSpace((char)c)) {
                    c = getChar();
                    continue;
                }
                if (c == ';') {
                    if (!decodeComment()) {
                        error("LReader: unterminated list");
                    } else {
                        c = getChar();
                        continue;
                    }
                }
                if (c == '}') {
                    break;
                }
                ungetChar(c);
                key = read();
                if (key == EOI)
                    error("LReader: unterminated object");
                value = read();
                if (value == EOI)
                    error("LReader: unterminated object");
                map.put(key, value);
                c = getChar();
            }
            if (c == -1)
                error("LReader: unterminated object");
            return map;
        }
        String decodeAtom(int nFirstChar) {
            int c;
            int i = 0;
            StringBuilder buf = new StringBuilder();
            if (nFirstChar > 0)
                buf.append((char)nFirstChar);
            while ((c = getChar()) != -1) {
                if (isWhiteSpace(c))
                    break;
                if (isDelimiter(c) || c == ';') {
                    ungetChar(c);
                    break;
                }
                buf.append((char)c);
                if (c == ':') break;
            }
            return buf.toString();
        }
    }

    public static class LWriterChannel extends LChannel {
        Writer raw;
        BufferedWriter out;
        LWriterChannel(Writer writer) {
            raw = writer;
            out = new BufferedWriter(raw);
        }

        @Override
        boolean readable() { return false; }

        @Override
        boolean writable() { return true; }

        void putChar(int ch) {
            try {
                out.write(ch);
            } catch (IOException e) {
                throw error("cannot write channel: " + e.getMessage());
            }
        }

        @Override
        void write(var data) {
            try {
                if (data == NIL)
                    out.write("nil");
                else if (isString(data))
                    out.write(Data.escapeString(asString(data).stringValue()));
                else if (isMap(data))
                    _writeMap(asMap(data));
                else if (isVector(data))
                    _writeVector(asVector(data));
                else if (isList(data))
                    _writeList(asList(data));
                else if (isSymbol(data))
                    out.write(data.toString());
                else if (isBoolean(data))
                    out.write(data.toString());
                else if (isNumber(data))
                    out.write(data.toString());
                else if (isKeyword(data))
                    out.write(data.toString());
                else {
                    //error("FIX: " + data);
                    //should probably prevent non datum objects from being written at all
                    //but I like to use write to have a clear debug output of objects, i.e. string vs symbol, etc.
                    //the default toString doesn't distinguish that.
                    out.write(data.toString());
                }
            } catch (IOException e) {
                throw error("cannot write channel: " + e.getMessage());
            }
        }

        void _writeList(var lst) throws IOException {
            if (lst == NIL) {
                out.write("nil");
                //out.write("()"); //scheme
            } else {
                out.write("(");
                write(car(lst));
                lst = cdr(lst);
                while (lst != NIL) {
                    if (isList(lst)) {
                        out.write(" ");
                        write(car(lst));
                        lst = cdr(lst);
                    } else {
                        out.write(" . ");
                        write(lst);
                        break;
                    }
                }
                out.write(")");
            }
        }

        void _writeVector(LVector vec) throws IOException {
            int count = vec.length();
            if (count == 0)
                out.write("[]");
            else {
                out.write("[");
                write(vec.ref(0));
                for (int i=1; i<count; i++) {
                    out.write(" ");
                    write(vec.ref(i));
                }
                out.write("]");
            }
        }

        void _writeMap(LMap obj) throws IOException {
            int count = obj.length();
            if (count == 0)
                out.write("{}");
            else {
                boolean first = true;
                out.write("{");
                for (int i=0; i<count; i++) {
                    if (i > 0)
                        out.write(" ");
                    write(obj.keyRef(i));
                    out.write(" ");
                    write(obj.valueRef(i));
                }
                out.write("}");
            }
        }

        @Override
        void flush() {
            try {
                out.flush();
            } catch (IOException e) {
                throw error("cannot close channel: " + e.getMessage());
            }
        }
        @Override
        public void close() {
            try {
                out.close();
                raw.close();
            } catch (IOException e) {
                throw error("cannot close channel: " + e.getMessage());
            }
        }
    }
    private static class LStringWriterChannel extends LWriterChannel {
        LStringWriterChannel() {
            super(new java.io.StringWriter());
        }
        String getString() {
            try {
                out.flush();
                return raw.toString();
            } catch (IOException e) {
                throw error("Cannot write: " + e.getMessage());
            }
        }
    }

    private static LWriterChannel writableChannel(var chan) {
        if (isWritable(chan))
            return (LWriterChannel)chan;
        throw error("Cannot write to " + chan);
    }

    private static var openWriter(var obj) {
        if (isFile(obj)) {
            LFile f = asFile(obj);
            try {
                return new LWriterChannel(new FileWriter(f.handle));
            } catch (Exception e) {
                throw error("Cannot write file: " + e.getMessage());
            }
        } else {
            throw error("Cannot open for writing: " + obj);
        }
    }

    /** Return a string holding the written notation for the object */
    public static var write(var obj) {
        LStringWriterChannel chan = new LStringWriterChannel();
        write(obj, chan);
        return string(chan.getString());
    }

    /** write the notation for the object to the destination. If dest is nil, return a string. */
    public static void write(var obj, var dest) {
        LWriterChannel chan = writableChannel(dest);
        chan.write(obj);
    }

    public static void newline(var chan) {
        LWriterChannel out = writableChannel(chan);
        out.putChar('\n');
    }


}
