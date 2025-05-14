package com.codename1.db;

import com.codename1.testing.AbstractTest;
import com.codename1.ui.Display;

public class SQLiteTest extends AbstractTest {

        @Override
        public boolean runTest() throws Exception {
            Database db = null;
            Cursor cur = null;
            try {
                db = Display.getInstance().openOrCreate("MyDB.db");

                db.execute("create table people (name TEXT, ssn TEXT, age INTEGER)");
                db.execute("insert into people (name, ssn, age) values ('John Doe', '123-45-6789', 25)");
                db.execute("insert into people (name, ssn, age) values ('Jane Doe', '987-65-4321', 30)");
                cur = db.executeQuery("select * from people");
                this.assertEqual(3, cur.getColumnCount(), "Column count mismatch");
                this.assertEqual("name", cur.getColumnName(0), "Column name mismatch");
                this.assertEqual("ssn", cur.getColumnName(1), "Column name mismatch");
                this.assertEqual("age", cur.getColumnName(2), "Column name mismatch");
                this.assertTrue(cur.next(), "No rows returned");
                this.assertEqual("John Doe", cur.getRow().getString(0), "Row data mismatch");
                this.assertEqual("123-45-6789", cur.getRow().getString(1), "Row data mismatch");
                this.assertEqual(25, cur.getRow().getInteger(2), "Row data mismatch");
                this.assertTrue(cur.next(), "No rows returned");
                this.assertEqual("Jane Doe", cur.getRow().getString(0), "Row data mismatch");
                this.assertEqual("987-65-4321", cur.getRow().getString(1), "Row data mismatch");
                this.assertEqual(30, cur.getRow().getInteger(2), "Row data mismatch");
                this.assertFalse(cur.next(), "Too many rows returned");
            } finally {
                if(cur != null) {
                    cur.close();
                }
                if(db != null) {
                    db.close();
                }
            }

            return true;
        }

        @Override
        public boolean shouldExecuteOnEDT() {
            return false;
        }
}
