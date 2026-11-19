/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates.
 *
 * This software is dual-licensed to you under the Universal Permissive License
 * (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl and Apache License
 * 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
 * either license.
 *
 * If you elect to accept the software under the Apache License, Version 2.0,
 * the following applies:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Rewritten in Java for Codename One from python-oracledb's thin mode
 * (src/oracledb/impl/thin: packet.pyx, messages.pyx, capabilities.pyx,
 * data_types.pyx, crypto.pyx, constants.pxi). The protocol layout -- packet
 * framing, the TTC messages, the capability and data type tables and the
 * O5LOGON key derivation -- follows that implementation; the code is new.
 * Changes and additions are Copyright (c) Codename One and offered under the
 * same choice of licenses.
 */
package com.codename1.backend.sql;

import com.codename1.backend.ConcurrencyFailureException;
import com.codename1.backend.Crypto;
import com.codename1.backend.DataIntegrityViolationException;
import com.codename1.backend.Tcp;
import com.codename1.backend.annotations.Isolation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// An Oracle Database client speaking TNS and TTC, the protocol of Oracle's
/// own "thin" drivers. Oracle Database 19c and later.
///
/// ```java
/// Oracle db = Oracle.connect("db.internal", 1521, "FREEPDB1", "app", "secret",
///         "disable", null, 10000, 0);
/// List rows = db.query("SELECT id, name FROM users WHERE id = :1", new Object[] {7L});
/// ```
///
/// #### Statements
///
/// Parameters are written `:1`, `:2` and so on and bound by number: the same
/// number used twice binds the same value twice. A DML statement that ends in
/// `RETURNING ... INTO :n` answers [#query] with one row holding what it
/// returned. Outside a transaction every statement commits as it completes,
/// as on the other engines.
///
/// #### Encryption
///
/// `sslmode` `require`, `verify-full` and `verify-ca` connect over TLS
/// (TCPS), which is a listener port of its own, usually 2484. Without TLS the
/// password still never crosses the network: the O5LOGON exchange proves it
/// with keys derived from it. Oracle's Native Network Encryption is not
/// implemented, and neither are wallets.
///
/// #### Values
///
/// A NUMBER that is a whole number within the range of a `long` arrives as
/// `Long`, any other NUMBER as its exact digits in a `String`. BINARY_DOUBLE
/// and BINARY_FLOAT arrive as `Double`, RAW and BLOB as `byte[]`, text and
/// CLOB as `String`, and DATE and TIMESTAMP as `yyyy-MM-dd HH:mm:ss[.fff]`.
public final class Oracle {
    // Packet types.
    private static final int PACKET_CONNECT = 1;
    private static final int PACKET_ACCEPT = 2;
    private static final int PACKET_REFUSE = 4;
    private static final int PACKET_REDIRECT = 5;
    private static final int PACKET_DATA = 6;
    private static final int PACKET_RESEND = 11;
    private static final int PACKET_MARKER = 12;

    private static final int DATA_FLAGS_EOF = 0x40;

    // Message types.
    private static final int MSG_PROTOCOL = 1;
    private static final int MSG_DATA_TYPES = 2;
    private static final int MSG_FUNCTION = 3;
    private static final int MSG_ERROR = 4;
    private static final int MSG_ROW_HEADER = 6;
    private static final int MSG_ROW_DATA = 7;
    private static final int MSG_PARAMETER = 8;
    private static final int MSG_STATUS = 9;
    private static final int MSG_WARNING = 15;
    private static final int MSG_DESCRIBE_INFO = 16;
    private static final int MSG_FLUSH_OUT_BINDS = 19;
    private static final int MSG_PIGGYBACK = 17;
    private static final int MSG_BIT_VECTOR = 21;
    private static final int MSG_SERVER_PIGGYBACK = 23;
    private static final int MSG_END_OF_RESPONSE = 29;

    // Function codes.
    private static final int FUNC_FETCH = 5;
    private static final int FUNC_EXECUTE = 94;
    private static final int FUNC_CLOSE_CURSORS = 105;
    private static final int FUNC_AUTH_PHASE_TWO = 115;
    private static final int FUNC_AUTH_PHASE_ONE = 118;

    // Execute options.
    private static final int EXEC_PARSE = 0x01;
    private static final int EXEC_BIND = 0x08;
    private static final int EXEC_DEFINE = 0x10;
    private static final int EXEC_EXECUTE = 0x20;
    private static final int EXEC_FETCH = 0x40;
    private static final int EXEC_COMMIT = 0x100;
    private static final int EXEC_PLSQL_BIND = 0x400;
    private static final int EXEC_NOT_PLSQL = 0x8000;

    // Data types.
    static final int TYPE_VARCHAR = 1;
    static final int TYPE_NUMBER = 2;
    static final int TYPE_LONG = 8;
    static final int TYPE_ROWID = 11;
    static final int TYPE_DATE = 12;
    static final int TYPE_RAW = 23;
    static final int TYPE_LONG_RAW = 24;
    static final int TYPE_CHAR = 96;
    static final int TYPE_BINARY_FLOAT = 100;
    static final int TYPE_BINARY_DOUBLE = 101;
    static final int TYPE_CLOB = 112;
    static final int TYPE_BLOB = 113;
    static final int TYPE_TIMESTAMP = 180;
    static final int TYPE_TIMESTAMP_TZ = 181;
    static final int TYPE_UROWID = 208;
    static final int TYPE_TIMESTAMP_LTZ = 231;
    static final int TYPE_BOOLEAN = 252;

    private static final int CHARSET_UTF8 = 873;
    private static final int CSFRM_IMPLICIT = 1;
    private static final int CSFRM_NCHAR = 2;

    private static final int VERIFIER_11G_1 = 0xb152;
    private static final int VERIFIER_11G_2 = 0x1b25;
    private static final int VERIFIER_12C = 0x4815;

    private static final long MAX_LONG_LENGTH = 0x7fffffffL;
    /// The longest VARCHAR2 a bind may be before it travels as a LONG, the
    /// server's limit under the default MAX_STRING_SIZE.
    private static final int MAX_STRING = 4000;
    private static final int MAX_RAW = 2000;
    private static final int PREFETCH = 100;
    private static final int SDU = 8192;

    /// The field version this client speaks, Oracle's 23 with its first
    /// extension. The server answers with its own and the lower one is used.
    private static final int FIELD_VERSION = 24;
    private static final int FIELD_VERSION_12_2 = 8;
    private static final int FIELD_VERSION_12_2_EXT1 = 9;
    private static final int FIELD_VERSION_18_1_EXT_1 = 14;
    private static final int FIELD_VERSION_19_1 = 17;
    private static final int FIELD_VERSION_21_1 = 20;
    private static final int FIELD_VERSION_23_1 = 18;
    private static final int FIELD_VERSION_23_1_EXT_1 = 24;

    /// The data types this client declares it reads, as triples of type,
    /// converted type and representation, from data_types.pyx.
    private static final int[] DATA_TYPES = {
        1, 1, 1, 2, 2, 10, 8, 8, 1, 12, 12, 10, 23, 23, 1, 24, 24, 1, 25, 25, 1, 26, 26, 1, 27,
        27, 10, 28, 28, 1, 29, 29, 1, 30, 30, 1, 31, 31, 1, 32, 32, 1, 33, 33, 1, 10, 10, 1, 11,
        11, 1, 40, 40, 1, 41, 41, 1, 117, 117, 1, 120, 120, 1, 290, 290, 1, 291, 291, 1, 292,
        292, 1, 293, 293, 1, 294, 294, 1, 298, 298, 1, 299, 299, 1, 300, 300, 1, 301, 301, 1,
        302, 302, 1, 303, 303, 1, 304, 304, 1, 305, 305, 1, 306, 306, 1, 307, 307, 1, 308, 308,
        1, 309, 309, 1, 310, 310, 1, 311, 311, 1, 312, 312, 1, 313, 313, 1, 315, 315, 1, 316,
        316, 1, 317, 317, 1, 318, 318, 1, 319, 319, 1, 320, 320, 1, 321, 321, 1, 322, 322, 1,
        323, 323, 1, 327, 327, 1, 328, 328, 1, 329, 329, 1, 331, 331, 1, 333, 333, 1, 334, 334,
        1, 335, 335, 1, 336, 336, 1, 337, 337, 1, 338, 338, 1, 339, 339, 1, 340, 340, 1, 341,
        341, 1, 342, 342, 1, 343, 343, 1, 344, 344, 1, 345, 345, 1, 346, 346, 1, 348, 348, 1,
        349, 349, 1, 354, 354, 1, 355, 355, 1, 359, 359, 1, 363, 363, 1, 380, 380, 1, 381, 381,
        1, 382, 382, 1, 383, 383, 1, 384, 384, 1, 385, 385, 1, 386, 386, 1, 387, 387, 1, 388,
        388, 1, 389, 389, 1, 390, 390, 1, 391, 391, 1, 393, 393, 1, 394, 394, 1, 395, 395, 1,
        396, 396, 1, 397, 397, 1, 398, 398, 1, 399, 399, 1, 400, 400, 1, 401, 401, 1, 404, 404,
        1, 405, 405, 1, 406, 406, 1, 407, 407, 1, 413, 413, 1, 414, 414, 1, 415, 415, 1, 416,
        416, 1, 417, 417, 1, 418, 418, 1, 419, 419, 1, 420, 420, 1, 421, 421, 1, 422, 422, 1,
        423, 423, 1, 424, 424, 1, 425, 425, 1, 426, 426, 1, 427, 427, 1, 429, 429, 1, 430, 430,
        1, 431, 431, 1, 432, 432, 1, 433, 433, 1, 449, 449, 1, 450, 450, 1, 454, 454, 1, 455,
        455, 1, 456, 456, 1, 457, 457, 1, 458, 458, 1, 459, 459, 1, 460, 460, 1, 461, 461, 1,
        462, 462, 1, 463, 463, 1, 466, 466, 1, 467, 467, 1, 468, 468, 1, 469, 469, 1, 470, 470,
        1, 471, 471, 1, 472, 472, 1, 473, 473, 1, 474, 474, 1, 475, 475, 1, 476, 476, 1, 477,
        477, 1, 478, 478, 1, 479, 479, 1, 480, 480, 1, 481, 481, 1, 482, 482, 1, 483, 483, 1,
        484, 484, 1, 485, 485, 1, 486, 486, 1, 490, 490, 1, 491, 491, 1, 492, 492, 1, 493, 493,
        1, 494, 494, 1, 495, 495, 1, 496, 496, 1, 498, 498, 1, 499, 499, 1, 500, 500, 1, 501,
        501, 1, 502, 502, 1, 509, 509, 1, 510, 510, 1, 513, 513, 1, 514, 514, 1, 516, 516, 1,
        517, 517, 1, 518, 518, 1, 519, 519, 1, 520, 520, 1, 521, 521, 1, 522, 522, 1, 523, 523,
        1, 524, 524, 1, 525, 525, 1, 526, 526, 1, 527, 527, 1, 528, 528, 1, 529, 529, 1, 530,
        530, 1, 531, 531, 1, 532, 532, 1, 533, 533, 1, 534, 534, 1, 535, 535, 1, 536, 536, 1,
        537, 537, 1, 538, 538, 1, 539, 539, 1, 540, 540, 1, 541, 541, 1, 542, 542, 1, 543, 543,
        1, 560, 560, 1, 565, 565, 1, 572, 572, 1, 573, 573, 1, 574, 574, 1, 575, 575, 1, 576,
        576, 1, 578, 578, 1, 563, 563, 1, 564, 564, 1, 579, 579, 1, 580, 580, 1, 581, 581, 1,
        582, 582, 1, 583, 583, 1, 584, 584, 1, 585, 585, 1, 3, 2, 10, 4, 2, 10, 5, 1, 1, 6, 2,
        10, 7, 2, 10, 9, 1, 1, 15, 1, 1, 39, 39, 1, 68, 2, 10, 91, 2, 10, 94, 1, 1, 95, 23, 1,
        96, 96, 1, 97, 96, 1, 100, 100, 1, 101, 101, 1, 102, 102, 1, 104, 11, 1, 106, 106, 1,
        108, 109, 1, 109, 109, 1, 110, 111, 1, 111, 111, 1, 112, 112, 1, 113, 113, 1, 114, 114,
        1, 115, 115, 1, 116, 102, 1, 119, 119, 1, 198, 198, 1, 146, 146, 1, 152, 2, 10, 153, 2,
        10, 154, 2, 10, 155, 1, 1, 156, 12, 10, 172, 2, 10, 178, 178, 1, 179, 179, 1, 180, 180,
        1, 181, 181, 1, 182, 182, 1, 183, 183, 1, 184, 12, 10, 185, 185, 1, 186, 186, 1, 187,
        187, 1, 188, 188, 1, 189, 189, 1, 190, 190, 1, 195, 112, 1, 196, 113, 1, 197, 114, 1,
        208, 208, 1, 231, 231, 1, 232, 231, 1, 233, 233, 1, 241, 109, 1, 252, 252, 1, 590, 590,
        1, 591, 591, 1, 592, 592, 1, 613, 613, 1, 614, 614, 1, 615, 615, 1, 616, 616, 1, 611,
        611, 1, 612, 612, 1, 593, 593, 1, 594, 594, 1, 595, 595, 1, 596, 596, 1, 597, 597, 1,
        598, 598, 1, 599, 599, 1, 600, 600, 1, 601, 601, 1, 602, 602, 1, 603, 603, 1, 604, 604,
        1, 605, 605, 1, 622, 622, 1, 623, 623, 1, 624, 624, 1, 625, 625, 1, 626, 626, 1, 627,
        627, 1, 628, 628, 1, 629, 629, 1, 630, 630, 1, 631, 631, 1, 632, 632, 1, 637, 637, 1,
        638, 638, 1, 636, 636, 1, 639, 639, 1, 663, 663, 1, 640, 640, 1, 652, 652, 1, 646, 646,
        1, 127, 127, 1, 660, 660, 1, 661, 661, 1, 665, 665, 1
    };

    private final Tcp connection;
    private final Wire wire;
    /// VOLATILE: isClosed() is read by whoever is deciding whether to reuse this
    /// session -- a pool, a health check -- from a thread that does not run its
    /// statements.
    private volatile boolean closed; //NOPMD AvoidUsingVolatile - read by a pool's thread, see above

    private boolean largeSdu;
    private int sdu = SDU;
    private int fieldVersion = FIELD_VERSION;
    private int sequence;
    private boolean inTransaction;
    private final List cursorsToClose = new ArrayList();

    // The response being read: the current DATA packet's payload.
    private byte[] packet = new byte[0];
    private int position;
    private int packetType;

    // What a response left behind for its caller.
    private int errorNumber;
    private String errorMessage;
    private long rowCount;
    private int cursorId;

    // The statement being read: its columns and the rows so far.
    private Column[] columns;
    private byte[] bitVector;
    private Object[] previousRow;
    private boolean moreRows;
    private int[] outTypes;
    private Object[] returned;

    private Oracle(Tcp connection) {
        this.connection = connection;
        this.wire = new Wire(connection);
    }

    /// One column of a result, from the server's description of it.
    static final class Column {
        String name;
        int type;
        int csfrm;
        int scale;
        long bufferSize;
    }

    /// Opens a session to the service `service` on a listener.
    ///
    /// `sslMode` is what the other engines take, except that a connection
    /// without TLS is a plain listener port and never an attempt that falls
    /// back: `require` and `verify-full` demand TLS to a certificate that
    /// verifies for `host`, and `verify-ca` a chain to `caFile` without the name.
    /// Without TLS the password is still never sent, but an active attacker
    /// can pick the verifier and its iteration count and crack the answer
    /// offline, and every row crosses in the clear, so plaintext is only
    /// `disable`, written deliberately; an absent mode or `prefer` is refused,
    /// as SQL Server refuses it.
    public static Oracle connect(String host, int port, String service, String user,
            String password, String sslMode, String caFile, int timeoutMillis,
            int socketTimeoutMillis) throws IOException {
        String mode = SqlLimits.canonicalSslMode(sslMode == null ? "prefer" : sslMode);
        if ("prefer".equals(mode)) {
            throw new IOException("Oracle serves TLS on a listener port of its own and never "
                    + "falls back, so sslmode=prefer cannot mean anything here. Set sslmode=require "
                    + "for a TCPS listener (usually port 2484), or sslmode=disable to connect in "
                    + "the clear deliberately.");
        }
        SqlLimits.requireRootForVerifyCa(mode, caFile);
        if (service == null || service.length() == 0) {
            throw new IOException("An Oracle URL names its service after the port: "
                    + "oracle://user:password@host:1521/SERVICE");
        }
        checkDescriptorValue("host", host);
        checkDescriptorValue("service", service);
        String targetHost = host;
        int targetPort = port <= 0 ? 1521 : port;
        String descriptor = null;
        for (int hop = 0 ; ; hop++) {
            Tcp tcp = Tcp.connect(targetHost, targetPort, timeoutMillis);
            Oracle session = new Oracle(tcp);
            try {
                if (socketTimeoutMillis > 0) {
                    tcp.setReadTimeout(socketTimeoutMillis);
                }
                if (SqlLimits.demandsTls(mode)) {
                    tcp.startTls(host, caFile, SqlLimits.verifiesHost(mode));
                }
                String sent = descriptor != null ? descriptor
                        : descriptor(targetHost, targetPort, service,
                                SqlLimits.demandsTls(mode));
                String[] redirect = session.connectPacket(sent);
                if (redirect != null) {
                    tcp.close();
                    if (hop > 0) {
                        throw new IOException("The listener at " + host
                                + " redirected the connection twice");
                    }
                    targetHost = redirect[0];
                    targetPort = Integer.parseInt(redirect[1]);
                    descriptor = redirect[2];
                    continue;
                }
                session.negotiate();
                session.login(user == null ? "" : user, password == null ? "" : password);
                return session;
            } catch (IOException err) {
                tcp.close();
                throw err;
            } catch (NumberFormatException err) {
                tcp.close();
                throw new IOException("The listener at " + host
                        + " redirected to an address this client cannot read", err);
            }
        }
    }

    private static void checkDescriptorValue(String what, String value) throws IOException {
        if (value == null) {
            throw new IOException("No " + what + " in the Oracle URL");
        }
        for (int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            if (c == '(' || c == ')' || c == '=' || c < ' ' || c > '~') {
                throw new IOException("The Oracle " + what + " cannot contain '" + c + "': "
                        + value);
            }
        }
    }

    static String descriptor(String host, int port, String service, boolean tls) {
        return "(DESCRIPTION=(ADDRESS=(PROTOCOL=" + (tls ? "TCPS" : "TCP") + ")(HOST=" + host
                + ")(PORT=" + port + "))(CONNECT_DATA=(SERVICE_NAME=" + service
                + ")(CID=(PROGRAM=cn1)(HOST=localhost)(USER=cn1))))";
    }

    // ---------------- CONNECT and ACCEPT ----------------

    /// Sends CONNECT and reads the listener's answer: null once accepted, or
    /// the host, port and descriptor of a redirect.
    private String[] connectPacket(String descriptor) throws IOException {
        byte[] text = Wire.utf8(descriptor);
        for (int attempt = 0 ; attempt < 3 ; attempt++) {
            Buf out = new Buf(); // NOPMD CloseResource - an in-memory buffer
            out.u16(319);               // version
            out.u16(300);               // lowest version accepted
            out.u16(1);                 // service options: none
            out.u16(SDU);
            out.u16(SDU);
            out.u16(0x4f98);            // protocol characteristics
            out.u16(0);
            out.u16(1);                 // byte order
            out.u16(text.length);
            out.u16(74);                // offset of the descriptor
            out.u32(0);
            out.write(0x84);            // NSI flags: no Native Network Encryption
            out.write(0x84);
            for (int iter = 0 ; iter < 24 ; iter++) {
                out.write(0);
            }
            out.u32(SDU);
            out.u32(SDU);
            out.u32(0);
            out.u32(0);
            if (text.length > 230) {
                sendPacket(PACKET_CONNECT, out.toByteArray(), 0);
                byte[] data = new byte[text.length + 2];
                System.arraycopy(text, 0, data, 2, text.length);
                sendPacket(PACKET_DATA, data, 0);
            } else {
                out.write(text, 0, text.length);
                sendPacket(PACKET_CONNECT, out.toByteArray(), 0);
            }
            byte[] answer = readPacket();
            if (packetType == PACKET_RESEND) {
                continue;
            }
            if (packetType == PACKET_ACCEPT) {
                if (answer.length < 28) {
                    throw new IOException("The Oracle listener sent a short ACCEPT");
                }
                int version = be16(answer, 0);
                if (version < 315) {
                    throw new IOException("The Oracle server speaks protocol version " + version
                            + "; this client needs 315 (Oracle Database 12.1) or later");
                }
                largeSdu = true;
                int accepted = be32(answer, 24);
                if (accepted > 0 && accepted < sdu) {
                    sdu = accepted;
                }
                return null;
            }
            if (packetType == PACKET_REFUSE) {
                String reason = answer.length > 4 ? Wire.fromUtf8(answer, 4, answer.length - 4) : "";
                throw new IOException("The Oracle listener refused the connection: "
                        + refusal(reason));
            }
            if (packetType == PACKET_REDIRECT) {
                return redirect(answer);
            }
            throw new IOException("The Oracle listener answered CONNECT with packet type "
                    + packetType);
        }
        throw new IOException("The Oracle listener asked for CONNECT to be resent three times");
    }

    /// The error a refusal carries, ERR=12514 and the like, with what it means
    /// when it is one of the two everybody meets.
    static String refusal(String reason) {
        int at = reason.indexOf("(ERR=");
        if (at < 0) {
            return reason;
        }
        int end = reason.indexOf(')', at);
        String code = end > at ? reason.substring(at + 5, end) : "";
        if ("12514".equals(code)) {
            return "ORA-12514: the listener does not know the service named in the URL";
        }
        if ("12505".equals(code)) {
            return "ORA-12505: the listener does not know the SID named in the URL";
        }
        return "ORA-" + code + " " + reason;
    }

    private static String[] redirect(byte[] answer) throws IOException {
        if (answer.length < 2) {
            throw new IOException("The Oracle listener sent an empty redirect");
        }
        int length = be16(answer, 0);
        byte[] data = answer;
        int from = 2;
        if (length > answer.length - 2) {
            throw new IOException("The Oracle listener sent a truncated redirect");
        }
        String text = Wire.fromUtf8(data, from, length);
        int nul = text.indexOf('\0');
        String address = nul < 0 ? text : text.substring(0, nul);
        String descriptor = nul < 0 ? null : text.substring(nul + 1);
        String host = descriptorValue(address, "HOST");
        String port = descriptorValue(address, "PORT");
        if (host == null || port == null || descriptor == null || descriptor.length() == 0) {
            throw new IOException("The Oracle listener redirected to an address this client "
                    + "cannot read: " + text);
        }
        return new String[] {host, port, descriptor};
    }

    static String descriptorValue(String descriptor, String key) {
        int length = descriptor.length();
        for (int at = 0 ; at + key.length() + 2 <= length ; at++) {
            if (descriptor.charAt(at) == '('
                    && descriptor.regionMatches(true, at + 1, key, 0, key.length())) {
                int equals = at + 1 + key.length();
                while (equals < length && descriptor.charAt(equals) == ' ') {
                    equals++;
                }
                if (equals < length && descriptor.charAt(equals) == '=') {
                    int end = descriptor.indexOf(')', equals);
                    if (end > equals) {
                        return descriptor.substring(equals + 1, end).trim();
                    }
                }
            }
        }
        return null;
    }

    // ---------------- protocol and data types ----------------

    private void negotiate() throws IOException {
        Buf protocol = new Buf(); // NOPMD CloseResource - an in-memory buffer
        protocol.write(MSG_PROTOCOL);
        protocol.write(6);              // protocol version 8.1 and later
        protocol.write(0);              // array terminator
        byte[] name = Wire.utf8("cn1");
        protocol.write(name, 0, name.length);
        protocol.write(0);
        sendData(protocol.toByteArray());
        int type = u8();
        if (type == MSG_ERROR) {
            readError();
            throw new IOException("The Oracle server refused the protocol: " + errorMessage);
        }
        if (type != MSG_PROTOCOL) {
            throw new IOException("The Oracle server answered the protocol with message "
                    + type);
        }
        u8();                           // server version
        u8();
        int banner = u8();
        while (banner != 0) {                // the server's banner
            banner = u8();
        }
        int charset = u8() | (u8() << 8);
        u8();                           // server flags
        int elements = u8() | (u8() << 8);
        skip(elements * 5);
        int fdoLength = (u8() << 8) | u8();
        skip(fdoLength);
        byte[] caps = bytesWithLength();
        bytesWithLength();              // runtime capabilities
        if (caps != null && caps.length > 7 && (caps[7] & 0xff) < fieldVersion) {
            fieldVersion = caps[7] & 0xff;
        }
        if (fieldVersion < FIELD_VERSION_12_2) {
            throw new IOException("The Oracle server is older than 12.2, which this client "
                    + "does not speak to");
        }
        if (charset != CHARSET_UTF8) {
            // NOT a refusal of the data: every value travels in the client's
            // character set, which is declared as AL32UTF8 below, and the server
            // converts. Only NCHAR columns use the national set.
            charset = CHARSET_UTF8;
        }

        Buf types = new Buf(); // NOPMD CloseResource - an in-memory buffer
        types.write(MSG_DATA_TYPES);
        types.write(CHARSET_UTF8 & 0xff);
        types.write(CHARSET_UTF8 >> 8);
        types.write(CHARSET_UTF8 & 0xff);
        types.write(CHARSET_UTF8 >> 8);
        types.write(3);                 // flags: UCS2 conversion for NCHAR
        byte[] compile = compileCapabilities();
        types.write(compile.length);
        types.write(compile, 0, compile.length);
        byte[] runtime = new byte[11];
        runtime[0] = 2;                 // version: 6.1
        runtime[6] = 5;                 // TTC and TTC returned-value flags
        types.write(runtime.length);
        types.write(runtime, 0, runtime.length);
        for (int iter = 0 ; iter < DATA_TYPES.length ; iter += 3) {
            types.u16(DATA_TYPES[iter]);
            types.u16(DATA_TYPES[iter + 1]);
            types.u16(DATA_TYPES[iter + 2]);
            types.u16(0);
        }
        types.u16(0);
        sendData(types.toByteArray());
        type = u8();
        if (type != MSG_DATA_TYPES) {
            throw new IOException("The Oracle server answered the data types with message "
                    + type);
        }
        while (true) {
            int dataType = (u8() << 8) | u8();
            if (dataType == 0) {
                break;
            }
            int converted = (u8() << 8) | u8();
            if (converted != 0) {
                skip(4);
            }
        }
    }

    /// The compile-time capabilities this client declares, from
    /// capabilities.pyx: what it can read and write, and the field version.
    static byte[] compileCapabilities() {
        byte[] caps = new byte[53];
        caps[0] = 6;                    // SQL version
        caps[4] = (byte) 0xea;          // logon flags: O5LOGON, UB2 in DTY, ...
        caps[5] = 0x08;                 // features
        caps[7] = (byte) FIELD_VERSION;
        caps[8] = 1;                    // server compiles the column info
        caps[15] = 0x29;                // LOB features
        caps[16] = (byte) 0x90;         // remote OCI
        caps[17] = 3;                   // database charset conversion
        caps[18] = 7;                   // RPC for 8.1 and later
        caps[19] = 3;
        caps[21] = 1;
        caps[23] = (byte) 0xcf;         // TTC capabilities
        caps[26] = 4;
        caps[27] = 1;
        caps[31] = 0x10;                // sessionless transactions
        caps[34] = 12;
        caps[37] = (byte) 0xb0;         // LOB prefetch, 12c features
        caps[40] = 4;
        caps[42] = 5;
        return caps;
    }

    // ---------------- O5LOGON ----------------

    private void login(String user, String password) throws IOException {
        byte[] name = Wire.utf8(user);
        Buf first = new Buf(); // NOPMD CloseResource - an in-memory buffer
        function(first, FUNC_AUTH_PHASE_ONE);
        first.write(1);                 // has a user
        first.ub4(name.length);
        first.ub4(1);                   // mode: logon
        first.write(1);                 // has key/value pairs
        first.ub4(5);
        first.write(1);
        first.write(1);
        first.bytesWithLength(name);
        first.keyValue("AUTH_TERMINAL", "unknown", 0);
        first.keyValue("AUTH_PROGRAM_NM", "cn1", 0);
        first.keyValue("AUTH_MACHINE", "localhost", 0);
        first.keyValue("AUTH_PID", "1", 0);
        first.keyValue("AUTH_SID", "cn1", 0);
        sendData(first.toByteArray());
        Map session = new HashMap();
        int verifier = readAuth(session);

        byte[] secret = Wire.utf8(password);
        String vfrHex = (String) session.get("AUTH_VFR_DATA");
        String sessionKeyHex = (String) session.get("AUTH_SESSKEY");
        if (vfrHex == null || sessionKeyHex == null) {
            throw new IOException("The Oracle server sent no password verifier for " + user);
        }
        byte[] vfr = unhex(vfrHex);
        byte[] passwordHash;
        byte[] passwordKey = null;
        int keyLength;
        if (verifier == VERIFIER_12C) {
            keyLength = 32;
            int iterations = parseCount(session, "AUTH_PBKDF2_VGEN_COUNT");
            passwordKey = Crypto.pbkdf2(Crypto.SHA512, secret,
                    concat(vfr, Wire.utf8("AUTH_PBKDF2_SPEEDY_KEY")), iterations, 64);
            passwordHash = slice(Crypto.sha512(concat(passwordKey, vfr)), 0, 32);
        } else if (verifier == VERIFIER_11G_1 || verifier == VERIFIER_11G_2) {
            keyLength = 24;
            passwordHash = concat(Crypto.sha1(concat(secret, vfr)), new byte[4]);
        } else {
            throw new IOException("The account " + user + " has only a password verifier "
                    + "older than 11g (0x" + Integer.toHexString(verifier) + "), which this "
                    + "client does not implement. Reset its password on a server with "
                    + "SQLNET.ALLOWED_LOGON_VERSION_SERVER at 12 or higher");
        }
        byte[] serverKey = unhex(sessionKeyHex);
        // 48 bytes with an 11g verifier, 32 with 12c; anything else would
        // index past the key below.
        if (serverKey.length != (verifier == VERIFIER_12C ? 32 : 48)) {
            throw new IOException("The Oracle server sent a session key of " + serverKey.length
                    + " bytes for verifier 0x" + Integer.toHexString(verifier));
        }
        byte[] serverPart = Crypto.aesCbcDecryptBlocks(passwordHash, new byte[16], serverKey);
        byte[] clientPart = Crypto.randomBytes(serverPart.length);
        byte[] encryptedClient = Crypto.aesCbcEncryptBlocks(passwordHash, new byte[16],
                clientPart);
        String sessionKey;
        byte[] combined;
        if (serverPart.length == 48) {
            sessionKey = hex(encryptedClient).substring(0, 96);
            byte[] mixed = new byte[24];
            for (int iter = 16 ; iter < 40 ; iter++) {
                mixed[iter - 16] = (byte) (serverPart[iter] ^ clientPart[iter]);
            }
            combined = slice(concat(Crypto.md5(slice(mixed, 0, 16)),
                    Crypto.md5(slice(mixed, 16, 24))), 0, keyLength);
        } else {
            sessionKey = hex(encryptedClient).substring(0, 64);
            byte[] salt = unhex((String) session.get("AUTH_PBKDF2_CSK_SALT"));
            int iterations = parseCount(session, "AUTH_PBKDF2_SDER_COUNT");
            byte[] mixed = concat(slice(clientPart, 0, keyLength), slice(serverPart, 0, keyLength));
            combined = Crypto.pbkdf2(Crypto.SHA512, Wire.utf8(hex(mixed)), salt, iterations,
                    keyLength);
        }
        String speedyKey = null;
        if (verifier == VERIFIER_12C) {
            speedyKey = hex(slice(encryptPadded(combined,
                    concat(Crypto.randomBytes(16), passwordKey)), 0, 80));
        }
        String encryptedPassword = hex(encryptPadded(combined,
                concat(Crypto.randomBytes(16), secret)));

        Buf second = new Buf(); // NOPMD CloseResource - an in-memory buffer
        function(second, FUNC_AUTH_PHASE_TWO);
        second.write(1);
        second.ub4(name.length);
        second.ub4(1 | 0x100);          // logon, with the password
        second.write(1);
        second.ub4(speedyKey != null ? 7 : 6);
        second.write(1);
        second.write(1);
        second.bytesWithLength(name);
        second.keyValue("AUTH_SESSKEY", sessionKey, 1);
        if (speedyKey != null) {
            second.keyValue("AUTH_PBKDF2_SPEEDY_KEY", speedyKey, 0);
        }
        second.keyValue("AUTH_PASSWORD", encryptedPassword, 0);
        second.keyValue("SESSION_CLIENT_CHARSET", "873", 0);
        second.keyValue("SESSION_CLIENT_DRIVER_NAME", "cn1", 0);
        second.keyValue("SESSION_CLIENT_VERSION", "0", 0);
        second.keyValue("AUTH_ALTER_SESSION", "ALTER SESSION SET TIME_ZONE='+00:00'\0", 1);
        sendData(second.toByteArray());
        session.clear();
        readAuth(session);
        // Text that converts to a date, a timestamp or a number is read in ONE
        // format whatever the server's locale says, the same text the other
        // engines accept and this client returns.
        run("ALTER SESSION SET NLS_DATE_FORMAT='YYYY-MM-DD HH24:MI:SS' "
                + "NLS_TIMESTAMP_FORMAT='YYYY-MM-DD HH24:MI:SS.FF' "
                + "NLS_TIMESTAMP_TZ_FORMAT='YYYY-MM-DD HH24:MI:SS.FF TZH:TZM' "
                + "NLS_NUMERIC_CHARACTERS='.,'", new Object[0], null);
    }

    private static final int MAX_PBKDF2_ITERATIONS = 1000000;

    private static int parseCount(Map session, String key) throws IOException {
        Object value = session.get(key);
        if (value instanceof String) {
            try {
                int count = Integer.parseInt((String) value);
                // A server picks these, so a hostile one could ask for hours
                // of PBKDF2. Oracle's defaults are 4096 and 3.
                if (count >= 1 && count <= MAX_PBKDF2_ITERATIONS) {
                    return count;
                }
            } catch (NumberFormatException err) {
                // reported below
            }
        }
        throw new IOException("The Oracle server sent no usable " + key);
    }

    /// AES-CBC with a zero IV and PKCS#7 padding, as O5LOGON encrypts.
    private static byte[] encryptPadded(byte[] key, byte[] data) throws IOException {
        int pad = 16 - data.length % 16;
        byte[] padded = new byte[data.length + pad];
        System.arraycopy(data, 0, padded, 0, data.length);
        for (int iter = data.length ; iter < padded.length ; iter++) {
            padded[iter] = (byte) pad;
        }
        return Crypto.aesCbcEncryptBlocks(key, new byte[16], padded);
    }

    /// Reads an authentication response into `session`, answering with the
    /// verifier type the server named.
    private int readAuth(Map session) throws IOException {
        int verifier = 0;
        errorNumber = 0;
        while (true) {
            int type = u8();
            if (type == MSG_PARAMETER) {
                int count = (int) ub();
                for (int iter = 0 ; iter < count ; iter++) {
                    ub();
                    String key = text();
                    long valueLength = ub();
                    String value = valueLength > 0 ? text() : "";
                    long flags = ub();
                    if ("AUTH_VFR_DATA".equals(key)) {
                        verifier = (int) flags;
                    }
                    if (key != null) {
                        session.put(key, value);
                    }
                }
            } else if (type == MSG_ERROR) {
                readError();
                if (errorNumber != 0) {
                    throw new IOException(errorMessage);
                }
                return verifier;
            } else if (type == MSG_STATUS) {
                ub();
                ub();
                return verifier;
            } else if (type == MSG_SERVER_PIGGYBACK) {
                serverPiggyback();
            } else if (type == MSG_WARNING) {
                warning();
            } else {
                throw new IOException("The Oracle server sent message " + type
                        + " during the login");
            }
        }
    }

    // ---------------- statements ----------------

    /// Runs a statement that returns no rows and answers with the number of
    /// rows it affected.
    public int execute(String sql, Object[] params) throws IOException {
        return run(sql, params, null);
    }

    /// Runs a statement and answers with its rows, each a `Map` of column name
    /// to value in projection order. A DML statement with `RETURNING ... INTO`
    /// answers with one row of the values it returned.
    public List query(String sql, Object[] params) throws IOException {
        QueryRows rows = new QueryRows(0);
        run(sql, params, rows);
        return rows;
    }

    private int run(String sql, Object[] params, QueryRows rows) throws IOException {
        checkOpen();
        Object[] values = params == null ? new Object[0] : params;
        try {
            return statement(sql, values, rows);
        } catch (OracleError err) {
            IOException failure = classify(err.number, err.getMessage() + " [" + sql + "]");
            failure.initCause(err);
            throw failure;
        } catch (Refusal err) {
            throw err;
        } catch (IOException err) {
            // Half a request sent or half an answer read: nothing that follows
            // on this connection can be trusted to be the answer to its question.
            close();
            throw err;
        }
    }

    /// A statement refused before anything was sent, which leaves the
    /// session as it was.
    private static final class Refusal extends IOException {
        private static final long serialVersionUID = 1L;

        Refusal(String message) {
            super(message);
        }
    }

    /// An error the server reported for the statement, which leaves the
    /// session usable.
    private static final class OracleError extends IOException {
        private static final long serialVersionUID = 1L;
        final int number;

        OracleError(int number, String message) {
            super(message);
            this.number = number;
        }
    }

    static IOException classify(int number, String text) {
        if (isConcurrencyFailure(number)) {
            return new ConcurrencyFailureException(text);
        }
        if (isIntegrityViolation(number)) {
            return new DataIntegrityViolationException(text);
        }
        return new IOException(text);
    }

    /// Deadlock, a serializable transaction that cannot proceed, and a lock
    /// that NOWAIT would not wait for.
    static boolean isConcurrencyFailure(int number) {
        return number == 60 || number == 8177 || number == 54 || number == 30006;
    }

    /// A unique key, a foreign key in either direction, a NOT NULL column and
    /// a check constraint.
    static boolean isIntegrityViolation(int number) {
        return number == 1 || number == 2291 || number == 2292 || number == 1400
                || number == 1407 || number == 2290;
    }

    private int statement(String sql, Object[] params, QueryRows rows) throws IOException {
        Binds binds = Binds.of(sql, params);
        String keyword = firstKeyword(sql);
        boolean query = "SELECT".equals(keyword) || "WITH".equals(keyword);
        boolean plsql = "BEGIN".equals(keyword) || "DECLARE".equals(keyword)
                || "CALL".equals(keyword);
        boolean ddl = "CREATE".equals(keyword) || "ALTER".equals(keyword)
                || "DROP".equals(keyword) || "TRUNCATE".equals(keyword)
                || "GRANT".equals(keyword) || "REVOKE".equals(keyword)
                || "COMMENT".equals(keyword) || "RENAME".equals(keyword);
        int count = binds.values.length;
        outTypes = binds.outCount > 0 ? new int[binds.outCount] : null;
        returned = null;

        Buf out = new Buf(); // NOPMD CloseResource - an in-memory buffer
        closeCursorsPiggyback(out);
        int options = EXEC_PARSE | EXEC_EXECUTE;
        if (query) {
            options |= EXEC_FETCH;
        }
        if (!plsql) {
            options |= EXEC_NOT_PLSQL;
        } else if (count > 0) {
            options |= EXEC_PLSQL_BIND;
        }
        if (count > 0) {
            options |= EXEC_BIND;
        }
        if (!inTransaction && !query && !ddl) {
            options |= EXEC_COMMIT;
        }
        byte[] text = Wire.utf8(trimTerminator(sql, plsql));
        function(out, FUNC_EXECUTE);
        executeHeader(out, options, 0, text.length, query ? PREFETCH : 1, count, 0);
        out.bytesWithLength(text);
        al8i4(out, query, query ? 0 : 1);
        if (count > 0) {
            boolean hasInput = false;
            for (int iter = 0 ; iter < count ; iter++) {
                Object value = binds.values[iter];
                int type;
                long size;
                int csfrm = 0;
                if (value instanceof Out) {
                    type = TYPE_VARCHAR;
                    size = MAX_STRING;
                    csfrm = CSFRM_IMPLICIT;
                } else {
                    hasInput = true;
                    type = bindType(value);
                    size = bindSize(value, type);
                    if (type == TYPE_VARCHAR || type == TYPE_LONG) {
                        csfrm = CSFRM_IMPLICIT;
                    }
                }
                bindMetadata(out, type, size, csfrm);
            }
            if (hasInput) {
                out.write(MSG_ROW_DATA);
                // A value too long for a VARCHAR2 or a RAW travels as a LONG,
                // and the server reads the LONGs AFTER every other value.
                for (int pass = 0 ; pass < 2 ; pass++) {
                    for (int iter = 0 ; iter < count ; iter++) {
                        Object value = binds.values[iter];
                        if (value instanceof Out) {
                            continue;
                        }
                        int type = bindType(value);
                        boolean isLong = type == TYPE_LONG || type == TYPE_LONG_RAW;
                        if (isLong == (pass == 1)) {
                            writeValue(out, value, type);
                        }
                    }
                }
            }
        }
        sendData(out.toByteArray());

        columns = null;
        previousRow = null;
        moreRows = false;
        cursorId = 0;
        List read = new ArrayList();
        readExecute(read);
        if (columns != null && hasLob(columns)) {
            // A LOB column arrives as a locator, which would need a round trip
            // per value to read. Defining it as a LONG asks the server to send
            // the value itself, as python-oracledb's fetch_lobs=False does.
            read.clear();
            previousRow = null;
            defineLobsAsLongs();
            readExecute(read);
        }
        while (moreRows && cursorId != 0) {
            Buf fetch = new Buf(); // NOPMD CloseResource - an in-memory buffer
            function(fetch, FUNC_FETCH);
            fetch.ub4(cursorId);
            fetch.ub4(PREFETCH);
            sendData(fetch.toByteArray());
            readExecute(read);
        }
        if (cursorId != 0) {
            cursorsToClose.add(Integer.valueOf(cursorId));
        }
        if (errorNumber != 0) {
            throw new OracleError(errorNumber, errorMessage);
        }
        if (rows != null) {
            fill(rows, read, binds);
        }
        return (int) rowCount;
    }

    private void fill(QueryRows rows, List read, Binds binds) {
        if (returned != null) {
            String[] names = binds.returningNames;
            rows.setColumnCount(returned.length);
            if (rowCount == 0) {
                // The statement wrote no row -- an ignored duplicate, an UPDATE
                // that matched nothing -- so there is nothing it returned, as a
                // PostgreSQL RETURNING that matched no row answers no rows.
                return;
            }
            Map row = new LinkedHashMap();
            for (int iter = 0 ; iter < returned.length ; iter++) {
                String name = names != null && iter < names.length ? names[iter] : "RETURNED" + (iter + 1);
                row.put(name, returned[iter]);
            }
            rows.add(row);
            return;
        }
        if (columns == null) {
            return;
        }
        rows.setColumnCount(columns.length);
        String[] kinds = new String[columns.length];
        boolean anyKind = false;
        for (int iter = 0 ; iter < columns.length ; iter++) {
            kinds[iter] = kind(columns[iter]);
            anyKind |= kinds[iter] != null;
        }
        if (anyKind) {
            rows.setColumnTypes(kinds);
        }
        for (Object each : read) {
            Object[] values = (Object[]) each;
            Map row = new LinkedHashMap();
            for (int column = 0 ; column < columns.length ; column++) {
                row.put(columns[column].name, values[column]);
            }
            rows.add(row);
        }
    }

    /// The portable column kind (`Database.columnTypes(List)`) of a column,
    /// for the kinds whose values arrive as text a caller must parse.
    static String kind(Column column) {
        switch (column.type) {
            case TYPE_NUMBER:
                return column.scale > 0 && column.scale != 0x81 ? "decimal" : null;
            case TYPE_DATE:
            case TYPE_TIMESTAMP:
            case TYPE_TIMESTAMP_LTZ:
                return "datetime";
            case TYPE_TIMESTAMP_TZ:
                return "datetimetz";
            default:
                return null;
        }
    }

    private static boolean hasLob(Column[] columns) {
        for (Column column : columns) {
            if (column.type == TYPE_CLOB || column.type == TYPE_BLOB) {
                return true;
            }
        }
        return false;
    }

    private void defineLobsAsLongs() throws IOException {
        Buf out = new Buf(); // NOPMD CloseResource - an in-memory buffer
        function(out, FUNC_EXECUTE);
        executeHeader(out, EXEC_DEFINE | EXEC_NOT_PLSQL, cursorId, 0, PREFETCH, 0,
                columns.length);
        al8i4(out, true, PREFETCH);
        for (Column column : columns) {
            if (column.type == TYPE_CLOB) {
                column.type = TYPE_LONG;
            } else if (column.type == TYPE_BLOB) {
                column.type = TYPE_LONG_RAW;
            }
            boolean isLong = column.type == TYPE_LONG || column.type == TYPE_LONG_RAW;
            long size = isLong ? MAX_LONG_LENGTH : column.bufferSize;
            boolean text = column.type == TYPE_VARCHAR || column.type == TYPE_CHAR
                    || column.type == TYPE_LONG;
            out.write(column.type);
            out.write(1);               // use indicators
            out.write(0);
            out.write(0);
            out.ub4(size);
            out.ub4(0);
            out.ub8(0);
            out.ub4(0);
            out.ub2(0);
            out.ub2(text ? CHARSET_UTF8 : 0);
            out.write(text ? column.csfrm : 0);
            out.ub4(0);
            out.ub4(0);                 // field version 12.2 and later
        }
        sendData(out.toByteArray());
    }

    /// The fixed part of an execute message, from messages.pyx.
    private void executeHeader(Buf out, int options, int cursor, int sqlLength, int iterations,
            int bindCount, int defineCount) {
        out.ub4(options);
        out.ub4(cursor);
        out.write(cursor == 0 ? 1 : 0); // pointer to the statement
        out.ub4(sqlLength);
        out.write(1);                   // pointer to al8i4
        out.ub4(13);
        out.write(0);
        out.write(0);
        out.ub4(0);                     // prefetch buffer size
        out.ub4(iterations);
        out.ub4(MAX_LONG_LENGTH);
        out.write(bindCount > 0 ? 1 : 0);
        out.ub4(bindCount);
        out.write(0);                   // al8app
        out.write(0);                   // al8txn
        out.write(0);                   // al8txl
        out.write(0);                   // al8kv
        out.write(0);                   // al8kvl
        out.write(defineCount > 0 ? 1 : 0);
        out.ub4(defineCount);
        out.ub4(0);                     // registration id
        out.write(0);                   // al8objlist
        out.write(1);                   // al8objlen
        out.write(0);                   // al8blv
        out.ub4(0);
        out.write(0);                   // al8dnam
        out.ub4(0);
        out.ub4(0);                     // registration id, part 2
        out.write(0);                   // al8pidmlrc
        out.ub4(0);
        out.write(0);
        // field version 12.2 and later: SQL signature, and 12.2 ext 1: chunk ids
        out.write(0);
        out.ub4(0);
        out.write(0);
        out.ub4(0);
        out.write(0);
        if (fieldVersion >= FIELD_VERSION_12_2_EXT1) {
            out.write(0);
            out.ub4(0);
        }
    }

    private static void al8i4(Buf out, boolean query, int executions) {
        out.ub4(1);                     // parse
        out.ub4(executions);
        out.ub4(0);
        out.ub4(0);
        out.ub4(0);
        out.ub4(0);                     // SCN, part 1
        out.ub4(0);                     // SCN, part 2
        out.ub4(query ? 1 : 0);
        out.ub4(0);
        out.ub4(query ? 0 : 0x8000);    // implicit result sets
        out.ub4(0);
        out.ub4(0);
        out.ub4(0);
    }

    private void bindMetadata(Buf out, int type, long size, int csfrm) {
        out.write(type);
        out.write(1);                   // use indicators
        out.write(0);
        out.write(0);
        out.ub4(size);
        out.ub4(0);
        out.ub8(0);
        out.ub4(0);
        out.ub2(0);
        out.ub2(csfrm != 0 ? CHARSET_UTF8 : 0);
        out.write(csfrm);
        out.ub4(0);
        out.ub4(0);                     // field version 12.2 and later
    }

    private void closeCursorsPiggyback(Buf out) {
        if (cursorsToClose.isEmpty()) {
            return;
        }
        out.write(MSG_PIGGYBACK);
        out.write(FUNC_CLOSE_CURSORS);
        out.write(nextSequence());
        if (fieldVersion >= FIELD_VERSION_23_1) {
            out.ub8(0);                 // token number
        }
        out.write(1);
        out.ub4(cursorsToClose.size());
        for (Object cursor : cursorsToClose) {
            out.ub4(((Integer) cursor).intValue());
        }
        cursorsToClose.clear();
    }

    static int bindType(Object value) {
        if (value == null) {
            return TYPE_VARCHAR;
        }
        if (value instanceof Double || value instanceof Float) {
            return TYPE_BINARY_DOUBLE;
        }
        if (value instanceof byte[]) {
            return ((byte[]) value).length > MAX_RAW ? TYPE_LONG_RAW : TYPE_RAW;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return TYPE_NUMBER;
        }
        return Wire.utf8(value.toString()).length > MAX_STRING ? TYPE_LONG : TYPE_VARCHAR;
    }

    private static long bindSize(Object value, int type) {
        switch (type) {
            case TYPE_BINARY_DOUBLE:
                return 8;
            case TYPE_NUMBER:
                return 22;
            case TYPE_RAW:
                return Math.max(1, ((byte[]) value).length);
            case TYPE_LONG:
            case TYPE_LONG_RAW:
                return MAX_LONG_LENGTH;
            default:
                return value == null ? 1 : Math.max(1, Wire.utf8(value.toString()).length);
        }
    }

    private static void writeValue(Buf out, Object value, int type) throws IOException {
        if (value == null) {
            out.write(0);
            return;
        }
        switch (type) {
            case TYPE_BINARY_DOUBLE:
                out.bytesWithLength(encodeDouble(((Number) value).doubleValue()));
                break;
            case TYPE_NUMBER:
                if (value instanceof Boolean) {
                    out.bytesWithLength(encodeNumber(((Boolean) value).booleanValue() ? "1" : "0"));
                } else {
                    out.bytesWithLength(encodeNumber(value.toString()));
                }
                break;
            case TYPE_RAW:
            case TYPE_LONG_RAW:
                out.bytesWithLength((byte[]) value);
                break;
            default:
                out.bytesWithLength(Wire.utf8(value.toString()));
                break;
        }
    }

    /// Reads one response to an execute or a fetch.
    private void readExecute(List rows) throws IOException {
        errorNumber = 0;
        errorMessage = null;
        while (true) {
            int type = u8();
            switch (type) {
                case MSG_DESCRIBE_INFO:
                    bytesWithLength();          // chunked, unused
                    describe();
                    moreRows = true;
                    break;
                case MSG_ROW_HEADER:
                    rowHeader();
                    break;
                case MSG_BIT_VECTOR: {
                    ub();                       // columns sent
                    int length = columns == null ? 0 : (columns.length + 7) / 8;
                    bitVector = raw(length);
                    break;
                }
                case MSG_ROW_DATA:
                    if (outTypes != null && columns == null) {
                        readReturned();
                    } else {
                        rows.add(readRow());
                    }
                    break;
                case MSG_ERROR:
                    readError();
                    if (errorNumber == 1403) {
                        errorNumber = 0;
                        moreRows = false;
                    } else if (errorNumber != 0) {
                        moreRows = false;
                    }
                    return;
                case MSG_PARAMETER:
                    returnParameters();
                    break;
                case MSG_SERVER_PIGGYBACK:
                    serverPiggyback();
                    break;
                case MSG_WARNING:
                    warning();
                    break;
                case MSG_STATUS:
                    ub();
                    ub();
                    return;
                case MSG_END_OF_RESPONSE:
                    return;
                case MSG_FLUSH_OUT_BINDS:
                    // The response ends here: the server holds the out binds
                    // (the values RETURNING ... INTO wrote) until the client
                    // asks for them with the same one-byte message, and then
                    // sends them as the rest of this response.
                    sendData(new byte[] {(byte) MSG_FLUSH_OUT_BINDS});
                    break;
                default:
                    throw new IOException("The Oracle server sent message " + type
                            + ", which this client does not read");
            }
        }
    }

    private void describe() throws IOException {
        ub();                                   // longest row
        int count = (int) ub();
        if (count > 0) {
            u8();
        }
        if (count > 65535) {
            throw new IOException("The Oracle server described " + count + " columns");
        }
        Column[] described = new Column[count];
        for (int iter = 0 ; iter < count ; iter++) {
            Column column = new Column();
            column.type = u8();
            u8();                               // flags
            u8();                               // precision; scale is what decides
            column.scale = u8();
            column.bufferSize = ub();
            ub();                               // largest array elements
            ub();                               // continuation flags
            if (ub() > 0) {
                bytesWithLength();              // object type id
            }
            ub();                               // version
            ub();                               // character set id
            column.csfrm = u8();
            ub();                               // size in characters
            if (fieldVersion >= FIELD_VERSION_12_2) {
                ub();                           // OAC MXLNLEN
            }
            u8();                               // nullable
            u8();                               // v7 length of name
            column.name = ub() > 0 ? foldLabel(text()) : "";
            if (ub() > 0) {
                text();                         // schema
            }
            if (ub() > 0) {
                text();                         // type name
            }
            ub();                               // position
            ub();                               // uds flags
            if (fieldVersion >= FIELD_VERSION_19_1) {
                if (ub() > 0) {
                    text();                     // domain schema
                }
                if (ub() > 0) {
                    text();                     // domain name
                }
            }
            if (fieldVersion >= FIELD_VERSION_21_1) {
                long annotations = ub();
                if (annotations > 0) {
                    u8();
                    annotations = ub();
                    u8();
                    for (long a = 0 ; a < annotations ; a++) {
                        ub();
                        text();
                        if (ub() > 0) {
                            text();
                        }
                        ub();
                    }
                    ub();
                }
            }
            if (fieldVersion >= FIELD_VERSION_23_1_EXT_1) {
                ub();                           // vector dimensions
                u8();                           // vector format
                u8();                           // vector flags
            }
            described[iter] = column;
        }
        if (ub() > 0) {
            bytesWithLength();                  // current date
        }
        ub();
        ub();
        ub();
        ub();
        if (ub() > 0) {
            bytesWithLength();
        }
        columns = described;
    }

    /// An all-upper-case label in lower case. Oracle reports an unquoted name
    /// upper-cased, `SELECT id FROM t` as `ID`, where every other engine keys the
    /// row as written; a label with any lower-case letter was quoted on purpose
    /// and is kept.
    static String foldLabel(String name) {
        if (name == null) {
            return "";
        }
        for (int iter = 0 ; iter < name.length() ; iter++) {
            char c = name.charAt(iter);
            if (c >= 'a' && c <= 'z' || c > 127) {
                return name;
            }
        }
        StringBuilder out = new StringBuilder(name.length());
        for (int iter = 0 ; iter < name.length() ; iter++) {
            char c = name.charAt(iter);
            out.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return out.toString();
    }

    private void rowHeader() throws IOException {
        u8();                                   // flags
        ub();                                   // requests
        ub();                                   // iteration number
        ub();                                   // iterations
        ub();                                   // buffer length
        long length = ub();
        if (length > 0) {
            u8();
            bitVector = raw((int) length);
        }
        if (ub() > 0) {
            bytesWithLength();                  // rxhrid
        }
    }

    private Object[] readRow() throws IOException {
        int count = columns == null ? 0 : columns.length;
        Object[] row = new Object[count];
        for (int iter = 0 ; iter < count ; iter++) {
            if (bitVector != null && previousRow != null
                    && (bitVector[iter >> 3] & (1 << (iter & 7))) == 0) {
                // The server sends a value only where it differs from the row
                // before; a clear bit means "the same as above".
                row[iter] = previousRow[iter];
            } else {
                row[iter] = value(columns[iter], true);
            }
        }
        bitVector = null;
        previousRow = row;
        return row;
    }

    private void readReturned() throws IOException {
        Object[] values = new Object[outTypes.length];
        Column column = new Column();
        column.type = TYPE_VARCHAR;
        column.csfrm = CSFRM_IMPLICIT;
        column.bufferSize = MAX_STRING;
        for (int iter = 0 ; iter < outTypes.length ; iter++) {
            long count = ub();
            for (long row = 0 ; row < count ; row++) {
                Object value = value(column, false);
                if (row == 0) {
                    values[iter] = value;
                }
            }
        }
        returned = values;
    }

    private void returnParameters() throws IOException {
        int count = (int) ub();
        for (int iter = 0 ; iter < count ; iter++) {
            ub();
        }
        count = (int) ub();
        if (count > 0) {
            skip(count);
        }
        count = (int) ub();
        for (int iter = 0 ; iter < count ; iter++) {
            if (ub() > 0) {
                bytesWithLength();
            }
            if (ub() > 0) {
                bytesWithLength();
            }
            ub();
        }
        count = (int) ub();
        if (count > 0) {
            skip(count);
        }
    }

    private Object value(Column column, boolean fetching) throws IOException {
        if (fetching && column.bufferSize == 0 && column.type != TYPE_LONG
                && column.type != TYPE_LONG_RAW && column.type != TYPE_UROWID) {
            // A column that can only be NULL -- SELECT NULL -- is described
            // with no buffer, and the server sends nothing at all for it.
            return null;
        }
        Object result;
        switch (column.type) {
            case TYPE_VARCHAR:
            case TYPE_CHAR:
            case TYPE_LONG: {
                byte[] data = bytesWithLength();
                result = data == null ? null : column.csfrm == CSFRM_NCHAR
                        ? fromUtf16be(data) : Wire.fromUtf8(data, 0, data.length);
                break;
            }
            case TYPE_RAW:
            case TYPE_LONG_RAW:
                result = bytesWithLength();
                break;
            case TYPE_NUMBER: {
                byte[] data = bytesWithLength();
                result = data == null ? null : number(decodeNumber(data));
                break;
            }
            case TYPE_BINARY_DOUBLE:
            case TYPE_BINARY_FLOAT: {
                byte[] data = bytesWithLength();
                result = data == null ? null : Double.valueOf(decodeBinary(data));
                break;
            }
            case TYPE_DATE:
            case TYPE_TIMESTAMP:
            case TYPE_TIMESTAMP_TZ:
            case TYPE_TIMESTAMP_LTZ: {
                byte[] data = bytesWithLength();
                result = data == null ? null : decodeDate(data, column.type == TYPE_TIMESTAMP_TZ);
                break;
            }
            case TYPE_BOOLEAN: {
                byte[] data = bytesWithLength();
                result = data == null || data.length == 0 ? null
                        : Long.valueOf(data[data.length - 1] == 1 ? 1 : 0);
                break;
            }
            case TYPE_ROWID:
            case TYPE_UROWID:
                throw new IOException("A ROWID column is not read by this client; select "
                        + "ROWIDTOCHAR(ROWID) instead");
            default:
                throw new IOException("An Oracle column of data type " + column.type
                        + " is not read by this client");
        }
        if (fetching) {
            if (column.type == TYPE_LONG || column.type == TYPE_LONG_RAW) {
                sb();                           // null indicator
                ub();                           // return code
            }
        } else {
            sb();                               // actual length
        }
        return result;
    }

    /// A NUMBER's digits as the value a caller receives: a Long for a whole
    /// number a long holds, otherwise the exact text.
    static Object number(String digits) {
        if (digits.indexOf('.') < 0 && digits.indexOf('E') < 0 && digits.length() <= 20) {
            // 20 is nineteen digits and a sign; parseLong refuses what overflows.
            try {
                return Long.valueOf(Long.parseLong(digits));
            } catch (NumberFormatException err) {
                return digits;
            }
        }
        return digits;
    }

    private void readError() throws IOException {
        ub();                                   // call status
        ub();                                   // end to end sequence
        ub();                                   // current row number
        ub();                                   // error number, short
        ub();                                   // array element errors
        ub();
        int cursor = (int) ub();
        sb();                                   // error position
        u8();                                   // SQL type
        u8();                                   // fatal
        u8();
        u8();
        u8();
        u8();
        ub();                                   // rowid: rba
        ub();                                   // partition
        u8();
        ub();                                   // block
        ub();                                   // slot
        ub();                                   // OS error
        u8();                                   // statement number
        u8();                                   // call number
        ub();
        ub();                                   // success iterations
        if (ub() > 0) {
            bytesWithLength();
        }
        int batchErrors = (int) ub();
        if (batchErrors > 0) {
            throw new IOException("The Oracle server sent batch errors this client did "
                    + "not ask for");
        }
        if (ub() > 0 || ub() > 0) {
            throw new IOException("The Oracle server sent batch errors this client did "
                    + "not ask for");
        }
        long number = ub();
        rowCount = ub();
        if (fieldVersion >= FIELD_VERSION_18_1_EXT_1) {
            ub();                               // sql type
            ub();                               // server checksum
        }
        if (cursor != 0) {
            cursorId = cursor;
        }
        errorNumber = (int) number;
        errorMessage = number != 0 ? trimMessage(text()) : null;
    }

    private static String trimMessage(String message) {
        if (message == null) {
            return "ORA-?";
        }
        int end = message.length();
        while (end > 0 && message.charAt(end - 1) <= ' ') {
            end--;
        }
        return message.substring(0, end);
    }

    private void warning() throws IOException {
        ub();                                   // number
        long length = ub();
        ub();                                   // flags
        if (length > 0) {
            text();
        }
    }

    private void serverPiggyback() throws IOException {
        int opcode = u8();
        switch (opcode) {
            case 1:                             // query cache invalidation
            case 3:                             // trace event
                break;
            case 2:                             // OS process id
                ub();
                bytesWithLength();
                break;
            case 4: {                           // session state return
                ub();
                u8();
                int count = (int) ub();
                if (count > 0) {
                    u8();
                    for (int iter = 0 ; iter < count ; iter++) {
                        if (ub() > 0) {
                            bytesWithLength();
                        }
                        if (ub() > 0) {
                            bytesWithLength();
                        }
                        ub();
                    }
                }
                ub();
                ub();
                ub();
                break;
            }
            case 5: {                           // sync
                ub();
                u8();
                int count = (int) ub();
                u8();
                for (int iter = 0 ; iter < count ; iter++) {
                    if (ub() > 0) {
                        bytesWithLength();
                    }
                    if (ub() > 0) {
                        bytesWithLength();
                    }
                    ub();
                }
                ub();
                break;
            }
            case 7: {                           // LTXID
                long length = ub();
                if (length > 0) {
                    skip((int) length);
                }
                break;
            }
            case 8:                             // application continuity context
                ub();
                u8();
                ub();
                ub();
                u8();
                if (ub() > 0) {
                    bytesWithLength();
                }
                break;
            case 9:                             // session signature
                ub();
                u8();
                break;
            default:
                throw new IOException("The Oracle server sent piggyback " + opcode
                        + ", which this client does not read");
        }
    }

    // ---------------- transactions ----------------

    /// Opens a transaction: statements stop committing as they complete.
    public void begin() throws IOException {
        checkOpen();
        inTransaction = true;
    }

    /// Opens a read-only transaction.
    public void beginReadOnly() throws IOException {
        begin(true, Isolation.DEFAULT);
    }

    /// Opens a transaction at an isolation level. Oracle has two: READ
    /// COMMITTED, its default, and SERIALIZABLE. REPEATABLE READ is given
    /// SERIALIZABLE, the stronger of them, and READ UNCOMMITTED is READ
    /// COMMITTED, since Oracle never reads uncommitted data.
    public void begin(boolean readOnly, Isolation isolation) throws IOException {
        checkOpen();
        if (readOnly) {
            run("SET TRANSACTION READ ONLY", null, null);
        } else if (isolation == Isolation.SERIALIZABLE
                || isolation == Isolation.REPEATABLE_READ) {
            run("SET TRANSACTION ISOLATION LEVEL SERIALIZABLE", null, null);
        }
        inTransaction = true;
    }

    /// Commits the open transaction.
    public void commit() throws IOException {
        try {
            run("COMMIT", null, null);
        } finally {
            inTransaction = false;
        }
    }

    /// Rolls the open transaction back.
    public void rollback() throws IOException {
        try {
            run("ROLLBACK", null, null);
        } finally {
            inTransaction = false;
        }
    }

    /// Savepoints, under the verbs the other engines use. Oracle has nothing to
    /// release: a savepoint lasts until its transaction ends.
    public void savepoint(String verb, String name) throws IOException {
        String checked = Dialect.checkIdentifier(name);
        if ("SAVEPOINT".equals(verb)) {
            run("SAVEPOINT " + checked, null, null);
        } else if ("ROLLBACK TO SAVEPOINT".equals(verb)) {
            run("ROLLBACK TO SAVEPOINT " + checked, null, null);
        } else if (!"RELEASE SAVEPOINT".equals(verb)) {
            throw new IOException("Not a savepoint statement: " + verb);
        }
    }

    /// Always 0: Oracle reports a generated key through `RETURNING ... INTO`.
    public long lastInsertId() {
        return 0;
    }

    /// Closes the connection. An open transaction is rolled back by the server.
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            // End of file, so the server logs a clean disconnect.
            sendPacket(PACKET_DATA, new byte[] {0, DATA_FLAGS_EOF}, 0);
        } catch (IOException ignored) {
            // the connection is going away regardless
        }
        connection.close();
    }

    public boolean isClosed() {
        return closed;
    }

    private void checkOpen() throws IOException {
        if (closed) {
            throw new IOException("The Oracle connection is closed");
        }
    }

    // ---------------- statement text ----------------

    /// The statement's first word, upper-cased, past comments and brackets.
    static String firstKeyword(String sql) {
        int at = 0;
        int length = sql.length();
        while (at < length) {
            char c = sql.charAt(at);
            if (c <= ' ' || c == '(') {
                at++;
            } else if (c == '-' && at + 1 < length && sql.charAt(at + 1) == '-') {
                int end = sql.indexOf('\n', at);
                at = end < 0 ? length : end + 1;
            } else if (c == '/' && at + 1 < length && sql.charAt(at + 1) == '*') {
                int end = sql.indexOf("*/", at + 2);
                at = end < 0 ? length : end + 2;
            } else {
                break;
            }
        }
        int end = at;
        while (end < length && isWordChar(sql.charAt(end))) {
            end++;
        }
        StringBuilder out = new StringBuilder(end - at);
        for (int iter = at ; iter < end ; iter++) {
            char c = sql.charAt(iter);
            out.append(c >= 'a' && c <= 'z' ? (char) (c - 32) : c);
        }
        return out.toString();
    }

    private static boolean isWordChar(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_';
    }

    /// Oracle refuses a SQL statement that ends in `;`, where every other
    /// engine accepts one; a PL/SQL block needs its own.
    static String trimTerminator(String sql, boolean plsql) {
        int end = sql.length();
        while (end > 0 && sql.charAt(end - 1) <= ' ') {
            end--;
        }
        if (!plsql && end > 0 && sql.charAt(end - 1) == ';') {
            end--;
        }
        return sql.substring(0, end);
    }

    /// The marker for an OUT bind, a value RETURNING ... INTO writes.
    static final class Out {
        static final Out INSTANCE = new Out();
    }

    /// The values to bind, one per placeholder in the order they appear.
    static final class Binds {
        Object[] values = new Object[0];
        int outCount;
        String[] returningNames;

        static Binds of(String sql, Object[] params) throws IOException {
            Binds binds = new Binds();
            List markers = markers(sql);
            int into = returningInto(sql);
            List values = new ArrayList();
            int highest = 0;
            for (Object each : markers) {
                int[] marker = (int[]) each;
                if (into >= 0 && marker[0] > into) {
                    values.add(Out.INSTANCE);
                    binds.outCount++;
                    continue;
                }
                int number = marker[1];
                if (number < 1) {
                    throw new Refusal("Oracle parameters are written :1, :2 and so on, "
                            + "bound by number: [" + sql + "]");
                }
                if (number > params.length) {
                    throw new Refusal("The statement names :" + number + " and "
                            + params.length + " value" + (params.length == 1 ? " was" : "s were")
                            + " supplied: [" + sql + "]");
                }
                if (number > highest) {
                    highest = number;
                }
                values.add(params[number - 1]);
            }
            if (highest < params.length) {
                throw new Refusal("The statement does not use :" + params.length + " and "
                        + params.length + " value" + (params.length == 1 ? " was" : "s were")
                        + " supplied: [" + sql + "]");
            }
            binds.values = values.toArray();
            if (binds.outCount > 0) {
                binds.returningNames = returningNames(sql, into);
            }
            return binds;
        }
    }

    /// Every bind marker outside literals, quoted names and comments, as its
    /// offset and its number (0 for a named marker).
    static List markers(String sql) {
        List out = new ArrayList();
        int length = sql.length();
        int at = 0;
        while (at < length) {
            char c = sql.charAt(at);
            if (c == '\'' || c == '"') {
                int end = at + 1;
                while (end < length) {
                    if (sql.charAt(end) == c) {
                        if (end + 1 < length && sql.charAt(end + 1) == c) {
                            end += 2;
                            continue;
                        }
                        break;
                    }
                    end++;
                }
                at = end + 1;
            } else if (c == '-' && at + 1 < length && sql.charAt(at + 1) == '-') {
                int end = sql.indexOf('\n', at);
                at = end < 0 ? length : end + 1;
            } else if (c == '/' && at + 1 < length && sql.charAt(at + 1) == '*') {
                int end = sql.indexOf("*/", at + 2);
                at = end < 0 ? length : end + 2;
            } else if (c == ':' && at + 1 < length && isWordChar(sql.charAt(at + 1))
                    && (at == 0 || sql.charAt(at - 1) != ':')) {
                int end = at + 1;
                int number = 0;
                boolean digits = true;
                while (end < length && isWordChar(sql.charAt(end))) {
                    char d = sql.charAt(end);
                    if (d >= '0' && d <= '9' && digits && number < 100000) {
                        number = number * 10 + (d - '0');
                    } else {
                        digits = false;
                    }
                    end++;
                }
                out.add(new int[] {at, digits ? number : 0});
                at = end;
            } else {
                at++;
            }
        }
        return out;
    }

    /// The offset of the INTO that follows the statement's last top-level
    /// RETURNING, or -1.
    static int returningInto(String sql) {
        String upper = upperOutsideLiterals(sql);
        int returning = lastWord(upper, "RETURNING", upper.length());
        if (returning < 0) {
            returning = lastWord(upper, "RETURN", upper.length());
        }
        if (returning < 0) {
            return -1;
        }
        return nextWord(upper, "INTO", returning);
    }

    private static String[] returningNames(String sql, int into) {
        String upper = upperOutsideLiterals(sql);
        int returning = lastWord(upper, "RETURNING", into);
        int width = 9;
        if (returning < 0) {
            returning = lastWord(upper, "RETURN", into);
            width = 6;
        }
        String list = sql.substring(returning + width, into);
        List names = new ArrayList();
        int depth = 0;
        int start = 0;
        for (int iter = 0 ; iter <= list.length() ; iter++) {
            char c = iter < list.length() ? list.charAt(iter) : ',';
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                String name = list.substring(start, iter).trim();
                if (name.length() > 1 && name.charAt(0) == '"'
                        && name.charAt(name.length() - 1) == '"') {
                    name = name.substring(1, name.length() - 1);
                }
                name = foldLabel(name);
                names.add(name);
                start = iter + 1;
            }
        }
        String[] out = new String[names.size()];
        names.toArray(out);
        return out;
    }

    /// `sql` upper-cased with every literal, quoted name and comment blanked,
    /// so that keyword searches see only the statement's own words.
    private static String upperOutsideLiterals(String sql) {
        StringBuilder out = new StringBuilder(sql.length());
        int length = sql.length();
        int at = 0;
        while (at < length) {
            char c = sql.charAt(at);
            int end;
            if (c == '\'' || c == '"') {
                end = at + 1;
                while (end < length) {
                    if (sql.charAt(end) == c) {
                        if (end + 1 < length && sql.charAt(end + 1) == c) {
                            end += 2;
                            continue;
                        }
                        break;
                    }
                    end++;
                }
                end = Math.min(length, end + 1);
            } else if (c == '-' && at + 1 < length && sql.charAt(at + 1) == '-') {
                end = sql.indexOf('\n', at);
                end = end < 0 ? length : end;
            } else if (c == '/' && at + 1 < length && sql.charAt(at + 1) == '*') {
                end = sql.indexOf("*/", at + 2);
                end = end < 0 ? length : end + 2;
            } else {
                out.append(c >= 'a' && c <= 'z' ? (char) (c - 32) : c);
                at++;
                continue;
            }
            for (int iter = at ; iter < end ; iter++) {
                out.append(' ');
            }
            at = end;
        }
        return out.toString();
    }

    private static int lastWord(String upper, String word, int before) {
        int at = upper.lastIndexOf(word, before - word.length());
        while (at >= 0) {
            boolean left = at == 0 || !isWordChar(upper.charAt(at - 1));
            int end = at + word.length();
            boolean right = end >= upper.length() || !isWordChar(upper.charAt(end));
            if (left && right) {
                return at;
            }
            at = upper.lastIndexOf(word, at - 1);
        }
        return -1;
    }

    private static int nextWord(String upper, String word, int after) {
        int at = upper.indexOf(word, after);
        while (at >= 0) {
            boolean left = at == 0 || !isWordChar(upper.charAt(at - 1));
            int end = at + word.length();
            boolean right = end >= upper.length() || !isWordChar(upper.charAt(end));
            if (left && right) {
                return at;
            }
            at = upper.indexOf(word, at + 1);
        }
        return -1;
    }

    // ---------------- value codecs ----------------

    /// A NUMBER's digits: base 100, an exponent byte then the mantissa, with
    /// negative values complemented and ended by 102.
    static String decodeNumber(byte[] data) throws IOException {
        if (data.length == 0) {
            throw new IOException("The Oracle server sent an empty NUMBER");
        }
        // Positive infinity, which BINARY_DOUBLE converted to NUMBER can hold;
        // negative infinity is the one-byte 0x00 below.
        if (data.length == 2 && (data[0] & 0xff) == 0xff && (data[1] & 0xff) == 101) {
            return "1E+126";
        }
        int exponent = data[0] & 0xff;
        boolean positive = (exponent & 0x80) != 0;
        if (data.length == 1) {
            if (positive) {
                return "0";
            }
            return "-1E+126";
        }
        if (!positive) {
            exponent = ~exponent & 0xff;
        }
        exponent -= 193;
        int last = data.length - 1;
        if (!positive && (data[last] & 0xff) == 102) {
            last--;
        }
        StringBuilder digits = new StringBuilder(last * 2);
        for (int iter = 1 ; iter <= last ; iter++) {
            int pair = data[iter] & 0xff;
            pair = positive ? pair - 1 : 101 - pair;
            if (pair < 0 || pair > 99) {
                throw new IOException("The Oracle server sent a malformed NUMBER");
            }
            digits.append((char) ('0' + pair / 10)).append((char) ('0' + pair % 10));
        }
        // The decimal point falls after (exponent + 1) pairs.
        int point = (exponent + 1) * 2;
        int start = 0;
        while (start < digits.length() - 1 && start < point - 1 && digits.charAt(start) == '0') {
            start++;
        }
        int end = digits.length();
        StringBuilder out = new StringBuilder(digits.length() + 4);
        if (!positive) {
            out.append('-');
        }
        if (point <= 0) {
            out.append("0.");
            for (int iter = point ; iter < 0 ; iter++) {
                out.append('0');
            }
            while (end > 0 && digits.charAt(end - 1) == '0') {
                end--;
            }
            out.append(digits, 0, end);
            return out.toString();
        }
        if (point >= end) {
            out.append(digits, start, end);
            for (int iter = end ; iter < point ; iter++) {
                out.append('0');
            }
            return out.toString();
        }
        while (end > point && digits.charAt(end - 1) == '0') {
            end--;
        }
        out.append(digits, start, point);
        if (end > point) {
            out.append('.').append(digits, point, end);
        }
        return out.toString();
    }

    /// A number written as decimal text (`-12.5`, `1E10`) in NUMBER form.
    static byte[] encodeNumber(String text) throws IOException {
        String value = text.trim();
        boolean negative = false;
        int at = 0;
        if (at < value.length() && (value.charAt(at) == '-' || value.charAt(at) == '+')) {
            negative = value.charAt(at) == '-';
            at++;
        }
        StringBuilder digits = new StringBuilder(value.length());
        int point = -1;
        int exponent = 0;
        for (; at < value.length() ; at++) {
            char c = value.charAt(at);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            } else if (c == '.' && point < 0) {
                point = digits.length();
            } else if (c == 'e' || c == 'E') {
                try {
                    exponent = Integer.parseInt(value.substring(at + 1).startsWith("+")
                            ? value.substring(at + 2) : value.substring(at + 1));
                } catch (NumberFormatException err) {
                    throw new IOException("Not a number: " + text, err);
                }
                // Far past NUMBER's range either way; clamped so that adding
                // it to the point below cannot overflow.
                exponent = Math.max(-1000000, Math.min(1000000, exponent));
                break;
            } else {
                throw new IOException("Not a number: " + text);
            }
        }
        if (digits.length() == 0) {
            throw new IOException("Not a number: " + text);
        }
        if (point < 0) {
            point = digits.length();
        }
        point += exponent;
        int lead = 0;
        while (lead < digits.length() && digits.charAt(lead) == '0') {
            lead++;
        }
        if (lead == digits.length()) {
            return new byte[] {(byte) 0x80};
        }
        String significant = digits.substring(lead);
        point -= lead;
        int end = significant.length();
        while (end > 0 && significant.charAt(end - 1) == '0') {
            end--;
        }
        significant = significant.substring(0, end);
        // Align to base-100 pairs: the point must fall on a pair boundary.
        StringBuilder aligned = new StringBuilder(significant.length() + 2);
        if ((point % 2 + 2) % 2 != 0) {
            aligned.append('0');
            point++;
        }
        aligned.append(significant);
        if (aligned.length() % 2 != 0) {
            aligned.append('0');
        }
        int pairs = aligned.length() / 2;
        if (pairs > 20) {
            // Refused rather than rounded: NUMBER holds 40 digits, and a value
            // silently cut to fit is a different value.
            throw new IOException("The number " + text + " has more digits than Oracle's "
                    + "NUMBER holds (40)");
        }
        int pairExponent = point / 2;
        // The exponent byte is pairExponent + 192, which must stay in
        // 128..255 for the sign bit to mean positive.
        if (pairExponent > 63 || pairExponent < -64) {
            throw new IOException("The number " + text + " is out of the range Oracle's "
                    + "NUMBER holds");
        }
        boolean sentinel = negative && pairs < 20;
        byte[] out = new byte[1 + pairs + (sentinel ? 1 : 0)];
        int biased = pairExponent + 192;
        out[0] = (byte) (negative ? ~biased : biased);
        for (int iter = 0 ; iter < pairs ; iter++) {
            int pair = (aligned.charAt(iter * 2) - '0') * 10 + (aligned.charAt(iter * 2 + 1) - '0');
            out[1 + iter] = (byte) (negative ? 101 - pair : pair + 1);
        }
        if (sentinel) {
            out[out.length - 1] = 102;
        }
        return out;
    }

    /// BINARY_DOUBLE: IEEE big-endian with the sign bit flipped for positive
    /// values and every bit flipped for negative ones, so that the bytes sort.
    static byte[] encodeDouble(double value) {
        long bits = Double.doubleToLongBits(value);
        byte[] out = new byte[8];
        for (int iter = 0 ; iter < 8 ; iter++) {
            out[iter] = (byte) (bits >>> (56 - 8 * iter));
        }
        if ((out[0] & 0x80) == 0) {
            out[0] |= (byte) 0x80;
        } else {
            for (int iter = 0 ; iter < 8 ; iter++) {
                out[iter] = (byte) ~out[iter];
            }
        }
        return out;
    }

    static double decodeBinary(byte[] data) {
        byte[] copy = new byte[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        if ((copy[0] & 0x80) != 0) {
            copy[0] &= 0x7f;
        } else {
            for (int iter = 0 ; iter < copy.length ; iter++) {
                copy[iter] = (byte) ~copy[iter];
            }
        }
        long bits = 0;
        for (byte each : copy) {
            bits = (bits << 8) | (each & 0xff);
        }
        if (copy.length == 4) {
            return Float.intBitsToFloat((int) bits);
        }
        return Double.longBitsToDouble(bits);
    }

    /// DATE and TIMESTAMP: century and year offset by 100, month, day, and the
    /// time offset by 1, then nanoseconds. A TIMESTAMP WITH TIME ZONE holds
    /// UTC and its zone; it is shown in UTC with `+00:00`.
    static String decodeDate(byte[] data, boolean withZone) {
        if (data.length < 7) {
            return "";
        }
        int year = ((data[0] & 0xff) - 100) * 100 + (data[1] & 0xff) - 100;
        StringBuilder out = new StringBuilder(32);
        pad(out, year, 4).append('-');
        pad(out, data[2] & 0xff, 2).append('-');
        pad(out, data[3] & 0xff, 2).append(' ');
        pad(out, (data[4] & 0xff) - 1, 2).append(':');
        pad(out, (data[5] & 0xff) - 1, 2).append(':');
        pad(out, (data[6] & 0xff) - 1, 2);
        if (data.length >= 11) {
            long nanos = ((data[7] & 0xffL) << 24) | ((data[8] & 0xffL) << 16)
                    | ((data[9] & 0xffL) << 8) | (data[10] & 0xffL);
            if (nanos != 0) {
                StringBuilder fraction = new StringBuilder(9);
                pad(fraction, nanos, 9);
                int end = 9;
                while (end > 3 && fraction.charAt(end - 1) == '0') {
                    end--;
                }
                out.append('.').append(fraction, 0, end);
            }
        }
        if (withZone) {
            out.append("+00:00");
        }
        return out.toString();
    }

    private static StringBuilder pad(StringBuilder out, long value, int width) {
        String digits = Long.toString(value);
        for (int iter = digits.length() ; iter < width ; iter++) {
            out.append('0');
        }
        return out.append(digits);
    }

    static String fromUtf16be(byte[] data) {
        StringBuilder out = new StringBuilder(data.length / 2);
        for (int iter = 0 ; iter + 1 < data.length ; iter += 2) {
            out.append((char) (((data[iter] & 0xff) << 8) | (data[iter + 1] & 0xff)));
        }
        return out.toString();
    }

    // ---------------- packets ----------------

    private int nextSequence() {
        sequence = (sequence + 1) & 0xff;
        if (sequence == 0) {
            sequence = 1;
        }
        return sequence;
    }

    private void function(Buf out, int code) {
        out.write(MSG_FUNCTION);
        out.write(code);
        out.write(nextSequence());
        if (fieldVersion >= FIELD_VERSION_23_1) {
            out.ub8(0);                         // token number
        }
    }

    private void sendPacket(int type, byte[] body, int flags) throws IOException {
        int length = body.length + 8;
        byte[] framed = new byte[length];
        if (largeSdu) {
            framed[0] = (byte) (length >>> 24);
            framed[1] = (byte) (length >>> 16);
            framed[2] = (byte) (length >>> 8);
            framed[3] = (byte) length;
        } else {
            framed[0] = (byte) (length >>> 8);
            framed[1] = (byte) length;
        }
        framed[4] = (byte) type;
        framed[5] = (byte) flags;
        System.arraycopy(body, 0, framed, 8, body.length);
        connection.write(framed, 0, framed.length);
    }

    /// Sends one message as however many DATA packets the SDU needs.
    private void sendData(byte[] message) throws IOException {
        int room = sdu - 10;
        int at = 0;
        do {
            int chunk = message.length - at < room ? message.length - at : room;
            byte[] body = new byte[chunk + 2];
            System.arraycopy(message, at, body, 2, chunk);
            sendPacket(PACKET_DATA, body, 0);
            at += chunk;
        } while (at < message.length);
        packet = new byte[0];
        position = 0;
    }

    private byte[] readPacket() throws IOException {
        byte[] header = wire.readFully(8);
        int length = largeSdu ? be32(header, 0) : be16(header, 0);
        packetType = header[4] & 0xff;
        if (length < 8 || length - 8 > SqlLimits.maxMessageBytes()) {
            throw new IOException("A malformed packet arrived from Oracle");
        }
        return wire.readFully(length - 8);
    }

    /// Moves to the next DATA packet of the response, answering a break with
    /// the reset the server waits for.
    private void nextData() throws IOException {
        while (true) {
            byte[] body = readPacket();
            if (packetType == PACKET_DATA) {
                if (body.length < 2) {
                    throw new IOException("A malformed DATA packet arrived from Oracle");
                }
                if ((be16(body, 0) & DATA_FLAGS_EOF) != 0) {
                    throw new IOException("The Oracle server closed the session");
                }
                if (body.length > 2) {
                    packet = body;
                    position = 2;
                    return;
                }
            } else if (packetType == PACKET_MARKER) {
                // A break: the server discards what was in flight and expects a
                // reset marker back before it answers with the error.
                sendPacket(PACKET_MARKER, new byte[] {1, 0, 2}, 0);
                while (true) {
                    byte[] marker = readPacket();
                    if (packetType == PACKET_MARKER && marker.length >= 3 && marker[2] == 2) {
                        break;
                    }
                    if (packetType == PACKET_DATA) {
                        packet = marker;
                        position = 2;
                        if (marker.length > 2) {
                            return;
                        }
                    }
                }
            } else {
                throw new IOException("The Oracle server sent packet type " + packetType
                        + " inside a response");
            }
        }
    }

    private int u8() throws IOException {
        if (position >= packet.length) {
            nextData();
        }
        return packet[position++] & 0xff;
    }

    private byte[] raw(int length) throws IOException {
        if (length < 0 || length > SqlLimits.maxMessageBytes()) {
            throw new IOException("An Oracle value of " + length + " bytes is over the limit "
                    + "(CN1_DB_MAX_MESSAGE_MB)");
        }
        byte[] out = new byte[length];
        int at = 0;
        while (at < length) {
            if (position >= packet.length) {
                nextData();
            }
            int chunk = Math.min(length - at, packet.length - position);
            System.arraycopy(packet, position, out, at, chunk);
            position += chunk;
            at += chunk;
        }
        return out;
    }

    private void skip(int length) throws IOException {
        int left = length;
        while (left > 0) {
            if (position >= packet.length) {
                nextData();
            }
            int chunk = Math.min(left, packet.length - position);
            position += chunk;
            left -= chunk;
        }
    }

    /// An unsigned integer of up to eight bytes, preceded by its length.
    private long ub() throws IOException {
        int length = u8();
        if ((length & 0x80) != 0) {
            throw new IOException("The Oracle server sent a negative length");
        }
        if (length > 8) {
            throw new IOException("The Oracle server sent a " + length + "-byte integer");
        }
        long value = 0;
        for (int iter = 0 ; iter < length ; iter++) {
            value = (value << 8) | u8();
        }
        return value;
    }

    private long sb() throws IOException {
        int length = u8();
        boolean negative = (length & 0x80) != 0;
        length &= 0x7f;
        long value = 0;
        for (int iter = 0 ; iter < length && iter < 8 ; iter++) {
            value = (value << 8) | u8();
        }
        return negative ? -value : value;
    }

    /// Bytes preceded by their length, or by 0xFE and then chunks; null for
    /// a NULL.
    private byte[] bytesWithLength() throws IOException {
        int length = u8();
        if (length == 0 || length == 0xff) {
            return null;
        }
        if (length != 0xfe) {
            return raw(length);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(256);
        while (true) {
            long chunk = ub();
            if (chunk == 0) {
                break;
            }
            if (out.size() + chunk > SqlLimits.maxMessageBytes()) {
                throw new IOException("An Oracle value is over the limit (CN1_DB_MAX_MESSAGE_MB)");
            }
            byte[] part = raw((int) chunk);
            out.write(part, 0, part.length);
        }
        return out.toByteArray();
    }

    private String text() throws IOException {
        byte[] data = bytesWithLength();
        return data == null ? null : Wire.fromUtf8(data, 0, data.length);
    }

    private static int be16(byte[] data, int at) {
        return ((data[at] & 0xff) << 8) | (data[at + 1] & 0xff);
    }

    private static int be32(byte[] data, int at) {
        return ((data[at] & 0xff) << 24) | ((data[at + 1] & 0xff) << 16)
                | ((data[at + 2] & 0xff) << 8) | (data[at + 3] & 0xff);
    }

    static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    static byte[] slice(byte[] data, int from, int to) {
        byte[] out = new byte[to - from];
        System.arraycopy(data, from, out, 0, out.length);
        return out;
    }

    static String hex(byte[] data) {
        StringBuilder out = new StringBuilder(data.length * 2);
        for (byte each : data) {
            int b = each & 0xff;
            out.append("0123456789ABCDEF".charAt(b >> 4)).append("0123456789ABCDEF".charAt(b & 15));
        }
        return out.toString();
    }

    static byte[] unhex(String text) throws IOException {
        if (text == null || text.length() % 2 != 0) {
            throw new IOException("The Oracle server sent malformed hex");
        }
        byte[] out = new byte[text.length() / 2];
        for (int iter = 0 ; iter < out.length ; iter++) {
            int high = Character.digit(text.charAt(iter * 2), 16);
            int low = Character.digit(text.charAt(iter * 2 + 1), 16);
            if (high < 0 || low < 0) {
                throw new IOException("The Oracle server sent malformed hex");
            }
            out[iter] = (byte) ((high << 4) | low);
        }
        return out;
    }

    /// A message being written, with TTC's integer and string encodings.
    static final class Buf extends ByteArrayOutputStream {
        Buf() {
            super(256);
        }

        void u16(int value) {
            write(value >>> 8);
            write(value);
        }

        void u32(long value) {
            write((int) (value >>> 24));
            write((int) (value >>> 16));
            write((int) (value >>> 8));
            write((int) value);
        }

        /// An unsigned integer as its length then its big-endian bytes.
        void ub8(long value) {
            if (value == 0) {
                write(0);
                return;
            }
            int length = (value & 0xffffffffffffff00L) == 0 ? 1
                    : (value & 0xffffffffffff0000L) == 0 ? 2
                    : (value & 0xffffffff00000000L) == 0 ? 4 : 8;
            write(length);
            for (int iter = length - 1 ; iter >= 0 ; iter--) {
                write((int) (value >>> (8 * iter)));
            }
        }

        void ub4(long value) {
            ub8(value);
        }

        void ub2(int value) {
            ub8(value);
        }

        /// Bytes preceded by their length, chunked past 252 of them.
        void bytesWithLength(byte[] data) {
            if (data.length <= 252) {
                write(data.length);
                write(data, 0, data.length);
                return;
            }
            write(0xfe);
            for (int at = 0 ; at < data.length ; ) {
                int chunk = Math.min(32767, data.length - at);
                ub4(chunk);
                write(data, at, chunk);
                at += chunk;
            }
            ub4(0);
        }

        void keyValue(String key, String value, int flags) {
            byte[] k = Wire.utf8(key);
            byte[] v = Wire.utf8(value);
            ub4(k.length);
            bytesWithLength(k);
            ub4(v.length);
            if (v.length > 0) {
                bytesWithLength(v);
            }
            ub4(flags);
        }
    }
}
