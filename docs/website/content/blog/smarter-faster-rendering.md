---
title: SMARTER, FASTER RENDERING
slug: smarter-faster-rendering
url: /blog/smarter-faster-rendering/
original_url: https://www.codenameone.com/blog/smarter-faster-rendering.html
aliases:
- /blog/smarter-faster-rendering.html
date: '2020-10-16'
author: Steve Hannah
---

We’ve recently added some new display settings, and a new method, `revalidateLater()`, which can significantly increase your app’s rendering throughput in some cases. Before I get to the specifics of these changes, let’s review how UI rendering works in Codename One.

The Codename One UI rendering can be broken down into two phases:

  1. **Layout** – Calculating the bounds for each component on the screen, and “laying” them out.
  2. **Painting** – Actually drawing each component.

Generally you only want to run the “layout” step when the layout has changed. This would include when components are added or removed from a Container, or when a component’s bounds or position has changed.

When you make changes to a component and you want these changes to be updated on the screen, generally you would call `repaint()` or `revalidate()`, depending on whether your changes require a “layout” phase. `repaint()` will only trigger a “paint” without updating the layout. `revalidate()` will first layout the container, and then it will paint it.

__ |  `revalidate()` isn’t the only method that can be used to trigger “layout”. If you want the UI component changes to be animated, you can use `animateLayout()`, `animateHierarchy()`, or several other `animateXXX()` methods of the `Container` class.  
---|---  
  
### Unintended Revalidation Triggers

One cause of rendering performance issues is unintended revalidations.

As I mentioned above, revalidation is expensive, so you should strive to revalidate only when necessary, and only revalidate the portions of the UI that require revalidation. It gets tricky, however, because you may not always be aware of revalidations that are triggered by other actions. For example, if you make a change to a style in a Component, it will trigger a revalidation of the component’s parent container. E.g.
    
    
    myLabel.getStyle().setBgColor(0xff0000);
        // This will trigger a revalidate() call in the parent container!!!

One accidental `revalidate()` call is not a big deal, but suppose you are setting a whole bunch of styles in sequence:
    
    
    myLabel.getStyle().setBgColor(0xff0000);
    myLabel.getStyle().setFont(...);
    myLabel.getStyle().setBgTransparency(0xff);
    myLabel.getStyle().setPadding(0,0,0,0);
    myLabel.getStyle().setMargin(0,0,0,0);
    myLabel.getStyle().setTextDecoration(...);
    myLabel.getStyle().setUnderline(...);
    myLabel.getStyle().setStrikeThru(...);
    myLabel.getStyle().setFgColor(0x0);
    myLabel.getStyle().setBorder(...);
        // The above calls will trigger 10 revalidations()!!!

The above example shows some code that sets 10 style properties on a label. This code will trigger 10 calls to `revalidate()` in “myLabel”‘s parent container.

**How do we avoid these redundant revalidations?**

One simple way to avoid the redundant revalidations in the example above is to set all of the style properties **before** adding `myLabel` to a container. Then no revalidations would occur.

However, we have just recently added a display property that will prevent style changes from triggering revalidations at all. Add the following into the `init()` method of your app’s main class:
    
    
    CN.setProperty("Component.revalidateOnStyleChange", "false");

Currently I recommend setting this property to “false” in all apps. Future versions of Codename One may change the default behaviour to “false”, but for now, the default is “true” because it is possible that some existing apps depend on the current behaviour.

### Unintended Revalidation Scope

Another performance trap that you may accidentally step into is a revalidation scope that is greater than you intend. If you call `myContainer.revalidate()`, the current Codename One default behaviour is to trigger a revalidation on the entire form – not just “myContainer”. This behaviour was likely implemented to cover some edge, but it is a major performance killer. To fix this issue, we have added another display property “Form.revalidateFromRoot”, which can be set to “false” to prevent this behaviour. E.g. Add the following to your `init()` method:
    
    
    CN.setProperty("Form.revalidateFromRoot", "false");

