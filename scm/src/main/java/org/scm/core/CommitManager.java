package org.scm.core;

import org.scm.models.Commit;
import org.scm.models.IndexEntry;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.InflaterInputStream;

public class CommitManager {
    public String writeCommit(String commitMessage, List<IndexEntry> entries, String authorName, String authorEmail, String parentSha)
            throws IOException, NoSuchAlgorithmException {

        TreeManager treeManager = new TreeManager();
        // Step 1: Create the tree object
        String treeSha = treeManager.createTreeObject(entries);

        //check if there is any changes in the tree
        Commit commit = readCommit(parentSha);
        Map<String, List<String>> changes = treeManager.compareTrees(treeSha,commit.getTreeSha());
        // Step 5: Determine the current branch
        File headFile = new File(".gitty/HEAD");
        if (!headFile.exists()) {
            throw new IOException("No .gitty repository found. Are you inside a repository?");
        }

        String headContent = Files.readString(headFile.toPath()).trim();
        if (!headContent.startsWith("ref: ")) {
            throw new IOException("HEAD does not point to a valid branch reference.");
        }

        String branchPath = headContent.substring(5); // Remove "ref: " prefix
        File branchFile = new File(".gitty/" + branchPath);

        // Step 6: Ensure the branch file exists (handle first commit)
        if (!branchFile.exists()) {
            branchFile.getParentFile().mkdirs();
            branchFile.createNewFile();
        }
        String branchName = branchPath.replace("refs/heads/", "");

        if(changes.get("added").isEmpty() && changes.get("deleted").isEmpty() && changes.get("modified").isEmpty()){
            System.out.println("On branch:"+branchName);
            System.out.println("nothing to commit, working tree clean");
            return "";
        }


        // Step 2: Gather commit metadata
        long timestamp = System.currentTimeMillis() / 1000L; // Unix timestamp
        String timezoneOffset = "+0000"; // Use UTC for simplicity

        // Step 3: Prepare commit content
        StringBuilder commitContent = new StringBuilder();
        commitContent.append("tree ").append(treeSha).append("\n");
        if (parentSha != null && !parentSha.isEmpty()) {
            commitContent.append("parent ").append(parentSha).append("\n");
        }
        commitContent.append("author ").append(authorName).append(" <").append(authorEmail).append("> ")
                .append(timestamp).append(" ").append(timezoneOffset).append("\n");
        commitContent.append("committer ").append(authorName).append(" <").append(authorEmail).append("> ")
                .append(timestamp).append(" ").append(timezoneOffset).append("\n\n");
        commitContent.append(commitMessage).append("\n");

        // Step 4: Write the commit object
        byte[] commitData = commitContent.toString().getBytes(StandardCharsets.UTF_8);
        String commitSha = GitObject.createObject(commitData, "commit", true); // Save commit to .git/objects



        // Step 7: Write the new commit SHA to the branch file
        Files.writeString(branchFile.toPath(), commitSha);


        // Step 8: Return the appropriate commit message

        System.out.println( "[" + branchName + (parentSha == null ? " (root-commit)" : "") + " " + commitSha.substring(0, 7) + "] " + commitMessage);
        System.out.println("Added files: " + changes.get("added"));
        System.out.println("Deleted files: " + changes.get("deleted"));
        System.out.println("Modifies files: " + changes.get("modified"));
        return  commitSha;
    }


