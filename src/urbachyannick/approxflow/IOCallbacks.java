package urbachyannick.approxflow;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class IOCallbacks implements Closeable {
    private static class Temporary {
        public Path path;
        public String name;

        public Temporary(String name, Path path) {
            this.name = name;
            this.path = path;
        }
    }

    private final List<Temporary> temporaries;

    public IOCallbacks() {
        temporaries = new ArrayList<>();
    }

    protected abstract Path createTemporaryFileImpl(String name);
    protected abstract Path createTemporaryDirectoryImpl(String name);
    protected abstract boolean shouldDeleteTemporary(String name);

    public Path createTemporaryFile(String name) throws IOException {
        Path p = createTemporaryFileImpl(name);

        if (Files.exists(p))
            delete(p);

        temporaries.add(new Temporary(name, p));
        return p;
    }

    public Path createTemporaryDirectory(String name) throws IOException {
        Path p = createTemporaryDirectoryImpl(name);

        if (Files.exists(p))
            delete(p);

        Files.createDirectories(p);
        temporaries.add(new Temporary(name, p));
        return p;
    }

    @Override
    public void close() throws IOException {
        for (Temporary t : temporaries) {
            if (Files.exists(t.path) && shouldDeleteTemporary(t.name)) {
                delete(t.path);
            }
        }
    }

    private void delete(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder()) // to delete bottom to top
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }
}
