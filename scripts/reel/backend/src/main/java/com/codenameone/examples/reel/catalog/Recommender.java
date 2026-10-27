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
package com.codenameone.examples.reel.catalog;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.reel.Rows;
import com.codenameone.examples.reel.domain.Progress;
import com.codenameone.examples.reel.domain.Title;
import com.codenameone.examples.reel.domain.TitleRating;
import com.codenameone.examples.reel.repository.ProgressRepository;
import com.codenameone.examples.reel.repository.TitleRatingRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Recommendations, from two signals:
///
/// - **Taste.** Each genre and tag of a title the profile watched counts
///   once; finishing it counts double; rating it adds or subtracts by the
///   stars (a one-star title pushes its genres down). A candidate scores the
///   sum of its genres' and tags' weights.
/// - **Co-watching.** Profiles that watched the same titles as this one --
///   the titles they also watched score, weighted by how much they overlap.
///   This is item-to-item collaborative filtering at its simplest; it finds
///   what taste alone cannot, such as a course people take after another.
///
/// Popularity and the average rating break ties, so a new profile with no
/// history still gets a sensible row. Titles the profile finished are left
/// out.
@Component
public class Recommender {
    /// How many other profiles' histories one recommendation reads, at most.
    static final int NEIGHBOURS = 200;

    private final ProgressRepository progress;
    private final TitleRatingRepository ratings;

    public Recommender(ProgressRepository progress, TitleRatingRepository ratings) {
        this.progress = progress;
        this.ratings = ratings;
    }

    public static final class Scored {
        public final Title title;
        public final double score;

        public Scored(Title title, double score) {
            this.title = title;
            this.score = score;
        }
    }

    @Transactional(readOnly = true)
    public List<Scored> forProfile(String profileId, List<Title> candidates) {
        Map<String, Title> byId = new HashMap<String, Title>();
        for (int iter = 0; iter < candidates.size(); iter++) {
            byId.put(candidates.get(iter).getId(), candidates.get(iter));
        }
        List<Progress> history = progress.findTop50ByProfileIdOrderByUpdatedAtDesc(profileId);
        Map<String, Double> taste = new HashMap<String, Double>();
        Set<String> watched = new HashSet<String>();
        Set<String> finished = new HashSet<String>();
        for (int iter = 0; iter < history.size(); iter++) {
            Progress p = history.get(iter);
            watched.add(p.getTitleId());
            if (p.isCompleted()) {
                finished.add(p.getTitleId());
            }
            Title t = byId.get(p.getTitleId());
            if (t != null) {
                weigh(taste, t, p.isCompleted() ? 2.0 : 1.0);
            }
        }
        List<TitleRating> rated = ratings.findByProfileId(profileId);
        for (int iter = 0; iter < rated.size(); iter++) {
            Title t = byId.get(rated.get(iter).getTitleId());
            if (t != null) {
                weigh(taste, t, rated.get(iter).getStars() - 3.0);
            }
        }
        Map<String, Double> together = coWatched(profileId, watched);
        List<Scored> out = new ArrayList<Scored>();
        for (int iter = 0; iter < candidates.size(); iter++) {
            Title t = candidates.get(iter);
            if (finished.contains(t.getId()) || watched.contains(t.getId())) {
                continue;
            }
            double s = match(taste, t);
            Double co = together.get(t.getId());
            if (co != null) {
                s += 2.0 * co.doubleValue();
            }
            s += Math.log(1 + t.getViewCount()) / 5.0 + Catalog.average(t) / 5.0;
            out.add(new Scored(t, round(s)));
        }
        sortScored(out);
        return out.size() > Catalog.ROW ? new ArrayList<Scored>(out.subList(0, Catalog.ROW)) : out;
    }

