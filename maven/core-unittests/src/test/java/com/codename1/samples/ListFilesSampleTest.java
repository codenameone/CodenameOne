package com.codename1.samples;

import com.codename1.io.File;
import com.codename1.io.FileSystemStorage;
import com.codename1.ui.*;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import static org.junit.jupiter.api.Assertions.*;

public class ListFilesSampleTest extends UITestBase {

    @FormTest
    public void testListFilesSample() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.clearFileSystem();
        String appHome = impl.getAppHomePath();
        impl.mkdir(appHome);

        Form hi = new Form("Hi World", new BorderLayout());

        Button add = new Button("Add Directory");
        add.addActionListener(e -> {
            String inputName = appHome + "NewDir";
            File f = new File(inputName);
            f.mkdir();
        });

        Button rename = new Button("Rename File");
        rename.addActionListener(e -> {
             String oldNamePath = appHome + "NewDir";
             String newNameText = "RenamedDir";
             File f = new File(oldNamePath);
             FileSystemStorage fs = FileSystemStorage.getInstance();
             fs.rename(f.getPath(), newNameText);
        });

        TextArea listing = new TextArea();
        Button refresh = new Button("Refresh");
        refresh.addActionListener(e -> {
            File f = new File(FileSystemStorage.getInstance().getAppHomePath());
            StringBuilder sb = new StringBuilder();
            appendChildren(sb, f, 0);
            listing.setText(sb.toString());

        });

        hi.add(BorderLayout.NORTH, FlowLayout.encloseCenter(add, rename)).add(BorderLayout.CENTER, listing).add(BorderLayout.SOUTH, refresh);
        hi.show();
        waitForFormTitle("Hi World");

        impl.tapComponent(add);
        assertTrue(impl.exists(appHome + "NewDir"), "Directory NewDir should exist");
        assertTrue(impl.isDirectory(appHome + "NewDir"), "NewDir should be a directory");

        impl.tapComponent(refresh);
        assertTrue(listing.getText().contains("NewDir"), "Listing should contain NewDir");

        impl.tapComponent(rename);
        assertFalse(impl.exists(appHome + "NewDir"), "Old directory should not exist");
        assertTrue(impl.exists(appHome + "RenamedDir"), "Renamed directory should exist");

        impl.tapComponent(refresh);
        assertTrue(listing.getText().contains("RenamedDir"), "Listing should contain RenamedDir");
        assertFalse(listing.getText().contains("NewDir"), "Listing should not contain NewDir");
    }

    private void appendChildren(StringBuilder sb, File f, int indent) {
        if (f.listFiles() == null) return;
        for (File child : f.listFiles()) {
            for (int i = 0; i < indent; i++) {
                sb.append(' ');
            }
            // Check if getName returns null or something weird
            String name = child.getName();
            if (name != null) {
                sb.append(name).append("\n");
            }
            if (child.isDirectory()) {
                appendChildren(sb, child, indent + 2);
            }
        }
    }

    private void waitForFormTitle(String title) {
        long start = System.currentTimeMillis();
        while(System.currentTimeMillis() - start < 5000) {
            Form f = CN.getCurrentForm();
            if (f != null && title.equals(f.getTitle())) {
                return;
            }
            try { Thread.sleep(50); } catch(Exception e){}
        }
    }
}
