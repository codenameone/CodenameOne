---
title: "5. Main Form"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a WhatsApp Clone"
module_key: "14-creating-a-whatsapp-clone"
module_order: 14
lesson_order: 5
weight: 128
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 14: Creating a WhatsApp Clone


{{< youtube csTtSj6TqRE >}}

## Transcript

now that we implemented the model code
and the lifecycle code we are almost
finished we are down to ui code and css
we start with the main form which covers
the list of chat elements
as is the case with all the forms in
this app
we derive from form for simplicity
the main body of the form is a tabs
component that allows us to switch
between camera status and calls
camera kit is used to implement the
camera ui but due to a regression in the
native library this code is currently
commented out
there are the three tab containers
we make use of them in the scrolling
logic later
the main form is a singleton as we need
a way to refresh it when we are in a
different form
the form itself uses a border layout to
place the tabs in the center
we also save the form instance for later
use
we hide the tabs it generally means that
these aren't actually tabs they are
buttons
we draw ourselves
the reason for that is the special
animation we need in the title area
we add the tabs and select the second
one as the default as we don't want to
open with the camera view
instead of using the title we use title
component which may takes over the
entire title area and lets us build
whatever we want up there
i'll discuss it more when covering that
method
the back command of the form respects
the hardware back buttons and some
devices
and the android native back arrow
here we have custom behavior for the
form
if we are in a tab other than the first
tab we need to return to that tab
otherwise the app is minimized
this seems to be the behavior of the
native app
the calls container is a y-scrollable
container
this is simply a placeholder i placed
here a multi-button representing
incoming outgoing calls and a floating
action button
the same is true for the status
container this isn't an important part
of the functionality with this tutorial
you might recall that we invoke this
method from the main ui to refresh
the ongoing chat status
we fetch up to
date data from storage this is an
asynchronous call that returns on the
edt so the rest of the code goes into
the
lambda we remove the old content as
we'll just read it
we loop over the contacts and for every
new contact we create a
chat multi button with the given name
if there is a tagline defined we set
that tagline we also use the large icon
for that per person
if the button is clicked we show the
chat form for this user
the chats container is the same as the
other containers we saw but it's
actually fully implemented
it invokes the refresh chats container
method we previously saw previously saw
in the order in order to fill up the
container
and the floating action button here is
actually implemented by showing the new
message form
[Music]
camera support is currently commented
out due to a regression in the native
library however the concept is
relatively simple
we use the tab selection listener to
activate the camera as we need
the overflow menu is normally
implemented in the toolbar but since i
wanted more control over the toolbar
area i chose to implement it
manually in the code
i used buttons with the command uiid and
container with the command list uid to
create this ui
i'll discuss the css that created this
in the next lesson
i create a transparent dialog by giving
it the container ui id
i place the menu in the center
the dialog has no transition and
disposed
if the user taps outside of it or uses
the back button
this disables the default darkening of
the form when a dialog is shown
this version of the show method places
the dialog with a fixed distance from
the edges
we give it a small margin on the top to
take the state status bar into account
then use left and bottom margin to push
the dialog to the top right side
this gives us a lot of flexibility and
allows us to show the dialogue in any
way we want
this method creates the title component
for the form
which is this region
the method accepts the scrollable
containers in the tabs container this
allows us to track scrolling and
seamlessly fold the title area
the title itself is just a label
with a title ui id
it's placed in the center of the title
area border layout
if we are on ios we want the title to be
centered
in that case we need to use the center
version of the border layout the reason
for this is that center alignment
doesn't know about the full layout and
would center based on available space
it would ignore the search and overflow
buttons on the right when centering
since it isn't aware of other components
however using the center alignment and
placing these buttons in the east
solves that problem and gives us the
correct title position
a search and overflow commands are just
buttons with the title and the title ui
id
we already discussed the show overflow
menu method so this should be pretty
obvious
we just placed the two buttons in the
grid
i chose not to use a command as this
might create a misalignment for the this
use case and won't have saved on the
amount of code i had to write
these are the tabs for selecting camera
chat etc
there are just toggle buttons which in
this case are classified as radio
buttons
this means only one of the radio buttons
within the button group can be selected
we give them all the subtitle ui id
which again i'll discuss in the next
lesson
we use table layout to place the tabs
into the ui this allows us to explicitly
determine the width of the columns
notice that the table layout has two
rows
the second row of the table contains a
white line
using the side title underline ui id
this line is placed in
row one and column one so it's under the
chats entry
when we move between tabs this underline
needs to animate to the new position
here we bind the listeners to all four
buttons mapping to each tab
when a button is clicked we select the
appropriate tab
the next line two lines implement the
underline animation effect that we see
when we click a button notice how the
line animates to the right tab button
to achieve this we remove the current
white line
and add it back
to the toggle container in the right
position
we reset the height of the title in case
it was shrunk during scrolling
and we finally update the layout with an
animation which performs the actual line
move animation
the previous block updated the tab
selection when we select a button
this block does the opposite
it updates the button selection when we
swipe the tabs
it uses
a tab selection listener if the button
isn't selected then we need to update it
[Music]
again we need to reset the title size
next we select the button that matches
the tab
[Music]
finally we perform the animation of
moving the underline between tabs notice
that this is almost identical to the
previous animation code only in this
case it's triggered by dragging the tabs
instead of the button click events
the last two lines in this method are
the bind folding call which we'll
discuss soon and the box layout y which
wraps the two containers as one
the bind folding method implements this
animation of the folding title it's
implemented by tracking pointer drag
events and shrinking the title
when the pointer is released we need to
check if the title shrunk enough to
minimize it minimize or not enough so it
would go back to the full size
if the title area height is different
from the original height it means we are
in the process of shrinking the title
in that case we need to decide whether
the process is closer to the finish line
or to the start
it's less than halfway to the height of
the title we reset the preferred size of
the title area that means the title area
will take up its original original
preferred size and go back to full
height
otherwise we set the title area height
to zero so it's effectively hidden
regardless of the choice we made above
we show it using an animation
we detect the drag operation by binding
a scroll listener to the three
scrollable containers
i could have used pointer drag listeners
but they might generate too much noise
that isn't applicable
[Music]
i chose to make a special case for the
tensile drag effect
the tensile effect
in is the ios scroll behavior where a
drag extends beyond the top most part
then bounces back like a rubber band
this can cause a problem with the logic
below so i decided that any scroll
position above 10 pixels should probably
show the full title
now that all of that is out of the way
we can calculate the direction of the
scroll and shrink or grow the title area
appropriately
if the diff is larger than zero then the
title area should grow
we're setting the preferred height to
the diff plus the preferred height
but we make sure not to cross the
maximum height value
we then revalidate to refresh the ui
a negative diff is practically identical
with the exception of making it zero or
larger instead of using the minimum
value we use the max method and with
that the title folding is implemented
the one last method in the class is this
we use a custom toolbar that disables
centered title
the centered title places the title area
in the center of the ui and it doesn't
work for folding
we need to disable it for this form so
the title acts correctly on ios
