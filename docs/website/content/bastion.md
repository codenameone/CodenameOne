---
title: "Build a Secure Banking App in Java with Bastion"
description: "Bastion is a security-first banking and crypto-exchange application: accounts, device-signed transfers, cards, an order book, passkeys and a Java backend with a double-entry ledger. Free MIT-0 source."
url: "/bastion/"
summary: "A complete full-stack banking application whose every protection is visible and explained."
images: ["/developer-guide/img/bastion-dashboard.png"]
ShowToc: true
---

Bastion is a complete, full-stack virtual bank in Java. Customers open accounts in several currencies and coins, move money between their own accounts and to other people, order and freeze cards, and trade bitcoin and ether on an order book; an administrator verifies the ledger and the audit log. Every security measure it takes is listed with its real state in the app's Security Center. Use it to build your own banking, wallet or exchange app with Codename One.

**[Generate your project](/initializr/)** · **[Read the user's guide](/developer-guide/bastion-users-guide/)** · **[Browse the source](https://github.com/codenameone/CodenameOne/tree/master/scripts/bastion)**

In the Initializr, choose the **Bastion** banking template, set your application name and package, and generate the project. The application source is licensed under [MIT-0](https://github.com/codenameone/CodenameOne/blob/master/scripts/bastion/LICENSE): you can modify it, use it commercially and keep your changes private without royalties.

## What comes with the application?

| Part | Included workflows |
| --- | --- |
| Customer | Open an account with an authenticator app and a passkey, see balances and history, move money and pay others, manage payees, order and freeze cards, trade on the exchange, send coins to an allow-listed address |
| Security | Sign-in with PKCE and a second factor or a passkey, payments signed by a key on the device, an app lock with biometrics or a PIN, secure screens, certificate pinning and a Security Center that reports each protection honestly |
| Administration | Verify that the ledger balances and that the hash-chained audit log is intact |
| Java backend | An authorization server, a double-entry ledger with idempotent transfers, limits and cooling-off periods, an order-matching engine and the audit chain |
| Shared code | API contracts, data classes and the signed-request format shared between the client and server |

The same client project targets iOS, Android, desktop and the web, and the server runs on SQLite, PostgreSQL, MySQL and MariaDB.

![Bastion dashboard](/developer-guide/img/bastion-dashboard.png)
![Bastion order book](/developer-guide/img/bastion-order-book.png)
![Bastion Security Center](/developer-guide/img/bastion-security-center.png)
![Bastion payment confirmation](/developer-guide/img/bastion-transfer-confirm.png)

## From first run to customization

1. **[Run the project](https://github.com/codenameone/CodenameOne/tree/master/scripts/bastion#running-it).** The development profile needs nothing installed: an in-memory database, demo accounts and a simulated market to trade against.
2. **[Try every screen](/developer-guide/bastion-users-guide/).** The user's guide walks through opening an account, payments, cards, the exchange, the lock, the Security Center and the administrator's checks.
3. **[Customize and deploy](/developer-guide/bastion-implementors-guide/).** The implementor's guide explains the sign-in, signed payments, the ledger and the audit chain, and lists every security feature, setting and endpoint with its place in the code.

## What does a live bank still need?

Bastion supplies the connected application and its source, not a banking license or the systems around it. No money in it is real: deposits are a counter, cards spend nowhere, coins never touch a blockchain and the market is simulated. Device integrity checking is simulated on the server, and code hardening and attestation tokens take effect only in builds from the Codename One build server.

A real service also needs identity verification, payment rails, keys in a hardware security module, custody for coins, regulatory reporting and its own security review. The implementor's guide lists that work.

## Looking for a different starting point?

[Savor](/savor/) is a food-delivery marketplace and [Wayline](/wayline/) a ride-hailing service, both on the same architecture.
