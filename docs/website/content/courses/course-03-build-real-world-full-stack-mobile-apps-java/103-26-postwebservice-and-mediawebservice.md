---
title: "26. PostWebService and MediaWebService"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 26
weight: 103
is_course_lesson: true
description: "Finish the HTTP layer by exposing post, feed, comment, and media operations to the client."
---
> Module 13: Creating a Facebook Clone

{{< youtube h-I62aFYBNA >}}

With the user-facing endpoints in place, the rest of the web-service layer becomes mostly a matter of exposing the remaining service operations cleanly. This lesson closes that gap for posts and media.

`PostWebService` is intentionally direct, and that is a strength. The controller methods closely mirror the service methods for listing posts, loading feed pages, adding new posts, adding comments, and registering likes. That one-to-one mapping is a good sign that the service layer already holds the real application logic.

The media side is slightly more subtle because file retrieval and upload bring protocol details with them. MIME types, binary responses, multipart uploads, and visibility-aware retrieval all belong in this layer because they are transport concerns. The controller has to know how to expose them over HTTP even though the service layer decides who is allowed to see what.

The multipart upload support is still the right general approach for client-file uploads. The exact Spring APIs and wrappers may evolve, but the design idea is stable: the client submits the file and metadata in one structured request, and the server turns that into a media record plus an uploaded payload.

The main thing to notice about this lesson is how uneventful it feels. That is exactly what you want. By this point the backend is structured well enough that finishing the endpoint layer mostly means declaratively exposing operations that already exist.

## Further Reading

- [25. WebService Layer and UserWebService](/courses/course-03-build-real-world-full-stack-mobile-apps-java/102-25-webservice-layer-and-userwebservice/)
- [24. PostService](/courses/course-03-build-real-world-full-stack-mobile-apps-java/101-24-postservice/)
- [23. NotificationService and MediaService](/courses/course-03-build-real-world-full-stack-mobile-apps-java/100-23-notificationservice-and-mediaservice/)
- [18. Media Entity](/courses/course-03-build-real-world-full-stack-mobile-apps-java/095-18-media-entity/)
