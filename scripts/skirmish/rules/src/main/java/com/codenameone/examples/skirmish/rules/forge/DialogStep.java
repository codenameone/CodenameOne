/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.skirmish.rules.forge;

/// One line of a mission's script: someone says something at a moment of the
/// battle, and can ask the player to do something before the battle goes on.
///
/// Dialog never reaches the simulation. The app pauses its own clock while a step
/// is on screen, and the match only advances when the app steps it, so the same
/// mission plays identically with the dialog shown, skipped or replayed.
public final class DialogStep {
    /// The tick the step appears at.
    public int tick;
    /// Who speaks: "elda", a card id (shown by its model), or "" for a narrator.
    public String speaker = "elda";
    public String text = "";
    /// Stop the clock until the player taps on (or does what is asked).
    public boolean freeze = true;
    /// When set, the player must play this card before the battle goes on; only it
    /// can be picked while the step shows.
    public String playCard;
    /// Where the finger points the card to, in milli-tiles, or -1 for anywhere.
    public int targetX = -1;
    public int targetY = -1;

    public DialogStep copy() {
        DialogStep d = new DialogStep();
        d.tick = tick;
        d.speaker = speaker;
        d.text = text;
        d.freeze = freeze;
        d.playCard = playCard;
        d.targetX = targetX;
        d.targetY = targetY;
        return d;
    }
}
