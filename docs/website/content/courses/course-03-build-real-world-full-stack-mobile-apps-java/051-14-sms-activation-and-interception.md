---
title: "14. SMS Activation and Interception"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 14
weight: 51
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube TOWhbEhiRe4 >}}

## Transcript

the next step is binding this to the ui
for a fully working sms activation
process
the sms activation process is
practically done
the first step is in the enter mobile
number form where we need to change the
event handling on the floating action
button
this code might produce a dialog
if we show it in the current form it
might go back to this form instead of
enter sms verification digits form
by using this callback we can make sure
the next form is shown
only android supports intercepting sms's
so this code will only run on that
platform
in that case we automatically validate
against the string we get from the next
sms
if that works we automatically skip
ahead to the password form
regardless of the above
we send an sms message
to the given phone number
next we need to validate the input
in a case where sms isn't validated
automatically
or if the user rejected the permission
on android
we can do this in the enter sms
verification digits form class
by editing the is valid method
this pretty much does the sms activation
but we'd also want the countdown
functionality to work
if you recall the ui there is a
countdown label for resending the sms
to implement that we need to first
define two new member variables and
define two helper methods
the vari
the variables represent the countdown
value in seconds
for resending the sms
and the timer object
which we need to cancel
once it elapses
the format method
formats time in seconds as two digits
four minutes and two digits for seconds
next we need to make the following
changes to the constructor code
we schedule the timer to elapse every
second and repeat on the current form
we update the text which we draws
automatically notice that it's also a
good practice to revalidate normally
but since the string size would be
roughly the same
this shouldn't be necessary
we cancel the timer so we don't keep
sending the sms's over again
notice we don't cancel the timer in case
of success we don't need to
since it's a ui timer it's bound to the
form and once we leave the current form
it will no longer elapse
this sends us to the password entry form
where we now have two versions of the ui
we connect to the server to check if the
user exists and shown infinite progress
ui
over the previous form
if the user exists we show a welcome
back prompt
otherwise we enter a new password
we also need to change the code that
handles the floating action button event
to actually add or load the user
if the user exists
we call the login method and show the
map
on success
if the server returned an error on the
login
we dispose the progress dialog and show
the error message label
we prepared before
we use revalidate as the error label
size changed
and will occupy more space
if the user didn't exist before we
create a new user object and add that
user to the server
then show the map
if the operation is successful the error
handling code is pretty similar to the
previous code
