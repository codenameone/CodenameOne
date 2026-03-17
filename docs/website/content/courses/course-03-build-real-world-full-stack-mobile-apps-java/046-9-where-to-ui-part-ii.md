---
title: "9. Where To UI - Part II"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 9
weight: 46
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube Lai--eYYJTw >}}

## Transcript

the where2 ui is a pretty big piece of
the puzzle and it also includes the
navigation ui
which is the bottom portion
up until now we focused on this part of
the toolbar area
now we need to do this portion the
destination ui toggle
is a huge part of the navigation ui
it's the bottom section of the form that
contains the list of destinations
we can build it on top of the code we
wrote for the navigation ui and place it
in the center
of the layer
so it will play nicely with the rest of
the ui
for now i'll ignore this portion
as it's mostly a specialization
of other things and can be done
relatively easily if you understand the
rest of the things i did
let's jump right into the show
navigation bar method
the top elements are relatively
simple multi buttons we use container as
their ui id so they will be transparent
with zero padding and margin
the separator
is just a label with a specific style
notice that blank label
buttons etc are hidden by default in
codename one
and you should invoke set show even if
blank
if you want such a label to still render
we can reuse the form ui id here
because that's effectively what we want
we want this ui to appear as if it's a
form
we need this to be scrollable but we
don't want the scroll bar on the side as
it might cause
aesthetic issues
the showing of this element
is animated from the bottom
of the form
while this ui is very simple
it did define a few ui ids
let's start with where to button line 1
which represents the entries in the list
of elements and also adds the underline
the color is just black over transparent
which in this case leads
to white
the padding on the left side is
relatively low since the icon will take
the extra padding on the other sides
we have typical four millimeter padding
margin is zero
as usual
we put the underline here because the
design placed the underline only under
the text and it will place it under the
icon which has a different ui id
the underline is a gray two pixel high
line
the font is the standard three
millimeter light font
the where to button icon style
applies to the icon which has
less horizontal padding so it won't
drift too far from the text
but identical vertical padding
so it will align properly with the text
it derives from where to button line one
so they will fit together well
we need the no border version of the
style
so it will remove the underline border
on the last entry
otherwise we can see
an out of place underline in that one
last entry in the list
we derive from the same style so
everything else is identical
the wear separator
is just a gray padded line so it has the
right gray color and is completely
opaque so
with no background transparency
it's exactly
two millimeters tall so it will stand
out but won't take out an entire line
margin is zero so it can reach the edge
of the parent container
now that we added this we need to show
this ui and hide it when the user
toggles the focus in the text fields
we can do this by binding focus
listeners
to the to and from text fields
when focus is lost or gained we toggle
between the square and circle modes by
setting the icon
to the appropriate labels
we always have one container in the
layer except for the case where the
second component is the where2 container
it's always the second component because
it's always added last
we set the position of this container
below the forms
animate and layout moves the component
outside of the screen to the position we
asked for
using a smooth animation
this callback is invoked when the
unlayout completes
at this point we have an invalid ui that
needs a layout but before we do that we
remove the component that we animated
out of the form
now
that the ui appears we also need to
remove it when going back
so i'll update the back action listener
from above to handle the where to ui as
well
this is the exact same unlayout
operation
we did before
and finally
we need to make a subtle but important
change to the background painter code
from before
because of the drop shadow a gap is
formed between the top and bottom pieces
so a special case here paints a white
rectangle under the shadow to hide the
gap
without that the shadow would appear
on top of the map and not on top of the
white background
once this is done
opening the where to ui
and toggling the fields should work
as expected
