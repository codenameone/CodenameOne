---
title: "44. Push Notification - Theory, Entity and Service Layers"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 44
weight: 121
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 13: Creating a Facebook Clone


{{< youtube LFoSl_a2rs8 >}}

## Transcript

this is probably the easiest section in
terms of materials but since push is
always such a hassle it could possibly
take you the most to complete
i chose to use push for all the server
originating communications which has its
drawbacks a better strategy might have
been combination of push and web sockets
but in the interest of brevity i chose
to go with push alone as i demonstrated
web sockets enough and we'll go back to
them in the future
push notification allows us to send
notification
to a device while the application might
be in the background
this is important both as a marketing
tool and as a basic communications
service
polling the server seems like a sensible
time proven strategy
however there are many complexities
related to that approach in mobile
phones
the biggest problem is that polling
application
will be killed by the os as it is sent
to the background to conserve os
resources
while this might work in some os's and
some cases this isn't something you can
rely on for instance android 6 plus
tightened the background process
behavior significantly
the other issue
is
battery life
new os's exposed battery wasting
applications and as a result my trigger
uninstalls
this makes even foreground polling less
appealing
if you are new to mobile development
then you heard a lot of the buzzwords
and very little substance
the problem is that ios and android have
very different ideas of what push is and
should be
for android push is a communication
system that the server can initiate for
instance the cloud can send any packet
of data and the device can process it
in rather elaborate ways
for ios pushes mostly a visual
notification triggered by the server to
draw attention to new information inside
an app
these don't sound very different until
you realize that in android you can
receive slash process a push without the
awareness of the end user
in ios a push notification is displayed
to the user but the app might be unaware
of it
this is important ios will only deliver
the push notification to the app
if it is running or if the user clicked
the push notification pop-up
codename one tries to make both os's
feel similar so background push calls
act the same in ios and android as a
result
you shouldn't push important data
push is lossy
and shouldn't include a payload that
must arrive
instead use push as a flag to indicate
that the server has additional data
for the app to fetch
for this case we use push to let the app
know about an update
it then performs a refresh to fetch the
actual data with the usual web services
before we proceed i think it's a good
time to discuss the various types of
push messages
zero or one
is the default push type
they work everywhere and present the
string as the push alert to the user
two is hidden non-visual push
this won't show any visual indicator on
any os
in android this will trigger the push
string call with the message body in ios
this will only happen if the application
is in the foreground otherwise the push
will be lost
three allows combining a visual push
with a non-visual portion
expect a message in the form of this is
what the user won't see
semicolon this is something he will see
for instance you can bundle a special id
or even a json string in the hidden part
while including a friendly message in
the visual part
when active this will trigger the push
string method twice once with the visual
and once with the hidden data
notice that if the push arrives when the
app is in the background on ios and the
user doesn't tap the notification
neither one of the messages will be
received by the app
4 allows splitting the visual push
request based on the format title
uh semicolon body to provide better
visual representation and some os's
five sends a regular push message but
doesn't play a sound when the push
arrives
100 sets the number badge
it's applicable only to ios it allows
setting a numeric badge on the icon to
the given number
the body of the message must be a number
for instance unread count
101 is identical to 100 with an added
message payload separated with a space
for instance 30 space you have 30 unread
messages
we'll set the badge to 30 and present
the push notification text you have 30
unread messages
this again is ios only
we need some values from google and
apple to fill fb clone keys properties
these values help us send the push
through the apple google servers
we need the following keys in that
properties file notice i will go into
the process of obtaining each of those
soon
push.itunes production
is true or false
ios push calls target
either the production or the development
servers
push itunes prod cert
don't confuse
this with a certificate used for app
signing
this is a separate certificate used for
authenticating against apple's push
servers you need to host it in the cloud
we do that for you if you use the
certificate wizard
push dot itunes prod pass this is the
password for push itunes prod cert
certificate
push itunes devcert just like with the
code signing we have two certificates so
this one is used when we are pushing in
the sandbox during development
this is used when push itunes production
is set to false
push token
sorry push
itunes dev pass
this is the password
for the push itunes dev cert certificate
push token this is a secure token you
can fetch from your codename one account
in the settings tab
push.gcm key this is the value you need
to fetch from google developer tools
console
android push goes to google servers
and to do that we need to register with
google to get keys for the server usage
you need one important value push.gcm
key
to generate this value follow these
steps
login to
console.cloud.google.com
select apis and services
select library
select developer tools
select google cloud messaging
click enable
and follow the instructions
the value we need is the api key which
you can see under the credentials entry
you will need to rerun the certificate
wizard for the project for ios
if you generated certificates before
say no to the set step that asks you to
revoke them and copy your existing
credentials certificate p12 file and
password to the new project
make sure to check the include push flag
in the wizard so the generated
provisioning includes push data once
this is done you should receive an email
that includes the certificate details
this will include urls for the push
certificate we generated for you and the
passwords for those certificates
apple has two push servers sandbox use
this during development production this
will only work for shipping apps
you need to toggle the push.itunes
production flag so push messages go to
the production version of the app
now that we understood the theory let's
go into the practical terms of sending
push notifications from the server
the first step is in the user entity we
need to access the device push key so we
can send a push message to the user's
device
that's it
well almost
there are also getters and setters but i
always ignore those anyway
this isn't in the dow so there is no
boiler plate there the push key is
private to the server it has no place in
the dial so we leave it
there here
we do however need to add a method to
the user repository interface we need
this finder to remove out of date and
expired push keys based on messages sent
from the push server
we need to expose the properties that we
added to the fb clone keys properties
file in the notification service bin
my first intuition was to inject the
user service into the notification
service class
this failed
the reason it failed is circular
references
user service already references
notification service so including user
service there
would mean spring boot would need to
create user service create notification
service to inject into user service
create a user service to inject into the
notification service
etc
thankfully it fails quickly with an
error
the solution itself is pretty simple
though
we add a new service that will handle
this api keys
we removed this code from user service
as it is as is and placed it here
these two helper methods remove the need
to use api keys variables directly
the next step is injecting this into
both user service and notification
service
i'll demonstrate this in the
notification service but the change to
user service is same
i'll get to the surrounding method soon
but for now i just wanted to show how to
use this new api to get the values from
the properties file
so all we need to do is replace every
call to prop.getproperty
with
keys.get that's also far more flexible
and elegant in the future we can
probably use a smarter system for
configuration than a properties file
now that it's encapsulated
