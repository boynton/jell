package org.jelled.core;
import java.util.Collection;
import java.util.HashMap;
import java.io.Reader;
import java.io.Writer;

//An environment is where symbols are bound to values.
//It is where an Ell virtual machine runs, that is, the context for execution.
//It represents the global symboltable to run against.
//An Environment has a "current namespace" is defaults to when creating or
//referring to any symbols.
//An environment can intern simple symbols (creating them in the current namespace),
//or from another namespace (in which case this symboltable has a reference to the other
//symbol), or you can "refer" to an entire other namespace (all symbols in that namespace
//are interned into the current environment.
//
public class LEnvironment {
    
    //every Environment has a current namespace. This can be changed dynamically (like current working directory)
    private LNamespace currentNamespace;

    //an Environment has its own symbol table.
    private HashMap<String,LSymbol> symbols = new HashMap<String,LSymbol>();
    
    public static final LNamespace ELL_CORE_NS = new LNamespace("ell.core");
    public static final LNamespace JAVA_LAND_NS = new LNamespace("java.lang");

    public static final LSymbol SYM_DEF = new LSymbol(ELL_CORE_NS, "def");
    public static final LSymbol SYM_IF = new LSymbol(ELL_CORE_NS, "if");
    public static final LSymbol SYM_DO = new LSymbol(ELL_CORE_NS, "do");
    public static final LSymbol SYM_QUOTE = new LSymbol(ELL_CORE_NS, "quote");
    public static final LSymbol SYM_FN = new LSymbol(ELL_CORE_NS, "fn");
    public static final LSymbol SYM_SET = new LSymbol(ELL_CORE_NS, "set!");

    public static final LSymbol SYM_QUASIQUOTE = new LSymbol(ELL_CORE_NS, "quasiquote");
    public static final LSymbol SYM_UNQUOTE = new LSymbol(ELL_CORE_NS, "unquote");
    public static final LSymbol SYM_UNQUOTE_SPLICING = new LSymbol(ELL_CORE_NS, "unquote-splicing");

    public static final LSymbol SYM_DOT = new LSymbol(ELL_CORE_NS, ".");
    public static final LSymbol SYM_AMP = new LSymbol(ELL_CORE_NS, "&");

    /**
     * Create a new environment, using the specified namespace.
     * @param ns the initial value for the environment's current namespace
     */
    public LEnvironment(LNamespace ns) {
        currentNamespace = ns;
        internEllCore();
    }

    /**
     * Create a new environment, defaulting to the "user" namespace
     */
    public LEnvironment() {
        this(LNamespace.forName("user"));
    }

    /**
     * Change the environment's current namespace to that specified.
     * @param ns the new namespace for the environment
     * @returns the previous namespace
     */
    public LNamespace setCurrentNamespace(LNamespace ns) {
        LNamespace tmp = currentNamespace;
        currentNamespace = ns;
        return tmp;
    }
    
    /**
     * @returns the current namespace used by the environment
     */
    public LNamespace currentNamespace() {
        return currentNamespace;
    }
	
    /**
     * Return the LSymbol for the simple name in the environment. If the symbol
     * is created, it is created in the current namespace.
     * @param name the simple name of the symbol this environment
     * @returns the LSymbol for that name
     */
    public LSymbol intern(String name) {
        LSymbol sym = symbols.get(name);
        if (sym == null) {
            sym = new LSymbol(currentNamespace, name);
            symbols.put(name, sym);
        }
        return sym;
    }

    public LSymbol intern(LSymbol sym) {
        String name = sym.name;
        LSymbol prevSym = symbols.get(name);
        if (prevSym != sym) {
            if (prevSym != null) {
                //if (prevSym.value != LSymbol.UNBOUND) {
                    //this is like clojure 'refer', to intern a symbol from another namespace. CLojure 1.4 REPL does not throw, it overwrites. Hmm.
                    throw new LError("symbol already defined, cannot intern from a different namespace");
                    //}
            }
            symbols.put(name, sym);
        }
        return sym;
    }

    public void refer(LNamespace ns) {
        for (LSymbol sym : ns.symbols()) {
            intern(sym);
        }
    }

    private void internEllCore() {
        //        LNamespace prevNamespace = setCurrentNamespace(ELL_CORE_NS);
        intern(SYM_DEF);
        intern(SYM_IF);
        intern(SYM_DO);
        intern(SYM_QUOTE);
        intern(SYM_FN);
        intern(SYM_SET);
        //        setCurrentNamespace(prevNamespace);
    }

    private Collection<LSymbol> requireJavaPackage(String name) {
        //return a collection of all public classes in the package, as LSymbols bound to class objects
        //i.e. java.lang.String will be bound to a symbol with "java.lang" as the namespace, and "String" as the name
        //in this environment, it can be referred to as "java.lang.String". Either get(name) is smarter, or I defined a symbol with the long name.
        // -> I kind of prefer just using the string. Then, "import" would additionally bind the simple names
        //so, Namespaces as real objects, keeping track of all their symbols (i.e. Namespace.intern(simpleName) always returns the same object)
        //then, to intern into the environment, the references are duplicated, i.e. the simple (or compound) name added to the environment symboltable.
        return null;
    }

    private void importJavaPackage(String name) {
        //intern all public classes in the package as symbols
    }

    /**
     * Returns the symbol in the environment with the specified simple name.
     * @returns the LSymbol, or null if it doesn't exist in this environment.
     */
    public LSymbol getSymbol(String name) {
        return symbols.get(name);
    }

    /**
     * Returns the value associated with the symbol in the environment. If no value has
     * been bound, the result returned is the UNBOUND singleton object.
     * @param name the simple name of the symbol
     * @returns the value bound to the symbol, or UNBOUND if not bound
     */
    public Object get(String name) {
        if ("*ns*".equals(name))
            return currentNamespace;
        LSymbol sym = symbols.get(name);
        if (sym == null || sym.value == LSymbol.UNBOUND)
            return LSymbol.UNBOUND;
        return sym.value;
    }

    /**
     * @returns the collectionof symbols in this environment
     */
    public Collection<LSymbol> symbols() {
        return symbols.values();
    }
    
    //interns everything in the other namespace
    //public void refer(String anotherNs) {
        //bind all public symbols in the other namespace. I.e. bind all the classnames for java.
    //}
    	
}

