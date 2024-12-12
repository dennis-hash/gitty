package org.scm.core;

import org.scm.models.Commit;

import java.io.*;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

public class Diffs {
        public void diffBranches(String otherBranch) throws IOException {
            //get current branch
            String currentBranchName = getCurrentBranch();
            String currentBranchCommitSha = getBranchLatestCommitSha(currentBranchName);
            String otherBranchCommitSha = getBranchLatestCommitSha(otherBranch);

            //get  commits
            CommitManager commitManager = new CommitManager();
            Commit currentBranchCommit = commitManager.readCommit(currentBranchCommitSha);
            Commit otherBranchCommit = commitManager.readCommit(otherBranchCommitSha);

            //compare tree
            TreeManager treeManager = new TreeManager();
            Map<String, List<String>> changes = treeManager.compareTrees(currentBranchCommit.getTreeSha(), otherBranchCommit.getTreeSha());

            if(changes.get("added").isEmpty() && changes.get("deleted").isEmpty() && changes.get("modified").isEmpty()){
                System.out.println("no difference between branches"+currentBranchName+"and"+otherBranch);
            }



        }

        public String getCurrentBranch() throws IOException {
            File headFile = new File(".gitty/HEAD");
            if (!headFile.exists()) {
                throw new IOException("No .gitty repository found. Are you inside a repository?");
            }

            String headContent = Files.readString(headFile.toPath()).trim();
            if (!headContent.startsWith("ref: ")) {
                throw new IOException("HEAD does not point to a valid branch reference.");
            }

            String branchPath = headContent.substring(5);

            String branchName = branchPath.replace("refs/heads/", "");

            return branchName;
        }


    public String getBranchLatestCommitSha(String branchName) throws IOException {
        String branchPath = "refs/heads/"+branchName;
        File branchFile = new File(".gitty/" + branchPath);

        if (!branchFile.exists()) {
            return null;
        }

        String latestCommitSha = Files.readString(branchFile.toPath()).trim();
        return latestCommitSha.isEmpty() ? null : latestCommitSha;
    }

    private String decompress(File compressedFile) throws IOException {
        try (InputStream fileStream = new FileInputStream(compressedFile);
             GZIPInputStream gzipStream = new GZIPInputStream(fileStream)) {
            StringBuilder output = new StringBuilder();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipStream.read(buffer)) != -1) {
                output.append(new String(buffer, 0, len));
            }
            return output.toString();
        }
    }

    // Method to get unique lines between two blobs
    public Set<String> getUniqueLines(String sha1, String sha2) throws IOException {
        // Retrieve blob contents
        File objectFile = new TreeManager().getObjectFile(sha1);
        File objectFile2 = new TreeManager().getObjectFile(sha2);

        // Decompress the blobs
        String blob1Content = decompress(objectFile);
        String blob2Content = decompress(objectFile2);

        // Split contents into lines and store in sets
        Set<String> linesBlob1 = new HashSet<>(Set.of(blob1Content.split("\\R"))); // Split by line breaks
        Set<String> linesBlob2 = new HashSet<>(Set.of(blob2Content.split("\\R")));

        // Find unique lines
        Set<String> uniqueToBlob1 = new HashSet<>(linesBlob1);
        uniqueToBlob1.removeAll(linesBlob2); // Lines in blob1 not in blob2

        Set<String> uniqueToBlob2 = new HashSet<>(linesBlob2);
        uniqueToBlob2.removeAll(linesBlob1); // Lines in blob2 not in blob1

        // Combine unique lines from both blobs
        uniqueToBlob1.addAll(uniqueToBlob2);

        return uniqueToBlob1;
    }
}
