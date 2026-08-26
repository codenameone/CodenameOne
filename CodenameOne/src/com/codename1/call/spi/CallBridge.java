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
package com.codename1.call.spi;

/// Internal service-provider interface implemented by each platform port to
/// carry the `com.codename1.call` API onto the native call stacks: Apple's
/// CallKit and PushKit, and Android's `ConnectionService`, `TelecomManager`
/// and `CallScreeningService`.
///
/// Application code never touches this interface. It is obtained by the
/// `com.codename1.call` packages from
/// `com.codename1.ui.Display#getCallBridge()`, and the base implementation
/// returns `null` -- which is why the public API degrades to a well-behaved
/// `NOT_SUPPORTED` on ports that implement nothing, and why application code
/// needs no platform `if` statements.
///
/// #### Everything here is primitives, strings and byte arrays
///
/// A port may be Objective-C reached through ParparVM, where constructing a
/// Java object is expensive and easy to get wrong. So no method on this
/// interface takes or returns a framework type: enums cross as their
/// ordinals, capability sets cross as bit masks, and structured records
/// cross as tab-delimited strings built by
/// `com.codename1.impl.call.CallWire`.
///
/// #### Asynchrony is by request id, and every operation must answer
///
/// Operations that can fail take a `requestId` allocated by the caller and
/// answer exactly once by calling the matching `deliver...` entry point on
/// the public class. **An operation that never answers is worse than one
/// that fails**: the caller holds an `AsyncResource` that will never settle
/// and has no way to find out. A port that cannot start something must still
/// report the failure. This bites harder here than elsewhere, because both
/// platforms have a documented "the system refused your call" path --
/// Telecom's `onCreateIncomingConnectionFailed` and the `NSError` handed to
/// CallKit's report completion -- that is easy to leave unwired, and an
/// unwired refusal looks exactly like a call that is still ringing.
///
/// Unsolicited events -- the user answering, the system taking the audio --
/// carry the call id they belong to instead of a request id. Every entry
/// point may be called from any thread; they marshal to the EDT themselves.
///
/// #### The up direction has a deadline too
///
/// A system-originated action must be answered with [#completeAction] within
/// a few seconds or the platform times it out and the system UI and the app
/// disagree about the call, silently. The facade guarantees an answer the
/// same way this interface guarantees one downward.
public interface CallBridge {

    /// [#getCallCapabilities()] bit: the platform draws a system call UI.
    int CAPABILITY_SYSTEM_UI = 1;

    /// [#getCallCapabilities()] bit: outgoing calls can be reported.
    int CAPABILITY_OUTGOING = 2;

    /// [#getCallCapabilities()] bit: calls can be held and resumed.
    int CAPABILITY_HOLD = 4;

    /// [#getCallCapabilities()] bit: the system offers a mute control.
    int CAPABILITY_MUTE = 8;

    /// [#getCallCapabilities()] bit: the system offers a keypad and
    /// delivers DTMF digits.
    int CAPABILITY_DTMF = 16;

    /// [#getCallCapabilities()] bit: calls can be grouped into a conference.
    int CAPABILITY_GROUPING = 32;

    /// [#getCallCapabilities()] bit: video calls are supported.
    int CAPABILITY_VIDEO = 64;

    /// [#getCallCapabilities()] bit: the app can be woken by a VoIP push.
    int CAPABILITY_VOIP_PUSH = 128;

    /// [#getCallCapabilities()] bit: caller identification can be installed.
    int CAPABILITY_DIRECTORY = 256;

    /// [#getCallCapabilities()] bit: incoming calls can be screened or
    /// blocked.
    int CAPABILITY_SCREENING = 512;

    /// Reserved. **No port sets this.** Neither platform has a system audio
    /// route picker an app can present for a call -- iOS offers
    /// `AVRoutePickerView`, a view the app places itself, and Android offers
    /// nothing -- so [#showAudioRoutePicker] always answers NOT_SUPPORTED.
    /// The constant is kept so the bit values do not shift if that changes.
    int CAPABILITY_ROUTE_PICKER = 1024;

    /// [#requestPermissions] bit: the grant needed to own calls --
    /// `MANAGE_OWN_CALLS` on Android. Implicit on iOS.
    int PERMISSION_MANAGE_CALLS = 1;

    /// [#requestPermissions] bit: microphone access.
    int PERMISSION_MICROPHONE = 2;

    /// [#requestPermissions] bit: camera access, for video calls.
    int PERMISSION_CAMERA = 4;

    /// [#requestPermissions] bit: permission to post notifications, which
    /// Android needs to show a call in the shade.
    int PERMISSION_NOTIFICATIONS = 8;

    /// [#requestPermissions] bit: the call-screening role.
    int PERMISSION_SCREENING_ROLE = 16;

    // ---------------------------------------------------------------
    // Queries. Synchronous, cheap, and safe to call before configuring.
    // ---------------------------------------------------------------

    /// Whether this port can report calls to a system call UI at all.
    boolean isCallSupported();

    /// Whether this port can be woken by a VoIP push.
    boolean isVoipPushSupported();

    /// Whether this port can install caller identification or blocking.
    boolean isDirectorySupported();

