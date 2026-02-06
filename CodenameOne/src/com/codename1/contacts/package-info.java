/// Provides access to the contacts list from the device address book
///
/// The contacts API provides us with the means to query the phone's address book, delete elements from
/// it and create new entries into it.
///
/// Notice that on some platforms this will prompt the user for permissions and the user might choose not
/// to grant that permission. To detect whether this is the case you can invoke
/// `com.codename1.Display#isContactsPermissionGranted()` after invoking the contact listing API.
/// This can help you adapt your error message to the user.
///
/// The sample below demonstrates listing all the contacts within the device with their photos, notice that
/// this API is very performance sensitive and should be invoked on a separate thread unlike most
/// Codename One API's:
///
/// ```java
/// Form hi = new Form("Contacts", new BoxLayout(BoxLayout.Y_AXIS));
/// hi.add(new InfiniteProgress());
/// int size = Display.getInstance().convertToPixels(5, true);
/// FontImage fi = FontImage.createFixed("" + FontImage.MATERIAL_PERSON, FontImage.getMaterialDesignFont(), 0xff, size, size);
///
/// Display.getInstance().scheduleBackgroundTask(() -> {
///     Contact[] contacts = Display.getInstance().getAllContacts(true, true, false, true, false, false);
///     Display.getInstance().callSerially(() -> {
///         hi.removeAll();
///         for(Contact c : contacts) {
///             MultiButton mb = new MultiButton(c.getDisplayName());
///             mb.setIcon(fi);
///             mb.setTextLine2(c.getPrimaryPhoneNumber());
///             hi.add(mb);
///             mb.putClientProperty("id", c.getId());
///             Display.getInstance().scheduleBackgroundTask(() -> {
///                 Contact cc = ContactsManager.getContactById(c.getId(), false, true, false, false, false);
///                 Display.getInstance().callSerially(() -> {
///                     Image photo = cc.getPhoto();
///                     if(photo != null) {
///                         mb.setIcon(photo.fill(size, size));
///                         mb.revalidate();
///                     }
///                 });
///             });
///         }
///         hi.getContentPane().animateLayout(150);
///     });
/// });
/// ```
package com.codename1.contacts;
