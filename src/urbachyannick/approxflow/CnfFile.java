package urbachyannick.approxflow;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A CNF file on the disk. The contents are not kept in memory.
 */
public class CnfFile {
    private final Path path;
    private final int variablesCount;
    private final int clausesCount;

    /**
     * Open an existing CNF file at a given path
     *
     * @param path the path of the CNF file
     * @throws IOException if the file can not be read
     * @throws CnfException if the file does not have a valid CNF problem line
     */
    public CnfFile(Path path) throws IOException, CnfException {
        this.path = path;

        String[] problemLine = Files.lines(path)
                .filter(line -> line.charAt(0) == 'p')
                .findFirst().orElseThrow(() -> new CnfException("No problem line."))
                .split(" ");

        if (!problemLine[0].equals("p") || !problemLine[1].equals("cnf") || problemLine.length != 4)
            throw new CnfException("Invalid CNF file.");

        variablesCount = Integer.parseInt(problemLine[2]);
        clausesCount = Integer.parseInt(problemLine[3]);
    }

    /**
     * Creates a new CNF file that is identical to this CNF file, except that all variables are renamed by passing the
     * literals through the given renaming function. The renaming function should be a permutation of the variables, and
     * should preserve the sign of its argument,
     *
     * @param renameFunction the renaming function
     * @return the new CNF file with renamed variables
     * @throws IOException if this CNF file can not be read, or the new CNF file can not be created or written to
     */
    public CnfFile renameVariables(IntUnaryOperator renameFunction) throws IOException {
        Path newPath = FilesUtil.getNext(path);

        Stream<String> newClauseLines = getClauseLines().map(line -> line.renameLiterals(renameFunction).toString());
        Stream<String> newVarLines = getVarLines().map(line -> line.renameLiterals(renameFunction).toString());
        Stream<String> newIndLines = getIndLines().map(line -> line.renameLiterals(renameFunction).toString());
        Stream<String> newCrLines = getCrLines().map(line -> line.renameLiterals(renameFunction).toString());

        try (BufferedWriter writer = Files.newBufferedWriter(newPath)) {
            writer.write("p cnf " + getVariablesCount() + " " + getClausesCount() + "\n");
            FilesUtil.writeLines(writer, newClauseLines);
            FilesUtil.writeLines(writer, newVarLines);
            FilesUtil.writeLines(writer, newIndLines);
            FilesUtil.writeLines(writer, newCrLines);
        }

        try {
            return new CnfFile(newPath);
        } catch (CnfException e) {
            throw new Unreachable();
            // unless the file is modified between the write statements and the return statement
            // let's assume that doesn't happen
        }
    }

    /**
     * Creates a new CNF file that is identical to this CNF file, except that the given variables are renamed to the
     * range 1..variables.count(), and the variables that previously had those names are renamed accordingly to avoid
     * collisions.
     *
     * @param variables the variables to move to the bottom
     * @return the new CNF file with renamed variables
     * @throws IOException if this CNF file can not be read, or the new CNF file can not be created or written to
     */
    public CnfFile renameVariablesToBottom(IntStream variables) throws IOException {
        // generate the rename map
        //
        // Note that the trivial approach of swapping current with desired fails if the set of variables to rename
        // overlaps with the set of desired names.
        //
        // Still, there must be a more elegant way to do this. Anyways, the following works:

        Map<Integer, Integer> renameMap = new HashMap<>();

        int[] variablesArray = variables.map(Math::abs).toArray(); // variables to rename (negation doesn't matter)
        Set<Integer> free = new HashSet<>(); // variable names not currently used
        Set<Integer> pending = new HashSet<>(); // variables we will have to rename to avoid collisions

        // do the intended renaming first
        for(int i = 0; i < variablesArray.length; ++i) {
            renameMap.put(variablesArray[i], i + 1);
            renameMap.put(-variablesArray[i], -(i + 1)); // for negated literals

            // if the old name was pending for renaming, it isn't anymore
            // otherwise it is now free
            if (!pending.remove(variablesArray[i]))
                free.add(variablesArray[i]);

            // if the new name was free, it isn't anymore
            // otherwise we will have to rename it later to avoid collision
            if (!free.remove(i + 1))
                pending.add(i + 1);

            // free and pending are balanced: in each iteration, we either remove one from pending or add one to free,
            // and remove one from free or add one to pending.
        }

        // then assign the free names to the pending variables
        Iterator<Integer> freeIterator = free.iterator();
        Iterator<Integer> todoIterator = pending.iterator();

        while (todoIterator.hasNext()) {
            int f = freeIterator.next();
            int p = todoIterator.next();

            renameMap.put(p, f);
            renameMap.put(-p, -f); // for negated literals
        }

        return renameVariables(literal -> {
            Integer mapped = renameMap.get(literal);
            return mapped == null ? literal : mapped;
        });
    }