    public Commit readCommit(String commitSha) throws IOException {
        // Step 1: Locate the commit object
        String objectPath = ".gitty/objects/" + commitSha.substring(0, 2) + "/" + commitSha.substring(2);
        File commitFile = new File(objectPath);

        if (!commitFile.exists()) {
            throw new FileNotFoundException("Commit object not found: " + commitSha);
        }

        // Step 2: Read and decompress the commit object
        byte[] compressedData = Files.readAllBytes(commitFile.toPath());
        ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
        InflaterInputStream inflater = new InflaterInputStream(bais);
        ByteArrayOutputStream decompressedData = new ByteArrayOutputStream();

        int bytesRead;
        byte[] buffer = new byte[1024];
        while ((bytesRead = inflater.read(buffer)) != -1) {
            decompressedData.write(buffer, 0, bytesRead);
        }
        inflater.close();

        // Step 3: Parse the header and content
        byte[] rawContent = decompressedData.toByteArray();
        String commitContent = new String(rawContent, StandardCharsets.UTF_8);

        // Strip off the header (e.g., "commit <size>\0")
        int headerEnd = commitContent.indexOf('\0');
        if (headerEnd == -1) {
            throw new IOException("Invalid commit object format.");
        }
        String contentWithoutHeader = commitContent.substring(headerEnd + 1);

        // Step 4: Parse the commit content
        String[] lines = contentWithoutHeader.split("\n");
        String treeSha = null;
        List<String> parentShas = new ArrayList<>();
        String author = null;
        String committer = null;
        StringBuilder messageBuilder = new StringBuilder();
        boolean isMessage = false;

        for (String line : lines) {
            if (line.startsWith("tree ")) {
                treeSha = line.substring(5).trim();
            } else if (line.startsWith("parent ")) {
                parentShas.add(line.substring(7).trim());
            } else if (line.startsWith("author ")) {
                author = line.substring(7).trim();
            } else if (line.startsWith("committer ")) {
                committer = line.substring(10).trim();
            } else if (line.isEmpty()) {
                isMessage = true; // Start capturing the commit message
            } else if (isMessage) {
                messageBuilder.append(line).append("\n");
            }
        }

        String message = messageBuilder.toString().trim();

        // Step 5: Return the Commit object
        return new Commit(treeSha, parentShas, author, committer, message);
    }

    public String getLatestCommitSha() throws IOException {
        // Step 1: Read the HEAD file
        File headFile = new File(".gitty/HEAD");
        if (!headFile.exists()) {
            throw new IOException("No .gitty repository found. Are you inside a repository?");
        }

        String headContent = Files.readString(headFile.toPath()).trim();
        if (!headContent.startsWith("ref: ")) {
            throw new IOException("HEAD does not point to a valid branch reference.");
        }

        // Step 2: Determine the branch reference
        String branchPath = headContent.substring(5);
        File branchFile = new File(".gitty/" + branchPath);

        if (!branchFile.exists()) {
            return null;
        }

        String latestCommitSha = Files.readString(branchFile.toPath()).trim();
        return latestCommitSha.isEmpty() ? null : latestCommitSha;
    }



    public void viewCommitHistory() throws IOException {
        // Step 1: Read the current HEAD
        String headRef = Files.readString(new File(".gitty/HEAD").toPath()).trim();
        String currentCommitSha;

        if (headRef.startsWith("ref: ")) {
            // Resolve HEAD to the branch reference
            String branchRef = headRef.substring(5);
            currentCommitSha = Files.readString(new File(".gitty/" + branchRef).toPath()).trim();
        } else {
            // Detached HEAD (points directly to a commit)
            currentCommitSha = headRef;
        }

        // Step 2: Follow the commit chain
        System.out.println("Commit history:");
        while (currentCommitSha != null && !currentCommitSha.isEmpty()) {
            Commit commit = readCommit(currentCommitSha);

            // Print commit details
            System.out.println("Commit: " + currentCommitSha);
            System.out.println("Author: " + commit.getAuthor());
            System.out.println("Date: " + formatUnixTimestamp(commit.getCommitter())); // Optional formatting
            System.out.println("\n    " + commit.getMessage());
            System.out.println();

            // Get the next parent SHA
            if (commit.getParentShas().isEmpty()) {
                break; // No parent commits; end of history
            }
            currentCommitSha = commit.getParentShas().get(0); // Follow the first parent
        }
    }

    private String formatUnixTimestamp(String committerLine) {
        // Extract the timestamp from the committer line (e.g., "committer Name <email> 1699119735 +0000")
        String[] parts = committerLine.split(" ");
        if (parts.length >= 3) {
            long timestamp = Long.parseLong(parts[parts.length - 2]);
            return new java.util.Date(timestamp * 1000L).toString();
        }
        return "Unknown date";
    }


}
