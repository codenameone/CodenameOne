/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.examples.hellocodenameone;

import com.codename1.ai.ChatMessage;
import com.codename1.components.ChatView;
import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.ImageIO;
import com.codename1.ui.util.Resources;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Builds the ChatView screenshot embedded in the developer guide's "AI,
 * Chat UI, and Speech" chapter.
 *
 * The conversation is hard-coded so the capture is reproducible. Stored
 * under the override name `chat-view.png` -- the developer-guide build
 * picks it up from Storage / `~/.cn1` and copies the result into
 * `docs/developer-guide/img/`.
 */
public class ChatViewDevGuideScreenshotTest extends AbstractTest {
    private static final String STORAGE_KEY = "chat-view.png";
    private static final long FORM_TIMEOUT_MS = 5000L;

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }

    @Override
    public boolean runTest() throws Exception {
        installModernTheme();

        Form chat = new Form("Assistant", new BorderLayout());
        ChatView view = new ChatView();
        chat.add(BorderLayout.CENTER, view);

        view.addMessage(ChatMessage.system("AI Travel Assistant"));
        view.addMessage(ChatMessage.user(
                "Plan a 3-day Lisbon trip in November under 900 euro."));
        view.addMessage(ChatMessage.assistant(
                "Sure! Day 1: Alfama walking tour + fado dinner. "
                        + "Day 2: Belem + LX Factory. Day 3: Sintra day trip. "
                        + "Estimated total: 820 euro (flights from Madrid)."));
        view.addMessage(ChatMessage.user("Any vegetarian dinner spots?"));
        view.addMessage(ChatMessage.assistant(
                "Yes - in Alfama try Boi-Cavalo (modern Portuguese, "
                        + "vegan menu) and Ao 26 (vegan classics)."));
        view.setTypingIndicatorVisible(true);

        chat.show();
        TestUtils.waitForFormTitle("Assistant", FORM_TIMEOUT_MS);

        // Let the layout finish a frame before grabbing pixels.
        TestUtils.waitFor(200);

        return saveScreenshot(chat);
    }

    private void installModernTheme() throws java.io.IOException {
        String platform = Display.getInstance().getPlatformName();
        String resourceName;
        if ("ios".equals(platform)) {
            resourceName = "/iOSModernTheme.res";
        } else if ("and".equals(platform)) {
            resourceName = "/AndroidMaterialTheme.res";
        } else {
            return;
        }
        InputStream in = openResource(resourceName);
        if (in == null) {
            return;
        }
        try {
            Resources r = Resources.open(in);
            String[] names = r.getThemeResourceNames();
            if (names == null || names.length == 0) {
                return;
            }
            UIManager.getInstance().setThemeProps(r.getTheme(names[0]));
            UIManager.getInstance().refreshTheme();
        } finally {
            Util.cleanup(in);
        }
    }

    private InputStream openResource(String resourceName) {
        InputStream in = Display.getInstance().getResourceAsStream(getClass(), resourceName);
        if (in != null) {
            return in;
        }
        return ChatViewDevGuideScreenshotTest.class.getResourceAsStream(resourceName);
    }

    private boolean saveScreenshot(Form chat) throws Exception {
        Image screenshot = Image.createImage(chat.getWidth(), chat.getHeight());
        chat.paintComponent(screenshot.getGraphics(), true);

        Storage storage = Storage.getInstance();
        if (storage.exists(STORAGE_KEY)) {
            storage.deleteStorageFile(STORAGE_KEY);
        }
        ImageIO io = ImageIO.getImageIO();
        assertNotNull(io, "PNG image support is required for the dev-guide screenshot.");
        OutputStream out = null;
        try {
            out = storage.createOutputStream(STORAGE_KEY);
            io.save(screenshot, out, ImageIO.FORMAT_PNG, 1);
        } finally {
            Util.cleanup(out);
        }
        return true;
    }
}
