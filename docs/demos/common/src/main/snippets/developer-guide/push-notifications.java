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

// Receivers for stacks this repository does not build against. They are
// snippets rather than compiled demos because compiling them would mean adding
// Spring and Jakarta EE dependencies to the Codename One build for the sake of
// two documentation examples. The compiled receiver is the Codename One
// backend one, under docs/demos/backend.

// tag::push-notifications-java-spring[]
@RestController
public class PushFeedbackController {

    private static final long MAX_AGE_MS = 5 * 60 * 1000L;

    private final DeviceStore store;          // your repository
    private final byte[] secret;

    public PushFeedbackController(DeviceStore store,
            @Value("${cn1.push.callback.secret}") String secret) {
        this.store = store;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    // byte[], not a mapped DTO: the signature covers the bytes as sent, and
    // anything Jackson parses and re-serialises is a different byte string
    // that never verifies.
    @PostMapping(path = "/push/feedback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> feedback(
            @RequestHeader(name = "X-CN1-Signature", required = false) String signature,
            @RequestBody byte[] raw) throws Exception {
        String body = new String(raw, StandardCharsets.UTF_8);
        if (!verified(signature, body, secret)) {
            // Anything but 2xx keeps the window at the sender and resends it.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("bad signature");
        }
        JsonNode digest = new ObjectMapper().readTree(body);
        for (JsonNode event : digest.path("events")) {
            apply(event);
        }
        // 2xx advances the sender's watermark, so it is returned only after the
        // transactional method below has committed.
        return ResponseEntity.ok("ok");
    }

    /**
     * One transaction per event: the deduplication marker and the deletion have
     * to commit together. A marker written first turns a retry into a silent
     * skip; a deletion without one is applied twice.
     */
    @Transactional
    void apply(JsonNode event) {
        String deliveryId = event.path("deliveryId").asText();
        if (store.alreadyApplied(deliveryId)) {
            return;                            // digests are at-least-once
        }
        if ("INVALID_TARGET".equals(event.path("reason").asText())) {
            // token is absent for web push, which reports endpoint instead.
            String key = event.hasNonNull("token")
                    ? event.get("token").asText()
                    : event.path("endpoint").asText();
            store.removeKey(key);
        }
        store.markApplied(deliveryId);
    }

    private static boolean verified(String header, String body, byte[] secret)
            throws Exception {
        if (header == null) {
            return false;
        }
        long timestamp = 0;
        String provided = null;
        for (String part : header.split(",")) {
            int equals = part.indexOf('=');
            if (equals < 1) {
                continue;
            }
            String name = part.substring(0, equals).trim();
            String value = part.substring(equals + 1).trim();
            if ("t".equals(name)) {
                timestamp = Long.parseLong(value);
            } else if ("v1".equals(name)) {
                provided = value;
            }
        }
        if (provided == null
                || Math.abs(System.currentTimeMillis() - timestamp) > MAX_AGE_MS) {
            return false;                      // bound replay by age
        }
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        byte[] expected = mac.doFinal(
                (timestamp + "." + body).getBytes(StandardCharsets.UTF_8));
        // MessageDigest.isEqual is the JDK's constant-time comparison. Never
        // use Arrays.equals or String.equals here: both return at the first
        // differing byte, which is enough timing to forge a signature.
        return MessageDigest.isEqual(expected, HexFormat.of().parseHex(provided));
    }
}
// end::push-notifications-java-spring[]

// tag::push-notifications-java-microprofile[]
@Path("/push")
@ApplicationScoped
public class PushFeedbackResource {

    private static final long MAX_AGE_MS = 5 * 60 * 1000L;

    @Inject
    DeviceStore store;

    // MicroProfile Config, so the secret comes from the same place as every
    // other deployment value rather than from a constant.
    @Inject
    @ConfigProperty(name = "cn1.push.callback.secret")
    String secret;

    // byte[] entity, for the same reason as everywhere else: a provider that
    // binds this to a POJO reads the bytes, and the re-serialised form will not
    // match the signature.
    @POST
    @Path("/feedback")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response feedback(@HeaderParam("X-CN1-Signature") String signature, byte[] raw)
            throws Exception {
        String body = new String(raw, StandardCharsets.UTF_8);
        if (!verified(signature, body, secret.getBytes(StandardCharsets.UTF_8))) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("bad signature").build();
        }
        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            JsonObject digest = reader.readObject();
            for (JsonValue value : digest.getJsonArray("events")) {
                apply(value.asJsonObject());
            }
        }
        return Response.ok("ok").build();
    }

    /** Marker and deletion in one transaction, as in the Spring receiver. */
    @Transactional
    void apply(JsonObject event) {
        String deliveryId = event.getString("deliveryId", null);
        if (store.alreadyApplied(deliveryId)) {
            return;
        }
        if ("INVALID_TARGET".equals(event.getString("reason", null))) {
            String key = event.containsKey("token")
                    ? event.getString("token")
                    : event.getString("endpoint", null);
            store.removeKey(key);
        }
        store.markApplied(deliveryId);
    }

    // Identical to the Spring receiver: the verification is protocol, not
    // framework. HMAC-SHA256 over "<t>.<raw body>", constant-time compare,
    // reject a timestamp outside the age window.
    private static boolean verified(String header, String body, byte[] secret)
            throws Exception {
        // ... as above ...
        return false;
    }
}
// end::push-notifications-java-microprofile[]
