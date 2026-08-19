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
package com.codename1.home;

/// Typed failure reasons carried by [HomeException]. Ports map their platform
/// error codes onto these so cross-platform code can branch on a stable value
/// instead of parsing messages.
///
/// #### Crossing the boundary by name, not by ordinal
///
/// The native bridges send the [#name()] of one of these, not its ordinal, and
/// [#forName(String)] resolves it. Ordinals are a wire format that breaks
/// silently: a port built against a build where a constant was inserted in the
/// middle would map every error past the insertion point to the wrong one, and
/// a mis-mapped `UNAUTHORIZED` looks exactly like a mis-mapped `TIMEOUT` to
/// everyone downstream. Names cost a few bytes per failure -- and a failure is
/// not the hot path.
public enum HomeError {

    /// The port, device or OS version has no smart-home support at all, or
    /// the requested capability is unavailable on this platform. Returned by
    /// every operation on the fallback [SmartHome] base class.
    NOT_SUPPORTED,

    /// The accessory does not expose this trait, or the backend cannot
    /// express it. Distinct from [#ACCESSORY_NOT_FOUND]: the accessory is
    /// there and this particular capability is not.
    ///
    /// Some traits can never succeed on a given backend rather than merely
    /// being absent from one accessory -- [Trait#OUTLET_IN_USE] and
    /// [Trait#TARGET_HUMIDITY] have no Matter equivalent at all. The javadoc
    /// on each such constant says so.
    TRAIT_NOT_SUPPORTED,

    /// No accessory with the supplied id is in the graph. Usually means the
    /// snapshot the caller is holding is stale; call [SmartHome#refresh()] and
    /// wait for it before reading [SmartHome#getStructures()] again.
    ACCESSORY_NOT_FOUND,

    /// The accessory is in the graph but the platform could not talk to it --
    /// unplugged, out of Thread range, or its bridge is offline. Retryable.
    ACCESSORY_UNREACHABLE,

    /// The operation was refused for lack of authorization.
    UNAUTHORIZED,

    /// The user has not yet been asked. Recoverable by calling
    /// [SmartHome#requestAuthorization()].
    AUTHORIZATION_REQUIRED,

    /// No signed-in account. Google Home only: the Home APIs need an account
    /// and a per-structure grant before any accessory is visible.
    SIGN_IN_REQUIRED,

    /// Smart-home access is blocked by parental controls or device
    /// management. Not recoverable from inside the app.
    RESTRICTED,

    /// The user dismissed a platform authorization, setup or commissioning
    /// flow.
    USER_CANCELED,

    /// A request was rejected before reaching the platform because it was
    /// malformed -- an empty write batch, a trait written with the wrong
    /// [TraitValueKind], a negative timeout.
    INVALID_ARGUMENT,

    /// A write fell outside the range the accessory declares in its
    /// [TraitConstraint].
    ///
    /// Deliberately an error rather than a clamp. An app that asked for 40
    /// degrees and silently got 38 never learns it was wrong, and the bug
    /// surfaces as a user complaint about a thermostat rather than as a
    /// failure at the call site.
    VALUE_OUT_OF_RANGE,

    /// A [TraitUnit] was supplied that measures a different dimension than
    /// the trait requires.
    UNIT_MISMATCH,

    /// The trait can be read but not written.
    READ_ONLY_TRAIT,

    /// The trait can be written but not read. Rare; some Matter attributes
    /// are write-only commands in disguise.
    WRITE_ONLY_TRAIT,

    /// A door lock refused the operation because it requires a PIN and none
    /// was supplied. Set one with
    /// [TraitWrite#setAuthorizationData(java.lang.String)].
    ///
    /// Matter locks with `RequirePINforRemoteOperation` set behave this way.
    /// HomeKit never takes a PIN.
    PIN_REQUIRED,

    /// A door lock rejected the supplied PIN.
    PIN_REJECTED,

    /// The platform's smart-home provider is not installed -- Google Play
    /// services on Android. Recoverable via [SmartHome#openProviderSetup()].
    PROVIDER_UNAVAILABLE,

    /// The installed provider is too old. Also recoverable via
    /// [SmartHome#openProviderSetup()].
    PROVIDER_UPDATE_REQUIRED,

    /// The app is missing build configuration the backend needs -- an
    /// entitlement, a project id, an OAuth client. Always accompanied by
    /// [HomeConfigurationException] and by text from
    /// [SmartHome#getConfigurationProblems()] naming what is missing.
    NOT_CONFIGURED,

    /// Commissioning ran and did not add the accessory. The message carries
    /// the platform's own text.
    COMMISSIONING_FAILED,

    /// This platform cannot commission at all -- watchOS, tvOS, macOS, or an
    /// Android device with no Play services.
    COMMISSIONING_UNAVAILABLE,

    /// The ecosystem app a flow needs -- Apple Home, Google Home -- is not
    /// installed, so there was nothing to hand off to.
    ECOSYSTEM_APP_MISSING,

    /// The platform rate-limited the request.
    RATE_LIMITED,

    /// The platform refused because an operation of this kind is already in
    /// flight. Retryable once it settles.
    BUSY,

    /// The operation did not complete within its safety timeout.
    TIMEOUT,

    /// A payload could not be decoded -- a malformed Matter setup payload, a
    /// platform value the port could not map onto a [TraitValue]. Never
    /// surfaces as an unchecked exception from a parser.
    INVALID_DATA,

    /// Anything the port could not classify. The message carries the
    /// platform's own text.
    UNKNOWN;

    /// Resolves a constant by name, total: an unrecognized or `null` name
    /// answers [#UNKNOWN] rather than throwing.
    ///
    /// This exists so the boundary cannot throw. `Enum.valueOf` raises
    /// `IllegalArgumentException` on an unknown name, and the one place this
    /// is called is while decoding a failure that has already happened -- so
    /// the throw would replace a real error the caller could act on with an
    /// unrelated one they cannot. A port from a newer build naming an error
    /// this one does not have degrades to `UNKNOWN` with the platform text
    /// intact.
    ///
    /// #### Parameters
    ///
    /// - `name`: the [#name()] of a constant, or `null`
    ///
    /// #### Returns
    ///
    /// the matching constant, or [#UNKNOWN]
    public static HomeError forName(String name) {
        if (name == null) {
            return UNKNOWN;
        }
        for (HomeError candidate : values()) {
            if (candidate.name().equals(name)) {
                return candidate;
            }
        }
        return UNKNOWN;
    }
}
