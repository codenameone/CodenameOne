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
package com.codename1.backend.messaging.kafka;

import com.codename1.backend.Config;
import com.codename1.backend.messaging.Message;
import com.codename1.backend.messaging.MessageHandler;
import com.codename1.backend.messaging.MessageTransport;
import com.codename1.backend.messaging.Messaging;
import com.codename1.backend.messaging.PollingTransport;
import com.codename1.backend.messaging.RetryPolicy;
import com.codename1.backend.messaging.Subscription;
import com.codename1.impl.backend.WireBuffer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/// Messaging over Apache Kafka, with no client library.
///
/// ```properties
/// cn1.messaging.url=kafka://broker1:9092,broker2:9092
/// ```
///
/// A destination is a topic, created by the broker on first use when it allows
/// that (`auto.create.topics.enable`, its default). A send is one record,
/// acknowledged by every in-sync replica (`acks=all`); a keyed one goes to the
/// partition Kafka's own partitioner would choose, so records of one key stay
/// in order, and an unkeyed one goes round the partitions.
///
/// A listener's group is a Kafka consumer group: each group sees every record,
/// and within one the partitions are shared out among its members by range,
/// as the Java client's default does. Records of one partition are handled one
/// at a time, in order, and the group's offset moves past a record only after
/// its handler returned; concurrency beyond the number of partitions a member
/// holds therefore waits. Offsets are committed within a second of a
/// handler finishing, and before the member gives its partitions up.
///
/// A record whose handler failed goes to `<topic>.retry.<group>`, which the
/// group also consumes: a partition of it waits until its first record is due,
/// so retries never hold up the topic itself -- what Spring Kafka calls a
/// non-blocking retry. One that fails every attempt goes to `<topic>.dlq`.
///
/// A new group starts at the earliest offset, so nothing sent before a
/// listener first started is skipped; `cn1.messaging.kafka.auto-offset-reset`
/// (or Spring Boot's `spring.kafka.consumer.auto-offset-reset`) set to `latest`
/// starts at the end instead. Records compressed with gzip, snappy, lz4 or
/// zstd are read; what this transport sends is uncompressed.
///
/// `cn1.messaging.kafka.isolation-level` (or Spring Boot's
/// `spring.kafka.consumer.isolation-level`) set to `read_committed` hides the
/// records of aborted transactions and of transactions still open, as the
/// Java client does; the default, `read_uncommitted`, is the Java client's
/// default too. Sending in a transaction is not supported.
///
/// ## Authentication
///
/// SASL is configured as the Java client configures it, under
/// `cn1.messaging.kafka.sasl.*` or Spring Boot's own keys:
///
/// ```properties
/// cn1.messaging.url=kafkas://pkc-xxxxx.eu-west-1.aws.confluent.cloud:9092
/// cn1.messaging.kafka.sasl.mechanism=PLAIN
/// cn1.messaging.kafka.sasl.username=<api key>
/// cn1.messaging.kafka.sasl.password=<api secret>
/// ```
///
/// | Key | Spring Boot equivalent |
/// |---|---|
/// | `cn1.messaging.kafka.sasl.mechanism` | `spring.kafka.properties.sasl.mechanism` |
/// | `cn1.messaging.kafka.sasl.username`, `.password` | the `username` and `password` of `spring.kafka.properties.sasl.jaas.config` |
/// | `cn1.messaging.kafka.security.protocol` | `spring.kafka.security.protocol` |
/// | `cn1.messaging.kafka.sasl.aws-region` | none: the region of an MSK host name, or `AWS_REGION` |
///
/// The mechanisms are `PLAIN`, `SCRAM-SHA-256`, `SCRAM-SHA-512` and
/// `AWS_MSK_IAM`. A security protocol of `SSL` or `SASL_SSL` turns TLS on as
/// `kafkas://` does. Credentials in the URL, `kafka://user:password@host`
/// (percent-encoded), mean PLAIN unless a mechanism is set. Azure Event Hubs
/// takes PLAIN with the user name `$ConnectionString` and the connection
/// string as the password, on port 9093 over TLS; Amazon MSK takes
/// `SCRAM-SHA-512` or `AWS_MSK_IAM`, whose credentials come from the
/// environment, the container or the instance, as for S3.
public final class KafkaTransport extends PollingTransport {
    private static final int SESSION_TIMEOUT = 30000;
    private static final int REBALANCE_TIMEOUT = 60000;
    private static final long HEARTBEAT_EVERY = 3000;
    private static final long COMMIT_EVERY = 1000;
    private static final int FETCH_WAIT = 500;

    private final List<String[]> bootstrap;
    private final boolean tls;
    private final int timeout;
    private final boolean earliest;
    private final KafkaSasl sasl;
    private final boolean readCommitted;
    private final Object metaLock = new Object();
    private final Map<Integer, String[]> brokers = new HashMap<Integer, String[]>();
    private final Map<String, int[]> leaders = new HashMap<String, int[]>();
    private final Map<Integer, KafkaConnection> producers = new HashMap<Integer, KafkaConnection>();
    private KafkaConnection control;
    private final Map<String, Group> groups = new HashMap<String, Group>();
    private final AtomicInteger next = new AtomicInteger();
    private final AtomicBoolean closed = new AtomicBoolean();

    /// Registers this transport with [Messaging]; the generated wiring does it.
    public static final class Factory implements Messaging.TransportFactory {
        @Override
        public MessageTransport open(String url, Config config, RetryPolicy retry)
                throws IOException {
            return new KafkaTransport(url, retry, config);
        }
    }

    /// A transport to the brokers `url` lists (`kafka://host:port,...`, or
    /// `kafkas://` for TLS), without SASL; it connects on first use.
    public KafkaTransport(String url, RetryPolicy retry, int timeoutMillis, boolean earliest)
            throws IOException {
        this(url, retry, timeoutMillis, earliest, null, null, false);
    }