    /// Titles like `seed`: shared genres and tags, and what its viewers also
    /// watched.
    @Transactional(readOnly = true)
    public List<Scored> similar(Title seed, List<Title> candidates) {
        List<Scored> out = new ArrayList<Scored>();
        if (seed == null) {
            return out;
        }
        Map<String, Double> taste = new HashMap<String, Double>();
        weigh(taste, seed, 1.0);
        Map<String, Double> together = new HashMap<String, Double>();
        List<Progress> viewers = progress.findByTitleId(seed.getId());
        Set<String> profiles = new HashSet<String>();
        for (int iter = 0; iter < viewers.size() && profiles.size() < NEIGHBOURS; iter++) {
            profiles.add(viewers.get(iter).getProfileId());
        }
        for (String p : profiles) {
            Set<String> theirs = new HashSet<String>();
            List<Progress> rows = progress.findTop50ByProfileIdOrderByUpdatedAtDesc(p);
            for (int iter = 0; iter < rows.size(); iter++) {
                theirs.add(rows.get(iter).getTitleId());
            }
            for (String t : theirs) {
                Double d = together.get(t);
                together.put(t, Double.valueOf((d == null ? 0 : d.doubleValue()) + 1.0 / profiles.size()));
            }
        }
        for (int iter = 0; iter < candidates.size(); iter++) {
            Title t = candidates.get(iter);
            if (t.getId().equals(seed.getId())) {
                continue;
            }
            double s = match(taste, t);
            Double co = together.get(t.getId());
            if (co != null) {
                s += 3.0 * co.doubleValue();
            }
            if (s <= 0) {
                continue;
            }
            out.add(new Scored(t, round(s + Catalog.average(t) / 10.0)));
        }
        sortScored(out);
        return out.size() > 12 ? new ArrayList<Scored>(out.subList(0, 12)) : out;
    }

    private Map<String, Double> coWatched(String profileId, Set<String> watched) {
        Map<String, Double> out = new HashMap<String, Double>();
        if (watched.isEmpty()) {
            return out;
        }
        Map<String, Integer> overlap = new HashMap<String, Integer>();
        for (String titleId : watched) {
            List<Progress> rows = progress.findByTitleId(titleId);
            for (int iter = 0; iter < rows.size() && overlap.size() < NEIGHBOURS; iter++) {
                String other = rows.get(iter).getProfileId();
                if (!other.equals(profileId)) {
                    Integer n = overlap.get(other);
                    overlap.put(other, Integer.valueOf(n == null ? 1 : n.intValue() + 1));
                }
            }
        }
        for (Map.Entry<String, Integer> e : overlap.entrySet()) {
            double weight = e.getValue().doubleValue() / watched.size();
            List<Progress> rows = progress.findTop50ByProfileIdOrderByUpdatedAtDesc(e.getKey());
            Set<String> counted = new HashSet<String>();
            for (int iter = 0; iter < rows.size(); iter++) {
                String t = rows.get(iter).getTitleId();
                if (counted.add(t)) {
                    Double d = out.get(t);
                    out.put(t, Double.valueOf((d == null ? 0 : d.doubleValue()) + weight));
                }
            }
        }
        return out;
    }

    private static void weigh(Map<String, Double> taste, Title t, double by) {
        List<String> keys = Rows.split(t.getGenres());
        List<String> tags = Rows.split(t.getTags());
        for (int iter = 0; iter < tags.size(); iter++) {
            keys.add("#" + tags.get(iter));
        }
        for (int iter = 0; iter < keys.size(); iter++) {
            Double d = taste.get(keys.get(iter));
            taste.put(keys.get(iter), Double.valueOf((d == null ? 0 : d.doubleValue()) + by));
        }
    }

    private static double match(Map<String, Double> taste, Title t) {
        double s = 0;
        List<String> g = Rows.split(t.getGenres());
        for (int iter = 0; iter < g.size(); iter++) {
            Double d = taste.get(g.get(iter));
            s += d == null ? 0 : d.doubleValue();
        }
        List<String> tags = Rows.split(t.getTags());
        for (int iter = 0; iter < tags.size(); iter++) {
            Double d = taste.get("#" + tags.get(iter));
            s += d == null ? 0 : 0.5 * d.doubleValue();
        }
        return s;
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    static void sortScored(List<Scored> list) {
        for (int i = 1; i < list.size(); i++) {
            Scored s = list.get(i);
            int j = i - 1;
            while (j >= 0 && list.get(j).score < s.score) {
                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, s);
        }
    }
}
