---
title: "Let’s Encrypt, HTTPS Certificate Support"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Setting Up a Cloud Server"
module_key: "09-setting-up-a-cloud-server"
module_order: 9
lesson_order: 4
weight: 29
is_course_lesson: true
description: "Move the backend to HTTPS, connect a real domain name, and prepare the certificates Java needs."
---
> Module 9: Setting Up a Cloud Server

{{< youtube sH5GY816sRc >}}

Once the backend is public, HTTPS stops being optional. Modern mobile platforms expect secure traffic, and production apps should assume that all client-server communication needs transport security from the start.

The lesson begins with the application-side change: configure the server to listen on an HTTPS port and tell the Java runtime where to find the keystore, alias, and password it should use. That is still the correct mental model. HTTPS in Java is not just a hosting concern. The application itself needs access to the certificate material it will present to clients.

Before any certificate can work, the server also needs a real domain name. The video is right to pause on this. Certificates are tied to names, not to bare IP addresses, so DNS is part of the HTTPS setup whether you like it or not. The domain has to resolve to the server before certificate issuance makes sense.

The Let's Encrypt workflow shown in the video is from an earlier moment in the ecosystem, and some of the pain it describes is very specific to that period and stack. The durable lesson is that certificate issuance, renewal, and Java keystore handling need to be understood as one pipeline. Getting a certificate from a certificate authority is only half the job. The Java application also needs the certificate material in a format it can actually use.

That conversion step is why this lesson spends time on PEM files, keystores, and ownership changes. Even if the exact commands evolve, the deployment concern remains the same: obtain the certificate, convert or package it into the format your runtime expects, and place it somewhere the application can read without turning the whole server into a permissions mess.

The warning about Java trust stores is also still relevant in spirit. When certificate authority support changes over time, older runtimes can fail in surprising ways. Secure deployment is easier when your Java runtime is current enough to understand the certificate ecosystem you are using.

## Further Reading

- [Automating Lets Encrypt Renewal Process](/courses/course-03-build-real-world-full-stack-mobile-apps-java/030-automating-lets-encrypt-renewal-process/)
- [Security Basics and Certificate Pinning](/courses/course-02-deep-dive-mobile-development-with-codename-one/024-security-basics-and-certificate-pinning/)
- [Introduction to Spring Boot](/courses/course-02-deep-dive-mobile-development-with-codename-one/002-introduction-to-spring-boot/)
- [Communicating from the Client](/courses/course-03-build-real-world-full-stack-mobile-apps-java/009-communicating-from-the-client/)
