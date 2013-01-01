#!/bin/bash

ROOT=`pwd`
export CLASSES=$ROOT/target/classes
export SRC=$ROOT/src
export LIB=$ROOT/lib
export CLASSPATH="$CLASSES:$LIB/*:"

function compile {
    (cd $1;
        files=''
         for file in $( find . -name '*.java' )
             do
                 CLASS=`echo $CLASSES/${file%.java}`.class
                 if [ ! -e $CLASS ] || [ $file -nt $CLASS ] ; then
                       files="$files $file"
                 fi
             done
         if [ ${#files} -ne 0 ]; then 
            echo javac $files
            javac -d $CLASSES -Xlint:unchecked -Xlint:deprecation $files
            if [ $? -ne 0 ]; then
                exit 1;
            fi
         fi
    )
}

function compile_dirs {
   for dir in $*; do
       compile $dir || exit 1
   done
}

function ensure_build_dir {
    mkdir -p $CLASSES
}

function run {
    if [ $2 ]; then
        shift
        java -Xmx1g org.jelled.Main $*
    else
        rlwrap java -Xmx1g org.jelled.Main repl.scm
    fi
}

function run_java {
    if [ $2 ]; then
        shift
        java -Xmx1g $*
    else
        usage;
    fi
}

function run_tests {
    if [ $2 ]; then
        shift
        java -Xmx1g org.jelled.test.$*
    else
        java -Xmx1g org.jelled.test.TestMain
    fi
}

function clean {
     (cd $1;
        for file in $( find . -name '*.class' )
            do
                rm -f $file
            done
        for file in $( find . -name '*~' )
            do
                rm -f $file
            done
        for file in $( find . -name '.DS_Store' )
            do
                rm -f $file
            done
    )
}

function clean_dirs {
   rm -rf $CLASSES
   for dir in $*; do
       clean $dir
   done
}

function usage {
    echo "usage: m [clean] [build] [classpath] [cp] [run class [args]]"
    exit 0
}

case $1 in
    clean) clean_dirs $SRC; exit 0;;
    build) ensure_build_dir && compile_dirs $SRC/main/java; exit 0;;
    test) ensure_build_dir && compile_dirs $SRC/main/java && compile_dirs $SRC/test/java; echo ok; run_tests $*; exit 0;;
    run) run $*; exit 0;;
    java) run_java $*; exit 0;;
    classpath) echo $CLASSPATH; exit 0;;
    *) usage; exit 1;;
esac
