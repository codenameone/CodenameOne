/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

import com.codename1.rbx.CFrame;
import com.codename1.rbx.Engine;
import com.codename1.rbx.Input;
import com.codename1.rbx.Instance;
import com.codename1.rbx.ReplicaClient;
import com.codename1.rbx.ReplicaServer;
import com.codename1.rbx.RigidBodies;
import com.codename1.rbx.Vector3;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Bot players for a Brickyard server: each signs in as a real account,
/// joins an experience through the same join ticket and game socket the
/// app uses, and plays as a client of the authoritative server -- it walks
/// to the nearest human, follows them, hops over what is in the way and
/// answers in chat. For filling a room while testing multiplayer, and for
/// a demo that needs people in it.
///
///     java -cp <luau-runtime, rbx-compat, codenameone-core jars> BrickyardBots.java \
///         --server http://127.0.0.1:8080 --experience <id> --password <pw> \
///         --seconds 180 leo@brickyard.example sam@brickyard.example
///
/// `run.sh` beside this file builds that class path from a local Maven
/// repository.
public final class BrickyardBots {
    private static final String CLIENT_ID = "brickyard-app";
    private static final String REDIRECT = "http://127.0.0.1/callback";
    /// The obby template's course, start to finish: the parts a player
    /// lands on, in order.
    static final String[] COURSE = {"Step1", "Step2", "Step3", "Step4", "Step5", "Stage1", "Ramp", "Beam",
        "Checkpoint2", "SpinDeck", "Mover", "Landing", "Spiral1", "Spiral2", "Spiral3", "Spiral4", "Spiral5",
        "Spiral6", "Spiral7", "Spiral8", "Finish"};
    private static final String[] GREETINGS = {"hey!", "hi everyone", "yo"};
    private static final String[] REPLIES = {"nice", "lol", "let's go", "same", "haha"};

    /// The players this process drives, so bots follow and answer people.
    static final java.util.Set<String> BOT_NAMES = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    private final String server;
    private final String password;

    private BrickyardBots(String server, String password) {
        this.server = server;
        this.password = password;
    }

