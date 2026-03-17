---
title: "Server Part II"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Create a Netflix Clone"
module_key: "15-create-a-netflix-clone"
module_order: 15
lesson_order: 3
weight: 140
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 15: Create a Netflix Clone


{{< youtube 47WhIiLxv78 >}}

## Transcript

in the third part we'll dive right into
the model objects representing the
server and ultimately the front end code
if you're unfamiliar with entities jpa
uuid etc i suggest going back to the
previous modules and refreshing your
memory a bit as we'll build a lot on top
of that
one way in which this will be different
though is the usage of lombok which will
make our code far more tears
still the code here is mostly mock the
real world netflix has a lot of code but
most of it applies to algorithmic
scheduling user management scaling etc
all of these aren't applicable here
in this lesson our focus will be on
entities and data transfer objects also
known as dtos which are sometimes mixed
with data access objects or dowels
there is overlap between both of these
concepts but what we have is generally
dtos is they transfer data to the client
they don't just abstract the database
layer
writing an entity with lombok is much
easier
there are no getters setters
constructors equals hash codes etc
notice we still use jpa just like we
used to so
we have the jpa entity annotation
and then the lombok annotations
everything works as you would expect
including the primary key definition etc
notice i chose to go with uuid object as
a primary key coupled with auto
generation
that's a much simpler trick than the one
i picked in previous modules
we already talked about using strings
for keys when we use a uuid object we
get a long string that isn't guessable
in the database
that means we can expose that primary
key to the end user without worrying
that he might use it to scan through
details of other users
as the string is pretty long and hard to
guess
as we saw we just need to use the uuid
object type there are several other
strategies for generating a uuid and jpa
i chose the simplest one it might not be
the best one but it is convenient
so why doesn't everyone use this
approach turns out it's much slower than
using numeric auto increment values on a
database column
databases such as mysql are heavily
optimized for auto increment fields and
string based primary keys are just
slower to insert
some developers consider that a
non-starter especially when looking at
performance graphs which
is scary
performance really takes a dive for
instant operations
while it remains flat when using long
auto increment fields
personally i don't think that's a
problem even for video app like this you
wouldn't insert too often and read
operations are still pretty fast
this might become an issue if you have a
log or auditing table that might include
multiple insert operations per second
at that point you need to use a long for
that for the primary key and make sure
never to expose it externally
the name and description fields
correspond to these fields in the
database
this is the entire definition as the
excesses are generated automatically
we have three one-to-one media relations
these include the three images for every
content item specifically
the hero image which is the big picture
that appears on top of the application
the show logo is displayed on top of the
hero image it's a separate image to
support different device aspect ratio
and layout
and the icon is the image representing a
show within the list
finally we have the actual video files
which we store in media objects as well
we have multiple video files
representing different quality levels of
the video
in real life we can have even more
options such as different aspect ratios
languages etc
normally i would like
this to be a map between quality and
media
but this is a bit challenging to
represent correctly in jpa so i left
this as a simple set
for convenience we place the dto
creation within the entity object
this code is mostly just the
construction
but it's uh it
it's there's
one block where we convert the media
object
if the dto
in the dto it makes more sense to hold
the media as a map instead of a list or
set so we translate the video to a map
i find the stream syntax a bit obtuse
sometimes this is how it would look with
a standard for loop
essentially for each element we replace
the content with a map where the key is
the quality and the value as the media
url
once this is done we create a new dto
object with the automatic constructor
and return it
and finally i also added a small helper
method to make the code above a bit
simpler so we won't get a null pointer
exception if the media is null
this is the dto object we just created
notice it's super simple and mostly
consistent
consists of the lombok annotations
the strings just map directly to the
entity there's nothing to say here
for the media i chose to include the
icons themselves i could have taken the
approach of returning urls for the media
which might have advantages in the
future for now this is simpler but
possibly not as efficient
using a url would have had the advantage
of caching the data locally for future
refreshes
using the actual icon means all the data
is transferred with one request
this is the map we created for the media
items we already discussed this in the
stream part before
it maps between the video quality enum
and the string
url for the sake of completeness this is
the video quality enum
pretty simple but matches what we need
right now
the media entity is another standard
lombok entity with the standard
trimmings
we use the same uuid primary key
generation logic
rest of the stuff is pretty standard
notice that we store the modified time
as an instant instead of date
instant is a java 8 date time api class
it represents a timestamp and is more
convenient to use than date
the media data is stored in blob storage
in the database
finally the url to the media and the
video quality enum are stored as well
that means we can have multiple
instances of the same media object for
various quality levels
one thing i didn't cover here is the
repositories for the entity objects
they're all empty as we don't need any
finder methods for this specific demo so
it's all pretty trivial
thanks for watching i hope you'll enjoy
the rest of the course and find it
educational
