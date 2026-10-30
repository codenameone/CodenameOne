---
title: "Build a Gym and Fitness Studio App in Java with Pulse"
description: "Pulse is a fitness studio application: class booking with waitlists, family accounts, a rotating QR door pass and NFC fobs, memberships and billing, trainer and front-desk tools, an owner dashboard and a Java backend. Free MIT-0 source."
url: "/pulse/"
summary: "A complete full-stack gym and studio application you can brand and extend."
ShowToc: true
---

Pulse is a complete, full-stack fitness studio application in Java. Members book classes for themselves and their children, open the door with a pass on their phone, pay for their membership and train with friends; trainers run their classes and follow heart rate zones live; the front desk checks people in and takes walk-ins; the owner reads the numbers and fills the quiet classes. Use it to build your own gym, studio or club app with Codename One.

**[Read the user's guide](/developer-guide/pulse-users-guide/)** · **[Browse the source](https://github.com/codenameone/CodenameOne/tree/master/scripts/pulse)**

The application source is licensed under [MIT-0](https://github.com/codenameone/CodenameOne/blob/master/scripts/pulse/LICENSE): you can modify it, use it commercially and keep your changes private without royalties.

## What comes with the application?

| Part | Included workflows |
| --- | --- |
| Member | Book classes or join the waitlist, book for children in the household, show a rotating QR door pass, join, change, freeze and cancel plans, declare away periods, see visits and workouts, add friends, give kudos and invite friends to a class |
| Trainer | See the week's classes and rosters, mark attendance, follow members' heart rate zones live in class, offer and take shifts |
| Front desk and kiosk | Check people in by QR pass, NFC fob or name with a reason for every refusal, record guests, day passes and trials, run a door kiosk on a tablet |
| Owner | Monthly recurring revenue, utilisation, members at risk and under-filled classes; plans, timetable, staff and campaigns |
| Java backend | Accounts and roles, households, bookings that never oversell, door passes, subscription billing with proration, freezes and a dunning ladder, automatic class-filling and we-miss-you nudges with quiet hours, live updates over WebSocket |
| Shared code | API contracts and data classes shared between the client and server |

The account's roles decide which screens a person sees. The same client project targets iOS, Android, desktop and the web, and the server runs on SQLite, PostgreSQL, MySQL and MariaDB.

## From first run to customization

1. **[Run the project](https://github.com/codenameone/CodenameOne/tree/master/scripts/pulse#running-it).** The development profile needs nothing installed: an in-memory database, a demo studio with two locations and a full timetable, demo accounts for every role and simulated cards.
2. **[Try every role](/developer-guide/pulse-users-guide/).** The user's guide walks through the member, trainer, front desk and owner screens with the demo accounts and test cards.
3. **[Customize and deploy](/developer-guide/pulse-implementors-guide/).** The implementor's guide covers booking, the door pass, billing, nudges, providers, the database and what a studio needs before launch.

## What does a live studio still need?

Pulse supplies the connected application and its source. Running a studio on it also requires your own hosting, a payment processor, waiver and consent text for your jurisdiction, door hardware integration and provider configuration. The [implementor's guide](/developer-guide/pulse-implementors-guide/) lists them.

## Other full-stack samples

[Savor](/savor/) is a food-delivery service, [Wayline](/wayline/) a ride-hailing service and [Bastion](/bastion/) a security-first bank, all on the same architecture.
