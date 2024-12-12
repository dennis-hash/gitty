package org.scm.core;

import org.scm.models.Commit;
import org.scm.models.IndexEntry;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class GitObject {
    public String createTreeObject(List<IndexEntry> entries) throws IOException, NoSuchAlgorithmException {
        // Step 1: Build directory map
        Map<String, List<IndexEntry>> directoryMap = new HashMap<>();
        for (IndexEntry entry : entries) {
            String path = entry.getPath();
            int lastSlash = path.lastIndexOf('/');
            String directory = lastSlash == -1 ? "" : path.substring(0, lastSlash);
            directoryMap.computeIfAbsent(directory, k -> new ArrayList<>()).add(entry);
        }

        System.out.println("--------------------------");
        System.out.println("--------------------------");
        System.out.println(directoryMap.keySet());
        System.out.println("--------------------------");
        System.out.println("--------------------------");

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
                    treeContent.write(String.format("040000 %s\0", directSubDir).getBytes(StandardCharsets.UTF_8));
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
            String sha1 =rawBytesToHex(sha1Bytes);

            // Add entry to the map
            if (mode.toString().equals("040000")) {
                // Directory entry (tree)
                treeEntries.put(name.toString() + "/", sha1);
            } else if (mode.toString().equals("100644")) {
                // File entry (blob)
                treeEntries.put(name.toString(), sha1);
            }
        }

        return treeEntries;
    }

    // Utility to locate a Git object file by SHA
    private File getObjectFile(String sha) {
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



    public static String createObject(byte[] data, String objType, boolean write) throws IOException, NoSuchAlgorithmException {
        String header = objType + " " + data.length;
        byte[] fullData = (header + "\0").getBytes(StandardCharsets.UTF_8);
        fullData = concatenate(fullData, data);

        String sha1 = HashUtils.computeSHA1(fullData);

        if (write) {
            String path = ".gitty/objects/" + sha1.substring(0, 2) + "/" + sha1.substring(2);
            File file = new File(path);
            file.getParentFile().mkdirs();

            try (FileOutputStream fos = new FileOutputStream(file)) {
                try (DeflaterOutputStream dos = new DeflaterOutputStream(fos, new Deflater(Deflater.DEFAULT_COMPRESSION))) {
                    dos.write(fullData);
                }
            }
        }

        return sha1;
    }


    private static byte[] concatenate(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }



}
