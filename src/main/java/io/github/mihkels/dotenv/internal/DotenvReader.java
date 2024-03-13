package io.github.mihkels.dotenv.internal;

import io.github.mihkels.dotenv.DotenvException;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * (Internal) Reads a .env file
 */
public class DotenvReader {
    private final String directory;
    private final String filename;

    /**
     * Creates a dotenv reader
     * @param directory the directory containing the .env file
     * @param filename the file name of the .env file e.g. .env
     */
    public DotenvReader(String directory, String filename) {
        this.directory = directory;
        this.filename = filename;
    }

    /**
     * (Internal) Reads the .env file
     * @return a list containing the contents of each line in the .env file
     * @throws DotenvException if a dotenv error occurs
     * @throws IOException if an I/O error occurs
     */
    public List<String> read() throws DotenvException, IOException {
        var workingPath = getWorkingPath();

        if (Files.exists(workingPath)) {
            return Files.readAllLines(workingPath);
        }

        var result = loadFromGivenPaths();
        if (Files.exists(result.path)) {
            return Files.readAllLines(result.path);
        }

        try {
            return ClasspathHelper
                .loadFileFromClasspath(result.location.replaceFirst("^\\./", "/"))
                .collect(Collectors.toList());
        } catch (DotenvException e) {
            Path cwd = FileSystems.getDefault().getPath(".").toAbsolutePath().normalize();
            String cwdMessage = !result.path.isAbsolute() ? "(working directory: " + cwd + ")" : "";
            e.addSuppressed(new DotenvException("Could not find " + result.path + " on the file system " + cwdMessage));
            throw e;
        }
    }

    private Path getWorkingPath() {
        var workingDir = System.getProperty("user.dir");
        var workingLocation = String.format("%s/%s", workingDir, filename);
        return Paths.get(workingLocation);
    }

    private Result loadFromGivenPaths() {
        String dir = directory
            .replace("\\\\", "/")
            .replaceFirst("\\.env$", "")
            .replaceFirst("/$", "");

        String location = dir + "/" + filename;
        String lowerLocation = location.toLowerCase();
        Path path = (
            lowerLocation.startsWith("file:")
                || lowerLocation.startsWith("android.resource:")
                || lowerLocation.startsWith("jimfs:")
        ) ? Paths.get(URI.create(location)) : Paths.get(location);
        return new Result(location, path);
    }

    private static class Result {
        public final String location;
        public final Path path;

        public Result(String location, Path path) {
            this.location = location;
            this.path = path;
        }
    }
}
