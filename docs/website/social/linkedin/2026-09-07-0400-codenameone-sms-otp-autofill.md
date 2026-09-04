---
title: "A six-digit field should not read every SMS"
slug: 2026-09-07-0400-codenameone-sms-otp-autofill
platform: linkedin
account: codenameone
source_slug: sms-otp-autofill
publish_at: '2026-09-07T04:00:00'
timezone: Asia/Jerusalem
image: /blog/sms-otp-autofill.jpg
---

A six-digit verification field should not need access to every message on a phone.

Codename One now marks an input as a one-time code on iOS, Android, and the web. The operating system can offer the code through its protected autofill path. The application never receives the rest of the inbox.

`PhoneVerification` packages phone entry, OTP entry, resend timing, and asynchronous server callbacks. Application code still owns sending, expiry, attempt limits, rate limits, verification, and session issuance.

The visual OTP boxes now share one underlying editor. The old one-editor-per-box design truncated a six-digit autofill value at the focused box. One editor makes typing, paste, deletion, accessibility, and autofill behave as one input.

No SMS permission, entitlement, native library, or build hint is added. The safer implementation also has the smaller permission surface.

{{canonical}}
