package org.jelled.scheme;
import org.jelled.core.*;
import java.io.Reader;

public class ScmReader extends LReader {
    ScmReader(Reader in, LEnvironment env) {
        super(in, env);
    }
}
