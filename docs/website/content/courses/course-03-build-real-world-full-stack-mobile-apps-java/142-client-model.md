---
title: "Client Model"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Create a Netflix Clone"
module_key: "15-create-a-netflix-clone"
module_order: 15
lesson_order: 5
weight: 142
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 15: Create a Netflix Clone


{{< youtube HJvMQKM5FPY >}}

## Transcript

we're finally at the client side which
is also pretty simple
this time we'll start with the model
and move to the ui rather than the other
direction which is what we normally
cover
since we did the server work first this
fits pretty directly after that
which makes it easier to start with that
portion
this model is pretty much proper
property business objects to match the
lombok objects in the server side
while some of the terse aspects of
lombok are missed properties make up for
it by being far more powerful overall
we start with the server class which
abstracts our connection to the server
since we have one method in the server
web service this is continued here we
fetch the content from the server
synchronically using the rest api this
api translates to the the response to
json almost seamlessly
we say that the response should be in
the form of the content collection class
this means the json will be parsed into
that class which will
will examine next
the content collection is a standard
property business object it maps almost
directly to the class with the same name
on the server side the main difference
is that the properties use our object
syntax
these match their definition in the
server side and include the exact same
data the entire class is pretty standard
property business object
finally the last properties object we
have is the content object which is the
same as the one in the server
the final piece is the enum that matches
the one in the server with that our
communication layer is complete and we
can move on to the ui elements
thanks for watching i hope you'll enjoy
the rest of the course and find it
educational
