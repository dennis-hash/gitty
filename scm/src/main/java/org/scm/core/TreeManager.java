package org.scm.core;

import org.scm.models.IndexEntry;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.InflaterInputStream;

import static org.scm.core.GitObject.createObject;

public class TreeManager {
    public String createTreeObject(List<IndexEntry> entries) throws IOException, NoSuchAlgorithmException {
        // Step 1: Build directory map
        Map<String, List<IndexEntry>> directoryMap = new HashMap<>();
        for (IndexEntry entry : entries) {
            String path = entry.getPath();
            int lastSlash = path.lastIndexOf('/');
            String directory = lastSlash == -1 ? "" : path.substring(0, lastSlash);
            directoryMap.computeIfAbsent(directory, k -> new ArrayList<>()).add(entry);
        }

        // Step 2: Recursively create tree objects starting from the root
        return writeTree(directoryMap, "");
    }


    private String writeTree(Map<String, List<IndexEntry>> directoryMap, String currentDir) throws IOException, NoSuchAlgorithmException {
        ByteArrayOutputStream treeContent = new ByteArrayOutputStream();
        List<IndexEntry> entries = directoryMap.getOrDefault(currentDir, new ArrayList<>());

        // Add blobs (files) to the tree
        for (IndexEntry entry : entries) {
            String fileName = entry.getPath().substring(currentDir.length() + (currentDir.isEmpty() ? 0 : 1));
            treeContent.write(String.format("100644 %s\0", fileName).getBytes(StandardCharsets.UTF_8));
            treeContent.write(hexToRawBytes(entry.getSha1())); // Convert hex SHA1 to raw bytes
        }

        // Add subdirectories to the tree
        for (String dir : directoryMap.keySet()) {
            if (dir.startsWith(currentDir) && !dir.equals(currentDir)) {
                // Process subdirectories
                String subDir = dir.substring(currentDir.length() + (currentDir.isEmpty() ? 0 : 1));
                if (subDir.contains("/")) {
                    String directSubDir = subDir.split("/")[0]; // Get the first level of the subdirectory
                    String subTreeHash = writeTree(directoryMap, dir); // Recursively process the subdirectory
                    treeContent.write(String.format("040000 %s\0", dir).getBytes(StandardCharsets.UTF_8));
                    treeContent.write(hexToRawBytes(subTreeHash)); // Convert hex SHA1 to raw bytes
                }
            }
        }

        // Create the tree object
        byte[] treeData = treeContent.toByteArray();
        return createObject(treeData, "tree", true);
    }

    public Map<String, String> readTree(String treeSha) throws IOException {
        Map<String, String> treeEntries = new HashMap<>();

        // Locate and decompress the tree object
        File objectFile = getObjectFile(treeSha);
        byte[] objectData = decompressObject(objectFile);

        // Skip the header (e.g., "tree <size>\0")
        int headerEnd = 0;
        while (objectData[headerEnd] != 0) {
            headerEnd++;
        }
        headerEnd++; // Move past the null terminator

        ByteArrayInputStream inputStream = new ByteArrayInputStream(objectData, headerEnd, objectData.length - headerEnd);

        // Parse tree entries
        while (inputStream.available() > 0) {
            // Read the mode (e.g., 100644 or 040000)
            StringBuilder mode = new StringBuilder();
            while (true) {
                int b = inputStream.read();
                if (b == ' ') break;
                mode.append((char) b);
            }

            // Read the file or directory name
            StringBuilder name = new StringBuilder();
            while (true) {
                int b = inputStream.read();
                if (b == 0) break; // Null terminator signals end of name
                name.append((char) b);
            }

            // Read the raw SHA1 (20 bytes)
            byte[] sha1Bytes = new byte[20];
            inputStream.read(sha1Bytes);
            String sha1 = rawBytesToHex(sha1Bytes);

            // Skip the entry if it already exists in the map
            if (treeEntries.containsKey(name.toString())) {
                continue; // Skip duplicate entries
            }

            if (mode.toString().equals("040000")) {
                // Directory entry (tree)
                treeEntries.put(name.toString() + "/", sha1);

                // Recursive call to process nested trees
                Map<String, String> subTreeEntries = readTree(sha1);
                for (Map.Entry<String, String> entry : subTreeEntries.entrySet()) {
                    // Add entries with unique names
                    String fullPath = entry.getKey();
                    if (!treeEntries.containsKey(fullPath)) {
                        treeEntries.put(fullPath, entry.getValue());
                    }
                }
            } else if (mode.toString().equals("100644")) {
                // File entry (blob)
                treeEntries.put(name.toString(), sha1);
            }
        }
        return treeEntries;
    }



    public Map<String, List<String>> compareTrees(String currentTreeSha, String parentTreeSha) throws IOException {
        Map<String, String> currentTree = readTree(currentTreeSha);
        Map<String, String> parentTree = parentTreeSha != null ? readTree(parentTreeSha) : new HashMap<>();


        List<String> addedFiles = new ArrayList<>();
        List<String> deletedFiles = new ArrayList<>();
        List<String> modifiedFiles = new ArrayList<>();

        // Detect added files
        for (String filePath : currentTree.keySet()) {
            if (!parentTree.containsKey(filePath)) {
                addedFiles.add(filePath);
            } else {
                // Check if the file has been modified (compare SHA1)
                String currentSha1 = currentTree.get(filePath);
                String parentSha1 = parentTree.get(filePath);
                if (!currentSha1.equals(parentSha1)) {
                    modifiedFiles.add(filePath);
                }
            }
        }

        // Detect deleted files
        for (String filePath : parentTree.keySet()) {
            if (!currentTree.containsKey(filePath)) {
                deletedFiles.add(filePath);
            }
        }

        // Prepare the changes map
        Map<String, List<String>> changes = new HashMap<>();
        changes.put("added", addedFiles);
        changes.put("deleted", deletedFiles);
        changes.put("modified", modifiedFiles);

        return changes;
    }



    // Utility to locate a Git object file by SHA
    File getObjectFile(String sha) {
        String dir = sha.substring(0, 2);
        String file = sha.substring(2);
        return new File(".gitty/objects/" + dir + "/" + file);
    }
    private String rawBytesToHex(byte[] sha1Bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : sha1Bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    // Utility to decompress Git objects (zlib)
    private byte[] decompressObject(File objectFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(objectFile);
             InflaterInputStream inflater = new InflaterInputStream(fis);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] temp = new byte[1024];
            int len;
            while ((len = inflater.read(temp)) != -1) {
                buffer.write(temp, 0, len);
            }
            return buffer.toByteArray();
        }
    }

// Utility methods remain the same as previously defined



    private byte[] hexToRawBytes(String hex) {
        int length = hex.length();
        byte[] rawBytes = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            rawBytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return rawBytes;
    }



}
