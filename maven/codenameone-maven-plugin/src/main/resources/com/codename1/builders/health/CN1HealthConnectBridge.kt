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
package com.codename1.health

import android.content.Context
import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.units.Temperature
import androidx.health.connect.client.units.Volume
import com.codename1.impl.android.HealthConnectDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant

/**
 * Injected by the Codename One build server when an app references
 * `com.codename1.health`. Implements the pure-Java
 * [HealthConnectDelegate] seam in terms of Health Connect's Kotlin
 * coroutine API.
 *
 * This file is Kotlin because `androidx.health.connect` exposes only
 * `suspend` functions. It lives here, in the generated app project,
 * rather than in the Codename One Android port, because the port compiles
 * against an old `android.jar` with no AndroidX and no Kotlin on its
 * classpath -- the same reason the Android Auto glue is injected rather
 * than shipped.
 *
 * Every `suspend` call is wrapped so no coroutine escapes across the
 * boundary: the delegate interface speaks only `String` and primitives.
 *
 * Keep this file in sync with the BuildDaemon copy.
 */
class CN1HealthConnectBridge(private val context: Context)
    : HealthConnectDelegate {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val client: HealthConnectClient? by lazy {
        try {
            if (HealthConnectClient.getSdkStatus(context)
                    == HealthConnectClient.SDK_AVAILABLE) {
                HealthConnectClient.getOrCreate(context)
            } else {
                null
            }
        } catch (t: Throwable) {
            null
        }
    }

    override fun sdkStatus(): Int = try {
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE ->
                HealthConnectDelegate.SDK_AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectDelegate.SDK_UPDATE_REQUIRED
            else -> HealthConnectDelegate.SDK_UNAVAILABLE
        }
    } catch (t: Throwable) {
        HealthConnectDelegate.SDK_UNAVAILABLE
    }

    override fun providerPackageName(): String =
        "com.google.android.apps.healthdata"

    /**
     * Runs a suspending block and reports the outcome through the
     * callback, translating the exceptions Health Connect throws into the
     * delegate's small error vocabulary.
     */
    private fun run(cb: HealthConnectDelegate.Callback,
                    block: suspend () -> String) {
        scope.launch {
            try {
                cb.onSuccess(block())
            } catch (e: SecurityException) {
                cb.onError(HealthConnectDelegate.ERR_AUTH_DENIED,
                    e.message ?: "permission denied")
            } catch (e: IllegalStateException) {
                cb.onError(HealthConnectDelegate.ERR_PROVIDER,
                    e.message ?: "Health Connect unavailable")
            } catch (e: IllegalArgumentException) {
                cb.onError(HealthConnectDelegate.ERR_INVALID_ARGUMENT,
                    e.message ?: "invalid request")
            } catch (t: Throwable) {
                // A token that has aged out surfaces as a provider-defined
                // exception; the portable layer maps this onto a resync.
                val expired = t.javaClass.name.contains("ChangesTokenExpired")
                cb.onError(
                    if (expired) HealthConnectDelegate.ERR_TOKEN_EXPIRED
                    else HealthConnectDelegate.ERR_UNKNOWN,
                    t.message ?: t.javaClass.simpleName)
            }
        }
    }

    private fun requireClient(): HealthConnectClient =
        client ?: throw IllegalStateException(
            "Health Connect is not available on this device")

    override fun grantedPermissions(cb: HealthConnectDelegate.Callback) {
        run(cb) {
            val granted = requireClient().permissionController
                .getGrantedPermissions()
            granted.flatMap { toTokens(it) }.distinct()
                .joinToString(",")
        }
    }

    override fun permissionIntent(permissionsCsv: String): Intent {
        val perms = permissionsCsv.split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { toHealthPermission(it.trim()) }
            .toMutableSet()
        perms.addAll(declaredSpecialPermissions())
        return PermissionController.createRequestPermissionResultContract()
            .createIntent(context, perms)
    }

    /**
     * The background and history permissions the manifest declares.
     *
     * These are not per-type, so they never appear in a HealthAccess list
     * and cannot come through the token table. Declaring them without ever
     * requesting them -- which is what the builder used to produce -- gives
     * an app that is configured for background or historical reads and is
     * never authorized for either.
     *
     * Read back from the manifest rather than passed in, so the bridge
     * asks for exactly what the build declared.
     */
    private fun declaredSpecialPermissions(): Set<String> {
        val special = setOf(
            "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND",
            "android.permission.health.READ_HEALTH_DATA_HISTORY")
        return try {
            val info = context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_PERMISSIONS)
            val declared = info.requestedPermissions ?: emptyArray()
            declared.filter { special.contains(it) }.toSet()
        } catch (t: Throwable) {
            emptySet()
        }
    }

    override fun parsePermissionResult(resultCode: Int,
                                       data: Intent?): String {
        return try {
            val granted = PermissionController
                .createRequestPermissionResultContract()
                .parseResult(resultCode, data)
            granted.flatMap { toTokens(it) }.distinct()
                .joinToString(",")
        } catch (t: Throwable) {
            ""
        }
    }

    override fun readRecords(requestJson: String,
                             cb: HealthConnectDelegate.Callback) {
        run(cb) {
            val json = JSONObject(requestJson)
            val start = Instant.ofEpochMilli(json.getLong("start"))
            val end = Instant.ofEpochMilli(json.getLong("end"))
            val filter = TimeRangeFilter.between(start, end)
            val types = json.getJSONArray("types")
            val sources = json.optJSONArray("sources")
            val origins = if (sources == null) emptySet() else
                (0 until sources.length())
                    .map { DataOrigin(sources.getString(it)) }.toSet()
            val sb = StringBuilder()
            // The limit is the caller's budget for the whole query, not per
            // type. Spending it again on each type lets the reply exceed the
            // advertised limit, and the shared layer then trims the combined
            // block -- which can discard every record after the first type.
            var budget = json.optInt("limit", 0)
            if (budget <= 0) {
                budget = Int.MAX_VALUE
            }
            // Descending matters for the common "latest reading" shape: a
            // limit-1 ascending query returns the oldest record, which is
            // the opposite of what the caller asked for.
            val ascending = !json.optBoolean("descending", false)
            // One continuation token per type, not one for the query. A
            // single token is opaque to a record class other than the one
            // that issued it, so carrying just the first type's token
            // restarted every other type from the beginning on page two --
            // or handed a steps token to HeartRateRecord.
            // Whether a series record may stay whole. Defaults to true so
            // an older descriptor, or one from a caller that never touched
            // the option, keeps the flattened shape it has always had.
            val flatten = json.optBoolean("flatten", true)
            val inTokens = parseTokens(json.optString("pageToken", ""))
            val outTokens = LinkedHashMap<String, String>()
            // Every type is read to the full budget and merged afterwards.
            // Spending the budget type by type made the answer depend on
            // the order of `types` rather than on time: a descending limit-1
            // query over [steps, heart_rate] always returned the newest
            // step even when a heart-rate sample was newer.
            val perType = ArrayList<String>()
            for (i in 0 until types.length()) {
                val token = types.getString(i)
                val block = StringBuilder()
                val r = appendRecords(block, token, filter,
                    budget, origins, ascending, inTokens[token], flatten)
                perType.add(block.toString())
                // Exhausted types are recorded with an empty token, not
                // omitted. Omitting them made the next page see no token
                // for that type and read it from the beginning again,
                // duplicating records while the other types advanced.
                outTokens[token] = r.second ?: ""
            }
            // Nothing left anywhere means no trailer token at all.
            if (outTokens.values.all { it.isEmpty() }) {
                outTokens.clear()
            }
            // A single type is never trimmed here.
            //
            // Its token resumes after the records already fetched, so a
            // record dropped by the trim is behind the token and gone for
            // good -- and clearing the token to signal that only made the
            // next call restart from the beginning and fetch the same page
            // again. appendRecords already bounds the read by the limit at
            // page granularity, so the overshoot is at most the tail of one
            // page and the shared layer trims it for the caller while the
            // token still points at the right place.
            //
            // Trimming and paging genuinely cannot both be honoured across
            // several types: a Health Connect token advances its own type,
            // so a sample dropped by the merge has no token that would
            // return it. There the limit wins, because it is what bounds
            // memory, and the reply says so rather than pretending to be
            // complete -- with no token, because there is no next page to
            // ask for. Multi-type queries are single-page.
            val singleType = types.length() == 1
            val merged = if (singleType) perType[0]
                else mergeByTime(perType, budget, ascending)
            val truncated = !singleType && perType.sumOf { block ->
                block.count { it == '\n' }
            } > merged.count { it == '\n' }
            if (truncated) {
                outTokens.clear()
            }
            sb.append(merged)
            // Trailer: what is left on the platform side. Reporting nothing
            // made every page look complete, so the caller's paging loop
            // stopped at the first limit and lost the rest.
            sb.append('#').append(encodeTokens(outTokens)).append('\t')
                .append(if (outTokens.isEmpty() && !truncated) "0" else "1")
                .append('\n')
            sb.toString()
        }
    }

    /** Decodes the per-type continuation tokens from a page token. */
    private fun parseTokens(encoded: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        if (encoded.isEmpty()) {
            return out
        }
        for (part in encoded.split(TOKEN_SEPARATOR)) {
            val eq = part.indexOf('=')
            if (eq > 0) {
                // An empty value means that type is exhausted, which is
                // deliberately different from the type being absent.
                out[part.substring(0, eq)] = part.substring(eq + 1)
            }
        }
        return out
    }

    private fun encodeTokens(tokens: Map<String, String>): String {
        val sb = StringBuilder()
        for ((type, token) in tokens) {
            if (sb.isNotEmpty()) {
                sb.append(TOKEN_SEPARATOR)
            }
            sb.append(type).append('=').append(token)
        }
        return sb.toString()
    }

    /// Neither a portable type id nor a Health Connect token contains it.
    private val TOKEN_SEPARATOR = '\u0001'

    /**
     * Merges per-type blocks in time order and applies the shared limit.
     *
     * The line format leads with id, type, start -- start is field 2 -- so
     * the merge sorts on that rather than re-parsing into records.
     */
    private fun mergeByTime(blocks: List<String>, limit: Int,
                            ascending: Boolean): String {
        val lines = ArrayList<String>()
        for (block in blocks) {
            for (line in block.split('\n')) {
                if (line.isNotBlank()) {
                    lines.add(line)
                }
            }
        }
        val keyed = lines.sortedBy {
            val f = it.split('\t')
            if (f.size > 2) f[2].toLongOrNull() ?: 0L else 0L
        }
        val ordered = if (ascending) keyed else keyed.asReversed()
        val out = StringBuilder()
        // The cut never falls inside a record. Every line from one series
        // record shares its id, and Health Connect's token resumes after a
        // whole record -- so trimming mid-record stranded the remaining
        // samples with no token that could ever reach them. Overshooting
        // the limit by the tail of one record is recoverable; losing it is
        // not.
        //
        // Records are admitted, and admission is what the budget buys; the
        // loop never breaks. Two earlier attempts stopped at the first
        // line of an unseen record: comparing against only the previous
        // line's id cut inside a record whose samples interleaved with
        // another's (A1, B1, A2), and remembering which records had
        // started still stopped at C1 in A1, B1, C1, A2 -- leaving A
        // half-emitted while the token had advanced past all of it, so A2
        // was unreachable for good. Skipping a line whose record was never
        // admitted, rather than stopping on it, lets every admitted record
        // run to its end and keeps the output in time order.
        val admitted = HashSet<String>()
        var emitted = 0
        for (line in ordered) {
            val id = line.substringBefore('\t')
            if (!admitted.contains(id)) {
                if (limit in 1..emitted) {
                    continue
                }
                admitted.add(id)
            }
            emitted++
            out.append(line).append('\n')
        }
        return out.toString()
    }

    /**
     * Reads one portable type and appends it in the shared line format.
     *
     * Returns how many records were emitted so the caller can spend one
     * shared limit across every requested type.
     */
    private suspend fun appendRecords(sb: StringBuilder, token: String,
                                      filter: TimeRangeFilter, limit: Int,
                                      origins: Set<DataOrigin>,
                                      ascending: Boolean,
                                      resumeToken: String?,
                                      flatten: Boolean): Pair<Int, String?> {
        // A type this bridge cannot read is rejected rather than returned
        // as an empty page: the caller cannot tell an empty page apart from
        // "you have no data", which is the one answer a health API must
        // never guess at.
        val type = readableRecordClassFor(token)
            ?: throw IllegalArgumentException(
                "Health Connect reads are not implemented for '" + token
                    + "' in this build")
        if (limit <= 0) {
            return Pair(0, null)
        }
        if (resumeToken != null && resumeToken.isEmpty()) {
            return Pair(0, null)
        }
        // Health Connect rejects a pageSize above MAX_PAGE_SIZE outright, so
        // a caller wanting more records than one page holds is served by
        // walking pages rather than by failing the read. Without this an
        // ordinary unbounded query throws before it reads anything.
        var remaining = if (limit > 0) limit else Int.MAX_VALUE
        var emitted = 0
        var pageToken: String? = resumeToken
        // Samples seen per record so far. The caller's limit counts
        // samples while pageSize counts records, and one series record
        // holds many samples -- so asking for `remaining` records fetched
        // far more than was wanted. Nothing fetched is ever discarded (the
        // page token has already moved past it), which makes the overshoot
        // real memory rather than a rounding error. This is measured, not
        // guessed: it starts at one sample per record, which is exactly
        // right for every scalar type, and the first page of a series type
        // corrects it for every page after.
        var samplesPerRecord = 1
        while (remaining > 0) {
            val wantRecords = maxOf(1, remaining / samplesPerRecord)
            val page = requireClient().readRecords(
                ReadRecordsRequest(type, timeRangeFilter = filter,
                    // Source filtering happens here rather than after the
                    // fact: SampleQuery.addSource is how an app avoids
                    // counting the same steps from both phone and watch,
                    // and ignoring it silently inflates every total.
                    dataOriginFilter = origins,
                    ascendingOrder = ascending,
                    pageSize = minOf(wantRecords, MAX_PAGE_SIZE),
                    pageToken = pageToken))
            // Counted in emitted lines, not records. One heart-rate record
            // holds many samples and appendOne flattens them, so a
            // limit-1 request could otherwise return hundreds of lines.
            // Every record in the fetched page is emitted. pageSize is
            // counted in records while the budget is counted in samples,
            // so breaking out mid-page abandoned records already fetched --
            // and the page token resumes after the whole page, so they
            // could never be read again. Overshooting the caller's limit
            // is recoverable; skipping records is not.
            var lines = 0
            for (record in page.records) {
                lines += appendOne(sb, record, token, flatten)
            }
            if (page.records.isNotEmpty()) {
                samplesPerRecord = maxOf(1, lines / page.records.size)
            }
            remaining -= lines
            emitted += lines
            pageToken = page.pageToken
            if (pageToken == null || page.records.isEmpty()) {
                break
            }
        }
        return Pair(emitted, pageToken)
    }

    /**
     * Emits one record in the shared line format, tagged with `token`.
     *
     * The token is supplied rather than derived because the relation is
     * many-to-one: Health Connect has a single `DistanceRecord` behind the
     * walking, cycling and swimming distance types, so a read must label
     * its lines with the type that was actually asked for or the portable
     * layer discards them as the wrong type.
     *
     * Returns how many lines were written, which is zero for record
     * shapes with no single-value form -- the caller reports those rather
     * than silently dropping them.
     */
    private fun appendOne(sb: StringBuilder, record: Record,
                          token: String, flatten: Boolean): Int {
        if (!flatten && appendWholeSeries(sb, record, token)) {
            return 1
        }
        when (record) {
            is StepsRecord -> interval(sb, record, token,
                record.startTime, record.endTime,
                record.count.toDouble(), "count")

            is DistanceRecord -> interval(sb, record, token,
                record.startTime, record.endTime,
                record.distance.inMeters, "m")

            is FloorsClimbedRecord -> interval(sb, record, token,
                record.startTime, record.endTime,
                record.floors, "count")

            is ElevationGainedRecord -> interval(sb, record, token,
                record.startTime, record.endTime,
                record.elevation.inMeters, "m")

            is ActiveCaloriesBurnedRecord -> interval(sb, record, token,
                record.startTime, record.endTime,
                record.energy.inKilocalories, "kcal")

            is WheelchairPushesRecord -> interval(sb, record, token,
                record.startTime, record.endTime,
                record.count.toDouble(), "count")

            is HydrationRecord -> interval(sb, record, token,
                record.startTime, record.endTime,
                record.volume.inLiters, "L")

            is WeightRecord -> instant(sb, record, token, record.time,
                record.weight.inKilograms, "kg")

            is LeanBodyMassRecord -> instant(sb, record, token, record.time,
                record.mass.inKilograms, "kg")

            is BoneMassRecord -> instant(sb, record, token, record.time,
                record.mass.inKilograms, "kg")

            is BodyFatRecord -> instant(sb, record, token, record.time,
                record.percentage.value, "%")

            is HeightRecord -> instant(sb, record, token, record.time,
                record.height.inMeters, "m")

            is RestingHeartRateRecord -> instant(sb, record, token,
                record.time,
                record.beatsPerMinute.toDouble(), "count/min")

            is OxygenSaturationRecord -> instant(sb, record, token,
                record.time,
                record.percentage.value, "%")

            is RespiratoryRateRecord -> instant(sb, record, token, record.time,
                record.rate, "count/min")

            is BasalBodyTemperatureRecord -> instant(sb, record, token,
                record.time,
                record.temperature.inCelsius, "degC")

            is BodyTemperatureRecord -> instant(sb, record, token, record.time,
                record.temperature.inCelsius, "degC")

            is Vo2MaxRecord -> instant(sb, record, token, record.time,
                record.vo2MillilitersPerMinuteKilogram, "mL/(kg*min)")

            is BloodGlucoseRecord -> instant(sb, record, token, record.time,
                record.level.inMillimolesPerLiter, "mmol/L")

            // Series records hold many samples in one record and the
            // portable layer flattens by default, so emit one line per
            // sample rather than per record.
            is HeartRateRecord -> record.samples.forEach {
                sample(sb, record, token, it.time.toEpochMilli(),
                    it.beatsPerMinute.toDouble(), "count/min")
            }

            is PowerRecord -> record.samples.forEach {
                sample(sb, record, token, it.time.toEpochMilli(),
                    it.power.inWatts, "W")
            }

            is SpeedRecord -> record.samples.forEach {
                sample(sb, record, token, it.time.toEpochMilli(),
                    it.speed.inMetersPerSecond, "m/s")
            }

            is CyclingPedalingCadenceRecord -> record.samples.forEach {
                sample(sb, record, token, it.time.toEpochMilli(),
                    it.revolutionsPerMinute, "count/min")
            }

            is StepsCadenceRecord -> record.samples.forEach {
                sample(sb, record, token, it.time.toEpochMilli(),
                    it.rate, "count/min")
            }

            else -> return 0
        }
        // One line per scalar record; a series contributes one per sample.
        //
        // A series record is never split. Health Connect's page token
        // resumes after a whole record, so emitting a prefix would strand
        // the rest with no token that could ever reach it. The page may
        // therefore overshoot the limit by less than one record, which the
        // shared layer trims for the caller while the token still resumes
        // at the right place.
        return when (record) {
            is HeartRateRecord -> record.samples.size
            is PowerRecord -> record.samples.size
            is SpeedRecord -> record.samples.size
            is CyclingPedalingCadenceRecord -> record.samples.size
            is StepsCadenceRecord -> record.samples.size
            else -> 1
        }
    }

    // The record timestamps are passed in rather than read off a shared
    // supertype: androidx.health.connect marks IntervalRecord and
    // InstantaneousRecord internal, so they cannot be named here. Each
    // branch above knows its concrete type, which is where the times come
    // from.
    private fun interval(sb: StringBuilder, r: Record, token: String,
                         start: Instant, end: Instant, value: Double,
                         unit: String) {
        line(sb, r.metadata.id, token, start.toEpochMilli(),
            end.toEpochMilli(), value, unit, originOf(r), r)
    }

    private fun instant(sb: StringBuilder, r: Record, token: String,
                        at: Instant, value: Double, unit: String) {
        line(sb, r.metadata.id, token, at.toEpochMilli(),
            at.toEpochMilli(), value, unit, originOf(r), r)
    }

    private fun sample(sb: StringBuilder, r: Record, token: String,
                       at: Long, value: Double, unit: String) {
        line(sb, r.metadata.id, token, at, at, value, unit, originOf(r), r)
    }

    private fun originOf(r: Record): String {
        val pkg = r.metadata.dataOrigin.packageName
        return if (pkg.isNullOrEmpty()) "UNKNOWN" else pkg
    }

    private fun line(sb: StringBuilder, id: String, type: String,
                     start: Long, end: Long, value: Double, unit: String,
                     origin: String, r: Record) {
        sb.append(id).append('\t').append(type).append('\t')
            .append(start).append('\t').append(end).append('\t')
            .append(value).append('\t').append(unit).append('\t')
            // The real originating package, because the shared layer reads
            // this field as the source bundle id and filters on it. Writing
            // a placeholder here made every source-filtered read come back
            // empty, even though the native request had filtered correctly.
            .append(origin).append('\t')
            // Fields 7 and 8 are the source display name and device name,
            // which Health Connect does not give us. They stay empty so
            // the recording method lands in field 9 rather than being read
            // back as the name of the app that wrote the sample.
            .append('\t').append('\t')
            .append(recordingMethodName(r)).append('\n')
    }

    /**
     * Emits a series record whole, as one line, when the caller asked to
     * keep it intact. Returns false for anything that is not a series, so
     * the ordinary per-measurement path handles it.
     *
     * Health Connect is the only platform here that has series records at
     * all, so this is the only place `setFlattenSeries(false)` can be
     * honoured. Ignoring it meant the option did nothing anywhere and the
     * caller got scalar samples whatever it asked for.
     */
    private fun appendWholeSeries(sb: StringBuilder, record: Record,
                                  token: String): Boolean {
        when (record) {
            is HeartRateRecord -> series(sb, record, token, "count/min",
                record.samples.map {
                    Pair(it.time.toEpochMilli(), it.beatsPerMinute.toDouble())
                })

            is PowerRecord -> series(sb, record, token, "W",
                record.samples.map {
                    Pair(it.time.toEpochMilli(), it.power.inWatts)
                })

            is SpeedRecord -> series(sb, record, token, "m/s",
                record.samples.map {
                    Pair(it.time.toEpochMilli(), it.speed.inMetersPerSecond)
                })

            is CyclingPedalingCadenceRecord -> series(sb, record, token,
                "count/min",
                record.samples.map {
                    Pair(it.time.toEpochMilli(), it.revolutionsPerMinute)
                })

            is StepsCadenceRecord -> series(sb, record, token, "count/min",
                record.samples.map {
                    Pair(it.time.toEpochMilli(), it.rate)
                })

            else -> return false
        }
        return true
    }

    private fun series(sb: StringBuilder, r: Record, token: String,
                       unit: String, points: List<Pair<Long, Double>>) {
        // An empty series would decode to a record with no measurements,
        // which is indistinguishable from a decode failure. There is
        // nothing to report, so nothing is written.
        if (points.isEmpty()) {
            return
        }
        val start = points.minOf { it.first }
        val end = points.maxOf { it.first }
        sb.append('~').append(r.metadata.id).append('\t')
            .append(token).append('\t')
            .append(start).append('\t').append(end).append('\t')
            .append(points.size).append('\t').append(unit).append('\t')
            .append(originOf(r)).append('\t')
            // Source display name and device name, which Health Connect
            // does not give us, then the recording method -- the same
            // column order the scalar line uses.
            .append('\t').append('\t')
            .append(recordingMethodName(r)).append('\t')
        for ((i, p) in points.withIndex()) {
            if (i > 0) {
                sb.append(',')
            }
            // Start and end are the same instant: every Health Connect
            // series measurement is a point reading, not an interval.
            sb.append(p.first).append(':').append(p.first).append(':')
                .append(p.second)
        }
        sb.append('\n')
    }

    private fun recordingMethodName(r: Record): String {
        return when (r.metadata.recordingMethod) {
            Metadata.RECORDING_METHOD_MANUAL_ENTRY -> "MANUAL_ENTRY"
            Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED -> "AUTOMATIC"
            Metadata.RECORDING_METHOD_ACTIVELY_RECORDED -> "ACTIVE"
            else -> "UNKNOWN"
        }
    }

    override fun aggregate(requestJson: String,
                           cb: HealthConnectDelegate.Callback) {
        // Aggregation is intentionally delegated to the shared code, which
        // already computes daylight-saving-correct bucket boundaries and
        // the duration-weighted average. Returning nothing here makes the
        // portable layer fall back to aggregating the raw samples it read,
        // which keeps one implementation of the arithmetic rather than two
        // that can disagree.
        run(cb) { "" }
    }

    override fun insertRecords(recordsTsv: String,
                               cb: HealthConnectDelegate.Callback) {
        run(cb) {
            val records = mutableListOf<Record>()
            recordsTsv.split('\n').forEach { raw ->
                toRecord(raw)?.let { records.add(it) }
            }
            if (records.isEmpty()) {
                ""
            } else {
                requireClient().insertRecords(records)
                    .recordIdsList.joinToString("\n")
            }
        }
    }

    private fun toRecord(line: String): Record? {
        if (line.isBlank()) {
            return null
        }
        val f = line.split('\t')
        if (f.size < 6) {
            return null
        }
        val token = f[1]
        val start = Instant.ofEpochMilli(f[2].toLong())
        val end = Instant.ofEpochMilli(f[3].toLong())
        val value = f[4].toDouble()
        val zone = java.time.ZoneId.systemDefault().rules.getOffset(start)
        // Field 6 is the portable RecordingMethod. Dropping it stored every
        // sample with the platform default, so other health apps could not
        // tell a value the user typed from one a device measured -- which
        // is the whole point of the field.
        val meta = metadataFor(if (f.size > 6) f[6] else "")
        return when (token) {
            "steps" -> StepsRecord(startTime = start, endTime = end,
                startZoneOffset = zone, endZoneOffset = zone,
                count = value.toLong(), metadata = meta)

            // Only the generic distance type is written. Writing a
            // cycling distance as a bare DistanceRecord would erase the
            // modality, and it would read back as walking distance.
            "distance_walking_running" -> DistanceRecord(startTime = start,
                endTime = end, startZoneOffset = zone, endZoneOffset = zone,
                distance = Length.meters(value), metadata = meta)

            "flights_climbed" -> FloorsClimbedRecord(startTime = start,
                endTime = end, startZoneOffset = zone, endZoneOffset = zone,
                floors = value, metadata = meta)

            "elevation_gained" -> ElevationGainedRecord(startTime = start,
                endTime = end, startZoneOffset = zone, endZoneOffset = zone,
                elevation = Length.meters(value), metadata = meta)

            "active_energy" -> ActiveCaloriesBurnedRecord(startTime = start,
                endTime = end, startZoneOffset = zone, endZoneOffset = zone,
                energy = Energy.kilocalories(value), metadata = meta)

            "wheelchair_pushes" -> WheelchairPushesRecord(startTime = start,
                endTime = end, startZoneOffset = zone, endZoneOffset = zone,
                count = value.toLong(), metadata = meta)

            "hydration" -> HydrationRecord(startTime = start, endTime = end,
                startZoneOffset = zone, endZoneOffset = zone,
                volume = Volume.liters(value), metadata = meta)

            "body_mass" -> WeightRecord(time = start, zoneOffset = zone,
                weight = Mass.kilograms(value), metadata = meta)

            "lean_body_mass" -> LeanBodyMassRecord(time = start,
                zoneOffset = zone, mass = Mass.kilograms(value),
                metadata = meta)

            "bone_mass" -> BoneMassRecord(time = start, zoneOffset = zone,
                mass = Mass.kilograms(value), metadata = meta)

            "body_fat_percentage" -> BodyFatRecord(time = start,
                zoneOffset = zone, percentage = Percentage(value),
                metadata = meta)

            "height" -> HeightRecord(time = start, zoneOffset = zone,
                height = Length.meters(value), metadata = meta)

            "resting_heart_rate" -> RestingHeartRateRecord(time = start,
                zoneOffset = zone, beatsPerMinute = value.toLong(),
                metadata = meta)

            "oxygen_saturation" -> OxygenSaturationRecord(time = start,
                zoneOffset = zone, percentage = Percentage(value),
                metadata = meta)

            "respiratory_rate" -> RespiratoryRateRecord(time = start,
                zoneOffset = zone, rate = value, metadata = meta)

            "body_temperature" -> BodyTemperatureRecord(time = start,
                zoneOffset = zone, temperature = Temperature.celsius(value),
                metadata = meta)

            "basal_body_temperature" -> BasalBodyTemperatureRecord(
                time = start, zoneOffset = zone,
                temperature = Temperature.celsius(value), metadata = meta)

            "vo2_max" -> Vo2MaxRecord(time = start, zoneOffset = zone,
                vo2MillilitersPerMinuteKilogram = value, metadata = meta)

            "blood_glucose" -> BloodGlucoseRecord(time = start,
                zoneOffset = zone,
                level = BloodGlucose.millimolesPerLiter(value), metadata = meta)

            // HeartRateRecord is an interval record and rejects
            // start == end in its constructor. A heart rate is an instant
            // reading, and every flattened series point is one too, so
            // without a span of its own every ordinary Android heart-rate
            // write threw before it reached the insert.
            "heart_rate" -> HeartRateRecord(startTime = start,
                endTime = if (end.isAfter(start)) end else start.plusMillis(1),
                startZoneOffset = zone, endZoneOffset = zone,
                samples = listOf(HeartRateRecord.Sample(time = start,
                    beatsPerMinute = value.toLong())), metadata = meta)

            else -> throw IllegalArgumentException(
                "Health Connect writes are not implemented for '" + token
                    + "' in this build")
        }
    }

    /**
     * Builds Health Connect metadata carrying the portable recording
     * method.
     *
     * Falls back to unspecified metadata when this connect-client release
     * does not expose the matching factory, which is better than refusing
     * the write.
     */
    private fun metadataFor(recordingMethod: String): Metadata {
        // connect-client models this as an int on Metadata rather than as
        // named factories, and the constructor's other parameters have
        // defaults, so only the recording method is supplied here.
        val method = when (recordingMethod) {
            "MANUAL_ENTRY" -> Metadata.RECORDING_METHOD_MANUAL_ENTRY
            "AUTOMATIC", "AUTOMATICALLY_RECORDED" ->
                Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED
            "ACTIVE", "ACTIVELY_RECORDED" ->
                Metadata.RECORDING_METHOD_ACTIVELY_RECORDED
            else -> Metadata.RECORDING_METHOD_UNKNOWN
        }
        return Metadata(recordingMethod = method)
    }

    override fun deleteRecords(requestJson: String,
                               cb: HealthConnectDelegate.Callback) {
        run(cb) {
            val json = JSONObject(requestJson)
            val types = json.getJSONArray("types")
            // Health Connect deletes by record class, so the portable layer
            // sends the type alongside the ids. Deleting the ids under a
            // guessed class would delete nothing and still report success.
            val classes = (0 until types.length())
                .map { types.getString(it) }
                .map { token ->
                    recordClassFor(token) ?: throw IllegalArgumentException(
                        "Health Connect deletes are not implemented for '"
                            + token + "' in this build")
                }
            if (json.has("ids")) {
                val ids = json.getJSONArray("ids")
                val list = (0 until ids.length()).map { ids.getString(it) }
                classes.forEach {
                    requireClient().deleteRecords(it, list, emptyList())
                }
                // Health Connect reports no affected-row count, and an id
                // that is stale or owned by another app deletes nothing
                // while still succeeding. Returning the id count claimed
                // deletions that may not have happened.
                "-1"
            } else {
                val filter = TimeRangeFilter.between(
                    Instant.ofEpochMilli(json.getLong("start")),
                    Instant.ofEpochMilli(json.getLong("end")))
                classes.forEach {
                    requireClient().deleteRecords(it, filter)
                }
                // Health Connect does not report how many records a range
                // delete removed, so report the honest unknown rather than
                // a fabricated count.
                "-1"
            }
        }
    }

    override fun getChangesToken(typesCsv: String,
                                 cb: HealthConnectDelegate.Callback) {
        run(cb) {
            val types = typesCsv.split(",").filter { it.isNotBlank() }
                .mapNotNull { recordClassFor(it.trim()) }.toSet()
            if (types.isEmpty()) {
                ""
            } else {
                requireClient().getChangesToken(ChangesTokenRequest(types))
            }
        }
    }

    override fun getChanges(token: String,
                            cb: HealthConnectDelegate.Callback) {
        run(cb) {
            val changes = requireClient().getChanges(token)
            val sb = StringBuilder()
            sb.append(changes.nextChangesToken).append('\t')
                .append(if (changes.changesTokenExpired) "1" else "0")
                .append('\t')
                .append(if (changes.hasMore) "1" else "0").append('\n')
            // The page body was previously discarded, which silently threw
            // away every change in the batch while still advancing the
            // token, so the data could never be recovered on a later poll.
            changes.changes.forEach { change ->
                when (change) {
                    is UpsertionChange -> appendChangedRecord(sb, "+",
                        change.record)

                    is DeletionChange -> sb.append("-").append('\t')
                        .append(change.recordId).append('\n')

                    else -> {}
                }
            }
            sb.toString()
        }
    }

    /**
     * Emits one upserted record as ordinary sample lines prefixed with
     * `op`, so a drained change carries its values rather than only an id
     * the caller would have to re-query for.
     *
     * A record shape with no single-value form is still reported, as an
     * identity-only line, so the caller learns something changed instead
     * of the change being dropped.
     */
    private fun appendChangedRecord(sb: StringBuilder, op: String,
                                    record: Record) {
        val token = TOKEN_FOR_RECORD[record.javaClass.simpleName]
        val body = StringBuilder()
        // Always flattened. A subscription has no query on it to carry the
        // option, and the change page is a notification of what moved
        // rather than a shaped read.
        if (token == null || appendOne(body, record, token, true) == 0) {
            sb.append(op).append("\t").append(record.metadata.id)
                .append('\t').append(token ?: "").append('\n')
            return
        }
        body.toString().split('\n').forEach {
            if (it.isNotBlank()) {
                sb.append(op).append('\t').append(it).append('\n')
            }
        }
    }

    /**
     * Record classes the line format can carry values for.
     *
     * Session-shaped records are deliberately absent: a sleep session or a
     * workout is not a single number and squeezing one into a value line
     * would misreport it. They are still deletable and still register for
     * change notifications, which is why the two maps differ.
     */
    private fun readableRecordClassFor(token: String) = when (token) {
        "sleep", "workout" -> null
        else -> recordClassFor(token)
    }

    /**
     * Health Connect's own ceiling on `ReadRecordsRequest.pageSize`. A
     * larger value is not clamped by the library, it is rejected.
     */
    private val MAX_PAGE_SIZE = 5000

    private fun recordClassFor(token: String) = when (token) {
        "steps" -> StepsRecord::class
        // Health Connect has a single DistanceRecord with no modality
        // field, so only the generic distance type maps onto it. Aliasing
        // the cycling and swimming types here would return the same records
        // under whichever label was asked for -- duplicating them when both
        // are queried, and reporting a cycle as a run when only one is.
        "distance_walking_running" -> DistanceRecord::class
        "flights_climbed" -> FloorsClimbedRecord::class
        "elevation_gained" -> ElevationGainedRecord::class
        "active_energy" -> ActiveCaloriesBurnedRecord::class
        "wheelchair_pushes" -> WheelchairPushesRecord::class
        "hydration" -> HydrationRecord::class
        "heart_rate" -> HeartRateRecord::class
        "resting_heart_rate" -> RestingHeartRateRecord::class
        "oxygen_saturation" -> OxygenSaturationRecord::class
        "respiratory_rate" -> RespiratoryRateRecord::class
        "body_temperature" -> BodyTemperatureRecord::class
        "basal_body_temperature" -> BasalBodyTemperatureRecord::class
        "vo2_max" -> Vo2MaxRecord::class
        "blood_glucose" -> BloodGlucoseRecord::class
        "body_mass" -> WeightRecord::class
        "lean_body_mass" -> LeanBodyMassRecord::class
        "bone_mass" -> BoneMassRecord::class
        "body_fat_percentage" -> BodyFatRecord::class
        "height" -> HeightRecord::class
        "power" -> PowerRecord::class
        "speed" -> SpeedRecord::class
        "cycling_cadence" -> CyclingPedalingCadenceRecord::class
        "running_cadence" -> StepsCadenceRecord::class
        "sleep" -> SleepSessionRecord::class
        "workout" -> ExerciseSessionRecord::class
        else -> null
    }

    private val TOKEN_FOR_RECORD: Map<String, String> = mapOf(
        "StepsRecord" to "steps",
        "DistanceRecord" to "distance_walking_running",
        "FloorsClimbedRecord" to "flights_climbed",
        "ElevationGainedRecord" to "elevation_gained",
        "ActiveCaloriesBurnedRecord" to "active_energy",
        "WheelchairPushesRecord" to "wheelchair_pushes",
        "HydrationRecord" to "hydration",
        "HeartRateRecord" to "heart_rate",
        "RestingHeartRateRecord" to "resting_heart_rate",
        "OxygenSaturationRecord" to "oxygen_saturation",
        "RespiratoryRateRecord" to "respiratory_rate",
        "BodyTemperatureRecord" to "body_temperature",
        "BasalBodyTemperatureRecord" to "basal_body_temperature",
        "Vo2MaxRecord" to "vo2_max",
        "BloodGlucoseRecord" to "blood_glucose",
        "WeightRecord" to "body_mass",
        "LeanBodyMassRecord" to "lean_body_mass",
        "BoneMassRecord" to "bone_mass",
        "BodyFatRecord" to "body_fat_percentage",
        "HeightRecord" to "height",
        "PowerRecord" to "power",
        "SpeedRecord" to "speed",
        "CyclingPedalingCadenceRecord" to "cycling_cadence",
        "StepsCadenceRecord" to "running_cadence",
        "SleepSessionRecord" to "sleep",
        "ExerciseSessionRecord" to "workout")

    /**
     * Portable token to Health Connect permission suffix.
     *
     * This table must stay identical to `PERMISSION_SUFFIX` in
     * `HealthManifestFragments`, which decides what the manifest declares.
     * A token missing here but present there produces an app that holds a
     * permission it can never ask for; the reverse produces a request for
     * a permission the manifest never declared, which Health Connect
     * rejects outright. `HealthBridgeTokenTableTest` parses this file and
     * fails the build when the two drift.
     */
    private val PERMISSION_SUFFIX: Map<String, String> = mapOf(
        "steps" to "STEPS",
        "distance_walking_running" to "DISTANCE",
        "distance_cycling" to "DISTANCE",
        "distance_swimming" to "DISTANCE",
        "flights_climbed" to "FLOORS_CLIMBED",
        "elevation_gained" to "ELEVATION_GAINED",
        "active_energy" to "ACTIVE_CALORIES_BURNED",
        "basal_energy" to "BASAL_METABOLIC_RATE",
        "exercise_time" to "EXERCISE",
        "wheelchair_pushes" to "WHEELCHAIR_PUSHES",
        "heart_rate" to "HEART_RATE",
        "resting_heart_rate" to "RESTING_HEART_RATE",
        "walking_heart_rate_average" to "HEART_RATE",
        "heart_rate_variability_sdnn" to "HEART_RATE_VARIABILITY",
        "oxygen_saturation" to "OXYGEN_SATURATION",
        "respiratory_rate" to "RESPIRATORY_RATE",
        "body_temperature" to "BODY_TEMPERATURE",
        "basal_body_temperature" to "BASAL_BODY_TEMPERATURE",
        "vo2_max" to "VO2_MAX",
        "blood_pressure" to "BLOOD_PRESSURE",
        "blood_glucose" to "BLOOD_GLUCOSE",
        "body_mass" to "WEIGHT",
        "lean_body_mass" to "LEAN_BODY_MASS",
        "bone_mass" to "BONE_MASS",
        "body_fat_percentage" to "BODY_FAT",
        "height" to "HEIGHT",
        "waist_circumference" to "BODY_MEASUREMENTS",
        "power" to "POWER",
        "speed" to "SPEED",
        "cycling_cadence" to "CYCLING_PEDALING_CADENCE",
        "running_cadence" to "STEPS_CADENCE",
        "hydration" to "HYDRATION",
        "dietary_energy" to "NUTRITION",
        "nutrition" to "NUTRITION",
        "sleep" to "SLEEP",
        "workout" to "EXERCISE",
        "mindful_session" to "MINDFULNESS",
        "menstruation_flow" to "MENSTRUATION",
        "intermenstrual_bleeding" to "INTERMENSTRUAL_BLEEDING")

    /**
     * Health Connect permission suffix back to every portable token that
     * maps onto it.
     *
     * The relation is many-to-one: one `READ_DISTANCE` grant covers the
     * walking, cycling and swimming distance types. Reporting only one of
     * them would make an app that asked for `distance_cycling` believe its
     * request was refused, so a grant is reported for all of them.
     */
    private val TOKENS_FOR_SUFFIX: Map<String, List<String>> =
        PERMISSION_SUFFIX.entries.groupBy({ it.value }, { it.key })

    /** Portable token to Health Connect permission string. */
    private fun toHealthPermission(token: String): String? {
        val write = token.startsWith("w:")
        val name = token.removePrefix("r:").removePrefix("w:")
        val suffix = PERMISSION_SUFFIX[name] ?: return null
        return "android.permission.health." +
            (if (write) "WRITE_" else "READ_") + suffix
    }

    /**
     * Health Connect permission string back to portable tokens.
     *
     * The suffix is taken by stripping the direction prefix, not by
     * splitting on the last underscore: `READ_ACTIVE_CALORIES_BURNED`
     * would otherwise yield `burned`.
     */
    private fun toTokens(permission: String): List<String> {
        val name = permission.substringAfterLast('.')
        val write = name.startsWith("WRITE_")
        val suffix = name.removePrefix("READ_").removePrefix("WRITE_")
        val prefix = if (write) "w:" else "r:"
        val tokens = TOKENS_FOR_SUFFIX[suffix] ?: return emptyList()
        return tokens.map { prefix + it }
    }
}
