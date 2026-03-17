---
title: "21. Plotting the Route On the Map - Setup"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 21
weight: 58
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube FKTM8jAepJs >}}

## Transcript

next we'll bring these changes into the
map form class which handles the actual
heavy lifting of search
first we need to add a couple of members
to the class
some of these variables really exist in
the method body
i just move them into the class level
i'm skipping that code since it's pretty
trivial
we use map container to place a pin and
position it
we can track map position events using
this object
we need the last focused entry
so we can set the text in the text field
when the user drags the map to point at
a location
the listener is important for cleanup to
prevent multiple listener instances
that way we always have at most one
the timer instance allows us to cancel
it
we use it to delay web service requests
so we don't send them too frequently
the where to button
we need to hide it when the search ui is
showing and show it again when it's done
we place a lot of elements in that layer
on top of the map
it's pretty useful
indicates whether we are in the
navigation mode or in another mode such
as map or browse mode
this is important as we can't enter
navigation mode twice
Code
now that we have these variables in
place
let's look at the code
we created and placed a new layer for
the pin image placement
this allows us to drag the completion
container down and see the pin image on
the map
that also means we can remove it easily
once we exit the search ui
i refactored the text fields to use this
new api
and set the location to the current
location
be to the default
normally a user wouldn't enter the
origin address
only the destination
so using the current location makes
sense
Map
i'm using
name my current location method
to fetch the name of the location of
origin
the map listener is used for the point
location on the map functionality
if i drag the map it will fetch the
location from there and set it to the
last focused text field
however we wait 500 millisecond seconds
before doing that
so we don't send too many web service
requests
we cancel the timer if there is one
that's already in the waiting stage
this used to be if layer dot get
component count is greater than one
but that doesn't make sense anymore as
the completion container is always there
only folded or expanded
so
i check if the completion container is
in the center
or the south
when a button is pressed in the search
completion we get an event to begin
navigation at which point we ask for
directions and enter navigation mode
i'll discuss the whole navigation mode
in the route section
i use animation completion event to show
the completion bar which also has an
animation
in place
one thing i neglected to mention in the
map is the map listener
which we bind using this new method
this prevents duplicate map listeners
and allows us to easily clear the
selection
