jell
====

A Java implementation of the ℒ (ell) language, and some test languages on top of it.

This implementation defines a core package that is the basic environment. It then defines
an R4RS Scheme system on top of it.

## Usage

For now, you can build with maven, or just run the bash script to build it ("./m build"). Then try running
a simple test with the core language with this:

```
./m java org.jelled.core.CoreLanguage test1.ell
```

To run a scheme program, try this:

```
./m java org.jelled.scheme.R4RS test1.scm
```

Neither has the necessary primitive functions or syntactic forms provided yet, just the
minimum to run.

This is all just temporary, until I get the rest of the project checked in.
