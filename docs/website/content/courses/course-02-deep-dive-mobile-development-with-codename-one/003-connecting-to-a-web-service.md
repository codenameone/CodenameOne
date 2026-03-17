---
title: "Connecting to a Web Service"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Working with Custom Web Services"
module_key: "02-working-with-custom-web-services"
module_order: 2
lesson_order: 2
weight: 3
is_course_lesson: true
description: "Call a JSON web service from Codename One and structure the client-side code cleanly."
---
> Module 2: Working with Custom Web Services

{{< youtube 3B7C0ZbV8Bc >}}

Once a backend exists, the mobile app needs a clean way to talk to it. In practice that usually means sending HTTP requests, receiving JSON responses, and converting those responses into application objects that the UI can work with. That process is simple in concept, but it becomes messy fast if networking code leaks everywhere.

The first thing worth doing is separating transport code from UI code. A form should not be responsible for knowing how to build URLs, set headers, serialize request bodies, and parse raw JSON. Put that logic in a small service layer instead. Then the rest of the app can ask for higher-level operations such as "add item", "load items", or "log in" instead of dealing with low-level request mechanics every time.

The video builds requests directly with `ConnectionRequest`, serializes JSON explicitly, and parses responses back into model objects. That is still a valid way to understand what is happening on the wire, and it is a good lesson because it keeps the protocol visible. Even if you later wrap the code in helper utilities or higher-level abstractions, you still need to understand the basics: HTTP method, URL, headers, body, response status, and JSON parsing.

One of the most important design choices here is whether a call should be treated synchronously or asynchronously. Synchronous code can be easier to read because it flows top to bottom, but it must be used carefully and never in a way that freezes the user interface. Asynchronous code is more flexible and generally safer for long-running work, but it pushes you toward callback or continuation-style logic. There is no single correct answer for every case. What matters is knowing which model you are using and keeping UI responsiveness intact.

For most applications, the practical structure is this: define a model object that matches the data you exchange with the server, define a client-side service class that knows how to talk to the backend, and keep the UI layer focused on displaying results and collecting user input. That separation pays off quickly once the project grows beyond one or two endpoints.

This lesson also highlights a broader truth about mobile networking: the app is always dealing with a hostile environment. Networks fail, servers return unexpected responses, and payloads change over time. Good client code assumes that requests can fail and that parsing may not always go the happy-path way. Even a simple demo service is worth writing as though it were real.

## Further Reading

- [Developer Guide](/developer-guide/)
- [How Do I Access Remote Webservices, Perform Operations On The Server](/how-do-i/how-do-i-access-remote-webservices-perform-operations-on-the-server/)
- [How Do I Use HTTP, Sockets, Webservices & Websockets](/how-do-i/how-do-i-use-http-sockets-webservices-websockets/)
- [Threading and the EDT](/courses/course-01-java-for-mobile-devices/011-threading-and-the-edt/)
