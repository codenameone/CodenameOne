# Evidence map

Source: `docs/website/content/blog/uiscene-on-by-default-ios-performance.md`
Canonical: https://www.codenameone.com/blog/uiscene-on-by-default-ios-performance/

## Thesis

The UIScene migration and the compiler hints behind measurable iOS performance work

## Supported beats

- **Base64 Performance on iOS:** In our previous episode we discussed the work we did on performance for Base64 support. That class represents the potential speed of Codename One in production. If we can make it fly, we can make any code fly.
- **ParparVM Gets New Performance Hints:** Usually that is the right tradeoff. Safety checks, debug metadata, virtual dispatch, and conservative code generation are all there for a reason. They make the system easier to debug, safer to evolve, and more resilient when code does something unexpected.
- **Method-Level Hints:** This suppresses generated line and debug metadata for the annotated method. That reduces generated C size and trims some debug-related overhead from the generated native code.
- **@Concrete and Polymorphism:** This is a class-level hint for native ParparVM output that lets us tell the translator that a base type always maps to a known concrete implementation at runtime. Most of you will probably never need this annotation since polymorphism by its nature doesn’t do that.
- **Material Icons Will Now Update Automatically:** This is the kind of maintenance task that we often postpone because it’s annoying to do by hand. Automating it means icon updates can happen regularly instead of depending on someone remembering to revisit them after a long gap (thank you to those dedicated community members who ask for this!).
- **The Picker Gets Quick Action Buttons:** In those cases, giving the user a direct shortcut is just a better UX.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/issues/3152
- https://github.com/codenameone/CodenameOne/issues/4733
- https://github.com/codenameone/CodenameOne/issues/2085
