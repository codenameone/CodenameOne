---
title: "14. Web Socket"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a WhatsApp Clone"
module_key: "14-creating-a-whatsapp-clone"
module_order: 14
lesson_order: 14
weight: 137
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 14: Creating a WhatsApp Clone


{{< youtube YwD35TG6WAg >}}

## Transcript

next we'll jump to the websocket package
which is the last package
first we need to configure the websocket
we do this by implementing the websocket
configurer
and using the annotations on the class
to indicate its purpose
we define the socket so we can define
the packet size
to 8k
we can set a larger size but generally
keeping packets small
is a good practice
the app socket class is bound to the
slash socket url
in this line of code
this is the thread used to process the
websocket connections
we can allocate more thread resources
based on need
let's go to the app socket
the app
is an implementation of text websocket
handler which handles text messages
since all our messages are json this
makes more sense
i cache the currently active connections
here
this isn't a good approach in the long
term a better approach would be redis
for
this sort of caching
but for an initial app this can work
fine
we need access to the user service
so we can send message
to a group or
user
the method sends this method sends json
to a websocket based
on the user token and returns true if it
is successful
we get the sessions for the given client
if he has a web service session
we create a text message
with js
the json
we loop over all the websocket
connections
one by one
if a connection is open
we send the message
there and return
otherwise we add the socket to the
remove queue
we don't want to remove in the middle of
the loop to prevent an exception
we remove all the defunct websockets
from the queue in this
line
for all the classes
we're sending via socket in work we
return force
this method handles the incoming text
packets
we need
to parse the json into a map
if m
has a type of pro it's probably an init
method
if
init messages allows us to add a
websocket
to our cache of connections so we can
push a message back into the websocket
when we need to send a server note
notification
otherwise we test if this is a user
typing event in which case we need to
send a typing message onward
finally we send the message as json to
the users in the group or to the
specific user this invokes the code we
saw in the user service class
when combination
when a connection is closed we loop over
the existing list and purge it
of the dead connection
for simplicity we don't support partial
messages which shouldn't be necessary
for a small 8k messages
with that the class is done
