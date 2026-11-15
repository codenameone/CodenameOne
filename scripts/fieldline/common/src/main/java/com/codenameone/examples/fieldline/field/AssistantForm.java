package com.codenameone.examples.fieldline.field;

import com.codename1.appkit.Net;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.fieldline.Telemetry;
import com.codenameone.examples.fieldline.api.AssistantReplyDto;
import com.codenameone.examples.fieldline.api.AssistantRequestDto;
import com.codenameone.examples.fieldline.api.ChatTurnDto;
import com.codenameone.examples.fieldline.api.UserDto;
import com.codenameone.examples.fieldline.dispatch.RunView;
import com.codenameone.examples.fieldline.net.Api;
import com.codenameone.examples.fieldline.net.Session;
import com.codenameone.examples.fieldline.ui.Lang;
import com.codenameone.examples.fieldline.ui.Pulse;
import com.codenameone.examples.fieldline.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// A conversation with the field assistant.
///
/// For a technician it is a second pair of eyes: opened from a job it already
/// knows the equipment, the checklist and the photos, and answers from the
/// manuals. For a dispatcher it is a way to say what happened -- "Dana is out
/// Thursday" -- and have the planner act on it; the re-plan that comes back
/// is shown as a card with every job that moved.
///
/// The model is called by the server, never from the phone, so no key is in
/// the app.
public final class AssistantForm {
    private final Form form;
    /// Everything but the title: the screen's content, or a pane of the console.
    private final Container root = new Container(new BorderLayout());
    private final String jobId;
    private final Container thread = new Container(BoxLayout.y());
    private final List<ChatTurnDto> history = new ArrayList<ChatTurnDto>();
    private final TextField input = new TextField("", "", 20, TextField.ANY);
    private final Container suggestions = new Container(new FlowLayout());
    private Component typing;

    private AssistantForm(Form on, String jobId) {
        this.jobId = jobId;
        form = on;
        thread.setUIID("FlThread");
        thread.setScrollableY(true);
        thread.setScrollVisible(false);
        thread.setName("thread");

        Container compose = new Container(new BorderLayout());
        compose.setUIID("FlCompose");
        compose.setSafeArea(true);
        input.setUIID("FlComposeField");
        input.setHint(Lang.tr("Ask anything about the work"));
        input.setName("message");
        input.setDoneListener(e -> send(input.getText()));
        Button send = Ui.round(FontImage.MATERIAL_ARROW_UPWARD, "send",
                e -> send(input.getText()));
        send.setUIID("FlSend");
        compose.add(BorderLayout.CENTER, input);
        compose.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(send));
        Container south = new Container(BoxLayout.y());
        suggestions.setUIID("FlSuggestions");
        south.add(suggestions).add(compose);

