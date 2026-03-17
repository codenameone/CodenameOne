---
title: "15. Location Service - Client Side"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 15
weight: 52
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube TRF76P1Dwwc >}}

## Transcript

next we'll bind the websocket logic and
map UI to bring this all together
we can get started with a location
service class similarly to the user
service class that would abstract the
local location
unlock the user service class the
location service class should also deal
with the physical location of the device
these are the same constants we have on
the server
when sending a location update to the
server I don't want to exceed a fixed
amount of updates so we won't burden the
server or our Network
the Clause has a private Constructor so
the only way to create the location
service is via the bind method which in
turn invokes the instance method bind
impul
we provide two callbacks one is for a
car being added which we will use to
bind a new car to the UI and the other
is for location updates so we can
position the map
location update notifies the server
about changes to the location and also
invokes the Callback once we can
position the map
we invoke the callback with the location
so the map can be shifted to our current
position
once the server socket is connected we
start sending location updates there
we open the websocket connection using
the connect call
we cache the last set of values so we
don't send data to the server unless
something changed
easy threads lets us Post jobs onto a
dedicated thread so we don't have to
block the main EDT
it also means we don't need to deal with
synchronization or any other complexity
related to that as all operations happen
on that thread
until on open is invoked the connection
isn't ready
that's why the server member field is
only initialized here
when it's actually ready
if we already have a location we should
send a user location update
one we have once we have the socket
Connection in place
the connection is single threaded as I
mentioned before
there is this it method is similar to is
EDT and indicates if the current thread
is the one managed by the easy thread if
not we use the run runnable which
invokes the Target runnable on the easy
thread similarly to call serially
if the values didn't change since last
update so we do nothing
we don't update too much there is a
chance we'll miss an update here but
it's probably not a deal breaker if a
user didn't move much
we create a byte array output stream
into which we construct the message that
we received on the server with the
header location
Etc
I currently hard coded a one kilometer
search radius and find explicitly that
we aren't in taxi hailing mode
one line to actually send the binary
data it would be similar with text Data
with the exception of passing overhead
the IR exception isn't likely as this is
a ram-based stream
the
here we received the messages sent from
the server specifically driver search
result
we store user instances in a map where
the user ID is the key
this saves us from sending duplicate
core added events and allows us to just
mutate the user properties which other
code can observe using the built-in
listeners and properties
notice that this code is running on the
websocket thread so events need to go
back into the EDT to prevent potential
issues
this is really important we need to
handle errors properly in a websocket
application otherwise a failure can
leave us without a connection
the Callback interface is Trivial it's
mostly used as a Lambda expression in
the code
and that's it for the location service
