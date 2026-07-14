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

import com.codename1.impl.ImplementationFactory;
import com.codename1.io.Util;
import com.codename1.impl.javase.MCPStdioTransport;
import com.codename1.mcp.MCPServer;
import com.codename1.testing.SafeL10NManager;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Button;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import java.io.PrintStream;

/**
 * A minimal headless Codename One application that exposes itself over the MCP stdio
 * transport, used by scripts/mcp-inspector-e2e.sh to verify MCP spec conformance
 * against the reference MCP Inspector client. It boots the lightweight test
 * implementation (no Swing window) so it can run in headless CI.
 */
public class McpStdioDemo {
    public static void main(String[] args) throws Exception {
        // Reserve stdout for the protocol before anything else can print to it.
        PrintStream realOut = System.out;
        System.setOut(System.err);

        final TestCodenameOneImplementation impl = new TestCodenameOneImplementation();
        ImplementationFactory.setInstance(new ImplementationFactory() {
            public Object createImplementation() {
                return impl;
            }
        });
        impl.setLocalizationManager(new SafeL10NManager("en", "US"));
        Display.init(null);
        Util.setImplementation(impl);

        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                Form f = new Form("Demo Login");
                f.add(new Button("Save"));
                f.add(new TextField("hello"));
                f.show();
            }
        });
        Thread.sleep(600);

        MCPServer server = new MCPServer();
        server.setServerInfo("cn1-demo", "1.0");
        server.setScreenshotEnabled(false); // the test implementation cannot encode PNG
        server.start(new MCPStdioTransport(System.in, realOut, false));

        while (server.isRunning()) {
            Thread.sleep(100);
        }
        System.exit(0);
    }
}
