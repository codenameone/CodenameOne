---
title: Validation, RegEx & Masking
slug: validation-regex-masking
url: /blog/validation-regex-masking/
original_url: https://www.codenameone.com/blog/validation-regex-masking.html
aliases:
- /blog/validation-regex-masking.html
date: '2015-04-05'
author: Shai Almog
---

![Header Image](/blog/validation-regex-masking/validation-regex-masking.png)

![](/blog/validation-regex-masking/validation-regex-masking-1.png)

Up until recently we had to handcode validation logic into every form, this becomes tedious as we work  
thru larger applications. Some developers built their own generic logic, which leads to obvious duplication  
of effort. In the interest of keeping everything together we decided to release a standardized validation  
framework that allows us to define constraints on a set of components, mark invalid entries and disable  
submission buttons. 

Since we wanted regular expressions as a feature in the validation framework we incorporated an old regular  
expression cn1lib that Steve ported into the API as well. This allows us to write regex in Codename One although  
we didn’t integrate it as deeply as it is in JavaSE to avoid some of the overhead and potential incompatibilities.  
You can see the [RegEx](/javadoc/com/codename1/util/regex/RE.html) package  
description for further details. 

Using validation is pretty trivial, we use the `Validator` class to add constraints which define  
the validation constraints for a specific component. We can also define the components to disable/enable  
based on validation state and the way in which validation errors are rendered (change the components UIID,  
paint an emblem on top etc.). A `Constraint` is an interface that represents validation requirements.  
You can define a constraint in Java or use some of the builtin constraints such as `LengthConstraint`,  
`RegexConstraint` etc. 

Masking isn’t a part of validation per se but it is such a common concept that I feel its use is warranted in this  
short tutorial. Masking allows us to define input with a given structure e.g. credit card comprising of 16 digits  
split into groups of 4. This is common for date entry and many other types of input, we’d love to offer a more  
powerful masked input API in the future but for now you can implement a poor mans solution by using a  
`DataChangedListener` and stopping the current editing/moving to the next field based on  
constraints. We implemented such masking as a sample in the code below together with a rather complete  
validation sample. 

This sample below has a couple of neat tricks, the UI changes between tablet and phone form factor. It demonstrates  
masking and various types of validation. It also demonstrates disabling the submit button to show that validation  
failed and prevent the user from proceeding. 
    
    
    public void start() {
        if(current != null){
            current.show();
            return;
        }
        Form val = new Form("Validation");
        TableLayout tl;
        int spanButton = 2;
        if(Display.getInstance().isTablet()) {
            tl = new TableLayout(7, 2);
        } else {
            tl = new TableLayout(14, 1);
            spanButton = 1;
        }
        tl.setGrowHorizontally(true);
        val.setLayout(tl);
    
        val.addComponent(new Label("First Name"));
        TextField firstName = new TextField();
        val.addComponent(firstName);
    
        val.addComponent(new Label("Surname"));
        TextField surname = new TextField();
        val.addComponent(surname);
    
        val.addComponent(new Label("E-Mail"));
        TextField email = new TextField();
        email.setConstraint(TextArea.EMAILADDR);
        val.addComponent(email);
    
        val.addComponent(new Label("URL"));
        TextField url = new TextField();
        url.setConstraint(TextArea.URL);
        val.addComponent(url);
    
        val.addComponent(new Label("Phone"));
        TextField phone = new TextField();
        phone.setConstraint(TextArea.PHONENUMBER);
        val.addComponent(phone);
    
        val.addComponent(new Label("Credit Card"));
    
        Container creditCardContainer = new Container(new GridLayout(1, 4));
        final TextField num1 = new TextField(4);
        final TextField num2 = new TextField(4);
        final TextField num3 = new TextField(4);
        final TextField num4 = new TextField(4);
        num1.setConstraint(TextArea.NUMERIC);
        num2.setConstraint(TextArea.NUMERIC);
        num3.setConstraint(TextArea.NUMERIC);
        num4.setConstraint(TextArea.NUMERIC);
        creditCardContainer.addComponent(num1);
        creditCardContainer.addComponent(num2);
        creditCardContainer.addComponent(num3);
        creditCardContainer.addComponent(num4);
        val.addComponent(creditCardContainer);
    
        Button submit = new Button("Submit");
        TableLayout.Constraint cn = tl.createConstraint();
        cn.setHorizontalSpan(spanButton);
        cn.setHorizontalAlign(Component.RIGHT);
        val.addComponent(cn, submit);
    
        Validator v = new Validator();
        v.addConstraint(firstName, new LengthConstraint(2)).
                addConstraint(surname, new LengthConstraint(2)).
                addConstraint(url, RegexConstraint.validURL()).
                addConstraint(email, RegexConstraint.validEmail()).
                addConstraint(phone, new RegexConstraint(phoneRegex, "Must be valid phone number")).
                addConstraint(num1, new LengthConstraint(4)).
                addConstraint(num2, new LengthConstraint(4)).
                addConstraint(num3, new LengthConstraint(4)).
                addConstraint(num4, new LengthConstraint(4));
    
        automoveToNext(num1, num2);
        automoveToNext(num2, num3);
        automoveToNext(num3, num4);
    
        v.addSubmitButtons(submit);
    
        val.show();
    }
    
    private void automoveToNext(final TextField current, final TextField next) {
        current.addDataChangeListener(new DataChangedListener() {
            public void dataChanged(int type, int index) {
                if(current.getText().length() == 5) {
                    Display.getInstance().stopEditing(current);
                    String val = current.getText();
                    current.setText(val.substring(0, 4));
                    next.setText(val.substring(4));
                    Display.getInstance().editString(next, 5, current.getConstraint(), next.getText());
                }
            }
        });        
    }
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Diamond** — April 7, 2015 at 1:43 pm ([permalink](https://www.codenameone.com/blog/validation-regex-masking.html#comment-21688))

> Diamond says:
>
> Hi Shai,
>
> Is the Validator and RegexConstraint present in the current plugin or will come with the next update?
>
> Phone number validation, will it work against 07123456789 or +97123456789 or both?
>
> My suggestion on autoMoveToNext…Instead of checking the input on 5th letter why not 4 and just move the cursor to the next field for entry.
>
> Lastly, will the keyboard close and re-open when stopEditing() and editString() are called? Or make the keyboard flashes?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fvalidation-regex-masking.html)


### **Shai Almog** — April 7, 2015 at 7:15 pm ([permalink](https://www.codenameone.com/blog/validation-regex-masking.html#comment-22191))

> Shai Almog says:
>
> Hi,  
> it made it into the current plugin and should be there already.  
> Notice I just used the RegexConstraint for a phone, you can define any regex you want. We hardcoded email/URL since those are common and slightly more standardized.  
> Auto move might be an interesting approach to masking, this is one of those things we need to think about.  
> Yes, the keyboard does flash. On my iphone this felt acceptable.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fvalidation-regex-masking.html)


