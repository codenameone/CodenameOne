/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.ui;

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codenameone.examples.papercut.play.Arena;
import com.codenameone.examples.papercut.play.ArenaView;
import com.codenameone.examples.papercut.play.Hud;
import com.codenameone.examples.papercut.play.Sfx;

/// A run: the [ArenaView] filling the screen, the [Hud] over it, a pause
/// button, and the card that comes up when the last quill is lost.
public final class PlayForm extends Form implements ArenaView.Host {
    /// Told how a run ended, to submit it and move on.
    public interface Outcome {
        void finished(Arena arena, PlayForm form);
    }

    private final ArenaView view;
    private final Hud hud;
    private final Container layers;
    private final int mode;
    private final long seed;
    private final Outcome outcome;
    private Container overlay;

    public PlayForm(int mode, long seed, Outcome outcome) {
        super(new BorderLayout());
        this.mode = mode;
        this.seed = seed;
        this.outcome = outcome;
        setUIID("Form");
        setScrollable(false);
        getToolbar().setVisible(false);
        getToolbar().setUIID("Container");
        view = new ArenaView(new Arena(mode, seed));
        view.setHost(this);
        hud = new Hud(view);
        Button pause = new Button("", "HudButton");
        FontImage.setMaterialIcon(pause, FontImage.MATERIAL_PAUSE, 4);
        pause.addActionListener(e -> pause());
        layers = new Container(new LayeredLayout());
        layers.add(view).add(hud);
        layers.add(FlowLayout.encloseRightBottom(pause));
        add(BorderLayout.CENTER, layers);
        if (mode == Arena.DAILY) {
            hud.banner("Today's sheet");
        } else if (mode == Arena.FOLD) {
            hud.banner("Fly through the gates");
        }
        addShowListener(e -> view.start());
    }

    public ArenaView getView() {
        return view;
    }

    @Override
    public void hudChanged() {
        CN.callSerially(hud::repaint);
    }

    @Override
    public void runOver(Arena arena) {
        if (outcome != null) {
            outcome.finished(arena, this);
        } else {
            showOver(arena, null);
        }
    }

    /// The end-of-run card: the score, a note (a new best, a rank), and the
    /// way on.
    public void showOver(Arena arena, String note) {
        Container card = new Container(BoxLayout.y(), "Card");
        card.add(new Label(mode == Arena.FOLD ? "Folded!" : "Torn up!", "CardTitle"));
        card.add(new Label(Hud.format(arena.score), "BigScore"));
        String line = arena.kills + " cut out  -  " + (int) arena.time + " s";
        if (mode == Arena.FOLD) {
            line = arena.gates + " gates  -  " + (int) arena.time + " s";
        }
        card.add(new Label(line, "CardText"));
        if (note != null) {
            card.add(new Label(note, "Accent"));
        }
        Button again = new Button("Again");
        again.addActionListener(e -> new PlayForm(mode, mode == Arena.DAILY ? seed : System.currentTimeMillis(),
                outcome).show());
        Button home = new Button("Menu", "Secondary");
        home.addActionListener(e -> Nav.home());
        card.add(BoxLayout.encloseX(again, home));
        showOverlay(card);
    }

    private void pause() {
        if (overlay != null || view.getArena().over) {
            return;
        }
        view.pause();
        Container card = new Container(BoxLayout.y(), "Card");
        card.add(new Label("Paused", "CardTitle"));
        Button resume = new Button("Resume");
        resume.addActionListener(e -> {
            hideOverlay();
            view.resume();
        });
        Button sound = new Button(Sfx.isMuted() ? "Sound on" : "Sound off", "Secondary");
        sound.addActionListener(e -> {
            Sfx.setMuted(!Sfx.isMuted());
            sound.setText(Sfx.isMuted() ? "Sound on" : "Sound off");
        });
        Button quit = new Button("Quit run", "Secondary");
        quit.addActionListener(e -> {
            view.stop();
            Nav.home();
        });
        card.add(resume).add(sound).add(quit);
        showOverlay(card);
    }

    /// An in-form overlay rather than a Dialog: a dialog over a GPU view
    /// paints grey on some ports, and this keeps the page visible behind it.
    private void showOverlay(Container card) {
        hideOverlay();
        overlay = new Container(new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER), "Scrim");
        overlay.add(BorderLayout.CENTER, card);
        layers.add(overlay);
        layers.revalidate();
    }

    private void hideOverlay() {
        if (overlay != null) {
            overlay.remove();
            overlay = null;
            layers.revalidate();
        }
    }

    @Override
    protected void onShowCompleted() {
        super.onShowCompleted();
        Sfx.play(Sfx.FLIP);
    }
}
