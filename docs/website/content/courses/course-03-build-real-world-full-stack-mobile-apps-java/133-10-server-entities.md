---
title: "10. Server Entities"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a WhatsApp Clone"
module_key: "14-creating-a-whatsapp-clone"
module_order: 14
lesson_order: 10
weight: 133
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 14: Creating a WhatsApp Clone


{{< youtube Rok0Ubr4xes >}}

## Transcript

we are finally back to the spring boot
server we set up initially
the code here is pretty simple as well
the whatsapp application is the
boilerplate main class of the spring
boot project there isn't much here and
i'm only mentioning it for completeness
security configuration is a bit more
interesting
although again it's similar to what we
saw in previous projects
first we need to permit all requests to
remove some http security limits
we don't need them here since this isn't
a web app
similarly we need to disable csrf
protection as it isn't applicable for
native apps
finally we need to provide password
encoder implementation
we use this to encrypt the tokens in the
database
next let's go into the entity objects i
won't go into what entities are as i
discussed them a lot in the previous
modules they are effectively an
abstraction of the underlying data store
the user entity represents the data
we save for a chat contact
i use a string unique id with a
universal unique identifier which is
more secure as i mentioned before
it might make sense to use the phone as
the id value though
the username and tagline are also stored
similarly to the client side code
phone is listed as unique which makes
sure the value is unique in the database
when we send a verification code we
store it in a database i could use a
distributed caching system like redis or
memcached but they're an overkill for
something as simple as this
the date in which the user end entry was
created is a standard database date
this isn't used at this time but it's
very similar to the code we have in the
facebook clone to store media
in fact it's copied from there and we
can refer to that for media storage
slash upload
the auth token is effectively a
combination of username and password
as such it's hashed and as such only the
user
device knows that value
i believe that's how whatsapp works
that's why only one device can connect
to a whatsapp account since the token is
hashed when you need to retrieve an
access token you need to effectively
delete the last token and create a new
one in order to set up a hash
for the uninitiated a hash is an
encrypted value that can only be
generated but not retrieved so if my
password is password and the hash is x y
z j k l
then i can get the value of the password
from the hash
but i can check that and i can check
that password matches
x y
z jko
but not vice versa
hashes are also
salted so they have 60 characters and
length and the strong hashes are
impossible to crack with standard tools
the push key is the key used to send
push messages to the client device
this flag indicates whether a user is
verified
when we create a new user we initialize
the id and creation date sensibly
if this entity is loaded from the
database these values will be overridden
the dow methods create data access
objects
that we can send to the client
we will make use of them later in the
service code
the user repository maps to the user
object and exposes three finder methods
we use find by phone
during sign up and sending to detect the
user with the given phone
this method should be removed as it's
part of the copy and pasted media entity
code
we need to find the put the push by push
key in order to remove or update expired
push keys
if the server returns an error we need
to update that
user dao is pretty much the content of
the user class there isn't much to
discuss here with one major exception
and that's the json format annotation
here we explicitly declare how we want
the date object to translate to json
when it's sent to the client
this is the standard json pattern and
codename one in the client side knows
how to parse
this you
