---
title: "16. Connecting the Location Service to the Map Form"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 16
weight: 53
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube ymocxBIQn0o >}}

## Transcript

we discussed the location service class
now let's see how this maps into the ui
back in the map form we can change the
map player code which during markup just
featured a fixed position car image
we bind to the location service where we
get a callback every time a driver comes
into play
we create a label for every car and set
the icon with the right angle we keep
the angle in a client property
so when there is a change event
we can check if the angle actually
changed
one important piece of information that
might not be clear from the code is that
the core image must be square
we use the rotate method on the car
image which assumes a square image
otherwise it will appear cropped
we place the new car where the user is
located
thanks to the map layout
the change listener on the angle
property
automatically rotates the icon image in
the right direction
but it does that only if the angle
changed
to avoid performance
penalty we update latitude and longitude
separately
but we need to guard against duplicate
changes so we first test the existing
value
we can't replace a constraint in the
layout so we remove the component and
add it back
animate layout should still work in this
case
and it will move the car gracefully to
its new position
once all of this is done
we should be able to see everything
working right now we don't have drivers
in our database but we can add a fake
driver by pushing an entry to the mysql
database
this will create a fake driver entry and
allow you to see him when you log in
assuming you configured the values in
globals.java you should be able to run
the server and client then activate the
device using sms
and see the driver
i used the password from my account so i
would be able to log in as the driver
later
you can just copy the same password
value from your account
as you already know the password
