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
package com.codename1.impl.javase.health;

import com.codename1.health.HealthAccess;
import com.codename1.health.HealthAuthorizationStatus;
import com.codename1.health.HealthDataType;
import com.codename1.health.HealthDeleteRequest;
import com.codename1.health.HealthError;
import com.codename1.health.HealthException;
import com.codename1.health.HealthSample;
import com.codename1.health.HealthWriteResult;
import com.codename1.health.SampleQuery;
import com.codename1.health.SamplePage;
import com.codename1.impl.health.LocalHealthStore;
import com.codename1.util.AsyncResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The simulator's health store: a real local store with scripted
/// permissions and fault injection layered on top.
///
/// #### The read-authorization trap
///
/// This is the most valuable thing the simulator does. HealthKit
/// deliberately refuses to disclose read authorization: a denied read
/// returns an empty result, indistinguishable from having no data, so that
/// an app cannot infer what a user is choosing to hide. A developer who
/// only ever tests against a permissive store will not discover this until
/// their app is in review or in a user's hands.
///
/// So [ReadAuthPolicy#IOS_OPAQUE] is the **default** here, and
/// [ReadAuthScript#DENIED_SILENT] reproduces the exact trap: authorization
/// appears to succeed, the status reads UNKNOWN, and queries return empty
/// with no error. Switching to [ReadAuthPolicy#ANDROID_EXPLICIT] makes the
/// same script fail loudly instead, which is what Health Connect does.
///
/// Drive it from the Simulate menu, or from a test with
/// `CN.execute("health:item3")`.
public class SimulatedHealthStore extends LocalHealthStore {

    /// How a platform behaves when read access has been refused.
    public enum ReadAuthPolicy {
        /// HealthKit's behaviour: read authorization is never disclosed and
        /// a denied read is silently empty. The default, because it is the
        /// more surprising of the two and the one worth discovering early.
        IOS_OPAQUE,
        /// Health Connect's behaviour: read permission is an ordinary
        /// runtime grant and a denied read fails.
        ANDROID_EXPLICIT
    }

    /// What the user is pretending to have chosen for one data type.
    public enum ReadAuthScript {
        /// Granted; queries return data.
        GRANTED,
        /// Refused, and the platform hides the refusal -- queries return
        /// empty with no error. The trap.
        DENIED_SILENT,
        /// Refused, and the platform says so.
        DENIED_ERROR,
        /// Never asked.
        NOT_DETERMINED,
        /// Granted, but the user genuinely has no data of this type. Looks
        /// identical to DENIED_SILENT from inside the app, which is the
        /// point of having both.
        GRANTED_BUT_NO_DATA
    }

    private final Map<String, ReadAuthScript> readScripts =
            new HashMap<String, ReadAuthScript>();
    private final Map<String, HealthAuthorizationStatus> writeScripts =
            new HashMap<String, HealthAuthorizationStatus>();

    private ReadAuthPolicy policy = ReadAuthPolicy.IOS_OPAQUE;
    private boolean available = true;
    private String failNextOp;
    private HealthError failNextError;
    private String failNextMessage;

    /// Which platform's read-authorization behaviour to emulate.
    public ReadAuthPolicy getReadAuthorizationPolicy() {
        return policy;
    }

    /// Switches the emulated read-authorization behaviour.
    public void setReadAuthorizationPolicy(ReadAuthPolicy policy) {
        this.policy = policy == null ? ReadAuthPolicy.IOS_OPAQUE : policy;
    }

    /// Scripts what the user chose for reading `type`.
    public void setReadPermission(HealthDataType type, ReadAuthScript script) {
        if (type != null) {
            readScripts.put(type.getId(),
                    script == null ? ReadAuthScript.GRANTED : script);
        }
    }

    /// Scripts what the user chose for writing `type`.
    public void setWritePermission(HealthDataType type,
            HealthAuthorizationStatus status) {
        if (type != null) {
            writeScripts.put(type.getId(),
                    status == null ? HealthAuthorizationStatus.AUTHORIZED
                            : status);
        }
    }

    /// Applies one read script to every known type.
    public void setAllReadPermissions(ReadAuthScript script) {
        for (HealthDataType t : HealthDataType.values()) {
            setReadPermission(t, script);
        }
    }

    /// Applies one write status to every known type.
    public void setAllWritePermissions(HealthAuthorizationStatus status) {
        for (HealthDataType t : HealthDataType.values()) {
            setWritePermission(t, status);
        }
    }

    /// Whether the store is reachable at all, emulating a missing or
    /// disabled provider.
    public void setAvailable(boolean available) {
        this.available = available;
    }

    /// `true` when the store is currently reachable.
    public boolean isAvailable() {
        return available;
    }

