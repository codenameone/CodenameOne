---
title: Properties
slug: properties
url: /blog/properties/
original_url: https://www.codenameone.com/blog/properties.html
aliases:
- /blog/properties.html
date: '2016-11-21'
author: Shai Almog
---

![Header Image](/blog/properties/properties.jpg)

We usually just add a new feature and then tell you about it in these posts but properties is a special case and this  
post is intended not just as a tutorial but as a solicitation of feedback…​  
We committed properties as a deprecated API because we aren’t  
sure yet. This could be a very important API moving forward and we want as much peer review as possible over this.

### What are Properties?

In standard Java we usually have a POJO (Plain Old Java Object) which has getters/setters e.g. we can have a  
simple `Meeting` class like this:
    
    
    public class Meeting {
         private Date when;
         private String subject;
         private int attendance;
    
         public Date getWhen() {
            return when;
        }
    
         public String getSubject() {
            return subject;
         }
    
         public int getAttendance() {
            return attendance;
         }
    
         public void setWhen(Date when) {
            this.when = when;
        }
    
         public void setSubject(String subject) {
            this.subject  = subject;
         }
    
         public void setAttendance(int attendance) {
            this.attendance = attendance;
         }
    }

That’s a classic POJO and it is the force that underlies JavaBeans and quite a few tools in Java.

The properties are effectively the getters/setters e.g. `subject`, `when` etc. but properties have several features  
that are crucial:

  * They can be manipulated in runtime by a tool that had no knowledge of them during compile time

  * They are observable – a tool can monitor changes to a value of a property

  * They can have meta-data associated with them

These features are crucial since properties allow us all kinds of magic e.g. hibernate/ORM uses properties to bind  
Java objects to a database represenation, jaxb does it to parse XML directly into Java objects and GUI builders  
use them to let us customize UI’s visually.

POJO’s don’t support most of that so pretty much all Java based tools use a lot of reflection & bytecode manipulation.  
This works but has a lot of downsides e.g. say I want to map an object both to the Database and to XML/JSON.  
Would the bytecode manipulation collide?

Would it result in duplicate efforts?

And how do I write custom generic code that uses such abilities? Do I need to manipulate the VM?

### Properties in Java

These are all very abstract ideas, lets look at how we think properties should look in Java and how we can  
benefit from this moving forward.

__ |  The code below is preliminary and the syntax/classes might change without warning   
---|---  
  
This is the same class as the one above written with properties:
    
    
    public class Meeting implements PropertyBusinessObject {
         public final Property<Date,Meeting> when = new Property<>("when");
         public final Property<String,Meeting> subject = new Property<>("subject");
         public final Property<Integer,Meeting>  attendance = new Property<>("attendance");
         private final PropertyIndex idx = new PropertyIndex(this, "Meeting", when, subject, attendance);
    
        @Override
        public PropertyIndex getPropertyIndex() {
            return idx;
        }
    }

This looks a bit like a handful so let’s start with usage which might clarify a few things then dig into the class itself.

When we used a POJO we did this:
    
    
    Meeting meet = new Meeting();
    meet.setSubject("My Subject");
    Log.p(meet.getSubject());

With properties we do this:
    
    
    Meeting meet = new Meeting();
    meet.subject.set("My Subject");
    Log.p(meet.subject.get());

#### Encapsulation

At first glance it looks like we just created public fields (which we did) but if you will look closely at the declaration  
you will notice the `final` keyword:
    
    
         public final Property<String,Meeting> subject = new Property<>("subject");

This means that this code will not compile:
    
    
    meet.subject = otherValue;

So all setting/getting must happen thru the set/get methods and they can be replaced. E.g. this is valid syntax  
that prevents setting the property to null and defaults it to an empty string:
    
    
    public final Property<String,Meeting> subject = new Property<>("subject", "") {
         public Meeting set(String value) {
             if(value == null) {
                return Meeting.this;
             }
             return super.set(value);
         }
    };

__ |  We’ll discuss the reason for returning the `Meeting` instance below   
---|---  
  
#### Introspection & Observability

Since `Property` is a common class it’s pretty easy for introspective code to manipulate properties. However,  
it can’t detect properties in an object without reflection.

That’s why we have the index object and the `PropertyBusinessObject` interface (which defines `getPropertyIndex`).

The `PropertyIndex` class provides meta data for the surrounding class including the list of the properties within.  
It allows enumerating the properties and iterating over them making them accessible to all tools.

Furthermore all properties are observable with the property change listener. I can just write this to instantly print  
out any change made to the property:
    
    
    meet.subject.addChangeListener((p) -> Log.p("New property value is: " + p.get()));

### The Cool Stuff

That’s the simple stuff that can be done with properties, but they can do **much** more!

