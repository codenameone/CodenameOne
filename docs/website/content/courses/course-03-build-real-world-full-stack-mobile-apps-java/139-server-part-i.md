---
title: "Server Part I"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Create a Netflix Clone"
module_key: "15-create-a-netflix-clone"
module_order: 15
lesson_order: 2
weight: 139
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 15: Create a Netflix Clone


{{< youtube 7CoD9u6KM2Q >}}

## Transcript

in this lesson we'll go over the basic
server architecture and project lombok
which we use in the server
implementation
we'll start with some basic information
about the server and then dive into
quick lombok overview
so first let's talk about the database
unlike previous modules where i chose to
use mysql
this time around i picked h2 for the
database since it requires literally no
setup
i wouldn't use it for production but
since the whole complexity of mysql is
covered in other modules this just isn't
necessary
lombok is used to make the server code
tiny and devoid of boilerplate this is
pretty cool
we don't really need much in terms of
web services if we had authentication
and authorization there would have been
more i could also implement paging
support and more complex requests for
various segments of the ui but those are
pretty obvious if you've gone through
gone over the feed section of the
facebook loan
the authentication aspect is the big
missing piece here
and i just didn't want to get into it
it's usually one of the more painful
aspects of building a server but since
this is a mobile course i don't think
it's very important to cover again as it
was covered in previous modules
let's start with a palm file this is all
pretty minimal
first we have the standard declarations
we use spring boot 2.2.2 which is the
current version at this time
this code is generated with the spring
initializer which i described before
this declares the h2 database instead of
mysql
and here we declare the use of lombok
which i'll get into shortly
i also added a dependency on apache
commons io which is pretty useful for
small utility calls
lombok is a set of tools
that include ide plugins libraries and
runtime tools they essentially try to
modernize the java syntax using special
annotations and it does a pretty great
job at removing a lot of the common
boilerplate from java 8 syntax their
biggest claim to fame is removing the
getter and setup boilerplate code from
java
in this module we'll use lombok in the
server it works for codename one app
code but we won't touch on that
the main reason that is
that the value of lombok diminishes
thanks to properties so we don't need it
as much
but if you need it you can see this tip
about installing lombok for a codename
one app
let's look at a few examples of using
lombok notice that these examples are
picked directly from the lombok
developer guide
here we have a class with three fields
but only one of them is marked as
non-null
as such we can't construct this object
without the description field as we
would have an invalid object
so we have a private constructor that
accepts this required field to create
the actual instance of this class we use
the of method which accepts the required
description argument
so you would be able to just write
constructorexample.of
description
that's pretty nice
but it took five lines of code
not including curly brace braces or
spaces that's a bit verbose
that can be achieved with one annotation
in lombok
you just define the constructor and the
method name that you wish to add as a
static factory method and voila
it works exactly like the code we saw
before you can literally write
constructed example.of description
the other constructor is for subclasses
it accepts all of the state members and
also makes sure to fail if we try to
violate the not null annotation
notice it's scoped as protected so it
would be used only when deriving the
class
this can be implemented with a single
line of code
the all args constructor annotation does
all of that implicitly it also has an
optional access level property which
defaults to public
the inner class
is pretty simple there isn't too much to
save here but still there's a bit of
vibrosity
we can implement the blank constructor
using the no args constructor
notice that this example is a bit
synthetic normally we would use this
annotation in conjunction with other
constructor options to indicate that we
also want
that option
we already saw non-null
being used before so this example should
come as no surprise this annotation can
also apply to method arguments etc
the method can now assume the variable
isn't null notice that this usage is a
bit stupid as the person.get name call
will throw a null pointer exception
anyway but if you invoke code that might
propagate null
it could be useful
let's move on to another cool feature of
lombok
notice that this code can be improved by
using the java 8 try with resource
syntax so this isn't as beneficial
but it's still pretty cool
this block can be written like this
which is as terse as with
the try with resources code and possibly
even more terse
lombok claims
lombok's claim to fame has always been
getter and sitter elimination so this
whole block of code can be replaced with
this notice that this is still
relatively verbose as we want a
protected setter
so let's see something that's even more
terse
first notice that the setter for age as
package protected access while has
package protected access while the
getter is public
also check out all the boilerplate code
we have for equals and tostring
this can be optimized with some of the
newer objects class methods but not by
much
the boilerplate doesn't end though
we have a hashcode method too
and a non-trivial inner class with a
static creation method
notice that this is the required arc
constructor syntax we mentioned before
that code that includes pages of data
can be achieved using the at data
annotation
it includes getters setters tostring and
hash code implicitly notice you can
explicitly override the definition of a
specific setter from data as we did for
the case of age
another common task is variable
definition
again there is a lot of boilerplate here
so much
that java defined a new val keyword
keyword but this isn't yet available for
java 8 which is used by most of us
lombok added two keywords vel and var
va var
var lets us define a variable that can
change
a mutable variable
val defines an immutable variable
effectively a final variable
there are a lot of annotations we didn't
cover here
at value is the immutable variant of at
data
all fields are made private and final by
default and setters are not generated
the class itself is also made final by
default because immutability is not
something that can be forced onto a
subclass just like data
the tostring equals and code methods are
also generated each field gets a getter
method and a constructor that covers
every argument is also generated
the builder annotation produces complex
builder apis for your classes at builder
lets you automatically produce the code
required to have your class be
instantiatable with code such as
person.builder.name shy.build
notice that this works nicely with the
add value annotation to produce
immutable classes with the builder
pattern
add sneaky throws can be used to
sneakily throw checked exceptions
without
actually declaring this in your methods
throws clause since checked exceptions
are a feature of the java language and
not of the java bytecode this is
technically possible
synchronized is a safer variant of the
synchronized method modifier the
synchronized keyword locks on this
object which is problematic as it
exposes the lock state externally
this annotation implicitly creates a
hidden
dollar lock object and synchronizes on
that object
the next best alternative to a setter
for an immutable property is to
construct a clone of the object
but with a new value for this one field
a method to generate this clone is
precisely what
at with generates
a with field name method which produces
a clone except for the new value for the
associated field
you put at log in your clock
in your class
you
you then will have a static final log
field
initialized as is the commonly described
prescribed way for the logging framework
you use which you can then use to write
log statements
notice that there are a lot of
annotations you can use to describe
explicit log system you want to use in
the project
you can let lombok generate a getter
which will calculate a value once
the first time the scatter is called and
cached
and cache it from then on this can be
useful if calculating the value takes a
lot of cpu or the value takes a lot of
memory to use this feature create a
private final variable initialize it
with the expression that's expensive to
run and annotate your field with at
getter lazy equals true
the field will be hidden from the rest
of your code and the expression will be
evaluated no more than once
when the getter is first called
there are no magic marker values i.e
even
if the result of your expensive
calculation is null the result is cached
and your expensive calculation need not
be thread safe as lombok takes care of
locking
most of this is taken directly from
uh
the
projectlombok.org features slash all
tutorial
but there's a lot more information there
thanks for watching i hope you enjoy the
rest of the course and find it
educational
