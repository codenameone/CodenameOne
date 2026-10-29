---
title: "Bastion: A Hardened Bank Template, and Crypto Wallets in Core"
slug: bastion-hardened-bank-template
url: /blog/bastion-hardened-bank-template/
date: '2026-11-02'
author: Shai Almog
description: "Bastion is a virtual bank and small exchange, app and server in one Java project, built as a reference for how to harden a financial app: passkeys, TOTP, signed requests, device keys, pinning, a hash chained audit log and a double entry ledger. Codename One also gained wallet APIs for Ethereum, Bitcoin and Solana."
feed_html: "<img src=\"https://www.codenameone.com/blog/bastion-hardened-bank-template.jpg\" alt=\"The Bastion banking app and its Security Center\" /> Bastion is a virtual bank and exchange built as a reference for hardening a financial app, and Codename One now has wallet APIs for Ethereum, Bitcoin and Solana."
series: ["release-2026-10-30"]
---

![The Bastion banking app and its Security Center](/blog/bastion-hardened-bank-template.jpg)

A large share of the teams building on Codename One are banks and financial institutions. They don't ask me for a prettier list component. They ask how to pass a penetration test, what the auditor will want to see, and which protections actually work on which device.

**Bastion** is my answer, written as code. It's a virtual bank and a small exchange, the app and its server in one Java project. Customers hold dollars, euros, bitcoin and ether, move money, issue and freeze cards, trade, and withdraw coins to addresses they've allowed in advance. Staff verify the ledger and the audit trail. It's a template you can brand, but mostly it's a reference: every hardening technique in it is there so you can read how it's done.

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,150px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><a href="/developer-guide/img/bastion-dashboard.png"><img width="420" height="860" src="/developer-guide/img/bastion-dashboard.png" alt="The Bastion dashboard" loading="lazy" style="width:100%;height:auto" /></a><figcaption>Accounts in four currencies.</figcaption></figure>
<figure style="margin:0"><a href="/developer-guide/img/bastion-exchange.png"><img width="420" height="860" src="/developer-guide/img/bastion-exchange.png" alt="The Bastion exchange screen" loading="lazy" style="width:100%;height:auto" /></a><figcaption>A small exchange.</figcaption></figure>
<figure style="margin:0"><a href="/developer-guide/img/bastion-withdraw.png"><img width="420" height="860" src="/developer-guide/img/bastion-withdraw.png" alt="Withdrawing coins to an allow-listed address" loading="lazy" style="width:100%;height:auto" /></a><figcaption>Withdrawals to allowed addresses.</figcaption></figure>
<figure style="margin:0"><a href="/developer-guide/img/bastion-security-center.png"><img width="420" height="860" src="/developer-guide/img/bastion-security-center.png" alt="The Bastion Security Center" loading="lazy" style="width:100%;height:auto" /></a><figcaption>What is on, on this device.</figcaption></figure>
</div>
{{< /guide-block >}}

## Defense in layers

No single protection makes an app secure. Bastion stacks them, so an attacker who gets past one meets the next.

**Signing in.** OAuth authorization code with PKCE, five minute access tokens and rotating twelve hour refresh tokens. A TOTP second factor with ten recovery codes, plus WebAuthn passkeys. Once an account has a passkey, Bastion requires the strongest factor it has. Lockouts and per address rate limits slow down guessing, and changing the password ends every other session.

**The device.** On first sign in, the app generates a P-256 key pair, registers the public key with the server, and seals the private key with AES-256-GCM under a key kept in the Keychain or Keystore. The app locks after 30 seconds in the background or two minutes idle, and unlocks with biometrics, then a PIN, then a full sign in.

**Every request that moves money is signed.** Not just authenticated with a token. Signed with the device key, over a canonical form that includes a timestamp and a single use nonce:

```java {class="cn1-no-try"}
String text = SignedRequest.canonical(method, target, Hash.sha256(body),
        idempotencyKey, nonce, ts);
byte[] der = Signature.sign(Signature.SHA256_WITH_ECDSA, privateKey, utf8(text));
return SignedRequest.header(deviceId, ts, nonce, Base64.encodeUrlSafe(jose(der)));
```

A stolen access token can't move money without the device key. A captured request can't be replayed, because the nonce is spent.

{{< mermaid >}}
sequenceDiagram
    participant App
    participant Server
    App->>App: canonical form: method, path, body hash, nonce, time
    App->>App: sign with the device key (P-256)
    App->>Server: request + token + Bastion-Signature
    Server->>Server: token valid? device registered?
    Server->>Server: signature valid? nonce unused? clock in range?
    Server->>Server: limits, payee cooling-off, step-up
    Server-->>App: posted to the ledger, appended to the audit chain
{{< /mermaid >}}

**The network.** The app refuses cleartext, talks only to an allow list of hosts, and pins their certificates. That's `AppShield`, a Codename One API, configured in a few lines:

```java {class="cn1-no-try"}
ShieldConfig shield = new ShieldConfig().collectSignals(true);
for (String host : AppConfig.ALLOWED_HOSTS) {
    shield.protect(host, HostPolicy.ENFORCED);
}
AppShield.init(shield);
```

