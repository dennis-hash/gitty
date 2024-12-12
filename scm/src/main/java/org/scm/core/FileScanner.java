package org.scm.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileScanner {
    private List<File> files = new ArrayList<>();
    private Set<String> ignorePatterns = new HashSet<>();

    public FileScanner() {
        loadGitIgnore();
    }

    // Load patterns from .gitignore
    private void loadGitIgnore() {
        File gitIgnoreFile = new File(".gitignore");
        if (gitIgnoreFile.exists()) {
            try {
                List<String> lines = Files.readAllLines(gitIgnoreFile.toPath());
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                        ignorePatterns.add(trimmed);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading .gitignore file: " + e.getMessage());
            }
        }
    }

    // Check if a file or directory should be ignored
    private boolean shouldIgnore(File file) {
        String filePath = file.getPath();
        for (String pattern : ignorePatterns) {
            if (filePath.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    public void scanDirectory(String path) {
        File root = new File(path);
        for (File file : root.listFiles()) {
            if (file.isFile() && !shouldIgnore(file)) {
                files.add(file);
            } else if (file.isDirectory() && !shouldIgnore(file)) {
                scanDirectory(file.getPath());
            }
        }
    }

    public List<File> getFiles() {
        return files;
    }
}
