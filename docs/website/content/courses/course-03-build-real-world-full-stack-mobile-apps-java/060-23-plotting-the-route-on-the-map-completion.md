---
title: "23. Plotting the Route On the Map - Completion"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 23
weight: 60
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube aW3OlGTmasw >}}

## Transcript

finally after all the buildup let's draw
the path on the map form
we've had
quite a bit of code and it brought us to
the point where the navigation ui should
function as expected
this is implemented in the enter
navigation mode method
which i mentioned before
as you might recall it it's invoked when
clicking an entry in the search results
this method is invoked from a callback
on navigation
we have the coordinates of the path to
display on the map
as the arguments to the method as you
may recall these coordinates are
returned by the directions
method the first thing we do is remove
the existing search ui from the form
and animate it out
due to the way events are chained this
method can be invoked more than once in
some unique cases
this works around that behavior
we convert the path to an array and add
it to the map
this uses native path
plotting for these coordinates
i could have used a painter or something
similar
and might still use it later on
i move the camera to show the entire
path within the form
i create the two tags and add them to
the ui
notice that the from tag has a right
alignment
also notice i used the trimmed string
method to limit the string length
i'll cover that method soon
the back behavior is just a button style
to look like a command it invokes the
exit navigation mode method which we
will get to shortly
this is the ui to approve the taxi we
are ordering
i used a white container with the form
uiid on the south portion of the form
as a layer
Trimming
before i continue
i also use the trimmed string method in
the code before to trim the tag
components
there isn't much to say about this
method we rely on the fact that
addresses usually have a comma after
them
if the string is missing that or is too
long we have special cases for those
this guarantees a string of decent
length
for the tag elements
Exit Navigation
the one last missing piece of the code
is the exit navigation mode call
which just removes all the elements and
sets the invisible pieces back to
visible
it's pretty trivial
UI
we also have a few ui ids of note
mentioned in the code
the first is ride title which is the
title area for the ride ui it's pretty
much a label so it has black over
transparent colors
but
it's centered
it has the same padding as a label
zero margin
and same font
margin separator is a separator that has
marginal margins on the side
which we use in the ride dialog
other separators in the app
reach the edge of their parent container
it has a couple of pixels of padding in
the bottom
to leave room for the separator
it has the margin to keep it away from
the edges of the parent container
and it features a standard underline
border
the black button
is just a standard white over black
button
with center text alignment as is common
with buttons
it's got some padding but not too much
so it won't look huge
it's got some margin so it won't
literally touch
the things next to it
and to compensate over smaller padding
it uses a subtool round wrecked effect
that just
is just barely noticeable at 0.2
millimeters
and it's got a slightly
larger regular font instead of the
typical smaller light font
once all of this is done we can just see
navigation work and appear on the map as
expected
