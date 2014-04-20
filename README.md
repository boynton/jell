jell
====

A Java implementation of the ℒ (ell) language.

This implementation defines Data representations, a Notation for reading/writing it, 
a Runtime that defines a little VM for L, and a Compiler to generate VM code from 
the Ell source.

Ell is a core language, so currently some other lisp is useful to macroexpand normal looking
programs into it.
This implementation defines a core package that is the basic environment. It then defines
an R4RS Scheme system on top of it.

## Usage

For now, you can build with maven, or just run the bash script to build it ("./m build"). Then try running
a simple test with the lap code (assembler for the vm):

```
./m run org.jelled.Ell src/main/lap/hello.lap
```

To run a core Ell program, i.e. one that calculates digits of pi:

```
./m run org.jelled.Ell src/main/ell/pi.ell 
```

More on this coming soon.


