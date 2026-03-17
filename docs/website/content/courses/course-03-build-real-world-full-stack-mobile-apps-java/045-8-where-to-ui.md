---
title: "8. Where To UI"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 8
weight: 45
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube -7XHkBMK4NY >}}

## Transcript

when you tap the where to button on the
map form you see something that might
look like a new form but it isn't really
what you see initially is this and it
seems like a new form
but notice that the focus is on the
where to text field
if we switch focus to the where from
text field on top you will see something
else
you will see the map and the ability to
type in a new location
so what we really have here are two
separate overlays on top of the map
one above and one below
there is another septal behavior that i
only noticed when i started playing with
this ui
notice the line and the shapes next to
the text field
when you move the focus between the
fields the shapes
flip to highlight the focused field
we could build something like this with
a dialog or interaction dialog but i
chose to go with simpler container
instances on top of the map
to do this i first had to add a listener
to the where to
button then i add the show navigation
button method
let's dive into this method
we create a new layer on top of the
current layers
in the form
layers are associated with a component
class
which allows us to keep it unique and
prevents different code from messing
with our layer
also notice that we replicate the look
of the title area without actually
creating a title area
the square image already exists from
before
we created it for the where to button
we add a new circle image that we can
place next to the from two fields
we place the text fields in a border
layout next to the labels representing
the circle and square
we place that in a box layout y
container and that's effectively the
entire ui of the top portion
the background painter allows us to
control the shadow from the top area and
draw the line between the circle
square images
the fact that we have a background
painter makes some of the aspects of the
ui id less significant
for instance background color
but we still need it for padding margin
etc
the shadow image
is created asynchronously
by the call serially on idle code and
the constructor
so it might might not be ready when this
is drawn
we fill the rectangle on top of the drop
shadow
covering half of it
this makes it feel like a directional
shadow
i used fill rect instead of draw line to
make a 2 pixel wide line
i could have used draw line with stroke
but this is simpler and probably faster
the entire layer uses border layout
north makes sense for this as we wanted
to span the width but remain at
preferred height in the north
we'll use the center for the rest of the
ui soon
the component animates down from the top
with animate layout we pre position the
component location above
the from so animate layout will slide
everything
from the right point
this ui requires three new styles
first is
the wear toolbar
which is an opaque white container
we have five millimeter padding on the
bottom for the shadow of the container
and as usual the
zero margin
the from two text field is opaque with a
grayish color background and black
foreground
it has
two millimeters of padding
and two millimeters of margin to keep it
spaced
it uses a standard light font
the component also has a selected
version
which has
slightly darker grayish color
it derives from the unselected version
of the
uid
we also have a custom ui id which for
the most part just uses a darker gray
color for the hint text
the margin is zero again
most everything else is derived from the
from to text field
