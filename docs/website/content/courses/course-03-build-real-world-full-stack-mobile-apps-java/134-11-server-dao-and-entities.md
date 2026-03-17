---
title: "11. Server DAO and Entities"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a WhatsApp Clone"
module_key: "14-creating-a-whatsapp-clone"
module_order: 14
lesson_order: 11
weight: 134
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 14: Creating a WhatsApp Clone


{{< youtube BSj3KRM5Sj0 >}}

## Transcript

let's continue with the other entities
i'll skip media as it's just a copy and
paste of the facebook media class and
partially implemented to boot
chat group is an entity that traps the
concept of a group
in a sense it's similar to the user
entity but has no phone
all of these properties the id name
tagline creation date and even avatar
are a part of a group
however unlike a regular user a group is
effectively created by a user
a group also has two lists of group
administrators and group members
the chat group repository is empty as
this is a feature we didn't finish
a chat message must be stored on servers
for a while
if your device isn't connected at this
moment for instance flight
the server would need to keep the
message until you are available again
it can then be purged
if we wish to be very secure we can
encrypt the message based on client
public key that way no one
in the middle could peek into that
message
this should be easy enough to implement
but i didn't get around to to it
it would mostly be client-side work
here we store message messages so they
can be fetched by the app
like everything else we have a unique
string id per message
every message naturally has an author of
that message
but most importantly a destination which
can be either a different user or group
not both
every message has a date timestamp for
when it was sent
the message has a text body always
it can be now though
if we have a media attachment to the
message
the ack flag indicates whether the
client acknowledges acknowledged
receiving the message
this works great for one-to-one messages
but the message sent to a group would
probably need a more elaborate ack
implementation
the dao is again practically copied from
the facebook app
we can include the ids for the
attachments as part of the message
the chat message repository includes one
fine method which helps us find messages
that we didn't acknowledge yet
if a specific user has pending messages
this finder is used to locate such
messages and send them again
the message dao entry represents the
current message
it maps pretty accurately to the entity
object
with that effectively done with the
entity and dow layers
there is still an error dial but it's
effectively identical to what we had in
the facebook app
and it's totally trivial so i'll skip
that
