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
package com.codenameone.examples.bazaar.merchant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// What the inbox shows, kept current by the chat socket: the conversations
/// newest first with their unread counts, and the open thread's lines.
///
/// The REST list is the source of truth; frames only move it along between
/// loads. A frame about a conversation the board has never seen asks for a
/// reload rather than inventing a row from half the facts.
final class InboxBoard {
    /// One conversation in the list.
    static final class Row {
        final String id;
        final String name;
        final String subject;
        String status;
        final String assignee;
        int messages;
        int unread;
        long lastAt;
        /// A call rings on it: `"video"`, `"voice"` or empty.
        String ringing = "";

        Row(String id, String name, String subject, String status, String assignee,
                int messages, int unread, long lastAt) {
            this.id = id;
            this.name = name;
            this.subject = subject;
            this.status = status;
            this.assignee = assignee;
            this.messages = messages;
            this.unread = unread;
            this.lastAt = lastAt;
        }
    }

    /// One line of the open thread.
    static final class Line {
        final String id;
        final String author;
        final boolean staff;
        final String body;
        final long at;

        Line(String id, String author, boolean staff, String body, long at) {
            this.id = id;
            this.author = author;
            this.staff = staff;
            this.body = body;
            this.at = at;
        }
    }

    private final List<Row> rows = new ArrayList<Row>();
    private final List<Line> lines = new ArrayList<Line>();
    private String open = "";

    void reset(List<Row> fresh) {
        rows.clear();
        rows.addAll(fresh);
    }

    List<Row> rows() {
        return rows;
    }

    Row row(String id) {
        for (int iter = 0; iter < rows.size(); iter++) {
            if (rows.get(iter).id.equals(id)) {
                return rows.get(iter);
            }
        }
        return null;
    }

    int unread() {
        int n = 0;
        for (int iter = 0; iter < rows.size(); iter++) {
            n += rows.get(iter).unread;
        }
        return n;
    }

    /// The conversation on screen, or empty. Opening it reads it.
    String open() {
        return open;
    }

    void open(String id, List<Line> thread) {
        open = id == null ? "" : id;
        lines.clear();
        if (thread != null) {
            lines.addAll(thread);
        }
        Row r = row(open);
        if (r != null) {
            r.unread = 0;
        }
    }

    void close() {
        open = "";
        lines.clear();
    }

    List<Line> lines() {
        return lines;
    }

    /// The newest server time in the open thread, for a `seen` frame, or 0.
    long newestAt() {
        long at = 0;
        for (int iter = 0; iter < lines.size(); iter++) {
            at = Math.max(at, lines.get(iter).at);
        }
        return at;
    }

    /// The thread the server sent for `watch`; only for the open conversation.
    boolean thread(Map frame) {
        String id = ChatFrames.text(frame, "chat");
        if (!id.equals(open)) {
            return false;
        }
        lines.clear();
        List all = ChatFrames.list(frame, "messages");
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter) instanceof Map) {
                lines.add(line((Map) all.get(iter)));
            }
        }
        return true;
    }

    private static Line line(Map m) {
        return new Line(ChatFrames.text(m, "id"), ChatFrames.text(m, "author"),
                ChatFrames.truth(m, "staff"), ChatFrames.text(m, "body"),
                ChatFrames.number(m, "at"));
    }

    /// Outcomes of [#message].
    static final int IGNORED = 0;
    static final int LISTED = 1;
    static final int SHOWN = 2;
    static final int UNKNOWN = 3;

    /// A `message` frame: the row moves to the top and counts it, unread when
    /// a shopper wrote it into a conversation that is not open. Answers
    /// [#SHOWN] when the line joined the open thread, [#LISTED] when only the
    /// list changed, [#UNKNOWN] when the conversation is new to the board (a
    /// reload is due) and [#IGNORED] for a line already held -- a staff
    /// member's own reply comes back this way, with no other acknowledgement.
    int message(Map frame) {
        String id = ChatFrames.text(frame, "chat");
        String messageId = ChatFrames.text(frame, "id");
        if (id.length() == 0) {
            return IGNORED;
        }
        boolean isOpen = id.equals(open);
        if (isOpen && messageId.length() > 0) {
            for (int iter = 0; iter < lines.size(); iter++) {
                if (messageId.equals(lines.get(iter).id)) {
                    return IGNORED;
                }
            }
        }
        Row r = row(id);
        if (r == null) {
            if (isOpen) {
                lines.add(line(frame));
                return SHOWN;
            }
            return UNKNOWN;
        }
        boolean staff = ChatFrames.truth(frame, "staff");
        r.messages++;
        r.lastAt = Math.max(r.lastAt, ChatFrames.number(frame, "at"));
        if (!staff && !isOpen) {
            r.unread++;
        }
        if (staff) {
            r.status = "OPEN";
        }
        rows.remove(r);
        rows.add(0, r);
        if (isOpen) {
            lines.add(line(frame));
            return SHOWN;
        }
        return LISTED;
    }

    /// A `call` frame about a conversation: marks or clears the ringing row.
    /// Answers whether the board changed.
    boolean call(Map frame) {
        Row r = row(ChatFrames.text(frame, "chat"));
        if (r == null) {
            return false;
        }
        String kind = ChatFrames.text(frame, "kind");
        String was = r.ringing;
        if (ChatFrames.RING.equals(kind)) {
            r.ringing = ChatFrames.truth(frame, "video") ? "video" : "voice";
        } else if (ChatFrames.TAKEN.equals(kind) || ChatFrames.END.equals(kind)) {
            r.ringing = "";
        }
        return !was.equals(r.ringing);
    }
}
