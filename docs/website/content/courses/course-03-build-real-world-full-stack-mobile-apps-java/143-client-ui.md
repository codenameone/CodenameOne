---
title: "Client UI"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Create a Netflix Clone"
module_key: "15-create-a-netflix-clone"
module_order: 15
lesson_order: 6
weight: 143
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 15: Create a Netflix Clone


{{< youtube nImSppBdgkY >}}

## Transcript

for this final part we'll cover the ui
of the client which is relatively simple
we don't have that many forms but to be
fair the basic netflix app doesn't have
too many forms either
so this isn't too different from the
original
the one special thing we do here is use
the layered toolbar for the ui which i
will discuss soon enough
css is used for the design i'll
introduce the applicable css
incrementally when introducing a
specific uh ui id
i won't go over the entire css and will
instead go back to it when introducing a
specific ui id
i will cover
the gener generic concepts first though
we have only two constants in the css
the first is the include native feature
which should be on by default always
the second is a standard label gap with
between the label and the icon i think
one millimeter is generally a good
number here
we don't have too much in common between
the forms at this level
of the ui so there is only one method in
common
the init global toolbar method
initializes the toolbar component when
global toolbar is turned on
which is the default
we do two things here we set the toolbar
to use the layered mode we do that by
passing true to the toolbar constructor
next we set the ui id of the toolbar to
toolbar gradient which we use to
indicate the translucent gradient
background to
separate the toolbar from the content
the toolbar gradient is a gradient in
black between 0.6 alpha to almost clear
alpha
this creates a slight fade effect over
the title area so the title will still
be visible if the image in the
background has the same colors
condemned doesn't currently support
alpha gradients as a solution the css
support generates an image of the
gradient during build and uses that
the splash form is
stupid simple
we just place the logo in the center of
the form initialize there is only one
thing to discuss here and that's the
source of the netflix logo png file
all images are declared in the css
under two dummy ui ids specifically
image imports 1 and image import 2.
let's just pause all the images into the
resource file so we can later make use
of them
the source dpi is the reason we have two
two image import ui ids
when source dpi is set to zero it means
we want the image imported as is
not as a multi image but rather as a
single image
the logo is the only image where we're
interested in a multi-image behavior
since most images come from the server
we don't
need many multi images in the app
the home form is the main ui of the
application
listing the content that the user can
select from
it's a base form which means the title
of the form is overlaid on the content
and it uses border layout
i'll skip to the bottom first
this class is created using a create
method that returns the class instance
it accepts the content information
as the argument to the build to build
the ui
the actual implementation of the layout
is in the init method we see above
the edit method creates the entire ui
it starts with the logo title we can see
here
that matches the same image we see in
the splash screen that's mostly laziness
on my part
but isn't
too far off from the actual netflix ui
by adding a command to the side menu the
hamburger menu appears automatically i
didn't want to go into the design and
implementation of the side menu so i
left this effectively blank
i also added a search command which is
again blank since i didn't implement
that technically i just used that for
the icon
i could have just used add material
command to right bar but that would have
required a slightly longer line of code
so i chose this approach
the main ui has a logo image here which
is different from the background hero
shot
now you might be thinking why not have
the logo as a part of the background
hero shot why do we need a separate
image for the logo
two reasons we want the logo to appear
above the play button exactly if it's a
part of the background image we won't be
able to tell where that is
we want the ability to scale the
background and foreground image
differently in the background we want a
scale to fill so the ui will look good
in all resolutions for the foreground we
want a scale to fit behavior so the logo
text will always be visible regardless
of the device resolution
we set the uid for the series logo this
impacts the following css
the margin and padding push the logo to
the right location in the middle
with the right amount of spacing and the
background is defined as transparent so
the background image will be visible
through the logo
the play button looks like this again
most of the work is done in the css for
the button
we use a 1.5 millimeter round border
with a gray background and black
foreground for the text slash icon
the background image comes dynamically
from the server so we can't set it from
css
we create a box layout with the logo
play button and the popular on netflix
label
we then set the background image
dynamically using the style object
the lead ui id is a special case with a
dark gradient background it's overlaid
on the title image and needs that
gradient to be visible on all image
backgrounds
the tabs are set to appear at the bottom
explicitly to avoid top android style
tabs
i could have defined this in the theme
constants but chose to do it in the code
in this case
the lead ui is a special case with a
dark gradient background it's overlaid
on the title image and needs that
gradient to be visible on all image
backgrounds
each list below is created via the movie
list method
they have a lead label
top and reside within a scrollable
container so we can scroll through them
let's look at the
movie list method
here i create a box x container that's
scrollable on the x-axis every element
is a scalable image button that uses the
thumb icon uiid
when pressed we show the details form
finally the tabs themselves are added to
the bottom of the form
thanks for watching i hope you enjoyed
this course
and found it educational