For starters all the common methods of `Object` can be implemented with almost no code:
    
    
    public class Meeting implements PropertyBusinessObject {
         public final Property<Date,Meeting> when = new Property<>("when");
         public final Property<String,Meeting> subject = new Property<>("subject");
         public final Property<Integer,Meeting>  attendance = new Property<>("attendance");
         private final PropertyIndex idx = new PropertyIndex(this, "Meeting", when, subject, attendance);
    
        @Override
        public PropertyIndex getPropertyIndex() {
            return idx;
        }
    
        public String toString() {
            return idx.toString();
        }
    
        @Override
        public boolean equals(Object obj) {
            return obj.getClass() == getClass() && idx.equals(((TodoTask)obj).getPropertyIndex());
        }
    
        @Override
        public int hashCode() {
            return idx.hashCode();
        }
    }

This is easy thanks to introspection…​

We already have some simple code that can convert an object to/from JSON Maps e.g. this can fill the property  
values from parsed JSON:
    
    
    meet.getPropertyIndex().populateFromMap(jsonParsedData);

And visa versa:
    
    
    String jsonString = meet.toJSON();

We also have a very simple ORM solution that maps values to table columns and can create tables. It’s no hibernate  
but sqlite isn’t exactly big iron so it might be good enough.

#### Constructors

One of the problematic issues with constructors is that any change starts propagating everywhere. If I have  
fields in the constructor and I add a new field later I need to keep the old constructor for compatibility.

So we added a new syntax:
    
    
    Meeting meet = new Meeting().
            subject.set("My Subject").
            when.set(new Date());

That is why every property in the definition needed the `Meeting` generic and the set method returns the `Meeting`  
instance…​

We are pretty conflicted on this feature and are thinking about removing it.

Without this feature the code would look like this:
    
    
    Meeting meet = new Meeting();
    meet.subject.set("My Subject");
    meet.when.set(new Date());

Is this feature valuable?

Is it worth the cost of converting this:
    
    
    public class Meeting implements PropertyBusinessObject {
         public final Property<Date> when = new Property<>("when");
         public final Property<String> subject = new Property<>("subject");
         public final Property<Integer>  attendance = new Property<>("attendance");

To this:
    
    
    public class Meeting implements PropertyBusinessObject {
         public final Property<Date,Meeting> when = new Property<>("when");
         public final Property<String,Meeting> subject = new Property<>("subject");
         public final Property<Integer,Meeting>  attendance = new Property<>("attendance");

I’m personally conflicted here…​

### Feedback & Summary

The reason for this post is feedback, we’d like feedback on all of the above and general thoughts on this.

Is this something you are interested in?

What features are important to you?

Is setter based construction a good idea that is worth the compromise?

Do you have ideas on improving the syntax further?
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Stefan Eder** — November 23, 2016 at 9:50 am ([permalink](https://www.codenameone.com/blog/properties.html#comment-22967))

> Stefan Eder says:
>
> I like it a lot – and the fluent inferace via method chaining.
>



### **Shai Almog** — November 24, 2016 at 6:05 am ([permalink](https://www.codenameone.com/blog/properties.html#comment-22999))

> Shai Almog says:
>
> Thanks, that’s helpful!
>



### **Orlando D'Free** — December 6, 2016 at 12:01 am ([permalink](https://www.codenameone.com/blog/properties.html#comment-23136))

> Orlando D'Free says:
>
> Very interesting. I haven’t thought through the implications of returning Meeting (although I tend to like APIs that let me do method chaining), but here’s one thing I’d like to see: PropertyChangeListeners. For example, I might want to add a method, either in class Meeting or a more generic superclass, that looks like this:
>
> public void addPropertyChangeListener(String propertyName, PropertyListener listener) {  
> idx.addPropertyChangeListener(propertyName, listener);  
> }
>
> This would work if the PropertyIndex class had a method that added the listener to the appropriate Property object, and if the Property class would fire changes when the setter was called.
>



### **Shai Almog** — December 6, 2016 at 6:11 am ([permalink](https://www.codenameone.com/blog/properties.html#comment-22746))

> Shai Almog says:
>
> Notice the observability section above:
>
> meet.subject.addChangeListener((p) -> Log.p(“New property value is: ” + p.get()));
>



### **Mark Daniel Henning** — July 20, 2017 at 5:50 am ([permalink](https://www.codenameone.com/blog/properties.html#comment-23726))

> Mark Daniel Henning says:
>
> Quick question: Is there a concept of a virtual property? For instance, Say I have a PropertyBusinessObject called Length with a Property defined as  
> public final Property<double,length> metre = new Property<>(“metre”);
>
> It would be useful to have a virtual property km such that Length.km.get() would return Length.metre.get() * .001  
> and Length.km.set(val) would call Length.metre.set(val * 1000)
>
> the PropertyIndex would only include metre so that toJSON would only store the metre value.
>
> If not, how would one add methods to the class that would provide a consistent calling pattern?
>



### **Shai Almog** — July 21, 2017 at 1:32 pm ([permalink](https://www.codenameone.com/blog/properties.html#comment-23377))

> Shai Almog says:
>
> Sure that’s pretty important. See: [https://gist.github.com/cod…](<https://gist.github.com/codenameone/9e124bf7975b263a0319ec42d2718d64>)
>
> Notice I used DoubleProperty instead of Property which has some advantages with things like ORM due to erasures.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
