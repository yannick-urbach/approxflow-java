# approxflow-java

## Building

### Build Requirements
- JDK version 1.8
- Apache Maven

### Build Instructions
Clone the repository with submodules:

    $ git clone --recursive https://github.com/yannick-urbach/approxflow-java.git

Change into the project directory:

    $ cd approxflow-java

Run the build script:

    $ ./build.sh

## Usage

### Runtime Requirements
- JDK version 1.8
- JBMC version &ge; 5.17
- ApproxMC version &ge; 4

### Normal Usage
To analyze the information flow of a Java program run the following command in
the project directory:

    $ ./approxflow.sh <class_path>
    
where <class_path> is the path to the directory containing the Java source files
for the program.

#### Example
Run

    $ ./approxflow.sh test/OutParameter
    
to run the OutParameter test case.

### Test suite
To run all provided test cases and get a brief test summary, run the following
command in the project directory:

    $ ./approxflow.sh --tests
    
Detailed test results will be written to result.txt in each individual test
directory.

#### Adding a test case
To add a new test case,
-   create a new subdirectory in test; the name of the directory is the name of
    the test case
-   add Java source files
-   add an xml file called config.xml containing the maximum expected
    information flow in the node /test/maxflow and the minimum expected
    information flow in the node /test/minflow

### Other options
#### --keep-intermediate
Do not delete temporary files created for and by external tools used by the
program

#### --maxcount-k *k*
Passed as k parameter to MaxCount. Default is 1.

#### --tolerance *t*
Passed as tolerance (epsilon) to the model counters. Default is 0.8.

#### --confidence *c*
Passed as confidence (1 - delta) to the model counters. Default is 0.8.

#### --inline *depth*
Specifies the inlining depth

#### --loops-unroll *iterations*
Specifies the number of iterations to unroll

#### --loops-blackbox
Replace aborted loop iterations with blackboxes