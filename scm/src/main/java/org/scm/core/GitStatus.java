package org.scm.core;

import org.scm.models.IndexEntry;
import org.scm.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GitStatus {
    public void checkStatus() throws IOException, NoSuchAlgorithmException {
        // Read the staged files (index entries)
        List<IndexEntry> stagedEntries = new IndexManager().readIndex();
        System.out.println("======================================================");
         for(IndexEntry indexEntry: stagedEntries){
             System.out.println("IndexEntry{" +
                     "path='" + indexEntry.getPath() + '\'' +
                     ", sha1='" + indexEntry.getSha1() + '\'' +
                     ", modifiedTime=" + indexEntry.getModifiedTime() +
                     ", size=" + indexEntry.getSize() +
                     '}');

         }
        System.out.println("======================================================");
        Map<String, IndexEntry> stagedEntriesMap = new HashMap<>();
        for (IndexEntry entry : stagedEntries) {
            stagedEntriesMap.put(entry.getPath(), entry);
        }

        // Get the files in the working directory
        List<File> workingFiles = getWorkingFiles();

        // Track changes
        boolean isClean = true;
        List<String> changedFiles = new ArrayList<>();

        // Check for changes (modified or new files)
        for (File file : workingFiles) {
            String filePath = file.getPath();
            String sha1 = HashUtils.computeSHA1(FileUtils.readFile(file));

            IndexEntry stagedEntry = stagedEntriesMap.get(filePath);
            if (stagedEntry == null) {
                changedFiles.add(filePath + " (new file)");
                isClean = false;
            } else if (!stagedEntry.getSha1().equals(sha1)) {
                changedFiles.add(filePath + " (modified)");
                isClean = false;
            }
        }

        // Check for deleted files
        for (IndexEntry stagedEntry : stagedEntries) {
            File file = new File(stagedEntry.getPath());
            if (!file.exists()) {
                changedFiles.add(stagedEntry.getPath() + " (deleted)");
                isClean = false;
            }
        }

        // Output status
        if (isClean) {
            System.out.println("On branch main");
            System.out.println("nothing to commit, working tree clean");
        } else {
            System.out.println("Changes not staged for commit:");
            for (String file : changedFiles) {
                System.out.println(file);
            }
        }
    }

    // Helper method to retrieve working files in the current directory
    private List<File> getWorkingFiles() {
        FileScanner scanner = new FileScanner();
        scanner.scanDirectory(".");
        return scanner.getFiles();
    }
}
