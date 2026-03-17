---
title: "31. Search: Server Side with Spring Boot and Hibernate"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 31
weight: 108
is_course_lesson: true
description: "Add full-text search support on the backend and index only the fields that make sense to expose through user-facing search."
---
> Module 13: Creating a Facebook Clone

{{< youtube tWMVUDScyfQ >}}

Search feels like it should be enormous, but a lot of the complexity is hidden if you choose the right backend tooling. This lesson leans on Hibernate Search to add basic full-text capabilities without turning the whole app into a hand-built indexing engine.

The most important design decision here is not the library choice. It is deciding what should and should not be searchable. That is a product and privacy decision before it is a technical one.

The lesson handles that correctly by indexing only the fields that make sense for public-facing search, such as user names and post text. That is a much healthier default than treating every stored field as fair game just because the search engine can technically index it.

The search service then stays relatively small because the heavy lifting is delegated to the indexing layer. Query building, fuzzy matching, and pageable result retrieval become configuration and API choices rather than hand-written SQL gymnastics.

That does not mean search is trivial. It means the complexity has moved to a layer designed to absorb it. This is exactly the kind of feature where a framework-backed solution is worth using instead of inventing something clever in application code.

The one-time index build is also a useful operational reminder. Search is not just a query problem. It is an indexing lifecycle problem. The system needs a story for the first build and for staying up to date as entities change.

## Further Reading

- [32. Search: WebService and Client Code](/courses/course-03-build-real-world-full-stack-mobile-apps-java/109-32-search-webservice-and-client-code/)
- [33. Search: Client Side UI - SearchForm](/courses/course-03-build-real-world-full-stack-mobile-apps-java/110-33-search-client-side-ui-searchform/)
- [17. Spring Boot Server Architecture and the User Entity](/courses/course-03-build-real-world-full-stack-mobile-apps-java/094-17-spring-boot-server-architecture-and-the-user-entity/)
- [Search Results UI - UserForm and PostForm](/courses/course-03-build-real-world-full-stack-mobile-apps-java/111-34-search-results-ui-userform-and-postform/)
