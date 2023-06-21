package pl.luczak.michal;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileOperationExecutor {

    static void deleteFile(File file) throws IOException {
        if (file.isDirectory()) {
            FileUtils.deleteDirectory(file);
        } else {
            FileUtils.delete(file);
        }
    }

    static void copyFile(File srcFile, Path destFile) throws IOException {
        if (srcFile.isDirectory()) {
            FileUtils.copyDirectory(srcFile, new File(destFile.toUri()));
        } else {
            FileUtils.copyFile(srcFile, new File(destFile.toUri()));
        }
    }

    static void createDirectory(String path, String name) {
        try {
            Files.createDirectory(Paths.get(path, name));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