    /// Makes the next occurrence of `op` fail.
    ///
    /// Recognised ops: `requestAuthorization`, `query`, `aggregate`,
    /// `save`, `delete`. One-shot -- the following call succeeds -- so a
    /// test can assert that an app recovers rather than wedges.
    public void failNext(String op, HealthError error, String message) {
        this.failNextOp = op;
        this.failNextError = error == null ? HealthError.UNKNOWN : error;
        this.failNextMessage = message == null
                ? "simulated " + op + " failure" : message;
    }

    /// Seeds samples straight into the store, bypassing permission and
    /// validation checks.
    ///
    /// This is how a scripted dataset is loaded: the data is meant to look
    /// as though it had been there all along, written by other apps and
    /// devices, so it must not be subject to this app's write permissions.
    public void seed(List<HealthSample> samples) {
        if (samples == null) {
            return;
        }
        for (int i = 0; i < samples.size(); i++) {
            addSampleDirect(samples.get(i));
        }
    }

    /// Clears scripted permissions, faults and availability. Data is left
    /// alone; use `clear()` for that.
    public void resetScripts() {
        readScripts.clear();
        writeScripts.clear();
        policy = ReadAuthPolicy.IOS_OPAQUE;
        available = true;
        failNextOp = null;
    }

    private boolean consumeFailure(String op, AsyncResource out) {
        if (failNextOp == null || !failNextOp.equals(op)) {
            return false;
        }
        HealthError error = failNextError;
        String message = failNextMessage;
        failNextOp = null;
        out.error(new HealthException(error, message));
        return true;
    }

    private ReadAuthScript scriptFor(HealthDataType type) {
        ReadAuthScript s = readScripts.get(type.getId());
        return s == null ? ReadAuthScript.GRANTED : s;
    }

    // ------------------------------------------------------------------

    public boolean isSupported() {
        return available;
    }

    /// Reports what the emulated platform is willing to say.
    ///
    /// Under [ReadAuthPolicy#IOS_OPAQUE] this is always
    /// [HealthAuthorizationStatus#UNKNOWN] regardless of the script --
    /// that is the whole point, and code that branches on it must handle
    /// the value rather than assume it means "denied".
    public HealthAuthorizationStatus getReadAuthorizationStatus(
            HealthDataType type) {
        if (!available) {
            return HealthAuthorizationStatus.NOT_SUPPORTED;
        }
        if (policy == ReadAuthPolicy.IOS_OPAQUE) {
            return HealthAuthorizationStatus.UNKNOWN;
        }
        switch (scriptFor(type)) {
            case GRANTED:
            case GRANTED_BUT_NO_DATA:
                return HealthAuthorizationStatus.AUTHORIZED;
            case NOT_DETERMINED:
                return HealthAuthorizationStatus.NOT_DETERMINED;
            default:
                return HealthAuthorizationStatus.DENIED;
        }
    }

    public HealthAuthorizationStatus getWriteAuthorizationStatus(
            HealthDataType type) {
        if (!available) {
            return HealthAuthorizationStatus.NOT_SUPPORTED;
        }
        HealthAuthorizationStatus s = writeScripts.get(type.getId());
        return s == null ? HealthAuthorizationStatus.AUTHORIZED : s;
    }

    /// A capability, not a grant.
    ///
    /// Both mobile stores answer this from what the platform can store,
    /// independently of what the user has allowed -- and the shared layer
    /// rejects an unwritable type as TYPE_NOT_SUPPORTED before any
    /// authorization flow runs. Folding the scripted grant in here meant a
    /// test scripting DENIED never reached the authorization path at all:
    /// it got "this platform cannot store that" instead of "you are not
    /// allowed to", which is a different bug for a developer to chase. The
    /// scripted status is enforced in [#doWrite(List, AsyncResource)],
    /// where a real store enforces it.
    public boolean isWritable(HealthDataType type) {
        return type != null && available;
    }

    protected void doRequestAuthorization(List<HealthAccess> access,
            AsyncResource<Boolean> out) {
        if (consumeFailure("requestAuthorization", out)) {
            return;
        }
        // Resolves true because the *flow completed*, not because anything
        // was granted -- exactly as iOS behaves when the user declines
        // every switch on the sheet.
        out.complete(Boolean.TRUE);
    }

