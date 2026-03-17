---
title: "5. Social Login and Country Picker"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 5
weight: 42
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube gM-InTtdhVE >}}

## Transcript

in this part we'll finally leave the
confines of the login form
and move to the simple forms of social
login and the country picker ui
i'll start with the social login markup
since it's so small and simple
this is the form you see when you choose
the social login option
i crop the bottom as it's just white
this is a pretty trivial ui
there isn't that much to say about this
form it's trivial
we use two icons for facebook and google
and define the back arrow
thanks to all the theming work we did up
to this point everything just works and
looks like the uber app
we do need to define the flag button
though
as it's a ui id that i reused in several
forms
it got the name from the country's
picker form which we'll go into next
the flag button is just a label in terms
of padding etc
it has a sub
subtly larger font at 3.2 millimeters
not much larger
the country picker form
lists the countries next to their flags
the first letter of the country name is
highlighted between the countries and
when you scroll down the title area
collapses to make more room
it does that with a smooth animation
effect
speaking of the title area it's white on
black instead of black on white
we reach this form when we click the
country picker button but that only
happens in the phone number entry form
which we will discuss shortly
let's jump right into the code
predictably the form is a box layout
container on the y axis
the init black title form method is a
static method in a common utility class
we'll cover it soon
we don't have flags for all the
countries so we need a blank space icon
so the elements align
here we loop over all the country codes
and create a button with the flag
letter ui id for every entry we also
need to implement the alphabet letter
headers
every time the first character of a
country changes we add a label
representing the entry
when an entry is selected
we update the text and icon of the
country code pickup button that launched
this form
we need to override the toolbar
initialization so we can set the proper
black toolbar ui
id speaking of the black toolbar
it is predictably styled as black and
opaque
we have a one millimeter padding which
doesn't exist in the default toolbar
it's helpful for the collapse animation
effect so
padding still remains
the margin is zero as usual
flag's letter is the letter that appears
on top of every
letter change between country names
the colors
and the opacity are things are picked
from the screenshot image
the other aspects of this ui are derived
from label
the form with the black title requires
some work which should be more generic
as a black title area is used in several
places within the uber application
for this purpose we have the common code
class which stores common static code in
the application
this is a non-trivial task as the logic
needs to support animated collapse of
the title area as the user scrolls down
the method accepts a callback for the
case of a search operation
this isn't implemented yet
but
if it's null
a search icon isn't added
we add the
back command
as a button
which allows us to place it above the
title in a custom way and animate the
position
we can't use the title command uiid
as is
since it uses a black on white scheme
in other forms
i could have used a different ui id here
if we have a search callback
i build the layout that includes the
search
button otherwise i create a layout
without it
i place the title on top of the back
button container using a layered layout
it doesn't seem to be on top because
i've set the top margin so it resides
below the black
arrow icon
i did this so i can animate the position
of the label fluidly by changing the
margin value
the this one line allows the title to
collapse into place next to the arrow it
translates the style of the title which
currently has a large top margin
to one without top margin and with side
margin
this means that the change in the style
causes the title to move next to the
back arrow
cover transition is used in the back
title form on ios
notice that cover transitions expect in
and out values for cover and uncover
the white on black title is a white
title style
and as the name suggests it has white
foreground and a transparent background
as the black portion comes from the
black toolbar ui id
the padding is pretty standard these
numbers were picked to align properly
with the commands both in expanded and
collapsed states
the margin is actually zero
as we change this manually and code it
might make sense to do the margin here
for some cases
the font is
standard light font
but four millimeters in size which
should appear bigger but not huge
subtle
the left margin version of the style
does define the side margin to leave
room for the arrow this means that the
collapse animation will mutate
into this ui id which has no top margin
so it will effectively align with the
back arrow
however the left margin will keep it
from going on
on top of the arrow
it also evens out the padding so things
look more aligned
now that they are on the same row
derive the white on black title ui id so
the settings
we don't override are identical
the font is the same but a smaller three
millimeter size
this will also animate as the title
slides into place it's subtle but
noticeable
