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
package com.codename1.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The generated packet-tunnel extension.
///
/// This target is unlike the other generated extensions in one way that
/// matters: it HOSTS A VM and runs the application's own Java. So the checks
/// here are about the three things that make that work and fail silently when
/// they do not -- the VM being initialised, the writer being installed before
/// the tunnel can forward anything, and the read being re-armed.
class VpnTunnelExtensionTest {

    private static String provider() {
        return IOSVpnTunnelExtensionBuilder.providerSource(
                "com.example.MyTunnel", true);
    }

    private static String text(Map<String, byte[]> files, String name)
            throws Exception {
        return new String(files.get(name), "UTF-8");
    }

    /// The generated source with its comment lines removed.
    ///
    /// The checks below look for constructs that must not appear in the
    /// generated code, and the comments explaining why they must not appear
    /// name them -- so an assertion run over the raw text fails on the note
    /// describing the bug it is guarding against.
    private static String code(String src) {
        StringBuilder sb = new StringBuilder();
        String[] lines = src.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].trim().startsWith("//")) {
                sb.append(lines[i]).append('\n');
            }
        }
        return sb.toString();
    }

    @Test
    void aFaultBecomesAJavaExceptionHereToo() {
        String src = provider();
        // ParparVM leans on a SIGSEGV handler: a field read through null
        // faults rather than checking unless ios.fieldNullChecks is on, and
        // a call through null faults in every configuration. The app target
        // takes that from installSignalHandlers in
        // CodenameOne_GLAppDelegate.m -- a UIApplication delegate an
        // extension may not compile -- and CN1WatchRuntime.m mirrors it for
        // the same reason. Without it a null dereference in the tunnel's own
        // code kills the extension instead of reaching the Throwable the
        // host already catches, and iOS takes the VPN down with it.
        assertTrue(src.contains("static void cn1tnSignalHandler(int sig) {"),
                "the extension carries its own handler");
        assertTrue(src.contains("__NEW_INSTANCE_java_lang_NullPointerException("),
                "a bad access becomes a NullPointerException");
        assertTrue(src.contains("signal(SIGSEGV, cn1tnSignalHandler);"));
        // Installed with the VM, once, before any java runs.
        int install = src.indexOf("cn1tnInstallSignalHandlers();");
        int pool = src.indexOf("initConstantPool();");
        assertTrue(pool > 0 && install > pool,
                "installed inside the one-time bootstrap");
        assertTrue(src.contains("#include <signal.h>"));

        // And omitted for the hint that comments the app's call out, so a
        // developer who wants the fault to stay a fault gets that here.
        String off = IOSVpnTunnelExtensionBuilder.providerSource(
                "com.example.MyTunnel", false);
        assertFalse(off.contains("cn1tnSignalHandler"));
        assertFalse(off.contains("#include <signal.h>"));
        assertFalse(off.contains("__NEW_INSTANCE_java_lang_RuntimeException"),
                "nothing left declaring what is not generated");
    }

    @Test
    void aConstructorThatThrowsFailsTheStart() {
        String src = provider();
        // throwException with no handler on the thread RETURNS -- it records
        // the exception and lets the caller carry on -- and a call made from
        // here has no java try region around it. So a tunnel whose
        // constructor or class initializer threw went on to begin() as a
        // half-built object, and the start was reported a success.
        int made = src.indexOf("JAVA_OBJECT tunnel = __NEW_");
        int began = src.indexOf("_begin___java_lang_Object");
        assertTrue(made > 0 && began > made);
        String between = src.substring(made, began);
        assertTrue(between.contains("threadStateData->exception != JAVA_NULL"),
                "the constructor's failure has to be seen");
        assertTrue(between.contains("tunnel == JAVA_NULL"),
                "an allocation that failed as well");
        // Cleared, or the next thing this thread runs finds an exception
        // from a start that is over.
        assertTrue(between.contains("threadStateData->exception = JAVA_NULL;"));
        assertTrue(between.contains("The tunnel class could not be constructed"),
                "and the start is failed, not reported up");
    }

    @Test
    void theVmIsStartedBeforeAnyJavaRuns() {
        String src = provider();
        assertTrue(src.contains("initConstantPool()"),
                "the extension has no UIApplicationMain to do this for it");
        assertTrue(src.contains("dispatch_once"),
                "start and stop repeat within one process; initialising twice"
                + " would reset every static the tunnel holds");
    }

    @Test
    void aStartOvertakenBeforePublishingKeepsItsHandsOff() {
        String src = provider();
        // Claiming and publishing are two steps. A start suspended between
        // them resumed long after a stop and a restart had been and gone,
        // and put its own stopped provider in the slot over the one the
        // running tunnel had published -- and the writer, which only asked
        // whether the tunnel was current, sent that tunnel's packets out on
        // a link already down.
        assertTrue(src.contains("if (cn1tnStart == atomic_load(&cn1tnReadGeneration)) {\n"
                        + "            cn1tnProvider = self;"),
                "a start that lost its claim publishes nothing");
        assertTrue(src.contains("flow = cn1tnProviderGeneration == generation\n"
                        + "                ? [cn1tnProvider retain] : nil;"),
                "and the writer takes only the provider its own start published");
    }

    @Test
    void theWriterIsInstalledBeforeTheTunnelIsConstructed() {
        String src = provider();
        int install = src.indexOf("IOSExtensionTunnel_install___int(");
        // The CONSTRUCTION, not the extern that declares the allocator: the
        // extern necessarily comes first, and matching it made this compare
        // the wrong two positions and pass whatever the order really was.
        int construct = src.indexOf("JAVA_OBJECT tunnel = __NEW_");
        assertTrue(install >= 0, "the writer has to be installed");
        assertTrue(construct >= 0, "the app's tunnel has to be constructed");
        assertTrue(install < construct,
                "onStart may forward a packet, and a forward before the"
                + " writer is installed is dropped with nothing to say so");
    }

    @Test
    void theReadIsReArmedFromInsideItsOwnHandler() {
        String src = provider();
        // Twice: the initial arm after the settings are applied, and again
        // inside the completion handler. readPacketsWithCompletionHandler
        // delivers ONE batch, so an extension that does not ask again stops
        // receiving traffic and looks like a tunnel that hung.
        int first = src.indexOf("cn1ReadPackets");
        assertTrue(first >= 0);
        assertTrue(src.indexOf("cn1ReadPackets", first + 1) > 0,
                "the handler must arm the next batch");
    }

    @Test
    void aStartThatFailsGivesUpTheGlobal() {
        String src = provider();
        // A start that fails is torn down WITHOUT stopTunnelWithReason --
        // the tunnel never started, so there is nothing for NE to stop --
        // and the global went on naming the provider the start had
        // published. Without ARC that is a bare pointer to an object the
        // system may dispose, and the writer retains what it finds there
        // before it looks at any generation, so the retain landed on freed
        // memory.
        assertTrue(src.contains("- (void)cn1ForgetIfCurrent:(int)generation {"),
                "a failed start has to give up its claim");
        // The SLOT's generation, not the counter's. A replacement start
        // claims before it publishes, and in that window the counter
        // answers for a provider that is not in the slot yet -- so a stop
        // that asked it declined to clear and left its own provider named
        // by the global for NE to release under the next writer.
        assertTrue(src.contains("static int cn1tnProviderGeneration = 0;"),
                "the slot carries the start that filled it");
        assertTrue(src.contains("            cn1tnProvider = self;\n"
                        + "            cn1tnProviderGeneration = cn1tnStart;"),
                "published together, under the one lock");
        assertTrue(src.contains("if (cn1tnProvider == self\n"
                        + "                && cn1tnProviderGeneration == generation) {"),
                "and tested together");
        assertTrue(src.contains("if (cn1tnProvider == self"),
                "and clear only its own, never a newer start's");
        // Every failure path, and each one before its handler: NE can
        // dispose the provider inside that call, and clearing after it
        // would be the same use-after-free one line further on.
        // The GENERATION as well as the pointer: NE may hand a restart
        // to the same provider object, and then identity alone says yes to
        // a stale completion -- clearing the slot the newer start published
        // under the same self, so the running tunnel wrote through nil and
        // dropped every packet.
        assertTrue(src.contains("if (cn1tnProvider == self\n"
                        + "                && cn1tnProviderGeneration == generation) {"),
                "the claim is this start's, not just this object's");
        assertTrue(src.contains("[self cn1ForgetIfCurrent:cn1tnStart];"));
        int forgets = 0;
        int at = src.indexOf("[self cn1ForgetIfCurrent:");
        while (at >= 0) {
            forgets++;
            // From the end of THIS call, whatever it passes: the stop
            // clears by the generation it is ending, the failure paths by
            // the one they started.
            String after = src.substring(src.indexOf(';', at) + 1);
            assertTrue(after.trim().startsWith("completionHandler"),
                "the clear comes before the handler it precedes");
            at = src.indexOf("[self cn1ForgetIfCurrent:", at + 1);
        }
        // Unreadable setup, a settings error, stopped while the settings
        // were pending, and a begin that refused.
        // Five failure paths -- unreadable setup, a settings error,
        // stopped while the settings were pending, a constructor that threw
        // and a begin that refused -- and the stop.
        assertEquals(6, forgets,
                "every failure path after the publish, and the stop");
    }

    @Test
    void aStopWhileStartingCannotBringTheTunnelUpAnyway() {
        String src = provider();
        // NE stops a provider whose start is still in flight -- a user
        // toggling the switch back is enough. The settings completion used
        // to run regardless: it built the tunnel, told Java to begin, armed
        // a read and reported success, all after the stop, leaving a tunnel
        // running with cn1tnProvider nil and every forwarded packet dropped.
        int claimed = src.indexOf("int cn1tnStart =");
        int settings = src.indexOf("setTunnelNetworkSettings");
        assertTrue(claimed >= 0 && claimed < settings,
                "the start has to be claimed before anything asynchronous");
        assertTrue(src.contains("cn1tnStart != atomic_load(&cn1tnReadGeneration)"),
                "the completion has to abandon a start that was stopped");
        // The handler is still called exactly once, which NE requires.
        assertTrue(src.contains("The tunnel was stopped before it started"));
        // Armed with the generation this start claimed. Bumping again there
        // would invalidate the very start the completion belongs to.
        assertTrue(src.contains("[self cn1ReadPacketsForGeneration:cn1tnStart]"));
    }

    @Test
    void aStartBeginRefusesArmsNoRead() {
        String src = provider();
        // begin() is where the decision is made -- under the lock that
        // publishes the host -- and the check at the top of the completion
        // is only a first pass: a stop and a restart can land between them.
        // The caller used to ignore the answer and arm a read regardless,
        // putting a second reader on the flow for a tunnel that does not
        // exist, which can take a batch the live tunnel was owed and drop it
        // at its own generation check.
        assertTrue(src.contains("JAVA_BOOLEAN cn1tnBegan =")
                && src.contains("begin___java_lang_Object_java_lang_String_int_R_boolean"),
                "the caller has to take begin's answer");
        int refused = src.indexOf("if (!cn1tnBegan) {");
        int armed = src.indexOf("[self cn1ReadPacketsForGeneration:cn1tnStart]");
        assertTrue(refused > 0 && armed > refused);
        // NE requires the handler exactly once, so the refusal reports an
        // error rather than returning silently.
        String branch = src.substring(refused, armed);
        assertTrue(branch.contains("completionHandler([NSError"),
                "the handler is still called exactly once");
        assertTrue(branch.contains("return;"),
                "and nothing is armed after it");
    }

    @Test
    void aStopDuringDeliveryDoesNotLeaveAReadArmed() {
        String src = provider();
        // The handler checks its generation on entry and again before
        // re-arming. Without the second check a stop that lands while the
        // batch is being handed to Java left a read for a dead tunnel
        // outstanding on the flow, and the next start's first batch could go
        // to it and be dropped -- packets lost exactly when a tunnel comes
        // up.
        int body = src.indexOf("- (void)cn1ReadPacketsForGeneration:");
        assertTrue(body >= 0);
        // BOUNDED to this method. The writer carries the same check, and an
        // unbounded scan counted it too -- so the assertion below would have
        // been satisfied by a guard somewhere else entirely.
        int end = src.indexOf("- (void)stopTunnelWithReason:", body);
        assertTrue(end > body);
        String method = src.substring(body, end);
        int checks = 0;
        int at = method.indexOf("generation != atomic_load(&cn1tnReadGeneration)");
        while (at >= 0) {
            checks++;
            at = method.indexOf(
                    "generation != atomic_load(&cn1tnReadGeneration)", at + 1);
        }
        // THREE: on entry, before each packet, and before re-arming.
        // The middle one is what stops a batch captured on the old link
        // being pumped into a tunnel that started while it was being
        // delivered.
        assertEquals(3, checks,
                "entry, every packet and the re-arm all have to check");
    }

    @Test
    void aCanceledStartCannotStillRunTheApplication() {
        String src = provider();
        // end() used to reset the Java watermark to zero, which read as
        // "nothing has started" -- so a settings completion that had already
        // passed its own generation check and then lost the race to the stop
        // sailed through the guard in begin(), installed a host and ran the
        // application's onStart for a tunnel that was already over. No onStop
        // could follow it: the stop it belonged to had been and gone.
        int stop = src.indexOf("- (void)stopTunnelWithReason:");
        assertTrue(stop >= 0);
        String method = src.substring(stop);
        int bump = method.indexOf("atomic_compare_exchange_strong(");
        int ended = method.indexOf("ExtensionTunnelHost_end___int_int");
        assertTrue(bump >= 0 && ended > bump,
                "the stop has to invalidate the generation it ends");
        // The counter AS THE STOP LEFT IT, which is one past every start
        // that can still be in flight, so begin() rejects them all on the
        // comparison it already makes.
        assertTrue(method.substring(ended).startsWith(
                "ExtensionTunnelHost_end___int_int(\n"
                + "            threadStateData, cn1tnReason(reason),\n"
                + "            cn1tnEnded);"));
        // CAPTURED at the bump, not loaded again. A stop preempted between
        // the two handed itself the restart's generation, and tore down the
        // tunnel that had replaced it.
        // THIS PROVIDER's generation, taken from the object rather
        // than from the counter. A replacement that has already claimed and
        // published leaves the counter reading its own number, and a stop
        // that took it invalidated the live tunnel's reads and told Java to
        // tear down the host that had replaced its own.
        assertTrue(method.contains("cn1tnEnding = cn1tnMine;"));
        // Invalidated only while the counter is still this start's. An
        // overtaken stop invalidates nothing: the start that overtook it
        // already did, for itself.
        assertTrue(method.contains("atomic_compare_exchange_strong(\n"
                + "                    &cn1tnReadGeneration, &cn1tnExpected,\n"
                + "                    cn1tnEnding + 1)"));
        assertTrue(src.contains("cn1tnMine = cn1tnStart;"));
        // The generation being ENDED clears the slot, and the watermark it
        // leaves goes to Java. Passing the watermark to both compared N + 1
        // against a slot holding N, so an ordinary stop cleared nothing and
        // left the global naming a provider NE was about to release.
        assertTrue(method.contains("[self cn1ForgetIfCurrent:cn1tnEnding];"));
    }

    @Test
    void theWriterHoldsWhatItWritesThrough() {
        String src = provider();
        // WITHOUT ARC -- the translated sources cannot be built any other
        // way -- the global is a bare pointer, and the stop clears it just
        // before NE releases the provider. A write that overlapped a stop
        // therefore snapshotted a pointer, passed its generation check and
        // reached packetFlow on an object that had been deallocated in
        // between. The generation check picked the right tunnel; nothing
        // kept that tunnel alive.
        assertTrue(src.contains("? [cn1tnProvider retain] : nil;"),
                "the writer has to hold the provider it writes through");
        assertTrue(src.contains("- (void)cn1ForgetIfCurrent:(int)generation {\n"
                        + "    @synchronized ([CN1VpnTunnelProvider class]) {"),
                "the stop has to clear under the lock the retain is taken under");
        // BALANCED: every path out of the writer gives the retain back.
        int writer = src.indexOf("IOSExtensionTunnel_writeNative");
        assertTrue(writer >= 0);
        String method = src.substring(writer);
        int retains = 0;
        int at = method.indexOf("[cn1tnProvider retain]");
        while (at >= 0) {
            retains++;
            at = method.indexOf("[cn1tnProvider retain]", at + 1);
        }
        int releases = 0;
        at = method.indexOf("[flow release]");
        while (at >= 0) {
            releases++;
            at = method.indexOf("[flow release]", at + 1);
        }
        assertEquals(1, retains);
        // THREE returns: nothing to write, a generation that has moved, and
        // the write itself.
        assertEquals(3, releases,
                "every path out of the writer releases");
    }

    @Test
    void anOldTunnelCannotWriteOntoTheNewLink() {
        String src = provider();
        // A stopped tunnel's onPacket can still be running -- a callback
        // cannot be retracted, and the inbound checks only stop packets
        // BEFORE they enter Java. ExtensionTunnelHost.end clears the host
        // and the transport but not the writer, so a late forward reached
        // whatever provider was current: one session's packet leaving on
        // another's link.
        assertTrue(src.contains("JAVA_INT generation"),
                "the writer takes the generation it was installed for");
        assertTrue(src.contains("generation != atomic_load(&cn1tnReadGeneration)"),
                "...and refuses a write from a start that is over");
        // Installed PER START, with the generation this start claimed.
        assertTrue(src.contains(
                "IOSExtensionTunnel_install___int(\n                threadStateData, cn1tnStart)"));
    }

    @Test
    void packetsGoIntoThePooledBuffer() {
        // An allocation and a second copy per packet, at line rate, in a
        // process with a hard memory cap -- in an API whose buffers are
        // pooled to avoid precisely that.
        String src = provider();
        assertFalse(src.contains("__NEW_ARRAY_JAVA_BYTE"),
                "no per-packet Java array");
        assertTrue(src.contains("ExtensionTunnelHost_buffer___int"),
                "the pooled buffer is asked for instead");
        assertTrue(src.contains("ExtensionTunnelHost_received___int"),
                "and told how much was written");
    }

    @Test
    void aNestedTunnelReachesTheSymbolsTheTranslationDefines() {
        // ParparVM mangles '.', '/' and '$' to '_'. Replacing only '.' was
        // right for every name anyone had tried and wrong for a nested
        // tunnel: com.example.Outer$Tunnel is a legal value for
        // ios.vpn.tunnel.class, and the provider then declared
        // __NEW_com_example_Outer$Tunnel -- a symbol the translation never
        // defines, so the extension failed at link.
        String src = IOSVpnTunnelExtensionBuilder.providerSource(
                "com.example.Outer$Tunnel", true);
        assertTrue(src.contains("__NEW_com_example_Outer_Tunnel("));
        assertTrue(src.contains("com_example_Outer_Tunnel___INIT____"));
        assertFalse(src.contains("Outer$Tunnel"),
                "no '$' can survive into a C symbol");
    }

    @Test
    void anEmptyRouteListProducesNoRoutes() {
        String src = provider();
        // componentsSeparatedByString returns ONE empty item for @"", and a
        // setup with no routes of a family is ordinary -- a v6-only tunnel
        // passes @"" to the v4 helper. Without the skip that helper built an
        // NEIPv4Route whose destination was the empty string, and iOS
        // refused the whole settings object: a valid setup that would not
        // start. The v6 helper never showed it, because its own family test
        // skips an empty entry for having no colon in it.
        int helpers = 0;
        int at = src.indexOf("[[items objectAtIndex:i] length] == 0");
        while (at >= 0) {
            helpers++;
            at = src.indexOf("[[items objectAtIndex:i] length] == 0", at + 1);
        }
        assertEquals(2, helpers,
                "both route helpers have to skip an empty entry");
    }

    @Test
    void theTunnelIsAllocatedThroughTheAbiParparvmEmits() {
        String src = provider();
        // __NEW_X takes the thread state -- it uses it for class
        // initialisation and for the GC allocation -- and an EMPTY parameter
        // list is an old-style declaration that compiles and then leaves the
        // argument register unset on arm64.
        assertTrue(src.contains("extern JAVA_OBJECT __NEW_com_example_MyTunnel("
                + "CODENAME_ONE_THREAD_STATE);"),
                "the allocator has to be declared with the thread state");
        assertTrue(src.contains("__NEW_com_example_MyTunnel(threadStateData)"),
                "...and called with it");
        // The no-argument constructor is X___INIT____. X_ctor__ is a symbol
        // the translation never defines, so the extension did not link.
        assertTrue(src.contains(
                "com_example_MyTunnel___INIT____(threadStateData, tunnel)"));
        // No CALL or declaration of it. The generated file names the old
        // spelling once, in the comment that explains why it is not used.
        assertFalse(src.contains("_ctor__("),
                "_ctor__ is not a symbol ParparVM emits");
    }

    @Test
    void theTunnelClassIsNamedRatherThanLookedUp() {
        // Class.forName would not survive obfuscation, which is why the
        // framework bans it -- so the class is baked in as a symbol at
        // build time.
        String src = provider();
        assertTrue(src.contains("com_example_MyTunnel"),
                "the tunnel is reached as a translated symbol");
        // A CALL, not the word. The comment above the extern explains why
        // reflection is not used here and names Class.forName doing it, so a
        // substring test for the name matches the explanation and reports
        // the opposite of what it meant to check.
        assertFalse(src.contains("forName("),
                "a name looked up at run time would be gone by then");
    }

    @Test
    void settingsAreAppliedBeforePacketsAreRead() {
        String src = provider();
        int settings = src.indexOf("setTunnelNetworkSettings");
        int read = src.indexOf("cn1ReadPackets");
        assertTrue(settings >= 0 && read > settings,
                "reading before the settings land returns nothing, for ever,"
                + " with no error");
    }

    @Test
    void anIpv6SetupRoutesTraffic() {
        // Addresses establish the interface and route nothing, so a v6
        // tunnel that assigned only addresses came up carrying nothing --
        // including one that asked for the default route.
        String src = provider();
        assertTrue(src.contains("v6s.includedRoutes"),
                "the v6 branch has to install routes, like the v4 one");
        assertTrue(src.contains("NEIPv6Route"),
                "v6 routes are their own class; the v4 helper cannot make"
                + " them");
    }

    @Test
    void aReadArmedByAnEarlierStartCannotFeedTheNextTunnel() {
        // The extension process outlives a tunnel -- NE stops and starts this
        // provider without tearing the process down -- so a read armed by one
        // start can complete after it. The handler delivered whatever it got
        // and re-armed itself unconditionally, guarded only by a null buffer
        // meaning "no tunnel running", which stops nothing once a NEW tunnel
        // is running. A stop followed by a start therefore fed packets
        // captured on the old link into the new tunnel, and left two readers
        // competing for one flow for the rest of the process.
        String src = code(provider());
        assertTrue(src.contains("cn1ReadPacketsForGeneration:"),
                "the read has to carry the start it belongs to");
        assertFalse(src.contains("[self cn1ReadPackets]"),
                "and the ungeneration-ed entry point must be gone, not merely"
                + " unused -- it is the one a later edit would reach for");

        int handler = src.indexOf("readPacketsWithCompletionHandler:");
        assertTrue(handler >= 0, "the read handler has to exist");
        String body = src.substring(handler);
        int guard = body.indexOf(
                "if (generation != atomic_load(&cn1tnReadGeneration))");
        int deliver = body.indexOf("ExtensionTunnelHost_received___int");
        int rearm = body.indexOf("cn1ReadPacketsForGeneration:generation");
        assertTrue(guard >= 0, "the handler has to check its generation");
        assertTrue(guard < deliver,
                "before delivering, or a stale batch reaches the new tunnel");
        assertTrue(guard < rearm,
                "and before re-arming, or the stale reader lives for ever");

        // Moved on BOTH sides. Only on start, a read outstanding across a
        // long stop would still be live when the next start bumped it -- but
        // nothing would have stopped it in between, and it would deliver into
        // whatever the process did next.
        int start = src.indexOf("startTunnelWithOptions");
        int stop = src.indexOf("stopTunnelWithReason");
        assertTrue(start >= 0 && stop > start);
        assertTrue(src.substring(start, stop)
                        .contains("atomic_fetch_add(&cn1tnReadGeneration, 1)"),
                "a start claims its own generation");
        // CONDITIONALLY on the stop side: a stop that has been overtaken
        // invalidates nothing, because the start that overtook it already
        // did, for itself -- and bumping there would have invalidated the
        // live tunnel's reads instead of its own.
        assertTrue(src.substring(stop)
                        .contains("atomic_compare_exchange_strong("),
                "and a stop invalidates the one it is ending");
    }

    @Test
    void anEmptyRouteListNeverBecomesTheDefaultRoute() {
        // Two ways a list ends up empty, and now one answer. A setup listing
        // only v4 routes on a v6 interface asked for no v6 traffic; a setup
        // naming no routes at all asked for none either. Inventing the
        // default route for either captures every packet on the device --
        // the opposite of the request, and the one direction where guessing
        // wrong cannot be undone by the app.
        //
        // The filtered case was fixed first and the empty-input case was
        // deliberately left defaulting, on the reasoning that a tunnel
        // carrying nothing is useless so an app naming no routes must have
        // meant all of them. That reasoning is refuted by both the API and
        // the other port: TunnelSetup.route documents the full tunnel as an
        // explicit 0.0.0.0/0 or ::/0, never as an absence, and Android's
        // CN1VpnService adds exactly the routes it was given with no
        // fallback -- so one setup carried nothing there and everything
        // here.
        String src = code(provider());
        assertFalse(src.contains("defaultRoute"),
                "neither helper may invent a route the setup did not ask"
                + " for");
        for (String helper : new String[] {"cn1tnRoutes(NSString",
                "cn1tnRoutes6(NSString"}) {
            int at = src.indexOf(helper);
            assertTrue(at >= 0, helper + " has to exist");
            String body = src.substring(at, src.indexOf("return out;", at));
            assertFalse(body.contains("[list length] == 0"),
                    helper + " may not special-case an empty list: an empty"
                    + " list is already the right answer");
        }
    }

    @Test
    void bothRouteHelpersFilterTheOtherFamily() {
        // The v6 helper skipped v4 entries and the v4 helper skipped
        // nothing, so address("10.0.0.2/32").route("::/0") built an
        // NEIPv4Route whose destination was "::" and whose mask was a dotted
        // quad. That is not a route the system can install, and it does not
        // fail alone -- the whole NEPacketTunnelNetworkSettings object is
        // rejected, so a setup naming one v6 route brought down the v4 half
        // with it.
        //
        // Asserted as a pair rather than on the v4 helper alone: the defect
        // was the ASYMMETRY, and a filter added to one side is exactly what
        // produced it.
        String src = provider();
        for (String helper : new String[] {"cn1tnRoutes(NSString",
                "cn1tnRoutes6(NSString"}) {
            int at = src.indexOf(helper);
            assertTrue(at >= 0, helper + " has to exist");
            String body = src.substring(at, src.indexOf("return out;", at));
            assertTrue(body.contains("rangeOfString:@\":\"")
                    && body.contains("continue;"),
                    helper + " has to skip entries of the other family,"
                    + " not build a route class for them");
        }
    }

    @Test
    void theHelpersAreDeclaredBeforeTheyAreCalled() {
        // The implementation calls cn1tnSettings and cn1tnReason and their
        // definitions follow @end, which reads well and does not compile:
        // C99 removed implicit declarations and current clang makes that an
        // error, so the generated target failed on its own first build.
        //
        // Nothing in this repository compiles this file -- it is written
        // here and built by Xcode on a machine none of our tests run on --
        // which is exactly why a break like this sat here unseen. Checked
        // by generating the provider and running clang against the real iOS
        // SDK; that cannot run in a unit test, so this holds the property
        // the fix established.
        String src = provider();
        int impl = src.indexOf("@implementation");
        assertTrue(impl > 0, "the provider has an implementation");
        String preamble = src.substring(0, impl);
        for (String helper : new String[] {"cn1tnSettings", "cn1tnReason"}) {
            assertTrue(preamble.contains(helper),
                    helper + " is called from the implementation, so it has"
                    + " to be declared above it: " + preamble);
        }
    }

    @Test
    void routesOfBothFamiliesReachTheLink() {
        // The address decides which family carries the interface and the
        // route helpers drop entries of the other, so
        // address("10.0.0.2/32").route("0.0.0.0/0").route("::/0") built v4
        // settings, discarded the v6 route, and reported the tunnel
        // connected while v6 traffic went around it -- a full tunnel
        // carrying half the traffic.
        String src = provider();
        int at = src.indexOf("The OTHER family");
        assertTrue(at > 0, "the other family has to be considered at all");
        String block = src.substring(at, src.indexOf("NSString *dns", at));
        assertTrue(block.contains("cn1tnRoutes6(cn1tnField(f, 2))")
                        && block.contains("cn1tnRoutes(cn1tnField(f, 2))"),
                "both helpers are consulted, whichever family the address is");
        assertTrue(block.contains("[cn1tnField(f, 2) length] > 0"),
                "and only when routes were NAMED -- an empty list means the"
                + " default route, which belongs to the family that has the"
                + " address");
    }

    @Test
    void searchDomainsReachTheLink() {
        // TunnelSetup documents iOS applying these, and field 4 was carried
        // across the wire and then never read -- so a short hostname that
        // resolved on Android did not here.
        String src = provider();
        assertTrue(src.contains("searchDomains"),
                "the documented behaviour has to be the implemented one");
        assertTrue(src.contains("cn1tnField(f, 4)"),
                "field 4 is where the wire puts them");
    }

    @Test
    void theDefaultRouteSurvivesTheMaskHelper() {
        // /0 is what a full-tunnel VPN asks for and what the documentation
        // shows. Folding zero into 32 gave 255.255.255.255, so the extension
        // installed a host route, started successfully, and carried almost
        // nothing -- the same bug the Android parser had, in the other
        // language.
        String src = provider();
        assertFalse(src.contains("if (bits <= 0 || bits > 32)"),
                "zero is a valid prefix, and the important one");
        assertTrue(src.contains("if (bits < 0 || bits > 32)"),
                "only a negative or oversized prefix is unusable");
    }

    @Test
    void theSuppliedIpv6PrefixIsUsed() {
        // fd00::2/64 was parsed and then discarded for a hardcoded 128, so
        // the interface did not match the requested subnet and what onStart
        // reported was not what iOS installed.
        //
        // Asserted on the ARGUMENT rather than on the local's name: the
        // first version of this checked for "v6bits", which broke the moment
        // the two families started sharing one parse and said nothing about
        // whether the value still reached the settings object.
        String src = provider();
        int v6 = src.indexOf("NEIPv6Settings *v6s = [[[NEIPv6Settings alloc]");
        assertTrue(v6 >= 0, "the v6 settings have to be built");
        String block = src.substring(v6, src.indexOf("autorelease];", v6));
        assertTrue(block.contains("networkPrefixLengths:"),
                "the prefix length is what this is about");
        assertFalse(block.matches("(?s).*numberWithInt:\\s*\\d.*"),
                "a literal there is the hardcoded 128 coming back;"
                + " the parsed prefix has to reach NEIPv6Settings");
    }

    @Test
    void theInfoPlistDeclaresAPacketTunnel() throws Exception {
        Map<String, byte[]> files = IOSVpnTunnelExtensionBuilder.buildFileMap(
                "com.example.app", "My VPN", "1.0", "17",
                "com.example.MyTunnel", true);
        String plist = text(files, "Info.plist");
        assertTrue(plist.contains(
                "com.apple.networkextension.packet-tunnel"),
                "a bundle whose extension point does not match is never"
                + " started, and nothing reports that it was not");
        assertTrue(plist.contains("CN1VpnTunnelProvider"),
                "iOS instantiates the principal class directly");
        assertTrue(plist.contains("$(EXECUTABLE_NAME)"),
                "a project may rename the product; the plist has to follow");
    }

    @Test
    void theExtensionCarriesTheEntitlementThatMakesItATunnel() throws Exception {
        Map<String, byte[]> files = IOSVpnTunnelExtensionBuilder.buildFileMap(
                "com.example.app", "My VPN", "1.0", "17",
                "com.example.MyTunnel", true);
        String ent = text(files, "CN1VpnTunnel.entitlements");
        assertTrue(ent.contains(
                "com.apple.developer.networking.networkextension"),
                "without it the extension is never started");
        assertTrue(ent.contains("packet-tunnel-provider"),
                "and this is the value that says which kind it is");
        // An ARRAY, not a string: this key is array-valued, and a string
        // either fails codesigning or is dropped.
        assertTrue(ent.contains("<array>"),
                "the key is array-valued");
    }

    @Test
    void anUnreadablePrefixIsRefusedRatherThanReadAsZero() {
        // NSString's intValue reads "foo" as 0, and 0 is meaningful here:
        // /0 is the default route. So route("10.0.0.0/foo") did not fail --
        // it installed a route over ALL traffic, which is the opposite of
        // the single subnet it named, and the tunnel came up reporting
        // success. The interface address had the same coercion.
        //
        // Asserted as the ABSENCE of the coercion primitive from the three
        // places that read a prefix, rather than the presence of a
        // particular guard: any parse that goes back through intValue has
        // the defect back, whatever the code around it looks like.
        //
        // Scoped to prefixes rather than to the whole file, because the MTU
        // legitimately coerces: there zero is not a meaningful value, so an
        // unreadable one falls through to the system default, which is the
        // recoverable answer TunnelWire.mtu picks on the Java side too.
        String src = provider();
        assertTrue(src.contains("static int cn1tnBits(NSString *prefix,"
                + " int max)"),
                "the strict parser is what tells -1 from a legitimate 0");

        // Every one of the three places that reads a prefix uses it, and
        // each does the only thing it can with a refusal: a route is
        // dropped, the interface address is not droppable and fails the
        // whole settings object.
        for (String helper : new String[] {"cn1tnRoutes(NSString",
                "cn1tnRoutes6(NSString"}) {
            int at = src.indexOf(helper);
            assertTrue(at >= 0, helper + " has to exist");
            String body = code(
                    src.substring(at, src.indexOf("return out;", at)));
            assertTrue(body.contains("cn1tnBits("),
                    helper + " has to parse its prefix strictly");
            assertFalse(body.contains("intValue"),
                    helper + " may not read a prefix with NSString's"
                    + " lenient coercion");
            assertTrue(body.contains("if (bits < 0) {"),
                    helper + " has to act on a refusal, not ignore it");
        }
        int settings = src.indexOf(
                "cn1tnSettings(\n        NSString *wire) {");
        assertTrue(settings >= 0, "the settings builder has to exist");
        // Ends at the DNS block: the MTU that follows it coerces on purpose.
        String body = code(src.substring(settings,
                src.indexOf("s.DNSSettings", settings)));
        assertTrue(body.contains("cn1tnBits(") && body.contains("return nil;"),
                "an interface address cannot be dropped, so an unreadable"
                + " prefix has to fail the settings object");
        assertFalse(body.contains("intValue"),
                "and its prefix may not be read with the lenient coercion"
                + " either");
    }

    @Test
    void aFailedSettingsBuildFailsTheStartRatherThanTheLink() {
        // The extension is a separate process and Tunnels.start() returned
        // long ago, so failing the start is the whole of what it can say --
        // and it is the right thing to say. Proceeding with nil settings
        // would establish a link on a configuration nobody asked for.
        String src = provider();
        int nilCheck = src.indexOf("if (settings == nil) {");
        int apply = src.indexOf("[self setTunnelNetworkSettings:settings");
        assertTrue(nilCheck >= 0, "the nil settings case has to be handled");
        assertTrue(apply > nilCheck,
                "and handled BEFORE the settings are applied");
        assertTrue(src.substring(nilCheck, apply).contains(
                "completionHandler([NSError"),
                "the start has to fail with an error, not silently return");
    }

    @Test
    void theBundleIdIsUnderTheHostApp() {
        // An extension's identifier has to be prefixed by the host's, or the
        // App ID cannot be created and codesigning fails.
        assertEquals("com.example.app.vpntunnel",
                IOSVpnTunnelExtensionBuilder.bundleId("com.example.app"));
    }

    @Test
    void theClassSymbolIsMangledTheWayParparvmDoesIt() {
        assertEquals("com_example_MyTunnel",
                IOSVpnTunnelExtensionBuilder.mangle("com.example.MyTunnel"));
    }
}
