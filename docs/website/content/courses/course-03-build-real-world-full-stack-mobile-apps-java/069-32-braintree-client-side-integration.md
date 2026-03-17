---
title: "32. Braintree - Client Side Integration"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 32
weight: 69
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube RBYGdCllnww >}}

## Transcript

next we'll address the client side of
the payment process
before we begin we need to download the
braintree cn1 lib from the extension
manager right click the project and
select refresh libs
we'll start the client side with the
payment service class which encapsulates
the web service aspects
payment service has a private
constructor so it can't be instantiated
by other classes
we use the instance of
this class to get callback events from
the client side purchase api
using the purchase callback interface
notice that we need
a ride id in the object instance so we
can communicate purchase results to the
server correctly
this is literally the entire purchase
api process
we just invoke the native purchase ui
and provide the callback instance for
the native code
on purchase success is the first
callback from the callback interface
it occurs when a purchase succeeded
and produced a nonce
we can then send the nonce to the server
with the ride id
on purchase fail or console
aren't very interesting in this use case
i chose to ignore them but you might
need them to know whether that
the charge ui should be shown again
notice the only way to verify purchase
success is on the server
fetchtoken is a callback method
in the callback interface
it's invoked internally by the on
purchase process to fetch the server
token value
that initializes the purchase process
this is pretty much everything
the only remaining piece is binding this
into the ui i've changed the ok button
to pay with cash
and added an option to pay with credit
which essentially maps to the braintree
api
this implements the four payment process
integration
including credit card verification and
everything involved
once this is done payments should now
work both in the client and the server
the user is presented with an option to
pay or use cash
which
just dismisses the dialogue
