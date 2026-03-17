---
title: "2. Client to Server Abstraction"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a WhatsApp Clone"
module_key: "14-creating-a-whatsapp-clone"
module_order: 14
lesson_order: 2
weight: 125
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 14: Creating a WhatsApp Clone


{{< youtube gJoJQST5jyM >}}

## Transcript

we'll jump into the client functionality
from the server connectivity class
I won't start with the UI and build
everything up but instead go through the
code relatively quickly as I'm assuming
you've gone through the longer
explanations in the previous modules
like before the server class abstracts
the back end I'll soon go into the
details of the other classes in this
package which are property business
object abstractions
as a reminder notice that I import the
CN class so I can use shorthand Syntax
for various apis
I do this in almost all files in the
project
right now the debug environment points
at the local host but in order to work
with devices this will need to point at
an actual URL or IP address
as I mentioned before we'll store the
data as Json in storage the file names
don't have to end in dot Json I just did
that for our convenience
this is a property business object we'll
discuss soon
we use it to represent all our contacts
and ourselves
this is the current websocket connection
we need this to be Global as we will
disconnect from the server when the app
is minimized
that's important otherwise battery
saving code might kill the app
this flag indicates whether the
websocket is connected
which saves us from asking the
connection if it's still active
if we aren't connected new messages go
into the message queue and will go out
when we reconnect
the user logged into the app is global
the init method is invoked when the app
is loaded
it loads the global data from storage
and sets the variable values normally
there should be data here with the
special case of the first activation
if this is the first activation before
receiving the validation SMS this file
won't exist
in that case we'll just initialize the
contact cache as an empty list and be on
our way
assuming we are logged in we can load
the data for the current user this is
pretty easy to do for property business
objects
if there are messages in the message
queue we need to load them as well this
can happen if the user sends a message
without connectivity and the app is
killed
contacts are cached here
the contacts essentially contain
everything in the app this might be a
bit wasteful to store all the data in
this way but it should work reasonably
even for relatively large data sets
this method sends the content of the
message queue it's invoked when we go
back online
these methods are shorthand for get and
post methods of the rest API
they force Json usage and add the auth
header which most of the server-side
apis will need
that lets us write shorter code
the login method is the first server
sign method
and doesn't do much it sends the current
user to the server then Saves The
Returned instance of that user
this allows us to refresh you the data
from the server
we pass the current user as the body in
an argument notice I can pass the
property business object directly and it
will be converted to Json
in the response we read the user replace
the current instance and save it to disk
sign up is very similar to login in fact
it's identical however after signup is
complete you still don't have everything
anything since we need to verify the
user so let's skip down to that
on the server signup triggers an SMS
which we need to intercept
we then need to send the SMS code via
this API
only after this method returns okay our
user becomes valid
update is practically identical to the
two other methods but sends the updated
data from the client to the server it
isn't interesting
send message is probably the most
important method here it delivers a
message to the server and saves it into
the Json storage
here we have the time in which a
specific contact last chatted this
allows us to sort the contacts based on
the time a specific contact last chatted
with us
this sends the message using a web
service
the message body is submitted as a chat
message business object which is
implicitly translated to Json
initially I sent messages via the
websocket
but there wasn't a big benefit to doing
that I kept that code in place for
reference
the advantage of using websocket is
mostly in the server side where
the calls are seamlessly translated
sorry the advantages of using web
service
is mostly that
if we are offline the message is added
to the message queue and the content of
the queue is saved
this method binds the websocket to the
server and handles incoming and outgoing
messages over the websocket connection
this is a pretty big method because of
the inner class within it but it's
relatively simple as the inner class is
mostly trivial
the bind method receives a callback
interface for various application Level
events for instance when a message is
received we'd like to update the UI to
indicate that
we can do that via the Callback
interface without getting all of that
logic into the server clause
here we create a subclass of websocket
and override all the relevant callback
methods
skipping to the end of the method we can
see the connection call and also Auto
reconnect method which automatically
tries to reconnect every five seconds if
we lost the websocket connection
let's go back to the Callback method
starting with on open
this method is invoked when a connection
is established once this is established
we can start making websocket goals and
receiving messages
we start by sending an init message this
is a simple Json message that provides
the authorization token for the current
user and the time of the last message
received
this means the server now knows we are
connect connected and knows the time of
the message we last received it means
that if the server has messages pending
it can send them now
next we send an event that we are
connected notice I used calls serially
to send it on the EDT since these events
will most likely handle GUI this makes
sense
finally we open a thread to send a ping
message every 80 Seconds
this is redundant for most users and you
can remove that code if you don't use
cloudflare
however if you do then cloudflare closes
connections after 100 seconds of
inactivity
that way the connection isn't closed as
cloudflare sees that it's active
cloudflare is a Content delivery Network
we use for our web properties it helps
scale and protect your domain but it
isn't essential for this specific
deployment
still I chose to keep that code in
because this took us a while to discover
and might be a stumbling block for you
as well
when a connection is closed we call the
event again on the EDT and mark the
connected flag appropriately
all the messages in the app are
text-based messages so we use this
version of the message callback event to
handle incoming messages
technically the messages are Json
strings so we convert the string to a
reader object then we parse the message
and pass the result into the property
business object
this can actually be written in a
slightly more concise way with the from
Json method however that method didn't
exist when I wrote this code
now that we parsed the object we need to
decide what to do with it
we do that on the EDT since the results
would process uh to to impact the UI
the typing flag allows us to send an
event that a user is typing I didn't
fully implement this feature but the
Callback and event behavior is correct
another feature that I didn't completely
finish is the viewed by feature here
here we can process an event indicating
there was a change in the list of people
who saw a specific message
if it's not one of those then it's an
actual message we need to start by
updating the last received message time
I'll discuss update messages soon it
effectively stores the message
act message acknowledges the server to
the server that the message was received
this is important otherwise a message
might be resent to make sure we received
it
finally we invoke the message received
callback since we are already within the
call serially we don't need to wrap this
too
we don't use binary messages and most
errors would be resolved by o to
reconnect still it's important to at
least log the errors
the update method is invoked to update
messages in the chat
first we Loop over the existing contacts
to try to find the right one
once we find the contact we can add the
message to the contact
the find method finds that contact and
we add a new message into the database
this is invoked when a contact doesn't
already exist within the list of
contacts we already have cached
the method closes the websocket
connection
it's something we need to do when the
app is suspended so the OS doesn't kill
the app
we'll discuss this when talking about
the lifecycle methods later
the contacts are saved on the contacts
grid we use this helper method to go
into the helper thread to prevent race
conditions
fetch contacts loads the contacts from
the Json list or the device contacts
since this can be an expensive operation
we do it on a separate context thread
which is an easy thread
easy trades let us send tasks to the
thread similarly to call serially on the
EDT
here we lazily create the easy thread
and then run fetch contacts on that
thread assuming the current easy thread
is null
if the thread already exists we check
whether we already are on the easy
thread assuming we aren't on the easy
thread we call this method again on the
thread and return
all the following lines are now
guaranteed to run on one thread which is
the easy thread as such there are
effectively thread safe and won't slow
down the EDT unless we do something
that's very CPU intensive
we already have the data we use called
serial if we already have the data we
use call Siri on idle this is a slow
version of call serially that waits for
the EDT to reach idle state
this is important for performance a
regular call serially might occur when
the system is animating or in need of
resources if we want to do something
expensive or slow it might cause choking
of the UI call Syria on idle will delay
the call serially to a point where there
are no pending animations or user
interaction
this means that there is enough CPU to
perform the operation
if we have a Json file for the contacts
we use that as a starting point this
allows us to store all the data in one
place and mutate the data as we see fit
we keep the contacts in a contacts cache
map which enables fast access at the
trade-off of some Ram this isn't too
much since we store the thumbnails as
external jpegs
once we loaded the core Json data we use
call serially to send the event of
loading completion
but we aren't done yet
we Loop over the contacts we loaded and
check if there is an image file matching
the contact name
assuming there is we load it on the
context thread and set it to the contact
this will fire an event on the property
object and trigger a repaint
asynchronously
if we don't have a Json file we need to
create it and the place to start is the
contacts on the device
get all contacts fetches all the device
contacts
the first argument is true if we only
want contacts that have phone numbers
associated with them this is true as we
don't need contacts without phone
numbers
the next few values indicate the
attributes we need from the contacts
database
we don't need most of the attributes we
only need we only fetch the full name
and phone number the reason for this is
performance fetching all attributes can
be very expensive even on a fast device
next we Loop over each contact and add
it to the list of contacts we convert
the built-in contact object to chat
contact and the process
for every entry in the contacts we need
to fetch an image we can use calls
serially on idle to do that this allows
the image loading to occur when the user
isn't scrolling the UI so it won't
noticeably impact performance
once we load the photo into the object
we save it to storage as well for faster
retrieval in the future this is pretty
simplistic code proper code would have
scaled the image to a uniform size as
well this would have saved memory
finally once we are done we save the
contacts to the Json file this isn't
shown here but the content of the photo
property is installed to the Json file
to keep the size minimal and loading
time short
once loaded we invoke the callback with
the proper argument
when we want to contact a user we need
to First make sure he's on our chat
platform
for this we have the find registered
user server API with this API we will
receive a list with one user object or
an empty list from the server this API
is asynchronous and we use it to decide
whether we can send a message to someone
from our contacts
this is a similar method that allows us
to get a user based on the user ID
instead of a phone if we get a chat
message that was sent by a specific user
we will need to know about that user
this method lets us fetch the method
metadata related to that user
the chats we have open with users can be
extracted from the list of contacts
since every contact has its own chat
thread
so to fetch the chats we see in the main
form of the WhatsApp UI we need to First
fetch the contacts as they might not
have been loaded yet
we Loop over the contacts and if we had
activity with that contact we add him to
the list in the response
but before we finish we need to sort the
responses based on activity time the
sort method is built into Java
Collections API it accepts a comparator
which we represented here as a Lambda
expression
the comparator Compares two objects in
the list to one another
it returns a value smaller than zero to
indicate the first value is smaller zero
to indicate the values are identical and
More Than Zero To indicate the second
value is larger
The Simple Solution is
sorry smaller The Simple Solution is
subtracting the time values to get a
valid comparison result
we saw the ack call earlier this stands
for acknowledgment we effectively
acknowledge that the message was
received if this doesn't go to the
server
the server doesn't know if a message
reached its destination
finally we need this method for push
notification it sends the push key to
the device of the device to the server
so the server will be able to send push
messages to the device
