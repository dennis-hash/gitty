package org.scm.core;

import org.scm.models.Commit;
import org.scm.models.IndexEntry;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;


public class GitObject {
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
