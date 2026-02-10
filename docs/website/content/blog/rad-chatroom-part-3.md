---
title: RAD Chat Room – Part 3
slug: rad-chatroom-part-3
url: /blog/rad-chatroom-part-3/
original_url: https://www.codenameone.com/blog/rad-chatroom-part-3.html
aliases:
- /blog/rad-chatroom-part-3.html
date: '2020-05-14'
author: Steve Hannah
---

![Header Image](/blog/rad-chatroom-part-3/chat-ui-kit-feature.jpg)

This is part 3 of the RAD Chatroom tutorial. You can find part 1 [here](/blog/rad-chatroom-part-1.html) and part 2 [here](/blog/rad-chatroom-part-2.html).

### Adding Text Messages from Other Users

Our current example only includes messages that the current user posted themself. I.e. We only have chat bubbles on the right-hand side of the view. Let’s add some more sample data to our view model to give us a feel for how a real chat will look. In the `ChatFormController` class, we’ll change the `createViewModel()` method as follows:

Creating more interesting sample data for the ChatRoom’s view model. We add messages from both the current user and other users.
    
    
    // Create a view model for the chat room
    private Entity createViewModel() {
        ChatRoomView.ViewModel room = new ChatRoomView.ViewModel();
    
        // Make up some dummy times for the chat messages.
        long SECOND = 1000l;
        long MINUTE = SECOND * 60;
        long HOUR = MINUTE * 60;
        long DAY = HOUR * 24;
    
        // Make first message 2 days ago.
        long t = System.currentTimeMillis() - 2 * DAY;
    
        // Some thumbnails for the avatars of the participants
        String georgeThumb = "https://weblite.ca/cn1tests/radchat/george.jpg";
        String kramerThumb = "https://weblite.ca/cn1tests/radchat/kramer.jpg";
    
        room.addMessages(createDemoMessage("Why couldn't you have made me an architect? You know I always wanted to pretend that I was an architect. "
                + "Well I'm supposed to see her tomorrow, I'm gonna tell her what's goin on. Maybe she likes me for me.",
                new Date(t), "George", georgeThumb));
        t += HOUR;
        room.addMessages(createDemoMessage("Hey", new Date(t), "Kramer", kramerThumb));
        t += MINUTE;
        room.addMessages(createDemoMessage("Hey", new Date(t), null,  null));
    
        return room;
    }
    
    // Create a single demo message
    private Entity createDemoMessage(String text,
            Date datePosted,
            String participant,
            String iconUrl) {
        ChatBubbleView.ViewModel msg = new ChatBubbleView.ViewModel();
        msg.messageText(text)
                .date(datePosted)
                .iconUrl(iconUrl)
                .isOwn(participant == null);
        if (participant != null) {
            msg.postedBy(participant);
        }
        return msg;
    }

To make things easier to read, I’ve broken out the code for creating a message into a separate method so we can call create new messages more easily. I’ve created a couple of pretend users, “George” and “Kramer”, and I’ve provided some thumbnail URLs for them, which can be used as avatars in the chat room.

And the result:

![Chat room now includes messages from two other users](/blog/rad-chatroom-part-3/rad-chat-room-11.png)

Figure 1. Chat room now includes messages from two other users, George and Kramer

Notice that it shows the time of the first chat message, but not the others. This is intentional. The chat room will only show the time of messages if there is a long delay between it and the previous message. You can see the time of each message by swiping to the left:

![Swipe to the left to reveal the date and time of each message](/blog/rad-chatroom-part-3/rad-chat-room-12.png)

Figure 2. Swipe to the left to reveal the date and time of each message

Adding the “Participants” Title Component

Recall the screenshot of the finished app, in which the form title included a list of participants in the chat room with their avatars.

![Participants title component with avatars](/blog/rad-chatroom-part-3/rad-chat-room-13.png)

Figure 3. Participants title component with avatars

Let’s add this now by adding some participants to the view model. The ChatRoomView.ViewModel includes methods to directly add participants to the model via addParticpant(Entity…​ participants). Each participant entity should implement the Thing.name or Thing.thumbnailUrl tags, or both. If Only Thing.name is provided, then it will generate an avatar with the first letter of their name. If Thing.thumbnailUrl is provided, then it will use the image at this url as the avatar.

Let’s begin by creating a custom entity/view model named “ChatAccount” which will be used as participants in the chat. Create a new Java class named “ChatAccount” with the following contents:

The ChatAccount entity will be used to encapsulate profiles for participants in the chat room.
    
    
    package com.codename1.cn1chat;
    import com.codename1.rad.models.Entity;
    import com.codename1.rad.models.EntityType;
    import static com.codename1.rad.models.EntityType.tags;
    import com.codename1.rad.models.StringProperty;
    import com.codename1.rad.schemas.Thing;
    
    /**
     * View model for an account profile.
     * @author shannah
     */
    public class ChatAccount extends Entity {
    
        // The name property
        public static StringProperty name; __**(1)**
    
        private static final EntityType TYPE = new EntityType() {{ __**(2)**
            name = string(tags(Thing.name)); __**(3)**
        }};
        {
            setEntityType(TYPE); __**(4)**
        }
    
        public ChatAccount(String nm) {
            set(name, nm);
        }
    
    }

