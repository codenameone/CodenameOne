---
title: "2. Basic Setup"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 2
weight: 39
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube -brmZYVWb0Y >}}

## Transcript

let's jump into the code and styling
with a mock-up of the final application
but before we begin i'd like to start by
setting up some styles and basic utility
methods
i've created a new uber cn1 project and
placed it in the package
com.codname1.apps.uberclone
i gave the main class the name uberclone
as well
in the main form i'd like to highlight
three different lines
first we have the default gap between
the label text and icon
which is relatively large in the uber
wrap
so i've set it to two millimeters by
default
next
we lock the phone into portrait mode
this isn't the only thing we need to use
for this
finally we show the login form which
will get to in the next part
we need to install and configure some
cn1 lib extensions
we'll install more later but for the
first part we'll need sms activation
for the ui of the country picker
we also need google native maps for the
map ui support
don't forget to set up google maps in
the project as mentioned in the maps
module
as i mentioned before locking your in
orientation and code isn't enough for
ios
in ios we need to define orientation
lock in the project level
which we can do in the codename one
settings ios section
some styles are essential to begin with
so we need to add the following styles
into the theme
notice that a lot of these styles are a
result of trial and error to get the ui
to look like the designs
the process of choosing the values
boiled down to trying grabbing device
screenshots adjusting runes repeat
it sounds like a lot of work but it's
not too hard as you quickly get a sense
of what needs fixing
i define form as white
which is really the main thing here
on android by default they are a bit
off-white
i defined label as heavily padded with a
light font
black on white
this is consistent with the common use
of labels within the app
so i've set foreground to black and
background to white
i've set padding to a generous four
millimeters as padding is very heavy in
the uber
the margin is set to zero for almost all
of the components here
i use standard native light fonts for
almost everything as they look great
everywhere in this case i chose a 3.2
millimeter font
which seems to closely match the
dimensions of uber font choices
one important thing to mention is that i
used derive all on pretty much every
style in the theme
this works by right clicking a style and
selecting derive all
once you do that it creates styles for
selected pressed disabled
that derive from this style
that's a very useful starting point it's
important even for labels as they can be
used in lead components and we'd like
them to have a common base setting
i defined toolbar as transparent without
the border that exists on some platforms
notice that this doesn't handle them
consistent title issue
where some forms have a black title area
where others have a transparent white
title area
i will discuss those later
i've set the background to opaque white
just to be sure
i've also disabled the border of the
base toolbar by explicitly defining it
as empty
i defined title command
as black on white
this is a bit problematic with the black
toolbar which requires
a bit of a hack and code to work
the padding numbers are there to make
the collapsible toolbar possible
this collapse effect featured
in several forms such as the country's
form
margin is zero as usual
and the font is relatively large
four millimeters this is mostly used to
size
the back arrow icon
and the search icon
the side navigation panel is mostly
black on white and relatively clean so
i'll just define the background as
opaque white and ignored the black since
that's part of the command
we have an underline at the bottom to
separate the panel from the south
component below
so we need to reserve 2 pixels for it in
the margin
the padding is zero as usual
as spacing will come from the commands
not from the panel
the underline separator from the south
component is just an underlying gray
border with a thickness of two pixels
side command pretty much continues what
we started in side navigation panel
here we set the foreground to black on
transparent color
this will be useful with the black
toolbar where we will only change the
color to leave
to white but leave the transparency in
place
the padding of the style command
prevents duplicate padding when commands
are one on top of the other which is why
the bottom padding is so small
margin is zero as usual
and the font is again a standard size
light font
the text field in the uber app is based
on the material design simple underlined
text field even when running on ios
so we need the text field to have an
underlying border and work with black on
white
we define the ui as transparent with a
black foreground
the padding below is relatively low
too so the line won't be too far from
the text input
the left and right paddings are zero so
the text starts
will align with the line start
the margin serves the role we usually
use for padding
it spaces out the component
the underlying border is pretty simple a
black
two pixel border
however in the selected version of the
text field we have a four pixel version
of the same border to indicate selection
font is a standard three millimeter
light font
the text hint needs to align with the
text field
so it's important to override it when we
manipulate the text field
we use the same padding as text fields i
could have derived text field which
might have been better a better approach
but i didn't want to get into that
the margin is again identical to the one
in the text field
the font size is smaller and regular
instead of common light font
that looked closer to the choices uber
made
finally the floating action button which
is just white on black nothing else
with this we can move forward to
creating the mockup
although there are some additional
styles we'll define during the creation
itself
