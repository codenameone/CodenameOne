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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.documents.DocumentIndexSerializer;
import com.codename1.documents.DocumentNode;
import com.codename1.documents.DocumentProvider;
import com.codename1.io.FileSystemStorage;
import com.codename1.ui.Display;

import java.io.OutputStream;

/// Publishes a document tree on the device VM, so CI compiles what the build generates for it.
///
/// Declaring this is the coverage. Without a reference to `com.codename1.documents` anywhere in
/// the project the iOS builder writes no extension at all, so `CN1FileProviderExtension.swift`,
/// the shared index/item/enumerator sources and the generated `CN1DocumentConfig.swift` are never
/// compiled by Xcode in CI, the `CN1Documents` target never enters the Xcode project, and the
/// Android builder writes no `<provider>` for AAPT to reject. Every generated-code mistake in
/// that half of the feature -- a Swift file that does not typecheck against the deployment
/// target, a plist key Xcode will not take, a manifest attribute AAPT rejects -- is a build error
/// that only appears once something publishes documents.
///
/// The tree is chosen to cover the shapes that serialize differently rather than to be a
/// realistic library: a nested folder, a file backed by bytes in the shared directory, a file
/// backed by a remote id, a read-only file, and one that declares neither size nor date.
/// Assertion-only test, no screenshot.
public class DocumentProviderPublishTest extends BaseTest {

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        try {
            // Support probes must never throw, whatever they answer.
            boolean supported = DocumentProvider.isSupported();
            System.out.println("CN1SS:INFO:test=DocumentProviderPublishTest supported=" + supported
                    + " platform=" + Display.getInstance().getPlatformName());

            // The shared directory is either a usable path or null; it is never a path that
            // cannot be written to, because every port creates it before answering.
            String shared = DocumentProvider.getSharedDirectory();
            if (shared != null) {
                assertBool(shared.length() > 0, "shared directory path is not empty");
                writeProbe(shared);
            }

            DocumentNode root = buildTree(shared);

            // The serializer is the on-disk contract with the native readers, so the shape is
            // asserted here too: on device this is the only place it runs at all.
            String json = DocumentIndexSerializer.serialize(root);
            assertBool(json.indexOf("\"v\"") >= 0, "index carries a schema version");
            assertBool(json.indexOf("cn1ss_invoice") >= 0, "index carries the published ids");
            DocumentNode back = DocumentIndexSerializer.deserialize(json);
            assertEqual("cn1ss_root", back.getId(), "round-tripped root id");
            assertEqual(2, back.getChildren().size(), "round-tripped child count");

            // Publishing must not throw anywhere: it is a no-op without a bridge, and persists
            // plus signals where one exists.
            DocumentProvider.setRemoteEndpoint("https://example.com/cn1ss", "cn1ss-token");
            DocumentProvider.publish(root);
            DocumentProvider.signalChange();

            // Publishing twice with the same tree is explicitly harmless, which is what lets an
            // app publish on every data change without tracking whether anything moved.
            DocumentProvider.publish(root);

            // A null tree is a programming error rather than something to swallow.
            boolean threw = false;
            try {
                DocumentProvider.publish(null);
            } catch (IllegalArgumentException expected) {
                threw = true;
            }
            assertBool(threw, "publishing a null tree is refused");

            // Clearing must be safe everywhere, including when nothing was ever published.
            DocumentProvider.clear();
            DocumentProvider.clear();
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            fail("DocumentProviderPublishTest threw " + t);
            return false;
        }
    }

    /// Writes a real file into the shared directory, so the local-content path is exercised with
    /// bytes that exist rather than with a path that only looks plausible.
    private void writeProbe(String shared) {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String path = shared + "/cn1ss-invoice.txt";
        try {
            OutputStream out = fs.openOutputStream(path);
            try {
                out.write("cn1ss".getBytes("UTF-8"));
            } finally {
                out.close();
            }
            assertBool(fs.exists(path), "probe file exists in the shared directory");
        } catch (Exception err) {
            fail("could not write into the shared directory: " + err);
        }
    }

    private DocumentNode buildTree(String shared) {
        DocumentNode root = DocumentNode.folder("cn1ss_root", "CN1SS Documents");
        DocumentNode folder = DocumentNode.folder("cn1ss_2031", "2031");
        folder.add(DocumentNode.file("cn1ss_invoice", "invoice.txt")
                .setContentType("text/plain")
                .setPath(shared == null ? null : "cn1ss-invoice.txt")
                .setSize(5L)
                .setLastModified(1735689600000L));
        folder.add(DocumentNode.file("cn1ss_remote", "remote.pdf")
                .setContentType("application/pdf")
                .setRemoteId("cn1ss/remote-1")
                .setReadOnly(true));
        root.add(folder);
        // Neither size nor date: the readers have to treat both as unknown rather than as zero.
        root.add(DocumentNode.file("cn1ss_bare", "bare.bin"));
        return root;
    }
}
