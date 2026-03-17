---
title: "33. Social Login - Basics and Facebook App"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 33
weight: 70
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube qKc1hZyw360 >}}

## Transcript

we already prepared a lot of the
groundwork for social login on the
server
but didn't finish all the pieces so
before i step into the client side
changes needed for social login let's
discuss some of the required server side
work
i had to include support for the exists
functionality that works based on a
social token
i also had to include a similar call in
the user web service class
i also have a similar subtitle change
to the regular exists method
the argument name was phone and is now v
which means i can invoke all three web
services with very similar code on the
client side
that's it pretty much everything else
was already done
social login lets us authenticate a user
without getting into the
username password complexity
this is usually almost seamless on
devices where we
the pre-installed social app is invoked
explicitly and the user just needs to
approve the permission
this is defined as a low friction
approach to authenticate the user
and is often superior to phone number
activation
in codename one this is pretty trivial
to accomplish especially for
google and facebook login both of which
are built into codename one and to
android slash ios respectively
connection to social networks and
codename one has several common concepts
if the device has native support or
social app installed this native
integration will perform a login
if it doesn't but we are on the device
the native sdk will show a web based
login
if we are on the simulator we will fall
back to an oauth based login this leads
to a situation where login might work on
one device but fail on a simulator or
fail on a different device type
it also makes the configuration process
a bit more tedious
to be fair the native configuration is
much harder and involves more code
since the driver app is physically a
separate app
we'll need to redo some of the steps and
effectively go through everything
twice
a core concept of the login process
in facebook is the app
which is a facebook internal term
unrelated to your actual app
facebook's view of an app
is anything that uses the facebook graph
api and authentication
in this case we need to create a new app
and should name it like we do our actual
app so the user will be able to identify
it
the steps are pretty easy we navigate to
developer.facebook.com
apps
and press the add a new app button
next we need to select the product we
are trying to use and we need to select
facebook login
once there we are presented with a
wizard containing multiple steps
to set up your app
you need to run through the wizard twice
once for ios and once for android
the content of the wizard
changes but the gist is the same
we don't really need much information
and can skip almost everything
the first step is download and install
the facebook sdk for ios
this is obviously unnecessary for us so
we can just press
next
the second step is add login kit to your
xcode project again there is no need to
do anything and we can press next
the third step is add your bundle
identifier
it's more interesting we need to enter
the project package name here and
press save
then we are effectively done with ios as
everything else is more of the same
the android wizard has one task that is
a bit challenging but other than that it
should be trivial
before we begin we need to generate key
hashes for facebook which need to be
done on your development machine
to do that you will need a command line
with the jdks bin directory in your path
you will also need the path to the
android key store you use for signing
you can find this file in the android
signing section in codename one settings
if there is no certificate file there
make sure to generate it
once all this
is in place you can use this command
line for linux mac
this will provide the sha1 key you will
need in the android wizard
similarly on windows the command
follows a similar structure but uses
windows command line conventions
now that this is out of the way
let's go over the android wizard steps
the first step is download the facebook
sdk for android this is obviously
unnecessary for us so we can just press
next
the next step is import the facebook sdk
again there is no need to do anything
and we can press next
the next step is tell us about your
android project we need to specify the
package name for the application
which in our case is com
codename1.app.uberclone
we also need to specify the main class
which is ubercn1 stub
the main class is effectively the main
class name with the word stub with an
uppercase s appended at the end we can
then press next
after i press next i got this warning
because the app isn't in the store yet
facebook thinks i might have typed the
package incorrectly and provides this
warning which we can ignore
and finally
we are at the add your development and
release key hashes here we need to add
the hash we got before and press next
the rest of the wizard isn't important
before we proceed we need to enter the
facebook dashboard and copy two values
the app id
and app secret which we will need when
we set up the code
