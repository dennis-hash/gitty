package org.scm.core;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {
    public static String computeSHA1(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return bytesToHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not found", e);
        }
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


    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Computes the SHA-1 hash of the given byte array.
     *
     * @param data the byte array to hash
     * @return the SHA-1 hash as a byte array
     */
    public static byte[] computeSHA1Bytes(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }
    }
}
