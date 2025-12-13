package com.codename1.samples;

import com.codename1.db.Cursor;
import com.codename1.db.Database;
import com.codename1.db.Row;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.table.DefaultTableModel;
import com.codename1.ui.table.Table;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import com.codename1.testing.TestCodenameOneImplementation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class SQLExplorerSampleTest extends UITestBase {

    @FormTest
    public void testSQLExplorer() {
        Form f = new Form("SQL Explorer", new BorderLayout());
        f.show();

        Database db = null;
        try {
            db = Display.getInstance().openOrCreate("MyDB.db");
            TestCodenameOneImplementation.TestDatabase testDb = (TestCodenameOneImplementation.TestDatabase) db;

            // Simulate "Execute" logic directly without Dialog
            executeQuery(f, db, "create table test (id integer, name text);");
            assertLabelContains(f, "Query completed successfully");
            assertTrue(testDb.getExecutedStatements().contains("create table test (id integer, name text);"));

            executeQuery(f, db, "insert into test (id, name) values (1, 'Codename One');");
            assertLabelContains(f, "Query completed successfully");
            assertTrue(testDb.getExecutedStatements().contains("insert into test (id, name) values (1, 'Codename One');"));

            executeQuery(f, db, "insert into test (id, name) values (2, 'Java');");
            assertLabelContains(f, "Query completed successfully");
            assertTrue(testDb.getExecutedStatements().contains("insert into test (id, name) values (2, 'Java');"));

            // Mock the result for the select query
            testDb.setQueryResult(new String[]{"id", "name"}, new Object[][]{
                {1, "Codename One"},
                {2, "Java"}
            });

            executeQuery(f, db, "select * from test;");
            // Verify table content
            Component c = f.getContentPane().getComponentAt(0);
            if (c instanceof Table) {
                Table table = (Table) c;
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                assertEquals(2, model.getRowCount());
                assertEquals("Codename One", model.getValueAt(0, 1));
                assertEquals("Java", model.getValueAt(1, 1));
            } else if (c instanceof com.codename1.ui.Label) {
                 fail("Expected Table but found Label: " + ((com.codename1.ui.Label)c).getText());
            } else {
                 fail("Expected Table but found " + c.getClass().getName());
            }
        } catch (IOException e) {
            fail("Database error: " + e.getMessage());
        } finally {
            Util.cleanup(db);
        }
    }

    private void assertLabelContains(Form f, String text) {
        Object cmp = f.getContentPane().getComponentAt(0);
        String actualText = "";
        if (cmp instanceof com.codename1.ui.Label) {
            actualText = ((com.codename1.ui.Label)cmp).getText();
        } else if (cmp instanceof com.codename1.components.SpanLabel) {
             actualText = ((com.codename1.components.SpanLabel)cmp).getText();
        } else if (cmp instanceof com.codename1.ui.TextArea) {
             actualText = ((com.codename1.ui.TextArea)cmp).getText();
        }

        if (!actualText.contains(text)) {
            assertEquals(text, actualText);
        }
    }

    private void executeQuery(Form hi, Database db, String queryText) {
        Cursor cur = null;
        try {
            if (queryText.startsWith("select")) {
                cur = db.executeQuery(queryText);
                int columns = cur.getColumnCount();
                hi.removeAll();
                if (columns > 0) {
                    boolean next = cur.next();
                    if (next) {
                        ArrayList<String[]> data = new ArrayList<>();
                        String[] columnNames = new String[columns];
                        for (int iter = 0; iter < columns; iter++) {
                            columnNames[iter] = cur.getColumnName(iter);
                        }
                        while (next) {
                            Row currentRow = cur.getRow();
                            String[] currentRowArray = new String[columns];
                            for (int iter = 0; iter < columns; iter++) {
                                currentRowArray[iter] = currentRow.getString(iter);
                            }
                            data.add(currentRowArray);
                            next = cur.next();
                        }
                        Object[][] arr = new Object[data.size()][];
                        data.toArray(arr);
                        hi.add(BorderLayout.CENTER, new Table(new DefaultTableModel(columnNames, arr)));
                    } else {
                        hi.add(BorderLayout.CENTER, "Query returned no results");
                    }
                } else {
                    hi.add(BorderLayout.CENTER, "Query returned no results");
                }
            } else {
                db.execute(queryText);
                hi.removeAll();
                hi.add(BorderLayout.CENTER, "Query completed successfully");
            }
            hi.revalidate();
        } catch (IOException err) {
            Log.e(err);
            hi.removeAll();
            hi.add(BorderLayout.CENTER, "Error: " + err);
            hi.revalidate();
        } finally {
            Util.cleanup(cur);
        }
    }
}
