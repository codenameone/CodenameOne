---
title: "Fill an SMS Verification Code Without Reading the Inbox"
slug: sms-otp-autofill
url: /blog/sms-otp-autofill/
date: '2026-09-07'
author: Shai Almog
description: "Codename One adds phone-number and OTP components plus one-time-code autofill on iOS, Android, and the web without requesting permission to read SMS messages."
feed_html: '<img src="https://www.codenameone.com/blog/sms-otp-autofill.jpg" alt="A phone verification code flowing from an SMS into an OTP field" /> Codename One adds phone-number and OTP components plus one-time-code autofill on iOS, Android, and the web without requesting permission to read SMS messages.'
series: ["release-2026-09-04"]
---

![A phone verification code flowing from an SMS into an OTP field](/blog/sms-otp-autofill.jpg)

A six-digit text field should not need access to every message on a phone.

[PR #5642](https://github.com/codenameone/CodenameOne/pull/5642) adds one-time-code autofill, a country-aware phone-number field, and a complete verification component. iOS, Android, and the browser can offer the code through their protected autofill path. The application never receives the rest of the inbox.

For the rest of this release, including native call management, VPN, AppKit, windows, and contacts, read the [weekly overview](/blog/voip-vpn-builders/).

## The complete verification flow

`PhoneVerification` combines phone entry, code entry, resend timing, and the two server calls:

```java
PhoneVerification verification = new PhoneVerification();

verification.setCodeSender((number, response) ->
        api.requestCode(number, response));

verification.setCodeVerifier((number, code, response) ->
        api.verifyCode(number, code, response));

verification.addVerifiedListener(evt ->
        showAccountHome());

form.add(verification);
```

The callbacks are asynchronous. The component owns the screen state while the application owns the network and account policy. That split is important: Codename One cannot decide how long a code lives or whether a verified phone number should create a session.

{{< mermaid >}}
sequenceDiagram
    participant User
    participant App as Codename One app
    participant Server
    participant OS as OS autofill
    User->>App: Enter phone number
    App->>Server: Request code
    Server-->>User: Send SMS
    OS-->>App: Offer only the detected code
    User->>App: Accept suggestion
    App->>Server: Verify phone and code
    Server-->>App: Verification result or session
{{< /mermaid >}}

The server should rate-limit requests by account, number, device, and network; expire codes; cap attempts; and issue a session only after successful verification. The endpoint must never return the code in its response. Autofill improves entry. It does not provide the security policy.

## Add autofill to an existing form

Applications with their own flow can use the new input constraint directly:

```java
TextField code = new TextField(
        "",
        "Verification code",
        6,
        TextArea.NUMERIC | TextArea.ONE_TIME_CODE);
```

On iOS this maps to the one-time-code content type. Android exposes the appropriate autofill hint. The JavaScript port emits the browser's `one-time-code` autocomplete value. Desktop ignores the constraint.

There is no SMS permission, entitlement, native library, or build hint. An application that uses the field gets metadata on the editor it already has.

## Six boxes, one editor

The visual OTP component still presents separate boxes, but they no longer contain six independent native editors. One editor owns the entire value while the boxes draw its characters.

The old design set a maximum size of one on each box. When the operating system supplied all six digits to the focused editor, the native peer truncated the value after the first digit. Paste had the same basic problem.

{{< mermaid >}}
flowchart LR
    S[SMS suggestion<br/>six digits] --> E[One hidden editor<br/>owns complete value]
    E --> B1[1]
    E --> B2[2]
    E --> B3[3]
    E --> B4[4]
    E --> B5[5]
    E --> B6[6]
{{< /mermaid >}}

This changes one obscure customization path. Code should read and write the `OtpField` value through the field API. Calling `getBox(i).setText()` no longer changes the code because the boxes are presentation, not storage.

That trade makes typing, deletion, selection, paste, accessibility, and autofill behave as one input instead of a relay race between six editors.

## Less access is the better integration

Some older Android OTP examples ask for SMS access and scan the inbox. That makes a small convenience feature responsible for one of the most sensitive data sets on the device. Protected autofill gives the application the value the user intends to enter and nothing else.

The same rule appears throughout this release. The {{< post-link path="/blog/private-contact-picker" text="Contact Picker" >}} returns chosen fields without exposing the address book. Call and VPN packages trigger only the native services they use. The builders turn an API reference into exact platform metadata rather than accumulating permissions in a generic application template.

Secure-by-default development often looks like subtraction: no inbox reader, no copied SMS parser, no permission explanation screen, and no background component holding more data than the feature needs.

Next, read how the {{< post-link path="/blog/platform-deprecation-watch" text="daily platform watch" >}} found framework gaps before users reported them.

---

## Discussion

_Which permission have you kept only because the narrower system API was too difficult to wire up?_

{{< giscus >}}
