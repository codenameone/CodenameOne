---
title: "3. Login and Country Code"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 3
weight: 40
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube Cc8YG-_M02M >}}

## Transcript

in this part we'll create a mock-up for
some of the forms starting with the
login process
we'll start with the login form
since the uber app is portrait locked it
should be pretty easy to produce this ui
however
we'll first start with one element and
that's the country button
the reason it's logistically separate
is that it has some non-trivial logic
and resides in two separate forms
so it makes sense to define it
in a single generic class
the country code picker
is a button subclass
but it doesn't really look like it
because we set the uid
to the country code picker ui id
in this block of code we try to guess
the current country based on the
localization settings
the flags.res file is included in the
sms activation cn1 lib it's a bit of an
implementation detail so this code might
break in the future
if so we might need to copy the res file
from there or update the code
we don't have all the flags for all the
countries
without a blank icon
the alignment might seem broken
so it's crucial to have
this show picker form
is useful for overriding
in the login form clicking this button
should lead to a different form
however
there is a lot more going on here the
cover transition
collides with the default slide
transition producing a weird effect
so here we keep the transition instance
remove the out transition so the cover
effect will work properly
we then bind a show listener that
restores the old transition
after we are done
that's important for when the user will
click to move to the next form
the styling for country code picker
are pretty simple
it's black on transparent
background the padding is big on the
left but small on the right so the text
element stays near the button
margin is again zero
and the font is pretty standard
light three millimeter font
so without further ado let's go to the
login form code
this is the code of the first form you
will see when the uber app launches
it's a relatively simple class without
too many frills
let's go over a few elements of note
initially i wrote the word uber without
the right font it looked weird
using an image for a logo is generally
the best approach
i want the logo to be square
so
height and width should be identical
we place the entire tile section and
logo
in the center of the form so they will
take up the available space
we place the logo itself
in the absolute center so it will float
in the middle
i override the behavior of the country
picker button
for consistency with the native uber app
this looks like a text field but acts
like a button
in the native app so i implemented it as
such
considering the rows and size
is important for proper layout
the rest of the ui is relegated to the
south of the form
this would have issues in landscape mode
but since the app is portray clocked
this shouldn't be a problem
we need
these two images to complete the form ui
the tile png
and uberlogo.png
there are a few styles we need to define
in order to finish this form
the square logo ui id
is used for the logo
i still have the foreground defined as
this is
this used to be text and not an image
the main thing here
are the white opaque background
we use some white padding on the logo
but we don't need margin
the logo background style represents the
pattern tile
in behind the logo
this is pretty easy to accomplish
once we have the tile.png file
we can style it to tile on both asus
we set the transparency to 255
as the image is opaque and we want to
make sure
this is totally opaque
we define the margin to zero as usual
notice we ignore padding as it just
doesn't matter for this component
the get moving with uber ui id just sets
the padding to a right size so it's
spaced enough from the sides but close
enough to the element below
we don't need margin color or anything
else because we derive from label
the main reason for this ui id
is the large dominating 4.8 millimeter
font size
the phone number hint represents the
text that looks like a hint next to the
flag
when we move to the next form it
actually becomes the real hint text
it's gray with no background
it has zero padding on the left to keep
it close to the country picker button
the font is 3.7 millimeters which looked
right after some trial and error
the separator ui id is a container
which has an underline below it
as a container we define it as a as
completely transparent
it has a two pixel bottom padding to
leave space for the underline
the margin is zero as usual
the border is an underlying two pixel
border in a gray color
the connect with social button is the
button at the bottom of the ui it looks
like a label but has a bluish color
i need to define the padding even though
it should be derived from label as
deriving from built-in types isn't
always 100 reliable
i could have worked around it by
defining a my label uiid and deriving
from that
i still chose to derive from label
which mostly works
but i define the font
just to be on the safe side
this is the ui we've made next to the
native uber ui you will notice some
minor differences mostly with fonts not
being pixel perfect
i didn't aim for perfection with fonts
as that can be endless
there are some other nuances i'll go
into
