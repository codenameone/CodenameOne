package com.codename1.properties;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codename1.xml.Element;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class InstantUIBindingIntegrationTest extends UITestBase {

    static class Profile extends PropertyBusinessObject {
        final Property<String, String> email = new Property<String, String>("email", String.class);
        final Property<String, String> password = new Property<String, String>("password", String.class);
        final Property<String, String> phoneNumber = new Property<String, String>("phoneNumber", String.class);
        final Property<Boolean, Boolean> active = new Property<Boolean, Boolean>("active", Boolean.class);
        final Property<Date, Date> birthday = new Property<Date, Date>("birthday", Date.class);
        final Property<String, String> role = new Property<String, String>("role", String.class);
        final Property<String, String> custom = new Property<String, String>("custom", String.class);
        final SetProperty<String, Profile> tags = new SetProperty<String, Profile>("tags");
        final PropertyIndex index = new PropertyIndex(this, "Profile", email, password, phoneNumber, active, birthday, role, custom, tags);

        public PropertyIndex getPropertyIndex() {
            return index;
        }
    }

    static class AdaptedHolder extends PropertyBusinessObject {
        final Property<Integer, Integer> count = new Property<Integer, Integer>("count", Integer.class);
        final PropertyIndex index = new PropertyIndex(this, "AdaptedHolder", count);
    }

    static class XmlChild extends PropertyBusinessObject {
        final Property<String, String> description = new Property<String, String>("description", String.class);
        final PropertyIndex index = new PropertyIndex(this, "XmlChild", description);
    }

    static class XmlParent extends PropertyBusinessObject {
        final Property<String, String> title = new Property<String, String>("title", String.class);
        final Property<Integer, Integer> identifier = new Property<Integer, Integer>("identifier", Integer.class);
        final Property<XmlChild, XmlChild> nested = new Property<XmlChild, XmlChild>("nested", XmlChild.class);
        final PropertyIndex index = new PropertyIndex(this, "XmlParent", title, identifier, nested);
    }

    static class Record extends PropertyBusinessObject {
        final Property<Integer, Integer> id = new Property<Integer, Integer>("id", Integer.class);
        final Property<String, String> name = new Property<String, String>("name", String.class);
        final Property<Long, Long> created = new Property<Long, Long>("created", Long.class);
        final PropertyIndex index = new PropertyIndex(this, "Record", id, name, created);
    }

    private Component findByName(Container root, String name) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component cmp = root.getComponentAt(i);
            if (name.equals(cmp.getName())) {
                return cmp;
            }
            if (cmp instanceof Container) {
                Component nested = findByName((Container) cmp, name);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    @FormTest
    void instantUICreatesAndBindsComponents() {
        Profile profile = new Profile();
        InstantUI iui = new InstantUI();
        iui.excludeProperty(profile.password);
        iui.setMultiChoiceLabels(profile.role, new String[]{"User", "Admin", "Guest"});
        iui.setMultiChoiceValues(profile.role, new Object[]{"U", "A", "G"});
        iui.setComponentClass(profile.custom, CheckBox.class);
        iui.setTextFieldConstraint(profile.phoneNumber, TextField.PHONENUMBER);
        iui.setOrder(profile.custom, profile.role, profile.phoneNumber, profile.email, profile.active, profile.birthday, profile.tags);

        Container editUi = iui.createEditUI(profile, false);
        Form host = new Form(BoxLayout.y());
        host.add(editUi);
        host.show();

        assertNull(findByName(editUi, profile.password.getName()));
        Component phoneCmp = findByName(editUi, profile.phoneNumber.getName());
        assertTrue(phoneCmp instanceof TextField);
        assertEquals(TextField.PHONENUMBER, ((TextField) phoneCmp).getConstraint());

        Component customCmp = findByName(editUi, profile.custom.getName());
        assertTrue(customCmp instanceof CheckBox);

        Component roleCmp = findByName(editUi, profile.role.getName());
        assertTrue(roleCmp instanceof Picker || roleCmp instanceof Container);

        UiBinding.Binding binding = iui.getBindings(editUi);
        binding.setAutoCommit(false);

        TextField emailField = (TextField) findByName(editUi, profile.email.getName());
        emailField.setText("new@example.com");
        binding.commit();
        assertEquals("new@example.com", profile.email.get());

        profile.email.set("rollback@example.com");
        emailField.setText("stale@example.com");
        binding.rollback();
        assertEquals("rollback@example.com", emailField.getText());

        UiBinding.unbind(profile);
    }

    @FormTest
    void mapAdapterOverridesMapConversions() {
        new MapAdapter(Integer.class) {
            public boolean useAdapterFor(PropertyBase b) {
                return true;
            }

            public void placeInMap(PropertyBase b, Map m) {
                m.put(b.getName(), ((Integer) b.get()) * 2);
            }

            public void setFromMap(PropertyBase b, Map m) {
                Object incoming = m.get(b.getName());
                if (incoming instanceof Integer) {
                    b.setImpl(((Integer) incoming) + 1);
                }
            }
        };

        AdaptedHolder holder = new AdaptedHolder();
        holder.count.set(5);
        Map<String, Object> converted = holder.getPropertyIndex().toMapRepresentation();
        assertEquals(10, converted.get("count"));

        AdaptedHolder fromMap = new AdaptedHolder();
        fromMap.getPropertyIndex().populateFromMap(converted);
        assertEquals(11, fromMap.count.get().intValue());
    }

    @FormTest
    void setPropertySupportsMutationAndComparison() {
        Profile profile = new Profile();
        AtomicInteger changes = new AtomicInteger();
        profile.tags.addChangeListener(new PropertyChangeListener<String, Profile>() {
            public void propertyChanged(PropertyBase<String, Profile> source) {
                changes.incrementAndGet();
            }
        });

        profile.tags.add("two");
        profile.tags.addAll(Arrays.asList("three", "four"));
        assertEquals(3, profile.tags.size());
        assertTrue(profile.tags.contains("three"));

        profile.tags.remove("two");
        profile.tags.remove(0);
        profile.tags.removeAll(new HashSet<String>(Arrays.asList("four")));
        assertEquals(0, profile.tags.size());
        assertTrue(changes.get() >= 4);

        Profile mirror = new Profile();
        mirror.tags.set(profile.tags.asList());
        assertEquals(profile.tags, mirror.tags);
        assertEquals(profile.tags.hashCode(), mirror.tags.hashCode());

        profile.tags.add("z");
        profile.tags.clear();
        assertEquals(0, profile.tags.size());
    }

    @FormTest
    void propertyXmlElementSupportsTraversal() {
        XmlParent parent = new XmlParent();
        parent.title.set("Root");
        parent.identifier.set(7);
        XmlChild child = new XmlChild();
        child.description.set("Nested");
        parent.nested.set(child);
        parent.getPropertyIndex().setXmlTextElement(parent.title, true);

        PropertyXMLElement element = (PropertyXMLElement) parent.getPropertyIndex().asElement();
        assertEquals("XmlParent", element.getTagName());
        assertEquals("Root", element.getAttribute("title"));
        assertEquals(7, element.getAttributeAsInt("identifier", 0));
        assertEquals(2, element.getNumChildren());

        Element textChild = element.getChildAt(0);
        assertEquals("title", textChild.getTagName());
        assertEquals("Root", textChild.getText());

        Element nestedElement = element.getChildAt(1);
        assertTrue(element.contains(nestedElement));
        assertEquals(1, element.getChildIndex(nestedElement));
        assertEquals(element, nestedElement.getParent());
        assertEquals(child.description.get(), ((PropertyXMLElement) nestedElement).getChildAt(0).getText());
    }

    @FormTest
    void sqlMapExecutesAgainstTestDatabase() throws IOException {
        TestCodenameOneImplementation.TestDatabase db = new TestCodenameOneImplementation.TestDatabase("memory");
        db.markOpen();
        db.setQueryResult(new String[]{"name"}, new Object[][]{});

        SQLMap map = SQLMap.create(db);
        Record record = new Record();
        record.name.set("alpha");
        record.created.set(123L);
        map.setPrimaryKeyAutoIncrement(record, record.id);
        map.setTableName(record, "records");
        map.setColumnName(record.name, "full_name");
        map.setSqlType(record.created, SQLMap.SqlType.SQL_LONG);

        assertEquals(SQLMap.SqlType.SQL_BOOLEAN, map.getSqlType(new Property<Boolean, Boolean>("flag", Boolean.class)));
        assertEquals("records", map.getTableName(record));
        assertEquals("full_name", map.getColumnName(record.name));

        assertTrue(map.createTable(record));
        assertTrue(db.getExecutedStatements().get(0).startsWith("SELECT * FROM sqlite_master"));
        assertTrue(db.getExecutedStatements().get(1).startsWith("CREATE TABLE"));

        map.insert(record);
        assertTrue(db.getExecutedStatements().get(2).startsWith("INSERT INTO"));

        record.id.set(1);
        map.update(record);
        assertTrue(db.getExecutedStatements().get(3).startsWith("UPDATE"));

        map.delete(record);
        assertTrue(db.getExecutedStatements().get(4).startsWith("DELETE"));
    }
}
