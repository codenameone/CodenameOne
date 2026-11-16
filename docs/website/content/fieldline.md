---
title: "Build a Field Service App in Java with Fieldline"
description: "Fieldline is a field service and inspection application: a scheduling engine that plans every technician's day around skills, hours, time off and travel, a dispatch board with a crew timeline, a Gantt view and a map, job checklists, photos an AI assistant diagnoses, and customer signatures sealed with a hash. Technician and dispatch apps with a Java backend. Free MIT-0 source."
url: "/fieldline/"
summary: "A complete full-stack field service application you can brand and extend."
images: ["/developer-guide/img/fieldline-today.png"]
ShowToc: true
---

Fieldline is a complete, full-stack field service and inspection application in Java. Technicians open their day as a route on a map, drive to each job, work through its checklist, photograph what they find, ask the assistant what is wrong and have the customer sign on the screen. Dispatchers see the whole crew on a timeline, a Gantt chart and a map, drag work between people, and let the optimizer plan the week. When someone is out, the plan is repaired around them, moving as few jobs as it can. The same project serves HVAC, electrical, plumbing, elevator, solar and fire-safety contractors, and inspection teams. Use it to build your own field service app with Codename One.

{{< guide-block >}}
<video controls playsinline preload="metadata" poster="/fieldline/fieldline-backoffice-desktop-poster.jpg" style="width:100%;max-width:960px;height:auto;display:block;margin:0 auto" aria-label="The back office on a desktop: the dispatch board, staffing against capacity, each technician's pay rate, labour rates, company settings and time tracking">
<source src="/fieldline/fieldline-backoffice-desktop.mp4" type="video/mp4">
<a href="/fieldline/fieldline-backoffice-desktop.mp4">Download the video</a>.
</video>

<video controls playsinline preload="metadata" poster="/fieldline/fieldline-technician-tablet-poster.jpg" style="width:100%;max-width:960px;height:auto;display:block;margin:1.5rem auto 0" aria-label="A technician's day on a tablet: the route on a map, driving to the job, the checklist and the stop list following as each job is done">
<source src="/fieldline/fieldline-technician-tablet.mp4" type="video/mp4">
<a href="/fieldline/fieldline-technician-tablet.mp4">Download the video</a>.
</video>

<video controls playsinline preload="metadata" poster="/fieldline/fieldline-technician-mobile-poster.jpg" style="width:100%;max-width:360px;height:auto;display:block;margin:1.5rem auto 0" aria-label="A technician on a phone: the day, a job from setting off to the checklist, the customer's signature, working hours and a time-off request">
<source src="/fieldline/fieldline-technician-mobile.mp4" type="video/mp4">
<a href="/fieldline/fieldline-technician-mobile.mp4">Download the video</a>.
</video>

<video controls playsinline preload="metadata" poster="/fieldline/fieldline-dispatcher-mobile-poster.jpg" style="width:100%;max-width:360px;height:auto;display:block;margin:1.5rem auto 0" aria-label="A dispatcher on a phone: the crew timeline, the map, an optimizer run with what it changed, the Gantt chart, timesheets and company settings">
<source src="/fieldline/fieldline-dispatcher-mobile.mp4" type="video/mp4">
<a href="/fieldline/fieldline-dispatcher-mobile.mp4">Download the video</a>.
</video>
{{< /guide-block >}}

**[Generate your project](/initializr/)** · **[Read the user's guide](/developer-guide/fieldline-users-guide/)** · **[Browse the source](https://github.com/codenameone/CodenameOne/tree/master/scripts/fieldline)**

In the Initializr, choose the **Fieldline** field service template, set your application name and package, and generate the project. The application source is licensed under [MIT-0](https://github.com/codenameone/CodenameOne/blob/master/scripts/fieldline/LICENSE): you can modify it, use it commercially and keep your changes private without royalties.

## What comes with the application?

| Part | Included workflows |
| --- | --- |
| Technician | The day as a numbered route on a map with travel time between stops, start driving, check in on site, a checklist that saves as you go, photos with AI diagnosis cards, notes, a drafted report, a customer signature sealed with a hash, working hours and limits, time-off requests, and jobs added to the phone's calendar |
| Dispatcher | A crew timeline, a Gantt chart against the last plan and a map of the crew, drag to reassign with every constraint checked before it is applied, new jobs placed automatically, an absence covered in one tap, the time-off queue, and every optimizer run with what it changed and why |
| Assistant | Diagnosis of a photo against the equipment and the checklist, answers about the job in hand, a drafted inspection report, and a dispatcher's assistant that re-plans when asked in plain words |
| Scheduling engine | Regret insertion, then adaptive large neighbourhood search over skills, working hours, time windows, time off, maximum drive and weekly hours, travel time and overtime, with a seeded search that gives one plan for one seed, and a repair that keeps work in place when the day changes |
| Java backend | Accounts and roles, technicians and availability, sites and jobs, checklists, photos and signatures, the optimizer runs, the assistant behind the server's own key, push notifications and the live channel |
| Shared code | API contracts and data classes shared between the client and server |

The account's roles decide which screens a person sees. The same client project targets iOS, Android, desktop and the web, and the server runs on SQLite, PostgreSQL, MySQL and MariaDB.

![Fieldline technician day](/developer-guide/img/fieldline-today.png)
![Fieldline route with every stop](/developer-guide/img/fieldline-today-route.png)
![Fieldline job checklist](/developer-guide/img/fieldline-checklist.png)
![Fieldline field assistant answering from a manual](/developer-guide/img/fieldline-assistant.png)
![Fieldline customer signature sealed with a hash](/developer-guide/img/fieldline-sealed.png)
![Fieldline dispatch board](/developer-guide/img/fieldline-board.png)
![Fieldline optimizer run with what it moved](/developer-guide/img/fieldline-optimize.png)
![Fieldline dispatcher's assistant covering an absence](/developer-guide/img/fieldline-dispatch-assistant.png)
![Fieldline staffing against booked work](/developer-guide/img/fieldline-staffing.png)

## From first run to customization

1. **[Run the project](https://github.com/codenameone/CodenameOne/tree/master/scripts/fieldline).** The development profile needs nothing installed: an in-memory database, a demo contractor with a crew of twelve and a week of jobs around Portland, demo accounts for every role, and an assistant that answers from built-in rules.
2. **Try every role.** Sign in as `tech@fieldline.example` to work a day, and as `dispatch@fieldline.example` to run the board. The password is `fieldline-demo`.
3. **Customize and deploy.** The scheduling engine is a module of its own with no dependencies, so its rules and weights change without touching the app or the server. Give the server a model key and the assistant calls a real model; the key stays on the server.

## What does a live business still need?

Fieldline supplies the connected application and its source. Running a business on it also requires your own hosting, a routing service if straight-line travel times are not good enough, a key for the AI model, push credentials, and the checklists and equipment of your own trade.

## Other full-stack samples

[Slate](/slate/) is appointment booking, [Pulse](/pulse/) a fitness studio, [Savor](/savor/) a food-delivery service, [Wayline](/wayline/) a ride-hailing service and [Bastion](/bastion/) a security-first bank, all on the same architecture.
