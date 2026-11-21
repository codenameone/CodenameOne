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
package com.codenameone.examples.bazaar.journey;

import com.codenameone.examples.bazaar.api.NodeParamDto;
import com.codenameone.examples.bazaar.api.NodeTypeDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/// The steps a customer journey is drawn from. The ids are what a graph's
/// nodes name in `data.type`; the ports are what its edges name.
public final class JourneyCatalog {
    public static final String ENTRY_SEGMENT = "journey.entry.segment";
    public static final String ENTRY_TRIGGER = "journey.entry.trigger";
    public static final String WAIT = "journey.wait";
    public static final String SPLIT_CONDITION = "journey.split.condition";
    public static final String SPLIT_AB = "journey.split.ab";
    public static final String SEND_EMAIL = "journey.send.email";
    public static final String SEND_SMS = "journey.send.sms";
    public static final String SEND_PUSH = "journey.send.push";
    public static final String POINTS_ADD = "journey.points.add";
    public static final String TAG_ADD = "journey.tag.add";
    public static final String EXIT = "journey.exit";
    /// The one event a trigger entry listens for today.
    public static final String ORDER_PAID = "order.paid";
    public static final String ENTRY = "ENTRY";
    public static final String STEP = "STEP";
    public static final String SPLIT = "SPLIT";
    public static final String END = "END";
    public static final String IN = "in";
    public static final String OUT = "out";
    public static final String YES = "yes";
    public static final String NO = "no";
    public static final String A = "a";
    public static final String B = "b";

    private static final List<NodeTypeDto> ALL = build();

    private JourneyCatalog() {
    }

    public static List<NodeTypeDto> all() {
        return new ArrayList<NodeTypeDto>(ALL);
    }

    public static NodeTypeDto find(String type) {
        for (int iter = 0; iter < ALL.size(); iter++) {
            if (ALL.get(iter).type().equals(type)) {
                return ALL.get(iter);
            }
        }
        return null;
    }

    public static boolean isEntry(String type) {
        return ENTRY_SEGMENT.equals(type) || ENTRY_TRIGGER.equals(type);
    }

    private static List<NodeTypeDto> build() {
        List<NodeTypeDto> all = new ArrayList<NodeTypeDto>();
        all.add(new NodeTypeDto(ENTRY_SEGMENT, ENTRY, "Joins a segment",
                "Everyone in the segment enters, and anyone who matches it later", none(),
                Arrays.asList(OUT), Arrays.asList(param("segmentId", "Segment", "segment", ""))));
        all.add(new NodeTypeDto(ENTRY_TRIGGER, ENTRY, "Something happens",
                "A customer enters when the event happens to them", none(), Arrays.asList(OUT),
                Arrays.asList(param("event", "Event", "choice", ORDER_PAID))));
        all.add(new NodeTypeDto(WAIT, STEP, "Wait", "Waits this many minutes before the next step",
                Arrays.asList(IN), Arrays.asList(OUT), Arrays.asList(param("minutes", "Minutes", "number",
                "1440"))));
        all.add(new NodeTypeDto(SPLIT_CONDITION, SPLIT, "Condition",
                "Yes when the customer is in the segment, or has the tag", Arrays.asList(IN),
                Arrays.asList(YES, NO), Arrays.asList(param("segmentId", "Segment", "segment", ""),
                param("tag", "Tag", "text", ""))));
        all.add(new NodeTypeDto(SPLIT_AB, SPLIT, "A/B split", "Sends this percent down A, the rest down B",
                Arrays.asList(IN), Arrays.asList(A, B), Arrays.asList(param("percent", "Percent to A",
                "number", "50"))));
        all.add(new NodeTypeDto(SEND_EMAIL, STEP, "Send e-mail", "Mails the customer, unless they opted out",
                Arrays.asList(IN), Arrays.asList(OUT), Arrays.asList(param("subject", "Subject", "text", ""),
                param("body", "Message", "longtext", ""))));
        all.add(new NodeTypeDto(SEND_SMS, STEP, "Send SMS", "Texts the customer, unless they opted out",
                Arrays.asList(IN), Arrays.asList(OUT), Arrays.asList(param("body", "Message", "longtext", ""))));
        all.add(new NodeTypeDto(SEND_PUSH, STEP, "Send push", "Notifies the customer's devices",
                Arrays.asList(IN), Arrays.asList(OUT), Arrays.asList(param("title", "Title", "text", ""),
                param("body", "Message", "text", ""))));
        all.add(new NodeTypeDto(POINTS_ADD, STEP, "Add points", "Adds loyalty points, once per customer",
                Arrays.asList(IN), Arrays.asList(OUT), Arrays.asList(param("points", "Points", "number",
                "100"))));
        all.add(new NodeTypeDto(TAG_ADD, STEP, "Add tag", "Tags the customer", Arrays.asList(IN),
                Arrays.asList(OUT), Arrays.asList(param("tag", "Tag", "text", ""))));
        all.add(new NodeTypeDto(EXIT, END, "Exit", "The customer leaves the journey", Arrays.asList(IN),
                none(), new ArrayList<NodeParamDto>()));
        return all;
    }

    private static List<String> none() {
        return new ArrayList<String>();
    }

    private static NodeParamDto param(String name, String label, String kind, String value) {
        return new NodeParamDto(name, label, kind, value, new ArrayList<String>());
    }
}
