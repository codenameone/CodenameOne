/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.sql;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.codename1.backend.ConcurrencyFailureException;
import com.codename1.backend.Crypto;
import com.codename1.backend.DataIntegrityViolationException;
import com.codename1.backend.Tcp;
import com.codename1.backend.annotations.Isolation;

/// A SQL Server client speaking TDS 7.4, the Tabular Data Stream, written
/// from Microsoft's published MS-TDS specification. SQL Server 2017 and
/// later, and Azure SQL.
///
/// ```java
/// SqlServer db = SqlServer.connect("db.internal", 1433, "app", "user", "secret",
///         "require", null, 10000, 0);
/// List rows = db.query("SELECT id, name FROM users WHERE id = @p1", new Object[] {7L});
/// ```
///
/// #### Statements
///
/// A statement with parameters is an RPC call to `sp_executesql`, with the
/// parameters named `@p1`, `@p2` and so on and typed from the Java values. One
/// without is a plain batch. Values never travel inside the statement text.
///
/// #### Encryption
///
/// The TLS handshake is carried inside PRELOGIN packets, which is how every
/// SQL Server before TDS 8.0 negotiates it. The client asks for the whole
/// session to be encrypted, never the login alone, for every `sslmode` but
/// `disable`. A login sends the password scrambled, not hashed, so a server
/// without TLS is refused unless `sslmode=disable` says the plaintext is
/// deliberate.
///
/// #### Sign-in
///
/// A SQL login by name and password, or a Windows account over NTLMv2 when the
/// user is written `DOMAIN\\user`. Kerberos and Microsoft Entra ID are not
/// implemented.
///
/// #### Values
///
/// Integers and bits arrive as `Long`, floats as `Double`, binary as
/// `byte[]`, and everything else as a `String` in the form the other engines
/// use: decimals and money as digits, dates and times as
/// `yyyy-MM-dd HH:mm:ss.fff`.
public final class SqlServer {
    private static final int PACKET_BATCH = 0x01;
    private static final int PACKET_RPC = 0x03;
    private static final int PACKET_LOGIN = 0x10;
    private static final int PACKET_SSPI = 0x11;
    private static final int PACKET_PRELOGIN = 0x12;

    private static final int ENCRYPT_ON = 1;
    private static final int ENCRYPT_NOT_SUPPORTED = 2;
    private static final int ENCRYPT_REQUIRED = 3;

    private static final int TOKEN_RETURN_STATUS = 0x79;
    private static final int TOKEN_COLMETADATA = 0x81;
    private static final int TOKEN_TABNAME = 0xa4;
    private static final int TOKEN_COLINFO = 0xa5;
    private static final int TOKEN_ORDER = 0xa9;
    private static final int TOKEN_ERROR = 0xaa;
    private static final int TOKEN_INFO = 0xab;
    private static final int TOKEN_RETURN_VALUE = 0xac;
    private static final int TOKEN_LOGINACK = 0xad;
    private static final int TOKEN_FEATURE_EXT_ACK = 0xae;
    private static final int TOKEN_ROW = 0xd1;
    private static final int TOKEN_NBCROW = 0xd2;
    private static final int TOKEN_ENVCHANGE = 0xe3;
    private static final int TOKEN_SESSION_STATE = 0xe4;
    private static final int TOKEN_SSPI = 0xed;
    private static final int TOKEN_FED_AUTH_INFO = 0xee;
    private static final int TOKEN_DONE = 0xfd;
    private static final int TOKEN_DONEPROC = 0xfe;
    private static final int TOKEN_DONEINPROC = 0xff;

    private static final int TYPE_NULL = 0x1f;
    private static final int TYPE_IMAGE = 0x22;
    private static final int TYPE_TEXT = 0x23;
    private static final int TYPE_GUID = 0x24;
    private static final int TYPE_INTN = 0x26;
    private static final int TYPE_DATE = 0x28;
    private static final int TYPE_TIME = 0x29;
    private static final int TYPE_DATETIME2 = 0x2a;
    private static final int TYPE_DATETIMEOFFSET = 0x2b;
    private static final int TYPE_INT1 = 0x30;
    private static final int TYPE_BIT = 0x32;
    private static final int TYPE_INT2 = 0x34;
    private static final int TYPE_INT4 = 0x38;
    private static final int TYPE_SMALLDATETIME = 0x3a;
    private static final int TYPE_FLOAT4 = 0x3b;
    private static final int TYPE_MONEY = 0x3c;
    private static final int TYPE_DATETIME = 0x3d;
    private static final int TYPE_FLOAT8 = 0x3e;
    private static final int TYPE_VARIANT = 0x62;
    private static final int TYPE_NTEXT = 0x63;
    private static final int TYPE_BITN = 0x68;
    private static final int TYPE_DECIMAL = 0x6a;
    private static final int TYPE_NUMERIC = 0x6c;
    private static final int TYPE_FLOATN = 0x6d;
    private static final int TYPE_MONEYN = 0x6e;
    private static final int TYPE_DATETIMEN = 0x6f;
    private static final int TYPE_MONEY4 = 0x7a;
    private static final int TYPE_INT8 = 0x7f;
    private static final int TYPE_VARBINARY = 0xa5;
    private static final int TYPE_VARCHAR = 0xa7;
    private static final int TYPE_BINARY = 0xad;
    private static final int TYPE_CHAR = 0xaf;
    private static final int TYPE_NVARCHAR = 0xe7;
    private static final int TYPE_NCHAR = 0xef;
    private static final int TYPE_UDT = 0xf0;
    private static final int TYPE_XML = 0xf1;

    /// The most bytes a short NVARCHAR or VARBINARY parameter carries; past
    /// it the value travels as a (MAX) type in chunks.
    private static final int SHORT_VALUE = 8000;

    /// How many statements' parameter descriptions are remembered, see
    /// [#binaryNulls].
    private static final int DESCRIBED_STATEMENTS = 256;

    private final Tcp connection;
    private final Wire wire;
    /// VOLATILE: isClosed() is read by whoever is deciding whether to reuse this
    /// session -- a pool, a health check -- from a thread that does not run its
    /// statements.
    private volatile boolean closed; //NOPMD AvoidUsingVolatile - read by a pool's thread, see above

    private int packetSize = 4096;
    /// The server's name for the open transaction, which every request has to
    /// carry while one is open. Zero outside a transaction.
    private long transaction;
    private boolean isolationChanged;
    private boolean encrypted;
    /// The key the last INSERT generated, see [#lastInsertId].
    private long lastIdentity;
    private boolean readingIdentity;

    // The response being read: what is left of the current packet, and
    // whether it is the message's last.
    private int packetLeft;
    private boolean lastPacket;

    // What a response left behind for its caller.
    private IOException failure;
    private boolean fatal;
    private boolean loginAcknowledged;
    private byte[] sspi;
    private String routeHost;
    private int routePort;

    private final Map describedNulls = new HashMap();

    private SqlServer(Tcp connection) {
        this.connection = connection;
        this.wire = new Wire(connection);
    }