    /// A transport to the brokers `url` lists, configured by `config` as the
    /// class description says: the timeout, the offset reset, the isolation
    /// level, TLS and SASL. It connects on first use.
    public KafkaTransport(String url, RetryPolicy retry, Config config) throws IOException {
        this(url, retry, config == null ? 5000 : config.getInt("cn1.messaging.timeout", 5000),
                !"latest".equalsIgnoreCase(setting(config, "cn1.messaging.kafka.auto-offset-reset",
                        "spring.kafka.consumer.auto-offset-reset")),
                config, setting(config, "cn1.messaging.kafka.security.protocol",
                        "spring.kafka.security.protocol",
                        "spring.kafka.properties.security.protocol"),
                "read_committed".equalsIgnoreCase(setting(config,
                        "cn1.messaging.kafka.isolation-level",
                        "spring.kafka.consumer.isolation-level",
                        "spring.kafka.consumer.properties.isolation.level")));
    }

    private KafkaTransport(String url, RetryPolicy retry, int timeoutMillis, boolean earliest,
                           Config config, String protocol, boolean readCommitted)
            throws IOException {
        super(retry);
        int scheme = url.indexOf("://");
        String s = scheme < 0 ? "" : url.substring(0, scheme);
        if (!"kafka".equalsIgnoreCase(s) && !"kafkas".equalsIgnoreCase(s)) {
            throw new IOException("Not a Kafka URL: " + redact(url));
        }
        String rest = authority(url.substring(scheme + 3));
        String user = null;
        String password = null;
        int at = rest.lastIndexOf('@');
        if (at >= 0) {
            String info = rest.substring(0, at);
            rest = rest.substring(at + 1);
            int colon = info.indexOf(':');
            user = KafkaSasl.percentDecode(colon < 0 ? info : info.substring(0, colon));
            password = colon < 0 ? "" : KafkaSasl.percentDecode(info.substring(colon + 1));
        }
        int slash = rest.indexOf('/');
        if (slash >= 0) {
            rest = rest.substring(0, slash);
        }
        boolean secure = "kafkas".equalsIgnoreCase(s);
        boolean saslProtocol = false;
        if (protocol != null) {
            String p = protocol.trim();
            if ("SSL".equalsIgnoreCase(p) || "SASL_SSL".equalsIgnoreCase(p)) {
                secure = true;
            } else if (!"PLAINTEXT".equalsIgnoreCase(p) && !"SASL_PLAINTEXT".equalsIgnoreCase(p)) {
                throw new IOException("Kafka security protocol " + p + " is not one of "
                        + "PLAINTEXT, SSL, SASL_PLAINTEXT and SASL_SSL");
            }
            saslProtocol = p.length() > 5 && "SASL_".equalsIgnoreCase(p.substring(0, 5));
        }
        this.tls = secure;
        this.sasl = sasl(config, user, password, saslProtocol);
        this.readCommitted = readCommitted;
        bootstrap = new ArrayList<String[]>();
        int from = 0;
        while (from <= rest.length()) {
            int comma = rest.indexOf(',', from);
            int end = comma < 0 ? rest.length() : comma;
            String h = rest.substring(from, end).trim();
            from = end + 1;
            if (h.length() == 0) {
                continue;
            }
            int colon = h.lastIndexOf(':');
            bootstrap.add(colon > 0 ? new String[] {h.substring(0, colon), h.substring(colon + 1)}
                    : new String[] {h, "9092"});
        }
        if (bootstrap.isEmpty()) {
            throw new IOException("The Kafka URL names no broker: " + redact(url));
        }
        this.timeout = timeoutMillis;
        this.earliest = earliest;
    }

    /// The first of `keys` that `config` sets, trimmed, or null.
    private static String setting(Config config, String... keys) throws IOException {
        if (config == null) {
            return null;
        }
        for (String k : keys) {
            String v = config.get(k);
            if (v != null && v.trim().length() > 0) {
                return v.trim();
            }
        }
        return null;
    }

    /// The SASL settings `config` and the URL's credentials make, or null for
    /// none. A JAAS line is read only for its `username` and `password`.
    private static KafkaSasl sasl(Config config, String urlUser, String urlPassword,
                                  boolean saslProtocol) throws IOException {
        String mechanism = setting(config, "cn1.messaging.kafka.sasl.mechanism",
                "spring.kafka.properties.sasl.mechanism");
        String user = config == null ? null : config.get("cn1.messaging.kafka.sasl.username");
        String password = config == null ? null : config.get("cn1.messaging.kafka.sasl.password");
        String jaas = setting(config, "cn1.messaging.kafka.sasl.jaas-config",
                "spring.kafka.properties.sasl.jaas.config");
        if (jaas != null) {
            if (user == null) {
                user = jaasOption(jaas, "username");
            }
            if (password == null) {
                password = jaasOption(jaas, "password");
            }
        }
        if (user == null && urlUser != null) {
            user = urlUser;
            password = urlPassword;
        }
        if (mechanism == null && jaas != null) {
            mechanism = mechanismOf(jaas);
        }
        if (mechanism == null) {
            if (user == null && !saslProtocol) {
                return null;
            }
            mechanism = KafkaSasl.PLAIN;
        }
        return new KafkaSasl(mechanism, user, password,
                setting(config, "cn1.messaging.kafka.sasl.aws-region"));
    }

    /// The value of `name="..."` (or `name=value`) in a JAAS configuration
    /// line, with `\` escapes undone; null when it is not there.
    static String jaasOption(String jaas, String name) {
        // Walked option by option, so a quoted value that happens to contain
        // `username=` is never mistaken for the option itself.
        int p = 0;
        int n = jaas.length();
        while (p < n) {
            p = skipSpaces(jaas, p);
            int start = p;
            while (p < n && jaas.charAt(p) != '=' && jaas.charAt(p) != ' '
                    && jaas.charAt(p) != ';' && jaas.charAt(p) != '"') {
                p++;
            }
            String word = jaas.substring(start, p);
            int eq = skipSpaces(jaas, p);
            if (eq < n && jaas.charAt(eq) == '=') {
                int v = skipSpaces(jaas, eq + 1);
                if (word.equals(name)) {
                    return jaasValue(jaas, v);
                }
                p = skipValue(jaas, v);
            } else if (p < n && jaas.charAt(p) == '"') {
                p = skipValue(jaas, p);
            } else if (p == start) {
                p++;
            }
        }
        return null;
    }

