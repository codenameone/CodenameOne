---
title: "1. Getting Started"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a WhatsApp Clone"
module_key: "14-creating-a-whatsapp-clone"
module_order: 14
lesson_order: 1
weight: 124
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 14: Creating a WhatsApp Clone


{{< youtube fJD45Mz8SZM >}}

## Transcript

in this module we'll build a rudimentary
whatsapp clone
i'll take a very different path when
compared to previous cloned applications
here
we'll talk about that and
these things in the next few slides
the first thing i'd like to go into is
that this clone isn't pixel perfect
it's a fast and dirty clone
i'll focus only on the basic text
sending functionality
and won't go into
nearly as much as detail as i did in the
other clones
frankly it's not necessary
the functionality is simple once you
understand the facebook clone
i skipped a lot of the core features
mostly due to time
but also because these features are
covered in the facebook clone and
re-implementing them for yet another
clone would have been redundant
i'll only focus on these forms i won't
implement everything here either
since we already implemented sms
verification in the facebook loan i'll
use the sms activation library however
i'll use server side authentication for
that library for extra security
whatsapp doesn't use passwords
i considered using the phone number as
the id which seems to be what they are
doing i ended up using a regular id
though there is an authorization token
which i will discuss later
like other clones i use spring boot
again that makes this familiar and
allows me to grab some code from the
other projects the database will read
mysql again for the same reason
i'll store data as json locally instead
of sqlite this makes it easy to use and
debug the code that might be something
worth changing if you are building this
for scale
i'm still using web services for most of
the functionality they are still easier
to work with than web sockets
however messaging is the ideal use case
for websockets and i'm using it for that
exact purpose
when the websocket is closed which would
happen if the app isn't running or is
minimized i use push
push is used strictly as a visual medium
to notify the user of a new message
when sending and receiving data in
websockets i use json
in the past i used a binary websocket
and i wanted to show how this approach
works as well
so let's start by creating the spring
boot project for the server i'm assuming
you went through the previous modules
and i won't repeat myself too much
you can use the spring boot initializer
which i used to create this pom file
i just defined a new project with this
package for java 8.
we'll use jpa to communicate with
the mysql database
we use jersey for pojo json
serialization
the security package allows us to
encrypt credentials into the database
it has a lot of other features but they
aren't as useful for us
we need
web services support too
and web sockets to implement the full
communication protocol
the underlying database is mysql so we
need the right drivers for that
and finally we need the server side
twillow library to implement sms support
on the server this will be used for the
activation code functionality
next we need to create the client
application which i create as whatsapp
clone
i use the default native and bare bones
settings in the ide
i pick the com
codename1.whatsapp package nothing
special next we'll go into the classes
that implement the client functionality
