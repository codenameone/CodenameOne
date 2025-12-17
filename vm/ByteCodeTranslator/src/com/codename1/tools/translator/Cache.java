/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.tools.translator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

/**
 * Manages checksums for generated files to avoid unnecessary writes.
 */
public class Cache {
    private final Properties checksums = new Properties();
    private final File cacheFile;
    private final MessageDigest digest;

    public Cache(File outputDir) {
        this.cacheFile = new File(outputDir, "cn1_checksums.props");
        try {
            this.digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
        load();
    }

    private void load() {
        if (cacheFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(cacheFile);
                BufferedInputStream bis = new BufferedInputStream(fis);
                checksums.load(bis);
                bis.close();
            } catch (IOException err) {
                // ignore corrupted cache
                err.printStackTrace();
            }
        }
    }

    public void save() {
        try {
            FileOutputStream fos = new FileOutputStream(cacheFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            checksums.store(bos, "Codename One Build Checksums");
            bos.close();
        } catch (IOException err) {
            err.printStackTrace();
        }
    }

    /**
     * Checks if the content matches the cache. Updates the cache if it changed.
     * @param filename The relative filename
     * @param content The content to write
     * @return true if the file should be written (changed or new), false otherwise.
     */
    public boolean shouldWrite(File destFile, byte[] content) {
        if (!destFile.exists()) {
            updateCache(destFile.getName(), content);
            return true;
        }

        String hex = calculateHash(content);
        String existing = checksums.getProperty(destFile.getName());

        if (existing == null || !existing.equals(hex)) {
            checksums.setProperty(destFile.getName(), hex);
            return true;
        }
        return false;
    }

    private void updateCache(String key, byte[] content) {
        checksums.setProperty(key, calculateHash(content));
    }

    private String calculateHash(byte[] content) {
        digest.reset();
        byte[] d = digest.digest(content);
        StringBuilder sb = new StringBuilder();
        for (byte b : d) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
