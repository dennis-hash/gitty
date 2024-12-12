package org.scm.utils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class FileUtils {
    /**
     * Reads the contents of a file as a byte array.
     *
     * @param file the file to read
     * @return the file's content as a byte array
     * @throws IOException if an I/O error occurs
     */
    public static byte[] readFile(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    /**
     * Writes a byte array to a file.
     *
     * @param path the path of the file
     * @param data the data to write
     * @throws IOException if an I/O error occurs
     */
    public static void writeFile(String path, byte[] data) throws IOException {
        File file = new File(path);
        file.getParentFile().mkdirs(); // Ensure parent directories exist
        Files.write(file.toPath(), data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Writes a byte array to a file after compressing it.
     *
     * @param path the path of the file
     * @param data the data to compress and write
     * @throws IOException if an I/O error occurs
     */
    public static void writeCompressedFile(String path, byte[] data) throws IOException {
        File file = new File(path);
        file.getParentFile().mkdirs(); // Ensure parent directories exist
        try (FileOutputStream fos = new FileOutputStream(file);
             DeflaterOutputStream dos = new DeflaterOutputStream(fos)) {
            dos.write(data);
        }
    }

    /**
     * Reads and decompresses a file's contents as a byte array.
     *
     * @param path the path of the compressed file
     * @return the decompressed file's content as a byte array
     * @throws IOException if an I/O error occurs
     */
    public static byte[] readCompressedFile(String path) throws IOException {
        try (FileInputStream fis = new FileInputStream(path);
             InflaterInputStream iis = new InflaterInputStream(fis);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = iis.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }
            return baos.toByteArray();
        }
    }

    /**
     * Deletes a file or directory recursively.
     *
     * @param file the file or directory to delete
     * @throws IOException if an I/O error occurs
     */
    public static void deleteRecursively(File file) throws IOException {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteRecursively(child);
            }
        }
        if (!file.delete()) {
            throw new IOException("Failed to delete file: " + file.getAbsolutePath());
        }
    }

    /**
     * Checks if a file exists.
     *
     * @param path the path of the file
     * @return true if the file exists, false otherwise
     */
    public static boolean fileExists(String path) {
        return Files.exists(Path.of(path));
    }

    public static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    public static byte[] shortToBytes(int value) {
        return ByteBuffer.allocate(2).putShort((short) value).array();
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static byte[] hexToBytes(String hex) {
        int length = hex.length();
        byte[] bytes = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return bytes;
    }

}
