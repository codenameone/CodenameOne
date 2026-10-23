---
title: "WebRTC Calls, Key Agreement and ORM Checks at Build Time"
slug: private-calls-and-safer-backends
url: /blog/private-calls-and-safer-backends/
date: '2026-10-29'
author: Shai Almog
description: "Codename One adds WebRTC voice and video calls, key agreement and HKDF on every platform, a backend WebClient with mutual TLS and HTTP/2, and ORM mapping mistakes that fail the build instead of production."
feed_html: "<img src=\"https://www.codenameone.com/blog/private-calls-and-safer-backends.jpg\" alt=\"WebRTC calls and compile-time checked backends in Codename One\" /> WebRTC voice and video calls, key agreement and HKDF everywhere, a backend WebClient with mutual TLS, and ORM mistakes that fail the build."
series: ["release-2026-10-23"]
---

![WebRTC calls and compile-time checked backends in Codename One](/blog/private-calls-and-safer-backends.jpg)

The last post this week covers what [Parley](/blog/launch-your-own-private-messenger/), [Savor](/blog/launch-your-own-food-delivery-app/) and [Wayline](/blog/create-an-uber-clone-in-1-minute/) needed underneath: calls, encryption and a backend you can trust with real data. These are features you'd usually assemble from three vendors and a few weekends of glue code.

## Voice and video calls with WebRTC

`com.codename1.webrtc` carries live audio and video directly between two devices, encrypted on the way, with a TURN relay when there's no direct path. On Android it's Google's WebRTC library, on iOS the Apple build of the same library, and in the browser it's the browser's own WebRTC. Referencing the package is what adds the native library to a build, so apps that don't make calls don't pay for it.

```java {class="cn1-no-try"}
WebRtc.openUserMedia(true, true, true).ready(media -> {
    PeerConnectionConfig config = new PeerConnectionConfig()
            .addIceServer(IceServer.stun("stun:stun.example.com:3478"));
    pc = WebRtc.createPeerConnection(config, new PeerConnection.Listener() {
        public void iceCandidate(PeerConnection connection, IceCandidate candidate) {
            signalling.send(candidate);
        }

        public void stateChanged(PeerConnection connection, String state) {
        }

        public void remoteTrack(PeerConnection connection, MediaTrack track) {
            if (track.isVideo()) {
                form.add(BorderLayout.CENTER, new VideoView(track, false));
                form.revalidate();
            }
        }
    });
    pc.addTrack(media.getAudio());
    pc.addTrack(media.getVideo());
    pc.createOffer().ready(offer -> pc.setLocalDescription(offer)
            .ready(done -> signalling.send(offer)));
});
```

Signaling is yours on purpose. The package creates the offer, the answer and the candidates, and you send them over whatever you already have: a WebSocket, your server, a push. So the same call code works with a Codename One backend or with anything else. Parley sends them over its live channel, and the media never touches its server.

{{< mermaid >}}
sequenceDiagram
    participant A as Caller
    participant S as Your server
    participant B as Callee
    A->>S: Offer and candidates
    S->>B: Offer and candidates
    B->>S: Answer and candidates
    S->>A: Answer and candidates
    A-->>B: Encrypted audio and video, peer to peer
{{< /mermaid >}}

## Key agreement and HKDF everywhere

End-to-end encryption needs two parties that have never met to agree on a key over a channel anyone can read. Codename One now does that on every port and on the backend. `KeyGenerator.ec(256)` makes a P-256 key pair, `KeyAgreement.ecdh` combines your private key with the other side's public key, and `Hkdf` turns the shared secret into a proper key:

```java {class="cn1-no-try"}
byte[] shared = KeyAgreement.ecdh(mine.getPrivateKey(), theirs.getPublicKey());

// The shared secret is not a key yet. HKDF turns it into one, and the info
// label keeps keys derived for different purposes apart.
byte[] info = "chat message key v1".getBytes();
byte[] key = Hkdf.derive("SHA-256", null, shared, info, 32);

byte[] nonce = SecureRandom.bytes(12);
byte[] sealed = Cipher.aesEncrypt(Cipher.AES_GCM, new SecretKey("AES", key),
        nonce, null, message);
```

The key agreement runs in the platform's native crypto provider. `Hkdf` is portable Java over `Hmac`, so it gives the same bytes on every device and on your server. These are the building blocks of Parley's X3DH and Double Ratchet, and they're just as useful for encrypted backups, secure pairing or signing data between your app and your server.

## A backend client for serious integrations

Real backends call other services: a payment processor, a bank, a partner API. Many of those authenticate callers with a client certificate. The backend's `WebClient` handles that, along with HTTP/2, timeouts and connection pooling:

```java {class="cn1-no-try"}
this.partner = new WebClient()
        .setClientCertificate(ClientCertificate.pkcs12(archive, password))
        .setRequireHttp2(true)
        .setRequestTimeoutMillis(10000);
```

Server certificates and host names are always verified, and there is no option to turn that off. Two clients never share a connection, so a connection authenticated with one certificate is never used for a request made with another.

## ORM mistakes fail the build

This is my favorite one. With Hibernate and Spring, plenty of mapping mistakes compile fine and only blow up when the application starts, or worse, on the first request that touches the broken entity. You find out in staging if you're lucky.

Codename One's ORM reads your entities when you compile, so those mistakes are build errors that name the class and the field:

```
@Temporal requires a java.util.Date field: signedUp
Persistent fields must be writable: name
@Entity class com.example.Customer must declare a public no-arg constructor
@Version requires an int or long field: revision
```

Repositories get the same treatment: an unsupported method signature, a derived query that names a field the entity doesn't have, or a missing parameter name stops the build and names the repository and the method.

The same release makes it easier to put the ORM on a database you already have. Columns keep their names, native `TIMESTAMP` and `DATE` columns map to `java.util.Date` with `@Temporal`, and lifecycle callbacks can stay private:

```java {class="cn1-no-try"}
@Entity(table = "legacy_customer")
public class LegacyCustomer {
    @Id @Column(name = "customer_no")
    private long id;

    @Column(name = "full_name", nullable = false)
    private String name;

    @Temporal(TemporalType.TIMESTAMP) @Column(name = "signed_up")
    private Date signedUp;

    @PrePersist
    private void defaults() {
        if (signedUp == null) {
            signedUp = new Date();
        }
    }
}
```

Before you deploy, `session.schemaReport()` compares the mappings with the real tables and tells you what's missing, without changing anything in the database.

## That's the week

Three new apps you can launch, a word processor that holds up against Rust, and a stack of features that make the next app easier. Everything is in the [repository](https://github.com/codenameone/CodenameOne), and the guides for [WebRTC](/developer-guide/webrtc/), [security](/developer-guide/security/), [backend web](/developer-guide/backend-web/) and the [ORM](/developer-guide/annotation-sqlite-orm/) have the details. If you build something with these, I want to see it.

{{< giscus >}}
