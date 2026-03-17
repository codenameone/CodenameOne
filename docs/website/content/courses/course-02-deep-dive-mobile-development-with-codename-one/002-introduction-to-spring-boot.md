---
title: "Introduction to Spring Boot"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Working with Custom Web Services"
module_key: "02-working-with-custom-web-services"
module_order: 2
lesson_order: 1
weight: 2
is_course_lesson: true
description: "Set up a simple Spring Boot backend for a Codename One application."
---
> Module 2: Working with Custom Web Services

{{< youtube tugJ_7xMdzs >}}

Sooner or later most non-trivial mobile apps need a server. That server might store data, authenticate users, send push notifications, validate business rules, or expose an API that several client applications share. In this course module, Spring Boot is used as the server-side counterpart because it is a straightforward way to build a Java backend without drowning in boilerplate.

Spring Boot works well for this kind of project because it removes a lot of setup friction. Instead of manually assembling an application server, wiring configuration by hand, and spending the first hour fighting infrastructure, you can create a project, add the dependencies you need, and start exposing endpoints quickly. That makes it a good fit for a mobile course where the goal is to understand the client/server relationship, not get lost in container administration.

The video introduces Spring Boot through older IDE screenshots, but the important part is not the IDE. The important part is the shape of the backend: a normal Java project, usually built with Maven, with dependencies for web support, data access, and whatever database driver you need. Today you can create that project with Spring Initializr and open it in any mainstream IDE.

For the persistence layer, the lesson chooses a relational database and uses JPA-style mapping. That is still a sensible default. Mobile backends often need reliable querying, reporting, and clear data relationships long before they need exotic distributed-database tricks. Starting with a conventional SQL-backed model keeps the application understandable and leaves room to grow later if the problem actually demands something more specialized.

That does not mean every Codename One backend must be Spring Boot or even Java. The mobile client can talk to any server that exposes a usable API. But if you already work in Java, Spring Boot remains a practical choice because it keeps the server in the same language ecosystem as the client while still giving you mature tools for HTTP endpoints, data access, and deployment.

The key outcome of this lesson is not "install Spring Boot." It is understanding that a mobile backend can be treated like a normal application project. It has its own build, its own runtime, its own persistence layer, and its own deployment concerns. Once you see it that way, the client and server stop feeling like two mysterious worlds and start feeling like two parts of the same system.

## Further Reading

- [Developer Guide](/developer-guide/)
- [How Do I Access Remote Webservices, Perform Operations On The Server](/how-do-i/how-do-i-access-remote-webservices-perform-operations-on-the-server/)
- [Connecting to a Web Service](/courses/course-02-deep-dive-mobile-development-with-codename-one/003-connecting-to-a-web-service/)
