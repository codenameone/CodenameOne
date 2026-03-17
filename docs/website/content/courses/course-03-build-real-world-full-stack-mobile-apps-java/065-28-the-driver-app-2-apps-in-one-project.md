---
title: "28. The Driver App - 2 Apps in One Project"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 28
weight: 65
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube 91BrBoia4nM >}}

## Transcript

when i started on this road i didn't
want to create a driver app
and wanted to focus only on the client
app
unfortunately a driver app is
unavoidable as the main app is useless
without it
unlike the main uber app i don't want to
clone the driver app as this would throw
the whole course out of focus
i decided to hack the existing app to
implement driver specific features there
and as a result we use a whole lot of
code
this means the driver app can reuse sign
up map networking code etc
this is important
as code reuse breeds stability and
maturity
unfortunately it also means the app
isn't as well made
as the main app
since it was designed for end users with
driver mode tacked on top
Use Cases
we often build one app and sell it to
multiple customers
after all
most customers ask for similar things
with minor changes
for example if i build a restaurant app
and then sell it to one establishment i
can then resell it to another one with
almost no change at
all another common use case is the demo
or free version of a paid app
you want to use as much of the work as
possible without maintaining two code
bases
another case which is relevant what i'm
building today
is two target audiences of roughly the
same functionality
for instance in this case the driver app
has many common elements with the
passenger app
so why not build the same app with minor
modifications
the first thing we need to understand is
how the app store identifies your
application
pretty much
all apps use a unique identifier
a
string that is similar to package names
which we map to which we map the main
application package name so the trick is
to add a new package for the new app
Creating a New Package
the basic hello driver
would be something like this
notice i chose to override the methods
but i didn't have to
i could have just derived the main class
and that would have been enough
but i prefer overriding the lifecycle
methods for clarity in this case
this isn't enough though
the codename one settings properties
file contains all of the internal
configuration details about project
i copied it aside and renamed it to
codename one settings user properties
i then edited the file to use the driver
details and copied that into codename
one settings driver properties
notice i snipped most of the text here
and there are several other locations
where the values
are mentioned
Updating the App
it's important to update the app id
to use your apple developer account
prefix and package name
this is automatically generated when you
run the signing wizard to create ios
developer certificates
these certificates should be the same
between the driver and the user app
however provisioning profiles should be
generated
for both when you build an ios version
of your app
update codename1 dot main name to match
the class name
update codename 1 package name to match
the package name
other than certificates there are many
nuances you would probably want to
customize in this file such as
the app name and the icon
once this is all done
you would have a separate app
that does the exact same thing
Driver Detection
it's really easy for us to detect the
driver app and write custom code for it
first let's change the uber clone class
to add driver detection
this introduces a public is driver mode
method into the uber clone class
it would always return false unless we
made one tiny change
to driver app
this
is a block within driver app class
we can now invoke his driver on the uber
clone class and get the right result
Push Notification
i've discussed push notification before
in the
beginner course
please make sure to follow the
registration instructions there and
update the values in the globals class
once those are in place we can integrate
push into the driver app
we need to register for push every time
the app loads
the logic is that the push key might
change so we need to keep it up to date
we send a type 3 push which will invoke
this code twice
the first time around will receive a
hash symbol followed by a numeric id
the second time will receive a the
display value
when we receive the display value we can
show a notification to the driver
and if he clicks this notification
we can show him the details of the ride
ideally we would also have a rides menu
item in the side menu but for now i'm
compromising on the driver app to move
forward
when register succeeds this method is
invoked
the main use case is sending the push
key to the server
so it can trigger
push messages
notice that the device id
is not equal to the push dot get key
value
device id is the historic native device
key
and shouldn't be used
also notice i send this every time even
if the value didn't change
it's not a big deal in terms of overhead
so i ignored that small bit of overhead
