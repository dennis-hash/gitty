package org.scm.core;

import org.scm.models.IndexEntry;
import org.scm.utils.FileUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.InflaterInputStream;

import static org.scm.core.GitObject.createObject;
import static org.scm.utils.FileUtils.*;

public class IndexManager {
    public void addFilesToIndex(List<File> files) throws IOException, NoSuchAlgorithmException {
        List<IndexEntry> entries = readIndex();

        // Map existing entries for quick lookup by path
        Map<String, IndexEntry> entriesByPath = new HashMap<>();
        for (IndexEntry entry : entries) {
            entriesByPath.put(entry.getPath(), entry);
        }

        for (File file : files) {
            byte[] data = FileUtils.readFile(file);
            long currentTime = System.currentTimeMillis();

            String sha1 = createObject(data, "blob", true); // Create new blob object

            // Check if the file is already in the index
            IndexEntry existingEntry = entriesByPath.get(file.getPath());
            if (existingEntry != null) {
                existingEntry.setSha1(sha1);
                existingEntry.setModifiedTime(currentTime);
                existingEntry.setSize(file.length());
            } else {
                IndexEntry newEntry = new IndexEntry(
                        file.getPath(),
                        sha1,
                        currentTime,
                        file.length()
                );
                entries.add(newEntry);
                entriesByPath.put(file.getPath(), newEntry); // Update the map
            }
        }

        writeIndex(entries);
    }


    public List<IndexEntry> readIndex() throws IOException {
        File indexFile = new File(".gitty/index");
        if (!indexFile.exists()) {
            return new ArrayList<>(); // Return an empty list if the index file doesn't exist
        }

        try (FileInputStream fis = new FileInputStream(indexFile)) {
            byte[] data = fis.readAllBytes();

            // Validate checksum (last 20 bytes are SHA-1 hash of the rest)
            byte[] content = Arrays.copyOf(data, data.length - 20);
            byte[] expectedChecksum = Arrays.copyOfRange(data, data.length - 20, data.length);
            byte[] actualChecksum = HashUtils.computeSHA1Bytes(content);

            if (!Arrays.equals(expectedChecksum, actualChecksum)) {
                throw new IOException("Invalid index checksum");
            }

            // Parse header
            ByteBuffer buffer = ByteBuffer.wrap(content);
            buffer.order(ByteOrder.BIG_ENDIAN);

            byte[] signature = new byte[4];
            buffer.get(signature);
            if (!Arrays.equals(signature, "DIRC".getBytes(StandardCharsets.UTF_8))) {
                throw new IOException("Invalid index signature");
            }

            int version = buffer.getInt();
            if (version != 2) {
                throw new IOException("Unsupported index version: " + version);
            }

            int numEntries = buffer.getInt();
            List<IndexEntry> entries = new ArrayList<>();

            // Parse entries
            for (int i = 0; i < numEntries; i++) {
                long mtimeSec = buffer.getInt() & 0xFFFFFFFFL;
                long mtimeNano = buffer.getInt() & 0xFFFFFFFFL; // Unused, but aligned with written structure
                buffer.getInt(); // Skip dev (placeholder)
                buffer.getInt(); // Skip ino (placeholder)
                buffer.getInt(); // Skip mode (placeholder)
                buffer.getInt(); // Skip uid (placeholder)
                buffer.getInt(); // Skip gid (placeholder)
                long size = buffer.getInt() & 0xFFFFFFFFL;
                byte[] sha1 = new byte[20];
                buffer.get(sha1);
                buffer.getShort(); // Skip flags (placeholder)

                // Read variable-length path
                StringBuilder pathBuilder = new StringBuilder();
                while (true) {
                    byte b = buffer.get();
                    if (b == 0) break; // Null-terminator
                    pathBuilder.append((char) b);
                }
                String path = pathBuilder.toString();

                // Skip padding
                int entryLength = ((62 + path.length() + 8) / 8) * 8;
                int paddingLength = entryLength - (62 + path.length());
                buffer.position(buffer.position() + paddingLength);

                entries.add(new IndexEntry(path, bytesToHex(sha1), mtimeSec, size));
            }

            return entries;
        }
    }



    private void writeIndex(List<IndexEntry> entries) throws IOException {
        File indexFile = new File(".gitty/index");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             FileOutputStream fos = new FileOutputStream(indexFile)) {

            // Write header
            baos.write("DIRC".getBytes(StandardCharsets.UTF_8)); // Signature
            baos.write(intToBytes(2)); // Version
            baos.write(intToBytes(entries.size())); // Number of entries

            // Write entries
            for (IndexEntry entry : entries) {
                baos.write(intToBytes((int) (entry.getModifiedTime() / 1000))); // mtime_s
                baos.write(intToBytes((int) (entry.getModifiedTime() % 1000))); // mtime_n
                baos.write(intToBytes(0)); // dev (placeholder)
                baos.write(intToBytes(0)); // ino (placeholder)
                baos.write(intToBytes(0)); // mode (placeholder)
                baos.write(intToBytes(0)); // uid (placeholder)
                baos.write(intToBytes(0)); // gid (placeholder)
                baos.write(intToBytes((int) entry.getSize())); // size
                baos.write(hexToBytes(entry.getSha1())); // sha1
                baos.write(shortToBytes(0)); // flags (placeholder)

                // Write path
                baos.write(entry.getPath().getBytes(StandardCharsets.UTF_8));
                baos.write(0); // Null-terminator for path

                // Add padding
                int entryLength = ((62 + entry.getPath().length() + 8) / 8) * 8;
                int paddingLength = entryLength - (62 + entry.getPath().length());
                baos.write(new byte[paddingLength]); // Write padding as zeros
            }

            // Compute checksum
            byte[] content = baos.toByteArray();
            byte[] checksum = HashUtils.computeSHA1Bytes(content);
            baos.write(checksum);

            // Write all data to the file
            fos.write(baos.toByteArray());
        }
    }




}
