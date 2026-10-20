---
title: "Build an End-to-End Encrypted Messenger in Java with Parley"
description: "Parley is a full-stack messaging application: end-to-end encrypted chats and groups, voice notes, voice and video calls, linked devices, privacy controls and an admin console, with a Java backend. Free MIT-0 source."
url: "/parley/"
summary: "A complete full-stack end-to-end encrypted messenger you can brand and extend."
ShowToc: true
---

Parley is a complete, full-stack messenger in Java. People chat one to one and in groups and channels, send photos, video and voice notes, and call each other with voice or video. Messages, attachments and calls are end-to-end encrypted: the server relays what it cannot read. Use it to build your own branded messaging app, or to learn how encryption, calls and push fit together in a Codename One application.

**[Generate your project](/initializr/)** · **[Read the user's guide](/developer-guide/parley-users-guide/)** · **[Browse the source](https://github.com/codenameone/CodenameOne/tree/master/scripts/parley)**

In the Initializr, choose the **Parley** messaging template, set your application name and package, and generate the project. The application source is licensed under [MIT-0](https://github.com/codenameone/CodenameOne/blob/master/scripts/parley/LICENSE): you can modify it, use it commercially and keep your changes private without royalties.

## What comes with the application?

| Part | Included workflows |
| --- | --- |
| Conversations | One-to-one chats, groups of up to 256 people and read-only channels, replies, forwarding, editing, delete for everyone, pinning, muting and archiving |
| Media | Photos, video and voice notes, each encrypted on the device before it is uploaded |
| Encryption | X3DH key agreement and a double ratchet over P-256 with HKDF and AES-GCM, a separate session per device, safety numbers to verify a contact |
| Devices | Up to six linked devices per account, including the app in a browser |
| Privacy | Disappearing messages, read receipts and typing indicators you can turn off, last-seen and photo visibility, blocking and reporting, app lock and screen security |
| Calls | Voice and video over WebRTC, the system call screen on iOS and Android, VoIP push for incoming calls, TURN credentials issued per call |
| Bridges | Optional links from a group to Telegram or WhatsApp, off unless configured and clearly marked as not end-to-end encrypted |
| Administration | Statistics, reports, account moderation and the list of bridges, in an admin console |
| Java backend | Accounts, device keys, envelope delivery over WebSocket and push, call signalling, and storage for encrypted attachments |
| Shared code | API contracts and data classes shared between the client and server |

The same client project targets iOS, Android, desktop and the web, and the server runs on SQLite, PostgreSQL, MySQL and MariaDB.

## From first run to customization

1. **[Run the project](https://github.com/codenameone/CodenameOne/tree/master/scripts/parley#running-it).** The development profile needs nothing installed: an in-memory database, demo accounts that share a few conversations and a bot that answers, so one person can try everything.
2. **[Try every feature](/developer-guide/parley-users-guide/).** The user's guide walks through chats, privacy, calls, linked devices and the admin console with the demo accounts.
3. **[Customize and deploy](/developer-guide/parley-implementors-guide/).** The implementor's guide covers the protocol, multi-device delivery, media encryption, calls and TURN, the bridges and their privacy trade-off, branding, text messages, push, the database and hosting.

## What does a live service still need?

Parley supplies the connected application and its source. Running a messaging service also requires your own hosting and a TURN server for calls between mobile networks, a text-message provider for phone verification and push credentials. The protocol code is a sample implementation of published specifications: have it reviewed independently before you rely on it to protect people's conversations.

A conversation linked to Telegram or WhatsApp passes through the server in plain text, which the app tells its members. Decide whether your service wants bridges at all; they stay off until the server has their credentials.

## Looking for something else?

[Savor](/savor/) is the sibling sample for food delivery and [Wayline](/wayline/) for ride hailing, on the same architecture.