    /// The index just past the option value starting at `from`.
    private static int skipValue(String jaas, int from) {
        int p = from;
        if (p < jaas.length() && jaas.charAt(p) == '"') {
            for (p++; p < jaas.length() && jaas.charAt(p) != '"'; p++) {
                if (jaas.charAt(p) == '\\') {
                    p++;
                }
            }
            return p + 1;
        }
        while (p < jaas.length() && jaas.charAt(p) != ' ' && jaas.charAt(p) != ';') {
            p++;
        }
        return p;
    }

    /// The mechanism the JAAS line's login module implies, as the Java client
    /// would need `sasl.mechanism` to say: a SCRAM module never means PLAIN,
    /// which would send its password in the clear. Null when it implies none.
    static String mechanismOf(String jaas) throws IOException {
        int end = 0;
        while (end < jaas.length() && jaas.charAt(end) != ' ') {
            end++;
        }
        String module = jaas.substring(0, end);
        if (module.endsWith(".PlainLoginModule")) {
            return KafkaSasl.PLAIN;
        }
        if (module.endsWith(".IAMLoginModule")) {
            return KafkaSasl.AWS_MSK_IAM;
        }
        if (module.endsWith(".ScramLoginModule")) {
            throw new IOException("A SCRAM JAAS configuration needs the mechanism set: "
                    + "spring.kafka.properties.sasl.mechanism (or "
                    + "cn1.messaging.kafka.sasl.mechanism) to SCRAM-SHA-256 or SCRAM-SHA-512");
        }
        return null;
    }

    private static int skipSpaces(String s, int from) {
        int p = from;
        while (p < s.length() && s.charAt(p) == ' ') {
            p++;
        }
        return p;
    }

    /// The JAAS option value starting at `from`: quoted with backslash
    /// escapes, or bare up to a space or the closing `;`.
    private static String jaasValue(String jaas, int from) {
        StringBuilder v = new StringBuilder();
        int p = from;
        if (p < jaas.length() && jaas.charAt(p) == '"') {
            for (p++; p < jaas.length() && jaas.charAt(p) != '"'; p++) {
                char c = jaas.charAt(p);
                if (c == '\\' && p + 1 < jaas.length()) {
                    p++;
                    c = jaas.charAt(p);
                }
                v.append(c);
            }
        } else {
            for (; p < jaas.length() && jaas.charAt(p) != ' ' && jaas.charAt(p) != ';'; p++) {
                v.append(jaas.charAt(p));
            }
        }
        return v.toString();
    }

