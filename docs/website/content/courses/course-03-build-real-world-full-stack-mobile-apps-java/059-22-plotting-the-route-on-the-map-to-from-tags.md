---
title: "22. Plotting the Route On the Map - to/from Tags"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 22
weight: 59
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube vjTexRDCihA >}}

## Transcript

with that out of the way search should
work
but how does it display the result
for that we need to add additional
features to the map form that address
these capabilities
before we go into that
let's check out what that means
Ride Booking UI
the ride booking ui plots the path of
the ride on the map
and includes two tags one pointing at
your current location and one on the
destination the first tag is divided to
a time indication and name
the latter only contains the name
notice that the elements have rounded
rectangle shape
with a pointy end on one side to point
at the position within the path
Tags
let's start with creating these tags
the tag code itself is trivial it's just
a label or a container with some details
nothing special
except for one important detail
the black and white border
in order to implement the unique shape
of the tag i created a new border class
notice i used the preferred width of the
west component to determine the black
section
this is done in the black line position
method
before we go any further i'd like to
make one point clear
i would have used a nine piece border if
this were a real application i would
have just cut a nine piece border and
moved on
however
since the point is teaching i chose to
do this the hard way
UI IDs
before we get into the black and white
border
there are a few ui ids we need to define
the navigation label uid
is the black on white label that appears
in the tag
the only unique thing about it
is the relatively small amount of
padding
so the tag doesn't take up too much
space
the margin is predictably zero
and the font is slightly smaller than
usual
the navigation minute label ui id
is the white on black minute value
it's center aligned
it has zero padding below to keep the
text and number close together
but has similar padding on the other
side either sides to match the
navigation label
it has zero margin
and it has a smaller font size than
usual although not a tiny font
the navigation minute disk label uid
is used for the text below that the text
with the word min
it derives from navigation minute label
and has an even smaller font size than
that
Border
that these are out of the way
let's take a look at the border
notice we can just subclass the border
class just like we can implement
painters etc
this provides a similar path for
customization but is sometimes more
flexible
most of this code is based on the
built-in
round wrecked border class
drawing this type of border is pretty
expensive
so we draw onto an image and place that
image in cache within the component
using put client property
we use this value
as the key
the black line position is used in the
version of this border that's partially
black
here we create the border image that we
will cache for the given component
since a shadow is set on the border and
that can take up some processing power
to speed this up we have two versions of
the method
fast and slow we call the fast one and
invoke the slow one asynchronously to
update the border
we do a shadow effect by drawing a
gradient with varying alpha degrees then
blurring that out
if we have a cached version of the
border image we will just use that as
the background of the component
assuming the size of the component
didn't change
otherwise we create that image and
update it later
with the slower version that includes
the gradient shadow effect
we create the shape of the component
if it's the one with the black line we
place the corner in the top right
otherwise we place it in the bottom left
we can now fill out the shape as part of
the image creation code
if we have a black line we do the fill
operation twice with different clip
sizes to create that effect
