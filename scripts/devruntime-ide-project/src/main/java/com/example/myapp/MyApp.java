package com.example.myapp;

import com.codename1.system.Lifecycle;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Slider;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.util.UITimer;

import java.util.Date;

/**
 * Your app. Edit it, run the push, watch it change on the phone.
 *
 * <p>It ships its own theme in {@code src/main/resources/theme.res}, which is
 * why it looks nothing like the runtime that hosts it -- the theme travels with
 * the bundle and the framework loads it the usual way.</p>
 */
public class MyApp extends Lifecycle {
    private int taps;

    /// A label that stays readable on the dark background set above.
    private static Label whiteText(String text) {
        Label l = new Label(text);
        l.getAllStyles().setFgColor(0xffffff);
        return l;
    }

    @Override
    public void start() {
        // No "already showing" guard. Every push runs start() again, and a
        // guard that sees the previous push's form still on screen makes the
        // next one a silent no-op -- the form keeps ticking and it looks like
        // the edit never arrived.
        Form f = new Form("Pushed App", BoxLayout.y());
        Toolbar.setGlobalToolbar(true);

        // Colours set in code rather than left to a theme. On Android the
        // platform's own Material palette paints standard widgets, so two apps
        // with different theme.res files can still look identical -- which is
        // exactly the doubt worth removing here. Nothing below can come from
        // anywhere but this file.


        // Live, so it is obvious this is executing rather than a screenshot.
        final Label clock = new Label("");
        clock.getAllStyles().setAlignment(Label.CENTER);
        UITimer.timer(1000, true, f, new Runnable() {
            public void run() {
                clock.setText(new Date().toString());
                clock.getParent().revalidate();
            }
        });

        final Label counter = whiteText("tapped 0 times");
        Button tap = new Button("Tap me");
        tap.addActionListener(e -> counter.setText("tapped " + (++taps) + " times"));

        final Label echo = whiteText("type above and it echoes here");
        TextField field = new TextField("", "type something", 20, TextField.ANY);
        field.addDataChangeListener((type, index) ->
                echo.setText(field.getText().length() == 0
                        ? "type above and it echoes here"
                        : field.getText().toUpperCase()));

        final Label slid = whiteText("slider: 0");
        Slider s = new Slider();
        s.setEditable(true);
        s.addDataChangedListener((type, index) -> slid.setText("slider: " + s.getProgress()));

        // A grid of fixed rows squashes its cells when the form runs out of
        // room; one label per line simply wraps.
        Container facts = new Container(BoxLayout.y());
        Display d = Display.getInstance();
        facts.add(whiteText("Platform: " + d.getPlatformName()));
        facts.add(whiteText("Screen: " + d.getDisplayWidth() + "x" + d.getDisplayHeight()));
        facts.add(whiteText("Density: " + d.getDeviceDensity()));
        facts.add(whiteText("This class was interpreted, not compiled in."));

        Label banner = new Label("This is your code, running on the device.");
        f.add(banner);
        f.add(clock);
        f.add(tap).add(counter);
        f.add(field).add(echo);
        f.add(s).add(slid);
        f.add(facts);
        f.getContentPane().setScrollableY(true);
        f.show();
        // After show(), not before: the theme is applied to a component when it
        // is laid out, which overwrites anything set at construction. This is
        // also the answer to "is that really my code" -- these colours exist
        // nowhere but this file.
        f.getContentPane().getAllStyles().setBgColor(0x102027);
        f.getContentPane().getAllStyles().setBgTransparency(255);
        f.getToolbar().getAllStyles().setBgColor(0xff6d00);
        f.getToolbar().getAllStyles().setBgTransparency(255);
        f.getTitleComponent().getAllStyles().setFgColor(0xffffff);
        tap.getAllStyles().setBgColor(0xff6d00);
        tap.getAllStyles().setBgTransparency(255);
        tap.getAllStyles().setFgColor(0xffffff);
        banner.getAllStyles().setFgColor(0xffffff);
        clock.getAllStyles().setFgColor(0x80ff80);
        counter.getAllStyles().setFgColor(0xffffff);
        echo.getAllStyles().setFgColor(0xffffff);
        slid.getAllStyles().setFgColor(0xffffff);
        for (int i = 0; i < facts.getComponentCount(); i++) {
            facts.getComponentAt(i).getAllStyles().setFgColor(0xffffff);
        }
        f.revalidate();
    }
}
