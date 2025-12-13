package com.codename1.properties;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.db.Database;
import org.junit.jupiter.api.Assertions;
import java.util.List;

public class SQLMapTest extends UITestBase {

    public static class MyData implements PropertyBusinessObject {
        public final Property<String, MyData> name = new Property<>("name", String.class);
        public final Property<Integer, MyData> age = new Property<>("age", Integer.class);
        public final PropertyIndex idx = new PropertyIndex(this, "MyData", name, age);

        @Override
        public PropertyIndex getPropertyIndex() {
            return idx;
        }
    }

    @FormTest
    public void testCreateTable() throws Exception {
        Database db = TestCodenameOneImplementation.getInstance().openOrCreateDB("test.db");
        SQLMap map = SQLMap.create(db);
        MyData data = new MyData();

        map.setPrimaryKey(data, data.name);

        // Mock query result for table existence check (returns false initially)
        ((TestCodenameOneImplementation.TestDatabase)db).setQueryResult(new String[]{"type", "name"}, new Object[][]{});

        boolean created = map.createTable(data);
        Assertions.assertTrue(created);

        List<String> statements = ((TestCodenameOneImplementation.TestDatabase)db).getExecutedStatements();
        boolean foundCreate = false;
        for(String s : statements) {
            if (s.contains("CREATE TABLE")) {
                foundCreate = true;
                break;
            }
        }
        Assertions.assertTrue(foundCreate);
    }

    @FormTest
    public void testInsert() throws Exception {
        Database db = TestCodenameOneImplementation.getInstance().openOrCreateDB("test.db");
        SQLMap map = SQLMap.create(db);
        MyData data = new MyData();
        data.name.set("John");
        data.age.set(30);

        map.insert(data);

        List<String> statements = ((TestCodenameOneImplementation.TestDatabase)db).getExecutedStatements();
        boolean foundInsert = false;
        for(String s : statements) {
            if (s.contains("INSERT INTO")) {
                foundInsert = true;
                break;
            }
        }
        Assertions.assertTrue(foundInsert);

        List<String[]> params = ((TestCodenameOneImplementation.TestDatabase)db).getExecutedParameters();
    }

    @FormTest
    public void testSelect() throws Exception {
        Database db = TestCodenameOneImplementation.getInstance().openOrCreateDB("test.db");
        SQLMap map = SQLMap.create(db);
        MyData data = new MyData();

        // Mock result
        ((TestCodenameOneImplementation.TestDatabase)db).setQueryResult(
            new String[]{"name", "age"},
            new Object[][]{
                {"John", 30},
                {"Jane", 25}
            }
        );

        List<PropertyBusinessObject> results = map.select(data, null, true, 0, 0);
        Assertions.assertEquals(2, results.size());
        MyData res1 = (MyData) results.get(0);
        Assertions.assertEquals("John", res1.name.get());
        Assertions.assertEquals(30, res1.age.get().intValue());
    }

    @FormTest
    public void testDelete() throws Exception {
        Database db = TestCodenameOneImplementation.getInstance().openOrCreateDB("test.db");
        SQLMap map = SQLMap.create(db);
        MyData data = new MyData();
        map.setPrimaryKey(data, data.name);
        data.name.set("John");

        map.delete(data);

        List<String> statements = ((TestCodenameOneImplementation.TestDatabase)db).getExecutedStatements();
        boolean foundDelete = false;
        for(String s : statements) {
            if (s.contains("DELETE FROM")) {
                foundDelete = true;
                break;
            }
        }
        Assertions.assertTrue(foundDelete);
    }
}
