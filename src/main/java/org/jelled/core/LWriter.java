package org.jelled.core;
import java.io.Writer;
import java.io.StringWriter;
import java.io.IOException;

public class LWriter {

    final LEnvironment env;
    final Writer out;
    boolean readable;

    public static String toString(Object o) {
        StringWriter sw = new StringWriter();
        LWriter w = new LWriter(sw, null);
        w.encode(o, false);
        return sw.toString();
    }

    public LWriter(Writer writer, LEnvironment env) {
        this.env = env;
        this.out = writer;
        //cache the current namespace
        //cache whether to pretty print or not?
    }

    public LWriter encode(Object o) {
        return encode(o, true);
    }
    
    public LWriter encode(Object o, boolean forRead) {
        readable = forRead;
        StringBuilder sb = new StringBuilder();
        encode(o, sb); //options?
        String s = sb.toString();
        try {
            out.write(s);
        } catch (IOException e) {
            throw new LError("Cannot write: " + e);
        }
        return this;
    }
    
    protected void encode(Object o, StringBuilder sb) {
        if (o == null)
            sb.append("nil");
        else if (o instanceof String)
            encodeString((String)o, sb);
        else if (o instanceof Boolean)
            encodeBoolean((Boolean)o, sb);
        else if (o instanceof IMap)
            encodeMap((IMap)o, sb);
        else if (o instanceof LArray)
            encodeArray((LArray)o, sb);
        else if (o instanceof LList)
            encodeList((LList)o, sb);
        else if (o instanceof ISequence)
            encodeSequence((ISequence)o, sb, '(', ')');
        else
            encodeMisc(o, sb);
    }

    protected void encodeMisc(Object o, StringBuilder sb) {
        sb.append(o.toString());
    }

    protected void encodeNil(StringBuilder sb) {
        sb.append("nil");
    }

    protected void encodeBoolean(Boolean b, StringBuilder sb) {
        sb.append(b.booleanValue()? "true" : "false");
    }

    protected void encodeMap(IMap map, StringBuilder sb) {
        boolean first = true;
        sb.append("{");
        ISequence seq = map.seq();
        if (seq != null) {
            for (Object o : seq) {
                LArray a = (LArray)o;
                Object k = a.ref(0);
                Object v = a.ref(1);
                if (first)
                    first = false;
                else
                    sb.append(' ');
                encode(k, sb);
                sb.append(" ");
                encode(v, sb);
            }
        }
        sb.append('}');
    }


    protected void encodeArray(LArray arr, StringBuilder sb) {
        encodeSequence(arr, sb, '[', ']');
    }

    protected void encodeList(LList lst, StringBuilder sb) {
        encodeSequence(lst, sb, '(', ')');
    }

    protected void encodeSequence(ISequence seq, StringBuilder sb, char openDelim, char closeDelim) {
        sb.append(openDelim);
        encode(seq.first(), sb);
        ISequence o = seq.next();
        while (o != null) {
            sb.append(" ");
            encode(o.first(), sb);
            o = o.next();
        }
        sb.append(closeDelim);
    }

    static char [] hex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    protected void encodeString(String s, StringBuilder sb) {
        if (!readable) {
            sb.append(s);
            return;
        }
        if (s == null) {
            sb.append("\"\"");
            return;
        }
        int max = s.length();
        char [] chars = new char[max];
        s.getChars(0, max, chars, 0);
        sb.append('"');
        for (int i=0; i<max; i++) {
            char c = chars[i];
            if (c == '\\')
                sb.append("\\\\");
            else if (c == '"')
                sb.append("\\\"");
            else if (c == '\b')
                sb.append("\\b");
            else if (c == '\n')
                sb.append("\\n");
            else if (c == '\r')
                sb.append("\\r");
            else if (c == '\f')
                sb.append("\\f");
            else if (c == '\t')
                sb.append("\\t");
            else if (c < ' ') {
                sb.append("\\u");
                sb.append(hex[(c>>12)&15]);
                sb.append(hex[(c>>8)&15]);
                sb.append(hex[(c>>4)&15]);
                sb.append(hex[(c)&15]);
            } else {
                sb.append(c);
            }
        }
        sb.append('"');
    }

}
