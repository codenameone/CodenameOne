---
title: "Server Part III"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Create a Netflix Clone"
module_key: "15-create-a-netflix-clone"
module_order: 15
lesson_order: 4
weight: 141
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 15: Create a Netflix Clone


{{< youtube vLBQhAJ6aTk >}}

## Transcript

in this final installment covering the
server we'll go over the service classes
and the final entity representing the
content creation
the content collection is an
oversimplification of the concept
a site like netflix would probably
generate this data dynamically based on
user viewing preferences and complex
heuristics
i just hard coded an entity which was
simpler
the service class doesn't do anything
other than create the built-in data and
implements the basic service call
the content collection starts similarly
to the other entities
again it uses a uuid as an identifier at
the moment will only have one content
collection but in theory there can be as
many as there are users
the lead content represents the show
that appears on the top of the ui in
this case it's the stranger things
header
these represents the rows of content
below that they contain the popular
recommended and personal list of shows
next we have
the method that returns the dto
since all lists are effectively lots of
content
we use the same method to convert
everything
again we make use of java 8 streams to
get all the dtos from the list by
invoking the getdto method on every
element
now we're getting to the web service
code
we're using the request mapping
attribute to specify that this is a web
service on the video path
the rest controller attribute designates
this as a simple json api so a lot of
common sense defaults follow
for instance response in the body etc
notice the all args constructor this
means the class has a constructor that
accepts all arguments no default
constructor this is important
notice the final field for the video
service
it's passed via the constructor
notice that the video service doesn't
have the autowad annotation
we usually place for beans in spring
boot this is called constructor
injection and it has a few advantages
normally it's a bit too verbose as we
need to maintain a constructor with all
the injected beans
but in this case lombok makes it
seamless
in other words lombok and spring boot
inject the video service being pretty
seamlessly for us
we only have one api in the server it
returns the
content json
a more real world api would also have
authentication identity apis and maybe a
state querying submitting api
for instance view positions statistics
etc
but those are relatively simple and we
were covered by other modules
here so i'm skipping them for now
the service class is similar to the rest
api class i used the required rx
constructor which is effectively the
same as all arcs constructed in this
case it creates a constructor for all
the required args specifically all the
final fields
this again works for creating
constructor based injection
this class is also transactional as it
accesses the database
we need access to all the repositories
to create the entities
this is a simple utility method to read
bytes from a stream
in the class path notice the usage of
the add cleanup annotation from lombok
and apaches i o util
api
post construct is a feature of spring
boot that lets us invoke a method after
the
container was constructed
this is effectively a constructor for
the entire application
here we can initialize the app with
default data if necessary
notice i
throw an exception here since i assume
this method won't fail
it's the first launch so it's core that
it succeeds
it's pretty easy to detect the first
launch
if the database is empty
the count method on the repository will
return 0
elements in that case we need to
initialize the database
in this large method i set up the
initial data in the database i prefer
doing it through code rather than
manually populating the database and
providing a pre-filled one as it's
easier to do when working in a fluid
environment where you constantly wipe
the database
in this case i just take the hard-coded
images and get their byte array data
i then create media objects for all the
thumbnail entities
the rest is pretty self-explanatory
eventually all the videos are created
and all the media entities are added
notice the urls are to an external video
sample site i was able to find online
this is consistent with the way a video
site would work your actual content
would be hosted on a cdn for performance
also notice i didn't get into the whole
process of encryption encryption and
complex drm streaming that's a whole
different level of complexity
finally the last bit of content is added
to the content repository and everything
is saved to the database
this is the entire server api this
returns a json structure used in the
client we could stream this in smaller
blocks but that was already covered in
the facebook demo so i skipped it here
thanks for watching
i hope you'll enjoy the rest of this
course and find it educational
you
