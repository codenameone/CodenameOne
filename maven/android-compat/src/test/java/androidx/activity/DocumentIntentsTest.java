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
package androidx.activity;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.io.filechooser.FileDialogResult;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/// `ACTION_OPEN_DOCUMENT` and `ACTION_CREATE_DOCUMENT` threw
/// ActivityNotFoundException; they now go to the platform's Open and Save
/// dialogs, by `startActivityForResult` and by the AndroidX contracts.
public class DocumentIntentsTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @After
    public void reset() {
        HeadlessImplementation.openRequest = null;
        HeadlessImplementation.openCallback = null;
        HeadlessImplementation.saveRequest = null;
        HeadlessImplementation.saveCallback = null;
    }

    static final class ResultActivity extends ComponentActivity {
        final List<Object[]> results = new ArrayList<Object[]>();

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            results.add(new Object[] {Integer.valueOf(requestCode), Integer.valueOf(resultCode), data});
        }
    }

    @Test
    public void documentIntentsResolve() {
        Context c = AndroidTestSupport.context();
        Intent open = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/gpx+xml");
        assertNotNull(open.resolveActivity(c.getPackageManager()));
        Intent create = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain");
        assertNotNull(create.resolveActivity(c.getPackageManager()));
    }

    @Test
    public void openDocumentAnswersThePickedFile() {
        AndroidTestSupport.context();
        ResultActivity a = new ResultActivity();
        Intent open = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/gpx+xml");
        a.startActivityForResult(open, 7);
        assertNotNull("no Open dialog was shown", HeadlessImplementation.openCallback);
        assertArrayEquals(new String[] {"application/gpx+xml"},
                HeadlessImplementation.openRequest.getFilters()[0].getMimeTypes());
        HeadlessImplementation.openCallback.onSucess(new FileDialogResult(HeadlessImplementation.openRequest,
                new String[] {"file:///docs/track.gpx"}, -1, null));
        assertEquals(1, a.results.size());
        assertEquals(Integer.valueOf(7), a.results.get(0)[0]);
        assertEquals(Integer.valueOf(Activity.RESULT_OK), a.results.get(0)[1]);
        assertEquals("file:///docs/track.gpx", ((Intent) a.results.get(0)[2]).getData().toString());
    }

    @Test
    public void cancellingAnswersCanceled() {
        AndroidTestSupport.context();
        ResultActivity a = new ResultActivity();
        a.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*"), 3);
        assertEquals("*/* must not filter", 0, HeadlessImplementation.openRequest.getFilters().length);
        HeadlessImplementation.openCallback.onSucess(FileDialogResult.cancelled(HeadlessImplementation.openRequest));
        assertEquals(Integer.valueOf(Activity.RESULT_CANCELED), a.results.get(0)[1]);
        assertNull(a.results.get(0)[2]);
    }

    @Test
    public void openMultipleDocumentsAnswersEveryUri() {
        AndroidTestSupport.context();
        final List<List<Uri>> got = new ArrayList<List<Uri>>();
        ComponentActivity a = new ComponentActivity();
        ActivityResultLauncher<String[]> l = a.registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(), new ActivityResultCallback<List<Uri>>() {
                    @Override
                    public void onActivityResult(List<Uri> result) {
                        got.add(result);
                    }
                });
        l.launch(new String[] {"*/*"});
        assertTrue(HeadlessImplementation.openRequest.isMultiSelect());
        HeadlessImplementation.openCallback.onSucess(new FileDialogResult(HeadlessImplementation.openRequest,
                new String[] {"file:///a.gpx", "file:///b.gpx"}, -1, null));
        assertEquals(1, got.size());
        assertEquals(2, got.get(0).size());
        assertEquals("file:///b.gpx", got.get(0).get(1).toString());
    }

    /// A non-media GetContent is a document, not a gallery pick.
    @Test
    public void getContentForADocumentTypeOpensTheOpenDialog() {
        AndroidTestSupport.context();
        HeadlessImplementation.gallery = null;
        ComponentActivity a = new ComponentActivity();
        final List<Uri> got = new ArrayList<Uri>();
        a.registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri result) {
                got.add(result);
            }
        }).launch("application/pdf");
        assertNull("a PDF went to the gallery", HeadlessImplementation.gallery);
        HeadlessImplementation.openCallback.onSucess(new FileDialogResult(HeadlessImplementation.openRequest,
                new String[] {"file:///r.pdf"}, -1, null));
        assertEquals("file:///r.pdf", got.get(0).toString());
    }

    /// Creating answers a staging file at once; closing the stream written
    /// to it shows the Save dialog that exports it, under the requested name.
    @Test
    public void createDocumentExportsTheWrittenFileOnClose() throws Exception {
        AndroidTestSupport.context();
        ComponentActivity a = new ComponentActivity();
        final List<Uri> got = new ArrayList<Uri>();
        a.registerForActivityResult(new ActivityResultContracts.CreateDocument("application/gpx+xml"),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri result) {
                        got.add(result);
                    }
                }).launch("track.gpx");
        assertEquals("the result was delivered inside launch()", 0, got.size());
        MainThreadRule.drain();
        assertEquals(1, got.size());
        Uri uri = got.get(0);
        assertTrue(uri.toString(), uri.toString().endsWith("/track.gpx"));
        assertNull("the dialog was shown before anything was written", HeadlessImplementation.saveRequest);

        OutputStream out = a.getContentResolver().openOutputStream(uri);
        out.write("<gpx/>".getBytes("UTF-8"));
        out.close();
        MainThreadRule.drain();
        assertNotNull("closing did not show the Save dialog", HeadlessImplementation.saveRequest);
        assertEquals("track.gpx", HeadlessImplementation.saveRequest.getSuggestedName());
        assertEquals("<gpx/>", new String(HeadlessImplementation.FILES.get(
                HeadlessImplementation.saveRequest.getSourceFile()), "UTF-8"));

        HeadlessImplementation.saveRequest = null;
        out.close();
        MainThreadRule.drain();
        assertNull("a second close exported again", HeadlessImplementation.saveRequest);
    }

    /// A title with characters a URI treats specially still names the file
    /// the stream goes to, so the export is not lost.
    @Test
    public void createDocumentWithSpecialCharactersStillExports() throws Exception {
        AndroidTestSupport.context();
        ResultActivity a = new ResultActivity();
        a.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain")
                .putExtra(Intent.EXTRA_TITLE, "Report #3 50%?.txt"), 4);
        MainThreadRule.drain();
        Uri uri = ((Intent) a.results.get(0)[2]).getData();
        OutputStream out = a.getContentResolver().openOutputStream(uri);
        out.write(1);
        out.close();
        MainThreadRule.drain();
        assertNotNull("the export was lost", HeadlessImplementation.saveRequest);
        assertEquals("Report #3 50%?.txt", HeadlessImplementation.saveRequest.getSuggestedName());
    }

    /// Append mode on a file that does not exist yet is a plain write.
    @Test
    public void appendModeOnANewFileWrites() throws Exception {
        Context c = AndroidTestSupport.context();
        Uri uri = Uri.parse("file:///home/append-new.txt");
        OutputStream out = c.getContentResolver().openOutputStream(uri, "wa");
        out.write("x".getBytes("UTF-8"));
        out.close();
        assertEquals("x", new String(HeadlessImplementation.FILES.get("file:///home/append-new.txt"), "UTF-8"));
    }
}
