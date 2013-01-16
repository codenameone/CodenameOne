/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */
package com.codename1.share;

import com.codename1.components.MultiButton;
import com.codename1.contacts.ContactsManager;
import com.codename1.contacts.ContactsModel;
import com.codename1.messaging.Message;
import com.codename1.ui.*;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.list.GenericListCellRenderer;
import com.codename1.ui.list.ListCellRenderer;
import com.codename1.ui.util.Resources;
import java.util.Hashtable;

/**
 * Email sharing service.
 * 
 * @author Chen
 */
public class EmailShare extends ShareService {

    /**
     * Default Constructor
     */
    public EmailShare() {
        super("Email", Resources.getSystemResource().getImage("mail.png"));
    }

    /**
     * @inheritDoc
     */
    public void share(final String toShare, final String image, final String mimeType) {
        final Form currentForm = Display.getInstance().getCurrent();
        final Form contactsForm = new Form("Contacts");
        contactsForm.setLayout(new BorderLayout());
        contactsForm.setScrollable(false);
        contactsForm.addComponent(BorderLayout.CENTER, new Label("Please wait..."));
        contactsForm.show();
        Display.getInstance().startThread(new Runnable() {

            public void run() {
                
                String[] ids = ContactsManager.getAllContacts();
                if(ids == null || ids.length == 0){
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            Dialog.show("Failed to Share", "No Contacts Found", "Ok", null);
                            currentForm.showBack();
                        }
                    });
                    return;
                }
                ContactsModel model = new ContactsModel(ids);
                final List contacts = new List(model);
                contacts.setRenderer(createListRenderer());
                Display.getInstance().callSerially(new Runnable() {

                    public void run() {

                        contacts.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                final ShareForm [] f = new ShareForm[1];
                                Hashtable contact = (Hashtable) contacts.getSelectedItem();
                                if(image == null){
                                    f[0] = new ShareForm(contactsForm, "Send Email", (String)contact.get("email"), toShare,
                                            new ActionListener() {

                                                public void actionPerformed(ActionEvent evt) {
                                                    String [] recieptents = new String[1];
                                                    recieptents[0] = f[0].getTo();
                                                    Message msg = new Message(toShare);
                                                    Message.sendMessage(recieptents, "share", msg);
                                                    finish();
                                                }
                                            });
                                    f[0].show();
                                }else{
                                    f[0] = new ShareForm(contactsForm, "Send Email", (String)contact.get("email"), toShare, image,
                                            new ActionListener() {

                                                public void actionPerformed(ActionEvent evt) {
                                                    String [] recieptents = new String[1];
                                                    recieptents[0] = f[0].getTo();
                                                    Message msg = new Message(toShare);
                                                    msg.setAttachment(image);
                                                    msg.setMimeType(mimeType);
                                                    Message.sendMessage(recieptents, "share", msg);
                                                    finish();
                                                }
                                            });
                                    f[0].show();                                
                                }
                            }
                        });
                        contactsForm.addComponent(BorderLayout.CENTER, contacts);
                        Command back = new Command("Back"){

                            public void actionPerformed(ActionEvent evt) {
                                currentForm.showBack();
                            }
                            
                        };
                        contactsForm.addCommand(back);
                        contactsForm.setBackCommand(back);                        
                        contactsForm.revalidate();
                    }
                });
            }
        }, "Email Thread").start();
    }
    
    
    
    /**
     * @inheritDoc
     */
    public void share(final String toShare) {
        share(toShare, null, null);
    }
    
    
    private MultiButton createRendererMultiButton() {
        MultiButton b = new MultiButton();
        b.setIconName("icon");
        b.setNameLine1("fname");
        b.setNameLine2("phone");
        b.setUIID("Label");
        return b;
    }
    
    private ListCellRenderer createListRenderer() {
        MultiButton sel = createRendererMultiButton();
        MultiButton unsel = createRendererMultiButton();
        return new GenericListCellRenderer(sel, unsel);
    }

    /**
     * @inheritDoc
     */
    public boolean canShareImage() {
        return true;
    }
    
}
