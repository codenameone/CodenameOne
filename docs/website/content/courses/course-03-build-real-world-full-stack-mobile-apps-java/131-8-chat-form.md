---
title: "8. Chat Form"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a WhatsApp Clone"
module_key: "14-creating-a-whatsapp-clone"
module_order: 14
lesson_order: 8
weight: 131
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 14: Creating a WhatsApp Clone


{{< youtube 65jD9oGw61w >}}

## Transcript

the next step is the actual chat form
the chat form is a form like the main
form we discussed earlier
a chat with a specific contact which is
passed in the constructor as we saw in
the previous main form
we also have a getter for this property
which we used in the main class to check
if the current chat contact matches the
one in the incoming message
this is a common constant i use a lot to
represent a single day in milliseconds
it's convenient it's probably probably
should have been static as well
this is the date format used to
represent the day label here
this indicates whether the blue label
that says today
was added
if not
when we type in the first message to we
need to add a label
indicating that this message was sent
today
the chat form uses the y last box layout
this is a special type of box layout y
where the last component is pushed to
the bottom of the form
so here
we will use that position for the text
field
historically i used to build apps like
this with border layout and place the
text field in the south
both approaches are good with different
trade-offs for each
this approach has the downside of hiding
the chat text field when scrolling up
which is inconsistent with where
whatsapp works
so you might want to revisit this with a
border layout if you want that behavior
we use the chat form uiid for the
background image of the form
here we use a standard title with the
back arrow to navigate to the previous
form
these are all the standard comment
commands that appear in the whatsapp
application
i didn't implement them and just added
them to create a similar design
the input container is this
we just add it to the box layout y since
it's in the last mode
it will glue itself to the bottom as
long as it's the last component
from now on we'll need to add all
components before that component in the
container
now we go over the existing chats so the
ui opens
with the content of previous chats
stored on the phone
each chat has a time
time is represented in milliseconds
since epoch which uh is january 1st 2009
sorry 1970.
we use that to divide the time by
constant the constant we have for day
this will give us a number that
represents a day since 1970.
we can then check if the time from one
chat is from the same day or from a
different day this can be tested with a
simple not equals test
notice i could have used code based on
java util calendar to do this test but i
think this approach is both faster and
simpler
in both cases we use add date to add
this label before we add the chat
content for a specific day
we then add a message to the ui
normally when we add a component to the
ui we need to animate or revalidate the
ui so it will show the component
however since this code is running
before the form is shown we don't want
such an animation as it might collide
with the transition
the show listener is invoked when a form
is actually shown
here we can do things that will happen
only after the form becomes visible
notice i used an anonymous inner class
instead of a lambda expression here
this is the reason
we can remove the show listener using
this command
if it's an anonymous inner class but we
can't remove it if it's a lambda as this
will map to the chat form instance
this is why we need this listener in the
first place
this scrolls down to the last component
it's important to do this after the form
is shown
because if it's done before some
components could still be laid out
incorrectly
the add day method adds the day label if
necessary there are some nuances to this
beyond the label alone
first we need to check if today is the
current day
since every day has a number associated
with it as we discussed before this is
pretty easy
if the day is today then we need to add
that special case label
we also need to flip the flag indicating
that it was added
this boolean flag is a special case that
we will discuss later
there is another slightly simpler
special case for yesterday showing the
previous day with a special matching
label
otherwise we use the formatter we
declared at the top of the class to
generate the text of the day label
we create a label with the right style
and add it before the text input
component
which brings us to the input container i
mentioned before
this method creates that container which
includes everything in this line
including the microphone button
the container starts with a text field
where the messages can be typed
there are two important features we need
in this text field first we don't want a
border
we'll set the border to the parent
container which we defined as
a pill border in the css
second we want this to be a multi-line
text field
this allows us to type in longer
messages and review them
however it means that enter won't map to
sending
the done listener is the first way to
send a message it can be triggered by
using the device virtual keyboard when
pressing done
this adds the message to the ui and it
does that with an animation that lays
out the message on the form
after adding the message we want to
clear the text so we stop the editing
process and set the text to an empty
string
this would have worked had we not
stopped the edit wouldn't have worked
had we not stopped the editing
these are the three buttons we see next
to the text field here
the text field is between them and not
under them
it just has no border
the round pill border is a container
that surrounds the buttons and the text
field
i'll talk about attachments soon this
isn't fully implemented but i'll cover
this when we reach these methods
the input container wraps these three
buttons and the text field
we give it the chat text field uiid
which includes the perl border
finally microphone has the record button
design
there is a partial partial worker for
voice recording when we override pointer
pressed we can do use the media recorder
to start voice recording and stop stop
it in pointer released i eventually
didn't get around to doing that
if there is text in the input field the
microphone icon becomes a send icon here
we use the data change listener to track
that and update the icon on the fly
the action listener is only applicable
when the microsoft microphone is in send
mode it's used to send the text in the
text field
this code is effectively identical to
the code we saw earlier in the method it
might make sense to generalize this
block
finally this method returns a border
layout that pairs together the input
container and the microphone as a single
container
this is a utility method i use quite
often
to write a number as two digits it's
useful for formatting hours so a number
like
1 will be written as 0 1 and etc
the logic is trivial if a number is
smaller than 10
return it with a 0 prepended to it
otherwise return the number as a string
i
make use of this method in the time
formatting method below
this method formats the time as hours
and minutes used to display the hour
next to the message
this method just gets the hour and
minutes from the calendar class and
formats them as two digits with a colon
in the middle
we saw the add message method invoked
when the user types a new message
it's pretty simple as most of the logic
in this method
is
delegated to the following add message
to ui no animation method
but it does contain a couple of
interesting bits
this is why we have the today
added flag
if this flag isn't set we need to add
today to the dates before adding a new
message
this method does the heavy lifting of
adding a message to the ui
here we animate the addition notice i
used an and weight variant of the method
it's so the scroll component to visible
below will work this won't work during
the animation as the position wouldn't
be correct at that point
this is the method that actually adds
chat bubbles to the ui
this is the logic to determine if the
bubble goes
in the right or left i
use this logic
so it will work even with groups
i could have simplified it if there were
only two options
the main difference between the left and
right chat bubble is the ui id as i
mentioned
in the theme
the actual component placed in the chat
is created by a separate method
if this is a media component we'll call
create media message
and if it's a text message we use create
text message the rest of this method
styles the result of that method
the component uses the chat bubble
however we need to know whether the
component should have an arrow or not
notice that if we have two chat bubbles
one on top the other
and and the other from the same sender
the second bubble doesn't have an arrow
this is true for both sides of the
conversation
however if we have a message from
someone else
then the arrow returns
the this block tries to implement that
logic by detecting this situation
the first condition is meant to detect a
chat with no components in it in this
case the arrow must be shown so we can
easily set the arrow on the correct side
by using the left boolean variable to
determine that
otherwise
we need to dig through the components
we already added the last component is
the input component so we need to look
at the components before that
to align chat bubbles correctly we use
container wrapped
wrapper so the previous container is a
container
we exact the child
the first child of that container and
save it into cnt
cnt is now the previous component if the
ui id of the previous component matches
our current ui id it means that the
previous component
pinpointed to the same side
pointed to the same side that means we
don't need an arrow
if the uids aren't identical the arrow
needs to point
into one of these directions
this is another special case
when the arrow is removed we also want
less space between the chat bubbles this
is a small nuance of the app
that helps associate
uh the components as part of the same
conversation
we wrap the component in a flow layout
so it will align correctly to the left
or right side
we set the ui id to the ui id we picked
at the beginning of the method
we add the component
to the offset just before the input
component
we stole the message itself
we store the message itself as a client
property in the component
this makes it easier to implement
features such as search as we can easily
determine the business object related to
a specific component
finally we set the border to the
component and return the container
instance
this version of the method does all of
that but also sends the message to the
server
this is the version of the method we
invoke when a user types a message
this is the method that creates the
components for a text message
as you recall we have two methods one
for text and one for images
we use a text area to represent the text
it's more direct than span label which
is technically just a text area wrapped
in a container
act as label removes some optimizations
from the text area
so the text is laid out accurately
it can make a it's slightly slower
to render though
we block editing and focus so a user
can't interact with the text area
we set the ui id to chat text so it will
use the right font and colors
time is the label with the current time
for this message
it uses the chat time style
time is the label with the current time
for this message it uses the chat time
style
notice that short messages place the
time next to the text whereas long
messages place it below i chose to use
the 30 character mark to decide this
a better strategy might have been to
calculate preferred size in some way and
decide based on that but i wanted to
keep things simple so i chose this
approach
finally we returned the bubble container
that includes the text and the time
the media message is similar but it uses
the media file url right now the method
is designed only to work with images but
this can probably be fixed relatively
easily
we open a fast stream for the media
we calculate a desired size for the
media which is half the size of the
portrait screen
we load the image then scale it into a
button as the icon of that button
once there we enclose it in a layered
layout so the time is overlaid at the
bottom right of the layered layout
as you might recall the open camera
method was mapped as a listener to this
button
it's a simple method that invokes
capture and then adds the result as a
message
the last method in the class is open
file which is effectively identical
it uses the file chooser cn1 lib api and
maps to the attach button
it allows opening all files but in order
for this to work we need to process all
file types
however i only implemented image support
as i did in the previous method
