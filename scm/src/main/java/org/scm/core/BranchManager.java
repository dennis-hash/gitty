package org.scm.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class BranchManager {
    public void createBranch(String branchName) throws IOException {
        // Get the current HEAD reference
        File headFile = new File(".git/HEAD");
        if (!headFile.exists()) {
            throw new IOException("No .git directory found. Are you inside a repository?");
        }

        // Read the current HEAD content
        String currentHead = Files.readString(headFile.toPath()).trim();
        System.out.println("current head 1"+currentHead);

        // If HEAD points to a branch, resolve the commit hash
        if (currentHead.startsWith("ref: ")) {
            String branchPath = currentHead.substring(5); // Remove "ref: " prefix
            File branchFile = new File(".git/" + branchPath);
            if (!branchFile.exists()) {
                throw new IOException("Current branch reference does not exist: " + branchPath);
            }
            currentHead = Files.readString(branchFile.toPath()).trim(); // Read the commit hash
        }

        // Create the new branch file in .git/refs/heads
        File newBranchFile = new File(".git/refs/heads/" + branchName);
        if (newBranchFile.exists()) {
            System.out.println("Branch " + branchName + " already exists.");
            return;
        }
//
//        // Write the commit hash to the new branch file
        newBranchFile.getParentFile().mkdirs(); // Ensure parent directory exists
        System.out.println("current head"+currentHead);
        System.out.println("current head"+ newBranchFile.toPath());
        Files.writeString(newBranchFile.toPath(), currentHead); // Write the commit hash
        System.out.println("Branch " + branchName + " created, pointing to commit " + currentHead + ".");
    }

    public void switchBranch(String branchName) throws IOException {
        // Step 1: Verify if the branch exists
        File branchFile = new File(".git/refs/heads/" + branchName);
        if (!branchFile.exists()) {
            throw new IOException("Branch " + branchName + " does not exist.");
        }

        // Step 2: Read the commit hash the branch points to
        String branchCommitHash = Files.readString(branchFile.toPath()).trim();

        // Step 3: Update the HEAD file to point to the new branch
        File headFile = new File(".git/HEAD");
        if (!headFile.exists()) {
            throw new IOException("No .git directory found. Are you inside a repository?");
        }

        String newHeadContent = "ref: refs/heads/" + branchName;
        Files.writeString(headFile.toPath(), newHeadContent);

        System.out.println("Switched to branch '" + branchName + "'.");
        System.out.println("Branch now points to commit " + branchCommitHash + ".");
    }
}
