/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.javase;

import com.codename1.documents.spi.DocumentProviderBridge;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

/// The simulator's document provider. There is no desktop file browser to publish into, so this
/// reports the feature unsupported -- and still writes everything the device ports would write.
///
/// That combination is deliberate. `DocumentProvider.publish()` does not consult
/// `isSupported()`, so a developer running in the simulator exercises the real serializer and
/// the real layout, and can open `cn1documents/index.json` under the app's home directory to see
/// exactly what a device would have been handed. Reporting supported instead would be a lie that
/// only shows up on a device.
public class JavaSEDocumentProviderBridge implements DocumentProviderBridge {
    private static final Logger LOG =
            Logger.getLogger(JavaSEDocumentProviderBridge.class.getName());

    private final File root;

    public JavaSEDocumentProviderBridge(File root) {
        this.root = root;
    }

    @Override
    public boolean isDocumentProviderSupported() {
        return false;
    }

    @Override
    public String getSharedDirectory() {
        File files = new File(root, "files");
        if (!files.exists() && !files.mkdirs()) {
            LOG.log(Level.WARNING, "Could not create {0}", files);
            return null;
        }
        return files.getAbsolutePath();
    }

    @Override
    public void publishIndex(String indexJson) {
        write("index.json", indexJson);
    }

    @Override
    public void setRemoteEndpoint(String endpoint, String authToken) {
        StringBuilder sb = new StringBuilder("{");
        if (endpoint != null && endpoint.length() > 0) {
            sb.append("\"endpoint\":\"").append(endpoint.replace("\\", "\\\\")
                    .replace("\"", "\\\"")).append("\"");
        }
        if (authToken != null && authToken.length() > 0) {
            if (sb.length() > 1) {
                sb.append(',');
            }
            sb.append("\"authToken\":\"").append(authToken.replace("\\", "\\\\")
                    .replace("\"", "\\\"")).append("\"");
        }
        write("endpoint.json", sb.append('}').toString());
    }

    @Override
    public void signalChange() {
    }

    @Override
    public void clear() {
        deleteTree(root);
    }

    private void write(String name, String text) {
        if (text == null) {
            return;
        }
        if (!root.exists() && !root.mkdirs()) {
            LOG.log(Level.WARNING, "Could not create {0}", root);
            return;
        }
        File target = new File(root, name);
        try (FileOutputStream out = new FileOutputStream(target)) {
            out.write(text.getBytes("UTF-8"));
        } catch (IOException err) {
            LOG.log(Level.WARNING, "Could not write " + target, err);
        }
    }

    private void deleteTree(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        // Descends into a real directory only. isDirectory and listFiles both follow symbolic
        // links, so a link inside the published tree -- easy to create by hand on a developer
        // machine, where this tree is an ordinary folder -- would have clear() delete the
        // contents of whatever it names. Deleting the link removes the link, never its target.
        // Same rule as the device ports; a simulator that eats unrelated files is worse, not
        // better, than a device that does.
        if (file.isDirectory() && !Files.isSymbolicLink(file.toPath())) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteTree(child);
                }
            }
        }
        if (!file.delete()) {
            LOG.log(Level.WARNING, "Could not delete {0}", file);
        }
    }
}
