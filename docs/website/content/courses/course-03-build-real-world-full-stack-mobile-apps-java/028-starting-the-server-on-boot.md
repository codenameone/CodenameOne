---
title: "Starting the Server on Boot"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Setting Up a Cloud Server"
module_key: "09-setting-up-a-cloud-server"
module_order: 9
lesson_order: 3
weight: 28
is_course_lesson: true
description: "Copy the deployed artifacts to the server and wire the backend into the operating system so it starts like a real service."
---
> Module 9: Setting Up a Cloud Server

{{< youtube mqNtfN2C3fM >}}

Manual deployment is tolerable exactly once. After that, you want a repeatable way to copy the backend onto the server, keep configuration files in the right place, and make sure the application comes back automatically after a reboot.

The lesson starts with secure copy, and that is still a perfectly reasonable first deployment tool. You copy the configuration file, copy the built artifact, and keep the transfer explicit enough that you can see what changed. It is not fancy, but it is understandable, and that matters when you are still assembling the deployment story.

The next step is the one that turns the backend from a jar file into a service. The original lesson leans on a Spring Boot feature that allows the packaged artifact to behave like an executable Linux service. That is a strong fit for a small VPS because it lets the operating system manage startup and logs instead of forcing you to keep the app alive manually in a shell.

The video uses symbolic links and classic init-style service integration. The exact boot integration mechanism you would choose today may differ depending on distribution and tooling, but the principle is the same: the server should know how to start the backend on boot, where its logs live, and which configuration belongs to that deployed instance.

The lesson also includes supporting pieces such as copying environment-specific properties, creating the database, and placing build tools where the backend expects them. Those details are not glamorous, but they are what make deployment dependable. A server is not really deployed until the configuration, runtime, filesystem layout, and startup behavior all line up.

## Further Reading

- [Setting up the VPS Server](/courses/course-03-build-real-world-full-stack-mobile-apps-java/026-setting-up-the-vps-server/)
- [Let’s Encrypt, HTTPS Certificate Support](/courses/course-03-build-real-world-full-stack-mobile-apps-java/029-let-s-encrypt-https-certificate-support/)
- [Introduction to Spring Boot](/courses/course-02-deep-dive-mobile-development-with-codename-one/002-introduction-to-spring-boot/)
- [Connecting to a Web Service](/courses/course-02-deep-dive-mobile-development-with-codename-one/003-connecting-to-a-web-service/)
