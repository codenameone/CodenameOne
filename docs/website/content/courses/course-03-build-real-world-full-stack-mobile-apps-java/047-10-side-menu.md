---
title: "10. Side Menu"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 10
weight: 47
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube AFvuY7Ev-XA >}}

## Transcript

we are nearing the end of the mock-up
code
the next thing on the agenda is the side
menu ui
i've moved that code into a separate
hard-coded class for reuse in other
forms frankly i'm not sure if this ui is
used in other forms but i think it's a
good habit to separate the menu
from the component code
the code in the common code class which
i used for the black toolbar as well
it's a sort of util class that's really
convenient when you have repeating ui
elements
one thing you will notice is that there
just isn't all that much code here
the main reason is that
most of the ui is
within the theme and we already defined
most of these ui elements such as side
command uid
let's dive into the code
we'll discuss the get avatar method soon
this code generates the avatar image at
the top
with the name next to it
the gap between the text and the icon in
the avatar is larger than average
the legal button
is a south component
it's a special case in the on top side
menu that allows you to place an element
below the menu itself
its styling is separate and it slides in
out
so
we need to give it the psi navigation
panel styling to
let's move to the get avatar method
which generates the round image of the
user
we create an opaque 10 millimeter black
image
to use as a mask
masks allow us to crop out unwanted
pieces of an image in this case we want
to make the image round
we fill the shape we want in white in
this case as an arc
notice we activate anti-aliasing
otherwise the resulting image will look
jagged
which is also why we avoided
shape clipping here
the font image class can use the given
color and opacity settings
we use the version of the class that
accepts a style object and size so we'll
have fine grained control from code
we can't apply a mask to an image of a
size that's different from the mask size
masking doesn't work well with complex
images such as font images
so we convert it to a regular image
first