    public static void main(String[] args) throws Exception {
        String server = "http://127.0.0.1:8080";
        String experience = null;
        String password = null;
        double seconds = 120;
        List<String> accounts = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--server" -> server = args[++i];
                case "--experience" -> experience = args[++i];
                case "--password" -> password = args[++i];
                case "--seconds" -> seconds = Double.parseDouble(args[++i]);
                default -> accounts.add(args[i]);
            }
        }
        if (experience == null || password == null || accounts.isEmpty()) {
            System.err.println("usage: BrickyardBots --experience <id> --password <pw> [--server url] "
                    + "[--seconds n] <email>...");
            System.exit(2);
        }
        BrickyardBots b = new BrickyardBots(server, password);
        List<Bot> bots = new ArrayList<>();
        for (String a : accounts) {
            Bot bot = b.join(a, experience, bots.size());
            bots.add(bot);
            System.out.println(a + " joined " + bot.ticketServer);
        }
        long end = System.nanoTime() + (long) (seconds * 1e9);
        long next = System.nanoTime();
        while (System.nanoTime() < end) {
            for (Bot bot : bots) {
                bot.frame();
            }
            next += 16_666_667L;
            long wait = next - System.nanoTime();
            if (wait > 0) {
                Thread.sleep(wait / 1_000_000L, (int) (wait % 1_000_000L));
            }
        }
        for (Bot bot : bots) {
            bot.socket.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
        }
    }

    // ---- signing in, as the app does: OAuth 2 with PKCE ----

    private String signIn(String email) throws Exception {
        HttpResponse<String> login = http.send(HttpRequest.newBuilder(URI.create(server + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form("username", email, "password", password)))
                .build(), HttpResponse.BodyHandlers.ofString());
        String cookie = login.headers().firstValue("set-cookie").orElse("").split(";")[0];
        byte[] raw = new byte[48];
        new SecureRandom().nextBytes(raw);
        String verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        HttpResponse<String> auth = http.send(HttpRequest.newBuilder(URI.create(server + "/oauth2/authorize?"
                + form("response_type", "code", "client_id", CLIENT_ID, "redirect_uri", REDIRECT,
                        "scope", "openid profile", "state", "s", "code_challenge", challenge,
                        "code_challenge_method", "S256")))
                .header("Cookie", cookie).GET().build(), HttpResponse.BodyHandlers.ofString());
        String location = auth.headers().firstValue("location").orElseThrow(
                () -> new IllegalStateException("sign in failed for " + email + ": " + auth.statusCode()));
        Matcher m = Pattern.compile("[?&]code=([^&]+)").matcher(location);
        if (!m.find()) {
            throw new IllegalStateException("no code for " + email);
        }
        HttpResponse<String> token = http.send(HttpRequest.newBuilder(URI.create(server + "/oauth2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form("grant_type", "authorization_code", "code", m.group(1),
                        "redirect_uri", REDIRECT, "code_verifier", verifier, "client_id", CLIENT_ID)))
                .build(), HttpResponse.BodyHandlers.ofString());
        return field(token.body(), "access_token");
    }

    private Bot join(String email, String experience, int index) throws Exception {
        String token = signIn(email);
        HttpResponse<String> r = http.send(HttpRequest.newBuilder(
                URI.create(server + "/api/experiences/" + experience + "/join"))
                .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"serverId\":\"\"}")).build(),
                HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200) {
            throw new IllegalStateException("join failed for " + email + ": " + r.statusCode() + " " + r.body());
        }
        String url = field(r.body(), "url");
        if (url == null || url.isEmpty()) {
            url = "ws" + server.substring(4) + "/ws/play";
        }
        url += (url.indexOf('?') < 0 ? "?" : "&") + "ticket=" + field(r.body(), "ticket");
        Bot bot = new Bot(index, field(r.body(), "serverId"));
        bot.socket = http.newWebSocketBuilder().buildAsync(URI.create(url), bot).join();
        return bot;
    }

    private static String form(String... kv) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < kv.length; i += 2) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(kv[i], StandardCharsets.UTF_8)).append('=')
                    .append(URLEncoder.encode(kv[i + 1], StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private static String field(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    // ---- one bot ----

    /// A client engine kept in step with the server, and the bot's mind.
    /// Socket messages arrive on the socket's threads and are queued; the
    /// engine is only ever touched from the frame loop.
    private static final class Bot implements WebSocket.Listener {
        final Engine engine = new Engine(Engine.CLIENT);
        final ReplicaClient client;
        final ConcurrentLinkedQueue<byte[]> inbox = new ConcurrentLinkedQueue<>();
        final ConcurrentLinkedQueue<String> pendingChat = new ConcurrentLinkedQueue<>();
        final Random random;
        final String ticketServer;
        final int index;
        WebSocket socket;
        java.util.concurrent.CompletableFuture<WebSocket> sending =
                java.util.concurrent.CompletableFuture.completedFuture(null);
        ByteBuffer partial = ByteBuffer.allocate(0);
        int frame;
        int replyAt = -1;
        String reply;
        double lastX;
        double lastZ;
        int stuck;
        int jumpUntil;
        double wanderAngle;
        /// The course stop the bot is running to, or -1 when it is not
        /// racing; a human saying "race" starts it.
        int leg = -1;
        int raceFrom;

        Bot(int index, String ticketServer) {
            this.index = index;
            this.ticketServer = ticketServer;
            this.random = new Random(17 + index);
            engine.setPhysics(new RigidBodies());
            client = new ReplicaClient(engine, new ReplicaServer.Link() {
                @Override
                public void send(byte[] message) {
                    // A socket takes one send at a time: chain them.
                    final ByteBuffer b = ByteBuffer.wrap(message);
                    sending = sending.thenCompose(ws -> socket.sendBinary(b, true));
                }
            });
            engine.addListener(new Engine.ChatListener() {
                @Override
                public void chat(Instance from, String text, String channel) {
                    hear(from, text);
                }
            });
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
            ByteBuffer all = ByteBuffer.allocate(partial.remaining() + data.remaining());
            all.put(partial).put(data).flip();
            if (last) {
                byte[] m = new byte[all.remaining()];
                all.get(m);
                inbox.add(m);
                partial = ByteBuffer.allocate(0);
            } else {
                partial = all;
            }
            ws.request(1);
            return null;
        }

        void frame() {
            byte[] m;
            while ((m = inbox.poll()) != null) {
                client.receive(m);
            }
            if (!client.isJoined()) {
                return;
            }
            frame++;
            Instance me = engine.getLocalPlayer();
            if (me != null) {
                BOT_NAMES.add(me.getName());
            }
            Instance root = rootOf(me);
            if (root != null) {
                double[] at = Behaviors.position(root);
                steer(me, at);
                if (frame % 600 == 0) {
                    System.out.printf("%s at %.1f %.1f %.1f, %d players%n", me.getName(), at[0], at[1], at[2],
                            engine.service("Players").getChildren().size());
                }
            } else if (frame % 600 == 0) {
                System.out.println((me == null ? "?" : me.getName()) + " has no character yet");
            }
            if (frame == 150 + index * 90) {
                say(GREETINGS[index % GREETINGS.length]);
            }
            if (replyAt == frame && reply != null) {
                say(reply);
                reply = null;
            }
            String s;
            while ((s = pendingChat.poll()) != null) {
                System.out.println((me == null ? "?" : me.getName()) + " says: " + s);
                engine.sendChat(s);
            }
            engine.step(1 / 60.0);
            client.tick();
        }

        void say(String text) {
            pendingChat.add(text);
        }

        /// Walks to the nearest human and stays near them, circling a
        /// little as a player does; with nobody around it wanders. When it
        /// pushes and does not move, it jumps.
        void steer(Instance me, double[] at) {
            // Movement is camera relative: a camera looking down -Z makes
            // "forward" world -Z.
            Object cam = engine.getWorkspace().prop("CurrentCamera");
            if (cam instanceof Instance) {
                ((Instance) cam).put("CFrame", new CFrame(at[0], at[1] + 4, at[2] + 10, 1, 0, 0, 0, 1, 0, 0, 0, 1));
            }
            if (leg >= 0 && race(at)) {
                return;
            }
            double[] goal = null;
            double best = Double.MAX_VALUE;
            for (Instance p : engine.service("Players").getChildren()) {
                if (p == me) { // NOPMD CompareObjectsWithEquals - player identity
                    continue;
                }
                Instance r = rootOf(p);
                if (r == null || isBot(p)) {
                    continue;
                }
                double[] q = Behaviors.position(r);
                double d = (q[0] - at[0]) * (q[0] - at[0]) + (q[2] - at[2]) * (q[2] - at[2]);
                if (d < best) {
                    best = d;
                    goal = q;
                }
            }
            double dx;
            double dz;
            if (goal != null) {
                // Keep a few studs off, each bot on its own side.
                double side = (index % 2 == 0 ? 1 : -1) * 4;
                dx = goal[0] + side - at[0];
                dz = goal[2] + 3 - at[2];
            } else {
                if (frame % 180 == 0) {
                    wanderAngle += random.nextDouble() * 2 - 1;
                }
                dx = Math.sin(wanderAngle);
                dz = Math.cos(wanderAngle);
            }
            double len = Math.sqrt(dx * dx + dz * dz);
            // Stop a few studs short of the player: closer, a bot climbs onto them.
            boolean moving = goal == null || len > 6;
            if (moving) {
                Input.move(engine, dx / len, -dz / len);
            } else {
                Input.move(engine, 0, 0);
            }
            double moved = Math.abs(at[0] - lastX) + Math.abs(at[2] - lastZ);
            lastX = at[0];
            lastZ = at[2];
            stuck = moving && moved < 0.02 ? stuck + 1 : 0;
            if (stuck > 60 && goal == null) {
                // A wall the jump did not clear: try another way.
                wanderAngle += Math.PI / 2 + random.nextDouble();
                stuck = 0;
            }
            if (stuck > 12 && frame > jumpUntil + 30) {
                Input.key(engine, "Space", true);
                jumpUntil = frame + 8;
            } else if (frame == jumpUntil) {
                Input.key(engine, "Space", false);
            }
        }

        /// Runs the obby: to the top of each stop in [#COURSE] in turn,
        /// jumping up to the next one and across the gaps between them. A
        /// fall respawns the bot at its checkpoint, and it picks the course
        /// up from the stop nearest to where it landed. Returns false when
        /// the place has no course to run.
        boolean race(double[] at) {
            if (frame < raceFrom) {
                Input.move(engine, 0, 0);
                return true;
            }
            Instance stop = leg < COURSE.length ? find(COURSE[leg]) : null;
            if (stop == null) {
                leg = -1;
                return false;
            }
            double jumpedTo = Math.abs(at[0] - lastX) + Math.abs(at[2] - lastZ);
            if (jumpedTo > 15) {
                // Respawned: carry on from the closest stop.
                double best = Double.MAX_VALUE;
                for (int i = 0; i < COURSE.length; i++) {
                    Instance p = find(COURSE[i]);
                    if (p != null) {
                        double[] q = Behaviors.position(p);
                        double d = Math.abs(q[0] - at[0]) + Math.abs(q[1] - at[1]) + Math.abs(q[2] - at[2]);
                        if (d < best) {
                            best = d;
                            leg = i;
                        }
                    }
                }
                stop = find(COURSE[leg]);
            }
            double[] q = Behaviors.position(stop);
            Object sz = stop.prop("Size");
            double top = q[1] + (sz instanceof Vector3 ? ((Vector3) sz).y / 2 : 0.5);
            double feet = at[1] - 3;
            double dx = q[0] - at[0];
            double dz = q[2] - at[2];
            double len = Math.sqrt(dx * dx + dz * dz);
            lastX = at[0];
            lastZ = at[2];
            if (len < 1.8 && feet > top - 1.5) {
                leg++;
                if (leg >= COURSE.length) {
                    leg = -1;
                    say(index == 0 ? "made it! gg" : "gg, that spinner got me");
                    Input.move(engine, 0, 0);
                }
                return true;
            }
            Input.move(engine, dx / Math.max(len, 1e-6), -dz / Math.max(len, 1e-6));
            boolean up = top > feet + 0.6;
            boolean gap = len > 3.5 && len < 11;
            if ((up || gap) && frame > jumpUntil + 20) {
                Input.key(engine, "Space", true);
                jumpUntil = frame + 8;
            } else if (frame == jumpUntil) {
                Input.key(engine, "Space", false);
            }
            return true;
        }

        Instance find(String name) {
            for (Instance d : engine.getWorkspace().descendants()) {
                if (name.equals(d.getName()) && d.isA("BasePart")) {
                    return d;
                }
            }
            return null;
        }

        /// Bots answer people, not each other, and one at a time.
        void hear(Instance from, String text) {
            if (from == null || from == engine.getLocalPlayer() || isBot(from)) { // NOPMD CompareObjectsWithEquals
                return;
            }
            String t = text.toLowerCase(java.util.Locale.ROOT);
            String name = from.str("DisplayName");
            String first = name == null || name.isEmpty() ? from.getName() : name.split(" ")[0];
            if (t.contains("race") || t.contains("go!")) {
                // Leo is off at once, Sam a beat later: a race, not a parade.
                leg = 0;
                raceFrom = frame + 60 + index * 100;
            }
            if (t.contains("race") || t.contains("follow")) {
                reply = index == 0 ? "you're on! 3.. 2.. 1.." : "right behind you";
            } else if (t.contains("hi") || t.contains("hello") || t.contains("hey")) {
                reply = index == 0 ? "hi " + first + "!" : "hey " + first;
            } else if (t.contains("?")) {
                reply = index == 0 ? "yes!" : "for sure";
            } else {
                reply = REPLIES[random.nextInt(REPLIES.length)];
            }
            replyAt = frame + 70 + index * 60;
        }

        static boolean isBot(Instance p) {
            return BOT_NAMES.contains(p.getName());
        }

        static Instance rootOf(Instance p) {
            Instance ch = p == null ? null : p.ref("Character");
            Instance r = ch == null ? null : ch.findFirstChild("HumanoidRootPart");
            return r != null && r.isA("BasePart") ? r : null;
        }
    }

    /// Positions through the public CFrame property.
    private static final class Behaviors {
        static double[] position(Instance part) {
            Object v = part.prop("CFrame");
            if (v instanceof CFrame) {
                CFrame f = (CFrame) v;
                return new double[] {f.x, f.y, f.z};
            }
            return new double[] {0, 0, 0};
        }
    }
}
