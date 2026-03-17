---
title: "46. Push - Client Side Integration"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 46
weight: 123
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 13: Creating a Facebook Clone


{{< youtube GEzM1MXkqnk >}}

## Transcript

it's now time to map the push support
into the client side
to do that we need to implement the push
callback interface in the main class of
our app
we can only implement push callback in
the main class
nowhere else
register push should be invoked with
every launch to refresh the push key as
it might change
we use call serially to defer this
so we don't block the start method
the push method is invoked with the text
of the push
in that case we call the refresh method
to update the ui
we'll discuss that method soon
the device id isn't the push key
it's the os native push value
normally you would have no need for it
as it's here for compatibility only
the push key value is only guaranteed
once this callback is invoked
if the push key changed since the last
time we update the push key in the
server
we'll cover this method soon
there isn't much
we can do in the case of an error so
right now i only log it
in some cases you can show a ui
notification but make sure not to do it
from this method as it's invoked early
in the app launch cycle and that might
impact the ui in odd ways
just store the error details and show a
notice later on
next we need to cover the server api
changes and refresh method
we'll start with the former
in the sign up or login method of the
server api
we need to invoke register push after
the successful login
this is important for the first time the
user logs in to the app
next we need to map the update push key
method
this is just a simple call with one
argument
we return a boolean value to indicate
that the call worked that's it
ui controller needs a bit of work too
first we need to add a field for the
main form instance
this will allow us to refresh the main
form as notifications come in
the method is now updated
this method is now updated with the new
main field so we can refresh the ui
refresh delegates into the main form
instance
it's possible that the push method is
invoked before loading completed so main
would be null
in that case this isn't a problem since
main would be created with fresh data
anyway
again with mainform we need to take
fields that were in the constructor
before and move them to the class level
so we can refresh them
once we went through that with the
refresh method is easy
if we are currently viewing the news
feed
and we
aren't minimized a refresh might create
a bad user experience
if we are minimized or in a different
tab we can refresh the ui immediately
this refresh method is from infinite
container
the same logic applies to the
notification code
this method shows a toast bar offering
the user a refresh if he taps the toast
bar
we use this to prompt the user instead
of refreshing automatically
with that push should work and refresh
when applicable
