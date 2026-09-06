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
package com.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.codename1.backend.Crypto;
import com.codename1.backend.Db;
import com.codename1.backend.Jwt;
import com.codename1.backend.Web;

/**
 * The application code: implements the GENERATED server interface, whose
 * signatures come from the shared GreeterApi contract. Persistence is real - rows
 * go into SQLite through bound parameters, never string-concatenated SQL.
 */
public class GreeterService implements GreeterApiServer {
    private static final long TOKEN_TTL_SECONDS = 3600;

    private final Db db;
    private final byte[] signingSecret;

    public GreeterService(Db db) throws Exception {
        this(db, Crypto.randomBytes(32));
    }

    /**
     * - `signingSecret`: at least 32 bytes. A real deployment reads this from its
     *   environment so tokens survive a restart and every instance agrees; the
     *   generated-per-process default is right for a demo and wrong for a fleet.
     */
    public GreeterService(Db db, byte[] signingSecret) throws Exception {
        this.db = db;
        this.signingSecret = signingSecret;
        db.execute("CREATE TABLE IF NOT EXISTS pet ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL,"
                + "species TEXT,"
                + "weight REAL,"
                + "good INTEGER,"
                + "photo BLOB)", null);
        db.execute("CREATE TABLE IF NOT EXISTS account ("
                + "username TEXT PRIMARY KEY,"
                + "password TEXT NOT NULL)", null);
        // A demo account. Stored as a PBKDF2 verifier, never as the password.
        if(db.query("SELECT username FROM account WHERE username = ?",
                new Object[]{"shai"}).isEmpty()) {
            db.execute("INSERT INTO account (username, password) VALUES (?, ?)",
                    new Object[]{"shai", Crypto.hashPassword("hunter2")});
        }
    }

    public String login(Credentials credentials) throws Exception {
        if(credentials == null || credentials.username == null) {
            throw new IllegalArgumentException("username and password are required");
        }
        List rows = db.query("SELECT password FROM account WHERE username = ?",
                new Object[]{credentials.username});
        // The same rejection for an unknown user and a wrong password: telling them
        // apart turns the login endpoint into a list of valid usernames.
        String stored = rows.isEmpty() ? null : str(((Map)rows.get(0)).get("password"));
        if(!Crypto.verifyPassword(credentials.password, stored)) {
            throw new SecurityException("bad credentials");
        }
        Map claims = new java.util.LinkedHashMap();
        claims.put("sub", credentials.username);
        return Jwt.issue(claims, signingSecret, TOKEN_TTL_SECONDS);
    }

    /** The subject of a valid token, or a SecurityException. */
    private String requireCaller(String authorization) throws Exception {
        String token = Jwt.bearer(authorization);
        if(token == null) {
            throw new SecurityException("a bearer token is required");
        }
        try {
            Map claims = Jwt.verify(token, signingSecret);
            return str(claims.get("sub"));
        } catch (Exception err) {
            throw new SecurityException("invalid token");
        }
    }

    public String greet(String name, String loud) throws Exception {
        String greeting = "hello " + name;
        return "yes".equals(loud) ? greeting.toUpperCase() : greeting;
    }

    /**
     * Hands the decoded DTO back, with its weight replaced by the total weight of
     * its tags. Deliberately does not touch the database: what this proves is that
     * the generated codec reads and writes the same shape, nested collections
     * included.
     *
     * The tag loop is the point of the route. It reads a TYPED field off every
     * element, which is what a List whose elements are decoded Maps typed as Tags
     * fails at -- on the JVM with a ClassCastException, and on the native target
     * by reading a Map's header as a Tag's, which is not survivable.
     */
    public Pet echo(Pet pet) throws Exception {
        if(pet == null) {
            throw new IllegalArgumentException("a pet is required");
        }
        if(pet.tags != null) {
            int total = 0;
            for(int iter = 0 ; iter < pet.tags.size() ; iter++) {
                Tag tag = pet.tags.get(iter);
                if(tag != null) {
                    total += tag.weight;
                }
            }
            pet.weight = total;
        }
        return pet;
    }

    public String whoami(String user, String session) throws Exception {
        return "user=" + user + ",session=" + session;
    }