    /// Opens a session.
    ///
    /// `sslMode` is what the other engines take: `require` (and `verify-full`)
    /// demand TLS to a certificate that verifies for `host`, `verify-ca` a
    /// chain to `caFile` without the name, `prefer` uses TLS and fails when the
    /// server has none, since a login cannot be protected without it, and
    /// `disable` is plaintext, deliberately.
    ///
    /// A user written `DOMAIN\\user` signs in as that Windows account over
    /// NTLMv2; any other is a SQL login.
    ///
    /// An Azure SQL gateway that answers the login with a redirect is followed,
    /// once.
    public static SqlServer connect(String host, int port, String database, String user,
            String password, String sslMode, String caFile, int timeoutMillis,
            int socketTimeoutMillis) throws IOException {
        String mode = SqlLimits.canonicalSslMode(sslMode == null ? "prefer" : sslMode);
        SqlLimits.requireRootForVerifyCa(mode, caFile);
        String targetHost = host;
        int targetPort = port <= 0 ? 1433 : port;
        for (int hop = 0 ; ; hop++) {
            SqlServer session = open(targetHost, targetPort, database, user, password, mode,
                    sslMode, caFile, timeoutMillis, socketTimeoutMillis);
            if (session.routeHost == null) {
                return session;
            }
            session.close();
            if (hop > 0) {
                throw new IOException("The server at " + host + " redirected the login twice");
            }
            targetHost = session.routeHost;
            targetPort = session.routePort;
        }
    }

    private static SqlServer open(String host, int port, String database, String user,
            String password, String mode, String modeAsWritten, String caFile,
            int timeoutMillis, int socketTimeoutMillis) throws IOException {
        Tcp connection = Tcp.connect(host, port, timeoutMillis);
        try {
            if (socketTimeoutMillis > 0) {
                connection.setReadTimeout(socketTimeoutMillis);
            }
            SqlServer session = new SqlServer(connection);
            session.negotiate(host, mode, modeAsWritten, caFile);
            session.login(host, database, user == null ? "" : user,
                    password == null ? "" : password);
            return session;
        } catch (IOException err) {
            connection.close();
            throw err;
        }
    }

    // ---------------- PRELOGIN and encryption ----------------

    private void negotiate(String host, String mode, String modeAsWritten, String caFile)
            throws IOException {
        boolean plaintextByChoice = "disable".equals(mode);
        // Five options, each a token, an offset and a length, then the values:
        // VERSION, ENCRYPTION, INSTOPT, THREADID, MARS.
        int[] lengths = {6, 1, 1, 4, 1};
        byte[] request = new byte[26 + 13];
        int offset = 26;
        int at = 0;
        for (int iter = 0 ; iter < lengths.length ; iter++) {
            request[at++] = (byte) iter;
            request[at++] = (byte) (offset >> 8);
            request[at++] = (byte) offset;
            request[at++] = 0;
            request[at++] = (byte) lengths[iter];
            offset += lengths[iter];
        }
        request[at] = (byte) 0xff;
        request[26 + 6] = (byte) (plaintextByChoice ? ENCRYPT_NOT_SUPPORTED : ENCRYPT_ON);
        send(PACKET_PRELOGIN, request, request.length);

        byte[] answer = readWholeMessage(4096);
        int offered = -1;
        for (int iter = 0 ; iter + 5 <= answer.length && (answer[iter] & 0xff) != 0xff ; iter += 5) {
            if (answer[iter] == 1) {
                int where = ((answer[iter + 1] & 0xff) << 8) | (answer[iter + 2] & 0xff);
                if (where < answer.length) {
                    offered = answer[where] & 0xff;
                }
            }
        }
        if (plaintextByChoice) {
            if (offered == ENCRYPT_REQUIRED) {
                throw new IOException("The server at " + host + " requires encryption and "
                        + "sslmode=" + modeAsWritten + " refuses it");
            }
            return;
        }
        if (offered != ENCRYPT_ON && offered != ENCRYPT_REQUIRED) {
            if (SqlLimits.demandsTls(mode)) {
                throw new IOException("The server at " + host
                        + " refused TLS and sslmode=" + modeAsWritten);
            }
            throw new IOException("The server at " + host + " offers no TLS, and a SQL "
                    + "Server login sends the password in a form anyone on the path can "
                    + "read. Set sslmode=disable to connect in the clear deliberately.");
        }
        try {
            connection.startTlsTunnelled(host, caFile, SqlLimits.verifiesHost(mode),
                    PACKET_PRELOGIN);
            encrypted = true;
        } catch (IOException err) {
            if (SqlLimits.demandsTls(mode)) {
                throw err;
            }
            throw new IOException("TLS to " + host + " could not be verified ("
                    + err.getMessage() + "). Point sslrootcert at the server's CA, or set "
                    + "sslmode=disable to connect in the clear deliberately.", err);
        }
    }

    // ---------------- LOGIN7 ----------------

    private void login(String host, String database, String user, String password)
            throws IOException {
        int slash = user.indexOf('\\');
        boolean windows = slash > 0;
        String domain = windows ? user.substring(0, slash) : "";
        String account = windows ? user.substring(slash + 1) : user;

        byte[] secret = Wire.utf16le(windows ? "" : password);
        for (int iter = 0 ; iter < secret.length ; iter++) {
            // The protocol's own scrambling: swap the nibbles, flip alternate
            // bits. It hides the password from a glance at a trace and from
            // nothing else, which is why a login is not sent without TLS.
            int b = secret[iter] & 0xff;
            secret[iter] = (byte) ((((b << 4) & 0xf0) | (b >> 4)) ^ 0xa5);
        }
        byte[] negotiate = windows ? Ntlm.negotiate() : new byte[0];
        byte[][] fields = {
            new byte[0],                                   // client host name
            Wire.utf16le(windows ? "" : account),
            secret,
            Wire.utf16le("Codename One"),                  // application
            Wire.utf16le(host),                            // server
            new byte[0],                                   // feature extension, unused
            Wire.utf16le("cn1"),                           // interface library
            new byte[0],                                   // language
            Wire.utf16le(database == null ? "" : database),
        };
        ByteArrayOutputStream out = new ByteArrayOutputStream(256);
        int fixed = 94;
        int variable = negotiate.length;
        for (byte[] field : fields) {
            variable += field.length;
        }
        le32(out, fixed + variable);
        le32(out, 0x74000004);          // TDS 7.4
        le32(out, 4096);                // packet size asked for
        le32(out, 1);                   // client program version
        le32(out, 0);                   // client process
        le32(out, 0);                   // connection
        out.write(0xe0);                // warn on the database and language changes
        out.write(0x03 | (windows ? 0x80 : 0)); // ODBC session defaults; integrated security
        out.write(0);
        out.write(0);
        le32(out, 0);                   // time zone
        le32(out, 0x0409);              // locale
        int offset = fixed;
        for (int iter = 0 ; iter < fields.length ; iter++) {
            le16(out, offset);
            // The extension slot counts bytes; every other counts characters.
            le16(out, iter == 5 ? 0 : fields[iter].length / 2);
            offset += fields[iter].length;
        }
        for (int iter = 0 ; iter < 6 ; iter++) {
            out.write(0);               // client id
        }
        le16(out, offset);
        le16(out, negotiate.length);
        offset += negotiate.length;
        le16(out, offset);              // attached database file
        le16(out, 0);
        le16(out, offset);              // new password
        le16(out, 0);
        le32(out, 0);                   // long SSPI length
        for (byte[] field : fields) {
            out.write(field, 0, field.length);
        }
        out.write(negotiate, 0, negotiate.length);
        send(PACKET_LOGIN, out.toByteArray(), out.size());
        readResponse(null);
        if (failure == null && windows && sspi != null) {
            byte[] answer = Ntlm.authenticate(sspi, domain, account, password,
                    Crypto.randomBytes(8), Ntlm.fileTime(System.currentTimeMillis()));
            send(PACKET_SSPI, answer, answer.length);
            readResponse(null);
        }
        if (failure != null) {
            throw failure;
        }
        if (!loginAcknowledged && routeHost == null) {
            throw new IOException("The server at " + host + " did not acknowledge the login");
        }
    }

