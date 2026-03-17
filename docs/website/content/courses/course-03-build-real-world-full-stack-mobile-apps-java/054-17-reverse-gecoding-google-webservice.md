---
title: "17. Reverse Gecoding Google Webservice"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 17
weight: 54
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube A72rY4rU7E0 >}}

## Transcript

we finally have a client server
application that works
but we don't have any real functionality
to get to that point we need some web
services
WebServices from the Device?
i chose to invoke the google web
services from the device code instead of
using our server to proxy the calls
i did this because this is a mobile
programming course not a server
development course
this makes sense for a simple project
and is arguably faster as i connect
directly to google
however if i was building a real world
application i would have directed all
requests to our server and invoked
google from there
this might have a performance penalty
but it's worth it
here are some of the advantages
api keys would be stored on the server
and would thus be more secure from theft
we can cache queries and reduce api
costs between users
currently i can only cache calls per
user but if lots of people are searching
for the same things
this can add up
i can use analytics and statistics in
the server code
and notice patterns about user behavior
and finally i can switch to a different
company for the web service
implementation to get a better deal
if google discontinues its service i
don't have to ship an app update either
Googles GIS Services
we'll use
four google location-based web services
to implement the functionality of this
app
with geocoding we provide an address or
location and get back a location on the
map
this is useful when a driver receives a
set of locations and need to know
the actual map coordinates
reverse geocoding
is the exact opposite
it provides the name of a given location
this is useful for pointing a pin on the
map and naming the location
directions provides
directions trip time etc we can get the
points of the path and plot them out on
the map
the places api allows searching for a
place similar to the geocoding api
the auto complete version
lets us type into a text field and see
suggestions appear
we'll use it for the search
functionality
New Fields in Globals
all of these apis require developer keys
which you can obtain from their
respective websites
i've edited the globals class to include
these new keys required by the three
apis
make sure to replace the dashes with the
relevant keys
you can get the keys by going to the
directions geocoding and places websites
and follow the process
there
we use the maps api geocode json url for
reverse geocoding
google provides this example for usage
of the api
it's just a latitude longitude pair
and your api key
Reverse Geocoding - Result
the result of that url
look like this response json
let's go over two important pieces
we need to get this result array from
the response
we only care about the first element and
will discard the rest
this is the only attribute we need at
this time from this api
now that we know what we are looking for
let's look at the code that accomplishes
this
SearchService
i'll use the search service class to
encapsulate this functionality
for each of these services
there is an edge case where location
isn't ready yet when this method is
invoked
in this case i found it best to just do
nothing
usually it's best to fail by throwing an
exception
but that is a valid situation
to which i have a decent fallback option
so i prefer doing nothing
if we send two such calls in rapid
succession i only need the last one
so i'm canceling the previous request
the reverse geocode api
latitude long argument
determines the location
for which we are
looking
we get the past result as a map
containing a hierarchy of objects the
callback is invoked asynchronously
when the response arrives
this gets the result list from the json
and extracts the first element from
there
we extract the one attribute we care
about
the formatted address entry and invoke
the callback method with this result
the places autocomplete api is a bit
more challenging since this api is
invoked as a user types
we'll need the ability to cancel a
request just as we would with the
geocoding calls
caching is also crucial in this case
so we must cache as much as possible to
avoid overuse of the api
and performance issues
let's start by reviewing the api url and
responses
the default sample from google wasn't
very helpful so i had to read the docs a
bit and came up with this url
the search is relevant to a specific
location and radius
otherwise it would suggest places from
all over the world which probably
doesn't make sense for an uber style
application notice the radius is
specified in meters
the input value is a the string for
which we would like
autocomplete suggestions
Places Autocomplete - Result
this request produces this json result
all predictions
are again within an array but this time
we'll need all of them
the ui would require the text broken
down so we need the main text
and we'll need the secondary text to
we'll also need the place id and the
reason for this is a huge omission in
this api
notice it has no location information
we will need the place id value to query
again for the location
SuggestionResult
before we move on to the code
will need a way to send the results back
we can do that with a list of suggestion
result entries
this is a pretty trivial class and
doesn't require any explaining
the class solves the issue of getting
the location for a specific entry
with the method get location
i won't go too much into the details of
that code above since it's
very similar to the code we saw before
we just get additional details about a
place and parse the results
notice that this is a part of the
suggestion result class
so we don't invoke this unless we
actually need the location of a place
there is one last thing we need before
we go into the suggestion method itself
we need variables to cache the data and
current request
otherwise multiple incoming requests
might collide and block the network
we need the last
suggestion request so we can cancel it
the last suggestion value
lets us distinguish duplicate values
this can sometimes happen as an edit
event might repeat a request
that was already sent
for example if a user types and deletes
a character
this can happen since we will wait two
500 milliseconds before sending
characters
the location cache
reduces duplicate requests notice that
this can grow to a level of a
huge memory leak
but realistically that would require a
huge number of searches
if this still bothers you we can have
the cash map class
that serializes extra data to storage
