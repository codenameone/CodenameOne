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
package com.codename1.push;

import com.codename1.components.InfiniteProgress;
import com.codename1.components.MultiButton;
import com.codename1.components.WebBrowser;
import com.codename1.io.services.ImageDownloadService;
import com.codename1.l10n.L10NManager;
import com.codename1.messaging.Message;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.table.TableLayout;
import java.util.Date;

/**
 * Simple UI that allows developers to integrate the push inbox functionality directly into
 * their application. 
 *
 * @author Shai Almog
 */
class PushInboxUI extends Container {
    private String replyEmail;
    
    //private static String defaultFrom = "";

    /**
     * The default entry in the from field for this inbox when a user writes a message
     * @return the defaultFrom
     */
    /*public static String getDefaultFrom() {
        return defaultFrom;
    }*/

    /**
     * The default entry in the from field for this inbox when a user writes a message
     * @param aDefaultFrom the defaultFrom to set
     */
    /*public static void setDefaultFrom(String aDefaultFrom) {
        defaultFrom = aDefaultFrom;
    }*/
    
    /**
     * Constructs a new inbox UI
     * 
     * @param placeholder the placeholder image
     */
    public PushInboxUI(final EncodedImage placeholder) {
        setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        int messages = PushInbox.getInstance().getMessageCount();
        for(int iter = 0 ; iter < messages ; iter++) {
            addComponent(createMessageButton(iter, placeholder));
        }
        PushInbox.getInstance().setUpdateCallback(new Runnable() {
            public void run() {
                PushInbox p = PushInbox.getInstance();
                int add = p.getMessageCount() - getComponentCount();
                for(int iter = 0 ; iter < add ; iter++) {
                    addComponent(createMessageButton(iter, placeholder));
                }
            }
        });
        addPullToRefresh(new Runnable() {
            public void run() {
                PushInbox.getInstance().syncInbox();
            }
        });
    }

    private MultiButton createMessageButton(final int iter, EncodedImage placeholder) {
        final MultiButton b = new MultiButton();
        b.setIcon(placeholder);
        final PushInbox p = PushInbox.getInstance();
        b.setTextLine1(p.getTitle(iter));
        b.setTextLine2(Display.getInstance().getLocalizationManager().formatDateTimeShort(new Date(p.getTime(iter))) + 
                " - " + p.getFrom(iter));
        String url = p.getIconURL(iter);
        if(url != null && url.length() > 0 && placeholder != null) {
            ImageDownloadService.createImageToStorage(url, b.getIconComponent(), p.getMessageId(iter), new Dimension(placeholder.getWidth(), placeholder.getHeight()));
        }
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                PushInbox.getInstance().setRead(iter, true);
                final Form previous = Display.getInstance().getCurrent();
                Form frm = new Form(b.getTextLine1());
                frm.setScrollable(false);
                frm.setLayout(new BorderLayout());
                if(p.isHTML(iter)) {
                    WebBrowser wb = new WebBrowser();
                    wb.setPage(p.getBody(iter), null);
                    frm.addComponent(BorderLayout.CENTER, wb);
                } else {
                    TextArea t = new TextArea(p.getBody(iter), 30, 30);
                    t.setFocusable(false);
                    t.setEditable(false);
                    frm.addComponent(BorderLayout.CENTER, t);
                }
                Command cmd = new Command("Back") {
                    public void actionPerformed(ActionEvent ev) {
                        previous.showBack();
                    }
                };
                frm.setBackCommand(cmd);
                if(replyEmail != null && replyEmail.length() > 0) {
                    Command reply = new Command("Reply") {
                        public void actionPerformed(ActionEvent ev) {
                            String subject = b.getTextLine1();
                            if(subject.toLowerCase().indexOf("re:") < 0) {
                                subject = "RE: " + subject;
                            }
                            Display.getInstance().sendMessage(new String[] { replyEmail }, subject, new Message(""));
                            // showComposeUI(defaultFrom, subject, "", p.getMessageId(iter));
                        }
                    };
                    reply.putClientProperty("android:showAsAction", "withText");
                    frm.addCommand(reply);
                }
                frm.show();
                PushInbox.getInstance().setRead(iter, true);
            }
        });
        return b;
    }
    
    /**
     * Shows a compose message UI
     * @param from the default from text
     * @param subject the default subject for the message
     * @param body the default body of the message
     * @param replyToMessageId the id of the message replying to or null if this isn't a reply
     */
    /*private void showComposeUI(String from, String subject, String body, final String replyToMessageId) {
        final Form previous = Display.getInstance().getCurrent();
        Form frm = new Form("Message");
        frm.setScrollable(false);
        frm.setLayout(new BorderLayout());
        final TextArea bodyText = new TextArea(body, 30, 30);
        final TextField subjectText = new TextField(subject);
        final TextField fromText = new TextField(from);
        
        TableLayout tl = new TableLayout(2, 2);
        tl.setGrowHorizontally(true);
        Container north = new Container(tl);
        north.addComponent(new Label("From"));
        north.addComponent(fromText);
        north.addComponent(new Label("Subject"));
        north.addComponent(subjectText);
        frm.addComponent(BorderLayout.NORTH, north);
        frm.addComponent(BorderLayout.CENTER, bodyText);
        
        Command cmd = new Command("Cancel") {
            public void actionPerformed(ActionEvent ev) {
                previous.showBack();
            }
        };
        Command send = new Command("Send") {
            public void actionPerformed(ActionEvent ev) {
                Dialog dlg = new InfiniteProgress().showInifiniteBlocking();
                PushInbox.getInstance().sendMessageToDeveloper(fromText.getText(), subjectText.getText(), bodyText.getText(), replyToMessageId);
                dlg.dispose();
                previous.showBack();
            }
        };
        send.putClientProperty("android:showAsAction", "withText");
        frm.setBackCommand(cmd);
        frm.addCommand(send);
        frm.show();
    }*/

    /**
     * When setting a reply email the user will have an option to reply to a message via email
     * @return the replyEmail
     */
    public String getReplyEmail() {
        return replyEmail;
    }

    /**
     * When setting a reply email the user will have an option to reply to a message via email
     * @param replyEmail the replyEmail to set
     */
    public void setReplyEmail(String replyEmail) {
        this.replyEmail = replyEmail;
    }

}