    public Pet addPet(Pet pet) throws Exception {
        if(pet == null || pet.name == null || pet.name.length() == 0) {
            throw new IllegalArgumentException("a pet needs a name");
        }
        db.execute("INSERT INTO pet (name, species, weight, good) VALUES (?, ?, ?, ?)",
                new Object[]{pet.name, pet.species, new Double(pet.weight),
                        Boolean.valueOf(pet.good)});
        pet.id = db.lastInsertId();
        return pet;
    }

    public Pet getPet(long id) throws Exception {
        List rows = db.query("SELECT id, name, species, weight, good FROM pet WHERE id = ?",
                new Object[]{new Long(id)});
        if(rows.isEmpty()) {
            return null;
        }
        return toPet((Map)rows.get(0));
    }

    public List<Pet> listPets(String species) throws Exception {
        List rows;
        if(species == null || species.length() == 0) {
            rows = db.query("SELECT id, name, species, weight, good FROM pet ORDER BY id", null);
        } else {
            rows = db.query("SELECT id, name, species, weight, good FROM pet "
                    + "WHERE species = ? ORDER BY id", new Object[]{species});
        }
        List<Pet> out = new ArrayList<Pet>();
        for(int iter = 0 ; iter < rows.size() ; iter++) {
            out.add(toPet((Map)rows.get(iter)));
        }
        return out;
    }

    public String setPhoto(long id, String data) throws Exception {
        byte[] bytes = data == null ? new byte[0] : data.getBytes("UTF-8");
        int changed = db.execute("UPDATE pet SET photo = ? WHERE id = ?",
                new Object[]{bytes, new Long(id)});
        if(changed == 0) {
            throw new IllegalArgumentException("no pet " + id);
        }
        return "stored " + bytes.length + " bytes";
    }

    public String getPhoto(long id) throws Exception {
        List rows = db.query("SELECT photo FROM pet WHERE id = ?", new Object[]{new Long(id)});
        if(rows.isEmpty()) {
            return null;
        }
        Object photo = ((Map)rows.get(0)).get("photo");
        if(photo == null) {
            return "no photo";
        }
        // The point of the round trip: a BLOB column comes back as byte[], not as a
        // lossy text rendering of the bytes.
        byte[] bytes = (byte[])photo;
        return "bytes=" + bytes.length + " content=" + new String(bytes, "UTF-8");
    }

    public String addPets(String authorization, final List<Pet> pets) throws Exception {
        requireCaller(authorization);
        if(pets == null || pets.isEmpty()) {
            throw new IllegalArgumentException("no pets given");
        }
        Object inserted = db.transaction(new Db.Work() {
            public Object run(Db conn) throws Exception {
                int count = 0;
                for(int iter = 0 ; iter < pets.size() ; iter++) {
                    Pet p = pets.get(iter);
                    if(p == null || p.name == null || p.name.length() == 0) {
                        // Throwing here rolls the whole batch back, including the
                        // rows already inserted in this loop.
                        throw new IllegalArgumentException("pet " + iter + " needs a name");
                    }
                    conn.execute("INSERT INTO pet (name, species, weight, good) VALUES (?, ?, ?, ?)",
                            new Object[]{p.name, p.species, new Double(p.weight),
                                    Boolean.valueOf(p.good)});
                    count++;
                }
                return new Integer(count);
            }
        });
        return "inserted " + inserted;
    }

    public String fetch(String url) throws Exception {
        if(url == null || !url.startsWith("https://")) {
            throw new IllegalArgumentException("only https URLs are fetched");
        }
        Web.Result r = Web.get(url);
        String body = r.getBodyAsString();
        if(body != null && body.length() > 120) {
            body = body.substring(0, 120);
        }
        return "status=" + r.getStatus() + " body=" + body;
    }

    public String deletePet(String authorization, long id) throws Exception {
        requireCaller(authorization);
        int changed = db.execute("DELETE FROM pet WHERE id = ?", new Object[]{new Long(id)});
        return changed > 0 ? "deleted" : "not found";
    }

    private static Pet toPet(Map row) {
        Pet p = new Pet();
        // Db hands every integer column back as Long and every real as Double, so
        // the reads go through Number rather than casting to the field's type.
        p.id = num(row.get("id")).longValue();
        p.name = str(row.get("name"));
        p.species = str(row.get("species"));
        p.weight = num(row.get("weight")).doubleValue();
        p.good = num(row.get("good")).longValue() != 0;
        return p;
    }

    private static Number num(Object v) {
        return v instanceof Number ? (Number)v : new Long(0);
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
