---
title: "7. Bubble Border"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a WhatsApp Clone"
module_key: "14-creating-a-whatsapp-clone"
module_order: 14
lesson_order: 7
weight: 130
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 14: Creating a WhatsApp Clone


{{< youtube jR7_OTg-aG0 >}}

## Transcript

while we are on the subject of theming
there is one missing piece we neglected
in the theme css
the chat bubble border implements the
chat bubble appearance
it extends the border class and is based
on the code of round rect border from
codename one
i won't go into the whole code as there
is a lot here
in fact it's pretty similar to the
special border i created for the uber
clone module
so i'll just review the changes i did
for this class
i added two flags to indicate whether
this border has a left pointing arrow or
a right pointing pointed arrow
these variables are exposed using setter
methods like the rest of the set of
methods in this class
the create shape method is where we do
the actual change to implement the arrow
support
the arrows are drawn by moving the pen
further to the side to draw the
respective arrows
