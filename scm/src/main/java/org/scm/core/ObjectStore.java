package org.scm.core;

import org.scm.utils.FileUtils;

import java.io.IOException;

public class ObjectStore {
    public String storeBlob(byte[] data) throws IOException {
        String sha1 = HashUtils.computeSHA1(data);
        String path = ".git/objects/" + sha1.substring(0, 2) + "/" + sha1.substring(2);
        FileUtils.writeCompressedFile(path, data);
        return sha1;
    }
}
