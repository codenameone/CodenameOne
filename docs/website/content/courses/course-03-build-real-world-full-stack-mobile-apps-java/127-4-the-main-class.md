---
title: "4. The Main Class"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a WhatsApp Clone"
module_key: "14-creating-a-whatsapp-clone"
module_order: 14
lesson_order: 4
weight: 127
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 14: Creating a WhatsApp Clone


{{< youtube 65LciCzyRNQ >}}

## Transcript

there is still one other class within
the model package
once we finish that
we'll go to the main class
the chat message class is another
property business object
but one that's even simpler than the
last one
this is the author of the message since
we use ids and not phones this might
become an issue so we need to keep track
of both
sent to can be important as this message
might have been sent to a group and not
directly to our current user
the timestamp of the message and the
actual text of the message are the main
payload
attachments aren't fully implemented the
general idea is a url of the attachment
mapping to a mime type so we can
represent it in the ui as image slash
video audio or document
the list of people who viewed the
message which can be more than one for a
message sent to a group
the typing message is a special type of
message that we don't currently send
but it can be sent pretty easily and
just update the ui that the user is
typing to this chat
this is the final property in this class
which is relatively simple
with that long detour out of the way
let's go back to the main class
as you can see i left most of the code
intact and kept it as the default
the first piece of code you will see
that isn't part of the default code is
this line to initialize the server and
load the saved data
there is also the push interface which
we need to implement to receive push
callbacks
that leads us directly to the first
method from that interface
we don't need to implement this method
since we use push only as a visual
medium and rely on web sockets to carry
the actual data
push is inherently unreliable and might
perform badly it places limitations on
the type of data you can send
we have more control over web sockets
we use push only when the app is
minimized
the one method we need to implement from
the push interface is the registered for
push corbett
when this callback is invoked we need to
send the push key to the server this is
important
notice that the push key isn't the
device id
there are different values don't confuse
them
the sms verification class is an
abstract class from the sms verification
cn1 lib
it lets us move some of the
functionality of that library into the
server
the first method is the send sms code
method it sends an sms mesh message to
the given phone number
on the server it invokes the signup call
which triggers an sms to that phone
number
this callback is invoked as part of the
signup process
when the user types in or the system
intercepts a phone number
this callback is invoked it sends the
verification string to the server side
and returns the result based on that
the message listener allows us to track
messages from the server such as connect
incoming messages etc
a lot of this isn't implemented as we
don't need it right now
but it could be useful for the ui as it
evolves
one thing we do implement here is the
message received api
there isn't much going on in this method
though
if the current form
is the chat form
then we need to check if we are
currently in the chat form with the
sender of the incoming message assuming
this is the case we can add this message
to the ui
regardless we need to refresh the main
ui
of the chat list container
since the order
to the contacts will change
next we have the start lifecycle method
you will notice that we invoke bind
message listener even when we restore a
running app
as you might recall we close the
websocket connection when the app is
minimized this effectively restores that
connection when the app is restored back
to normal
the this call happens when the app is
launched in a cold start
if the phone number isn't set this is
the first activation and we need to set
up a new user
the activation form api builds the data
and you are using a builder pattern
where every method adds
to the resulting form
first we allocate the activation form
with the title sign up
when
we then determine that we want a six
digit activation code instead of the
default four digit code
we finally show the activation ui this
accepts two arguments
the second argument is the sms
verification subclass we discussed
earlier
it sends the sms details to the server
which issues an sms it then performs
verification on the server which is more
secure than client-side verification
the first argument is a callback that's
invoked when the activation is completed
it's invoked with a phone number in the
result
here we store the new phone number to
the preferences then show the main form
ui
if the user was already registered we
show the main form
directly we discussed the bind method
before so the last piece is the register
push core
this is an essential part of the push
notification support
finally we added close websocket code to
the stop method this implements the
logic of stopping the websocket
connection
when the app is minimized
