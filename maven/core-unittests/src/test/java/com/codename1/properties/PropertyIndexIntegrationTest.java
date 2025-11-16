package com.codename1.properties;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.list.MultiList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PropertyIndexIntegrationTest extends UITestBase {

    static class ContactInfo extends PropertyBusinessObject {
        final Property<String, String> first = new Property<String, String>("first", String.class);
        final Property<String, String> last = new Property<String, String>("last", String.class);
        final Property<Integer, Integer> age = new Property<Integer, Integer>("age", Integer.class);
        final PropertyIndex index = new PropertyIndex(this, "ContactInfo", first, last, age);

        public PropertyIndex getPropertyIndex() {
            return index;
        }
    }

    @FormTest
    void propertyIndexSupportsSerializationAndCopyFrom() throws IOException {
        ContactInfo source = new ContactInfo();
        source.first.set("Ada");
        source.last.set("Lovelace");
        source.age.set(28);

        ContactInfo clone = new ContactInfo();
        clone.getPropertyIndex().copyFrom(source.getPropertyIndex());
        assertEquals("Ada", clone.first.get());
        assertEquals(Integer.valueOf(28), clone.age.get());

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        source.getPropertyIndex().getPropertyIndexState().save(new DataOutputStream(bout));
        ContactInfo loaded = new ContactInfo();
        loaded.getPropertyIndex().getPropertyIndexState().load(new DataInputStream(new ByteArrayInputStream(bout.toByteArray())));
        assertEquals("Lovelace", loaded.last.get());
    }

    @FormTest
    void propertyIndexPopulatesFromMapAndTriggersListeners() {
        ContactInfo info = new ContactInfo();
        final AtomicInteger changeEvents = new AtomicInteger();
        info.getPropertyIndex().addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChanged(String propertyName, Object oldValue, Object newValue) {
                changeEvents.incrementAndGet();
            }
        });

        Hashtable data = new Hashtable();
        data.put("first", "Grace");
        data.put("last", "Hopper");
        data.put("age", new Integer(30));
        info.getPropertyIndex().populateFromMap(data);

        assertEquals(3, changeEvents.get());
        Hashtable snapshot = info.getPropertyIndex().asHashtable();
        assertEquals("Grace", snapshot.get("first"));
        assertEquals("Hopper", snapshot.get("last"));
    }

    @FormTest
    void propertyIndexIntegratesWithMultiListModel() {
        ContactInfo info = new ContactInfo();
        info.first.set("Tim");
        info.last.set("Berners-Lee");
        info.age.set(39);

        List<ContactInfo> entries = Arrays.asList(info);
        String json = PropertyIndex.toJSONList(entries);
        assertTrue(json.contains("Berners-Lee"));

        MultiList list = new MultiList();
        list.setName("contacts");
        Form form = new Form(BoxLayout.y());
        form.add(new Label("Header"));
        form.add(list);
        form.show();

        list.setModel(new com.codename1.ui.list.DefaultListModel(entries.toArray()));
        assertEquals(entries.get(0).getPropertyIndex(), list.getModel().getItemAt(0));
        assertTrue(list.getRenderer() instanceof com.codename1.ui.list.ListCellRenderer);
    }
}