    /// The authority of the URL text after `://`: everything before the
    /// first `/`, `?` or `#`. An `@` in a path is not a user name.
    private static String authority(String rest) {
        int end = rest.length();
        for (int i = 0; i < rest.length(); i++) {
            char c = rest.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                end = i;
                break;
            }
        }
        return rest.substring(0, end);
    }

    /// `url` without the password it may carry.
    static String redact(String url) {
        int scheme = url.indexOf("://");
        if (scheme < 0) {
            return url;
        }
        String auth = authority(url.substring(scheme + 3));
        int at = auth.lastIndexOf('@');
        return at < 0 ? url : url.substring(0, scheme + 3) + "***@"
                + url.substring(scheme + 3 + at + 1);
    }

    @Override
    public String getName() {
        return "kafka";
    }

    // ------------------------------------------------------------- metadata

    private KafkaConnection connect(String host, String port) throws IOException {
        try {
            return KafkaConnection.open(host, Integer.parseInt(port), tls, timeout, sasl);
        } catch (NumberFormatException err) {
            throw new IOException("Not a port: " + port, err);
        }
    }

    /// A connection for metadata, to any broker that answers.
    private KafkaConnection control() throws IOException {
        if (control != null) {
            return control;
        }
        IOException last = null;
        List<String[]> candidates = new ArrayList<String[]>(brokers.values());
        candidates.addAll(bootstrap);
        for (String[] b : candidates) {
            try {
                control = connect(b[0], b[1]);
                return control;
            } catch (IOException err) {
                refused(err);
                last = err;
            }
        }
        throw last == null ? new IOException("No Kafka broker answered") : last;
    }

    /// The leader of each partition of `topic`, asking the cluster when it is
    /// not known or `refresh` says the answer went stale. Waits while the
    /// broker creates the topic.
    int[] leaders(String topic, boolean refresh) throws IOException {
        synchronized (metaLock) {
            int[] known = leaders.get(topic);
            if (known != null && !refresh) {
                return known;
            }
            long until = System.currentTimeMillis() + Math.max(timeout, 10000);
            String problem = null;
            while (System.currentTimeMillis() < until) {
                KafkaConnection.In in;
                try {
                    KafkaConnection.Out req = new KafkaConnection.Out();
                    req.int32(1).string(topic).bool(true);
                    in = control().call(KafkaConnection.METADATA, 4, req);
                } catch (IOException err) {
                    refused(err);
                    dropControl();
                    problem = err.getMessage();
                    pause(200);
                    continue;
                }
                in.int32();
                int nb = in.int32();
                for (int i = 0; i < nb; i++) {
                    int node = in.int32();
                    String host = in.string();
                    int port = in.int32();
                    in.string();
                    brokers.put(Integer.valueOf(node), new String[] {host, Integer.toString(port)});
                }
                in.string();
                in.int32();
                int nt = in.int32();
                int[] found = null;
                for (int t = 0; t < nt; t++) {
                    int error = in.int16();
                    String name = in.string();
                    in.bool();
                    int np = in.int32();
                    int[] parts = new int[np];
                    boolean ready = error == 0 && np > 0;
                    for (int p = 0; p < np; p++) {
                        in.int16();
                        int index = in.int32();
                        int leader = in.int32();
                        skipInts(in);
                        skipInts(in);
                        if (index >= 0 && index < np) {
                            parts[index] = leader;
                        }
                        if (leader < 0) {
                            ready = false;
                        }
                    }
                    if (topic.equals(name)) {
                        if (ready) {
                            found = parts;
                        } else {
                            problem = KafkaConnection.error(error);
                        }
                    }
                }
                if (found != null) {
                    leaders.put(topic, found);
                    return found;
                }
                // A topic being created has no leader for a moment.
                pause(100);
            }
            throw new IOException("Kafka has no partitions for " + topic
                    + (problem == null ? "" : ": " + problem));
        }
    }

    /// Rethrows `err` when the broker refused the credentials, which no
    /// retry or other broker of the cluster will accept either.
    private static void refused(IOException err) throws IOException {
        if (err instanceof KafkaSasl.Refused) {
            throw err;
        }
    }

    private static void skipInts(KafkaConnection.In in) throws IOException {
        int n = in.int32();
        for (int i = 0; i < n; i++) {
            in.int32();
        }
    }

    private void dropControl() {
        if (control != null) {
            control.close();
            control = null;
        }
    }

    String[] broker(int node) throws IOException {
        synchronized (metaLock) {
            String[] b = brokers.get(Integer.valueOf(node));
            if (b == null) {
                throw new IOException("Kafka named an unknown broker " + node);
            }
            return b;
        }
    }

    static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
        }
    }

    // ------------------------------------------------------------- sending

    @Override
    public void send(Message message) throws IOException {
        produce(message.getDestination(), message);
    }

    private void produce(String topic, Message message) throws IOException {
        if (closed.get()) {
            throw new IOException("The kafka transport is closed");
        }
        Map<String, byte[]> headers = new LinkedHashMap<String, byte[]>();
        for (Map.Entry<String, String> h : message.getHeaders().entrySet()) {
            if (h.getValue() != null) {
                headers.put(h.getKey(), WireBuffer.utf8(h.getValue()));
            }
        }
        byte[] key = message.getKey() == null ? null : WireBuffer.utf8(message.getKey());
        byte[] batch = KafkaConnection.batch(key, message.getPayload(), headers,
                System.currentTimeMillis());
        IOException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            int[] parts = leaders(topic, attempt > 0);
            int partition = key != null ? KafkaConnection.partition(key, parts.length)
                    : (next.getAndIncrement() & 0x7fffffff) % parts.length;
            KafkaConnection c;
            try {
                c = producer(parts[partition]);
            } catch (IOException err) {
                refused(err);
                last = err;
                continue;
            }
            KafkaConnection.Out req = new KafkaConnection.Out();
            req.string(null).int16(-1).int32(timeout);
            req.int32(1).string(topic).int32(1).int32(partition).bytes(batch);
            int error;
            try {
                KafkaConnection.In in = c.call(KafkaConnection.PRODUCE, 3, req);
                in.int32();
                in.string();
                in.int32();
                in.int32();
                error = in.int16();
            } catch (IOException err) {
                refused(err);
                dropProducer(parts[partition], c);
                last = err;
                continue;
            }
            if (error == 0) {
                return;
            }
            last = new IOException("Kafka refused a record for " + topic + ": "
                    + KafkaConnection.error(error));
            if (error != 3 && error != 5 && error != 6 && error != 7) {
                break;
            }
        }
        throw last;
    }

    private KafkaConnection producer(int node) throws IOException {
        synchronized (producers) {
            KafkaConnection c = producers.get(Integer.valueOf(node));
            if (c == null) {
                String[] b = broker(node);
                c = connect(b[0], b[1]);
                producers.put(Integer.valueOf(node), c);
            }
            return c;
        }
    }

    private void dropProducer(int node, KafkaConnection c) {
        synchronized (producers) {
            if (producers.get(Integer.valueOf(node)) == c) { // NOPMD CompareObjectsWithEquals - identity
                producers.remove(Integer.valueOf(node));
            }
        }
        c.close();
    }

    @Override
    protected void redeliver(String destination, String group, Delivery delivery, int attempt,
                             long notBefore) throws IOException {
        produce(retryTopic(destination, group),
                delivery.message.withHeader(Message.ATTEMPT, Integer.toString(attempt))
                        .withHeader(Message.NOT_BEFORE, Long.toString(notBefore)));
        ack(destination, group, delivery);
    }

    /// The topic a group's failed records wait in. Kafka allows letters,
    /// digits, `.`, `_` and `-` in a name; anything else in the group becomes
    /// `_`.
    static String retryTopic(String destination, String group) {
        StringBuilder sb = new StringBuilder(destination).append(".retry.");
        for (int i = 0; i < group.length(); i++) {
            char c = group.charAt(i);
            boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == '.' || c == '_' || c == '-';
            sb.append(ok ? c : '_');
        }
        return sb.toString();
    }

    // ----------------------------------------------------------- receiving

    private static String key(String destination, String group) {
        return destination + "\n" + group;
    }

    @Override
    public Subscription subscribe(String destination, String group, int concurrency,
                                  MessageHandler handler) throws IOException {
        final String g = group == null ? "default" : group;
        final String k = key(destination, g);
        final Group state;
        synchronized (groups) {
            Group existing = groups.get(k);
            state = existing == null ? new Group(destination, g) : existing;
            state.users++;
            if (existing == null) {
                groups.put(k, state);
                state.start();
            }
        }
        final Subscription inner;
        try {
            inner = super.subscribe(destination, g, concurrency, handler);
        } catch (IOException err) {
            release(k, state);
            throw err;
        }
        return new Subscription() {
            private final AtomicBoolean done = new AtomicBoolean();

            @Override
            public void close() {
                if (!done.getAndSet(true)) {
                    inner.close();
                    release(k, state);
                }
            }
        };
    }

    private void release(String k, Group state) {
        boolean last;
        synchronized (groups) {
            state.users--;
            last = state.users <= 0;
            if (last) {
                groups.remove(k);
            }
        }
        if (last) {
            state.stop();
        }
    }

    @Override
    protected List<Delivery> poll(String destination, String group, int max, long waitMillis)
            throws IOException {
        Group g;
        synchronized (groups) {
            g = groups.get(key(destination, group));
        }
        if (g == null) {
            throw new IOException("Not subscribed to " + destination);
        }
        return g.take(waitMillis);
    }

    @Override
    protected void ack(String destination, String group, Delivery delivery) {
        Tag tag = (Tag) delivery.handle;
        tag.group.acked(tag);
    }

    /// A delivered record: its partition's state, as it was when delivered.
    private static final class Tag {
        final Group group;
        final Part part;
        final long offset;

        Tag(Group group, Part part, long offset) {
            this.group = group;
            this.part = part;
            this.offset = offset;
        }
    }

    /// One assigned partition.
    private static final class Part {
        final String topic;
        final int partition;
        final boolean retry;
        long fetchOffset;
        long commitOffset = -1;
        long committed = -1;
        boolean inFlight;
        final LinkedList<KafkaConnection.Record> buffer = new LinkedList<KafkaConnection.Record>();

        Part(String topic, int partition, boolean retry) {
            this.topic = topic;
            this.partition = partition;
            this.retry = retry;
        }

        String name() {
            return topic + "-" + partition;
        }
    }

    /// One consumer group membership: the thread that keeps it, fetches and
    /// commits, and the records it fetched for the consumer threads.
    private final class Group implements Runnable {
        final String topic;
        final String group;
        final String retry;
        int users;
        private final AtomicBoolean running = new AtomicBoolean(true);
        private Thread thread;
        private KafkaConnection coordinator;
        private final Map<Integer, KafkaConnection> fetchers = new HashMap<Integer, KafkaConnection>();
        private String memberId = "";
        private int generation = -1;
        private boolean rejoin = true;
        /// Guarded by this group's monitor.
        private Map<String, Part> parts = new LinkedHashMap<String, Part>();
        private int turn;
        /// Set with the monitor held, so a waiter sees why it was woken.
        private boolean stopped;
        private long lastHeartbeat;
        private long lastCommit;

        Group(String topic, String group) {
            this.topic = topic;
            this.group = group;
            this.retry = retryTopic(topic, group);
        }

        void start() {
            thread = new Thread(this, "cn1-kafka-" + topic + "-" + group);
            thread.setDaemon(true);
            thread.start();
        }

        void stop() {
            synchronized (this) {
                running.set(false);
                stopped = true;
                notifyAll();
            }
            if (thread != null) {
                try {
                    thread.join(5000);
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        @Override
        public void run() {
            boolean failing = false;
            while (running.get()) {
                try {
                    if (rejoin) {
                        join();
                    }
                    long now = System.currentTimeMillis();
                    if (now - lastHeartbeat >= HEARTBEAT_EVERY) {
                        heartbeat();
                    }
                    if (now - lastCommit >= COMMIT_EVERY) {
                        commit();
                    }
                    if (!rejoin) {
                        fetch();
                    }
                    if (failing) {
                        System.err.println("cn1: the Kafka consumer of " + topic + " for "
                                + group + " works again");
                        failing = false;
                    }
                } catch (IOException err) {
                    if (!running.get()) {
                        break;
                    }
                    if (!failing) {
                        System.err.println("cn1: the Kafka consumer of " + topic + " for " + group
                                + " failed, retrying: " + err.getMessage());
                        failing = true;
                    }
                    dropConnections();
                    rejoin = true;
                    pause(1000);
                }
            }
            try {
                commit();
                leave();
            } catch (IOException err) {
                // The broker ends the membership when the session times out.
            }
            dropConnections();
        }

        private void dropConnections() {
            if (coordinator != null) {
                coordinator.close();
                coordinator = null;
            }
            for (KafkaConnection c : fetchers.values()) {
                c.close();
            }
            fetchers.clear();
        }

        private KafkaConnection coordinator() throws IOException {
            if (coordinator != null) {
                return coordinator;
            }
            long until = System.currentTimeMillis() + Math.max(timeout, 10000);
            while (true) {
                KafkaConnection.Out req = new KafkaConnection.Out();
                req.string(group).int8(0);
                KafkaConnection.In in;
                synchronized (metaLock) {
                    try {
                        in = control().call(KafkaConnection.FIND_COORDINATOR, 1, req);
                    } catch (IOException err) {
                        dropControl();
                        throw err;
                    }
                }
                in.int32();
                int error = in.int16();
                in.string();
                in.int32();
                String host = in.string();
                int port = in.int32();
                if (error == 0) {
                    coordinator = connect(host, Integer.toString(port));
                    return coordinator;
                }
                if (error != 14 && error != 15 || System.currentTimeMillis() > until) {
                    throw new IOException("Kafka found no coordinator for group " + group + ": "
                            + KafkaConnection.error(error));
                }
                pause(200);
            }
        }

        /// Joins the group, assigns the partitions when this member leads it,
        /// and takes up the partitions it was given.
        private void join() throws IOException {
            commit();
            while (running.get() && rejoin) {
                KafkaConnection.Out subscription = new KafkaConnection.Out();
                subscription.int16(0).int32(2).string(topic).string(retry).bytes((byte[]) null);
                KafkaConnection.Out req = new KafkaConnection.Out();
                req.string(group).int32(SESSION_TIMEOUT).int32(REBALANCE_TIMEOUT)
                        .string(memberId).string("consumer").int32(1).string("range")
                        .bytes(subscription);
                KafkaConnection.In in = callCoordinator(KafkaConnection.JOIN_GROUP, 2, req);
                in.int32();
                int error = in.int16();
                int gen = in.int32();
                in.string();
                String leader = in.string();
                String me = in.string();
                if (error == 25) {
                    memberId = "";
                    continue;
                }
                if (error == 27 || error == 79) {
                    continue;
                }
                if (lostCoordinator(error)) {
                    continue;
                }
                if (error != 0) {
                    throw new IOException("Kafka refused to let this server join group " + group
                            + ": " + KafkaConnection.error(error));
                }
                memberId = me;
                Map<String, List<String>> members = new HashMap<String, List<String>>();
                int n = in.int32();
                for (int i = 0; i < n; i++) {
                    String member = in.string();
                    members.put(member, subscription(in.bytes()));
                }
                KafkaConnection.Out sync = new KafkaConnection.Out();
                sync.string(group).int32(gen).string(memberId);
                if (me.equals(leader)) {
                    assign(sync, members);
                } else {
                    sync.int32(0);
                }
                KafkaConnection.In synced = callCoordinator(KafkaConnection.SYNC_GROUP, 1, sync);
                synced.int32();
                error = synced.int16();
                byte[] assignment = synced.bytes();
                if (error == 27) {
                    continue;
                }
                if (error == 25) {
                    memberId = "";
                    continue;
                }
                if (lostCoordinator(error)) {
                    continue;
                }
                if (error != 0) {
                    throw new IOException("Kafka refused the assignment of group " + group + ": "
                            + KafkaConnection.error(error));
                }
                generation = gen;
                takeUp(assignment);
                rejoin = false;
                lastHeartbeat = System.currentTimeMillis();
            }
        }

        /// True when `error` says the coordinator moved or is still loading:
        /// forgets it, so the next call looks it up again.
        private boolean lostCoordinator(int error) {
            if (error != 14 && error != 15 && error != 16) {
                return false;
            }
            if (coordinator != null) {
                coordinator.close();
                coordinator = null;
            }
            pause(200);
            return true;
        }

        /// The topics a member's subscription metadata names.
        private List<String> subscription(byte[] metadata) throws IOException {
            List<String> topics = new ArrayList<String>();
            if (metadata == null || metadata.length < 6) {
                return topics;
            }
            KafkaConnection.In in = new KafkaConnection.In(metadata);
            in.int16();
            int n = in.int32();
            for (int i = 0; i < n; i++) {
                topics.add(in.string());
            }
            return topics;
        }

        /// Range assignment, as Kafka's own assignor does it: topic by topic,
        /// over the members that subscribe to that topic, in name order. A
        /// group id may be shared by listeners of different topics, in this
        /// server or another.
        private void assign(KafkaConnection.Out sync, Map<String, List<String>> members)
                throws IOException {
            List<String> names = new ArrayList<String>(members.keySet());
            Collections.sort(names);
            List<String> topics = new ArrayList<String>();
            for (String member : names) {
                for (String t : members.get(member)) {
                    if (!topics.contains(t)) {
                        topics.add(t);
                    }
                }
            }
            Map<String, Map<String, List<Integer>>> given =
                    new HashMap<String, Map<String, List<Integer>>>();
            for (String member : names) {
                given.put(member, new LinkedHashMap<String, List<Integer>>());
            }
            for (String t : topics) {
                List<String> takers = new ArrayList<String>();
                for (String member : names) {
                    if (members.get(member).contains(t)) {
                        takers.add(member);
                    }
                }
                int partitions = leaders(t, true).length;
                int m = takers.size();
                int each = partitions / m;
                int extra = partitions % m;
                for (int i = 0; i < m; i++) {
                    int start = i * each + Math.min(i, extra);
                    int length = each + (i < extra ? 1 : 0);
                    List<Integer> list = new ArrayList<Integer>();
                    for (int p = 0; p < length; p++) {
                        list.add(Integer.valueOf(start + p));
                    }
                    given.get(takers.get(i)).put(t, list);
                }
            }
            sync.int32(names.size());
            for (String member : names) {
                KafkaConnection.Out a = new KafkaConnection.Out();
                Map<String, List<Integer>> plan = given.get(member);
                a.int16(0).int32(plan.size());
                for (Map.Entry<String, List<Integer>> e : plan.entrySet()) {
                    a.string(e.getKey()).int32(e.getValue().size());
                    for (Integer p : e.getValue()) {
                        a.int32(p.intValue());
                    }
                }
                a.bytes((byte[]) null);
                sync.string(member).bytes(a);
            }
        }

        /// Keeps the partitions this member still holds as they are, and
        /// starts each new one at its committed offset.
        private void takeUp(byte[] assignment) throws IOException {
            Map<String, Part> taken = new LinkedHashMap<String, Part>();
            if (assignment != null && assignment.length > 0) {
                KafkaConnection.In in = new KafkaConnection.In(assignment);
                in.int16();
                int nt = in.int32();
                for (int t = 0; t < nt; t++) {
                    String name = in.string();
                    int np = in.int32();
                    for (int p = 0; p < np; p++) {
                        int index = in.int32();
                        Part part = new Part(name, index, !topic.equals(name));
                        taken.put(part.name(), part);
                    }
                }
            }
            List<Part> fresh = new ArrayList<Part>();
            synchronized (this) {
                for (Map.Entry<String, Part> e : taken.entrySet()) {
                    Part kept = parts.get(e.getKey());
                    if (kept != null) {
                        e.setValue(kept);
                    } else {
                        fresh.add(e.getValue());
                    }
                }
            }
            offsets(fresh);
            synchronized (this) {
                parts = taken;
                notifyAll();
            }
        }

        /// The committed offset of each of `fresh`, or where the reset policy
        /// starts a partition the group never committed.
        private void offsets(List<Part> fresh) throws IOException {
            if (fresh.isEmpty()) {
                return;
            }
            Map<String, List<Part>> byTopic = byTopic(fresh);
            KafkaConnection.Out req = new KafkaConnection.Out();
            req.string(group).int32(byTopic.size());
            for (Map.Entry<String, List<Part>> e : byTopic.entrySet()) {
                req.string(e.getKey()).int32(e.getValue().size());
                for (Part p : e.getValue()) {
                    req.int32(p.partition);
                }
            }
            KafkaConnection.In in = callCoordinator(KafkaConnection.OFFSET_FETCH, 1, req);
            List<Part> unset = new ArrayList<Part>();
            int nt = in.int32();
            for (int t = 0; t < nt; t++) {
                String name = in.string();
                int np = in.int32();
                for (int i = 0; i < np; i++) {
                    int index = in.int32();
                    long offset = in.int64();
                    in.string();
                    in.int16();
                    for (Part p : fresh) {
                        if (p.topic.equals(name) && p.partition == index) {
                            if (offset >= 0) {
                                p.fetchOffset = offset;
                                p.committed = offset;
                                p.commitOffset = offset;
                            } else {
                                unset.add(p);
                            }
                        }
                    }
                }
            }
            for (Part p : unset) {
                reset(p);
            }
        }

        /// Starts `p` at the earliest or the latest offset.
        private void reset(Part p) throws IOException {
            int leader = leaders(p.topic, false)[p.partition];
            KafkaConnection.Out req = new KafkaConnection.Out();
            req.int32(-1).int32(1).string(p.topic).int32(1).int32(p.partition)
                    .int64(earliest || p.retry ? -2L : -1L);
            KafkaConnection.In in = fetcher(leader).call(KafkaConnection.LIST_OFFSETS, 1, req);
            in.int32();
            in.string();
            in.int32();
            in.int32();
            int error = in.int16();
            in.int64();
            long offset = in.int64();
            if (error != 0) {
                leaders(p.topic, true);
                throw new IOException("Kafka has no offset for " + p.name() + ": "
                        + KafkaConnection.error(error));
            }
            p.fetchOffset = offset;
        }

        private KafkaConnection.In callCoordinator(int api, int version, KafkaConnection.Out req)
                throws IOException {
            try {
                return coordinator().call(api, version, req);
            } catch (IOException err) {
                if (coordinator != null) {
                    coordinator.close();
                    coordinator = null;
                }
                throw err;
            }
        }

        private void heartbeat() throws IOException {
            KafkaConnection.Out req = new KafkaConnection.Out();
            req.string(group).int32(generation).string(memberId);
            KafkaConnection.In in = callCoordinator(KafkaConnection.HEARTBEAT, 1, req);
            in.int32();
            int error = in.int16();
            lastHeartbeat = System.currentTimeMillis();
            if (error == 27 || error == 22 || error == 25) {
                if (error == 25) {
                    memberId = "";
                }
                rejoin = true;
            } else if (error == 16 || error == 15) {
                coordinator.close();
                coordinator = null;
            } else if (error != 0) {
                throw new IOException("Kafka refused the heartbeat of group " + group + ": "
                        + KafkaConnection.error(error));
            }
        }

        /// Commits the offset past every record handled since the last commit.
        private void commit() throws IOException {
            lastCommit = System.currentTimeMillis();
            List<Part> dirty = new ArrayList<Part>();
            List<Long> at = new ArrayList<Long>();
            synchronized (this) {
                for (Part p : parts.values()) {
                    if (p.commitOffset > p.committed) {
                        dirty.add(p);
                        at.add(Long.valueOf(p.commitOffset));
                    }
                }
            }
            if (dirty.isEmpty() || generation < 0) {
                return;
            }
            Map<String, List<Part>> byTopic = byTopic(dirty);
            KafkaConnection.Out req = new KafkaConnection.Out();
            req.string(group).int32(generation).string(memberId).int64(-1)
                    .int32(byTopic.size());
            for (Map.Entry<String, List<Part>> e : byTopic.entrySet()) {
                req.string(e.getKey()).int32(e.getValue().size());
                for (Part p : e.getValue()) {
                    req.int32(p.partition).int64(at.get(dirty.indexOf(p)).longValue())
                            .string(null);
                }
            }
            KafkaConnection.In in = callCoordinator(KafkaConnection.OFFSET_COMMIT, 2, req);
            int nt = in.int32();
            for (int t = 0; t < nt; t++) {
                String name = in.string();
                int np = in.int32();
                for (int i = 0; i < np; i++) {
                    int index = in.int32();
                    int error = in.int16();
                    for (int d = 0; d < dirty.size(); d++) {
                        Part p = dirty.get(d);
                        if (p.topic.equals(name) && p.partition == index) {
                            if (error == 0) {
                                synchronized (this) {
                                    p.committed = Math.max(p.committed, at.get(d).longValue());
                                }
                            } else if (error == 27 || error == 22 || error == 25) {
                                rejoin = true;
                            }
                        }
                    }
                }
            }
        }

        private void leave() throws IOException {
            if (memberId.length() == 0 || coordinator == null) {
                return;
            }
            KafkaConnection.Out req = new KafkaConnection.Out();
            req.string(group).string(memberId);
            callCoordinator(KafkaConnection.LEAVE_GROUP, 1, req);
        }

        private KafkaConnection fetcher(int node) throws IOException {
            KafkaConnection c = fetchers.get(Integer.valueOf(node));
            if (c == null) {
                String[] b = broker(node);
                c = connect(b[0], b[1]);
                fetchers.put(Integer.valueOf(node), c);
            }
            return c;
        }

        /// Fetches for every partition whose records have all been handed out,
        /// one request per leader; waits up to half a second when nothing new
        /// is there.
        private void fetch() throws IOException {
            Map<Integer, List<Part>> byLeader = new LinkedHashMap<Integer, List<Part>>();
            synchronized (this) {
                for (Part p : parts.values()) {
                    if (p.buffer.isEmpty()) {
                        int leader = leaders(p.topic, false)[p.partition];
                        List<Part> list = byLeader.get(Integer.valueOf(leader));
                        if (list == null) {
                            list = new ArrayList<Part>();
                            byLeader.put(Integer.valueOf(leader), list);
                        }
                        list.add(p);
                    }
                }
            }
            if (byLeader.isEmpty()) {
                long until = System.currentTimeMillis() + FETCH_WAIT;
                synchronized (this) {
                    long left = FETCH_WAIT;
                    while (!stopped && !drained() && left > 0) {
                        try {
                            wait(left);
                        } catch (InterruptedException err) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        left = until - System.currentTimeMillis();
                    }
                }
                return;
            }
            int wait = byLeader.size() > 1 ? FETCH_WAIT / byLeader.size() : FETCH_WAIT;
            for (Map.Entry<Integer, List<Part>> e : byLeader.entrySet()) {
                fetchFrom(e.getKey().intValue(), e.getValue(), wait);
            }
        }

        /// True when some partition has handed out every record it fetched.
        /// Called with the monitor held.
        private boolean drained() {
            for (Part p : parts.values()) {
                if (p.buffer.isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        private void fetchFrom(int leader, List<Part> list, int wait) throws IOException {
            Map<String, List<Part>> byTopic = byTopic(list);
            KafkaConnection.Out req = new KafkaConnection.Out();
            req.int32(-1).int32(wait).int32(1).int32(4 * 1024 * 1024).int8(readCommitted ? 1 : 0)
                    .int32(byTopic.size());
            for (Map.Entry<String, List<Part>> e : byTopic.entrySet()) {
                req.string(e.getKey()).int32(e.getValue().size());
                for (Part p : e.getValue()) {
                    req.int32(p.partition).int64(p.fetchOffset).int32(1024 * 1024);
                }
            }
            KafkaConnection.In in;
            try {
                in = fetcher(leader).call(KafkaConnection.FETCH, 4, req);
            } catch (IOException err) {
                KafkaConnection c = fetchers.remove(Integer.valueOf(leader));
                if (c != null) {
                    c.close();
                }
                throw err;
            }
            in.int32();
            int nt = in.int32();
            for (int t = 0; t < nt; t++) {
                String name = in.string();
                int np = in.int32();
                for (int i = 0; i < np; i++) {
                    int index = in.int32();
                    int error = in.int16();
                    in.int64();
                    in.int64();
                    int n = in.int32();
                    long[][] aborted = n < 0 ? new long[0][] : new long[n][];
                    for (int a = 0; a < n; a++) {
                        aborted[a] = new long[] {in.int64(), in.int64()};
                    }
                    byte[] set = in.bytes();
                    Part p = find(list, name, index);
                    if (p == null) {
                        continue;
                    }
                    if (error == 1) {
                        reset(p);
                        continue;
                    }
                    if (error == 3 || error == 5 || error == 6) {
                        leaders(p.topic, true);
                        continue;
                    }
                    if (error != 0) {
                        throw new IOException("Kafka refused a fetch of " + p.name() + ": "
                                + KafkaConnection.error(error));
                    }
                    if (set == null || set.length == 0) {
                        continue;
                    }
                    List<KafkaConnection.Record> records = new ArrayList<KafkaConnection.Record>();
                    long next = KafkaConnection.records(set, p.fetchOffset, records,
                            readCommitted ? aborted : null);
                    synchronized (this) {
                        if (parts.get(p.name()) == p) { // NOPMD CompareObjectsWithEquals - still this member's
                            p.fetchOffset = Math.max(p.fetchOffset, next);
                            if (!records.isEmpty()) {
                                p.buffer.addAll(records);
                                notifyAll();
                            }
                        }
                    }
                }
            }
        }

        private Part find(List<Part> list, String name, int index) {
            for (Part p : list) {
                if (p.topic.equals(name) && p.partition == index) {
                    return p;
                }
            }
            return null;
        }

        /// The next record of a partition with none being handled, or none
        /// after `waitMillis`. A retry partition whose first record is not due
        /// yet is passed over until it is.
        List<Delivery> take(long waitMillis) {
            long until = System.currentTimeMillis() + waitMillis;
            synchronized (this) {
                while (running.get()) {
                    long now = System.currentTimeMillis();
                    long due = until;
                    List<Part> all = new ArrayList<Part>(parts.values());
                    for (int i = 0; i < all.size(); i++) {
                        Part p = all.get((turn + i) % all.size());
                        if (p.inFlight || p.buffer.isEmpty()) {
                            continue;
                        }
                        KafkaConnection.Record r = p.buffer.getFirst();
                        Message m = message(p, r);
                        if (p.retry && notYet(m, now)) {
                            due = Math.min(due, Long.parseLong(m.getHeader(Message.NOT_BEFORE)));
                            continue;
                        }
                        p.buffer.removeFirst();
                        p.inFlight = true;
                        turn = (turn + i + 1) % all.size();
                        if (p.buffer.isEmpty()) {
                            notifyAll();
                        }
                        List<Delivery> out = new ArrayList<Delivery>(1);
                        out.add(new Delivery(m, new Tag(this, p, r.offset)));
                        return out; // NOPMD AvoidBranchingStatementAsLastInLoop - the first ready partition wins
                    }
                    long left = due - now;
                    if (now >= until || left <= 0 && due == until) {
                        break;
                    }
                    try {
                        wait(Math.max(1, left));
                    } catch (InterruptedException err) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                return new ArrayList<Delivery>(0);
            }
        }

        private Message message(Part p, KafkaConnection.Record r) {
            Map<String, String> headers = new LinkedHashMap<String, String>();
            for (Map.Entry<String, byte[]> h : r.headers.entrySet()) {
                headers.put(h.getKey(), h.getValue() == null ? null
                        : WireBuffer.fromUtf8(h.getValue()));
            }
            return identified(new Message(topic, r.key == null ? null : WireBuffer.fromUtf8(r.key),
                    r.value == null ? new byte[0] : r.value, headers), p.name() + "@" + r.offset);
        }

        /// The handler of `tag` returned (or its record was rescheduled): the
        /// offset may move past it, and its partition is free again.
        synchronized void acked(Tag tag) {
            if (parts.get(tag.part.name()) != tag.part) { // NOPMD CompareObjectsWithEquals - still this member's
                // Given up in a rebalance; its next owner handles it again.
                return;
            }
            tag.part.commitOffset = Math.max(tag.part.commitOffset, tag.offset + 1);
            tag.part.inFlight = false;
            notifyAll();
        }
    }

    private static Map<String, List<Part>> byTopic(List<Part> list) {
        Map<String, List<Part>> out = new LinkedHashMap<String, List<Part>>();
        for (Part p : list) {
            List<Part> l = out.get(p.topic);
            if (l == null) {
                l = new ArrayList<Part>();
                out.put(p.topic, l);
            }
            l.add(p);
        }
        return out;
    }

    @Override
    public boolean isHealthy() {
        return !closed.get();
    }

    @Override
    public void close() {
        if (closed.getAndSet(true)) {
            return;
        }
        stopSubscriptions();
        List<Group> all;
        synchronized (groups) {
            all = new ArrayList<Group>(groups.values());
            groups.clear();
        }
        for (Group g : all) {
            g.stop();
        }
        synchronized (producers) {
            for (KafkaConnection c : producers.values()) {
                c.close();
            }
            producers.clear();
        }
        synchronized (metaLock) {
            dropControl();
        }
    }
}
