---
title: "Setting up the VPS Server"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Setting Up a Cloud Server"
module_key: "09-setting-up-a-cloud-server"
module_order: 9
lesson_order: 1
weight: 26
is_course_lesson: true
description: "Provision a small Linux server, install Java, and prepare a low-cost deployment target for the backend."
---
> Module 9: Setting Up a Cloud Server

{{< youtube PRk7EhXQRqs >}}

At some point the app has to leave the laptop and run somewhere public. This module takes the deliberately pragmatic route: rent a small VPS, install what the backend needs, and get to a working deployment without pretending that this is a full course on cloud architecture.

That framing is still useful. A small VPS is often the right starting point for a real project because it keeps cost under control and forces you to understand the basic moving parts of your deployment. The lesson is not trying to design a globally distributed platform. It is trying to get a Java backend onto the internet in a way you can operate and afford.

The Spring Boot detail at the start is one of the best parts of the original lesson. Packaging the application so it behaves like an executable service on Linux makes the rest of the deployment story much simpler. Instead of treating the server as a special one-off machine where you run `java -jar` manually forever, you can integrate the app into the operating system like a real service.

The provider shown in the video is just an example. The important decision is the class of machine, not the brand. A modest Linux VPS is enough for many early deployments, and it avoids the trap of overbuilding infrastructure before the product has earned it.

The old lesson uses CentOS and walks through a root-first setup with a dedicated non-root `builder` user for normal work. The exact Linux distribution you choose today may differ, because the hosting and Linux ecosystem has moved on since the video was recorded, but the operational advice is still right. Use root sparingly, create a regular deployment user, and keep day-to-day work under that account so mistakes are less dangerous.

The JDK download discussion in the video is mostly historical now. The specific Oracle download friction it describes is not the interesting part anymore. The enduring lesson is that the server needs a predictable Java runtime and that you should install it in a way that is maintainable for your distribution and deployment process.

This setup step is intentionally unglamorous, but it matters. A backend deployment is not just code. It is an operating system, a runtime, an account model, a filesystem layout, and a plan for how the application will actually be started and updated.

## Further Reading

- [Yum, MariaDB, Security and iptables](/courses/course-03-build-real-world-full-stack-mobile-apps-java/027-yum-mariadb-security-and-iptables/)
- [Starting the Server on Boot](/courses/course-03-build-real-world-full-stack-mobile-apps-java/028-starting-the-server-on-boot/)
- [Introduction to Spring Boot](/courses/course-02-deep-dive-mobile-development-with-codename-one/002-introduction-to-spring-boot/)
- [Corporate Server](/corporate-server/)