__**1** | The “name” property of our entity.  
---|---  
__**2** | Define an entity type for the ChatAccount entity. The entity type defines which properties are supported by the ChatAccount entity.  
__**3** | Generating the “name” property as a string property. Notice that we assign the Thing.name tag to this property, which will allow views to bind to it.  
__**4** | Set the entity type inside the “instance” initializer so that all `ChatAccount` objects have the same entity type. This could have been placed inside the constructor, but placing it simply inside the initializer (i.e. inside `{..}`) makes for a little less typing, and also helps to signify the declarative nature of this call.  
  
I’ve added some notes about the key lines of the code listing above which should help to get you up to speed if this is your first custom entity. This entity defines a single property, “name”. If we were to define this entity as a POJO (Plain-Old Java object), the class might look something like:

What the `ChatAccount` entity would look like if implemented as a POJO (Plain old java object).
    
    
    public class ChatAccount {
        private String name;
        public ChatAccount(String name) {
            this.name = name;
        }
    }

So why not use a POJO for our entity?

The `Entity` class, together with `EntityType` provide lots of useful features such as bindable properties, property change events, data conversion, observability, and reflection. All of these features are necessary to enable the creation of loosely coupled components with clean separation between models, views, and controllers. As you’ll see, this loose coupling greatly enhances our ability to produce complex, reusable components, which results in better apps with less code.

Getting and Setting Properties on Entities

Before proceeding, its worth discussing the basics of how to use entities. The Entity class allows us to get and set properties without needing to define getter and setter methods. It also includes a rich set of convenience methods for handling data-conversion. Finally, one of the most powerful features of entities is its loose coupling. It is possible to get and set property values without any knowledge of which properties exist in the entity, via tags.

First things first: Getting and setting property values.

Getting and setting property values using a direct property reference.
    
    
    ChatAccount account = new ChatAccount("George");
    String name = account.get(ChatAccount.name);  // "George"
    account.set(ChatAccount.name, "Kramer");
    name = account.get(ChatAccount.name);  // "Kramer"

This code is tightly coupled to the `ChatAccount` entity because it directly references the `ChatAccount.name` property. In some cases, this tight coupling is fine. In other cases, such as when you want to develop a reusable component that requires a “name” property, you may prefer to use “loose” coupling, as follows:
    
    
    Entity account = ...;  // Could be any entity, but happens to be a ChatAccount
    account.setText(Thing.name, "George");
    String name = account.getText(Thing.name); // "George"

__ |  The `CodeRAD` library includes a hierarchy of schemas which define tags that may be used to tag entity properties. These schemas were adapted from <https://schema.org>, which defines entities and properties for a large number of common object types. All schemas extend the base schema, [Thing](https://schema.org/Thing). Some common tags include `name`, `identifier`, and `thumbnailUrl`. When creating reusable components, you can use these schema “tags” to access property values of view models in loosely coupled way. The javadocs for View components should list the tags that it expects on its view models, so you can tag the properties on your entities accordingly. For a full list of schemas, check out <https://schema.org/docs/full.html>. Only a subset has been ported into the CodeRAD library. More will be added over time, and you may contribute your own with a pull request.   
---|---  
  
### Finally…​ Adding the Participants

After a lengthy discussion of Entities, Entity types, Tags, and Properties, we can now go ahead and add some participants to the chat room. Add the following inside our `createViewModel()` method of the `ChatFormController` class:
    
    
    room.addParticipants(new ChatAccount("George"), new ChatAccount("Kramer"));

This adds two profiles to the chat room as participants. Now, if we launch the app we’ll see the form title replaced with the following avatars.

![Title component with avatars generated from our participants](/blog/rad-chatroom-part-3/rad-chat-room-14.png)

Figure 4. Title component with avatars generated from our participants

Now, let’s go a step further and add a “thumbnail url” property to our ChatAccount entity.

Adding a thumbnailUrl property to the ChatAccount entity:
    
    
    package com.codename1.cn1chat;
    import com.codename1.rad.models.Entity;
    import com.codename1.rad.models.EntityType;
    import static com.codename1.rad.models.EntityType.tags;
    import com.codename1.rad.models.StringProperty;
    import com.codename1.rad.schemas.Thing;
    
    /**
     * View model for an account profile.
     * @author shannah
     */
    public class ChatAccount extends Entity {
    
        // The name property
        public static StringProperty name, thumbnailUrl;
    
        private static final EntityType TYPE = new EntityType() {{
            name = string(tags(Thing.name));
            thumbnailUrl = string(tags(Thing.thumbnailUrl));
        }};
        {
            setEntityType(TYPE);
        }
    
        public ChatAccount(String nm, String thumb) {
            set(name, nm);
            set(thumbnailUrl, thumb);
        }
    
    }

And modify our ChatFormController to set the thumbnail URL on our entity.
    
    
    room.addParticipants(
        new ChatAccount("George", georgeThumb),
        new ChatAccount("Kramer", kramerThumb)
    );

And reload…​

![Title component after setting thumbnail URLs for our participants](/blog/rad-chatroom-part-3/rad-chat-room-15.png)

Figure 5. Title component after setting thumbnail URLs for our participants

### Next Week

In part four we’ll discuss adding more actions.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
