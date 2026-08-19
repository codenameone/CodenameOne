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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.intents.IntentDeclaration;
import com.codename1.intents.IntentResult;
import com.codename1.intents.Intents;
import com.codename1.ui.Display;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The app intents this project declares, exercised on the device VM.
///
/// The build-time half of this feature is covered by the declarations existing at all: they
/// are what make the iOS builder emit Swift for Xcode to compile and the Android builder emit
/// shortcut resources for AAPT to accept. This is the runtime half -- the generated Java
/// registry, the coercion it wraps every parameter in, and the entity resolution behind an
/// id -- running where it actually has to work, which for ParparVM is not the same thing as
/// running in a JUnit suite. A coercion that quietly does the wrong thing under a VM whose
/// CHECKCAST is unchecked cannot be found on the desktop.
///
/// Assertion-only, no screenshot.
public class IntentsApiTest extends BaseTest {

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        try {
            // Support probes must never throw, whatever they answer. A port with no bridge
            // answers false to all of them and that is a correct answer, not a failure.
            boolean supported = Intents.areIntentsSupported();
            boolean headless = Intents.isHeadlessExecutionSupported();
            boolean voice = Intents.isVoiceInvocationSupported();
            boolean indexing = Intents.isIndexingSupported();

            List<IntentDeclaration> declared = Intents.getDeclarations();
            System.out.println("CN1SS:INFO:test=IntentsApiTest platform="
                    + Display.getInstance().getPlatformName()
                    + " supported=" + supported + " headless=" + headless
                    + " voice=" + voice + " indexing=" + indexing
                    + " declarations=" + declared.size());

            // An unknown id is answered, never thrown, on every port.
            IntentResult unknown = Intents.invoke("cn1_no_such_intent", null);
            assertBool(unknown != null, "an unknown intent id must return a result");
            assertBool(unknown.isFailed(), "an unknown intent id must report failure");

            if (declared.isEmpty()) {
                // The generated registry is installed by the bootstrap the device builders
                // splice into the stub, so the table is populated on iOS and Android and empty
                // on the ports that have no such splice. Reported rather than asserted away:
                // an empty table here is the honest state of those ports, and the assertions
                // below would be measuring nothing if they ran against it.
                System.out.println("CN1SS:INFO:test=IntentsApiTest registry=absent "
                        + "(no generated dispatcher on this port; dispatch assertions skipped)");
                return true;
            }
            System.out.println("CN1SS:INFO:test=IntentsApiTest registry=present");

            assertBool(hasDeclaration(declared, "cn1_note_count"), "cn1_note_count is declared");
            assertBool(hasDeclaration(declared, "cn1_log_note"), "cn1_log_note is declared");
            assertBool(hasDeclaration(declared, "cn1_open_note"), "cn1_open_note is declared");
            assertBool(hasDeclaration(declared, "cn1_wipe_notes"), "cn1_wipe_notes is declared");

            // A handler that takes nothing.
            IntentResult counted = Intents.invoke("cn1_note_count", null);
            assertBool(!counted.isFailed(), "cn1_note_count must run");
            assertEqual("3", String.valueOf(counted.getValue()), "counted notes");

            // The wire carries whatever the platform sent, so an int parameter arrives as text
            // and the generated coercion is what turns it into 20. The declared default is what
            // an absent optional becomes.
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("kind", "todo");
            params.put("minutes", "20");
            IntentResult logged = Intents.invoke("cn1_log_note", params);
            assertBool(!logged.isFailed(), "cn1_log_note must run with coerced values");
            assertEqual("todo/20", String.valueOf(logged.getValue()), "coerced parameters");

            Map<String, Object> defaulted = new HashMap<String, Object>();
            defaulted.put("kind", "idea");
            IntentResult fell = Intents.invoke("cn1_log_note", defaulted);
            assertBool(!fell.isFailed(), "an omitted optional must fall back to its default");
            assertEqual("idea/5", String.valueOf(fell.getValue()), "declared default applied");

            // A value outside the declared vocabulary is refused by the framework rather than
            // reaching the handler, which is the whole point of declaring one.
            Map<String, Object> bogus = new HashMap<String, Object>();
            bogus.put("kind", "not_a_kind");
            IntentResult refused = Intents.invoke("cn1_log_note", bogus);
            assertBool(refused.isFailed(), "a value outside the closed vocabulary must fail");

            // An entity parameter crosses as its id and is resolved through the BY_ID query.
            Map<String, Object> byId = new HashMap<String, Object>();
            byId.put("note", "n2");
            IntentResult opened = Intents.invoke("cn1_open_note", byId);
            assertBool(!opened.isFailed(), "an entity id must resolve through BY_ID");
            assertEqual("n2", String.valueOf(opened.getValue()), "resolved entity");

            // And an id that names nothing is a failure, not a handler call with null.
            Map<String, Object> missing = new HashMap<String, Object>();
            missing.put("note", "no_such_note");
            IntentResult gone = Intents.invoke("cn1_open_note", missing);
            assertBool(gone.isFailed(), "an unresolvable entity id must fail");

            return true;
        } catch (Throwable t) {
            System.out.println("CN1SS:INFO:test=IntentsApiTest threw " + t);
            t.printStackTrace();
            return false;
        }
    }

    private static boolean hasDeclaration(List<IntentDeclaration> all, String id) {
        for (int i = 0; i < all.size(); i++) {
            if (id.equals(all.get(i).getId())) {
                return true;
            }
        }
        return false;
    }
}
