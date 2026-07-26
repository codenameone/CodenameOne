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
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
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
            granted.joinToString(",") { toToken(it) }
        }
    }

    override fun permissionIntent(permissionsCsv: String): Intent {
        val perms = permissionsCsv.split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { toHealthPermission(it.trim()) }
            .toSet()
        return PermissionController.createRequestPermissionResultContract()
            .createIntent(context, perms)
    }

    override fun parsePermissionResult(resultCode: Int,
                                       data: Intent?): String {
        return try {
            val granted = PermissionController
                .createRequestPermissionResultContract()
                .parseResult(resultCode, data)
            granted.joinToString(",") { toToken(it) }
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
            val sb = StringBuilder()
            for (i in 0 until types.length()) {
                appendRecords(sb, types.getString(i), filter,
                    json.optInt("limit", 10000))
            }
            sb.toString()
        }
    }

    /** Reads one portable type and appends it in the shared line format. */
    private suspend fun appendRecords(sb: StringBuilder, token: String,
                                      filter: TimeRangeFilter, limit: Int) {
        when (token) {
            "steps" -> requireClient().readRecords(
                ReadRecordsRequest(StepsRecord::class,
                    timeRangeFilter = filter, pageSize = limit)
            ).records.forEach {
                line(sb, it.metadata.id, token, it.startTime.toEpochMilli(),
                    it.endTime.toEpochMilli(), it.count.toDouble(), "count")
            }

            "heart_rate" -> requireClient().readRecords(
                ReadRecordsRequest(HeartRateRecord::class,
                    timeRangeFilter = filter, pageSize = limit)
            ).records.forEach { record ->
                // One Health Connect record holds many samples; the
                // portable layer flattens by default, so emit one line per
                // sample rather than per record.
                record.samples.forEach { s ->
                    line(sb, record.metadata.id, token,
                        s.time.toEpochMilli(), s.time.toEpochMilli(),
                        s.beatsPerMinute.toDouble(), "count/min")
                }
            }

            "body_mass" -> requireClient().readRecords(
                ReadRecordsRequest(WeightRecord::class,
                    timeRangeFilter = filter, pageSize = limit)
            ).records.forEach {
                line(sb, it.metadata.id, token, it.time.toEpochMilli(),
                    it.time.toEpochMilli(), it.weight.inKilograms, "kg")
            }
        }
    }

    private fun line(sb: StringBuilder, id: String, type: String,
                     start: Long, end: Long, value: Double, unit: String) {
        sb.append(id).append('\t').append(type).append('\t')
            .append(start).append('\t').append(end).append('\t')
            .append(value).append('\t').append(unit).append('\t')
            .append("UNKNOWN").append('\n')
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
        val start = Instant.ofEpochMilli(f[2].toLong())
        val end = Instant.ofEpochMilli(f[3].toLong())
        val value = f[4].toDouble()
        val zone = java.time.ZoneId.systemDefault().rules.getOffset(start)
        return when (f[1]) {
            "steps" -> StepsRecord(startTime = start, endTime = end,
                startZoneOffset = zone, endZoneOffset = zone,
                count = value.toLong())

            "body_mass" -> WeightRecord(time = start, zoneOffset = zone,
                weight = Mass.kilograms(value))

            else -> null
        }
    }

    override fun deleteRecords(requestJson: String,
                               cb: HealthConnectDelegate.Callback) {
        run(cb) {
            val json = JSONObject(requestJson)
            if (json.has("ids")) {
                val ids = json.getJSONArray("ids")
                val list = (0 until ids.length()).map { ids.getString(it) }
                requireClient().deleteRecords(StepsRecord::class, list,
                    emptyList())
                list.size.toString()
            } else {
                "0"
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
            sb.append(changes.nextChangesToken).append('\n')
            sb.toString()
        }
    }

    private fun recordClassFor(token: String) = when (token) {
        "steps" -> StepsRecord::class
        "heart_rate" -> HeartRateRecord::class
        "body_mass" -> WeightRecord::class
        else -> null
    }

    /** Portable token to Health Connect permission string. */
    private fun toHealthPermission(token: String): String? {
        val write = token.startsWith("w:")
        val name = token.removePrefix("r:").removePrefix("w:")
        val suffix = when (name) {
            "steps" -> "STEPS"
            "heart_rate" -> "HEART_RATE"
            "body_mass" -> "WEIGHT"
            "sleep" -> "SLEEP"
            "workout" -> "EXERCISE"
            "distance_walking_running", "distance_cycling",
            "distance_swimming" -> "DISTANCE"
            "active_energy" -> "ACTIVE_CALORIES_BURNED"
            "blood_glucose" -> "BLOOD_GLUCOSE"
            "blood_pressure" -> "BLOOD_PRESSURE"
            "oxygen_saturation" -> "OXYGEN_SATURATION"
            else -> return null
        }
        return "android.permission.health." +
            (if (write) "WRITE_" else "READ_") + suffix
    }

    /** Health Connect permission string back to a portable token. */
    private fun toToken(permission: String): String {
        val write = permission.contains(".WRITE_")
        val suffix = permission.substringAfterLast('_')
        val name = when {
            permission.endsWith("STEPS") -> "steps"
            permission.endsWith("HEART_RATE") -> "heart_rate"
            permission.endsWith("WEIGHT") -> "body_mass"
            permission.endsWith("SLEEP") -> "sleep"
            permission.endsWith("EXERCISE") -> "workout"
            permission.endsWith("DISTANCE") -> "distance_walking_running"
            else -> suffix.lowercase()
        }
        return (if (write) "w:" else "r:") + name
    }
}