    // ---------------- statements ----------------

    /// Runs a statement that returns no rows and answers with the number of
    /// rows it affected.
    public int execute(String sql, Object[] params) throws IOException {
        return run(sql, params, null);
    }

    /// Runs a statement and answers with its rows, each a `Map` of column name
    /// to value in projection order. A statement that produces several result
    /// sets answers with the first.
    public List query(String sql, Object[] params) throws IOException {
        QueryRows rows = new QueryRows(0);
        run(sql, params, rows);
        return rows;
    }

    private int run(String sql, Object[] params, QueryRows rows) throws IOException {
        checkOpen();
        int count = params == null ? 0 : params.length;
        if (count > 0 && !names(sql, count)) {
            // sp_executesql takes a parameter the statement never names and says
            // nothing, where every other engine refuses the surplus value. A
            // statement that does not name its LAST parameter was given too many.
            throw new IOException("The statement does not use @p" + count + " and " + count
                    + " value" + (count == 1 ? " was" : "s were") + " supplied: [" + sql + "]");
        }
        boolean[] binary = null;
        for (int iter = 0 ; iter < count ; iter++) {
            if (params[iter] == null) {
                binary = binaryNulls(sql, count);
                break;
            }
        }
        int affected;
        // An INSERT asks for the key it generated in the same batch, because
        // SCOPE_IDENTITY() answers for the batch it is asked in and nothing asked
        // afterwards can see into this one.
        boolean insert = rows == null && startsWithInsert(sql);
        String sent = sql;
        if (rows == null) {
            // The key belongs to the statement that generated it. Kept across
            // statements, a MERGE or an UPDATE would answer with the key of an
            // INSERT before it.
            lastIdentity = 0;
        }
        if (insert) {
            sent = sql + "\n; SELECT SCOPE_IDENTITY()";
        }
        readingIdentity = insert;
        try {
            if (count == 0) {
                ByteArrayOutputStream out = new ByteArrayOutputStream(64 + sent.length() * 2);
                headers(out);
                byte[] text = Wire.utf16le(sent);
                out.write(text, 0, text.length);
                send(PACKET_BATCH, out.toByteArray(), out.size());
            } else {
                sendRpc(sent, params, binary);
            }
            affected = readResponse(rows);
        } catch (IOException err) {
            // Half a request sent or half an answer read: nothing that follows
            // on this connection can be trusted to be the answer to its question.
            close();
            throw err;
        }
        if (failure != null) {
            IOException err = failure;
            failure = null;
            if (fatal) {
                close();
            }
            throw decorate(err, sql);
        }
        return affected;
    }

    /// Whether `sql` names the parameter `@p<number>`, as a whole name.
    private static boolean names(String sql, int number) {
        String name = "@p" + number;
        int from = 0;
        while (true) {
            int at = sql.indexOf(name, from);
            if (at < 0) {
                at = sql.indexOf("@P" + number, from);
                if (at < 0) {
                    return false;
                }
            }
            int end = at + name.length();
            if (end >= sql.length() || sql.charAt(end) < '0' || sql.charAt(end) > '9') {
                return true;
            }
            from = end;
        }
    }

    private static boolean startsWithInsert(String sql) {
        int at = 0;
        while (at < sql.length() && sql.charAt(at) <= ' ') {
            at++;
        }
        return sql.regionMatches(true, at, "INSERT", 0, 6);
    }

    /// The key the last INSERT on this session generated, or 0 when it generated
    /// none.
    public long lastInsertId() {
        return lastIdentity;
    }

    private static IOException decorate(IOException err, String sql) {
        String text = err.getMessage() + " [" + sql + "]";
        if (err instanceof ConcurrencyFailureException) {
            return new ConcurrencyFailureException(text);
        }
        if (err instanceof DataIntegrityViolationException) {
            return new DataIntegrityViolationException(text);
        }
        return new IOException(text);
    }