        root.add(BorderLayout.CENTER, thread);
        root.add(BorderLayout.SOUTH, south);
        welcome();
    }

    /// @param jobId the job the question is about, or null for one in general
    public static void show(Form previous, String jobId) {
        AssistantForm chat = new AssistantForm(Ui.form(jobId == null ? "Assistant"
                : Lang.tr("Assistant") + Ui.DOT + jobId, "Assistant"), jobId);
        Ui.back(chat.form, previous);
        chat.form.add(BorderLayout.CENTER, chat.root);
        chat.form.show();
    }

    /// The conversation as a pane of a screen that is already up: `on` is the
    /// screen it is on, where a re-plan's card opens its details from.
    public static Component pane(Form on, String jobId) {
        AssistantForm chat = new AssistantForm(on, jobId);
        chat.root.setName("Assistant");
        return chat.root;
    }

    private void welcome() {
        boolean desk = jobId == null && isDispatchMode();
        Container hello = new Container(BoxLayout.y());
        hello.setUIID("FlAssistantHello");
        Label spark = new Label("", "FlAssistantSpark");
        Ui.icon(spark, FontImage.MATERIAL_AUTO_AWESOME, 7f);
        hello.add(FlowLayout.encloseCenter(spark));
        hello.add(FlowLayout.encloseCenter(Ui.label(desk ? "Ask the dispatcher's assistant"
                : jobId == null ? "Your field assistant" : "Ask about this job",
                "FlAssistantTitle")));
        hello.add(Ui.text(desk
                ? "Tell it who is out, which jobs are urgent, or ask who is free. It plans "
                + "with the same optimizer as the board."
                : "It knows your equipment manuals and, from a job, the checklist and the "
                + "photos you took.", "FlAssistantText"));
        thread.add(hello);
        String[] ideas = desk
                ? new String[] {"Dana Okafor is out tomorrow, cover her jobs",
                    "Who has the most driving today?", "What is still unassigned?"}
                : jobId == null
                        ? new String[] {"How do I reset a tripped rooftop unit?",
                            "Torque for a 3/4 inch flare fitting?",
                            "What does error E4 mean on a boiler?"}
                        : new String[] {"What could cause this?", "Which parts should I bring?",
                            "Is it safe to keep running?"};
        for (final String idea : ideas) {
            Button chip = new Button(Lang.tr(idea), "FlSuggestion");
            chip.addActionListener(e -> send(chip.getText()));
            suggestions.add(chip);
        }
    }

    private static boolean isDispatchMode() {
        UserDto user = Session.user();
        return user != null && com.codenameone.examples.fieldline.ui.Nav.DISPATCH.equals(
                com.codenameone.examples.fieldline.ui.Nav.mode(user));
    }

    private void send(String text) {
        final String message = text == null ? "" : text.trim();
        if (message.length() == 0) {
            return;
        }
        input.setText("");
        suggestions.removeAll();
        thread.add(bubble(message, true));
        typing = typingBubble();
        thread.add(typing);
        scrollToEnd();
        Telemetry.assistantAsked(jobId == null ? "general" : "job");
        AssistantRequestDto request = new AssistantRequestDto(message,
                new ArrayList<ChatTurnDto>(history), jobId);
        history.add(new ChatTurnDto("user", message));
        Api.field().ask(request, Net.to(this::answered, (status, message2) -> {
            dropTyping();
            thread.add(bubble(Lang.tr("I could not answer that: {0}", message2), false));
            scrollToEnd();
        }));
    }

    private void answered(AssistantReplyDto reply) {
        dropTyping();
        history.add(new ChatTurnDto("assistant", reply.reply()));
        thread.add(bubble(reply.reply(), false));
        if (reply.run() != null) {
            thread.add(RunView.card(form, reply.run()));
        }
        if (reply.actions() != null) {
            for (final String action : reply.actions()) {
                Button chip = new Button(action, "FlSuggestion");
                chip.addActionListener(e -> send(chip.getText()));
                suggestions.add(chip);
            }
        }
        if ("demo".equals(reply.source())) {
            thread.add(FlowLayout.encloseCenter(Ui.label("Demo answers: no model is configured "
                    + "on this server", "FlDemoNote")));
        }
        scrollToEnd();
    }

    private Component bubble(String text, boolean mine) {
        Container row = new Container(new BorderLayout());
        row.setUIID("FlBubbleRow");
        Container bubble = new Container(BoxLayout.y());
        bubble.setUIID(mine ? "FlBubbleMine" : "FlBubble");
        bubble.add(Ui.text(text, mine ? "FlBubbleTextMine" : "FlBubbleText"));
        if (mine) {
            row.add(BorderLayout.EAST, bubble);
        } else {
            Label spark = new Label("", "FlBubbleAvatar");
            Ui.icon(spark, FontImage.MATERIAL_AUTO_AWESOME, 3f);
            row.add(BorderLayout.WEST, FlowLayout.encloseIn(spark));
            row.add(BorderLayout.CENTER, bubble);
        }
        return row;
    }

    private Component typingBubble() {
        Container row = new Container(new BorderLayout());
        row.setUIID("FlBubbleRow");
        Container bubble = new Container(new FlowLayout());
        bubble.setUIID("FlBubble");
        bubble.add(new Pulse());
        bubble.add(Ui.label("Thinking...", "FlBubbleText"));
        row.add(BorderLayout.WEST, bubble);
        row.setName("typing");
        return row;
    }

    private void dropTyping() {
        if (typing != null) {
            typing.remove();
            typing = null;
        }
    }

    private void scrollToEnd() {
        form.revalidate();
        CN.callSerially(() -> {
            if (thread.getComponentCount() > 0) {
                thread.scrollComponentToVisible(thread.getComponentAt(
                        thread.getComponentCount() - 1));
            }
        });
    }
}
