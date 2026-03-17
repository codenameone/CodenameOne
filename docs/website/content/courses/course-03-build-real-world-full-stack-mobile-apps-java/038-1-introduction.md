---
title: "1. Introduction"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 1
weight: 38
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube xCEOuV43Uqw >}}

## Transcript

Uber is a big influence on the mobile
market
and a lot of us try to replicate their
functionality and Design
in this module I'll try to create an
Uber application clone
but let's discuss a bit what that means
let's start by setting expectations in
place
I'm not going to build everything in the
app as there are so many nuances and
details within the final app that it
would be impossible to go through
everything
however
I will build most of the big ticket UI
elements and I'll actually try and focus
on the hard stuff rather than doing
things that are mostly simple
I chose to clone the existing app rather
than building my own since I want to
show a professional grade application
and Uber is pretty much that
the goal of this module is to teach the
theory of building a professional app
it's not there for the purpose of
rebuilding Uber
are you string boot as usual for the
server but I'll try to keep it bare
it's actually better to do stuff in the
server in real world scenarios but in
this case I want to focus on client
development
I'm trying to create a close clone but
not an identical clone
during that last bit between identical
and pretty close is a huge amount of
effort that doesn't provide a benefit
in fact it makes things worse because it
forces the code and designed to be more
convoluted
I will spend a lot of time in the map UI
and try to explain how to build a decent
GIS application
this isn't a GIS tutorial though as I'm
not an expert in that field
I took some shortcuts in building this
app so hopefully they don't show much
before we start we need to essentially
understand the functionality of uber
even if you use the app in the past a
lot of the functionality is pretty
subtle and you might have run through it
without noticing
we need to grab screenshots of uber
features that we can review and compare
to what we are trying to implement
once we have those images we can use
them to create a mock-up of the Uber UI
once the mock-up is in place we can
create a server and then connect the
whole thing together
and fill in the details as we move along
this is pretty similar to the process I
used to build all the apps and the
course as you will notice I prefer
building the UI first and think it's
always the best approach
when we finish the app it should be
fully working at but you will probably
need some work in order to bring it to
production grade I'll try to highlight
the bits that are necessary as we move
through the UI
there are some great transition
animations in the Uber app which you can
see here
I'll go into some details of how we can
achieve some of them near the end of the
module
we'll start by focusing on the basic
skeleton UI
notice that these animations differ
between Android and iOS
now let's go into the screenshots I
captured of the Uber application
this shows that even a major native app
can have ux blunders
we have the Uber logo splash screen
followed by a permission prompt for
location
but let's move on
this is the basic login flow to Uber
one of the first things I checked is the
look and landscape mode turns out that
Uber doesn't support that
the app is fixed to Portrait on mobile
phones it doesn't seem to have much
support for tablets either which makes
some sense as you would probably not
hail a cab with a tablet
this allows them to simplify some of
their user experience logic and we can
probably use a similar approach
there are two options for login the
first uses social again through Google
Facebook
and that falls to the native login
option
the second one uses SMS style activation
by collecting the phone number
we have support for that in our SMS
activation cn1 lib
the UI is shown here are all very simple
clean and minimalistic which should make
them very easy to replicate in codename
one
on the right you can see a simple form
that allows us to select a country
if the one we detected isn't correct
it's pretty simple list with flags and
search
notice that when you scroll down in the
form the title collapses in the material
design style to provide a more compact
View
the SMS activation process works with
four digits it doesn't seem to
automatically offer to grab the SMS
which is pretty lame
we can do better than this I don't
understand why Uber wouldn't do
something better on Android where it's
possible
one important thing to notice is the way
the digit input looks
these are four separate digits but they
have one error message below
when a number is already active in the
server
you can use your password and get a
password reset form which I didn't
include in the screenshots
I'm not sure I'll go into that level of
detail with the implementation
one thing that is missing from the
screenshots is the next button progress
effect which is pretty cool when you
press the arrow button the screen tints
and the arrow is surrounded by a
circular blue progress bar that should
be pretty easy to accomplish in codename
one so it's something I'll try to do as
well
the UI itself is mostly the map which is
great
there are the cause and landmarks
highlighted in the map the where to text
field isn't really a text field it's a
button
when you click on it you see the search
form to find directions to order an Uber
that you can see in the screenshot next
to the map
notice notices can be swiped from the
bottom
this is a doable element but it's
non-trivial so I won't go into it with
this app and ignore that specific
element
we can see two floating buttons of
recent searches trips that you can
repeat
that should be pretty easy to replicate
as well
notice that the side menu icon that just
floats with no title to disturb the UI
of the application
one of the small details is the fact
that the menu back button is black
surrounded by a white outline
that means it will be reasonably visible
both on a dark and light map that's a
great attention to detail
once we pick a location we can get a UI
prompt with an order option it also
shows the direction on the map
highlighting my location and destination
if we open the sign menu we can see the
design is very simple
I'll skip the Uber for business stuff in
the app we make but I'll try to
reproduce this exact UI design
notice how the minimalistic design that
even skims on colors is able to
broadcast Elegance
payment is a relatively simple you user
experience
I won't go into it at all because we can
just integrate brain free for billing
and Skip on some of these complexities
credit card billing is problematic not
just due to technical difficulties but
due to liability I wouldn't go into that
unless I had to
I won't go into the details of each of
these forms notice I blocked out in red
some private information about my trip
that isn't really important
one thing you should notice is how
simple these uis are I won't really get
into them but they should be pretty
trivial
the iOS version is remarkably similar to
the Android version both in design and
transitions notice that the login form
has some pretty cool background rotation
animation
but other than that only the transitions
differ in the app everything else looks
identical
even the text input and The Floating
Action button this will make our lives
much easier
