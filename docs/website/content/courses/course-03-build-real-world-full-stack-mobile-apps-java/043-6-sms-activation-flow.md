---
title: "6. SMS Activation Flow"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 6
weight: 43
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube cRqvkIpJlkg >}}

## Transcript

in this part we'll go into the sms
activation flow
the first form in the sms activation
flow is the enter mobile number form
it's a simple form even though there are
some interesting subtle features here
the cool thing is that we did almost all
of the work for this element already
Text Fields
let's jump right into the code that
makes that form
we'll use standard back navigation since
the toolbar is pretty standard here
the phone number text field is right
next to the country code button
we place it in the center of the border
layout so it will take up all available
space
i want the padding on the text field and
button to match so they align properly
once paddings are set they are always in
pixels so we need to change the style to
use pixels
i don't want to impact the left right
padding values so i extract them first
and save them so i can restore them into
the ui
i could technically create a separate ui
id to align both but i wanted to do this
in the code so future changes to the
theme don't break alignment
just so you'll get a sense of why this
exists
this screenshot shows
side by side how this looks with and
without the alignment code
guess
which is the right one
you can start editing a text field by
invoking start editing
however this is a bit more challenging
to do
with a form that isn't showing yet so we
have a special case for that
set edit on show
and this is pretty much it for this form
SMS Verification
once the number is entered
we move to the sms verification stage or
password entry stage
in this case
i've hard coded the sms verification
stage i didn't do the sms resend
countdown
but i did do the number input
notice that the text fields look like
android text fields but have a sort of
center alignment structure
also notice that the error mode spans
the four text elements
let's jump into the code and look at how
this was done
Digits Form
the enter sms verification digits form
is a bit of a mouthful but it describes
the function of the form rather well
let's go over this form line by line
we use a border layout and place a box
in the center
which we make scrollable on the y axis
the reason for the border layout is so
we can stick the countdown label in the
south
otherwise i would have used
box layout for the entire form
notice i set the container to be
scrollable on the y-axis this is
important for containers where we have
text input it allows our keyboard code
to resize the container properly
when the keyboard shows
i'd like to also point out that i used
the standard back command
in the toolbar
we create an array of text fields to
loop over this allows us to easily
change the code to accept six digits
i'll discuss the create digits method
soon
yes
this works it adds all the components
and the array so it will add the four
digit text fields
the error label
is always there
we just hide it
for now i don't animate the recent text
again notice that i use board layout to
position the recent label at the bottom
and place the rest of the stuff in a box
layout in the center
when the floating action button is
pressed we validate the input so we can
decide whether to show an error or
proceed
the generic creation code creates the
array of numeric text fields and aligns
the hints to the center
this logic makes sure that once we type
a character the input will automatically
move to the next text field
in case of an error
we just change the underline style
we could have also done this by invoking
set ui id which might have been more
elegant
we bind a listener to each text field
and if the length of the text is 1
we stop editing and move to the next
text field
and this is pretty much it with the
exception of the styles we had to add to
make this happen
Digits Style
the digit style is a special case of
text field specifically designed for
this form
the main reason for a special style is
the problematic center alignment and
text field
because of the way this works i
preferred using a one millimeter padding
on the sides to give the feel of center
alignment in this case
center alignment works in text area
label etc however it's flaky in text
fields because it's really hard to get
the position right when moving from
lightweight to native editing
another important bit
is the smaller margin that makes the
fields stand closer to one another
Selected Style
as i mentioned before since this is a
specialization of text field we derive
from text field the text field class
one thing to notice
is that the selected style
we need to override the border as well
to implement a
4 pixel underline border
it's because we derived from the
unselected digit and not from the
selected version of text field so we
need to redefine how selected digit
entry looks
however we also override the font size
to make it slightly smaller and thus
more consistent with the native uber app
the error label is just a red on white
label
it has a bit of a smaller padding
so it can use up space
it still has zero margin like most
components
but it has a smaller light font at 2.8
millimeters
which is more consistent with the
material design aesthetic
the recent code style
just pads the text so it will align
properly with the floating action button
it leaves margin as 0 by default
but it has smaller text size than a
typical label
Password Entry
the last form in the sms activation flow
is the password entry form
it's a trivial form after the others
we've been through
here i'll gloss over it relatively
quickly
this is the entire form code literally
in one page
after the activation
form there is literally nothing new or
interesting here the only aspect that's
here and wasn't there is the forget
password uiid
which we align with the floating action
button
in this case we have
two elements that we enclose in a box
layout y in the south
most of the work here is in the ui id
itself
the forget password buttons
have a bluish color and are transparent
the padding is carefully measured to
align properly with the floating action
button
margin is zero as usual
the font is a relatively small 2.5
millimeter size
and that concludes the sms activation ui
flow mockup
