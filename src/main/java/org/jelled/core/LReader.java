package org.jelled.core;
import java.io.Reader;
import java.io.IOException;
import java.util.ArrayList;
import java.io.UnsupportedEncodingException;

public class LReader {
    int lastc;
    Reader r;
    LEnvironment env;

    public LReader(Reader in, LEnvironment env) {
        this.env = env;
        this.r = in;
        this.lastc = -1;
    }
    
    private Object error(String s) {
        throw new LError(s);
    }

    int getChar() {
        try {
            if (lastc != -1) {
                int c = lastc;
                lastc = -1;
                return c;
            } else {
                return r.read();
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
    final String WHITESPACE = " \n\r\t";
    final String DELIMITER = "()[]{}";
    final String ALPHA = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    final String NUMERIC = "0123456789";
    final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    
    public static Object EOS = new Singleton("<eos>");

    public Object decode() {
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
                Object o = decode();
                if (o != EOS)
                    return LList.create(LEnvironment.SYM_QUOTE, o);
                break;
            } else if (c == ',') {
                int d = getChar();
                if (d != -1) {
                    Object k, obj;
                    if (d == '@')
                        k = LEnvironment.SYM_UNQUOTE_SPLICING;
                    else {
                        k = LEnvironment.SYM_UNQUOTE;
                        ungetChar(d);
                    }
                    obj = decode();
                    if (obj != EOS)
                        return new LList(k, (ISequence)obj);
                }
                break;
            } else if (c == '`') {
                Object obj = decode();
                if (obj != EOS)
                    return new LList(LEnvironment.SYM_QUASIQUOTE, (ISequence)obj);
                break;
            } else if (c == '#') {
                return parseReaderMacro();
                /*
                c = getChar();
                if (c == -1)
                    break;
                if (c == '\\') {
                    Object tmp = readCharacter();
                    if (tmp == EOS)
                        return tmp;
                    if (tmp instanceof Character)
                        return ((Character)tmp).toString();
                }
                String atom = decodeAtom(c);
                if ("t".equals(atom))
                    return Boolean.TRUE;
                else if ("f".equals(atom))
                    return Boolean.FALSE;
                else
                    error("LReader: bad syntax: #" + atom);
                */
            } else {
                Object obj;
                String atom = decodeAtom(c);
                //check for "0x" prefix, others?
                obj = parseNumber(atom, 10, -1); //figures out the type best it can
                if (obj != null)
                    return obj;
                return env.intern(atom);
            }
        }
        return EOS;
    }

    protected Object parseReaderMacro() {
        int c = getChar();
        if (c == -1)
            return EOS;
        if (c == '\\') {
            Object tmp = readCharacter();
            if (tmp == EOS)
                return tmp;
            if (tmp instanceof Character)
                return ((Character)tmp).toString();
        }
        String atom = decodeAtom(c);
        if ("t".equals(atom))
            return Boolean.TRUE;
        else if ("f".equals(atom))
            return Boolean.FALSE;
        else
            return error("LReader: bad syntax: #" + atom);
    }

    Object readCharacter() {
        int ctmp, c = getChar();
        if (c == -1)
            return EOS;
        ctmp = getChar();
        if (c == -1)
            return EOS;
        if (WHITESPACE.indexOf(ctmp) >= 0 || DELIMITER.indexOf(ctmp) >= 0) {
            /* a normal character literal */
            ungetChar(ctmp);
        } else {
            /* maybe a named character */
            int i = 64-2;
            byte buf[] = new byte[64];
            int n=0;
            buf[n++] = (byte)'#'; buf[n++] = (byte)'\\'; buf[n++] = (byte)c; buf[n++] = (byte)ctmp;  buf[n] = 0;
            c = getChar();
            while (ALPHANUMERIC.indexOf(c) >= 0) {
                buf[n++] = (byte)c; buf[n] = 0;
                if (i-- == 0) break;
                c = getChar();
            }
            if (c == -1)
                return EOS;
            if (c == '-') {
                /* i.e. ctrl-U */
                buf[n++] = (byte)c; buf[n] = 0;
                if (n != 7 || buf[2] != 'c' || buf[3] != 't' || buf[4] != 'r' || buf[4] != 'l')
                    error("bad character literal: " + bufToString(buf));
                c = getChar();
                if (c == -1)
                    return EOS;
                buf[n++] = (byte)c; buf[n] = 0;
                if (c >= 'a' && c <= 'z') c -= ('a' - 'A');
                c -= '@';
                if (c < 0 || c >= 32)
                    error("bad character literal: " + bufToString(buf));
                ctmp = getChar();
                if (ctmp == -1)
                    return EOS;
                buf[n++] = (byte)ctmp; buf[n] = 0;
                if (!isWhiteSpace(c) && !isDelimiter(c))
                    error("bad character literal: " + bufToString(buf));
                ungetChar(ctmp);
                return new Character((char)c);
            } else if (isWhiteSpace(c) || isDelimiter(c)) {
                /* the normal case - a simple symbolic name */
                Object cResult = namedCharacter(bufToSubstring(buf, 2, n-2));
                ungetChar(c); /* the delimiter */
                if (cResult != null)
                    return cResult;
                else
                    error("bad character literal: " + bufToString(buf));
            } else {
                error("bad character literal: " + bufToString(buf));
            }
        }
        return new Character((char)c);
    }

    Character namedCharacter(String sName) {
        String name = sName.toLowerCase();
        if (name.equals("space"))
            return new Character(' ');
        else if (name.equals("newline"))
            return new Character('\n');
        else if (name.equals("return"))
            return new Character('\r');
        else if (name.equals("backspace"))
            return new Character((char)8);
        else if (name.equals("tab"))
            return new Character('\t');
        else if (name.equals("escape"))
            return new Character((char)27);
        else if (name.equals("rubout"))
            return new Character((char)127);
        else if (name.equals("null"))
            return new Character((char)0);
        else
            return null;
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
    
    Object decodeString() {
        boolean escape = false;
        int n;
        char c, c2;
        StringBuilder buf = new StringBuilder();
        while ((n = getChar()) != -1) {
            c = (char)n;
            if (escape) {
                escape = false;
                switch (c) {
                case '\\':
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
                case 'r':
                    buf.append('\r');
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
            return EOS;
        else
            return buf.toString();
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

    Object decodeList() {
        Object the_list, the_tail, temp, element;
        int c;
        int dot_state = 0;
        the_list = LList.EMPTY;
        the_tail = null;
        Object dottedTail = null;
        c = getChar();
        ArrayList<Object> lst = new ArrayList<Object>();
        while (c != -1) {
            if (isWhiteSpace((char)c)) {
                c = getChar();
                continue;
            }
            if (c == ';') {
                if (!decodeComment()) {
                    error("LReader: EOF while decoding list");
                } else {
                    c = getChar();
                    continue;
                }
            }
            if (c == ')') {
                if (dot_state == 1)
                    error("LReader: expected element after dot in list");
                break;
            }
            if (dot_state > 1)
                error("LReader: more than one element after dot in list");
            ungetChar(c);
            element = decode();
            if (element == EOS) {
                error("LReader: EOF while reading list");
            /* dotted pairs are not handled by Ell. A dot is a valid symbol
            } else if (element == DOT) {
                if (lst.size() == 0)
                    error("LReader: dot in null list");
                else if (dot_state == 0)
                    dot_state++;
                else
                    error("LReader: more than one dot in list");
            } else if (dot_state == 1) {
                if (lst.size() > 0) {
                    dottedTail = element;
                    dot_state++;
                } else
                    error("LReader: expected element before dot in list");
            */
            } else {
                lst.add(element);
            }
            c = getChar();
        }
        if (c == -1)
            error("LReader: EOF while reading list: " + lst);
        /*
        if (dottedTail != null) {
            //created a LList with a special Pair as a tail
            LList res = LList.fromArrayList(lst, dottedTail);
            System.out.println("Created dotted list: " + res);
            System.out.println("car of it: " + res.car());
            System.out.println("cdr of it: " + res.cdr());
            return res;
        }
        */
        return LList.fromArrayList(lst);
    }

    Object decodeVector() {
        System.out.println("decodeVector NYI");
        return null;
    }
    Object decodeMap() {
        System.out.println("decodeMap NYI");
        return null;
    }
    Object decodeSet() {
        System.out.println("decodeSet NYI");
        return null;
    }
        
    String decodeAtom(int nFirstChar) {
        int c;
        int i = 0;
        StringBuilder buf = new StringBuilder();
        buf.append((char)nFirstChar);
        while ((c = getChar()) != -1) {
            if (isWhiteSpace(c)) break;
            if (isDelimiter(c) || c == ';') {
                ungetChar(c);
                break;
            }
            buf.append((char)c);
        }
        return buf.toString();
    }
        
    Number parseNumber(String s, int nRadix, int nExact) {
        if (s.equals("-"))
            return null;
        int result = 0;
        boolean negative = false;
        int i=0, max = s.length();
        if (max > 0) {
            if (s.charAt(0) == '-') {
                negative = true;
                i++;
            }
            while (i < max) {
                int digit = Character.digit(s.charAt(i++), nRadix);
                if (digit < 0)
                    return null;
                result = result * nRadix + digit;
            }
        } else
            return null;
        if (negative)
            return new Integer(-result);
        else
            return new Integer(result);
    }
}
