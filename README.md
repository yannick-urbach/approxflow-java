# approxflow-java

## Building

### Build Requirements
- JDK version 1.8
- Apache Maven

### Build Instructions
#### Using the provided build script
In the project root directory, run

    $ ./build.sh
    
#### Using maven directly
In the project root directory, run

    $ mvn clean compile assembly:single


## Usage

### Runtime Requirements
- JDK version 1.8
- JBMC version &ge; 5.17 

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

#### --blackbox-experimental
Use experimental blackbox handling
