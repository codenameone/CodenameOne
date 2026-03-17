---
title: "6. Theme CSS"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a WhatsApp Clone"
module_key: "14-creating-a-whatsapp-clone"
module_order: 14
lesson_order: 6
weight: 129
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 14: Creating a WhatsApp Clone


{{< youtube BbVoa3vw7OM >}}

## Transcript

now that we looked at the first ui form
let's take a short detour via the css
for the app
i chose to go with css as it's much
easier for tutorials
i can use
sources instead of multiple screenshots
to explain an idea
the first selector is a constants
selector it lets us define theme
constants
here we include two basic values
that most apps should include
include native ball indicates that we
want the theme to derive from the native
theme as a starting point
this is essential as it defines a lot of
important things such as the spacing on
top of ios devices etc
the scroll visible ball constant it
hides the scroll bars that might show in
some os's
pop-up dialog appears as an arrow
dialogue on in ios but other os's don't
implement it the pop-up dialog is used
in the sign-up process for picking the
country of sms verification
the background color of the pop-up is
white and implied implicitly
defined as opaque
we have a subtle rounded corners with a
radius of one millimeter on the dialog
you will notice zero margin as the
dialog touches the edges but two
millimeters in padding which space the
content a bit from its edges
label is pretty standard so i won't
spend too much time on that it's just a
two and a half millimeter light font
multiline 1 is used to describe the
first line of the multi button
there isn't much here just a light
slightly larger font
the one thing to notice is the zero
padding on the bottom so the first line
and the next line will be close together
multi-line 2 is pretty similar to
multiline one
only with a slightly smaller font
notice that the blue color of the text
is derived from the native theme and
isn't declared here
also also notice the one pixel grey
border at the bottom of the multi-line
multi-line three and four have zero
padding and margin
this removes potential error in spaces
from the list of buttons
the whole multi button is styled here to
have a white background with no margin
or padding
notice that border is explicitly defined
as none
this is important as the multi button
contains a line border and some native
themes
that would collide with the shorter
border we want in this case
the toolbar defines the background and
the top of the form we give it a one
pixel bottom border which is pretty
similar to the border defined in the
native app
it's very subtle
when styling the title we should usually
style the commands
as well so they have similar proportions
and alignment
subtitle represents the four buttons
below the title
they are similar but have an off-white
color
are slightly smaller and are center
center aligned
toggle buttons use
the press state to indicate selection
in which case we want them to be white
the subtitle underline is the animated
small thin white line below the buttons
this draws the full content of the
component
chat form uses the image as a background
image notice the source dpi 0 argument
which means we don't want to use multi
image for this for this specific case
this just sets the color of the floating
action button
we use the text field in the signup form
it's added by the sms activation cn1 lib
there's nothing special about it
however the chat text field is a
separate component it uses a pull border
to wrap its content
technically this is a container that
tracks the actual text field but it's
still perceived as a text field
the icons within the text field are just
simple font image icons with a specific
color and very little margin
the text field hint is just a great text
for the text field
and the record button has round border
similar to the floating action button
nothing special other than a bit of
padding and margin to position it
properly
chat time represents the hour next to
the message
within the chat bubble
it's just gray and smallish we make sure
it's transparent so it can work on white
or green backgrounds
the same goes for the chat text with
larger black font it's still transparent
for the same reason
the bubble of the chat is implemented in
code as
a custom border
i thought about using a nine piece
border and this seemed like a better
option
i use margin on the right side to
prevent the bubble from going too deep
into the other side this seems close to
what whatsapp is doing in their app
the same is true for chat bubbles right
with a different color and direction of
the margin which is now spaced on the
left side
the command list is the style of the
overflow menu
i use box shadow which unfortunately
produces a nine piece image border
hopefully the css support will improve
and use round wrecked border for this in
the future
notice there is a subtle border radius
css attribute that rounds the corners of
this dialog
each entry here is a command they have a
simple two milliliter millimeter padding
with light font style
the last style is day
it maps to the day label making chats in
a specific marking chat in a specific
date we use the pull border with a
bluish color with that the css is done
