---
title: "32. Search: WebService and Client Code"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 32
weight: 109
is_course_lesson: true
description: "Expose the backend search API and map it into the client-side ServerAPI so the UI can treat search like any other paged data source."
---
> Module 13: Creating a Facebook Clone

{{< youtube _WKRuO0Izxg >}}

Once the search service exists, the next step is mostly adaptation work: expose it over HTTP, then teach the client API how to consume it without turning search into a special snowflake.

That is the best part of this lesson. Search is mapped into the same overall client/server architecture as everything else. It gets endpoints on the server, thin client methods in `ServerAPI`, and simple pagination rules that match the rest of the app’s data-loading story.

The rebuild-search-database endpoint shown in the video is clearly a setup convenience rather than something you would want to expose casually in a production system. The important thing is not the exact hack. It is the operational need it addresses: search sometimes needs an explicit initialization path.

On the client side, the generic search method is a nice demonstration of reusing one pattern for several result types. People and posts are different business objects, but from the perspective of transport they are both just paged result lists that need to be converted from JSON into app models.

The note about generic erasure is also useful context here. Java generics feel expressive in application code, but at runtime the client often needs an explicit class token or another concrete hint if it wants to instantiate the right object type safely.

## Further Reading

- [31. Search: Server Side with Spring Boot and Hibernate](/courses/course-03-build-real-world-full-stack-mobile-apps-java/108-31-search-server-side-with-spring-boot-and-hibernate/)
- [33. Search: Client Side UI - SearchForm](/courses/course-03-build-real-world-full-stack-mobile-apps-java/110-33-search-client-side-ui-searchform/)
- [27. Client Side ServerAPI](/courses/course-03-build-real-world-full-stack-mobile-apps-java/104-27-client-side-serverapi/)
- [11. ServerAPI Abstraction Mockup](/courses/course-03-build-real-world-full-stack-mobile-apps-java/088-11-serverapi-abstraction-mockup/)
