/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// detachHost() used to only clear the `host` reference, leaving a focused
/// component's native editor active and the detached node still reporting
/// hasFocus()/primaryFocus() -- exactly what happens when bindFocus() moves a
/// mounted text field's attachment from a focused external FocusNode to its
/// own internally-created one. Flutter's FocusAttachment.detach unfocuses a
/// primary node first; detachHost() must do the same.
class FocusNodeTest {

    private static final class RecordingHost implements FocusNode.Host {
        int focusCalls;
        int blurCalls;

        @Override
        public void focusHost() {
            focusCalls++;
        }

        @Override
        public void blurHost() {
            blurCalls++;
        }
    }

    @Test
    void detachingTheFocusedHostBlursItAndClearsFocus() {
        FocusNode node = new FocusNode();
        RecordingHost host = new RecordingHost();
        node.attachHost(host);
        node.requestFocus();
        assertTrue(node.hasFocus(), "requestFocus should have focused the node");
        assertSame(node, FocusNode.primaryFocus());
        assertEquals(1, host.focusCalls);

        node.detachHost(host);

        assertEquals(1, host.blurCalls, "the still-attached host must be blurred before severing");
        assertFalse(node.hasFocus(), "a detached node cannot still report having focus");
        assertFalse(node == FocusNode.primaryFocus(), "a detached node cannot still be the primary focus");
    }

    @Test
    void detachingAnUnfocusedHostDoesNotBlurOrNotify() {
        FocusNode node = new FocusNode();
        RecordingHost host = new RecordingHost();
        node.attachHost(host);
        final int[] notifications = {0};
        node.addListener(() -> notifications[0]++);

        node.detachHost(host);

        assertEquals(0, host.blurCalls, "an unfocused host needs no blur");
        assertEquals(0, notifications[0], "detaching an already-unfocused node is a no-op for listeners");
    }

    @Test
    void detachingTheWrongHostLeavesFocusUntouched() {
        FocusNode node = new FocusNode();
        RecordingHost host = new RecordingHost();
        RecordingHost other = new RecordingHost();
        node.attachHost(host);
        node.requestFocus();

        node.detachHost(other);

        assertEquals(0, host.blurCalls, "the real host was never asked to blur");
        assertTrue(node.hasFocus(), "a detach for a host that is not attached changes nothing");
    }

    @Test
    void bindingATextFieldToItsOwnNodeUnfocusesTheReplacedExternalNode() {
        // The scenario the review finding described: an external FocusNode holds
        // the focus and a mounted field's host, then a rebuild rebinds that same
        // host to a different FocusNode (bindFocus(null) -> detachHost, as
        // TextFieldRenderElement.unmount()/apply() do).
        FocusNode external = new FocusNode();
        RecordingHost host = new RecordingHost();
        external.attachHost(host);
        external.requestFocus();
        assertTrue(external.hasFocus());

        external.detachHost(host);
        FocusNode replacement = new FocusNode();
        replacement.attachHost(host);

        assertFalse(external.hasFocus(), "the replaced node must not still claim the focus");
        assertFalse(external == FocusNode.primaryFocus());
        assertEquals(1, host.blurCalls);
    }
}
