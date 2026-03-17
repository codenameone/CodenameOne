---
title: "25. Hailing in the Client - Networking and Sending Push Messages"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 25
weight: 62
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube 54POZ4PFFBw >}}

## Transcript

next we'll go into the underlying
business logic portion
of the hailing process on the client
up until now i kept hailing as a vague
process
i think uber probably has a far more
elaborate system than the one i
developed in an hour
but this should work fine for most cases
healing includes the following phases
we mark what we are interested
that we are interested in hailing in the
location service websocket code
the server checks which drivers are
available in the area and returns to us
their push keys
we send push notifications to the
available drivers
as time moves on we expand the circle of
search for drivers
in order to do this i had to make some
changes to the websocket protocol
in the location service class
but first we need some additional
variables in the globals class
i'll go into more details on those
values in the next chapter
but for now we need the variables only
all of these apis require developer keys
which you can obtain from their
respective websites
i've edited the globals class to include
these new keys required by the three
apis
right now we can leave these all blank
and get to them
later
let's move to the location service class
and the variables i had to add
when a driver accepts our hail
the server sends a special message
indicating that the hail was accepted
previously we had two modes for polling
the server for searching and not
searching
i added a third mode that allows us to
disable hailing
i've made the location service into a
singleton
this these represent whether we are
hailing
and if so to what radius
we use a motion object to get a growing
radius that will stretch over time to
encapsulate a wider region for hailing
our source and destination values which
we need to broadcast a hail
when we send a push notification to a
car we need to make sure we didn't
already notify it
the unique id of the driver we've found
this is the callback we invoke when a
driver is found
now that these are out of the way let's
look at the other things that need doing
we changed the way we handle
communication protocol by and we added
some additional details
first we need to ignore location changes
when doing hailing
which we can do by adding that condition
next we need to change the protocol a
little bit
this was previously limited to 0 only
and now we check if we are in hailing
mode
during hailing mode the radius of search
grows over time
we send the from two values as utf-8
encoded strings
which allows us to communicate locale
specific locations
when we turn off healing in the server
it's a one-time thing after it's off we
can go back to the regular mode
this isn't likely as this is a ram based
stream
we also need to handle
message reception code
this is a new special case that provides
us with details on the driver that
picked up the right
we are provided with the driver id card
and name
notice we need the final user variable
since car might change and the value
that can change
can't be passed to an inner class or
lambda in java
this is a list of push keys
who we should not notify
i added a push token to the driver
details so we can send a push message to
a specific driver
if the car wasn't notified yet
add it to the list of cars
that we should notify
we send the push message
in a batch to speed this up
we send push type 3 which includes a
data payload the first section
and a visual payload which you can see
after the semicolon
before we can compile that code we need
to add a push token attribute to the
user class
finally we have the hail ride method
which is relatively simple
there isn't much we need to cover here
it just initializes the variables and
starts the motion object for the
expanding radius
this should conclude the client side of
the hailing process
and now we need to address the server
side
