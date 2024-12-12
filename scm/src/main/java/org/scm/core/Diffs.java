package org.scm.core;

import org.scm.models.Commit;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

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

            for (int i = 0; i < changes.get("modifiedShas").toArray().length; i += 2) {
                // Ensure we don't go out of bounds
                if (i + 1 < changes.get("modifiedShas").toArray().length) {
                    compareBlobs((String) changes.get("modifiedShas").toArray()[i], (String) changes.get("modifiedShas").toArray()[i + 1]);
                }
            }

            System.out.println("Added files: " + changes.get("added"));
            System.out.println("Deleted files: " + changes.get("deleted"));
            System.out.println("Modifies files: " + changes.get("modified"));


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

    private static byte[] decompressBlob(String blobSha) throws IOException {
        // Locate the blob file in the object storage
        File objectFile = getObjectFile(blobSha);
        if (!objectFile.exists()) {
            throw new FileNotFoundException("Blob object not found: " + blobSha);
        }

        // Decompress the object (e.g., using zlib)
        return decompressObject(objectFile);
    }

    private static File getObjectFile(String sha) {
        String objectPath = ".gitty/objects/" + sha.substring(0, 2) + "/" + sha.substring(2);
        return new File(objectPath);
    }

    private static byte[] decompressObject(File objectFile) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(objectFile);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            InflaterInputStream inflater = new InflaterInputStream(fileInputStream);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inflater.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            return out.toByteArray();
        }
    }

    public static void compareBlobs(String blobSha1, String blobSha2) throws IOException {
        // Decompress the blobs
        String content1 = new String(decompressBlob(blobSha1), StandardCharsets.UTF_8);
        String content2 = new String(decompressBlob(blobSha2), StandardCharsets.UTF_8);

        // Split the content into lines
        List<String> lines1 = Arrays.asList(content1.split("\n"));
        List<String> lines2 = Arrays.asList(content2.split("\n"));

        // Compare lines
        System.out.println("Differences:");
        int maxLines = Math.max(lines1.size(), lines2.size());
        for (int i = 0; i < maxLines; i++) {
            String line1 = i < lines1.size() ? lines1.get(i) : "";
            String line2 = i < lines2.size() ? lines2.get(i) : "";
            if (!line1.equals(line2)) {
                System.out.println("Line " + (i + 1) + ":");
                System.out.println("< " + line1);
                System.out.println("> " + line2);
            }
        }
    }
}
