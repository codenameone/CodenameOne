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
package keynote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

enum class Track(val label: String, val color: Long) {
    Language("Language", 0xFF7F52FF),
    Multiplatform("Multiplatform", 0xFF0095D5),
    Server("Server", 0xFF2E7D32),
    Tooling("Tooling", 0xFFE65100),
    UI("UI", 0xFFC2185B)
}

data class Speaker(val name: String, val company: String)

data class Session(
    val id: Int,
    val title: String,
    val speaker: Speaker,
    val track: Track,
    val day: Int,
    val startMinutes: Int,
    val lengthMinutes: Int,
    val room: String,
    val description: String
) {
    val timeLabel: String get() = clock(startMinutes) + " - " + clock(startMinutes + lengthMinutes)
}

fun clock(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return (if (h < 10) "0" else "") + h + ":" + (if (m < 10) "0" else "") + m
}

/// The conference programme. Generated from a fixed seed, so every build and
/// every run of the benchmark sees the same 150 sessions.
object Programme {
    private val first = listOf("Ada", "Linus", "Grace", "Alan", "Barbara", "Ken", "Margaret", "Dennis", "Frances", "Edsger",
        "Radia", "Bjarne", "Sophie", "Guido", "Hedy", "James")
    private val last = listOf("Lovelace", "Hopper", "Turing", "Liskov", "Thompson", "Hamilton", "Ritchie", "Allen",
        "Dijkstra", "Perlman", "Wilson", "Lamarr", "Gosling", "Kay", "Knuth", "Backus")
    private val companies = listOf("JetBrains", "Google", "Square", "Netflix", "Touchlab", "Cash App", "Shopify", "Zalando")
    private val topics = listOf("Coroutines", "Flows", "Compose", "K2", "Ktor", "Gradle", "Serialization", "Wasm",
        "Native interop", "Context parameters", "Value classes", "Multiplatform libraries", "Testing", "Performance",
        "Accessibility", "Animations", "State hoisting", "Server-side Kotlin", "Build speed", "Code sharing")
    private val shapes = listOf("Deep dive into %s", "%s in production", "What's new in %s", "%s: lessons learned",
        "Mastering %s", "%s from scratch", "The hidden cost of %s", "%s at scale")
    private val rooms = listOf("Auditorium", "Room 1", "Room 2", "Workshop A", "Lightning stage")

    val sessions: List<Session> by lazy { generate() }

    private fun generate(): List<Session> {
        var seed = 20261025L
        fun next(n: Int): Int {
            seed = (seed * 6364136223846793005L + 1442695040888963407L)
            return ((seed ushr 33) % n).toInt()
        }
        val out = ArrayList<Session>()
        var id = 0
        for (day in 1..3) {
            var start = 9 * 60
            while (out.count { it.day == day } < 50) {
                for (room in rooms) {
                    if (out.count { it.day == day } >= 50) break
                    val topic = topics[next(topics.size)]
                    val title = shapes[next(shapes.size)].replace("%s", topic)
                    val speaker = Speaker(first[next(first.size)] + " " + last[next(last.size)], companies[next(companies.size)])
                    val len = if (room == "Lightning stage") 15 else if (room.startsWith("Workshop")) 90 else 45
                    out.add(Session(id++, title, speaker, Track.entries[next(Track.entries.size)], day, start, len, room,
                        "$title. ${speaker.name} of ${speaker.company} walks through $topic: the model behind it, " +
                            "the mistakes teams make with it, and the patterns that hold up in real applications. " +
                            "Bring questions; the last ten minutes are open Q&A."))
                }
                start += 60
            }
        }
        return out
    }

    /// The programme as a feed would deliver it: off the main thread, after a
    /// network-like delay.
    fun load(): Flow<List<Session>> = flow {
        delay(150)
        emit(sessions)
    }.flowOn(Dispatchers.Default)
}
