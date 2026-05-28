# Component Binding and Validation Reference

The Codename One Maven plugin ships an annotation-driven binding framework
that wires a model POJO to the components on a `Form` / `Container` at
build time. The same annotations also drive validation: when you bind the
model, the generated binder configures a `Validator` whose constraints
come from `@Required` / `@Length` / `@Regex` / `@Email` / `@Url` /
`@Numeric` / `@ExistIn` / `@Validate` on the model fields.

This file is the agent's cheat-sheet for both. Open it when you see
`@Bindable`, `@Bind`, or any of the validation annotations -- or when the
user asks for "wire a form to a POJO," "auto-validate this screen,"
"disable the submit button until everything is valid," or similar.

## The two annotations that drive binding

```java
package com.example;

import com.codename1.annotations.*;
import com.codename1.binding.BindAttr;

@Bindable                                       // generates a XxxCn1Binder
public class LoginModel {

    @Bind(name = "userField", attr = BindAttr.TEXT)
    private String user;

    @Bind(name = "rememberMe", attr = BindAttr.SELECTED)
    private boolean remember;

    @Bind(name = "banner", attr = BindAttr.UIID, twoWay = false)
    public String bannerStyle;

    public String  getUser()                 { return user; }
    public void    setUser(String u)         { this.user = u; }
    public boolean isRemember()              { return remember; }
    public void    setRemember(boolean r)    { this.remember = r; }
}
```

- `@Bindable` marks a class for binder generation. The Maven plugin emits
  `<ClassName>Cn1Binder` in the same package + a single
  `cn1app.BinderBootstrap` that registers them all.
- `@Bind(name = ...)` matches `Component#getName()` on the form (the GUI
  builder name). The binder walks the container recursively.
- `attr` picks the component property the field mirrors. Default is
  `TEXT`. Other values: `UIID`, `HIDDEN`, `VISIBLE`, `ENABLED`,
  `SELECTED`, `ICON_NAME`, `NAME`.
- `twoWay = true` (the default for `TEXT` and `SELECTED`) installs a
  listener on the component so user input flows back into the model. The
  setter is bytecode-instrumented to call `Binders.notifyChanged(this)`,
  which fans out to every active binding when the model is mutated from
  any code path. Other attrs are write-only.

Accessor resolution order:

1. `@Bind(getter="...", setter="...")` -- explicit method names.
2. JavaBeans `getFoo()` / `isFoo()` / `setFoo(T)` detected from
   bytecode.
3. Direct public-field access (only when the field is `public`).

The build fails when none of the three resolves. Don't add `@Bind` to a
private field without a JavaBeans setter -- the error message is clear,
but it's faster to write the accessors up front.

## Bind at runtime

```java
import com.codename1.binding.Binders;
import com.codename1.binding.Binding;

Form form = (Form) Resources.getGlobalResources().getForm("LoginForm");
LoginModel model = new LoginModel();
Binding binding = Binders.bind(model, form);

// Two-way bindings flow automatically. Mutate through the setter:
model.setUser("alice");          // userField text updates

// Pull pending edits into the model before submit:
binding.commit();

// On form dispose:
binding.disconnect();             // remove every listener the binder added
```

The `Binding` handle exposes `refresh()`, `commit()`, `disconnect()`,
**and** `getValidator()`.

## Validation annotations

Stack any of these on a `@Bind` field. Each maps to a constraint in
`com.codename1.ui.validation.*`. Multiple annotations on a single field
combine into a `GroupConstraint` (first failure wins).

| Annotation                                 | Maps to                                | When to use |
| --- | --- | --- |
| `@Required`                                | `LengthConstraint(1, msg)`             | Mandatory non-empty field. |
| `@Length(min = N, message = ...)`          | `LengthConstraint(N, msg)`             | Minimum string length. |
| `@Regex(pattern = ..., message = ...)`     | `RegexConstraint(pat, msg)`            | Arbitrary pattern; pattern is the Codename One `RE` dialect. |
| `@Email(message = ...)`                    | `RegexConstraint.validEmail(msg)`      | Vetted email regex. Stack with `@Required` -- it accepts empty. |
| `@Url(message = ...)`                      | `RegexConstraint.validURL(msg)`        | `http` / `https` / `ftp` / `file` schemes. |
| `@Numeric(decimal = ..., min = ..., max = ..., message = ...)` | `NumericConstraint(...)`               | Integer or decimal, optional inclusive bounds. |
| `@ExistIn({"a","b"}, caseSensitive = ..., message = ...)` | `ExistInConstraint(...)`               | Whitelist of allowed strings. |
| `@Validate(MyConstraint.class)`            | `new MyConstraint()`                   | Escape hatch -- public no-arg constructor that implements `Constraint`. |

