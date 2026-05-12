package dk.cintix.librarian.artifact.services.domain.rules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class LibDirectoryManager {

    public void ensureDirectory(Path libDir) throws IOException {
        if (!Files.exists(libDir)) {
            Files.createDirectories(libDir);
        }
    }

    public List<Path> cleanOutdated(Path libDir, Set<String> expectedFiles) throws IOException {
        List<Path> removed = new ArrayList<>();
        if (!Files.exists(libDir)) return removed;
        try (Stream<Path> files = Files.list(libDir)) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (name.endsWith(".jar") && !expectedFiles.contains(name)) {
                    Files.delete(file);
                    removed.add(file);
                }
            }
        }
        return removed;
    }

    public Set<String> existingJars(Path libDir) throws IOException {
        if (!Files.exists(libDir)) return Set.of();
        try (Stream<Path> files = Files.list(libDir)) {
            return files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".jar"))
                    .collect(Collectors.toSet());
        }
    }

    public List<Path> listJars(Path libDir) throws IOException {
        if (!Files.exists(libDir)) return List.of();
        try (Stream<Path> files = Files.list(libDir)) {
            return files.filter(p -> p.getFileName().toString().endsWith(".jar"))
                    .collect(Collectors.toList());
        }
    }
}
