---
title: "13. User Web Service"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a WhatsApp Clone"
module_key: "14-creating-a-whatsapp-clone"
module_order: 14
lesson_order: 13
weight: 136
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 14: Creating a WhatsApp Clone


{{< youtube BSrupbUahRM >}}

## Transcript

next we'll jump to the web service
package
this is pretty much identical to the
facebook clone app
we create a web service mapping for the
user services
technically we could have broken this
down
to more web services but there is no
real reason as we don't have that much
functionality here
this is a thin wrapper around user
service that contains no actual
functionality it only translates the
logic in that class to web calls
if an exception is thrown in this class
it's implicitly translated to an error
dao which is translated to an error json
login and sign up are almost identical
with the small exception that login
expects an auth header value
both are simple post methods
that return the dao object as json body
to the client
verify and update return string values
to indicate that they succeeded
i added the implementation to get set
avatar
via url
but this
isn't mapped to the client side this can
probably be implemented in the same way
as the facebook clone
these methods return their result as an
array of one element or as a zero length
array since there is no way and json to
return null like the business logic
method does
so we return a blank array list
or a list with one element
and that's the end of the class the rest
of the methods delegate directly to the
user service bin
