---
title: "Introduction"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Create a Netflix Clone"
module_key: "15-create-a-netflix-clone"
module_order: 15
lesson_order: 1
weight: 138
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 15: Create a Netflix Clone


{{< youtube r_ti2QAhm9s >}}

## Transcript

welcome to the first lesson
of creating a netflix clone with
codename one
there are a few things i'd like to talk
about in this module
but first i want to clarify that this
module is relatively short as are trying
to focus only on the new things
so the netflix clone is less of a clone
and more of a proof of concept
the clone is much simpler in scope and
functionality when compared to previous
modules this is intentional i don't want
to repeat things that were better
covered in the facebook or uber
tutorials
but i do want to cover new things
i placed most of the focus on the
nuances of the netflix ui
but i also placed some focus on
different approaches
for working with spring boot
i think these will prove valuable as we
go back and look at the stuff we did in
the previous modules
but first let's talk about the
complexities of video platforms
technically they aren't very complex in
fact they are remarkably simple for the
most
part the biggest problem faced by
netflix is scale
and that only matters when you reach
netflix levels of scale
videos and platforms like netflix are
generally
generated statically before the first
request is made
that effectively means that servers just
serve ready-made files and don't do
complex runtime work
there are great tools that pre-process
video files such as ffmpeg
these tools can be used as native
libraries in the server or as command
line tools
most netflix clones just pre-generate
all the video files in the various
resolutions bitrate options
then the
work amounts to picking the right video
url
the video urls can be further scaled
using pre-existing content delivery
networks also known as cdns
we specifically use cloudflare at
codename one but any cdn would do
we didn't cover cdn hosting and
literally all of the complexities in the
server here we also don't cover anything
related to video processing
that's server logic that falls way
outside the scope of a mobile tutorial
furthermore a lot of this work can be
done completely outside of the server as
a separate tool that updates the url's
databases
video hosting can be done as a separate
microservice and mostly hidden from our
main backend logic
as a result the content of the
application will be mostly hard coded
this is important as there is an ip
issue with distributing a clone of
content which we don't want to get into
we also won't implement the multi-user
and authentication portions of the app
we covered all of that rather well in
the uber clone and there's no point of
going into this again
once all this is removed the server is
ridiculously trivial
most of this applies to the client ui to
we covered almost all of this before so
the netflix clone is a rehash of many of
those ideas with a new coat of paint
the ui is trivial and includes only two
forms both of which are only partially
implemented there is no reason to go
deeper as
the
their source application isn't very
complicated to begin with
you can use the facebook clone as a
reference to more elaborate ui
once the css is in place implementing
the missing functionality in a netflix
clone becomes trivial
but there's one bigger mission i chose
to use the native player
native video playback is actually pretty
great it handles everything we need in
terms of ui for the video player
the problem starts when that mode isn't
enough say we want more control over the
behavior of playback code we can't do
much in that mode our control is very
limited
however
native playback is pretty much a turnkey
solution for video playback that's why
we picked it it's a great tool for
getting started
lightweight is more error prone and
powerful a good example is closed
captions which we can implement manually
in lightweight mode but literally
placing by literally labeling and
placing labels on top of the playing
video
that's very powerful
i will create a separate module that
will cover lightweight video playback it
should be easy to adapt the playback
code to make use of that approach
the final ui should include these two
forms
the latter will allow video playback
notice that all the videos lead to a
hard-coded video url of a spinning earth
again due to ip issues
thanks for watching i hope you'll enjoy
the rest of the course and will find it
educational
