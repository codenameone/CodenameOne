package com.codename1.properties;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.table.TableModel;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;

public class UiBindingBoundTableModelTest extends UITestBase {

    public static class Person implements PropertyBusinessObject {
        public final Property<String, Person> name = new Property<>("name", String.class);
        public final Property<Integer, Person> age = new Property<>("age", Integer.class);

        private final PropertyIndex index = new PropertyIndex(this, "Person", name, age);

        public PropertyIndex getPropertyIndex() {
            return index;
        }
    }

    @FormTest
    public void testBoundTableModel() {
        UiBinding binding = new UiBinding();
        List<Person> people = new ArrayList<>();
        Person p1 = new Person();
        p1.name.set("John");
        p1.age.set(30);
        people.add(p1);

        Person prototype = new Person();

        // Use generic wildcard to match createTableModel signature
        List<? extends PropertyBusinessObject> genericPeople = people;

        UiBinding.BoundTableModel model = binding.createTableModel(genericPeople, prototype);

        Assertions.assertEquals(1, model.getRowCount());
        Assertions.assertEquals(2, model.getColumnCount()); // name and age

        int nameCol = -1;
        int ageCol = -1;

        for(int i=0; i<model.getColumnCount(); i++) {
            String colName = model.getColumnName(i);
            if("name".equals(colName)) nameCol = i;
            if("age".equals(colName)) ageCol = i;
        }

        Assertions.assertTrue(nameCol != -1);
        Assertions.assertTrue(ageCol != -1);

        Assertions.assertEquals("John", model.getValueAt(0, nameCol));
        Assertions.assertEquals(30, model.getValueAt(0, ageCol));

        // Test modifying value
        model.setValueAt(0, nameCol, "Jane");
        Assertions.assertEquals("Jane", p1.name.get());

        // Test add row
        Person p2 = new Person();
        p2.name.set("Bob");
        p2.age.set(40);
        model.addRow(1, p2);

        Assertions.assertEquals(2, model.getRowCount());
        Assertions.assertEquals("Bob", model.getValueAt(1, nameCol));

        // Test remove row
        model.removeRow(0);
        Assertions.assertEquals(1, model.getRowCount());
        Assertions.assertEquals("Bob", model.getValueAt(0, nameCol));
    }
}
