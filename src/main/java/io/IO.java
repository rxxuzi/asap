package io;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class IO {

    /**
     * Creates a directory at the specified path.
     *
     * @param path the path of the directory to create (String)
     * @return true if the directory was successfully created, false otherwise
     */
    public static boolean mkdir(String path) {
        return mkdir(Paths.get(path));
    }

    /**
     * Creates a directory at the specified path.
     *
     * @param path the path of the directory to create (Path)
     * @return true if the directory was successfully created, false otherwise
     */
    public static boolean mkdir(Path path) {
        return createDirectory(path, false);
    }

    /**
     * Creates a directory and all non-existent parent directories at the specified path.
     *
     * @param path the path of the directory to create (String)
     * @return true if the directories were successfully created, false otherwise
     */
    public static boolean mkdirs(String path) {
        return mkdirs(Paths.get(path));
    }

    /**
     * Creates a directory and all non-existent parent directories at the specified path.
     *
     * @param path the path of the directory to create (Path)
     * @return true if the directories were successfully created, false otherwise
     */
    public static boolean mkdirs(Path path) {
        return createDirectory(path, true);
    }

    /**
     * Checks if a directory exists at the specified path.
     *
     * @param path the path of the directory to check (String)
     * @return true if the directory exists, false otherwise
     */
    public static boolean exists(String path) {
        return exists(Paths.get(path));
    }

    /**
     * Checks if a directory exists at the specified path.
     *
     * @param path the path of the directory to check (Path)
     * @return true if the directory exists, false otherwise
     */
    public static boolean exists(Path path) {
        return Files.exists(path);
    }

    private static boolean createDirectory(Path path, boolean recursive) {
        try {
            if (Files.notExists(path)) {
                if (recursive) {
                    Files.createDirectories(path);
                } else {
                    Files.createDirectory(path);
                }
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Recursively deletes a directory and all its contents.
     *
     * <p>This method deletes the specified directory and all files and subdirectories within it.
     * If an error occurs during deletion, the method returns {@code false}.
     *
     * @param path the path to the directory to be deleted
     * @return {@code true} if the directory and all contents were successfully deleted,
     *         {@code false} if an error occurred during deletion
     */
    public static boolean rmdirs(Path path) {
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
