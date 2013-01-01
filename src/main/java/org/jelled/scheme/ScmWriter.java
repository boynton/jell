package org.jelled.scheme;
import org.jelled.core.*;
import java.io.Writer;

public class ScmWriter extends LWriter {
    ScmWriter(Writer out, LEnvironment env) {
        super(out, env);
    }

    protected void encode(Object o, StringBuilder sb) {
        if (o == null)
            sb.append("<void>");
        else if (o instanceof String) {
            encodeString((String)o, sb);
        } else if (o instanceof Boolean)
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

    protected void encodeNil(StringBuilder sb) {
    }

    protected void encodeBoolean(Boolean b, StringBuilder sb) {
        sb.append(b.booleanValue()? "#t" : "#f");
    }

    protected void encodeMap(IMap m, StringBuilder sb) {
        throw new LError("Cannot encode a map in Scheme");
    }

}
