---
title: "37. Cover Transition - Conditionally Showing a Form Transition"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 37
weight: 74
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube IaalGP1UDeU >}}

## Transcript

the code for morph transitions
broke another thing
it broke the facebook or google login
form
which looks awful going in now because
morph is generally designed for a
specific form
i want to use the vertical cover effect
which is common on ios and looks pretty
decent on android 2.
cover slides the form on top of the
existing form from the bottom
it's usually combined with uncover
which slides the form out in the reverse
way
because of this unique semantic the
cover transition uses both the in and
out transition flags
however this can pose a problem
with the default out transition of the
form that we are leaving
in this case you would see the out
animation of the login form
which in this case is morph
followed by the incoming cover animation
the solution is to remove the out
animation from the outgoing form
and restore it to the original value
when we get back
we do that within the remove transition
temporarily method
which we call here from the facebook or
google login form
we need to remove both the in and out
transitions
as we might show a cover transition
on top of another cover transition form
when we return to the original form we
restore its transitions to their
original values
we remove the show listener to prevent a
memory leak
and multiple restore calls when going
back and forth
