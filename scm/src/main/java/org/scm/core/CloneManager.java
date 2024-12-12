package org.scm.core;

import org.scm.models.Commit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class CloneManager {
    public void cloneRepo(String targetPath) throws IOException {

        CommitManager commitManager = new CommitManager();
        String commitSha = commitManager.getLatestCommitSha();

        // Read commit to get tree
        Commit commit = commitManager.readCommit(commitSha);
        String tree = commit.getTreeSha();

        TreeManager treeManager = new TreeManager();
        Map<String, String> treeEntries = treeManager.readTree(tree);
        Set<String> paths = new HashSet<>();
        Set<String> files = new HashSet<>();

        // Separate paths and files
        for (String filePath : treeEntries.keySet()) {
            if (filePath.endsWith("/")) {
                paths.add(filePath);
            } else {
                files.add(filePath);
            }
        }

        // Construct full paths for files
        Set<String> fullPaths = new HashSet<>();
        for (String path : paths) {
            File directory = new File(path);
            if (directory.exists() && directory.isDirectory()) {
                for (String file : files) {
                    // Check if the file exists in the directory
                    File potentialFile = new File(directory, file);
                    if (potentialFile.exists()) {
                        // Construct the full path
                        fullPaths.add(potentialFile.getPath());
                    }
                }
            }
        }

        // Clone files to target path
        for (String fullPath : fullPaths) {
            File sourceFile = new File(fullPath);
            // Determine the relative path from the original structure
            String relativePath = sourceFile.getParentFile().getPath().replace(File.separator, "/");
            String targetFilePath = targetPath + "/" + relativePath + "/" + sourceFile.getName();

            // Create directories in the target path
            File targetFile = new File(targetFilePath);
            targetFile.getParentFile().mkdirs();


            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println("Files cloned successfully to: " + targetPath);
    }


}
