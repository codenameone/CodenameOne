---
title: "4. Login Shadow and Rotation"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 4
weight: 41
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube owhInk5YAtg >}}

## Transcript

in this part we'll refine the login form
and go into low-level graphics for
animation and background
this is how i finished the previous part
i discussed the fonts but there are
other differences
obviously the flag is slightly different
since i used our own resource file
but there are two other noticeable
differences
the first is the drop shadow behind the
logo which is missing
the second is the background rotation of
the pattern which is ios specific in the
native app but i don't see a reason for
that
i'd like to have it on android too
the simplest thing to do is generate a
square image of the logo
that already has a translucent shadow
within
this would be pretty trivial to anyone
versed in photoshop and would look great
on the device
however my goal is to teach programming
not photoshop
so i'm picking the hard way of solving
this
the effects class in codename one allows
us to create a shadow image for the
given dimensions or image
since the logo is square we can just use
the dimensions approach
the method accepts the size of the
shadow
the blur radius which means how far it
should go out of the size limits
and the opacity
as a value between
0 and 1.
so now we have an image
of the shadow
but the logo image and the background
are already fixed so we need something
new
instead of using the logo as it is
we place the shadow in a layer below
using the layered layout
and this will produce the desired effect
with one huge caveat
it's really slow
shadows are computationally slow
we use
gaussian blur to generate shadows and
that's a very slow algorithm
the solution is to move that code
offline the ui will appear and the
shadow will appear a second later
when it's ready the placeholder is there
so we can put the shadow into place
when it's ready
when the shadow image is ready
we replace it on the edt with the new
shadow label
the label uses the container ui id
which is always transparent with zero
padding
and zero margin
the android version of uber doesn't
include the rotation animation for
reasons that are just unclear to me
i think it might collide with some of
the material design transitions or some
other problem
it works nicely on all os's with the way
i implemented it
i could just rotate the tiles like
the one pictured above and call it a day
the effect would look decent and perform
well
however i wanted better control
and in order to get that i need shapes
shapes allow us to draw arbitrary
vectors curves
in a performant way
since this is effectively a vector api
rotation and scaling don't distort the
result
in order to use this api i need to use
the low level graphics api
and the background painter
we can set the painter for the logo
object using this code
notice that normally we don't need a
reference to the parent component logo
but in this case we need it for the
animation
i'll go into that soon
but first i'd like to say a few words
about painters
styling can only go so far
if you want to customize the background
of a component in a completely custom
way
you can use the painter api to define
the actual rendering of the background
this overrides all style rendering and
provides you with a graphic object
you can use for drawing
notice that the graphics api is a low
level api and might have platform
specific behaviors that aren't as
refined as the component style apis
it's harder to optimize low-level
graphics code so use it with caution
now that we got this out of the way
let's look at the painter code itself
this is the rotation angle in degrees
we increment this as part of the
animation logic
this is the shape object representing
the background pattern
we draw it or stroke it like a rubber
stamp
the constructor and the draw shape
method
create the pattern shape that we stroke
later
this code happens once to generate the
lines
and we then color them later on
the register animated method of form is
needed for low-level animations
it triggers invocations of the animate
method with every
edt tick
so we can update the animation state
in this case we change the rotation
angle with every tick
the draw shape method
adds logical lines and quads to the
given path
a quad means quadratic curve to the
given position you can see three methods
used on the path element
move two
moves the virtual pen in the air without
drawing anything to a starting point
line two draws a line from the last
position of the pen to the given
position
quad 2
draws a quadratic curve
bezier curve to the given position
through the given curve position
the paint method
is the callback from the painter
we fill the background rotate the
graphics context and draw the shapes
notice we just invoke draw shape and it
draws with the current alpha and color
in place
the low level animation code invokes
animate at fixed intervals based on edt
heartbeats
normally you would return true to
trigger a repaint but here i only want
to repaint a specific component
notice that i only change the angle and
move every
other frame to conserve cpu
also notice i rotate by 0.1 degrees
which creates a very smooth slow and
subtle
rotation
this paint method belongs to the
animation interface
we don't need it as we always return
force
once all of this is done the login ui
rotates in the background slowly and
smoothly a shadow appears after a second
and the ui looks in my opinion as good
as the native ui
