---
title: "20. Search Completion Container"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 20
weight: 57
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube T8l7k2OeGpo >}}

## Transcript

we'll continue the search ui
implementation as we step into the
completion container class
the completion container
tries to coordinate the two instances of
the autocomplete address input class
by providing a single class that handles
the completion ui
most of the code in this class should be
very familiar
and some of it is refactored code from
the map form while other code relates to
the suggest suggest location api
we implemented earlier
the name of the class is misleading
completion container is not a container
here instead of deriving i encapsulated
the ui logic and tried to expose only
the business logic
event dispatches allow us to broadcast
events
using the add remove listener observer
style api
we use this dispatcher to broadcast an
event when a user presses a completion
button
this method is invoked when completion
is in progress
it invokes the web service call to
request completion suggestions for a
given string
we fill up the container with buttons if
one of the buttons is pressed we fetch
the location from the web service and
fill it into the autocomplete
address input
we then fire the event dispatcher to
process the actual selection in the ui
we have two types of entries here
one with only one line of text and one
with two lines of text
this is mostly in place to fit the ui
ids correctly with the right underline
behavior
this method is invoked externally
to clear up the content of the
completion ui and show the clean set of
initial options
history is positioned here
so we could later fill this with actual
search history etc
this method constructs and animates the
completion ui into place
notice that we place the content in a
container which we wrap up in a border
layout
this allows us to manipulate the
preferred size without breaking the
scrolling behavior
of the child container
in order to accomplish the design for
the buttons i had to add the where to
button line to uiid
it uses grey text on a transparent
background
padding aligns with the text above
we keep the top padding to zero so it
won't drift away from the first line in
the where to button
margin is zero
as usual
we have an underline here instead of the
underline of the first line of text
we use a slightly smaller font but the
standard main light font nevertheless
