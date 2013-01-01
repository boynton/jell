package org.jelled.core;
import java.io.Reader;
import java.io.Writer;

public interface ILanguage {
	
	/**
	 * Initialize the language, define globals, etc. Gets called once at VM construction time.
	 * The environment, its default namespace, and the VM will already be initialized.
	 * @param vm the VM to use this language with
	 */
    public void init(LVM vm);

	/**
	 * Read the next object from the input stream.
	 * @param in the java.io.Reader to read from
	 * @returns the next Object read, or LVM.EOS on end-of-stream.
	 */
    public Object read(Reader in);

	/**
	 * Write the object to the java.io.Writer. The forRead flag indicates that the output
	 * should be parseable by the read() method; if false, the output should be optimized
	 * for human readability.
         * @param obj the object to write
	 * @param out the java.io.Writer to direct output to
	 * @param forRead a flag indicating that the output should be parseable by read()
	 */
    public void write(Object obj, Writer out, boolean forRead);

	/**
	 * Expand all macros present in the expression, replacing them with core language primitives,
	 * Note that the result may contain direct calls to primitives defined in this language, only
	 * syntactic forms are expanded.
	 *
	 * @param expr the language expression to expand
	 * @returns an expression containing only core ℒ syntactic forms
	 */
    public Object expand(Object expr);

	/**
	 * Compile the core expression to code
	 * @param expr an expression containing only core ℒ syntactic forms
	 * @returns the code object representing the expression
	 */
    public LCode compile(Object expr);

}