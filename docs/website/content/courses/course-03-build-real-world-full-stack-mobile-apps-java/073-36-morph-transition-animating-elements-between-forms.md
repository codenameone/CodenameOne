---
title: "36. Morph Transition - Animating Elements Between Forms"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 36
weight: 73
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube 9T8MBBuWBDs >}}

## Transcript

i kept most of the default transitions
and did a few animations along the way
but i didn't spend too much time on
either one of those
by default codename one uses the slide
or slide fade transitions
these should look decent for the most
part but i wanted to demonstrate and
discuss
some of the nuanced transitions in the
uber app
[Music]
in the native uber app transitions look
a bit different between ios and android
i didn't go there because i don't think
this was done on purpose
in android's material design a common
transition pattern
is one where we move an element from one
view to the next and indeed
this is what we have between the login
form and the enter mobile number form
as you can see the enter your mobile
number and flag elements animate to the
place in the next form while other
elements fade in out respectively
this transition repeats itself in
reverse
when we press back
there are a couple of things that might
not be immediately obvious when you look
at this
the background pattern
instantly disappears
instead of fading
this might be on purpose but it doesn't
look good
this is a bit hard to see as it happens
relatively quickly but the back arrow
slides in from the left
codename one has a morph transition
which doesn't include the slide in out
option
for some elements
only the fade in out of these elements
so we'll pass on that aspect i chose to
fade the background pattern in out as it
looks much better
i'm not sure why uber chose not to do
that
notice that this works for us despite
the fact that the background is
constantly rotating
when we get back to the main form
and this isn't supported in the native
android app
transitions are decoupled from the forms
or components that they transition
this allows us to define a transition
regardless of the contents of a form in
order to use the morph transition
we need to communicate to it the
components we would like to animate
but they might not be instantiated at
this time yet
so we need to use component names
if the components on both forms have the
same name
we can make the code even shorter
we can perform the transition using this
code in login form
we will obviously need the corresponding
code in enter mobile number form
notice we set the names to the identical
values
we could have used different names and
then just specified those different
names in the morph method
we would also want morph to run in
reverse when going back
so the obvious thing to do
is define a morph transition in the back
command
but there are a couple of nuances
notice i used strings instead of get
name as the back command is defined
before the components in the full code
listing
this is one of those things that you
only see on the device the virtual
keyboard opens when we enter the mobile
number form
so when we go back it looks a bit weird
on android
i stop editing to fold it first
and then use the callback to detect when
the keyboard actually finished closing
otherwise the transition will run before
the form had time to adjust
the problem is that if you run this code
it will fail badly
the background animation tries to
repaint while the transition is in
progress
the solution for that is a small simple
change to login form painter
i'm effectively blocking animation of
the background during transition which
would also make the transition smoother
as a result
