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
package com.codename1.impl.ios;

import com.codename1.call.spi.CallBridge;
import com.codename1.io.FileSystemStorage;

/// The iOS half of `com.codename1.call`, on CallKit, PushKit and the Call
/// Directory extension.
///
/// A thin forwarder. Everything of substance lives in `CN1Call.m`, because
/// the deadline this API is built around cannot be met from Java: iOS
/// terminates the app if a VoIP push handler returns without reporting the
/// call to CallKit, and reporting it from here would mean a round trip
/// through the EDT -- which at launch has an unbounded queue in front of it
/// and can be blocked on the main thread the push arrived on.
///
/// So native code reports the call, and this class is how the application
/// enriches one that is already ringing.
class IOSCallBridge implements CallBridge {

    private final IOSNative nativeInstance;

    IOSCallBridge(IOSNative nativeInstance) {
        this.nativeInstance = nativeInstance;
        // Touch the callback class so the VM optimizer keeps its entry points,
        // for the reason IOSNearbyCallbacks documents at length: a native
        // callback target nothing in Java calls is replaced with an empty stub
        // and every operation then hangs waiting for an answer that was
        // compiled away.
        IOSCallCallbacks.install(nativeInstance);
    }

    @Override
    public boolean isCallSupported() {
        return nativeInstance.callSupported();
    }

    @Override
    public boolean isVoipPushSupported() {
        return nativeInstance.callVoipSupported();
    }

    @Override
    public boolean isDirectorySupported() {
        return nativeInstance.callDirectorySupported();
    }

    @Override
    public int getCallCapabilities() {
        return nativeInstance.callCapabilities();
    }

    @Override
    public int getCallAvailability() {
        return nativeInstance.callAvailability();
    }

    @Override
    public int getGrantedPermissions() {
        return nativeInstance.callGrantedPermissions();
    }

    @Override
    public void requestPermissions(int requestId, int permissionBits) {
        nativeInstance.callRequestPermissions(requestId, permissionBits);
    }

    @Override
    public void configureProvider(int requestId, String configWire) {
        nativeInstance.callConfigureProvider(requestId, configWire);
    }

    @Override
    public void reportIncomingCall(int requestId, String callId,
            String handleWire, String displayName, int capabilityBits,
            boolean hasVideo) {
        nativeInstance.callReportIncoming(requestId, callId, handleWire,
                displayName, hasVideo);
    }

    @Override
    public void reportOutgoingCall(int requestId, String callId,
            String handleWire, String displayName, int capabilityBits,
            boolean hasVideo) {
        nativeInstance.callReportOutgoing(requestId, callId, handleWire,
                displayName, hasVideo);
    }

    @Override
    public void reportOutgoingStartedConnecting(String callId, long timestampMs) {
        nativeInstance.callStartedConnecting(callId, timestampMs);
    }

    @Override
    public void reportOutgoingConnected(String callId, long timestampMs) {
        nativeInstance.callOutgoingConnected(callId, timestampMs);
    }

    @Override
    public void reportIncomingConnected(String callId, long timestampMs) {
        nativeInstance.callIncomingConnected(callId, timestampMs);
    }

    @Override
    public void updateCall(String callId, String handleWire, String displayName,
            int capabilityBits, boolean hasVideo) {
        nativeInstance.callUpdate(callId, handleWire, displayName, hasVideo);
    }

    @Override
    public void reportCallEnded(String callId, int endReasonOrdinal,
            long timestampMs) {
        nativeInstance.callReportEnded(callId, endReasonOrdinal, timestampMs);
    }

    @Override
    public void endCall(int requestId, String callId, int endReasonOrdinal) {
        nativeInstance.callEnd(requestId, callId, endReasonOrdinal);
    }

    @Override
    public void setHeld(int requestId, String callId, boolean held) {
        nativeInstance.callSetHeld(requestId, callId, held);
    }

    @Override
    public void setMuted(int requestId, String callId, boolean muted) {
        nativeInstance.callSetMuted(requestId, callId, muted);
    }

    @Override
    public void sendDtmf(int requestId, String callId, String digits) {
        nativeInstance.callSendDtmf(requestId, callId, digits);
    }

    @Override
    public void setCallGroup(int requestId, String callId, String otherCallId) {
        nativeInstance.callSetGroup(requestId, callId, otherCallId);
    }

    @Override
    public int getAudioRoute() {
        return nativeInstance.callAudioRoute();
    }

    @Override
    public void setAudioRoute(int requestId, int routeOrdinal) {
        nativeInstance.callSetAudioRoute(requestId, routeOrdinal);
    }

    @Override
    public void showAudioRoutePicker(int requestId, String callId) {
        nativeInstance.callShowRoutePicker(requestId, callId);
    }

    @Override
    public boolean completeAction(long actionToken, boolean fulfilled) {
        return nativeInstance.callCompleteAction(actionToken, fulfilled);
    }

    @Override
    public void registerVoipPush(int requestId) {
        nativeInstance.callRegisterVoipPush(requestId);
    }

    @Override
    public void unregisterVoipPush(int requestId) {
        nativeInstance.callUnregisterVoipPush(requestId);
    }

    @Override
    public void setJavaReady(boolean ready) {
        nativeInstance.callSetJavaReady(ready);
    }

    @Override
    public void drainPendingCalls(int requestId) {
        nativeInstance.callDrainPendingCalls(requestId);
    }

    @Override
    public void setDirectorySource(int requestId, String filePath) {
        // Converted, not passed through. getAppHomePath() on this platform
        // answers a file:// URL -- listFilesystemRoots() puts that prefix on
        // -- and the native side hands the string to fileURLWithPath:, which
        // treats it as a literal path rather than parsing it as a URL. Every
        // setEntries therefore failed to find its own staged file.
        nativeInstance.callSetDirectorySource(requestId,
                FileSystemStorage.getInstance().toNativePath(filePath));
    }

    @Override
    public void reloadDirectory(int requestId) {
        nativeInstance.callReloadDirectory(requestId);
    }

    @Override
    public void getDirectoryStatus(int requestId) {
        nativeInstance.callDirectoryStatus(requestId);
    }

    @Override
    public void requestScreeningRole(int requestId) {
        // iOS has no equivalent to ask for. Caller identification is enabled
        // by the user in Settings, and an app cannot prompt for it -- so the
        // honest answer is that the request did not apply, and
        // DirectoryStatus.isEnabled() is what an app reads instead.
        com.codename1.call.session.Calls.deliverAck(requestId, false,
                com.codename1.call.CallError.NOT_SUPPORTED.ordinal(),
                "On iOS the user enables call directories in Settings; an app"
                + " cannot request the role");
    }
}
