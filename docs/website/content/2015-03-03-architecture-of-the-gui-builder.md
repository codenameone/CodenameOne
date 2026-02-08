---
title: "Architecture Of The GUI Builder"
date: 2015-03-03
slug: "architecture-of-the-gui-builder"
---

# Architecture Of The GUI Builder

1. [Home](/)
2. Developers
3. Architecture Of The GUI Builder

The Codename One GUI builder has several unique underlying concepts that aren't as common among such tools, in this article I will try to clarify some of these basic ideas.

### Basic Concepts

The Codename One Designer isn't a standard code generator, the UI is saved within the resource file and can be designed without the source files available. This has several advantages:

1. No fragile generated code to break.
2. Designers who don't know Java can use the tool.
3. The "[Codename One LIVE!](http://www.codenameone.com/codename-one-live.html)" application can show a live preview of your design as you build it.
4. Images and theme settings can be integrated directly with the GUI without concern.
5. The tool is consistent since the file you save is the file you run.
6. GUI's/themes can be downloaded dynamically without replacing the application (this can reduce download size).
7. It allows for control over application flow. It allows preview within the tool without compilation.

This does present some disadvantages and oddities:

1. Its harder to integrate custom code into the GUI builder/designer tool.
2. The tool is somewhat opaque, there is no "code" you can inspect to see what was accomplished by the tool.
3. If the resource file grows too large it can significantly impact memory/performance of a running application.
4. Binding between code and GUI isn't as intuitive and is mostly centralized in a single class.

In theory you don't need to generate any code, you can load any resource file that contains a UI element as you would normally load a Resource file:

```
Resources r = Resources.open("/myFile.res");
```

Then you can just create a UI using the UIBuilder API:

```
UIBuilder u = new UIBuilder();
Container c = u.createContainer(r, "uiNameInResource");
```

(Notice that since Form & Dialog both derive from Container you can just downcast to the appropriate type).

This would work for any resource file and can work completely dynamically! E.g. you can download a resource file on the fly and just show the UI that is within the resource file... That is what [Codename One LIVE!](http://www.codenameone.com/codename-one-live.html) is doing internally.

### IDE Bindings

While the option of creating a Resource file manually is powerful, its not nearly as convenient as modern GUI builders allow. Developers expect the ability to override events and basic behavior directly from the GUI builder and in mobile applications even the flow for some cases.

To facilitate IDE integration we decided on using a single Statemachine class, similar to the common controller pattern. We considered multiple classes for every form/dialog/container and eventually decided this would make code generation more cumbersome.

The designer effectively generates one class "StatemachineBase" which is a subclass of UIBuilder (you can change the name/package of the class in the Codename One properties file at the root of the project). StatemachineBase is generated every time the resource file is saved assuming that the resource file is within the src directory of a Codename One project. Since the state machine base class is always generated, all changes made into it will be overwritten without prompting the user.

User code is placed within the Statemachine class, which is a subclass of the Statemachine Base class. Hence it is a subclass of UIBuilder!

When the resource file is saved the designer generates 2 major types of methods into  Statemachine base:

1. Finders - findX(Container c). A shortcut method to find a component instance within a hierarchy of containers. Effectively this is a shortcut syntax for [UIBuilder.findByName()](/javadoc/com/codename1/ui/util/UIBuilder.html#findByName%28java.lang.String,%20com.codename1.ui.Container%29), its still useful since the method is type safe. Hence if a resource component name is changed the find() method will fail in subsequent compilations.
2. Callback events - these are various callback methods with common names e.g.: onCreateFormX(), beforeFormX() etc. These will be invoked when a particular event/behavior occurs.
    
    Within the GUI builder, the event buttons would be enabled and the GUI builder provides a quick and dirty way to just override these methods. To prevent a future case in which the underlying resource file will be changed (e.g formX could be renamed to formY) a super method is invoked e.g. super.onCreateFormX();
    
    This will probably be replaced with the @Override annotation when Java 5 features are integrated into Codename One.

### Working With The Generated Code

The generated code is rather simplistic, e.g. the following code from the tzone demo adds a for the remove button toggle:

<script src="https://gist.github.com/2725146.js?file=Statemachine.java"></script>

As you can see from the code above implementing some basic callbacks within the state machine is rather simple. The method findFriendsRoot(c.getParent()); is used to find the "FriendsRoot" component within the hierarchy, notice that we just pass the parent container to the finder method. If the finder method doesn't find the friend root under the parent it will find the "true" root component and search there. The friends root is a container that contains the full list of our "friends" and within it we can just work with the components that were instantiated by the GUI builder. Implementing Custom Components There are two basic approaches for custom components:

1. Override a specific type - e.g. make all Form's derive a common base class.
2. Replace a deployed instance.

The first  uses a feature of UIBuilder which allows overriding component types, specifically override [createComponentInstance](/javadoc/com/codename1/ui/util/UIBuilder.html#createComponentInstance%28java.lang.String,%20java.lang.Class%29) to return an instance of your desired component e.g.:

<script src="https://gist.github.com/2725181.js?file=CreateComponentInstance.java"></script>

This code allows me to create a unified global form subclass. That's very useful when I want so global system level functionality that isn't supported by the designer normally.

The second approach allows me to replace an existing component:

<script src="https://gist.github.com/2725195.js?file=replace.java"></script>

Notice that we replace the title with an empty label, in this case we do this so we can later replace it while animating the replace behavior thus creating a slide-in effect within the title. It can be replaced though, for every purpose including the purpose of a completely different custom made component. By using the replace method the existing layout constraints are automatically maintained.
