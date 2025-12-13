package com.codename1.contacts;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;
import org.junit.jupiter.api.Assertions;

import java.util.Hashtable;

public class ContactsModelTest extends UITestBase {

    @FormTest
    public void testContactsModel() {
        ContactsModel model = new ContactsModel();
        Image placeholder = Image.createImage(10, 10, 0);
        model.setPlaceHolderImage(placeholder);

        // Add a contact manually as a Hashtable since we can't easily mock ContactsManager fully without modifying TestCodenameOneImplementation significantly or relying on it.
        // But ContactsModel.addItem handles Hashtable.

        Hashtable contactData = new Hashtable();
        contactData.put("id", "123");
        contactData.put("fname", "John");
        contactData.put("lname", "Doe");

        model.addItem(contactData);

        Assertions.assertEquals(1, model.getSize());

        Object item = model.getItemAt(0);
        Assertions.assertTrue(item instanceof Hashtable);
        Hashtable retrieved = (Hashtable) item;
        Assertions.assertEquals("John", retrieved.get("fname"));

        // Test removing
        model.removeItem(0);
        Assertions.assertEquals(0, model.getSize());
    }
}
