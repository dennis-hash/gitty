package org.scm.core;

import org.scm.models.Commit;
import org.scm.models.IndexEntry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class BranchMerger {

    public void mergeBranch(String branchName) throws IOException, NoSuchAlgorithmException {
        // Step 1: Ensure the branch exists
        File branchFile = new File(".gitty/refs/heads/" + branchName);
        if (!branchFile.exists()) {
            throw new IOException("Branch " + branchName + " does not exist.");
        }

        // Step 2: Get the current branch's HEAD SHA
        File headFile = new File(".gitty/HEAD");
        if (!headFile.exists()) {
            throw new IOException("No .gitty directory found. Are you inside a repository?");
        }

        String currentBranchRef = Files.readString(headFile.toPath()).trim();
        if (!currentBranchRef.startsWith("ref: ")) {
            throw new IOException("HEAD is not pointing to a branch.");
        }

        String currentBranch = currentBranchRef.substring(5); // Remove "ref: "
        File currentBranchFile = new File(".gitty/" + currentBranch);
        if (!currentBranchFile.exists()) {
            throw new IOException("Current branch reference does not exist.");
        }
        String currentHeadSha = Files.readString(currentBranchFile.toPath()).trim();

        // Step 3: Get the target branch's HEAD SHA
        String targetHeadSha = Files.readString(branchFile.toPath()).trim();

        // Step 4: Detect conflicting changes (basic conflict detection)
        List<String> conflicts = detectConflicts(currentHeadSha, targetHeadSha);
        if (!conflicts.isEmpty()) {
            System.out.println("Merge aborted: Conflicting changes detected in the following files:");
            for (String conflict : conflicts) {
                System.out.println(" - " + conflict);
            }
            return;
        }

        // Step 5: Create a new merge commit
        List<String> parentShas = new ArrayList<>();
        parentShas.add(currentHeadSha); // Current branch's HEAD
        parentShas.add(targetHeadSha);  // Target branch's HEAD

        // Dummy commit message for simplicity
        String mergeMessage = "Merge branch '" + branchName + "' into " + currentBranch;
        String authorName = "Default Author";
        String authorEmail = "default@example.com";

        String newCommitSha = new CommitManager().writeCommit(mergeMessage, Collections.emptyList(), authorName, authorEmail, String.join(" ", parentShas));

        // Step 6: Update the current branch to point to the new commit
        Files.writeString(currentBranchFile.toPath(), newCommitSha);
        String currentBranchName =  currentBranch.substring(11);
        System.out.println("Merged branch '" + branchName + "' into '" + currentBranchName + "'. New commit: " + newCommitSha);
    }

    // Helper function to detect conflicts
    private List<String> detectConflicts(String currentHeadSha, String targetHeadSha) throws IOException {
        CommitManager commitManager = new CommitManager();

        // Load the commits
        Commit currentCommit = commitManager.readCommit(currentHeadSha);
        Commit targetCommit = commitManager.readCommit(targetHeadSha);

        // Load the trees
        TreeManager treeManager = new TreeManager();
        Map<String, String> currentTree = treeManager.readTree(currentCommit.getTreeSha());
        Map<String, String> targetTree = treeManager.readTree(targetCommit.getTreeSha());

        // Detect conflicting files
        List<String> conflicts = new ArrayList<>();
        for (String filePath : currentTree.keySet()) {
            if (targetTree.containsKey(filePath)) {
                String currentFileSha = currentTree.get(filePath);
                String targetFileSha = targetTree.get(filePath);
                if (!currentFileSha.equals(targetFileSha)) {
                    conflicts.add(filePath); // File exists in both trees with different content
                }
            }
        }

        return conflicts;
    }

}

