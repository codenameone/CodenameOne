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
package com.codenameone.examples.brickyard.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codenameone.examples.brickyard.api.ExperienceDto;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/// The pure logic behind home, the avatar and the parental limit.
class HomeLogicTest {
    private static ExperienceDto x(String id, String genre, long visits, long up, long down) {
        return new ExperienceDto(id, "owner", id, "", genre, 8, "public", "all", 1, visits, 0, up, down);
    }

    @Test
    void rowsAreRankedAndGrouped() {
        List<ExperienceDto> all = Arrays.asList(x("a", "Obby", 10, 1, 0), x("b", "Tycoon", 500, 40, 10),
                x("c", "Obby", 90, 300, 20));
        assertEquals("b", Experiences.trending(all).get(0).id());
        assertEquals("c", Experiences.featured(all).get(0).id());
        assertEquals(Arrays.asList("Obby", "Tycoon"), new java.util.ArrayList<>(Experiences.genres(all).keySet()));
        assertEquals(80, Experiences.rating(all.get(1)));
        assertEquals(-1, Experiences.rating(x("d", "", 0, 0, 0)));
    }

    @Test
    void recentKeepsTheNewestFirstWithoutRepeats() {
        String s = Experiences.pushRecent("", "a");
        s = Experiences.pushRecent(s, "b");
        s = Experiences.pushRecent(s, "a");
        assertEquals("a,b", s);
    }

    @Test
    void avatarRoundTripsAndToleratesGarbage() {
        Avatar a = Avatar.decode("").with(Avatar.TORSO, 0x8a5cf0);
        assertEquals(0x8a5cf0, Avatar.decode(a.encode()).color(Avatar.TORSO));
        Avatar bad = Avatar.decode("zz,12");
        assertEquals(Avatar.decode("").color(Avatar.HEAD), bad.color(Avatar.HEAD));
    }

    @Test
    void spendingLimit() {
        assertTrue(SettingsForm.fits(-1, 5000, 1000));
        assertTrue(SettingsForm.fits(1000, 0, 1000));
        assertFalse(SettingsForm.fits(1000, 400, 1000));
        assertEquals(10, SettingsForm.age(2016, 2026));
    }

    @Test
    void birthYearMustBeFourDigits() {
        assertEquals(2010, RegisterForm.parseYear(" 2010 "));
        assertEquals(-1, RegisterForm.parseYear("20x0"));
        assertEquals(-1, RegisterForm.parseYear("99"));
    }
}
