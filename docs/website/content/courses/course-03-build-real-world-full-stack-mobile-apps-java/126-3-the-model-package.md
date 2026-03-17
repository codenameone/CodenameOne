---
title: "3. The Model Package"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a WhatsApp Clone"
module_key: "14-creating-a-whatsapp-clone"
module_order: 14
lesson_order: 3
weight: 126
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 14: Creating a WhatsApp Clone


{{< youtube 46TpE-Cgtw8 >}}

## Transcript

the next step are the other classes
within the model package
the rest is relatively trivial after the
server class
this is the callback interface we used
within the server class it's pretty
trivial i added some methods for future
enhancement too
the first two methods inform the
observer that the server is connected or
disconnected
message received is invoked to update
the ui based on the new incoming message
the last two callbacks aren't really
implemented but they allow us to update
the ui if a user is typing in a chat
the message viewed event similarly
indicates if a user viewed the message
this can provide an indicator in the ui
that the message was seen
chat contact is a property business
object that stores the content of a
specific contact entry
i chose to use unique ids instead of
using the phone as an id
this was something i was conflicted
about
i eventually chose to use an id which i
think is more secure overall
it would also support the option of
changing a phone number in a future in
the future or using an email as the
unique identifier
local id should map to the id in the
contacts this allows us to refresh the
contact details from the device address
book in the future
the phone property is pretty obvious
the photo property stores the picture of
the contact there is a lot to this
property so i'll discuss
it in more details soon
these are the common attributes for name
and tagline used in whatsapp
for simple simplicity i chose to use a
full name and ignored nuances such as
first lost middle initial etc
the token is effectively our password to
use the service
since there is no login process a token
is generated on the server as a key that
allows us to use the service
a chat contact can also serve as a group
i didn't fully implement this logic but
it's wired
almost everywhere
in this case we have two sets for
members of the group
and the admin of the group
these sets would be empty for a typical
user
the this property allows us to mute a
chat contact so we won't see
notifications from that contact
if this is a group then it was created
by a specific user the id of that user
should be listed here
the creation date is applicable to both
groups individual users
this is the timestamp of the last
message we received from the given user
we saw this updated in the server class
we use use this to sort the chats by
latest update
chat message is the property business
object
that contains the content of the message
here we saw the actual chats we had with
the contact or group
as i mentioned before photo is installed
in json when we save the contact to keep
the size low
we save the contact image in a separate
file and don't want too much noise here
the app has two thumbnail images one is
slightly smaller than the other and both
are rounded
to keep the code generic i used arrays
with the detail and then used two sizes
one small size maps to the zero offset
and the array and the large size maps to
the one offset
here are the sizes of these two images
in millimeters
images are masked to these sizes
masking allows us to round an image in
this case
we generate placeholder images which are
used when an image is unavailable
this method creates a mask image of a
given size and pixels
a mask image uses black pixels to
represent transparency and white pixels
to represent the physical visible
opacity
so where we draw a black rectangle image
with a white circle in the center
when we apply this mask to an image only
the portion represented by the white
circle will remain
the placeholder image is used when no
image is defined again we create this
based on size and pixels
we create a gray image and then draw on
it using white
we use the material font to draw the
image of a person onto this image
this method gets the image represented
by the contact
in theory i could have used the photo
property and overridden get to implement
this i thought this is a simpler
approach
here we lazily initialize the arrays of
the mask image
for larger or larger small images
we create the mask images then convert
the mask image to mask object finally we
create the placeholder image
if the photo is null i return the
placeholder image instead of using the
photo object
otherwise we fill the image into the
size of the mask and apply the mask to
create a round object fill scales the
image so it's cropped while filling the
exact boundaries given it doesn't
distort the aspect ratio of the image
like a typical scale operation would
the final public methods and variables
cache the small and large image
appropriately they are the publicly
exposed apis for this functionality
