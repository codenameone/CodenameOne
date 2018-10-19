/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui;

import com.codename1.db.Cursor;
import com.codename1.db.Database;
import com.codename1.io.File;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;

import com.codename1.ui.layouts.BorderLayout;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author shannah
 */
public class DatabaseTests extends AbstractTest {

    private static class Res {

        boolean complete;
        Throwable error;
    }

    
    private void testOpenOrCreate() throws Exception {
        String dbName = "testdb";
        Database.delete(dbName); // Start out fresh
        
        Database db = Database.openOrCreate(dbName);
        this.assertNotNull(db, "Failed to open database "+dbName);
        
        this.assertTrue(Database.exists(dbName), "Database.exists() returns false after openOrCreate");
        
        String path = Database.getDatabasePath(dbName);
        this.assertTrue(FileSystemStorage.getInstance().exists(path), "Database doesn't exist after creation");
        
        Database.delete(dbName);
        this.assertTrue(!FileSystemStorage.getInstance().exists(path), "Failed to delete database.");
        
    }
    
    private void testCustomPaths() throws Exception {
        if (Database.isCustomPathSupported()) {
            File dbFile = new File("somedir/testdb");
            dbFile.getParentFile().mkdirs();
            String dbName = dbFile.getAbsolutePath();
            Database.delete(dbName); // Start out fresh

            Database db = Database.openOrCreate(dbName);
            this.assertNotNull(db, "Failed to open database with custom path "+dbName);

            this.assertTrue(Database.exists(dbName), "Database.exists() returns false after openOrCreate with custom path: "+dbName);

            String path = Database.getDatabasePath(dbName);
            this.assertTrue(FileSystemStorage.getInstance().exists(path), "Database doesn't exist after creation with custom path: "+dbName);

            this.assertEqual(dbFile.getAbsolutePath(), path, "Result of getDatabasePath() doesn't match input path with custom path");

            Database.delete(dbName);
            this.assertTrue(!FileSystemStorage.getInstance().exists(path), "Failed to delete database with custom path: "+dbName);
        } else {
            Throwable ex = null;
            File dbFile = new File("somedir/testdb");
            dbFile.getParentFile().mkdirs();
            String dbName = dbFile.getAbsolutePath();
            try {
                Database.openOrCreate(dbName);
            } catch (Throwable t) {
                ex = t;
            }
            this.assertTrue(ex instanceof IllegalArgumentException, "Platforms that don't support custom paths should throw an illegalArgumentException when trying to open databases with file separators in them");
            
            ex = null;
            try {
                Database.exists(dbName);
            } catch (Throwable t) {
                ex = t;
            }
            this.assertTrue(ex instanceof IllegalArgumentException, "Platforms that don't support custom paths should throw an illegalArgumentException when trying to open databases with file separators in them");

            ex = null;
            try {
                Database.delete(dbName);
            } catch (Throwable t) {
                ex = t;
            }
            this.assertTrue(ex instanceof IllegalArgumentException, "Platforms that don't support custom paths should throw an illegalArgumentException when trying to open databases with file separators in them");

            ex = null;
            try {
                Database.getDatabasePath(dbName);
            } catch (Throwable t) {
                ex = t;
            }
            this.assertTrue(ex instanceof IllegalArgumentException, "Platforms that don't support custom paths should throw an illegalArgumentException when trying to open databases with file separators in them");
            
        }
    }
    
    private void testSimpleQueries() throws Exception {
        String dbName = "testdb";
        Database.delete(dbName);
        
        Database db = Database.openOrCreate(dbName);
        db.execute("create table tests (name text)");
        db.execute("insert into tests values ('Steve'), ('Mike'), ('Ryan')");
        Cursor c = db.executeQuery("select count(*) from tests");
        c.next();
        this.assertEqual(3, c.getRow().getInteger(0), "Expected result of 3 for count(*) after inserting 3 rows");
        
        
    }
    
    private void testSimpleQueriesInCustomPath() throws Exception {
        if (Database.isCustomPathSupported()) {
            String dbName = new File("somedir/testdb").getAbsolutePath();
            new File(dbName).getParentFile().mkdirs();
            Database.delete(dbName);

            Database db = Database.openOrCreate(dbName);
            db.execute("create table tests (name text)");
            db.execute("insert into tests values ('Steve'), ('Mike'), ('Ryan')");
            Cursor c = db.executeQuery("select count(*) from tests");
            c.next();
            this.assertEqual(3, c.getRow().getInteger(0), "Expected result of 3 for count(*) after inserting 3 rows");
        } else {
            Log.p("testSimpleQueriesInCustomPath skipped on this platform");
        }
        
        
    }
    
    @Override
    public boolean runTest() throws Exception {
        testOpenOrCreate();
        testCustomPaths();
        testSimpleQueries();
        testSimpleQueriesInCustomPath();
        return true;

    }

}
