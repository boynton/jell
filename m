#!/bin/bash

# a simple build script. Faster than invoking mvn. Uses standard mvn directory structure,
# and invokes it once to initialize the classpath

export ROOT=`pwd`
export NAME=$(basename `pwd`)
export CLASSES=$ROOT/target/classes
export SRC=$ROOT/src/main
export LIB=$ROOT/lib
export JARFILE=$NAME-0.0.2.jar
export DOC_TARGET=$ROOT/doc

function ensure_classpath {
    if [ ! -e .classpath ]; then
        if [ -e pom.xml ]; then
            echo Calculating classpath...
            mvn -q dependency:build-classpath -Dmdep.outputFile=.classpath
        else
            echo ".:$LIB/*:" > .classpath
        fi
    fi
    export CLASSPATH=".:$ROOT/target/classes:`cat .classpath`"
#    echo CLASSPATH= $CLASSPATH
}

function classpath {
    ensure_classpath
    echo $CLASSPATH
}

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
         echo -e "\033[0;31m=======================================================================================================\033[0;0m"
#         echo $CLASSPATH
         if [ ${#files} -ne 0 ]; then 
            echo javac $files
            javac -d $CLASSES -Xlint:unchecked -Xlint:deprecation $files
            if [ $? -ne 0 ]; then
                exit 1;
            fi
         fi
    )
}

function resources {
    if [ -e $SRC/resources ]; then
        for file in $( cd $SRC/resources; find . -type f)
          do
            path=${file:2}
            src=$SRC/resources/$path
            dst=$CLASSES/$path
            if [ ! -e $dst ] || [ $src -nt $dst ] ; then
                echo cp -p ./src/main/resources/$path ./target/classes/$path
                cp -p $SRC/resources/$path $CLASSES/$path
            fi
          done
    fi
}

function ensure_target_dir {
    if [ ! -e $CLASSES ] ; then mkdir -p $CLASSES; fi
}

function compile_dirs {
    ensure_target_dir
    ensure_classpath
    for dir in $*; do
        compile $dir || exit 1
    done
}

function compile_main {
    if [ -e target/generated-sources/java ]; then
        compile_dirs target/generated-sources/java || return -1
    fi
    compile_dirs src/main/java || return -1
    resources || return -1
}

function make_jar {
    compile_dirs src/main/java || return -1
    (cd $CLASSES; rm -f ../$JARFILE; jar cf ../$JARFILE *)
}

function compile_examples {
    echo "compile examples"
    compile_dirs src/examples/java || return -1
}

function compile_test {
    echo "compile test"
    compile_dirs src/test/java || return -1
}

function run_test {
    ensure_classpath
#    java -Xms1g -Xmx1g -Xss256k org.junit.runner.JUnitCore TestAll
    if [ $2 ]; then
        echo Running $2
        java -Xms1g -Xmx1g -Xss256k org.testng.TestNG -testclass $2
    else
        usage
    fi
}

function prof {
    ensure_classpath
    if [ $2 ]; then
        shift
#        java -Xprof -Xms1g -Xmx1g -Xss256k $*
        java -Xrunhprof:cpu=samples,thread=y,doe=y -Xprof -Xms1g -Xmx1g -Xss256k $*
    else
        usage
    fi
}

function disasm {
    ensure_classpath
    if [ $2 ]; then
        shift
        javap -c $*
    else
        usage
    fi
}

function clean_dir {
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
   clean_dir .
   for dir in $*; do
       clean_dir $dir
   done
}

function clean {
    rm -rf test-output
    rm -rf $CLASSES
    clean_dirs .
}

function really_clean {
    clean
    rm -rf target
    rm -f .classpath
}


function run {
    ensure_classpath
    if [ "$RUN" == "" ]; then
        if [ $2 ]; then
            shift
            java -Xnoclassgc -Xmx1g -Xss200k $*
        else
            usage
        fi
    else
        shift
        java -Xmx1g -Xss200k $RUN $*
    fi
}

function deps {
    rm -f .classpath
    ensure_classpath
}

function usage {
    echo "usage: m [clean] [deps] [reallyclean] [build] [jar] [run class [args]] [test class]"
    exit 0
}

case $1 in
    clean) clean; exit 0;;
    deps) deps; exit 0;;
    reallyclean) really_clean; exit 0;;
    main) compile_main; exit 0;;
    examples) compile_examples; exit 0;;
    gen) compile_dirs gen; exit 0;;
    build) compile_main exit 0;;
    jar) compile_main && make_jar; exit 0;;
    run) run $*; exit 0;;
    disasm) disasm $*; exit 0;;
    prof) prof $*; exit 0;;
    classpath) classpath; exit 0;;
    test) compile_main && compile_test && run_test $*; exit 0;;
    *) usage; exit 1;;
esac

