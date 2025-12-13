package com.codename1.contacts;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.*;

class ContactsManagerTest extends UITestBase {

    @FormTest
    void testRetrieveContactsFromImplementation() {
        implementation.clearContacts();
        implementation.setGetAllContactsFast(true);

        Contact original = new Contact();
        original.setId("manual");
        original.setFirstName("Alice");
        Hashtable numbers = new Hashtable();
        numbers.put("mobile", "123");
        original.setPhoneNumbers(numbers);
        original.setPrimaryPhoneNumber("123");
        implementation.putContact(original);

        Contact fetched = ContactsManager.getContactById("manual", true, false, true, false, false);
        assertNotSame(original, fetched);
        assertEquals("Alice", fetched.getFirstName());
        assertEquals("123", fetched.getPrimaryPhoneNumber());

        String[] ids = ContactsManager.getAllContacts();
        assertArrayEquals(new String[]{"manual"}, ids);

        Contact[] contacts = ContactsManager.getContacts(false, true, false, true, false, false);
        assertEquals(1, contacts.length);
        assertTrue(ContactsManager.isAllContactsFast());

        ContactsManager.refresh();
        assertEquals(1, implementation.getRefreshContactsCount());
    }

    @FormTest
    void testCreateAndDeleteContact() {
        implementation.clearContacts();
        String id = ContactsManager.createContact("John", "Doe", "111", null, null, "john@example.com");
        assertNotNull(id);
        Contact created = ContactsManager.getContactById(id, true, false, true, true, false);
        assertEquals("John", created.getFirstName());
        assertEquals("john@example.com", created.getPrimaryEmail());

        assertTrue(ContactsManager.deleteContact(id));
        assertNull(ContactsManager.getContactById(id));
    }
}
