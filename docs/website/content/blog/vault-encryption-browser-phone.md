---
title: "One Vault, from Your Phone to the Browser"
slug: vault-encryption-browser-phone
url: /blog/vault-encryption-browser-phone/
date: '2026-09-20'
author: Shai Almog
description: "Use Codename One's Vault API to encrypt synchronized records, keep operational keys opaque, and choose password, remembered-device, or passkey unlock."
feed_html: '<img src="https://www.codenameone.com/blog/vault-encryption-browser-phone.jpg" alt="One vault across your devices" /> Encrypted records, opaque keys, and passkey unlock through one Java API.'
series: ["release-2026-09-18"]
---

![One vault across your devices](/blog/vault-encryption-browser-phone.jpg)

Encrypting a file is the easy part. Giving the same person access to it on a phone and in a browser, without handing the sync server the key, is where the application starts accumulating security code.

Our new `com.codename1.security.vault` API takes responsibility for that work. It extends the [security work in this week's release](/blog/why-another-java-server/) to synchronized data and gives the JavaScript target a much better set of options than a key stored beside its ciphertext.

## Keep the data key separate from the password

A vault creates a random 32-byte data key. It encrypts records with authenticated encryption, which detects modifications as well as hiding the contents. The stored vault state holds wrapped copies of the data key rather than the key itself.

One wrapper is protected by material derived from the user's password. Other wrappers can support recovery or remembered access. Changing the password can replace its wrapper without re-encrypting every record.

{{< mermaid >}}
flowchart TD
    Password[Vault password] --> KDF[Password key derivation]
    KDF --> Wrap[Wrapped data key]
    Wrap --> Unlock[Unlock on an authorized device]
    Unlock --> Key[Data key]
    Key --> Records[Authenticated encrypted records]
    Wrap --> Sync[(Sync server)]
    Records --> Sync
    Device[Optional device or passkey wrapper] --> Unlock
{{< /mermaid >}}

The sync server can hold the encrypted records and exported vault state. It doesn't need plaintext or the vault password. Use separate credentials for server login: reusing the vault password as a password the server receives would hand it the material this design is meant to keep away.

## Enroll and seal a record

This first-run code uses the vault package, a `char[] password`, and a `byte[] noteBytes`. `upload()` is your application's method for sending ciphertext to its sync service.

```java
VaultOptions options = new VaultOptions()
        .policy(UnlockPolicy.SESSION_ONLY)
        .autoLockAfter(5 * 60 * 1000);

Vault vault = Vault.named("notes").configure(options);
vault.enroll(password, options).ready(ok -> {
    java.util.Arrays.fill(password, '\0');
    vault.seal("note-7", noteBytes).ready(sealed -> {
        upload("note-7", sealed);
        upload("vault", vault.exportSyncState());
    });
}).except(failure -> {
    java.util.Arrays.fill(password, '\0');
    showUnlockError(failure);
});
```

Clear application-owned plaintext buffers when you finish using them. The asynchronous callbacks matter: wait until an operation finishes before clearing the input it needs. `showUnlockError()` is the application's error UI.

For small named secrets, use `putSecret()` and `getSecret()`. The latter returns a `char[]` that the caller can clear after use. Avoid turning it into a `String` merely for convenience; immutable strings can't be cleared in place.

On a second device, retrieve the exported state and sealed note through your authenticated sync transport:

```java
Vault vault = Vault.named("notes");
vault.importSyncState(syncState, password)
        .ready(ok -> vault.open("note-7", sealedNote)
                .ready(this::showNote))
        .except(this::showUnlockError);
```

Use the same record identifier when opening it. The record's binding is part of the authenticated envelope, so moving ciphertext into another record's slot doesn't silently turn it into that record.

## “Remember me” is a security choice

The API has three unlock policies. They describe different ways to regain access, so the UI should let the user make the choice deliberately.

| Policy | How the vault reopens |
| --- | --- |
| `SESSION_ONLY` | Ask for the password again after the session ends. |
| `REMEMBER_DEVICE` | Use a local wrapper that permits unattended reopening. |
| `REQUIRE_USER_VERIFICATION` | Require the supported user-verification mechanism before reopening. |

In a browser, remembered access stores a non-extractable Web Crypto key in IndexedDB. Application code can use that key without an API that exports its bytes. A copy of the whole browser profile can still carry the stored wrapping key, so choose session-only access or user verification when unattended reopening is inappropriate.

The passkey path uses the WebAuthn PRF extension. It derives stable key material from the credential and a salt after verification. A passkey signature alone is not a replacement for key derivation.

```java
UnlockPolicy policy = UnlockPolicy.REQUIRE_USER_VERIFICATION;
if (vault.capabilities().supports(policy)) {
    showVerifiedUnlockOption();
}
```

Check capabilities before offering the control. The implementation checks whether the authenticator can perform the required derivation, instead of presenting a prompt that cannot unlock the data.

The [recorded browser verification](https://github.com/codenameone/CodenameOne/blob/2697dcfa2f0425170efd08655243032571b65869/docs/developer-guide/security.asciidoc) exercised the shipped bridge with Touch ID on macOS in Chrome 152, Safari 26.6, and Firefox 155. All three completed the device-key path and derived stable 32-byte PRF results. Chrome also reproduced the same PRF result after quitting and reopening the browser.

For your own integration, the repository includes `scripts/verify-javascript-vault-passkey.mjs`. Serve browser development from `localhost` or your HTTPS domain so the WebAuthn relying-party name is valid.

## Ask for the protection the application needs

A passkey may sync through the user's account. That's convenient when the goal is opening notes on a new laptop. An application that requires a credential to remain on one device can request that explicitly:

```java
VaultOptions options = new VaultOptions()
        .policy(UnlockPolicy.REQUIRE_USER_VERIFICATION)
        .requireDeviceBoundPasskey();
```

This checks backup eligibility, not just whether the authenticator is attached to the platform. An authenticator that cannot meet the requirement causes `POLICY_NOT_MET`. The application can explain the requirement or offer a separately chosen policy.

Protection reports distinguish yes, no, and unknown. Requirements only accept a confirmed protection. That distinction is useful in code: a browser declining to disclose hardware backing must not accidentally satisfy an application's hardware requirement.

Operational keys are exposed as `KeyHandle` objects with no byte accessor. Locking the vault invalidates those handles. That gives code a way to perform an operation without spreading key arrays across the application.

## Use it with an encrypted database

An unlocked vault can also supply the key for an encrypted database:

```java
Database database = Database.openOrCreate(
        "notes", DatabaseConfig.vault(vault, "notes"));
```

These are `com.codename1.db.Database` and `DatabaseConfig`. SQLCipher requires key bytes, so this path makes them available to the database engine. Close the database as part of your lock or sign-out flow; locking the vault doesn't close a connection that already holds its key. A vault configured with `requireOpaqueKeysOnly()` refuses this operation.

That is a concrete reason to have policy in the API. An application can enforce “handles only” instead of relying on every database call site to remember the rule.

Browser encryption also belongs alongside origin security. Code executing in your application's origin can use an unlocked vault just as your own code can. Keep script sources controlled and maintain the application's content security policy. Encryption at rest and trustworthy application code solve different parts of the problem.

## Less key handling in application code

[PR #5821](https://github.com/codenameone/CodenameOne/pull/5821) brings enrollment, key wrapping, remembered access, passkey derivation, and encrypted records into one maintained API. The practical gain is fewer raw keys and fewer independent platform implementations in your application.

This week's backend can carry those encrypted records without becoming their reader. The invitation API carries exact attribution without collecting a fingerprint. The runtime and builders handle more of the details that are difficult to repeat safely across targets. That's how we intend to keep extending Codename One's security work: give developers direct APIs whose requirements remain enforced when the target changes.

---

## Discussion

_For your users, should a remembered browser reopen a vault immediately, or ask for verification each time?_

{{< giscus >}}