    /// An `sp_executesql` call: the statement, the declaration of its
    /// parameters, then each value under the type that was declared for it.
    private void sendRpc(String sql, Object[] params, boolean[] binaryNulls)
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(256 + sql.length() * 2);
        headers(out);
        le16(out, 0xffff);      // a well-known procedure, by number
        le16(out, 10);          // sp_executesql
        le16(out, 0);
        StringBuilder declared = new StringBuilder(params.length * 20);
        for (int iter = 0 ; iter < params.length ; iter++) {
            if (iter > 0) {
                declared.append(',');
            }
            declared.append("@p").append(iter + 1).append(' ');
            declared.append(typeName(params[iter],
                    binaryNulls != null && iter < binaryNulls.length && binaryNulls[iter]));
        }
        out.write(0);
        out.write(0);
        writeText(out, sql);
        out.write(0);
        out.write(0);
        writeText(out, declared.toString());
        for (int iter = 0 ; iter < params.length ; iter++) {
            out.write(0);       // unnamed: bound by position
            out.write(0);       // an input
            writeValue(out, params[iter],
                    binaryNulls != null && iter < binaryNulls.length && binaryNulls[iter]);
        }
        send(PACKET_RPC, out.toByteArray(), out.size());
    }

    private static String typeName(Object value, boolean binaryNull) {
        if (value == null) {
            return binaryNull ? "varbinary(8000)" : "nvarchar(4000)";
        }
        if (value instanceof Long) {
            return "bigint";
        }
        if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return "int";
        }
        if (value instanceof Boolean) {
            return "bit";
        }
        if (value instanceof Double || value instanceof Float) {
            return "float";
        }
        if (value instanceof byte[]) {
            return ((byte[]) value).length > SHORT_VALUE ? "varbinary(max)" : "varbinary(8000)";
        }
        return value.toString().length() * 2 > SHORT_VALUE ? "nvarchar(max)" : "nvarchar(4000)";
    }

    private static void writeValue(ByteArrayOutputStream out, Object value, boolean binaryNull) {
        if (value == null) {
            if (binaryNull) {
                out.write(TYPE_VARBINARY);
                le16(out, SHORT_VALUE);
            } else {
                out.write(TYPE_NVARCHAR);
                le16(out, SHORT_VALUE);
                collation(out);
            }
            le16(out, 0xffff);
        } else if (value instanceof Long) {
            out.write(TYPE_INTN);
            out.write(8);
            out.write(8);
            le64(out, ((Long) value).longValue());
        } else if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            out.write(TYPE_INTN);
            out.write(4);
            out.write(4);
            le32(out, ((Number) value).intValue());
        } else if (value instanceof Boolean) {
            out.write(TYPE_BITN);
            out.write(1);
            out.write(1);
            out.write(((Boolean) value).booleanValue() ? 1 : 0);
        } else if (value instanceof Double || value instanceof Float) {
            out.write(TYPE_FLOATN);
            out.write(8);
            out.write(8);
            le64(out, Double.doubleToLongBits(((Number) value).doubleValue()));
        } else if (value instanceof byte[]) {
            byte[] data = (byte[]) value;
            out.write(TYPE_VARBINARY);
            writeBytes(out, data);
        } else {
            writeText(out, value.toString());
        }
    }

    private static void writeText(ByteArrayOutputStream out, String text) {
        byte[] data = Wire.utf16le(text);
        out.write(TYPE_NVARCHAR);
        if (data.length > SHORT_VALUE) {
            le16(out, 0xffff);
            collation(out);
            writeChunked(out, data);
        } else {
            le16(out, SHORT_VALUE);
            collation(out);
            le16(out, data.length);
            out.write(data, 0, data.length);
        }
    }

    private static void writeBytes(ByteArrayOutputStream out, byte[] data) {
        if (data.length > SHORT_VALUE) {
            le16(out, 0xffff);
            writeChunked(out, data);
        } else {
            le16(out, SHORT_VALUE);
            le16(out, data.length);
            out.write(data, 0, data.length);
        }
    }

    /// A (MAX) value: its whole length, one chunk holding all of it, and the
    /// empty chunk that ends it.
    private static void writeChunked(ByteArrayOutputStream out, byte[] data) {
        le64(out, data.length);
        le32(out, data.length);
        out.write(data, 0, data.length);
        le32(out, 0);
    }

    /// The server's default collation: a parameter's own does not matter, it
    /// takes the collation of what it is compared with.
    private static void collation(ByteArrayOutputStream out) {
        for (int iter = 0 ; iter < 5 ; iter++) {
            out.write(0);
        }
    }

    /// The headers every request opens with: one, naming the transaction the
    /// request belongs to.
    private void headers(ByteArrayOutputStream out) {
        le32(out, 22);
        le32(out, 18);
        le16(out, 2);
        le64(out, transaction);
        le32(out, 1);
    }

    /// Which of a statement's parameters are binary columns, for the ones that
    /// are null.
    ///
    /// A null has to be sent under SOME type, and SQL Server converts a
    /// character null to every type implicitly except the binary ones, where it
    /// refuses the statement. So a statement with a null in it is described
    /// once, by the server, and the answer kept for the session: one extra
    /// round trip the first time a statement meets a null, none after.
    private boolean[] binaryNulls(String sql, int count) throws IOException {
        Object known = describedNulls.get(sql);
        if (known != null) {
            return (boolean[]) known;
        }
        boolean[] binary = new boolean[count];
        QueryRows rows = new QueryRows(0);
        try {
            sendRpc("EXEC sp_describe_undeclared_parameters @tsql = @p1", new Object[] {sql}, null);
            readResponse(rows);
        } catch (IOException err) {
            close();
            throw err;
        }
        if (failure != null) {
            // A statement the server cannot describe -- one that names a
            // temporary table, say -- is sent with character nulls, which is
            // right for every column but a binary one.
            boolean dead = fatal;
            IOException err = failure;
            failure = null;
            if (dead) {
                close();
                throw err;
            }
        } else {
            for (int iter = 0 ; iter < rows.size() ; iter++) {
                Map row = (Map) rows.get(iter);
                Object ordinal = row.get("parameter_ordinal");
                Object type = row.get("suggested_system_type_id");
                if (ordinal instanceof Long && type instanceof Long) {
                    int index = (int) ((Long) ordinal).longValue() - 1;
                    long id = ((Long) type).longValue();
                    // image, varbinary, binary, timestamp
                    if (index >= 0 && index < count
                            && (id == 34 || id == 165 || id == 173 || id == 189)) {
                        binary[index] = true;
                    }
                }
            }
        }
        if (describedNulls.size() >= DESCRIBED_STATEMENTS) {
            describedNulls.clear();
        }
        describedNulls.put(sql, binary);
        return binary;
    }

    // ---------------- transactions ----------------

    /// Opens a transaction.
    public void begin() throws IOException {
        run("BEGIN TRANSACTION", null, null);
    }

    /// Opens a transaction. SQL Server has no read-only transaction, so this is
    /// [#begin].
    public void beginReadOnly() throws IOException {
        begin();
    }

    /// Opens a transaction at an isolation level. The level is a session
    /// setting on SQL Server, so it is put back to READ COMMITTED when the
    /// transaction ends.
    public void begin(boolean readOnly, Isolation isolation) throws IOException {
        if (isolation == Isolation.DEFAULT) {
            begin();
            return;
        }
        isolationChanged = true;
        run("SET TRANSACTION ISOLATION LEVEL " + isolation.name().replace('_', ' ')
                + "; BEGIN TRANSACTION", null, null);
    }

    /// Commits the open transaction.
    public void commit() throws IOException {
        try {
            run("COMMIT TRANSACTION", null, null);
        } finally {
            restoreIsolation();
        }
    }

    /// Rolls the open transaction back. A transaction the server already ended,
    /// as it does for the victim of a deadlock, is not an error here.
    public void rollback() throws IOException {
        try {
            run("IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION", null, null);
        } finally {
            restoreIsolation();
        }
    }

    private void restoreIsolation() {
        if (isolationChanged && !closed) {
            isolationChanged = false;
            try {
                run("SET TRANSACTION ISOLATION LEVEL READ COMMITTED", null, null);
            } catch (IOException err) {
                // A session that cannot be put back is not handed out again.
                close();
            }
        }
    }

    /// Savepoints, under the verbs the other engines use. SQL Server spells them
    /// SAVE TRANSACTION and ROLLBACK TRANSACTION, and has nothing to release: a
    /// savepoint lasts until its transaction ends.
    public void savepoint(String verb, String name) throws IOException {
        String checked = Dialect.checkIdentifier(name);
        if (checked.length() > 32) {
            throw new IOException("A SQL Server savepoint name is at most 32 characters: "
                    + name);
        }
        if ("SAVEPOINT".equals(verb)) {
            run("SAVE TRANSACTION " + checked, null, null);
        } else if ("ROLLBACK TO SAVEPOINT".equals(verb)) {
            run("ROLLBACK TRANSACTION " + checked, null, null);
        } else if (!"RELEASE SAVEPOINT".equals(verb)) {
            throw new IOException("Not a savepoint statement: " + verb);
        }
    }

    /// Closes the connection. An open transaction is rolled back by the server.
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        connection.close();
    }

    public boolean isClosed() {
        return closed;
    }

    private void checkOpen() throws IOException {
        if (closed) {
            throw new IOException("The SQL Server connection is closed");
        }
    }

    // ---------------- packets ----------------

    /// Sends one message, as however many packets it takes, in a single write.
    private void send(int type, byte[] data, int length) throws IOException {
        int payload = packetSize - 8;
        int packets = length == 0 ? 1 : (length + payload - 1) / payload;
        byte[] framed = new byte[length + packets * 8];
        int at = 0;
        int out = 0;
        for (int iter = 0 ; iter < packets ; iter++) {
            int chunk = length - at < payload ? length - at : payload;
            framed[out] = (byte) type;
            framed[out + 1] = (byte) (iter == packets - 1 ? 1 : 0);
            framed[out + 2] = (byte) ((chunk + 8) >> 8);
            framed[out + 3] = (byte) (chunk + 8);
            framed[out + 6] = (byte) (iter + 1);
            System.arraycopy(data, at, framed, out + 8, chunk);
            at += chunk;
            out += chunk + 8;
        }
        if (encrypted && packets > 1) {
            // ONE PACKET TO A TLS RECORD. SQL Server decrypts into a buffer the
            // size of a packet, and drops the connection, saying nothing, on a
            // record that holds more than one: so each packet is a write of its
            // own, which is a record of its own on both runtimes.
            for (int from = 0 ; from < framed.length ; from += packetSize) {
                connection.write(framed, from, framed.length - from < packetSize
                        ? framed.length - from : packetSize);
            }
        } else {
            connection.write(framed, 0, framed.length);
        }
        packetLeft = 0;
        lastPacket = false;
    }

    private void nextPacket() throws IOException {
        if (lastPacket) {
            throw new IOException("The SQL Server response ended inside a value");
        }
        byte[] header = wire.readFully(8);
        int total = ((header[2] & 0xff) << 8) | (header[3] & 0xff);
        if (total < 8) {
            throw new IOException("A malformed packet arrived from SQL Server");
        }
        packetLeft = total - 8;
        lastPacket = (header[1] & 1) != 0;
    }

    /// Whether the response has anything left, moving to its next packet when
    /// the current one is used up.
    private boolean more() throws IOException {
        while (packetLeft == 0) {
            if (lastPacket) {
                return false;
            }
            nextPacket();
        }
        return true;
    }

    private byte[] readWholeMessage(int limit) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(64);
        while (more()) {
            if (out.size() + packetLeft > limit) {
                throw new IOException("The server's answer to the first packet is too long");
            }
            byte[] part = wire.readFully(packetLeft);
            packetLeft = 0;
            out.write(part, 0, part.length);
        }
        return out.toByteArray();
    }

    private int u8() throws IOException {
        if (packetLeft == 0 && !more()) {
            throw new IOException("The SQL Server response ended inside a value");
        }
        packetLeft--;
        int value = wire.read();
        if (value < 0) {
            throw new IOException("The connection closed inside a SQL Server response");
        }
        return value;
    }

    private int u16() throws IOException {
        int low = u8();
        return low | (u8() << 8);
    }

    private int i32() throws IOException {
        int low = u16();
        return low | (u16() << 16);
    }

    private long i64() throws IOException {
        long low = i32() & 0xffffffffL;
        return low | ((long) i32() << 32);
    }

    private byte[] bytes(long length) throws IOException {
        if (length < 0 || length > SqlLimits.maxMessageBytes()) {
            throw new IOException("A SQL Server value of " + length + " bytes is over the "
                    + "limit (CN1_DB_MAX_MESSAGE_MB)");
        }
        byte[] out = new byte[(int) length];
        int at = 0;
        while (at < out.length) {
            if (packetLeft == 0 && !more()) {
                throw new IOException("The SQL Server response ended inside a value");
            }
            int chunk = out.length - at < packetLeft ? out.length - at : packetLeft;
            wire.readFully(out, at, chunk);
            packetLeft -= chunk;
            at += chunk;
        }
        return out;
    }

    /// A string under a one-byte character count.
    private String shortName() throws IOException {
        byte[] data = bytes(u8() * 2);
        return Wire.fromUtf16le(data, 0, data.length);
    }

    /// A string under a two-byte character count.
    private String longName() throws IOException {
        byte[] data = bytes(u16() * 2);
        return Wire.fromUtf16le(data, 0, data.length);
    }

    // ---------------- the token stream ----------------

    private static final class Column {
        String name;
        int type;
        int length;
        int scale;
        boolean utf8;
        boolean chunked;
    }

    /// Reads one response to its end and answers with the rows it affected.
    /// Rows of the first result set go to `rows` when it is not null. An error
    /// the server reported is left in `failure`, to be thrown once the whole
    /// response has been read and the connection is back in step.
    private int readResponse(QueryRows rows) throws IOException {
        long affected = 0;
        Column[] columns = null;
        int resultSets = 0;
        failure = null;
        fatal = false;
        sspi = null;
        while (more()) {
            int token = u8();
            switch (token) {
                case TOKEN_COLMETADATA: {
                    columns = readColumns();
                    resultSets++;
                    if (rows != null && resultSets == 1) {
                        String[] kinds = new String[columns.length];
                        for (int iter = 0 ; iter < columns.length ; iter++) {
                            kinds[iter] = kind(columns[iter].type);
                        }
                        rows.setColumnCount(columns.length);
                        rows.setColumnTypes(kinds);
                    }
                    break;
                }
                case TOKEN_ROW:
                case TOKEN_NBCROW: {
                    if (columns == null) {
                        throw new IOException("SQL Server sent a row before its columns");
                    }
                    byte[] nulls = token == TOKEN_NBCROW ? bytes((columns.length + 7) / 8) : null;
                    Map row = rows != null && resultSets == 1 ? new LinkedHashMap() : null;
                    for (int iter = 0 ; iter < columns.length ; iter++) {
                        Object value = nulls != null && (nulls[iter / 8] & (1 << (iter % 8))) != 0
                                ? null : readValue(columns[iter]);
                        if (readingIdentity && iter == 0 && value != null) {
                            lastIdentity = identity(value.toString());
                        }
                        if (row != null) {
                            row.put(columns[iter].name, value);
                        }
                    }
                    if (row != null) {
                        rows.add(row);
                    }
                    break;

                }
                case TOKEN_DONE:
                case TOKEN_DONEPROC:
                case TOKEN_DONEINPROC: {
                    int status = u16();
                    int command = u16();
                    long count = i64();
                    // 0x10: the count is meaningful. 0xc1 is a SELECT, whose
                    // count is rows returned, not rows changed.
                    if ((status & 0x10) != 0 && command != 0xc1) {
                        affected = count;
                    }
                    break;
                }
                case TOKEN_ERROR:
                    readError();
                    break;
                case TOKEN_ENVCHANGE:
                    readEnvironmentChange();
                    break;
                case TOKEN_LOGINACK:
                    bytes(u16());
                    loginAcknowledged = true;
                    break;
                case TOKEN_SSPI:
                    sspi = bytes(u16());
                    break;
                case TOKEN_INFO:
                case TOKEN_ORDER:
                case TOKEN_TABNAME:
                case TOKEN_COLINFO:
                    bytes(u16());
                    break;
                case TOKEN_RETURN_STATUS:
                    i32();
                    break;
                case TOKEN_RETURN_VALUE: {
                    u16();
                    shortName();
                    u8();
                    Column column = new Column();
                    i32();
                    u16();
                    readType(column, false);
                    readValue(column);
                    break;
                }
                case TOKEN_FEATURE_EXT_ACK:
                    while (u8() != 0xff) {
                        bytes(i32() & 0xffffffffL);
                    }
                    break;
                case TOKEN_SESSION_STATE:
                case TOKEN_FED_AUTH_INFO:
                    bytes(i32() & 0xffffffffL);
                    break;
                default:
                    throw new IOException("SQL Server sent a token this client does not "
                            + "read: 0x" + Integer.toHexString(token));
            }
        }
        return affected > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) affected;
    }

    private void readError() throws IOException {
        u16();
        int number = i32();
        u8();                       // state
        int severity = u8();
        String message = longName();
        shortName();                // server
        shortName();                // procedure
        i32();                      // line
        if (severity >= 20) {
            fatal = true;
        }
        if (failure == null) {
            String text = "SQL Server error " + number + ": " + message;
            failure = isConcurrencyFailure(number) ? new ConcurrencyFailureException(text)
                    : isIntegrityViolation(number) ? new DataIntegrityViolationException(text)
                    : new IOException(text);
        }
    }

    /// The victim of a deadlock (1205), a snapshot update conflict (3960) and a
    /// lock that was not granted in time (1222): each is the answer to "run it
    /// again".
    static boolean isConcurrencyFailure(int number) {
        return number == 1205 || number == 3960 || number == 1222;
    }

    /// A duplicate in a unique index (2601) or constraint (2627), a foreign key
    /// or check constraint (547), and NULL into a column that takes none (515).
    static boolean isIntegrityViolation(int number) {
        return number == 2601 || number == 2627 || number == 547 || number == 515;
    }

    private void readEnvironmentChange() throws IOException {
        byte[] body = bytes(u16());
        if (body.length == 0) {
            return;
        }
        int type = body[0] & 0xff;
        switch (type) {
            case 4: {
                // The packet size the server settled on, as decimal text.
                if (body.length > 1) {
                    int chars = body[1] & 0xff;
                    if (2 + chars * 2 <= body.length) {
                        int size = 0;
                        for (int iter = 0 ; iter < chars ; iter++) {
                            int c = body[2 + iter * 2] & 0xff;
                            if (c < '0' || c > '9' || size > 100000) {
                                size = 0;
                                break;
                            }
                            size = size * 10 + (c - '0');
                        }
                        if (size >= 512 && size <= 32767) {
                            packetSize = size;
                        }
                    }
                }
                break;
            }
            case 8:
                // A transaction began: its descriptor is the new value.
                if (body.length >= 10 && (body[1] & 0xff) == 8) {
                    long descriptor = 0;
                    for (int iter = 0 ; iter < 8 ; iter++) {
                        descriptor |= (body[2 + iter] & 0xffL) << (8 * iter);
                    }
                    transaction = descriptor;
                }
                break;
            case 9:
            case 10:
            case 17:
                // Committed, rolled back, or ended by the server.
                transaction = 0;
                break;
            case 20: {
                // Routing: sign in again at the server named here.
                if (body.length >= 8) {
                    int port = (body[4] & 0xff) | ((body[5] & 0xff) << 8);
                    int chars = (body[6] & 0xff) | ((body[7] & 0xff) << 8);
                    if (8 + chars * 2 <= body.length && chars > 0 && port > 0) {
                        routeHost = Wire.fromUtf16le(body, 8, chars * 2);
                        routePort = port;
                    }
                }
                break;
            }
            default:
                break;
        }
    }

    private Column[] readColumns() throws IOException {
        int count = u16();
        if (count == 0xffff) {
            return new Column[0];
        }
        if (count > 4096) {
            throw new IOException("SQL Server announced " + count + " columns");
        }
        Column[] columns = new Column[count];
        for (int iter = 0 ; iter < count ; iter++) {
            Column column = new Column();
            i32();      // user type
            u16();      // flags
            readType(column, true);
            column.name = shortName();
            if (column.name.length() == 0) {
                // SQL Server leaves an expression without an alias unnamed,
                // and two unnamed columns would be one entry in the row map.
                // PostgreSQL and MySQL name every column; this names it by its
                // position.
                column.name = "column" + (iter + 1);
            }
            columns[iter] = column;
        }
        return columns;
    }

    private void readType(Column column, boolean inResultSet) throws IOException {
        int type = u8();
        column.type = type;
        switch (type) {
            case TYPE_NULL:
            case TYPE_INT1:
            case TYPE_BIT:
            case TYPE_INT2:
            case TYPE_INT4:
            case TYPE_INT8:
            case TYPE_FLOAT4:
            case TYPE_FLOAT8:
            case TYPE_MONEY:
            case TYPE_MONEY4:
            case TYPE_DATETIME:
            case TYPE_SMALLDATETIME:
            case TYPE_DATE:
                break;
            case TYPE_GUID:
            case TYPE_INTN:
            case TYPE_BITN:
            case TYPE_FLOATN:
            case TYPE_MONEYN:
            case TYPE_DATETIMEN:
                column.length = u8();
                break;
            case TYPE_DECIMAL:
            case TYPE_NUMERIC:
                column.length = u8();
                u8();   // precision
                column.scale = u8();
                break;
            case TYPE_TIME:
            case TYPE_DATETIME2:
            case TYPE_DATETIMEOFFSET:
                column.scale = u8();
                break;
            case TYPE_VARBINARY:
            case TYPE_BINARY:
                column.length = u16();
                column.chunked = column.length == 0xffff;
                break;
            case TYPE_VARCHAR:
            case TYPE_CHAR:
            case TYPE_NVARCHAR:
            case TYPE_NCHAR: {
                column.length = u16();
                column.chunked = column.length == 0xffff;
                byte[] collation = bytes(5);
                column.utf8 = (collation[3] & 0x04) != 0;
                break;
            }
            case TYPE_TEXT:
            case TYPE_NTEXT:
            case TYPE_IMAGE: {
                i32();
                if (type != TYPE_IMAGE) {
                    byte[] collation = bytes(5);
                    column.utf8 = (collation[3] & 0x04) != 0;
                }
                if (inResultSet) {
                    int parts = u8();
                    for (int iter = 0 ; iter < parts ; iter++) {
                        longName();
                    }
                }
                break;
            }
            case TYPE_XML:
                if (u8() != 0) {
                    shortName();
                    shortName();
                    longName();
                }
                column.chunked = true;
                break;
            case TYPE_UDT:
                u16();
                shortName();
                shortName();
                shortName();
                longName();
                column.chunked = true;
                break;
            case TYPE_VARIANT:
                throw new IOException("A sql_variant column is not read by this client: "
                        + "CAST it to the type it holds");
            default:
                throw new IOException("SQL Server sent a column of a type this client does "
                        + "not read: 0x" + Integer.toHexString(type));
        }
    }

    static String kind(int type) {
        switch (type) {
            case TYPE_DECIMAL:
            case TYPE_NUMERIC:
            case TYPE_MONEY:
            case TYPE_MONEY4:
            case TYPE_MONEYN:
                return "decimal";
            case TYPE_DATE:
                return "date";
            case TYPE_DATETIME:
            case TYPE_SMALLDATETIME:
            case TYPE_DATETIMEN:
            case TYPE_DATETIME2:
                return "datetime";
            case TYPE_DATETIMEOFFSET:
                return "datetimetz";
            case TYPE_TIME:
                return "time";
            default:
                return null;
        }
    }

    private Object readValue(Column column) throws IOException {
        switch (column.type) {
            case TYPE_NULL:
                return null;
            case TYPE_INT1:
            case TYPE_BIT:
                return Long.valueOf(u8());
            case TYPE_INT2:
                return Long.valueOf((short) u16());
            case TYPE_INT4:
                return Long.valueOf(i32());
            case TYPE_INT8:
                return Long.valueOf(i64());
            case TYPE_FLOAT4:
                return Double.valueOf(Float.intBitsToFloat(i32()));
            case TYPE_FLOAT8:
                return Double.valueOf(Double.longBitsToDouble(i64()));
            case TYPE_MONEY:
                return money(bytes(8));
            case TYPE_MONEY4:
                return decimal(i32(), 4);
            case TYPE_DATETIME:
                return dateTime(bytes(8));
            case TYPE_SMALLDATETIME:
                return dateTime(bytes(4));
            case TYPE_VARBINARY:
            case TYPE_BINARY:
            case TYPE_UDT:
                return variable(column);
            case TYPE_VARCHAR:
            case TYPE_CHAR: {
                byte[] data = variable(column);
                return data == null ? null : narrow(data, column.utf8);
            }
            case TYPE_NVARCHAR:
            case TYPE_NCHAR:
            case TYPE_XML: {
                byte[] data = variable(column);
                return data == null ? null : Wire.fromUtf16le(data, 0, data.length);
            }
            case TYPE_TEXT:
            case TYPE_NTEXT:
            case TYPE_IMAGE: {
                int pointer = u8();
                if (pointer == 0) {
                    return null;
                }
                bytes(pointer);
                bytes(8);
                byte[] data = bytes(i32() & 0xffffffffL);
                if (column.type == TYPE_IMAGE) {
                    return data;
                }
                return column.type == TYPE_NTEXT ? Wire.fromUtf16le(data, 0, data.length)
                        : narrow(data, column.utf8);
            }
            default:
                break;
        }
        int length = u8();
        if (length == 0) {
            return null;
        }
        byte[] data = bytes(length);
        switch (column.type) {
            case TYPE_INTN:
                return Long.valueOf(length == 1 ? data[0] & 0xff : little(data, 0, length));
            case TYPE_BITN:
                return Long.valueOf(data[0] != 0 ? 1 : 0);
            case TYPE_FLOATN:
                return length == 4
                        ? Double.valueOf(Float.intBitsToFloat((int) little(data, 0, 4)))
                        : Double.valueOf(Double.longBitsToDouble(little(data, 0, 8)));
            case TYPE_MONEYN:
                return length == 4 ? decimal(little(data, 0, 4), 4) : money(data);
            case TYPE_DATETIMEN:
                return dateTime(data);
            case TYPE_GUID:
                return guid(data);
            case TYPE_DECIMAL:
            case TYPE_NUMERIC:
                return bigDecimal(data, column.scale);
            case TYPE_DATE:
                return date(unsigned(data, 0, 3) - 719162);
            case TYPE_TIME:
                return time(unsigned(data, 0, length), column.scale);
            case TYPE_DATETIME2:
                return date(unsigned(data, length - 3, 3) - 719162) + " "
                        + time(unsigned(data, 0, length - 3), column.scale);
            case TYPE_DATETIMEOFFSET:
                return dateTimeOffset(data, column.scale);
            default:
                throw new IOException("Unreadable SQL Server value of type 0x"
                        + Integer.toHexString(column.type));
        }
    }

    /// A value under a two-byte length, or, for a (MAX) type, in chunks.
    private byte[] variable(Column column) throws IOException {
        if (!column.chunked) {
            int length = u16();
            return length == 0xffff ? null : bytes(length);
        }
        long total = i64();
        if (total == -1L) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(total > 0 && total < 65536
                ? (int) total : 256);
        long limit = SqlLimits.maxMessageBytes();
        while (true) {
            long chunk = i32() & 0xffffffffL;
            if (chunk == 0) {
                break;
            }
            if (out.size() + chunk > limit) {
                throw new IOException("A SQL Server value is over the limit "
                        + "(CN1_DB_MAX_MESSAGE_MB)");
            }
            byte[] part = bytes(chunk);
            out.write(part, 0, part.length);
        }
        return out.toByteArray();
    }

    // ---------------- value formats ----------------

    /// Text of a VARCHAR column: UTF-8 under a UTF-8 collation, and otherwise
    /// read as Latin-1, which is exact for ASCII and for the Western code page's
    /// accented letters. What this runtime writes is NVARCHAR, which never
    /// comes through here.
    private static String narrow(byte[] data, boolean utf8) {
        if (utf8) {
            return Wire.fromUtf8(data);
        }
        StringBuilder out = new StringBuilder(data.length);
        for (byte each : data) {
            out.append((char) (each & 0xff));
        }
        return out.toString();
    }

    /// A signed little-endian integer of 2, 4 or 8 bytes.
    private static long little(byte[] data, int offset, int length) {
        long value = 0;
        for (int iter = 0 ; iter < length ; iter++) {
            value |= (data[offset + iter] & 0xffL) << (8 * iter);
        }
        if (length < 8) {
            int shift = 64 - 8 * length;
            value = (value << shift) >> shift;
        }
        return value;
    }

    private static long unsigned(byte[] data, int offset, int length) {
        long value = 0;
        for (int iter = 0 ; iter < length ; iter++) {
            value |= (data[offset + iter] & 0xffL) << (8 * iter);
        }
        return value;
    }

    /// SCOPE_IDENTITY() is a NUMERIC(38,0): digits, and nothing after them for
    /// a key that fits a long.
    private static long identity(String digits) {
        try {
            int dot = digits.indexOf('.');
            return Long.parseLong(dot < 0 ? digits : digits.substring(0, dot));
        } catch (NumberFormatException err) {
            return 0;
        }
    }

    /// MONEY: the high four bytes first, then the low, in ten-thousandths.
    private static String money(byte[] data) {
        long high = little(data, 0, 4);
        long low = unsigned(data, 4, 4);
        return decimal((high << 32) | low, 4);
    }

    static String decimal(long unscaled, int scale) {
        boolean negative = unscaled < 0;
        String digits = negative ? Long.toString(unscaled).substring(1) : Long.toString(unscaled);
        return place(digits, scale, negative);
    }

    /// Puts the decimal point `scale` digits from the right of `digits`.
    private static String place(String digits, int scale, boolean negative) {
        StringBuilder out = new StringBuilder(digits.length() + 3);
        if (negative) {
            out.append('-');
        }
        if (scale <= 0) {
            return out.append(digits).toString();
        }
        int whole = digits.length() - scale;
        if (whole <= 0) {
            out.append("0.");
            for (int iter = whole ; iter < 0 ; iter++) {
                out.append('0');
            }
            return out.append(digits).toString();
        }
        return out.append(digits, 0, whole).append('.').append(digits, whole, digits.length())
                .toString();
    }

    /// DECIMAL and NUMERIC: a sign byte, one for positive, then the magnitude as
    /// a little-endian integer of up to sixteen bytes.
    static String bigDecimal(byte[] data, int scale) {
        int words = (data.length - 1 + 3) / 4;
        int[] magnitude = new int[words];
        for (int iter = 1 ; iter < data.length ; iter++) {
            magnitude[(iter - 1) / 4] |= (data[iter] & 0xff) << (8 * ((iter - 1) % 4));
        }
        // Peeled off nine decimal digits at a time, least significant first.
        String digits = "";
        boolean zero = false;
        while (!zero) {
            long remainder = 0;
            zero = true;
            for (int iter = words - 1 ; iter >= 0 ; iter--) {
                long current = (remainder << 32) | (magnitude[iter] & 0xffffffffL);
                magnitude[iter] = (int) (current / 1000000000L);
                remainder = current % 1000000000L;
                if (magnitude[iter] != 0) {
                    zero = false;
                }
            }
            String group = Long.toString(remainder);
            if (!zero) {
                StringBuilder padded = new StringBuilder(9);
                for (int iter = group.length() ; iter < 9 ; iter++) {
                    padded.append('0');
                }
                group = padded.append(group).toString();
            }
            digits = group + digits;
        }
        boolean negative = data[0] == 0 && !"0".equals(digits);
        return place(digits, scale, negative);
    }

    /// The civil date of a day counted from 1970-01-01.
    static String date(long epochDay) {
        long z = epochDay + 719468;
        long era = (z >= 0 ? z : z - 146096) / 146097;
        long dayOfEra = z - era * 146097;
        long yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146096) / 365;
        long dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100);
        long shifted = (5 * dayOfYear + 2) / 153;
        long day = dayOfYear - (153 * shifted + 2) / 5 + 1;
        long month = shifted < 10 ? shifted + 3 : shifted - 9;
        long year = yearOfEra + era * 400 + (month <= 2 ? 1 : 0);
        StringBuilder out = new StringBuilder(10);
        pad(out, year, 4).append('-');
        pad(out, month, 2).append('-');
        pad(out, day, 2);
        return out.toString();
    }

    /// A time of day counted in units of ten to the minus `scale` seconds.
    static String time(long value, int scale) {
        long unit = 1;
        for (int iter = 0 ; iter < scale ; iter++) {
            unit *= 10;
        }
        long seconds = value / unit;
        StringBuilder out = new StringBuilder(16);
        pad(out, seconds / 3600, 2).append(':');
        pad(out, seconds / 60 % 60, 2).append(':');
        pad(out, seconds % 60, 2);
        if (scale > 0) {
            out.append('.');
            pad(out, value % unit, scale);
        }
        return out.toString();
    }

    /// DATETIME, days from 1900 and three-hundredths of a second, or, in four
    /// bytes, SMALLDATETIME: days and minutes.
    private static String dateTime(byte[] data) {
        if (data.length == 4) {
            long minutes = unsigned(data, 2, 2);
            return date(unsigned(data, 0, 2) - 25567) + " " + time(minutes * 60, 0);
        }
        long ticks = unsigned(data, 4, 4);
        return date(little(data, 0, 4) - 25567) + " " + time((ticks * 10 + 1) / 3, 3);
    }

    /// DATETIMEOFFSET: the instant in UTC and the offset it was written with,
    /// shown as the local time followed by that offset.
    private static String dateTimeOffset(byte[] data, int scale) {
        int length = data.length;
        long unit = 1;
        for (int iter = 0 ; iter < scale ; iter++) {
            unit *= 10;
        }
        long offset = little(data, length - 2, 2);
        long day = unsigned(data, length - 5, 3) - 719162;
        long ofDay = unsigned(data, 0, length - 5) + offset * 60 * unit;
        long whole = 86400 * unit;
        if (ofDay < 0) {
            ofDay += whole;
            day--;
        } else if (ofDay >= whole) {
            ofDay -= whole;
            day++;
        }
        StringBuilder out = new StringBuilder(36);
        out.append(date(day)).append(' ').append(time(ofDay, scale));
        long absolute = offset < 0 ? -offset : offset;
        out.append(offset < 0 ? '-' : '+');
        pad(out, absolute / 60, 2).append(':');
        pad(out, absolute % 60, 2);
        return out.toString();
    }

    /// A UNIQUEIDENTIFIER the way SQL Server prints it: the first three groups
    /// are stored little-endian, the rest as written.
    private static String guid(byte[] data) {
        int[] order = {3, 2, 1, 0, 5, 4, 7, 6, 8, 9, 10, 11, 12, 13, 14, 15};
        StringBuilder out = new StringBuilder(36);
        for (int iter = 0 ; iter < 16 ; iter++) {
            if (iter == 4 || iter == 6 || iter == 8 || iter == 10) {
                out.append('-');
            }
            int b = data[order[iter]] & 0xff;
            out.append("0123456789ABCDEF".charAt(b >> 4)).append("0123456789ABCDEF".charAt(b & 15));
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

    private static void le16(ByteArrayOutputStream out, int value) {
        out.write(value);
        out.write(value >> 8);
    }

    private static void le32(ByteArrayOutputStream out, int value) {
        out.write(value);
        out.write(value >> 8);
        out.write(value >> 16);
        out.write(value >>> 24);
    }

    private static void le64(ByteArrayOutputStream out, long value) {
        for (int iter = 0 ; iter < 8 ; iter++) {
            out.write((int) (value >>> (8 * iter)));
        }
    }
}
