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
package com.codenameone.developerguide.documents;

import com.codename1.documents.DocumentNode;
import com.codename1.documents.DocumentProvider;
import com.codename1.io.FileSystemStorage;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

/**
 * Snippets that accompany the Document Provider guide chapter. Each block
 * between the tag markers is included verbatim into the AsciiDoc.
 */
public class DocumentProviderSnippets {

    /** Stands in for whatever the application's own records look like. */
    public static class Invoice {
        public String id;
        public String title;
        public byte[] pdf;
        public long modified;
        public String serverKey;
    }

    private List<Invoice> invoices;

    public void publishLocalTree() {
        // tag::publishLocal[]
        String shared = DocumentProvider.getSharedDirectory();
        DocumentNode root = DocumentNode.folder("root", "My Invoices");
        for (Invoice invoice : invoices) {
            String name = invoice.title + ".pdf";
            writeInto(shared + "/" + name, invoice.pdf);
            root.add(DocumentNode.file(invoice.id, name)
                    .setContentType("application/pdf")
                    .setPath(name)
                    .setSize(invoice.pdf.length)
                    .setLastModified(invoice.modified));
        }
        DocumentProvider.publish(root);
        // end::publishLocal[]
    }

    public void publishRemoteTree() {
        // tag::publishRemote[]
        DocumentProvider.setRemoteEndpoint("https://api.example.com/drive", sessionToken());
        DocumentNode root = DocumentNode.folder("root", "Example Drive");
        for (Invoice invoice : invoices) {
            root.add(DocumentNode.file(invoice.id, invoice.title + ".pdf")
                    .setContentType("application/pdf")
                    .setRemoteId(invoice.serverKey)
                    .setSize(invoice.pdf.length)
                    // The date, not the size, is what tells the browser the content changed: a
                    // correction that keeps the length would move nothing otherwise.
                    .setLastModified(invoice.modified));
        }
        DocumentProvider.publish(root);
        // end::publishRemote[]
    }

    public void nestFolders() {
        // tag::nestFolders[]
        DocumentNode root = DocumentNode.folder("root", "My Invoices");
        DocumentNode year = DocumentNode.folder("y2031", "2031");
        year.add(DocumentNode.file("inv-1", "January.pdf")
                .setContentType("application/pdf")
                .setPath("2031/january.pdf"));
        root.add(year);
        DocumentProvider.publish(root);
        // end::nestFolders[]
    }

    public void withdrawOnLogout() {
        // tag::clear[]
        DocumentProvider.clear();
        // end::clear[]
    }

    public void guardWithSupport() {
        // tag::isSupported[]
        if (DocumentProvider.isSupported()) {
            showSetting("Show my documents in Files");
        }
        // end::isSupported[]
    }

    private void writeInto(String path, byte[] data) {
        try {
            OutputStream out = FileSystemStorage.getInstance().openOutputStream(path);
            try {
                out.write(data);
            } finally {
                out.close();
            }
        } catch (IOException err) {
            // The application decides; the guide's point is only where the bytes go.
            throw new RuntimeException(err);
        }
    }

    private String sessionToken() {
        return "the token your backend issued at login";
    }

    private void showSetting(String label) {
    }

    private Date unused() {
        return new Date();
    }
}
