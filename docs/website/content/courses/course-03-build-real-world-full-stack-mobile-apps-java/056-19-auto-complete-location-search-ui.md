---
title: "19. Auto Complete Location Search UI"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 19
weight: 56
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube Jk3tTyZroP0 >}}

## Transcript

now that the basic infrastructure is out
of the way
we'll start wiring it into the ui
starting with search
the initial search ui i did in the
mock-up was very naive
it just toggled the completion on and
off
the real uber app allows us to swipe the
search ui down and then pick a location
in a map using a pin
this then updates the text field with
the selected location
alternatively if you type into the text
field locations are suggested to you as
you type
there is a great deal of nuanced
behavior in this ui so i only focused on
the big ticket features
the first big feature is the swipe down
feature which allows us to swipe down
the completion ui as we type
the second is the actual completion ui
where elements update as we type
and finally the ability to point at a
location on the map
with a pin
and select that location
i didn't implement a lot of the
relatively easy features such as
bookmarked locations or history in the
search ui
those should be trivial to fill in
in order to implement these i moved most
of the ui logic into separate classes
specifically auto complete address input
and completion container both of which
i'll discuss shortly
a major feature of auto complete address
input is its ability to fold slash
unfold the completion container
it accomplishes this by binding pointer
listeners to the parent form and using
them to implement the drag and drop
behavior
on the left you can see the search ui
with completion suggestions appearing
below
when the suggestions are dragged down
we
can pick the location from the map as
you can see on the right
as the map is dragged the location name
is updated
into the text field
i refactored some of the code from the
map form class into the autocomplete
address input class
it made it easier to implement some of
the related logic
i chose to derive text field rather than
encapsulated
mostly due to convenience
encapsulation would have worked just as
well
for this case
with the exception of these last two
variables every other variable here is
in
the service of the drag and drop logic
we use the data change listener to send
events to the completion logic
however
this callback can be very verbose and
it's sometimes invoked by set text
the solution is a special version of set
text that blocks this callback and
reduces the noise
in the completion code
with the block change event variable
the last focused text field is the one
that now handles the completion
so if the user was in the to text field
everything typed will now impact the
completion for two
and vice versa
pointer listeners on the form allow us
to detect pointer events everywhere
we bind them in the init component
method and remove them in the
de-initialize method
this prevents a memory leak and a
situation where pointer processing code
keeps running and taking up cpu
the initialize is invoked when a
component is removed from the ui or its
hierarchy is removed
it's also invoked when a different form
is shown instead of the current form
init component is invoked when a
component is there
it will be invoked if a component is
added to an already showing form or if a
parent form is shown
you can rely on init component and
de-initialize working in tandem
they might be invoked multiple times in
a valid situations for instance a
dialogue shown on top of the form
triggers a de-initialize on the
components of the form
followed by an init component when it's
disposed
despite using the shorthand lambda
syntax for event handling i need to keep
a reference to the drag and release
event objects so i can remove them later
the dragged element is always the second
element 0 is the first
it can be dragged between the center
location and the south location
if this is indeed a drag operation we'd
like to block the event from propagating
onwards
when a component is in the south we set
its preferred size to 1 8 of the display
height
so it won't peak up too much
when it's dragged up we just increase
that size during drag
components in the center ignore their
preferred size and take up available
space so we use margin to provide the
drag effect
this prevents a drag event on a
different region of the form from
triggering this event
for instance if a user drags the map
dragging just displayed a motion
we now need to remove the component and
place it where it should be we also
reset the ui id
so styling changes for instance margin
unit type etc
will reset to the default
when we place the container in the south
we set the preferred size and margin to
match
when we place it in the center we set
the preferred size to null which will
which is a special case
that resets previous manual settings and
restores the default
the location of a text field
uses strings
but what we really care for care about
is
coordinates on the map
which is why i store them here
this is used both by the map pen logic
and by the search logic we will use
later
