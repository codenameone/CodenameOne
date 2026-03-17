---
title: "13. Client Side UserService"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 13
weight: 50
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube l97sDefhHMM >}}

## Transcript

now that we have a server and a mock
client we need to connect them together
so we have a working prototype
we also need to implement some core
functionality such as sms activation
before we get started we need to add
some things to the client project
first we need to add the websockets cn1
lib from the extension manager this is
pretty simple to do
if you already added a cn1 lib before
next we need to sign up to twillow.com
as developers
they have a free trial account notice we
don't need to install support for the
twillow lib since we already installed
the sms activation cn1 lib before
notice that we are sending the sms
activation code from the client side
this is a bad practice
you should use the server twillo api and
send the sms activation code from there
i chose this approach because it's
seamless and shows the usage of the apis
on the client which is what i'm trying
to teach
however
keeping authentication code in the
server is far more secure
you will need the following values from
the twillow developer account
account id
sid account sid
auth token
and phone number
make sure to pick a us phone number for
the free account otherwise payment would
be required
once you have those values you can
create a new globals class which we will
use for the global application data
notice you might want to replace
localhost with your ip during
development so you can test the device
against a server running on your machine
the device would obviously need to be
connected to the same wifi
the following values
are the values we have from twillow
for convenience i used static import for
these constants within the code
the user class on the client side
mirrors the user dao
but uses the properties syntax so we can
leverage observability json persistence
and other core capabilities
if you are familiar with properties
already you won't notice anything
special about this class it's just a
standard property object
if you aren't familiar with properties
please check out the video covering them
as it would be helpful moving forward
we need to define a connection layer
that will abstract the server access
code this will allow us flexibility as
we modify the server implementation and
the client implementation it will also
make testing far easier by separating
the different pieces into tiers
the abstraction is
similar to the one we have in the server
i chose to go with a mostly static class
implementation for the user service
as it's inherently a static web service
it makes no sense to have more than one
user service
once logged in
we will cache the current user object
here
so we have all the data locally
and don't need server communication for
every query
we bind the user object to preferences
so changes to the user object implicitly
connect to the preferences storage api
and vice versa
preferences allow us to store keys and
values in storage which maps every entry
to a similar key value pay
whether we are logged in or not or out
is determined by the token value
we need that to send updates to the
server side
i'm creating the four digit verification
code
and sending it via the twillow sms web
service api
i'm also storing the value and
preferences so i can check against it
when it's received even if the app dies
for some reason
this method
is invoked to validate the received sms
code
notice i don't just use equals
instead the validation string might
include the four sms text
this can happen on android where we can
automatically validate
notice i still limit the length of the
string to prevent an attack where a user
can inject all possible four code
combinations
into this method
maps to the user
exists method in the server which we use
to determine
add slash login flows
i use the rest api to make a single
connection with the get method
in this case the response is the string
true or the string force so i can just
check against the letter t
when adding a user i use the rest api's
post method
here i can set the body to the json
content content
of the user object
the response is a string token
representing the user which we can now
store into preferences
the login method accepts a phone and
password and is invoked
after we've validated the phone
it can succeed
or fail
if we get back a token that means the
user authentication exception
wasn't thrown in the server and we can
set it into the preferences
otherwise we need to send a failure
callback
