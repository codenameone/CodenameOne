/*
 * Test stub of com.codename1.ui.Form. Exposes the surface
 * RouteAnnotationProcessor fixtures need to subclass and that the generated
 * Routes class + Navigation stub call (#show, #showBack, #getTitle).
 */
package com.codename1.ui;

public class Form {
    public boolean shown;
    public boolean shownBack;
    private String title;

    public Form() { }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void show() {
        shown = true;
    }

    public void showBack() {
        shownBack = true;
    }
}