    /**
     * Gets all variable lines from this CNF file.
     *
     * @return the variable lines in this CNF file
     * @throws IOException if this CNF file can not be read
     */
    public Stream<CnfVarLine> getVarLines() throws IOException {
        return Files.lines(path)
                .filter(line -> line.charAt(0) == 'c' && !line.startsWith("c ind "))
                .map(CnfVarLine::new);
    }

    /**
     * Gets all clause lines from this CNF file.
     *
     * @return the clause lines in this CNF file
     * @throws IOException if this CNF file can not be read
     */
    public Stream<CnfClauseLine> getClauseLines() throws IOException {
        return Files.lines(path)
                .filter(line -> line.charAt(0) != 'c' && line.charAt(0) != 'p')
                .map(CnfClauseLine::new);
    }

    /**
     * Gets all ApproxMC scope lines (c ind ...) from this CNF file.
     *
     * @return the ApproxMC scope lines in this CNF file
     * @throws IOException if this CNF file can not be read
     */
    public Stream<CnfIndLine> getIndLines() throws IOException {
        return Files.lines(path)
                .filter(line -> line.startsWith("c ind "))
                .map(CnfIndLine::new);
    }

    /**
     * Gets all ApproxMC-py scope lines (cr ...) from this CNF file.
     *
     * @return the ApproxMC-py scope lines in this CNF file
     * @throws IOException if this CNF file can not be read
     */
    public Stream<CnfCrLine> getCrLines() throws IOException {
        return Files.lines(path)
                .filter(line -> line.startsWith("cr "))
                .map(CnfCrLine::new);
    }

    /**
     * Adds the given ApproxMC scope lines (c ind ...) to this CNF file.
     *
     * @param lines the lines to add
     * @throws IOException if this CNF file cannot be written to
     */
    public void addIndLines(Stream<CnfIndLine> lines) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.APPEND);
        FilesUtil.writeLines(writer, lines.map(CnfIndLine::toString));
        writer.close();
    }

    /**
     * Adds the given ApproxMC-py scope lines (cr ...) to this CNF file.
     *
     * @param lines the lines to add
     * @throws IOException if this CNF file cannot be written to
     */
    public void addCrLines(Stream<CnfCrLine> lines) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.APPEND);
        FilesUtil.writeLines(writer, lines.map(CnfCrLine::toString));
        writer.close();
    }

    /**
     * Gets the number of variables in the problem described by this CNF file, as specified in the problem line.
     * @return the number of variables in the problem described by this CNF file
     */
    public int getVariablesCount() {
        return variablesCount;
    }

    /**
     * Gets the number of clauses in the problem described by this CNF file, as specified in the problem line.
     * @return the number of clauses in the problem described by this CNF file
     */
    public int getClausesCount() {
        return clausesCount;
    }

    /**
     * Gets the path of this CNF file.
     * @return the path of this CNF file
     */
    public Path getPath() {
        return path;
    }
}
