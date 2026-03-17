---
title: "33. Search: Client Side UI - SearchForm"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 33
weight: 110
is_course_lesson: true
description: "Build a dedicated search UI that updates as the user types without flooding the server with unnecessary requests."
---
> Module 13: Creating a Facebook Clone

{{< youtube sxCADu-SjpQ >}}

The backend search pipeline is only useful once the UI can take advantage of it, and this lesson does that in the most practical way: a dedicated search form with debounced queries, a mode toggle, and paged results.

The debounce logic is the heart of the screen. Search-as-you-type feels great only if it avoids the trap of sending a request on every keystroke. The lesson solves that by tracking recent edits, using a short delay, and cancelling pending work when the user is clearly still typing.

That is the right tradeoff. The form remains responsive, the server is not spammed with pointless queries, and the user still experiences search as immediate.

The mode switch between people and posts is also handled cleanly. The screen does not need two unrelated search experiences. It needs one search form with a different result builder depending on the current target domain. That keeps the UI simple and keeps the code aligned with the generic search API from the previous lesson.

Using an `InfiniteContainer` for the results is a natural choice because search, like feeds, is a paged result stream. Once the query text and mode are stable, the result list can grow as needed without inventing a separate loading model.

The final touch is small but important: putting the editable search field directly in the title area and immediately entering edit mode makes the form feel like a search tool instead of a normal screen that happens to contain a text field.

## Further Reading

- [32. Search: WebService and Client Code](/courses/course-03-build-real-world-full-stack-mobile-apps-java/109-32-search-webservice-and-client-code/)
- [34. Search Results UI - UserForm and PostForm](/courses/course-03-build-real-world-full-stack-mobile-apps-java/111-34-search-results-ui-userform-and-postform/)
- [12. The Newsfeed Container](/courses/course-03-build-real-world-full-stack-mobile-apps-java/089-12-the-newsfeed-container/)
- [Threading and the EDT](/courses/course-01-java-for-mobile-devices/011-threading-and-the-edt/)