### Annotated model + use site

```java
@Bindable
public class SignupModel {
    @Bind(name = "userField")
    @Required @Length(min = 3, message = "At least 3 characters")
    private String user;

    @Bind(name = "emailField")
    @Required @Email
    private String email;

    @Bind(name = "ageField")
    @Numeric(min = 13, max = 120, message = "Age 13-120")
    private String age;

    @Bind(name = "roleField")
    @ExistIn({ "admin", "editor", "viewer" })
    private String role;

    @Bind(name = "siteField")
    @Url
    private String site;

    @Bind(name = "phoneField")
    @Validate(PhoneConstraint.class)
    private String phone;

    // getters / setters omitted
}
```

```java
SignupModel model = new SignupModel();
Binding b = Binders.bind(model, form);

// Gate the submit button:
Button submit = (Button) form.findByName("submitButton");
b.getValidator().addSubmitButtons(submit);

// Programmatic check before save:
if (b.getValidator().isValid()) {
    repository.save(model);
}

// Live feedback (global toggle):
Validator.setValidateOnEveryKey(true);
```

### Custom constraint class (`@Validate`)

```java
package com.example;
import com.codename1.ui.validation.Constraint;

public class PhoneConstraint implements Constraint {
    @Override public boolean isValid(Object value) {
        if (value == null) return false;
        String s = value.toString();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(c >= '0' && c <= '9') && c != '+' && c != '-') return false;
        }
        return s.length() > 0;
    }
    @Override public String getDefaultFailMessage() {
        return "Must be a valid phone number";
    }
}
```

The class must be `public` with a public no-argument constructor. The
generated binder instantiates it once per `bind()` call.

## Things to know

- `Binding#getValidator()` is never `null`. When the model has no
  validation annotations the validator is empty and `isValid()` returns
  `true` -- this is a safe call site for `addSubmitButtons`.
- `@Required` and `@Email` combine naturally -- the email regex accepts
  the empty string, so without `@Required` an empty address is "valid."
- `@Numeric` operates on the component's text value (parsed via
  `Integer.parseInt` / `Double.parseDouble`). The bound field can still
  be a `String` in the model.
- `@ExistIn` is case-insensitive by default. Set `caseSensitive = true`
  when matching identifiers like enum values.
- The generated binder uses `Binders.enterUpdate()` / `exitUpdate()` to
  break model -> component -> model loops. If a setter synchronously
  mutates a second bound field, call `binding.refresh()` to push the
  derived value out (the loop guard suppresses the in-region
  notification by design).
- Don't implement `Binding` yourself. The framework's only implementer
  is the generated binder; the interface may grow over time.

## When to reach for `UiBinding` instead

The imperative `com.codename1.properties.UiBinding` API still ships and
works alongside `@Bindable`. Use `UiBinding` when:

- the model isn't an `@Bindable` POJO (third-party class, generic
  property bag);
- you need a non-default converter (date formatting, currency, ...);
- the binding shape is dynamic / runtime-driven.

Use `@Bindable` for everything else -- it's terser, validates at build
time, and gives you `getValidator()` for free.

## Common pitfalls

- **`Binders.bind(...)` throws `IllegalStateException`**: the
  `cn1app.BinderBootstrap` didn't load. Either the `@Bindable` class
  lives outside the scan root, or you bypassed the standard `Lifecycle`
  start-up. In test code, call `XxxCn1Binder.register()` directly.
- **`@Bind` field has no accessor**: make the field `public` or add a
  JavaBeans `setX` (for two-way) and `getX` (always).
- **`@Validate(MyConstraint.class)` blows up at runtime**: make sure the
  class is reachable from the build classpath and exposes a public
  no-arg constructor.
- **Validation emblem never shows**: the `Validator` only paints emblems
  when at least one constraint has been added. Calling
  `addSubmitButtons` before any constraints is harmless but the buttons
  start in an enabled state until the first constraint registers.
