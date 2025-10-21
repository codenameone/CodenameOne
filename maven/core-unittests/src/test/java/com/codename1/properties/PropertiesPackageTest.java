package com.codename1.properties;

import com.codename1.io.Preferences;
import com.codename1.io.Storage;
import com.codename1.io.TestImplementationProvider;
import com.codename1.xml.Element;
import com.codename1.xml.XMLParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PropertiesPackageTest {

    private String originalPreferencesLocation;

    @BeforeEach
    void setup() throws Exception {
        TestImplementationProvider.installImplementation(true);
        resetPreferencesState();
        resetMetadata();
        PropertyBase.bindGlobalGetListener(null);
        PropertyBase.bindGlobalSetListener(null);
    }

    @AfterEach
    void tearDown() throws Exception {
        PropertyBase.bindGlobalGetListener(null);
        PropertyBase.bindGlobalSetListener(null);
        resetPreferencesState();
        if (originalPreferencesLocation != null) {
            Preferences.setPreferencesLocation(originalPreferencesLocation);
        }
        Storage.setStorageInstance(null);
        resetMetadata();
    }

    @Test
    void propertyChangeListenersCanBeRemoved() {
        Person person = new Person();
        List<PropertyBase> triggered = new ArrayList<PropertyBase>();
        PropertyChangeListener<String, Person> listener = new PropertyChangeListener<String, Person>() {
            public void propertyChanged(PropertyBase<String, Person> property) {
                triggered.add(property);
            }
        };
        person.name.addChangeListener(listener);

        person.name.set("Alice");
        assertEquals(1, triggered.size());
        triggered.clear();

        person.name.removeChangeListener(listener);
        person.name.set("Bob");
        assertTrue(triggered.isEmpty());
    }

    @Test
    void globalGetAndSetListenersFire() {
        Person person = new Person();
        AtomicReference<PropertyBase> lastGet = new AtomicReference<PropertyBase>();
        AtomicReference<PropertyBase> lastSet = new AtomicReference<PropertyBase>();
        PropertyBase.bindGlobalGetListener(new PropertyChangeListener() {
            public void propertyChanged(PropertyBase property) {
                lastGet.set(property);
            }
        });
        PropertyBase.bindGlobalSetListener(new PropertyChangeListener() {
            public void propertyChanged(PropertyBase property) {
                lastSet.set(property);
            }
        });

        person.name.get();
        assertSame(person.name, lastGet.get());

        person.name.set("Charles");
        assertSame(person.name, lastSet.get());
    }

    @Test
    void clientPropertiesAndLabelsPersist() {
        Person person = new Person();
        assertEquals("name", person.name.getLabel());
        person.name.setLabel("Full Name");
        assertEquals("Full Name", person.name.getLabel());
        person.name.putClientProperty("hint", "enter name");
        assertEquals("enter name", person.name.getClientProperty("hint"));
    }

    @Test
    void validateCollectionTypeRejectsInvalidTypes() {
        assertThrows(IllegalArgumentException.class, () -> new ListProperty<Object, Person>("bad", Object.class));
    }

    @Test
    void numericPropertyHonorsNullability() {
        IntProperty<Person> score = new IntProperty<Person>("score");
        score.setNullable(false);
        assertThrows(NullPointerException.class, () -> score.set(null));
    }

    @Test
    void propertyEqualityUsesNameAndValue() {
        Property<String, Person> a = new Property<String, Person>("name", "value");
        Property<String, Person> b = new Property<String, Person>("name", "value");
        assertTrue(a.equals(b));
        b.set("other");
        assertFalse(a.equals(b));
    }

    @Test
    void listAndMapPropertiesSupportMutation() {
        Person person = new Person();
        person.tags.add("alpha");
        person.tags.addAll(Arrays.asList("beta", "gamma"));
        assertEquals(3, person.tags.size());
        assertTrue(person.tags.contains("beta"));
        person.tags.remove("beta");
        assertEquals(2, person.tags.size());
        List<String> tags = person.tags.asList();
        assertEquals(Arrays.asList("alpha", "gamma"), tags);

        person.attributes.put("key", "value");
        person.attributes.set("second", "2");
        assertEquals(2, person.attributes.size());
        Map<String, Object> exploded = person.attributes.asExplodedMap();
        assertEquals("value", exploded.get("key"));
        assertEquals("2", exploded.get("second"));
    }

    @Test
    void propertyIndexToMapJsonAndXmlRoundTrip() {
        Person person = new Person();
        person.name.set("Dana");
        person.age.set(28);
        Address address = new Address();
        address.street.set("Main");
        address.city.set("Metropolis");
        address.zip.set(12345);
        person.address.set(address);
        person.tags.add("friend");
        person.history.add(address);
        person.attributes.put("role", "admin");
        person.created.set(new Date(123456789L));

        Map<String, Object> map = person.getPropertyIndex().toMapRepresentation();
        assertEquals("Dana", map.get("name"));
        assertEquals(28, map.get("age"));
        assertTrue(map.containsKey("address"));
        assertTrue(map.get("created") instanceof Date);
        assertEquals(123456789L, ((Date) map.get("created")).getTime());

        Person copy = new Person();
        copy.getPropertyIndex().populateFromMap(map);
        assertEquals("Dana", copy.name.get());
        assertEquals(28, copy.age.get().intValue());
        assertEquals(12345, copy.address.get().zip.getInt());
        assertEquals(1, copy.tags.size());
        assertEquals(1, copy.history.size());
        assertEquals(123456789L, copy.created.get().getTime());

        String json = person.getPropertyIndex().toJSON();
        Person fromJson = new Person();
        fromJson.getPropertyIndex().fromJSON(json);
        assertEquals("Dana", fromJson.name.get());
        assertEquals("admin", fromJson.attributes.get("role"));

        String xml = person.getPropertyIndex().toXML();
        Element element = parse(xml);
        assertEquals("Dana", element.getChildAt(0).getText());
        Person fromXml = new Person();
        fromXml.getPropertyIndex().fromXml(element);
        assertEquals("Dana", fromXml.name.get());
        assertEquals("Metropolis", fromXml.address.get().city.get());
    }

    @Test
    void xmlTextElementConfigurationIsHonored() {
        Person person = new Person();
        person.name.set("Eva");
        PropertyIndex index = person.getPropertyIndex();
        index.setXmlTextElement(person.name, true);
        assertTrue(index.isXmlTextElement(person.name));
        assertSame(person.name, index.getXmlTextElement());
        String xml = index.toXML();
        Element element = parse(xml);
        assertEquals("Eva", element.getChildAt(0).getText());
        index.setXmlTextElement(person.name, false);
        assertNull(index.getXmlTextElement());
    }

    @Test
    void initPopulatesPropertiesInOrder() {
        Person person = new Person();
        Address addr = new Address();
        addr.street.set("Elm");
        addr.city.set("Gotham");
        addr.zip.set(99999);
        person.getPropertyIndex().init("Frank", Integer.valueOf(42), addr,
                Arrays.asList("tag1", "tag2"), new LinkedHashMap<String, String>(), new Date(1L),
                Arrays.asList(new Address[]{addr}));
        assertEquals("Frank", person.name.get());
        assertEquals(42, person.age.getInt());
        assertEquals("Elm", person.address.get().street.get());
        assertEquals(2, person.tags.size());
        assertEquals(1, person.history.size());
    }

    @Test
    void preferencesObjectSynchronizesValues() {
        Settings settings = new Settings();
        settings.title.set("Initial");
        settings.enabled.set(Boolean.TRUE);
        settings.count.set(5);

        PreferencesObject po = PreferencesObject.create(settings);
        po.setPrefix("prefs.");
        po.setName(settings.count, "total");
        po.bind();

        settings.title.set("Updated");
        settings.enabled.set(Boolean.FALSE);
        settings.count.set(7);

        assertEquals("Updated", Preferences.get("prefs.title", ""));
        assertFalse(Preferences.get("prefs.enabled", true));
        assertEquals(7, Preferences.get("prefs.total", 0));
    }

    private Element parse(String xml) {
        XMLParser parser = new XMLParser();
        return parser.parse(new InputStreamReader(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
    }

    private void resetPreferencesState() throws Exception {
        FieldAccessor.resetStaticField(Preferences.class, "p", null);
        FieldAccessor.resetStaticMap(Preferences.class, "listenerMap");
        if (originalPreferencesLocation == null) {
            originalPreferencesLocation = Preferences.getPreferencesLocation();
        }
        Preferences.setPreferencesLocation("PropertiesTest-" + System.nanoTime());
        Storage.setStorageInstance(null);
    }

    private void resetMetadata() throws Exception {
        FieldAccessor.resetStaticMap(PropertyIndex.class, "metadata");
    }

    private static class FieldAccessor {
        private FieldAccessor() {
        }

        static void resetStaticField(Class<?> type, String name, Object value) throws Exception {
            java.lang.reflect.Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        }

        @SuppressWarnings("unchecked")
        static void resetStaticMap(Class<?> type, String name) throws Exception {
            java.lang.reflect.Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            Object obj = field.get(null);
            if (obj instanceof Map) {
                ((Map) obj).clear();
            }
        }
    }

    static class Address implements PropertyBusinessObject {
        final Property<String, Address> street = new Property<String, Address>("street");
        final Property<String, Address> city = new Property<String, Address>("city");
        final IntProperty<Address> zip = new IntProperty<Address>("zip");
        private final PropertyIndex index = new PropertyIndex(this, "Address", street, city, zip);

        public PropertyIndex getPropertyIndex() {
            return index;
        }
    }

    static class Person implements PropertyBusinessObject {
        final Property<String, Person> name = new Property<String, Person>("name");
        final IntProperty<Person> age = new IntProperty<Person>("age");
        final Property<Address, Person> address = new Property<Address, Person>("address", Address.class);
        final ListProperty<String, Person> tags = new ListProperty<String, Person>("tags");
        final MapProperty<String, String, Person> attributes = new MapProperty<String, String, Person>("attributes", String.class, String.class);
        final Property<Date, Person> created = new Property<Date, Person>("created", Date.class);
        final ListProperty<Address, Person> history = new ListProperty<Address, Person>("history", Address.class);
        private final PropertyIndex index = new PropertyIndex(this, "Person", name, age, address, tags, attributes, created, history);

        Person() {
            address.set(new Address());
        }

        public PropertyIndex getPropertyIndex() {
            return index;
        }
    }

    static class Settings implements PropertyBusinessObject {
        final Property<String, Settings> title = new Property<String, Settings>("title", "");
        final Property<Boolean, Settings> enabled = new Property<Boolean, Settings>("enabled", Boolean.class, Boolean.FALSE);
        final Property<Integer, Settings> count = new Property<Integer, Settings>("count", Integer.class, Integer.valueOf(0));
        private final PropertyIndex index = new PropertyIndex(this, "Settings", title, enabled, count);

        public PropertyIndex getPropertyIndex() {
            return index;
        }
    }

}