    /// The `CAPABILITY_*` bit mask this port supports.
    int getCallCapabilities();

    /// The ordinal of the current `com.codename1.call.CallAvailability` --
    /// whether a call could be rung right now, which is a different question
    /// from whether the platform supports calling.
    int getCallAvailability();

    /// The `PERMISSION_*` bit mask currently granted.
    int getGrantedPermissions();

    /// Requests the `PERMISSION_*` bits in `permissionBits`, answering with
    /// the granted mask.
    void requestPermissions(int requestId, int permissionBits);

    // ---------------------------------------------------------------
    // Provider registration. Must precede any report.
    // ---------------------------------------------------------------

    /// Installs the calling identity: the name the system shows, the
    /// ringtone, whether video is offered. `configWire` is a
    /// `CallWire`-encoded record.
    ///
    /// On Android this registers the `PhoneAccount`; until it has run,
    /// `TelecomManager.addNewIncomingCall` is a **silent no-op**, which is
    /// why this is a separate step rather than something inferred from the
    /// first report.
    void configureProvider(int requestId, String configWire);

    // ---------------------------------------------------------------
    // Down: report state into the system.
    // ---------------------------------------------------------------

    /// Reports a new incoming call and starts it ringing.
    void reportIncomingCall(int requestId, String callId, String handleWire,
            String displayName, int capabilityBits, boolean hasVideo);

    /// Reports a new outgoing call the app is placing.
    void reportOutgoingCall(int requestId, String callId, String handleWire,
            String displayName, int capabilityBits, boolean hasVideo);

    /// The outgoing call has begun connecting. `timestampMs` is wall clock.
    void reportOutgoingStartedConnecting(String callId, long timestampMs);

    /// The outgoing call is connected.
    void reportOutgoingConnected(String callId, long timestampMs);

    /// The incoming call is connected.
    void reportIncomingConnected(String callId, long timestampMs);

    /// Updates the display of a call already reported. Any argument may be
    /// null or -1 to leave that field alone.
    void updateCall(String callId, String handleWire, String displayName,
            int capabilityBits, boolean hasVideo);

    /// The **far end** ended the call. `endReasonOrdinal` is a
    /// `com.codename1.call.CallEndReason` ordinal and becomes what the
    /// system writes in the call log.
    void reportCallEnded(String callId, int endReasonOrdinal, long timestampMs);

    /// **This side** is ending the call.
    void endCall(int requestId, String callId, int endReasonOrdinal);

    /// Holds or resumes a call.
    void setHeld(int requestId, String callId, boolean held);

    /// Mutes or unmutes a call.
    void setMuted(int requestId, String callId, boolean muted);

    /// Sends DTMF digits.
    void sendDtmf(int requestId, String callId, String digits);

    /// Groups `callId` with `otherCallId`, or ungroups it when that is null.
    void setCallGroup(int requestId, String callId, String otherCallId);

    // ---------------------------------------------------------------
    // Audio.
    // ---------------------------------------------------------------

    /// The ordinal of the current `com.codename1.call.session.CallAudioRoute`.
    int getAudioRoute();

    /// Asks for a route by ordinal.
    void setAudioRoute(int requestId, int routeOrdinal);

    /// Shows the system's audio route picker.
    void showAudioRoutePicker(int requestId, String callId);

    // ---------------------------------------------------------------
    // Up-direction acknowledgement.
    // ---------------------------------------------------------------

    /// Answers a system-originated action delivered with `actionToken`.
    ///
    /// The token is opaque and allocated by the port. Exactly one call per
    /// token; a second is ignored rather than treated as an error, because
    /// the facade's safety net and the application may both answer and the
    /// race between them is not worth making the application think about.
    void completeAction(long actionToken, boolean fulfilled);

    // ---------------------------------------------------------------
    // VoIP push.
    // ---------------------------------------------------------------

    /// Registers for VoIP pushes, answering with the token.
    void registerVoipPush(int requestId);

    /// Stops VoIP push delivery.
    void unregisterVoipPush(int requestId);

    /// Tells the port whether application code is listening yet.
    ///
    /// Until this is true the port must hold pushed calls rather than
    /// delivering them, because on iOS the system call is reported by native
    /// code before any application code has run.
    void setJavaReady(boolean ready);

    /// Delivers every call reported natively but not yet seen by Java, then
    /// answers once with the count.
    void drainPendingCalls(int requestId);

    // ---------------------------------------------------------------
    // Directory.
    // ---------------------------------------------------------------

    /// Installs the caller-identification and blocking data at `filePath`.
    ///
    /// A path rather than an array: the list routinely runs to hundreds of
    /// thousands of numbers, and on iOS the process that reads it is a
    /// separate extension, so the data has to be on disk in a shared
    /// container whatever this API looked like.
    void setDirectorySource(int requestId, String filePath);

    /// Asks the system to re-read the directory source.
    void reloadDirectory(int requestId);

    /// Answers with a `CallWire`-encoded status record.
    void getDirectoryStatus(int requestId);

    /// Asks the user for the call-screening role.
    void requestScreeningRole(int requestId);
}
