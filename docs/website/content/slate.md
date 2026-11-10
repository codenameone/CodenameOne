---
title: "Build an Appointment Booking App in Java with Slate"
description: "Slate is an appointment booking application: availability across providers, rooms and the client's own day, gap rules, deposits, a waitlist, and device and online calendars synced both ways, with client, provider and owner apps and a Java backend. Free MIT-0 source."
url: "/slate/"
summary: "A complete full-stack booking application you can brand and extend."
images: ["/developer-guide/img/slate-book-slots.png"]
ShowToc: true
---

Slate is a complete, full-stack appointment booking application in Java. Clients pick a service, a provider and a free time, pay a deposit and add the booking to their calendar; providers keep their hours and time off and connect the calendars they already use; the owner sets the rules, previews what they do to the day and watches the business. The same project serves a salon, a tutor, a personal trainer, a consultancy or a clinic. Use it to build your own booking app with Codename One.

**[Generate your project](/initializr/)** · **[Read the user's guide](/developer-guide/slate-users-guide/)** · **[Browse the source](https://github.com/codenameone/CodenameOne/tree/master/scripts/slate)**

In the Initializr, choose the **Slate** appointment booking template, set your application name and package, and generate the project. The application source is licensed under [MIT-0](https://github.com/codenameone/CodenameOne/blob/master/scripts/slate/LICENSE): you can modify it, use it commercially and keep your changes private without royalties.

## What comes with the application?

| Part | Included workflows |
| --- | --- |
| Client | Choose a service, location and provider or anyone, see free times by the day or the week, mark times that clash with the phone's calendar, hold a time while booking, pay a deposit, add the booking to the calendar, reschedule, cancel, and join a waitlist when a day is full |
| Provider | A day, week and agenda of bookings, working hours and time off, marking appointments complete or no-show, connecting the phone's calendars, Google, Microsoft or a published ICS address, and a calendar feed of their own bookings |
| Owner | A dashboard of bookings, revenue and no-shows, the board of the day across providers, services, providers, locations and resources, and booking rules previewed on a real day before they are saved |
| Java backend | A slot engine that checks every provider, resource and the client at once, buffers, daily caps and breaks, travel time between locations, gap rules, bookings that can never overlap, holds, deposits and no-show fees, a waitlist, reminders and calendar sync |
| Shared code | API contracts and data classes shared between the client and server |

The account's roles decide which screens a person sees. The same client project targets iOS, Android, desktop and the web, and the server runs on SQLite, PostgreSQL, MySQL and MariaDB.

![Slate free times for a treatment](/developer-guide/img/slate-book-slots.png)
![Slate confirming a booking with its deposit](/developer-guide/img/slate-book-confirm.png)
![Slate booked, with add to calendar](/developer-guide/img/slate-book-booked.png)
![Slate provider day](/developer-guide/img/slate-provider-day.png)
![Slate provider week](/developer-guide/img/slate-provider-week.png)
![Slate provider calendars](/developer-guide/img/slate-provider-calendars.png)
![Slate owner dashboard](/developer-guide/img/slate-owner-dashboard.png)
![Slate board of the day](/developer-guide/img/slate-owner-board.png)
![Slate booking rules](/developer-guide/img/slate-owner-preview.png)

## From first run to customization

1. **[Run the project](https://github.com/codenameone/CodenameOne/tree/master/scripts/slate#running-it).** The development profile needs nothing installed: an in-memory database, a demo salon with two locations, demo accounts for every role, simulated cards and simulated online calendars.
2. **[Try every role](/developer-guide/slate-users-guide/).** The user's guide walks through the client, provider and owner screens with the demo accounts.
3. **[Customize and deploy](/developer-guide/slate-implementors-guide/).** The implementor's guide covers the slot engine, holds, the gap rules, calendar sync, deposits and what a business needs before launch.

## What does a live business still need?

Slate supplies the connected application and its source. Running a business on it also requires your own hosting, a payment processor, Google and Microsoft API credentials, and provider configuration. The [implementor's guide](/developer-guide/slate-implementors-guide/) lists them.

## Other full-stack samples

[Pulse](/pulse/) is a fitness studio, [Savor](/savor/) a food-delivery service, [Wayline](/wayline/) a ride-hailing service and [Bastion](/bastion/) a security-first bank, all on the same architecture.
