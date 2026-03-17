---
title: "18. Directions and Places Google Web Services"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 18
weight: 55
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube KhSWyE6rAN8 >}}

## Transcript

we can now move forward to the last two
pieces of the search service class
if the last request is this request
which can happen as a user types and
deletes etc
then we don't want to do anything
let the last request finish
however
if it isn't then we want to kill the
last request which might still be queued
and blocking us from going forward
we check if an entry is already in the
cache
as users might type and revise a lot
thus triggering significant web service
cost overhead
we clean the variable values
and then invoke the response
notice i use call serially
in this case to defer the response to
the next cycle
if we call back immediately we might
delay the input code which is currently
in place
by shifting the callback to the next edt
cycle we guarantee that suggest
locations will behave in a similar way
whether the data is cached locally or
not
the request extracts the predictions
array so we can construct the result
list
we iterate over the entries
notice i discard the generic context
which is legal in java but might produce
a warning
i could have used a more elaborate
syntax that would have removed the
warning but that would have created more
verbus code with no actual benefit
i extract the elements from the map
and create the suggestion result entries
then store the whole thing in cash
followed by the on success call
notice that this in this case i didn't
need the call serially since the
response is already asynchronous
the final web service api we will cover
is the directions api which will allow
us to set the path taken by the car on
the map
the directions api is challenging it
returns encoded data in a problematic
format
this is the sample query from google
notice we can give the origin and
destination values as longitude latitude
pair
which is
what we'll actually do
the response is a bit large so i trimmed
a lot of it to give you a sense of what
we are looking for
the one thing that matters to us from
the response
is the overview polyline entry which
seems like a bunch of gibberish but it
isn't
this is a special notation from google
that encodes the latitude longitude
values of the entire trip in a single
string
this encoding is described by google
in their map documentation
being lazy i found someone
who already implemented the algorithm in
java
and his code worked as is
i won't go into the code since it's
mostly just bitwise shifting to satisfy
requirements from google
the method signature is the only thing
that matters
it takes an encoded string and returns
the path matching that string
as a list of coordinates that we will be
able to add into the map
shortly
now that this is all out of the way the
directions method is relatively simple
this method is just another rest call
that doesn't include anything out of the
ordinary we extract the overview
polyline value and pass it to the
callback response
