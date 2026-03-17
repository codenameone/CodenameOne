---
title: "12. User Service"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a WhatsApp Clone"
module_key: "14-creating-a-whatsapp-clone"
module_order: 14
lesson_order: 12
weight: 135
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 14: Creating a WhatsApp Clone


{{< youtube 5EyrMpQqR-k >}}

## Transcript

the next step is the services layer
which implements
the relatively simple business layer of
this application
we'll start with the user service class
it's the main class in this package and
handles pretty much the entire business
logic of the app
it's a service bean this means it
handles generic business logic for the
application
we need access to the repositories we
just defined for users groups and
messages
the api keys service is the exact one i
used in facebook with the exception of a
different properties file name
i'll discuss that later
it generally abstracts api keys and
separates them from the source code
the password encoder is used to encrypt
and verify the token
media repository and notification
servers are identical to the stuff we
had in the facebook clone app
this method sends an sms message via the
twillow web service
if you recall we added the twillow sdk
into the pom file in the first lesson
this sdk makes sending an sms message
very easy as you can see from the code
the login api lets us validate a user
and get the current data
the server has for that user
since there is no username slash
password we need to use the token to
authenticate
first we need to find the user with the
given phone assuming the user isn't
there will throw an exception
since the auth is hashed as we discussed
before
we need to test the incoming auth via
the matches method in encoder it
verifies the hash matches the auth token
this method creates a string of the
given length which includes a random
number for verification
signup creates an entry for a specific
user but doesn't activate the account
until it's verified
we first check if the phone number is
already registered if so we need to fail
otherwise we create the new user and
initialize the value of the data and the
verification code
finally we send the activation code and
return the user entity
the verify method activates a user
account if the verification mode is
correct we mark the user as verified and
return true
we use set props both from the sign up
and update methods
there isn't much here but if we add
additional metadata this might become a
bigger method like it is and the
facebook clone
update verifies the user's token
then updates the properties there isn't
much here
these aren't used at the moment
but they are pretty much identical to
what we have in the facebook clone and
should be easy to integrate in a similar
way
this is part of the work to integrate
support for the user typing feature
right now the client app doesn't send or
render this event but it should be
relatively simple to add
when a user starts typing to a
conversation we can invoke this method
two user can be a user or a group
is
the user present it's if the user is
present it's a user
i'll discuss the event code in the
sockets
when we reach the app socket class
and this if this is a group we need to
send the event to all the users within
the group
via the socket connection
this method sends a message to its
destination which can be a user or a
group
in order to send a message we first need
to create a chat entity message entity
so we can persist the message in case
delivery failed
this is the same code we saw in the
typing event if the message is destined
to a user the following block will occur
otherwise we'll go to the else block
where the exact same code will execute
in a loop
over the members of the group
we mark the destination of the message
and convert it to json to adjacent
string
we invoke the send message api
the send message uses the socket to send
the message to the device
if this failed and the device isn't
reachable
we should send this message as text
using push notification
this method is identical to the other
send message method but it uses a json
string
which is more convenient when a message
comes in through the websocket
the previous version is the one used
when this is invoked from the web
service which is what we use
and this one works when a message is
sent via the websocket
this method converts a chat message
entity to json so we can send it to the
client
object mapper can convert a pojo object
to the equivalent json string
this method sends json via the socket to
the group or a user it allows us to
propagate a message onward it works
pretty much like the other methods in
this class that send to a group or a
user
this method finds the user matching the
given phone number this method is used
by find registered user and find
registered user by id
it generalizes that translation of a
user list to a single user dial value it
implicitly fails for unverified users as
well
ack allows us to acknowledge that a
message was received
it just toggles the ack flag
when a user connects via websocket this
method is invoked it finds all the
messages that weren't act by the user
and sends them to that user
that way if a device lost connection
it will get the content once it's back
online
this method is invoked on launch to
update the push key
in the server so we can send push
messages to the device
with that user service is finished
