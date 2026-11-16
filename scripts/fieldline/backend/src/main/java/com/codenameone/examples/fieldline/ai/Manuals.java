package com.codenameone.examples.fieldline.ai;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.fieldline.domain.ManualChunk;
import com.codenameone.examples.fieldline.repository.ManualChunkRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The equipment manuals the assistant answers from, and the search over them.
///
/// Retrieval is lexical, BM25 over the passages' words, and boosted for the
/// asset the question is about. It needs no model and no index service, its
/// results are the same on every run, and for a library of manuals --
/// short passages, technical words, part numbers, fault codes -- it finds what
/// an embedding search would, and finds "E7" where an embedding often does
/// not. The passages found go to the model with their titles, and the model is
/// told to cite them, so a technician can check the answer against the page.
@Component
public class Manuals {
    private static final double K1 = 1.2;
    private static final double B = 0.75;
    private static final Set<String> STOP = new HashSet<String>();

    static {
        String[] words = {"the", "a", "an", "and", "or", "of", "to", "in", "on", "for", "is",
            "it", "with", "at", "by", "be", "are", "this", "that", "from", "as", "if", "not",
            "what", "how", "do", "does", "i", "my", "can", "should", "when", "why", "there"};
        for (String w : words) {
            STOP.add(w);
        }
    }

    private final ManualChunkRepository chunks;

    public Manuals(ManualChunkRepository chunks) {
        this.chunks = chunks;
    }

    /// A passage found, and how well it matched.
    public static final class Hit {
        public final ManualChunk chunk;
        public final double score;

        Hit(ManualChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }
    }

    /// The best `limit` passages for `query`. Passages about `assetType`
    /// score half again as much, so the boiler's manual wins a question asked
    /// at a boiler, but a good match elsewhere still shows.
    @Transactional(readOnly = true)
    public List<Hit> search(String query, String assetType, int limit) {
        List<String> q = terms(query);
        List<ManualChunk> all = chunks.findAll();
        List<Hit> out = new ArrayList<Hit>();
        if (q.isEmpty() || all.isEmpty()) {
            return out;
        }
        Map<String, Integer> df = new HashMap<String, Integer>();
        List<List<String>> docs = new ArrayList<List<String>>();
        double total = 0;
        for (ManualChunk c : all) {
            List<String> words = Words.words(c.getTerms());
            docs.add(words);
            total += words.size();
            Set<String> seen = new HashSet<String>(words);
            for (String w : seen) {
                Integer n = df.get(w);
                df.put(w, Integer.valueOf(n == null ? 1 : n.intValue() + 1));
            }
        }
        double avg = total / all.size();
        for (int d = 0; d < all.size(); d++) {
            List<String> words = docs.get(d);
            double score = 0;
            for (String term : q) {
                int tf = 0;
                for (String w : words) {
                    if (w.equals(term)) {
                        tf++;
                    }
                }
                if (tf == 0) {
                    continue;
                }
                Integer n = df.get(term);
                double idf = Math.log(1 + (all.size() - n.intValue() + 0.5) / (n.intValue() + 0.5));
                score += idf * tf * (K1 + 1) / (tf + K1 * (1 - B + B * words.size() / avg));
            }
            if (score <= 0) {
                continue;
            }
            ManualChunk c = all.get(d);
            if (assetType != null && assetType.equals(c.getAssetType())) {
                score *= 1.5;
            }
            out.add(new Hit(c, score));
        }
        if (assetType != null) {
            // The equipment in front of the technician is what they asked
            // about. Other manuals answer only when none of its own does.
            List<Hit> own = new ArrayList<Hit>();
            for (Hit h : out) {
                if (assetType.equals(h.chunk.getAssetType())) {
                    own.add(h);
                }
            }
            if (!own.isEmpty()) {
                out = own;
            }
        }
        sort(out);
        return out.size() > limit ? new ArrayList<Hit>(out.subList(0, limit)) : out;
    }

    private static void sort(List<Hit> hits) {
        for (int i = 1; i < hits.size(); i++) {
            Hit h = hits.get(i);
            int j = i - 1;
            while (j >= 0 && hits.get(j).score < h.score) {
                hits.set(j + 1, hits.get(j));
                j--;
            }
            hits.set(j + 1, h);
        }
    }

    /// The words of a query worth matching on.
    static List<String> terms(String text) {
        List<String> out = new ArrayList<String>();
        for (String w : Words.words(Words.fold(text))) {
            if (!STOP.contains(w) && !out.contains(w)) {
                out.add(w);
            }
        }
        return out;
    }

    /// Adds a passage; the words are folded once, here.
    @Transactional(rollbackFor = IOException.class)
    public void add(String id, String assetType, String title, String body) throws IOException {
        if (chunks.existsById(id)) {
            return;
        }
        ManualChunk c = new ManualChunk();
        c.setId(id);
        c.setAssetType(assetType);
        c.setTitle(title);
        c.setBody(body.length() > 2000 ? body.substring(0, 2000) : body);
        String folded = Words.fold(title + " " + assetType + " " + body);
        c.setTerms(folded.length() > 2000 ? folded.substring(0, 2000) : folded);
        chunks.insert(c);
    }

    @Transactional(readOnly = true)
    public long count() {
        return chunks.count();
    }
}
