---
title: "35. Google Login Process"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 35
weight: 72
is_course_lesson: true
description: "Watch the lesson and follow the accompanying resources."
---
> Module 12: Creating an Uber Clone


{{< youtube JWLsSbbo4N4 >}}

## Transcript

the last piece is the google login
support which we almost finished as the
code we did for facebook is nearly
identical
google again went through some back and
forth originally it was mapped to google
plus functionality
as google phased out google plus we
switched to use the new google login
authentication
the process for google is pretty similar
to the one we had with facebook
first we need to go to the google
developer portal at
developers.google.com
mobile slash ad
and follow the steps to create an app
for google sign-in
notice we need to run through the
process four times once for android and
once for ios and again for the driver
app
the ios version requires the app name
and bundle id which map to the app name
and
package name in codename one
next select the google sign-in option
and click the generate button
once we enable google sign-in
we can generate the configuration files
then we can download and the
configuration file which should be named
googleserviceinfo.plist
place this file in your project root
under the native ios directory
the process for android is nearly
identical with one big difference we
need to provide an sha1 key for this to
work
since i discussed the process of
generating an sha1 key for your
certificate earlier i won't repeat it
again
check out the facebook section for the
details on that process
once we finish this step
we will receive a file named
googleservices.json
we should place this file under the
native slash android directory
in order to work in the simulator we'll
need some additional credentials from
console.cloud.google.com
apis
in the top portion of the browser make
sure the correct app name is selected
select the credentials menu
find the web client entry and click it
you should see the client id and client
secret values there
we will need them in the code soon
in the authorize redirect uri section
you will need to enter the url of the
page that the user will be sent to after
a successful login
this page will only appear in the
simulator for a split second as codename
one's browser component will intercept
this request to obtain the access token
upon successful login
you can use any url you like here
but it must match the value you give to
google connect dot set redirect url
in the code
once all of this is in place we can add
the code to handle google login process
you'll notice that the code is almost
identical to the facebook login code in
fact both google connect and facebook
connect
derive the login class which means we
can write very generic login code at
least in theory