    protected void doReadSamples(SampleQuery query,
            AsyncResource<SamplePage> out) {
        if (consumeFailure("query", out)) {
            return;
        }
        List<HealthDataType> types = query.getTypes();
        for (int i = 0; i < types.size(); i++) {
            if (failsRead(types.get(i))) {
                out.error(new HealthException(HealthError.UNAUTHORIZED,
                        "read access to " + types.get(i).getId()
                                + " was refused"));
                return;
            }
        }
        // Emptiness is per type, not per query. A type scripted to yield
        // nothing -- GRANTED_BUT_NO_DATA, or a silent denial under the
        // iOS policy -- used to empty the whole page, so adding one such
        // type to a query made unrelated steps and heart rate disappear
        // too. That is not what either platform does, and it turned the
        // simulator's most valuable script into a misleading one.
        //
        // Withheld through the store's visibility hook rather than by
        // filtering the finished page, which is where the first attempt
        // at this went wrong: the shared store sorts and cuts to the
        // limit before returning, so a hidden record that sorted first
        // ate a slot and a limit-one query came back empty with a visible
        // record sitting right behind it.
        super.doReadSamples(query, out);
    }

    @Override
    protected boolean isVisible(HealthSample s) {
        return !hidesData(s.getType());
    }

    /// Whether a read of `type` fails outright rather than coming back
    /// empty. An error is an exception, not an absence, so it is the one
    /// case that still fails the whole query.
    private boolean failsRead(HealthDataType type) {
        ReadAuthScript script = scriptFor(type);
        if (script == ReadAuthScript.GRANTED
                || script == ReadAuthScript.GRANTED_BUT_NO_DATA) {
            return false;
        }
        return script == ReadAuthScript.DENIED_ERROR
                || policy == ReadAuthPolicy.ANDROID_EXPLICIT;
    }

    /// Whether `type` contributes nothing while the rest of the query
    /// proceeds -- scripted as granted-but-empty, or silently denied
    /// under the iOS policy, which an app cannot tell apart and neither
    /// can this.
    private boolean hidesData(HealthDataType type) {
        ReadAuthScript script = scriptFor(type);
        if (script == ReadAuthScript.GRANTED_BUT_NO_DATA) {
            return true;
        }
        return script != ReadAuthScript.GRANTED
                && policy != ReadAuthPolicy.ANDROID_EXPLICIT;
    }

    /// Aggregates obey the read script too.
    ///
    /// Aggregation reads the local records directly rather than going
    /// through doReadSamples, so a type scripted DENIED_SILENT returned a
    /// real total while the matching sample read came back empty -- the
    /// developer got a chart from data the simulator was pretending they
    /// could not see, which is precisely the trap this store exists to
    /// spring.
    protected void doAggregate(com.codename1.health.AggregateQuery query,
            long[] boundaries,
            AsyncResource<List<com.codename1.health.AggregateResult>> out) {
        if (consumeFailure("aggregate", out)) {
            return;
        }
        List<HealthDataType> types = query.getTypes();
        for (int i = 0; i < types.size(); i++) {
            if (failsRead(types.get(i))) {
                out.error(new HealthException(HealthError.UNAUTHORIZED,
                        "read access to " + types.get(i).getId()
                                + " was refused"));
                return;
            }
        }
        // Per type, exactly as the read is, and through the same
        // visibility hook. Emptying every metric because one type in the
        // query was scripted to yield nothing made a scripted heart-rate
        // gap erase the step total beside it, and a developer would
        // reasonably read that as a bug in their own aggregation rather
        // than as the script doing its job.
        super.doAggregate(query, boundaries, out);
    }

    protected void doWrite(List<HealthSample> samples,
            AsyncResource<HealthWriteResult> out) {
        if (consumeFailure("save", out)) {
            return;
        }
        for (HealthSample sample : samples) {
            HealthAuthorizationStatus status =
                    getWriteAuthorizationStatus(sample.getType());
            // NOT_DETERMINED fails too. A write before the user has been
            // asked is refused by both platforms, and letting it succeed
            // here would let an app ship having never exercised its own
            // authorization flow.
            if (status == HealthAuthorizationStatus.DENIED
                    || status == HealthAuthorizationStatus.NOT_DETERMINED) {
                out.error(new HealthException(HealthError.UNAUTHORIZED,
                        "write access to " + sample.getType().getId()
                                + " is " + status));
                return;
            }
        }
        super.doWrite(samples, out);
    }

    /// Deleting needs write authorization, as it does on Health Connect.
    ///
    /// Writes honoured the scripted status while deletes went straight
    /// through, so a simulator test could delete records it was supposedly
    /// not allowed to touch.
    protected void doDelete(HealthDeleteRequest request,
            AsyncResource<Integer> out) {
        if (consumeFailure("delete", out)) {
            return;
        }
        List<HealthDataType> types = request.getTypes();
        for (int i = 0; i < types.size(); i++) {
            HealthAuthorizationStatus status =
                    getWriteAuthorizationStatus(types.get(i));
            if (status == HealthAuthorizationStatus.DENIED
                    || status == HealthAuthorizationStatus.NOT_DETERMINED) {
                out.error(new HealthException(HealthError.UNAUTHORIZED,
                        "write access to " + types.get(i).getId()
                                + " is " + status));
                return;
            }
        }
        super.doDelete(request, out);
    }
}
