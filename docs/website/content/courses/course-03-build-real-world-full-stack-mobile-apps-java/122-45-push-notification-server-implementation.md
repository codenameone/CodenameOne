---
title: "45. Push Notification - Server Implementation"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 45
weight: 122
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 13: Creating a Facebook Clone


{{< youtube 5WpZmIkdUfs >}}

## Transcript

the changes to the notification service
are significant and in effect that's the
class that implements push notification
calls from the server
before we start we need to add a few
fields to the class
these will be used by the code we
introduced soon
these are besides the keys field i
mentioned before
this is the url of the codename one push
server we'll use that to send out push
messages
logging is used for errors in the body
of the class
we need to inject the user's repository
now as we need to find the user there
the first step is trivial
it's about mapping the push key to the
service class which is exactly what we
do here
on push registration we invoke this
method to update the push key in the
user object
if the push server indicates that a push
key is invalid or expired we delete that
key
a fresh key will be created the next
time the user runs the app
the second method is internal to the
server and isn't exposed as a web
service but the former method is
the next stage is the push notification
itself
we add these lines to the send
notification method
notice that a push key can be null since
it can expire or might not be registered
yet
this leads us to the send push
notification method
but we'll get there via detour
this method is based on the server push
code from the developer guide notice
that this is the import method not the
method we invoked before
we'll go through this first and then
reach that code
we connect to the push server url to
send the push message
a standard form post submission request
is used
we fetch the values for the fields that
can defer specific
specifically the certificate and
passwords
which vary between production and
development
we send all the details as a post
request to the codename one push server
which then issues a push to the given
device type
the server can return 200 in case of an
error
but if the response isn't 200 then it's
surely an error
the server response is transformed to a
string for passing on the next method
i'll cover the read input stream method
soon
we need to go over the responses from
the push server these responses would
include information such as push key
expiration
and we would need to
purge that key from our database
that's all done in the actual method for
sending push messages
the async annotation indicates this
method should execute on a separate
thread this is handled by spring we'll
discuss that soon
senpush impul returns json with messages
which we need to process
the spring json parsing api has
different forms for map
list
but we can get both in the response from
the server so we need to check
if it's a list then it's a device list
with either acknowledgement of sending
for android only or error messages
if we have an error message for a
specific device key we need to remove it
to prevent future problems with the push
servers
if we got a map in response it could
indicate an error which we currently
don't really handle other than through
logging
if push doesn't work the app would still
work fine you'll just need to refresh
manually a better implementation would
also use a fallback for cases of push
failure for instance websocket but it's
not essential at least not at first
we referenced read input stream
in the previous code blocks
it's defined in the code as such
i could have written
more modern code using nio but i was
running out of time and i had this code
handy it does the job well
next we expose the push registration
method as a restful web service
this is a direct mapping to the update
method so there isn't much to say about
that
the last piece of the server code is the
changes we need to make to the facebook
clone server application class
we didn't touch that class at all when
we started off but now we need
some changes to support the asynchronous
send push notification method
first we need the at enable async
annotation so at async will work in the
app
the at bin for the async executor
creates the thread pool used when we
invoke an at async method
we define reasonable capacities
we don't want an infinite queue as we
can run out of ram and it can be used in
a denial of service attack
we also don't want too many threads in
the pool as our server might overcrowd
the push servers and receive rate limits
this level
should work fine
with that the server side portion of the
push support is done
