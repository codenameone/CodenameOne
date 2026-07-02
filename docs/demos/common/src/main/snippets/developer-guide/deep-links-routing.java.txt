// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::deep-links-routing-java-001[]
package com.example;

import com.codename1.annotations.Route;
import com.codename1.annotations.RouteParam;
import com.codename1.ui.Form;

@Route("/users/:id")
public class ProfileForm extends Form {
    public ProfileForm(@RouteParam("id") String id) {
        setTitle("Profile " + id);
        // ...
    }
}
// end::deep-links-routing-java-001[]

// tag::deep-links-routing-java-002[]
public class Routes {
    @Route("/home")
    public static Form home() {
        return new HomeForm();
    }

    @Route("/users/:id")
    public static Form profile(@RouteParam("id") String id) {
        return new ProfileForm(id);
    }
}
// end::deep-links-routing-java-002[]

// tag::deep-links-routing-java-003[]
Navigation.navigate("/users/42");

// Go back one step:
Navigation.back();

// Inspect the stack for a breadcrumb UI:
Container breadcrumbs = new Container(BoxLayout.x());
for (final NavigationEntry e : Navigation.getStack()) {
    Button crumb = new Button(e.getTitle());
    crumb.addActionListener(evt -> Navigation.popTo(e));
    breadcrumbs.add(crumb);
}
// end::deep-links-routing-java-003[]

// tag::deep-links-routing-java-004[]
String json = new com.codename1.maven.routing.AasaBuilder()
    .appId("ABCD1234.com.example.app")
    .addRouterPattern("/users/:id")
    .addRouterPattern("/share/**")
    .addPath("NOT /admin/*")
    .build();
// Write `json` to https://example.com/.well-known/apple-app-site-association
// end::deep-links-routing-java-004[]

// tag::deep-links-routing-java-005[]
String json = new com.codename1.maven.routing.AssetLinksBuilder()
    .addApp("com.example.app",
            "14:6D:E9:83:C5:73:06:50:D8:EE:B9:95:2F:34:FC:64:16:A0:83:42:E6:1D:BE:A8:8A:04:96:B2:3F:CF:44:E5")
    .addFingerprint("AB:CD:..." /* Play App Signing upload cert */)
    .build();
// end::deep-links-routing-java-005[]

// tag::deep-links-routing-java-006[]
import com.codename1.router.PopGuard;
import com.codename1.router.PopReason;

editForm.setPopGuard(new PopGuard() {
    public boolean canPop(Form form, PopReason reason) {
        if (!isDirty()) {
            return true;
        }
        Dialog.show("Discard changes?", "You have unsaved edits.",
                    "Stay", "Discard");
        return false;
    }
});
// end::deep-links-routing-java-006[]