This way, when you call `myContainer.revalidate()` it will relayout “myContainer” and only “myContainer”.

### Redundant Revalidations

It is also possible to initiate a number of redundant calls to `revalidate()` if you’re not careful. E.g. If your UI is build with views that are bound to one or more model objects, and the views are directed to “revalidate” whenever a property of the model is changed, it is possible, and even likely, that your model may generate 10 or 20 property change events in one batch, which will propagate into 10 or 20 revalidation calls on the same container.

For example, consider the following excerpt from a “View” class:
    
    
    /**
     * A UI view for a Person
     */
    public class PersonView extends Container {
    
        private Label name, description, ...;
        private Person model;
    
        public PersonView(Person model) {
            this.model = model;
            this.model.addPropertyChangeListener(evt -> update()); 
            ...
        }
    
        /**
         * This method is triggered whenever a property is changed in the model.
         */
        public void update() {
            boolean changed = false;
            if (!Objects.equals(name.getText(), model.getName())) {
                name.setText(model.getName());
                changed = true;
            }
            if (!Objects.equals(description.getText(), model.getDescription()) {
                description.setText(model.getDescription());
                changed = true
            }
    
            // ...  etc... check all the properties to see if they have changed
            // and update the corresponding UI component accordingly
    
            if (changed) {
                // If a change has occurred, then revalidate()
                revalidate(); 
            }
        }
    }

| In the constructor of this view, we register a PropertyChangeListener on the model so that the `update()` method will be called whenever a property is changed on the model.  
---|---  
| Inside the `update()` method, we trigger a `revalidate()` if any changes were made in the model that required an update in the view.  
  
Now consider a fairly typical snippet of code where we load data into the model. E.g.:
    
    
    person.setName("...");
    person.setDescription("...");
    person.setAge(21);
    person.setOnline(true);
    person.setHeight(121);
    // ... etc... set all the other properties

Given the way that our model is bound to the view, this code will generate a property change event for each property. If we set 30 property values, then we trigger 30 property change events. And each property change event results in the `update()` method being called, and `revalidate()` being triggered. Therefore, this code could result in 30 revalidations of the view (and, if we aren’t using `CN.setProperty("Form.revalidateFromRoot", "false")`, this also results in 30 revalidations of the full form). It is easy to see that this is a waste. You might not notice the performance degradation in simple interfaces, but it will become more evident as the UI grows more complex.

**How to fix this**

To combat the problem of redundant revalidations, we have introduced a new method, `revalidateLater()` which will defer the revalidation until just before the next paint cycle. This method will automatically eliminate duplicate revalidations, which will result in a significant performance increase in situations like the one depicted here. In our particular example, if we changed our view code from:
    
    
    if (changed) {
        revalidate();
    }

to
    
    
    if (changed) {
        revalidateLater();
    }

Then changing 30 properties on the model would trigger only a single revalidation instead of 30.

### Summary

So summarize the new features discussed in this post:

  1. Revalidation should be done as little as possible for best performance.
  2. Style changes on components trigger revalidations by default.
  3. You can set the “Component.revalidateOnStyleChange” display property to “false” to prevent style changes from triggering revalidations. E.g. In your app’s `init()` method do:`CN.setDisplayProperty("Component.revalidateOnStyleChange", "false");`
  4. Calling `revalidate()` on a container, will trigger revalidation of the entire form by default.
  5. You can set the “Form.revalidateFromRoot” display property to “false” to prevent `revalidate()` from triggering revalidation of the entire form. E.g. In your app’s `init()` method, do:`CN.setDisplayProperty("Form.revalidateFromRoot", "false");`
  6. You can use `revalidateLater()` instead of `revalidate()` to defer revalidation until the next paint cycle, and avoid redundant revalidations. You should prefer `revalidateLater()` in most cases. The only time when `revalidate()` might be required is if you need to perform calculations on measurements after revalidation occurs.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
