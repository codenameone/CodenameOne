---
title: "12. Server WebSocket Handler"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 12
weight: 49
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube 3-ZH2IFIIMY >}}

## Transcript

continuing the server side code will now
delve into the location logic
which maps to the websocket support in
spring boot
WebSockets
websocket is a special type of socket
that is created through http or https
request
a web server that supports web sockets
opens a regular http connection and then
uses
the socket open there to continue
working as a regular socket
as a result the websocket setup is
slower than a regular tcp socket
but they provide the same level of
flexibility
after creation
the advantage over tcp sockets is
compatibility and the ability to patch
pass through potential problematic
firewalls
as those would see a websocket as
another http
connection
the websocket api includes two types of
packets
text and binary
in this case i'll use the binary
protocol because it's pretty easy to do
this in java
up until now all our communications
went through web services
which is convenient and scalable
the fact we can use tools like curl and
the network monitor to see what is going
on under the hood
is very helpful
however web services suffer
from the performance overhead and fixed
structure issues of http
for more interactive data we would
prefer something like websockets
some people use websockets for all their
communications and it might work for
your use cases
a lot of developers use the text-based
websocket as a substitute to web
services altogether
and in some cases that makes sense
however as i mentioned before we have
decades of experience with http it works
well and has a huge infrastructure of
tools behind it
websockets are a low level api there are
some higher level abstractions on top of
them
but these often go back to the problems
of http without giving much in return
WebSocketConfig
spring boot has
decent support for websockets but you
need to activate it first
we need to define a configuration class
that sets up the websocket environment
this class serves as a configuration
tool for the websocket api defining
limits quotas and handlers
here i set common configuration
arguments for websocket messages
setting buffer sizes
for the different types
here i find the handler class to the ws
msg url which will receive all of the
websocket callbacks
before we go into the handler class
let's create a special service class to
handle location-based callbacks
similarly to the user service
LocationService
most of the location apis
map to the user class
but it's logically separate from the
user service
we will periodically update the user's
location
notice that location can only be updated
by the user himself
as the token is required
for that operation
it's more intuitive to work with radius
from the client but the jpa query
language
makes it easier to work in absolute
coordinates so i convert the kilometer
radius unit
to latitude longitude values
we have two versions of the query
one finds all of the drivers in the area
so we can draw them on the map
the second searches for available
drivers
only for hailing purposes
i use a version of the method that only
returns a part of the user data as we
normally don't need
all of the data
once this is in place we can implement
the handler class which is the actual
websocket implementation
but first let's review the communication
Handler - Packet structure for location update
protocol this is the binary structure we
will use when receiving request
on the server for a location update
so when a user changes his current
location we will send this data
the message type should be 1 for a
location update from the user
the length of the user token string
followed by a byte array of the token
length representing the string
notice that i used bytes instead of cars
since the token is 100 ascii
i can rely on that fact and reduce the
packet size further
the location data and the radius
slash direction of the user
a byte which is set to one when we are
hailing a taxi in which case it will
seek
only the available drivers
once this packet is processed the server
would return the cars within the search
radius by sending a packet back
Handler - Packet structure for response
in this case we don't need the token as
this is a message from the server
the response type can be 2 for driver
position update and 3 for available
driver position update
the entry
indicates the number of drivers
in the returned data
the rest of the lines repeat for every
driver
response size times
and include the position data for every
driver
now that we understand the protocol
let's dig into the code that implements
it
the handler class is a binary websocket
handler that receives callbacks on
incoming packets
let's go over the code
these are constants used in the binary
protocol to communicate the type of
request or response
this is a callback for a binary message
from the client
the api works with nios
bytebuffer which allows us to run
through a request
efficiently
we get the length of the user token
string and the battery again i used
bytes instead of cars since the token is
100 ascii
we can rely on that
assuming this is a location update
we pull out the data and update the user
object
we prepare to return a response based on
the seeking flag
we also need to mark the response type
correctly
i used a bytearray output stream to
construct the response
i use try with resources to close the
streams automatically when i'm done
i just write out the response data to
the stream
and finally we convert the battery data
from the stream to a battery
then send to the client
this is it for the basic server code
