---
title: "9. The New Message Form"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a WhatsApp Clone"
module_key: "14-creating-a-whatsapp-clone"
module_order: 14
lesson_order: 9
weight: 132
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 14: Creating a WhatsApp Clone


{{< youtube 0ETli_N__ZY >}}

## Transcript

the final part discussing the
client-side code covers the new message
form class
New Message Form
new message form
is a form we see when we press the
floating action button
in the main form
the class is trivial by comparison to
previous the previous class it's just a
box layout y form that lets us pick a
contact we wish to chat with
the new group and contact buttons aren't
currently mapped to anything they're
just simple buttons
List of Contacts
we use the fetch contacts method to
fetch the list of contacts to show here
for every contact in the list of
contacts we create a multi button
matching the name and icon
Contact Check
if a contact is clicked we check if he
has an id
if not this is someone that might not be
in the app yet
so we need to contact the server and
check
the find registered user
finds the specific user based on his
phone number
if we get null as a result it means this
is no such registered user in the app
we go back to the previous form and show
a toast bar message there
if there is we update the user id and
save
we can then launch the chat form with
this new contact
Launch Chat Form
since the multi buttons are added
asynchronously we need to revalidate so
they will show on the form
as i said this is a super simple short
class and with that we finished the
client side work
