// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::annotation-component-binding-java-001[]
package com.example;

import com.codename1.annotations.*;
import com.codename1.binding.BindAttr;
import com.codename1.properties.Property;

@Bindable
public class LoginModel {

    @Bind(name = "userField", attr = BindAttr.TEXT)
    private String user;
    public String getUser()              { return user; }
    public void   setUser(String u)      { this.user = u; }                  // <1>

    @Bind(name = "rememberMe", attr = BindAttr.SELECTED)
    public boolean remember;                                                   // <2>

    @Bind(name = "banner", attr = BindAttr.UIID, twoWay = false)
    public String bannerStyle;

    @Bind(name = "fullName",
          attr = BindAttr.TEXT,
          getter = "computeFullName",
          setter = "applyFullName")                                           // <3>
    private String fullName;
    public String computeFullName()      { return fullName.toUpperCase(); }
    public void   applyFullName(String f){ this.fullName = f.trim(); }
}
// end::annotation-component-binding-java-001[]

// tag::annotation-component-binding-java-002[]
import com.codename1.binding.Binders;
import com.codename1.binding.Binding;

Form f = (Form) Resources.getGlobalResources().getForm("LoginForm");
LoginModel model = new LoginModel();
Binding b = Binders.bind(model, f);

// The two-way bindings push every keystroke / toggle back into the model.
// Mutate the model through the setter and the bound component refreshes
// automatically:
model.setUser("alice");

// Or pull pending edits into the model before submit:
b.commit();

// On form dispose:
b.disconnect();          // remove every installed listener
// end::annotation-component-binding-java-002[]

// tag::annotation-component-binding-java-003[]
public void setName(String name) {
    this.name = name;
    com.codename1.binding.Binders.notifyChanged(this);   // injected
}
// end::annotation-component-binding-java-003[]

// tag::annotation-component-binding-java-004[]
package com.example;

import com.codename1.annotations.*;
import com.codename1.binding.BindAttr;

@Bindable
public class SignupModel {

    @Bind(name = "userField")
    @Required
    @Length(min = 3, message = "User name too short")
    private String user;

    @Bind(name = "emailField")
    @Required
    @Email                                                          // <1>
    private String email;

    @Bind(name = "ageField")
    @Numeric(min = 13, max = 120, message = "Age 13-120")           // <2>
    private String age;                                             // <3>

    @Bind(name = "roleField")
    @ExistIn({ "admin", "editor", "viewer" })
    private String role;

    @Bind(name = "phoneField")
    @Validate(PhoneConstraint.class)                                // <4>
    private String phone;

    // ... getters / setters omitted for brevity
}
// end::annotation-component-binding-java-004[]

// tag::annotation-component-binding-java-005[]
LoginModel model = new LoginModel();
Binding b = Binders.bind(model, form);

// Auto-disable a submit button until everything is valid.
Button submit = (Button) form.findByName("submitButton");
b.getValidator().addSubmitButtons(submit);

// Live feedback as the user types (static toggle on the Validator
// class -- one switch flips the behaviour for every validator in the
// app):
Validator.setValidateOnEveryKey(true);

// Programmatic gate before saving:
if (b.getValidator().isValid()) {
    repository.save(model);
}
// end::annotation-component-binding-java-005[]
