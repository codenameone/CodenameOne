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
package com.codenameone.examples.dash;

import com.codename1.io.WebSocket;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// A second player for a test that needs company: signs in through the same
/// OAuth steps the app takes, opens its own game socket and says what the
/// test tells it to. The app under test is the other player.
final class Guest {
    private final String server;
    private String access;
    private WebSocket socket;
    private final List<String> heard = new ArrayList<String>();

    private Guest(String server) {
        this.server = server;
    }

    static Guest signIn(String email, String password) throws Exception {
        Guest g = new Guest(System.getProperty("dash.server.url", "http://localhost:18090"));
        g.login(email.trim().toLowerCase(java.util.Locale.ROOT), password);
        return g;
    }

    private void login(String email, String password) throws Exception {
        HttpURLConnection c = open("POST", "/login");
        String session = cookie(send(c, "username=" + enc(email) + "&password=" + enc(password)));
        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        String verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        HttpURLConnection a = open("GET", "/oauth2/authorize?response_type=code&client_id=" + AppConfig.CLIENT_ID
                + "&redirect_uri=" + enc(AppConfig.REDIRECT_URI) + "&scope=" + enc("openid profile")
                + "&state=guest&code_challenge=" + challenge + "&code_challenge_method=S256");
        a.setRequestProperty("Cookie", session);
        a.getResponseCode();
        Matcher code = Pattern.compile("[?&]code=([^&]+)").matcher(String.valueOf(a.getHeaderField("Location")));
        if (!code.find()) {
            throw new IllegalStateException("no authorization code for the guest");
        }
        HttpURLConnection t = open("POST", "/oauth2/token");
        String body = read(send(t, "grant_type=authorization_code&code=" + code.group(1) + "&redirect_uri="
                + enc(AppConfig.REDIRECT_URI) + "&code_verifier=" + verifier + "&client_id=" + AppConfig.CLIENT_ID));
        Matcher token = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
        if (!token.find()) {
            throw new IllegalStateException("no token for the guest");
        }
        access = token.group(1);
    }

    /// Opens the game socket and joins the party `code`.
    void join(final String code) throws Exception {
        HttpURLConnection t = open("POST", "/api/game/ticket");
        t.setRequestProperty("Authorization", "Bearer " + access);
        Matcher m = Pattern.compile("\"ticket\"\\s*:\\s*\"([^\"]+)\"").matcher(read(send(t, "")));
        if (!m.find()) {
            throw new IllegalStateException("no game ticket for the guest");
        }
        socket = WebSocket.build("ws" + server.substring(4) + "/ws/game?ticket=" + m.group(1))
                .onConnect(ws -> ws.send("{\"type\":\"join\",\"code\":\"" + code + "\"}"))
                .onTextMessage((ws, text) -> {
                    synchronized (heard) {
                        heard.add(text);
                    }
                })
                .connect(10000);
    }

    void say(String text) {
        socket.send("{\"type\":\"chat\",\"text\":\"" + text.replace("\"", "'") + "\"}");
    }

    void emote(String id) {
        socket.send("{\"type\":\"emote\",\"id\":\"" + id + "\"}");
    }

    boolean heard(String fragment) {
        synchronized (heard) {
            for (String h : heard) {
                if (h.indexOf(fragment) >= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    void leave() {
        if (socket != null) {
            socket.close();
        }
    }

    private HttpURLConnection open(String method, String path) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(server + path).openConnection();
        c.setRequestMethod(method);
        c.setInstanceFollowRedirects(false);
        if ("POST".equals(method)) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        }
        return c;
    }

    private static HttpURLConnection send(HttpURLConnection c, String form) throws Exception {
        try (OutputStream out = c.getOutputStream()) {
            out.write(form.getBytes(StandardCharsets.UTF_8));
        }
        c.getResponseCode();
        return c;
    }

    private static String cookie(HttpURLConnection c) {
        String all = "";
        for (int i = 0; c.getHeaderFieldKey(i) != null || i == 0; i++) {
            if ("Set-Cookie".equalsIgnoreCase(c.getHeaderFieldKey(i))) {
                String v = c.getHeaderField(i);
                int semi = v.indexOf(';');
                all += (all.length() > 0 ? "; " : "") + (semi < 0 ? v : v.substring(0, semi));
            }
        }
        return all;
    }

    private static String read(HttpURLConnection c) throws Exception {
        InputStream in = c.getResponseCode() >= 400 ? c.getErrorStream() : c.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[4096];
        for (int n; in != null && (n = in.read(b)) > 0;) {
            out.write(b, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String enc(String s) throws Exception {
        return URLEncoder.encode(s, "UTF-8");
    }
}
