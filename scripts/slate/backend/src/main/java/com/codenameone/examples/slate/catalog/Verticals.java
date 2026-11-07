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
package com.codenameone.examples.slate.catalog;

// tag::slate-verticals[]
/// The kinds of business Slate is set up for, and what each calls things.
///
/// A vertical is words and a colour, nothing more. The booking rules, the
/// slot engine and every screen are the same for a salon and a consultancy;
/// what differs is that one books a stylist and a chair and the other a
/// consultant and a meeting room. A new vertical is a row here and, for the
/// demo, a set of services in `DemoBusiness`.
public final class Verticals {
    public static final String SALON = "SALON";
    public static final String TUTOR = "TUTOR";
    public static final String TRAINER = "TRAINER";
    public static final String CONSULTANT = "CONSULTANT";
    public static final String CLINIC = "CLINIC";

    public static final String[] ALL = {SALON, TUTOR, TRAINER, CONSULTANT, CLINIC};

    /// vertical, provider, providers, client, service, resource, accent
    private static final String[][] WORDS = {
        {SALON, "Stylist", "Stylists", "Client", "Treatment", "Chair", "#c2185b"},
        {TUTOR, "Tutor", "Tutors", "Student", "Lesson", "Room", "#3949ab"},
        {TRAINER, "Trainer", "Trainers", "Client", "Session", "Studio", "#00897b"},
        {CONSULTANT, "Consultant", "Consultants", "Client", "Meeting", "Meeting room",
            "#455a64"},
        {CLINIC, "Practitioner", "Practitioners", "Patient", "Appointment", "Treatment room",
            "#1e88e5"},
    };
    // end::slate-verticals[]

    private Verticals() {
    }

    /// The words of `vertical`, or of a consultancy for one this does not
    /// know: its words are the most general.
    public static String[] words(String vertical) {
        for (int iter = 0; iter < WORDS.length; iter++) {
            if (WORDS[iter][0].equals(vertical)) {
                return WORDS[iter];
            }
        }
        return WORDS[3];
    }

    public static boolean known(String vertical) {
        for (int iter = 0; iter < ALL.length; iter++) {
            if (ALL[iter].equals(vertical)) {
                return true;
            }
        }
        return false;
    }
}
