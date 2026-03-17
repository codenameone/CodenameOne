---
title: "34. Facebook and Google Login Code"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 34
weight: 71
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube UwdI_tMqYgU >}}

## Transcript

next we'll integrate the facebook login
process into the code
to get the native login working we only
need one step
add the build hint
facebook.app id
equals
the app id
app id is the id from the dashboard on
the facebook app page this will make
facebook work on the devices almost
seamlessly
notice that since we have effectively
two apps we'll need to add an app id to
the user app and the driver app
to get login working on the simulator
we'll need a bit more we'll also need to
write code that supports the login
process within the facebook or google
login form class
facebook connect is a subclass of the
login class that lets us login into
facebook and request publish permissions
if necessary
the client id and secret aren't used on
devices
these are hair strictly for the benefit
of the simulator
if you don't need to debug on the
simulator the lines until set callback
are redundant
notice that we have two versions of
these values for the uber app and the
driver app
the callback is invoked upon login
success failure
if the login is successful we get the
token from facebook which is an
authorization token this token allows us
to access information within the
facebook graph api to query facts about
the user
notice that we have a new constructor
for enter password form which i will
discuss soon
this triggers the actual login
but the method is asynchronous and login
will only actually succeed or fail
when the callback is reached
before we go to the google login support
let's look at the additional changes we
need to get
both facebook and google working
i already discussed the changes to enter
password form
so let's start there
the constructor accepts
one of the three options the other two
should be null in this case
i also updated the user service method
accordingly i'll get into that shortly
notice i snapped some code below here
to keep the entire block in one page
but it's still there
the login method now accepts the google
facebook credentials as an optional
argument
two of the three values for
identification will be null so we can
set all of them
and only one will have a value
next let's see the changes to the user
service class
this is the main method we use
which we broke up for the other types
the generic implementation demonstrates
why i chose to change
the argument names from phone to v
so it can now suit all permutations of
this method
login is almost identical to the
original code
i added the new values to the mix if
they are null the arguments won't be
sent
and everything will work as expected
once this is done facebook login should
work on the device and simulator