**The screen.** Secure windows that keep sensitive screens out of screenshots and the app switcher, tapjacking protection, and a clipboard that clears itself after you copy an account number.

**Limits.** Per transfer and daily limits, a cooling-off period for new payees, step up authentication above a threshold, and an allow list for crypto withdrawals.

## The Security Center tells the truth

Most security settings screens show a column of green checks. Bastion's Security Center shows what's actually on for this device and this build. Some protections need a cloud build: code obfuscation with string encryption and control flow flattening (`cn1.hardenLevel=aggressive`), Play Integrity, jailbreak detection, and blocking screenshots when the app goes to the background. When those aren't active, the Security Center labels them "Cloud build" rather than showing a green check. A test keeps that screen in sync with the build hints, so it can't drift into a lie.

## The back office auditors want

Money is a double entry ledger, not a balance column. The admin console has the journal, T-accounts, a trial balance, a balance sheet, an income statement, reconciliation, period close and a maker checker approvals queue. Every event lands in a hash chained audit log, and the console can verify the chain and export evidence as CSV. A read only auditor role sees all of it, on a desktop or on a phone.

{{< guide-block >}}
<figure style="margin:24px 0"><a href="/developer-guide/img/bastion-console-overview.png"><img width="1440" height="900" src="/developer-guide/img/bastion-console-overview.png" alt="The Bastion admin console overview on the desktop" loading="lazy" style="width:100%;height:auto" /></a><figcaption>The back office console.</figcaption></figure>
{{< /guide-block >}}

## Crypto wallets, in core

I'm not a fan of crypto currency. I know plenty of people who are, and a few of them are customers. We're here to serve, and if we serve, we should be the best at it.

The new `com.codename1.crypto` packages cover Ethereum and other EVM chains, Bitcoin and Solana: BIP-39 recovery phrases, HD derivation, addresses, transactions, signing, typed data, ERC-20 tokens, PSBT, node clients and price feeds. Addresses match MetaMask, Electrum, Sparrow and Phantom on the standard paths.

```java {class="cn1-no-try"}
String words = Mnemonic.generate(256);           // 24 words
Wallet wallet = Wallet.fromMnemonic(words, "");
EvmAddress eth = wallet.getEvmAddress(0, 0);                 // m/44'/60'/0'/0/0
BitcoinAddress btc = wallet.getBitcoinAddress(BitcoinNetwork.MAINNET,
        Wallet.BIP84, 0, 0, 0);                              // bc1q...
SolanaAddress sol = wallet.getSolanaAddress(0);              // m/44'/501'/0'/0'
String shown = eth.toChecksumString();
```

`VaultSigner` keeps the phrase sealed behind biometrics or a password and refuses to sign while locked. `revealMnemonic` returns a `char[]` you can wipe, not a `String` that lingers on the heap.

The expensive parts (secp256k1, Schnorr, Ed25519 and the hashes) run as native C on iOS, macOS, Android, Windows, and Linux. CI benchmarks every kernel against libsecp256k1, libsodium, OpenSSL and BouncyCastle, and a pull request that makes one slower fails. Even the portable Java engine, used in the simulator, holds its own against BouncyCastle on the JVM:

| Operation | Codename One | BouncyCastle | Ratio |
| --- | --- | --- | --- |
| secp256k1 public key | 30,583/s | 17,391/s | 1.76x |
| ECDSA sign | 24,109/s | 16,220/s | 1.49x |
| ECDSA verify | 14,252/s | 12,748/s | 1.12x |
| Schnorr sign | 15,297/s | 16,051/s | 0.95x |
| Keccak-256, 1 KB | 484,694/s | 503,524/s | 0.96x |

For an exchange or an indexer that checks thousands of signatures at once, `CryptoBatch` hands a whole batch to the engine, which spreads it across every core and, for large batches, the GPU: Metal on Apple, Vulkan elsewhere and WebGPU in the browser. The benchmark charts are in the [crypto wallets chapter](/developer-guide/crypto-wallets/).

Bastion uses this: ether can settle on a real network, while bitcoin stays a ledger balance in the sample.

## What's simulated

Bastion is a reference, not a licensed bank. Deposits, card spending and the market (a random walk) are simulated, and Android integrity checks trust every device in the dev profile. There's no KYC or sanctions screening, no payment rail, no HSM and no regulatory reporting. The [implementor's guide](/developer-guide/bastion-implementors-guide/) has a section for each of those that says what a production deployment needs. On the crypto side, only the three chain families above are supported, and the node clients are blocking calls, so keep them off the UI thread.

## Try it

Generate Bastion from the [Initializr](/initializr/), or run it from the repository:

```bash
CN1_PROFILE=dev backend/server.sh run
mvn verify -Psimulator -DskipTests -Dcodename1.platform=javase
```

Sign in as `customer@bastion.example` with the password `bastion-demo-password`, then open the Security Center and see what your build actually protects.

Tomorrow: {{< post-link path="/blog/reel-streaming-and-courses-template" text="Reel: Your Own Streaming Service or Course Platform" >}}.

{{< giscus >}}
