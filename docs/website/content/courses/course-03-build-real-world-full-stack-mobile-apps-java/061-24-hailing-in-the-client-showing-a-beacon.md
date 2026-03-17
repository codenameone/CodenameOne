---
title: "24. Hailing in the Client - Showing a Beacon"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 24
weight: 61
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube qEgDZHZJEYo >}}

## Transcript

now that we have a path on the map
we can move forward to the hailing
process
the hailing process is relatively simple
we tint the ui show a beacon and during
that time we ask the server for a call
we can start by adding an event handler
to the black button from the enter
navigation mode method
we are effectively coloring the pin
layer to create the tint effect
we added a new blink dot class
to implement the pausing blue dot effect
another new api
the hail ride method in location service
allows us to hail a ride
notice i don't show anything when the
ride is hailed i'll add that workflow
with the driver app later
there is one uid we need to cover here
and it's the searching dialog ui id
technically this ui element isn't a
dialog it is a label
but it looks like a dialog
padding is pretty standard for label
we have some margin on the sides to
space it out from the edges
most uii of this ui id is pretty
standard except for the use of the
special mode of the round wrecked border
the top only mode allows only the top
portion to be rounded and the bottom
appears square
usually we use it to combine two borders
together
with different colours or ui ids
in this case
we give the component a feel of peaking
from the bottom of the form
the font is the standard font just like
any other label
the blinker.class is pretty trivial
i could have used an animated gif but
instead i just did this
this is mostly for transparency we don't
really use
the uid here
i use
low level animations here
so the best practice is to register
remove
with init component
slash the initialize
the motion class represents a timed
motion
between values which allows us to
animate a value from point x to point y
in this case i'm just growing the circle
using the value
notice
only the animate method mutates values
as the paint method can be invoked more
than once per cycle
in theory
the drawing logic is mostly hard-coded
i would have used the shape api to get a
more refined effect
but it would have made things more
complicated
