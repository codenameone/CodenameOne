---
title: "26. Driver App Server"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 26
weight: 63
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube KckMUmmSzd4 >}}

## Transcript

before we proceed into the actual driver
app work and push notification
we need to implement all the
infrastructure in the server side
i also need to implement some changes we
need in the protocol
start by looking at the uber class
i had to add three new fields and modify
slash add some methods
hailing from and healing 2 allows us to
communicate our trip details with the
driver community
i need the push token of drivers so we
can hail them directly from the app
i'll discuss the ride dao class soon it
allows us to send the details about the
trip to drivers
not much of a change
but i added the push token to the user
dao factory method
ride isn't as simple as rydao despite
their common name
it contains far more information
currently we don't use all of that but
the fact that it's logged will let you
provide all of that information within
the app or a management app easily
the right class is a jpa entity similar
to the user class
i used an auto increment value for the
id instead of a random string
i wanted to keep things simple
but notice this can expose a security
vulnerability
of scanning for rides
the passenger and driver are relational
database references to the respective
database object representing each one of
them
the route itself is a set of waypoints
sorted by the time associated with the
given waypoint
we'll discuss waypoints soon enough but
technically
it's just a set of coordinates
i really oversimplified the cost field
it should work for some and currency
but it's usually not as simple as that
it's important to use something like big
decimal and not double when dealing with
financial numbers
as double is built for scientific usage
and has rounding errors
we have two balloon flags a ride is
started once a passenger is picked up
it's finished once he is dropped off or
if the ride was cancelled
the companion crude ride repository
is pretty standard with one big
exception
i added a special case finder that lets
us locate the user that is currently
hailing a call
notice the syntax b dot driver dot id
equals
question mark 1
which points through the relation to the
driver object
the waypoint entity referenced from the
right entity is pretty trivial
notice we still need a unique id for a
waypoint even if we don't actually use
it in code
the interesting part here
is the time value
which is the value of system current
time melees
this allows us to build a path based on
the time sequence
it will also allow us to reconstruct a
trip and generate additional details
such as speed cost
if we wish to do that in the future
notice that there is also a waypoint
repository interface i'm skipping it as
it contains no actual code
the right service class serves the same
purpose as the user service class
focusing on rides and driver related
features
i could have just stuck all of this
logic into one huge class
but separating functionality to
different service classes based on logic
makes sense
we manipulate both the rides and users
crude objects from this class
healing is a transactional method
this means that all operations within
the method will either succeed or fail
depending on the outcome this is
important to prevent an inconsistent
state in the database
this method
can be invoked to start and stop hailing
in this case we use the assigned user
property to detect if a driver accepted
the ride
if so we return the driver data to the
client
when a driver gets a notification of a
ride he invokes this method
to get back the data about the ride
if the driver wishes to accept the ride
he invokes this transactional method
the method accepts the token from the
driver and the id of the user hailing
the right
it creates a new write entity and
returns its id
from this point on we need to refer to
the right id and not the user id or
token
start ride and finish ride are invoked
by the driver when he picks up the
passenger and when he drops him off
normally
finish ride should also handle elements
like billing etc
but i won't go into that now
the next step is bringing this to the
user through a web service
the ride web service class
exposes the ride service call almost
verb team to the client
the get call
fetches the right dial
for the given user
id start and finish rides are again very
simple with only one argument which is
the ride id
we also have to add some minor changes
to the uber service and location service
classes
let's start with the user service class
drivers need a push token so we can hail
them
this is always set outside of the user
creation code for two reasons
the first time around the user is
created but the push key isn't there yet
it arrives asynchronously
push is re-registered in every launch
and refreshed so there is no reason to
update the entire object for that
the user web service class needs to
mirror these changes obviously
there isn't much here we just added a
new set push token url
and we accept this update
the location service
needs a bit more work
every time we update a user's location
we check if he's a driver on a ride
assuming we have a ride object we check
if this is currently an ongoing ride
that wasn't finished
if so
we add a waypoint to the ride and update
it
so we can later on inspect the path of
the ride
this pretty much tracks rides seamlessly
if we wanted to be really smart we could
detect the driver and use a position to
detect them traveling together and
automatically handle the ride
there are obvious problems with this as
it means a user can't order a cab for
someone else
but it might be an interesting feature
since we have two close data points
