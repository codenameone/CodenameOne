---
title: "11. The Spring Boot Server"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 11
weight: 48
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube EkiTDQn9Cpg >}}

## Transcript

now that we got the mock-up running
let's jump to the other side of the
fence and set up the server
if you aren't familiar with spring boot
or mysql or don't understand why i
picked both of them i suggest
checking the previous modules where i
discussed the reasons for this
extensively and gave a long overview
over both
you can create a new database by logging
into mysql and issuing a create database
command
i created a new spring boot project
which includes maven dependencies for
jpa which is the java persistence
architecture or hibernate
jersey which is the json and xml
serialization framework
web which is useful for web service
development
websocket for connecting over the newer
websocket protocol
security is mostly used for password
hashing which we will discuss
and mysql support is needed for the jdbc
connectivity
and finally braintree for the payment
processing we'll need later on
before we get into the code let's take a
minute or so to think about the things
we need from the server
the first thing the server needs to
offer
is an ability to add a new user
we also need
authentication and authorization for the
user such as password validation and
authentication
we need a way to update the user
information
we need to track car positions so we can
show them on the map
we need the ability to hail a car and
pick up
a driver
we
need a
we need to pay the hailed car so the
system knows the car is paired to us
we need to log every trip taken
including distance path etc
and finally we need to provide rating
facility so we can rate the drivers
i'm
glossing over some things here such as
billing push etc
now that we have a spring boot project
let's skip ahead to the server code
we'll start with storage which is a good
place to start a common design strategy
is deciding on the data structure and
then filling in the blanks
a good way to decide on the elements we
need in the user object is through the
ui
the account setting class contains
a lot of the data we need
the user jpa entity uses an auto
increment id value for simplicity
let's go over the various pieces here
these are
part of the user settings such as
just general configuration
the password stores
the hashed value of password and not the
plain text version we'll discuss hashing
passwords soon
we will use these when we need to enable
login with a social network account
these are the internal network ids
we'll
use for verification
a driver is also a user in the system
if this user is a driver this is marked
as true
by referring to both the end user and
the driver with the same class we can
simplify the code
if this is a driver
this is the description of his car
we'll need for the app
this field is set to true
if we are currently in the process of
hailing a taxi
if the taxi is taken by user this field
maps to the user id it is set to null if
the taxi is available
we will have a separate object dealing
with rating
but we can sum it for every query so the
rating value will be cached here
this is the position and direction of
the current user
whether it's a taxi or an end user
notice i chose to just store the
location values instead of using one of
the custom location based apis
supported by hibernate and mysql
i looked into those apis and they are
very powerful if you need complex
location based apis
but for most simple purposes like we
have here
they are an overkill and would have made
the project more complex than it needs
to be
if you are building a complex gis
application i would suggest delving into
some of those custom apis
this is a picture of the user stored in
the database blob
one last column is the auth token which
we initialize with a unique random id
we will use this token to update the
user and perform operations only the
user is authorized for think of
authorization as a key to the server we
want to block a different user from
sending a request that pretends to be
our user
this is possible to do if a hacker
sniffs out our network traffic and tries
to pretend he's our app
one approach would be sending the
password to the server every time
but that means storing and sending a
password which holds risk in this case
we generate a random and long key that's
hard to brute force
we send the key to the client
and it stores that key
from that point on we have proof that
this user is valid
i've discussed this before in the
restaurant app if you want to check that
out
the repository class
for the user starts off
pretty standard entries
we first
have the option to find a user based on
common features
you would expect such as the auth token
phone etc
however
we also need some more elaborate
location-based queries
in this case
i verify that the entry
is a driver by always passing true to
the driver value
i also use the between keyword to make
sure that the entries i find
fall between the given latitude
longitude values
the find by driver method finds all the
drivers in a region
it's useful to draw the driver on the
map
even if they are currently busy
the second method returns only the
available drivers
and is used when hailing
like before we need a data access object
or dao to abstract the underlying user
object and make client server
communication easier
notice several things about this dao
first notice that we don't provide the
of avatar in the dial
it doesn't really fit here
as we'll apply it directly to the image
also notice that the auth token and
password are never returned from the
server they are there for the client
requests only
in this case the password
would be the actual password and not the
hash as the client doesn't know the hash
and the server doesn't store the
password
i'll skip the rest of the code as it's
pretty obvious
including constructors
setters and getters
we create the user dao instances
in the server by asking the user object
notice we have two versions of the
method
one of which includes some private
information and is useful internally
the other is the one we need to ask for
when dealing with client requests
the user service class is a business
object that abstracts the user access
code
if we think of the user object as the
database abstraction and the dao as a
communication abstraction the service is
the actual api
of the server we will later wrap it with
a web service call to make that api
accessible to the end user
this might seem like an overkill with
too many classes which is a common
problem for java developers
however in this case it's justified
by using the service class i can build
unit tests that test the server logic
only without going through the
complexities of the web tier
i can also connect some of the common
apis to the websocket layer moving
forward
so having most of my business logic in
this class makes a lot of sense
i have two other wired values here
first is the crude interface we
discussed earlier the tarkan used to
work with users and drivers
the second one is this spring boot
interface
used to hash and salt the passwords just
using this interface means that even in
a case of a hack your user's passwords
would still be safe
i'll discuss this further soon
adding a user consists of creating a new
user object
with the dao and invoking the built-in
crude save method
here we encode the password this is
pretty seamless in spring but remarkably
secure as it uses a salted hash
passwords aren't encrypted they are
hashed and salted
encryption is a two-way algorithm you
can encode data and then decode it back
hashing codes the data in such a way
that can't be reversed to verify the
password we need to rehash it and check
the hashed strings
hashing alone isn't enough
as it can be assaulted with various
attacks
one of the tricks against hash attacks
is salt
the salt is random data that's injected
into the hash
an attacker can distinguish between the
salt and hash data which makes potential
attacks much harder
the password hashing algorithm of spring
boot always produces a 60 character
string which would be pretty hard to
crack
i'll soon discuss the process of
checking a password for validity as it's
a pretty big subject
notice that
get avatar uses the id value that leaves
a small security weakness
where a user can scan the ids for images
of the drivers users
i'm not too concerned about that issue
so i'm leaving it in place however
letting a user update the avatar is
something that needs a secure token
continuing with the security aspect
notice that things such as password and
token are special cases that we don't
want to update using the same flow as
they are pretty sensitive
we have
three login methods
and they are all technically very
similar so they all delegate to a single
login api call
they all throw the user authentication
exception which is a simple subclass of
exception
if a user wasn't found in the list
we failed
this should never
ever happen but it's important to test
against such conditions as during a hack
these should never happen conditions
might occur
since the passwords are hashed and
sorted
we can't just compare
regenerating the hash and comparing that
wouldn't work either as salt is random
the only way to test is to use the
matches method
we need to manually set the auth value
as it's not there by default to prevent
a credential leak
the one place where the auth value
should exist is in the login process
when we log in
at first we need to check if the user
with the given phone or social network
exists
the ui flow for users that exist and
don't exist is slightly different
a small piece of the puzzle i skipped
before is the security configuration
class
in spring boot we can use configuration
classes like this instead of xml
which i prefer by far
first we need to disable some oauth and
csrf attack protection
both of these make a lot of sense for
web-based javascript applications which
are vulnerable to attacks
and can use the built-in authentication
but in a native app they just add
complexity and overhead so they aren't
really necessary and can cause problems
if you recall the password encoded from
before
this is the location where we include
the actual implementation of this
encoder
you can place it in any configuration
class but i thought it's fitting to put
it into the security configuration class
so far so good
but the user service is
a server-only class
we'd like to expose this functionality
to the client code to do that we can add
a json-based web service by using user
web the user web service class
notice that this is just a thin layer on
top of the injected user service class
the user service class we throw a
user authentication exception when login
failed
this code automatically translates an
exception of that type to an error dao
object which returns a different error
json effectively
this means that when this exception type
is thrown a
user will receive a forbidden http
response
with the json body containing an error
message of invalid password
maps the
user exists with phone number url so it
will return true or
false strings based on whether the user
actually exists
images just map to our url for the given
image id so the url user slash avatar
slash user id will return the image for
the given user with the mime type image
jpeg if the image isn't there we'll
return an http not found
error 404
which we can handle in the client code
the user update avatar auth token api is
a mime multipart upload request
which we can use to upload an image
a multi-part upload is encoded using
base64 and is the http standard for file
upload
we check whether an id is set
to determine
if this is
an add or an update operation however we
don't use the id value for editing then
internally as the underlying api uses
the token
