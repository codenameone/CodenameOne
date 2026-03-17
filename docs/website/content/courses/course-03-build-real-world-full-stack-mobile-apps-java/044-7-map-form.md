---
title: "7. Map Form"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 7
weight: 44
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube UU6HCbenVAA >}}

## Transcript

in this section we finally get to the
map ui
i hope you followed the instructions
before for configuring the map
if not
please follow through with that if
things don't work and if you don't see
the map or it acts funny check out with
us in the online support forums
you can also check out the map section
in the deep dive into mobile development
course which comes bundled
before we begin
we need a class we discussed before in
the maps module
the map layout
i didn't change much as
left it as is
since it's already available elsewhere i
won't go into the full discussion here
and go right into the form itself
before we proceed i'd like to highlight
some subtle things in this screenshot
and discuss what we will and will not do
i won't do the side menu right now
but i will do it soon
the way to field is really a button that
leads to a different ui
this ui is overlaid on top of the map so
it is a part of this form
i'll position
one taxi in a hard-coded location as
part of the mock-up
the icons at the bottom are historic
rides i'll add two hard-coded historic
rides for now
i won't go into the notice at the bottom
it's it's possible
but it would be non-trivial
let's jump right into the code
you need the js key from google maps as
explained in the map extension page
this
must have a filled up value
we usually use border layout which
implicitly disables scrollability
layout layout doesn't do that
and the form's content pane is
scrollable on the y-axis by default
notice we didn't use a thread here
and instead used call serially on idle
method
on the login form i didn't use that
because the animation might have
prevented idle from occurring
this shadow is used later on in the show
navigation toolbar method
the transition and the main application
are based on cover and the transition
out will only be a problem
the map is on the lowest layer and
everything is placed on top of it
the layer is on top of the map and uses
the map layout
here we will place the car and other
landmarks we need
i place a car on top of the map in tel
aviv
notice that the car is just a label
i've set the opacity to 140 to match the
translucent cars in the native app
notice that the map layout takes a quart
as constraint
so it can properly position the car
this is the small square we place next
to the where to button
i could have used a unicode value too
but it wasn't available in all the fonts
notice the where to element is just a
button as it moves us to a separate ui
and isn't really a text field
the history buttons are floating action
button instances
that are customized in terms of styling
i used text area instead of span label
because i wanted the history element to
act as a single component with lead
component
lead components can take over a
hierarchy of several components and
handle the events for everyone so in
this case click on the text area below
the history will trigger an event in the
floating action button
the bottom of the map has a gradient
overlay that darkens the bottom
this is probably in place to make the
history labels readable
i just generated a gradient image in
photoshop and placed it here
we do
two important things here
we use the overlay toolbar which floats
on top of the ui
we initialize the side menu
which we will discuss soon
that was a lot to cover
but there is a lot more
we did mention three new styles above
which isn't that much all things
considered
the wear 2 style has some subtle nuances
such as
dark gray text
the padding is large and obvious i
played with it a bit to get it right
the margin is special we want some
margin from the sides so it won't touch
them
we need a lot of margin from the top to
leave room for the title area
the corners are rounded on the native
widget this is very subtle so i left it
at 0.3 millimeters which should be very
easy
it also has a shadow which is more
obvious
and 80 opacity
the font isn't big just slightly bigger
than normal
but the typical light font
the history button is the round button
on the bottom of the map leading to
historic rides it's black on white but
is implemented as a floating action
button
so it derives from that
and uses the border settings from the
floating action button
the
the history label is the dark text
element below which is technically a
text area but acts as a label
the text color for this is black
while the padding is two millimeters on
all sides except for the top where we
want to be as close as possible to the
floating action button which is already
well padded
margin is zero as usual
and the font is a relatively small 2.2
millimeters so we can fit multiple rides
in one form
