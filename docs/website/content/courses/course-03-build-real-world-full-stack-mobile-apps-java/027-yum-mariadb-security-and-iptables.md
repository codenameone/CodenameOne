---
title: "Yum, MariaDB, Security and iptables"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Setting Up a Cloud Server"
module_key: "09-setting-up-a-cloud-server"
module_order: 9
lesson_order: 2
weight: 27
is_course_lesson: true
description: "Install the backend dependencies, harden the database, and expose the application through standard web ports."
---
> Module 9: Setting Up a Cloud Server

{{< youtube 4jIqplr19HA >}}

Once the server exists, the next job is to make it useful. That means installing the packages the backend depends on, getting the database into a safe enough state for internet exposure, and making sure the application can be reached through the usual HTTP and HTTPS ports.

The video uses the package tools and service layout of its Linux distribution, and those exact commands are now more dated than the underlying ideas. What still matters is the sequence: install only what the app genuinely needs, understand why each dependency is there, and enable the pieces that must survive a reboot.

One of the more interesting details in this lesson is the use of a virtual framebuffer. That solves a real deployment problem for Java applications that occasionally need graphical capabilities on a headless server. If part of your build or asset pipeline relies on Java2D or other GUI-linked code, a headless Linux box can fail in surprising ways unless you provide an off-screen display environment.

The database step is less subtle but more important. Installing MariaDB or another compatible database engine is only the beginning. The real work is hardening it: removing unsafe defaults, setting credentials intentionally, and deciding whether remote database access should exist at all. In many small deployments, the safest default is to keep the database private to the server and administer it through SSH rather than by opening it broadly to the internet.

The lesson also uses port redirection so the backend can listen on higher application ports while the machine still exposes standard web ports such as 80 and 443. The exact firewall or proxy mechanism may differ in a modern deployment, but the reason for it is unchanged. Users and clients expect web services on standard ports, while the application process itself often runs more safely on a non-privileged internal port.

That combination of service hardening and port mapping is the difference between “the backend runs on the machine” and “the backend is actually deployable.” Getting it right early keeps the rest of the deployment process much simpler.

## Further Reading

- [Setting up the VPS Server](/courses/course-03-build-real-world-full-stack-mobile-apps-java/026-setting-up-the-vps-server/)
- [Starting the Server on Boot](/courses/course-03-build-real-world-full-stack-mobile-apps-java/028-starting-the-server-on-boot/)
- [Introduction to Spring Boot](/courses/course-02-deep-dive-mobile-development-with-codename-one/002-introduction-to-spring-boot/)
- [Security Basics and Certificate Pinning](/courses/course-02-deep-dive-mobile-development-with-codename-one/024-security-basics-and-certificate-pinning/)
