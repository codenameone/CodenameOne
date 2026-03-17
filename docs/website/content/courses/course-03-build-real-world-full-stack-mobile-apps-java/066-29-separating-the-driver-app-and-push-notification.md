---
title: "29. Separating the Driver App and Push Notification"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 29
weight: 66
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube gmqFd2bU_fM >}}

## Transcript

before we go into the big ui changes
let's go over some of the networking
level level changes in the code
in order to encapsulate the new ride
json object from the server i added an
equivalent properties object locally
the class is practically identical to
the server side ride dao class
but uses the properties syntax instead
of the standard pojo syntax
the driver service class is a static
representation of the driver specific
server api
this field represents the id of the
current ride from this driver
here we have the standard get method
i mentioned earlier to retrieve the ride
details and return them via a callback
we use accept to indicate a driver is
accepting a user's healing
if he doesn't we don't care
once accepted he gets a reference to the
id of the newly created ride object and
the server
notice that
failure
is indeed a possibility for example if
the user canceled the ride or a
different driver accepted first
when we invoke start ride and finish
ride we use current ride id
unlike the user id which we used to
create the ride
in search service we had to add support
for geocoding
before this we only had the reverse
geocoding which we used to locate the
from slash 2 points on the map
we need this api since the driver only
gets the two slash from location names
and we want to plot them on the map
there isn't all that much to say about
this method it just searches the google
geocode api for a location with the
given name and returns the coordinates
of that location
there were many small changes
in the user service class
most of them relate to the way identity
is managed in the app
one of the big problems in having two
applications with one project
is that both projects share the same
data in the simulator
so if i want to launch the project twice
once to run the user version and once
for the driver version i will have a
problem
both will inspect the same storage
information and use the same user
identity
they might collide
notice that this is purely a simulator
problem
the simulator doesn't currently isolate
separate applications
ideally this is something to improve in
the simulator
and might not be an issue in the future
the solution is simple though
we can just save the data to different
locations or keys
if we are in the driver app
let's review the changes
this is illustrated perfectly in the
first change in this class
we use a different token to determine if
the user is logged in
for the case of a driver
notice we replaced the invocations of
preferences dot get token
that were all over the code with this
method call
the preferences bind api
lets us set a different prefix for the
driver object that will be prepended to
the properties in the preferences
this is a cool little trick that allows
me to debug with a fake number
i used the is simulator method to log
the verification code on the simulator
and can just type it in
even if the twilio code failed to send a
message
sends the push notificati token to the
server
right now we don't need to do anything
in the event callback
this is invoked on registration success
and allows the server to send driver
push keys to the client
after the first activation of the driver
app
we need to register for push
notice i'm using the version of this
method from the cn class
with static import
but the callback will go as expected
into the driver app class
