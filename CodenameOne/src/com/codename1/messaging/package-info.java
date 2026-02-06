/* 
    Document   : package
    Created on : Feb 12, 2012, 10:38:26 AM
    Author     : Chen Fishbein
*/

/// Send e-mail messages through underlying platforms e-mail clients or thru the Codename One cloud.
///
/// You can send messages and include attachments by using the platform native email client like this:
///
/// ```java
/// Message m = new Message("Body of message");
/// m.getAttachments().put(textAttachmentUri, "text/plain");
/// m.getAttachments().put(imageAttachmentUri, "image/png");
/// Display.getInstance().sendMessage(new String[] {"someone@gmail.com"}, "Subject of message", m);
/// ```
///
/// The following code demonstrates sending an email via the Codename One cloud, notice that this is a pro
/// specific feature:
///
/// ```java
/// Message m = new Message("Check out Codename One");
/// m.setMimeType(Message.MIME_HTML);
///
/// // notice that we provide a plain text alternative as well in the send method
/// boolean success = m.sendMessageViaCloudSync("Codename One", "destination@domain.com", "Name Of User", "Message Subject",
///                             "Check out Codename One at https://www.codenameone.com/");
/// ```
package com.codename1.messaging;
