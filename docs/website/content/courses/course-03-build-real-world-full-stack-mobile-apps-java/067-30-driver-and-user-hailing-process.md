---
title: "30. Driver and User Hailing Process"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 30
weight: 67
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube sBAiPOnjX6o >}}

## Transcript

the last two big ticket items are
billing and social login
i won't implement them with adherence to
the way they are were implemented with
by uber
i want to keep both of these features
simple as they are both very volatile
features and requirements within both of
these apis can change
literally overnight
i will implement billing as a request
before the write starts
i'll use braintree to do this mostly
because it's already implemented in
codename one
the original implementation in uber
checks whether a billing method already
exists this is possible to do in
braintree but it would require some
extra work to keep billing simple i'll
just charge one dollar per minute and do
the charge in the server side
in-app purchase is one of the big ticket
features in mobile apps
we support this rather well in codename
one
but we can't use enough purchase for
this case
in-app purchase was devised as a tool to
buy virtual goods inside an application
this reduces friction as no credit card
is needed google slash apple already
have it
and makes purchases more fluid
the definition of virtual goods has some
gray areas but generally the idea is
that a good or service sold
would be something that has no immediate
physical cost
good examples of virtual goods would be
in-game item
upgrade of software functionality app
subscription etc
however
physical items and services are
explicitly prohibited
from using in-app purchase
this isn't a bad thing in-app purchase
takes a hefty commission of 30 percent
which isn't viable for most physical
goods sold
braintree is a part of paypal and
provides an easy to integrate mobile
payment sdk for selling physical goods
and services
in theory we could just collect a credit
card and call it a day
but that's naive
securing online transactions is a
nuanced task
by using a trusted third party a great
deal of the risk and liability is
transferred to them
one of the core concepts when working
with the braintree is opacity
the developer doesn't get access to the
credit card
or
billing
information instead a nonce and token
are passed between the client and the
server
even if a security flaw exists in the
app a hacker wouldn't gain access to any
valuable information
as the values expire
this diagram
covers the process of purchasing
via braintree
let's dig into the pieces within it
the client the client code our mobile
app
asks our server for a token
the server generates a token with the
braintree server code and returns it
a client token is a signed data blob
that includes configuration and
authorization information needed by
braintree to associate the transaction
correctly
it can't be reused and should be hard to
spoof in an attack
the mobile app invokes the braintree ui
with the token
that ui lets the user
pick a credit card or other payment
option
for instance paypal android pay apple
pay etc then communicates with
braintree's server
the result of all this is a nonce
which is a unique key that allows you to
charge this payment method
our app
now sends our nonce
to our spring boot server
the server uses the server side
braintree api
and the nonce to charge an amount amount
to the payment method
notice that the amount charged is
completely up to the server
and isn't part of the client-side ui
the braintree sdk for java is pretty
easy to use we already have it in maven
but just in case you skipped those lines
this needs to be in the pom file
next we add a braintree service class
which is remarkably simple
these values should be updated from
braintree and sandbox should be updated
to production once everything is working
this is the client token that we use to
identify the transaction
notice we generate a new one for every
request
we save the nonce into the ride object
this assumes payment authorization
happens before the ride is completed
once the rod is finished the nonce is
instantly available
to do
to perform the charge
before we proceed further the obvious
next step
is the web service to match
it's mostly trivial but i'd like to
point out a small
nuance
pay isn't mapped
we invoke pay in the server so we don't
need to expose it to the client side
that code requires some unexpected
changes which i will get to shortly the
first change was pretty predictable
though we just had to add a non-field to
the right
class
here's the part i didn't expect i needed
to add the right id to the user object
a driver has a reference to the right
object which is why we didn't need this
up until now
however when the user tries to pay he
can't set this anywhere else
unfortunately there is no other place
where the nonce would fit
since it's transient we can't add it to
the user as we'd want some logging
the ride object is the right place for
the nonce
to get this to work i had to make a few
changes to the accept ride method
i added the right reference to both the
driver and passenger for future
reference
i moved these lines downward because the
rider id will only be available after
the ride's save
call since payment is handled on the
server side we can go directly to it
even before we do the client side i've
decided to do this in the finish ride
method
a ride that was finished before it was
started is effectively cancelled a ride
without a nonce can't be charged
at all
i use the route which is
ordered based on time to find the start
time of the ride
i then go to the last element and find
the end time of the ride
assuming the ride has more than one
waypoint otherwise end time would be -1
we can just charge one usd per 60
seconds and payment is effectively done
on the server again i oversimplified a
lot and ignored basic complexities like
the driver forgetting to press finish
