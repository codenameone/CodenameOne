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
package com.codenameone.examples.reel.player;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.reel.Telemetry;
import com.codenameone.examples.reel.api.AnswerDto;
import com.codenameone.examples.reel.api.AnswerResultDto;
import com.codenameone.examples.reel.api.InteractionDto;
import com.codenameone.examples.reel.net.Api;
import com.codenameone.examples.reel.net.Net;
import com.codenameone.examples.reel.ui.Lang;
import com.codenameone.examples.reel.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// The moments a video stops and asks: a quiz (graded on the server, which
/// alone knows the answers, and counted toward a course's certificate), a
/// poll (shows how everyone answered) and a branch (each choice continues
/// with another video of the title).
final class Interactions {
    static final String QUIZ = "QUIZ";
    static final String POLL = "POLL";
    static final String BRANCH = "BRANCH";

    private Interactions() {
    }

    static void show(final PlayerForm player, final InteractionDto i) {
        final Dialog d = new Dialog(Lang.tr(QUIZ.equals(i.kind()) ? "Quiz" : POLL.equals(i.kind()) ? "Poll"
                : "Choose"), new BorderLayout());
        d.setName("interaction");
        Container body = new Container(BoxLayout.y());
        body.add(new SpanLabel(i.prompt(), "SvText"));
        final List<CheckBox> boxes = new ArrayList<CheckBox>();
        final Container result = new Container(BoxLayout.y());
        for (int iter = 0; i.options() != null && iter < i.options().size(); iter++) {
            final int index = iter;
            if (QUIZ.equals(i.kind())) {
                CheckBox c = new CheckBox(i.options().get(iter));
                c.setName("option-" + iter);
                boxes.add(c);
                body.add(c);
            } else {
                Button b = Ui.secondary(i.options().get(iter), "option-" + iter, e -> {
                    List<Integer> one = new ArrayList<Integer>();
                    one.add(Integer.valueOf(index));
                    answer(player, d, i, one, result);
                });
                body.add(b);
            }
        }
        body.add(result);
        if (QUIZ.equals(i.kind())) {
            body.add(Ui.primary("Check", "submitAnswer", e -> {
                List<Integer> chosen = new ArrayList<Integer>();
                for (int iter = 0; iter < boxes.size(); iter++) {
                    if (boxes.get(iter).isSelected()) {
                        chosen.add(Integer.valueOf(iter));
                    }
                }
                answer(player, d, i, chosen, result);
            }));
        }
        if (!i.required()) {
            body.add(Ui.link("Skip", "skipInteraction", e -> {
                d.dispose();
                player.resume();
            }));
        }
        d.add(BorderLayout.CENTER, body);
        d.showPacked(BorderLayout.CENTER, false);
    }

    private static void answer(final PlayerForm player, final Dialog d, final InteractionDto i,
            List<Integer> choices, final Container result) {
        Api.learning().answer(new AnswerDto(i.id(), player.sessionId(), choices, null), Net.to(r -> {
            Telemetry.answered(i.kind(), r.correct());
            if (BRANCH.equals(i.kind()) && r.nextEpisodeId() != null && r.nextEpisodeId().length() > 0) {
                d.dispose();
                player.jump(r.nextEpisodeId());
                return;
            }
            result.removeAll();
            result.add(new SpanLabel(text(i, r), r.correct() || !QUIZ.equals(i.kind()) ? "SvText" : "SvError"));
            result.add(Ui.primary("Continue", "continue", e -> {
                d.dispose();
                player.resume();
            }));
            d.revalidate();
            d.growOrShrink();
        }, (code, message) -> {
            result.removeAll();
            result.add(new SpanLabel(message, "SvError"));
            result.add(Ui.primary("Continue", "continue", e -> {
                d.dispose();
                player.resume();
            }));
            d.revalidate();
        }));
    }

    private static String text(InteractionDto i, AnswerResultDto r) {
        if (POLL.equals(i.kind())) {
            StringBuilder b = new StringBuilder(Lang.tr("How everyone answered:"));
            for (int iter = 0; r.tally() != null && iter < r.tally().size() && iter < i.options().size(); iter++) {
                b.append('\n').append(i.options().get(iter)).append(": ").append(r.tally().get(iter));
            }
            return b.toString();
        }
        String verdict = r.correct() ? Lang.tr("Correct") : Lang.tr("Not quite");
        String why = r.explanation() == null || r.explanation().length() == 0 ? "" : "\n" + r.explanation();
        return verdict + why + "\n" + Lang.tr("Course score {0}%", String.valueOf(r.scorePercent()));
    }
}