### **Gerben** — August 12, 2015 at 8:31 am ([permalink](https://www.codenameone.com/blog/validation-regex-masking.html#comment-22203))

> Gerben says:
>
> A regex containing (?= does not seem to work yet. Example, a password that should contain at least one lowercase character:  
> “^(?=.*[a-z]).{8,24}$”
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fvalidation-regex-masking.html)


### **shannah78** — August 12, 2015 at 5:50 pm ([permalink](https://www.codenameone.com/blog/validation-regex-masking.html#comment-21551))

> shannah78 says:
>
> I don’t think lookarounds are supported. The Javadocs don’t mention them, and the code doesn’t look like it accommodates them:  
> [http://www.codenameone.com/…](<http://www.codenameone.com/javadoc/com/codename1/util/regex/RE.html>)
>
> If I were you I would just break this up into two constraints. E.g. The regex “[a-z]” will be sufficient to make sure it contains at least one lowercase character, and you can do the length constraint separately.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fvalidation-regex-masking.html)


### **Mo** — June 2, 2016 at 5:03 pm ([permalink](https://www.codenameone.com/blog/validation-regex-masking.html#comment-22568))

> Mo says:
>
> Hi, thank you for an excellent work, and having used the Validator, I ran into the following issues:
>
> 1\. when I use the Validtor.setShowErrorMessageForFocusedComponent(true), the popup error message does not close as expected after correcting the invalid email/text or losing the focus on the TextField, and the only way to get ride of it, is to lose and gain the focus again on the TextField!.
>
> 2\. No internationalisation support for the error message and inline with the Label component. for example, you cannot use the RegexConstraint.validEmail(“[invalid.email](<http://invalid.email>)”) to retrive the localized message from the resource file.
>
> 3\. As the time of writing this!!, I have no idea on how to style the popup error component via CSS lib!!
>
> PLease advice if this is something was overlooked from my end?? and many thanks in advnce.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fvalidation-regex-masking.html)


### **Shai Almog** — June 3, 2016 at 4:03 am ([permalink](https://www.codenameone.com/blog/validation-regex-masking.html#comment-22623))

> Shai Almog says:
>
> 1\. Was this on a device?
>
> 2\. If you installed the bundle correctly you should see it localized.
>
> 3\. See setErrorMessageUIID(). It uses the PopupDialog UIID for the content itself.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fvalidation-regex-masking.html)


### **Mo** — June 4, 2016 at 9:24 am ([permalink](https://www.codenameone.com/blog/validation-regex-masking.html#comment-21518))

> Mo says:
>
> 1\. on the simulator,
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fvalidation-regex-masking.html)


### **Shai Almog** — June 5, 2016 at 4:15 am ([permalink](https://www.codenameone.com/blog/validation-regex-masking.html#comment-22777))

> Shai Almog says:
>
> On the device text entry behaves quite differently.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fvalidation-regex-masking.html)


### **Chris** — April 23, 2018 at 9:03 pm ([permalink](https://www.codenameone.com/blog/validation-regex-masking.html#comment-23542))

> Chris says:
>
> Shai, keyboard will not close after automovenext(). This will lead to an issue when the device is rotated. Text in the field will display in the wrong location and misaligned as well.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fvalidation-regex-masking.html)


### **Shai Almog** — April 24, 2018 at 5:23 am ([permalink](https://www.codenameone.com/blog/validation-regex-masking.html#comment-23691))

> Shai Almog says:
>
> Is the parent container scrollable on the Y axis?  
> Android resizes the screen on edit so you need to leave enough room for the text field in such cases.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fvalidation-regex-masking.html)


### **Chris** — April 24, 2018 at 6:45 pm ([permalink](https://www.codenameone.com/blog/validation-regex-masking.html#comment-23774))

> Chris says:
>
> Yes scrollable on Y-Axis. But this is happening other way around when the tablet flipped form portrait mode to landscape.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fvalidation-regex-masking.html)


### **Shai Almog** — April 25, 2018 at 7:12 am ([permalink](https://www.codenameone.com/blog/validation-regex-masking.html#comment-23906))

> Shai Almog says:
>
> Can you post a screenshot? What do you mean by “other way around”?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fvalidation-regex-masking.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
