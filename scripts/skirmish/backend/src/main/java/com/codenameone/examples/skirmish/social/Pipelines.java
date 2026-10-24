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
package com.codenameone.examples.skirmish.social;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.skirmish.Clock;
import com.codenameone.examples.skirmish.Ids;
import com.codenameone.examples.skirmish.api.ConditionDto;
import com.codenameone.examples.skirmish.api.PipelineDto;
import com.codenameone.examples.skirmish.domain.AdminAction;
import com.codenameone.examples.skirmish.domain.Pipeline;
import com.codenameone.examples.skirmish.domain.PipelineHit;
import com.codenameone.examples.skirmish.domain.Profile;
import com.codenameone.examples.skirmish.push.GamePushes;
import com.codenameone.examples.skirmish.repository.AdminActionRepository;
import com.codenameone.examples.skirmish.repository.PipelineHitRepository;
import com.codenameone.examples.skirmish.repository.PipelineRepository;
import com.codenameone.examples.skirmish.repository.ProfileRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Engagement pipelines: "a player who has not played for 7 days gets a
/// push that opens the shop", "a player who reached 300 trophies and never
/// bought anything gets 20 gems".
///
/// A pipeline is a query ([Audience]) and an action. Once an hour the
/// server runs every enabled one: each matching player it has not acted on
/// within the pipeline's cool-down gets the action, and the time is kept in
/// `pipeline_hit`, so nobody is pushed twice for the same reason. Pushes go
/// through Codename One push with the campaign `pipeline-<name>`, so an open
/// is attributed to the pipeline in Codename One analytics.
// tag::skirmish-pipelines[]
@Component
public class Pipelines {
    public static final String PUSH = "push";
    public static final String GEMS = "gems";
    public static final int MAX_PIPELINES = 20;
    public static final int MAX_GEMS = 500;
    public static final long EVERY = 60L * 60 * 1000;

    private final PipelineRepository pipelines;
    private final PipelineHitRepository hits;
    private final ProfileRepository profiles;
    private final Insights insights;
    private final GamePushes pushes;
    private final AdminActionRepository audit;

    public Pipelines(PipelineRepository pipelines, PipelineHitRepository hits, ProfileRepository profiles,
            Insights insights, GamePushes pushes, AdminActionRepository audit) {
        this.pipelines = pipelines;
        this.hits = hits;
        this.profiles = profiles;
        this.insights = insights;
        this.pushes = pushes;
        this.audit = audit;
    }

    // tag::skirmish-pipelines-schedule[]
    /// Every enabled pipeline, once an hour, on one server at a time.
    /// Transactional itself: a call to [#run] from here is a call on `this`,
    /// which does not go through the transaction wrapper.
    @Scheduled(fixedDelay = EVERY, initialDelay = 5L * 60 * 1000, lock = "skirmish-pipelines")
    @Transactional(rollbackFor = IOException.class)
    public void runAll() throws IOException {
        for (Pipeline p : pipelines.byName()) {
            if (p.isEnabled()) {
                run(p.getId());
            }
        }
    }
    // end::skirmish-pipelines-schedule[]

    /// Runs one pipeline now: answers it with how many players it reached
    /// in total.
    @Transactional(rollbackFor = IOException.class)
    public PipelineDto run(String id) throws IOException {
        Pipeline p = find(id);
        long now = Clock.now();
        Audience audience = insights.audience(Audience.decode(p.getConditions()));
        // One read of this pipeline's hits, by its index, not one per player.
        Map<String, PipelineHit> last = new HashMap<>();
        for (PipelineHit h : hits.findByPipeline(p.getId())) {
            last.put(h.getUsername(), h);
        }
        List<String> due = new ArrayList<>();
        for (Profile player : audience.filter(profiles.findAll())) {
            String key = p.getId() + ":" + player.getUsername();
            PipelineHit hit = last.get(player.getUsername());
            if (hit != null && hit.getHitAt().toEpochMilli() > now - p.getCooldownDays() * Clock.DAY) {
                continue;
            }
            if (GEMS.equals(p.getAction())) {
                player.setGems(player.getGems() + p.getAmount());
                profiles.save(player);
            }
            if (hit == null) {
                hit = new PipelineHit();
                hit.setId(key);
                hit.setPipeline(p.getId());
                hit.setUsername(player.getUsername());
                hit.setHitAt(Instant.ofEpochMilli(now));
                hits.insert(hit);
            } else {
                hit.setHitAt(Instant.ofEpochMilli(now));
                hits.save(hit);
            }
            due.add(player.getUsername());
        }
        // "Reached" counts the players the pipeline acted on. A push goes to
        // the devices a player registered; one without any still had their
        // turn, and gets the gems, so they are not chosen again every hour.
        if (!due.isEmpty()) {
            pushes.broadcast(due, Ids.next(), p.getTitle(), p.getBody(), p.getScreen(), "pipeline-" + p.getName());
        }
        p.setReached(p.getReached() + due.size());
        p.setLastRun(Instant.ofEpochMilli(now));
        pipelines.save(p);
        return dto(p);
    }
    // end::skirmish-pipelines[]

    @Transactional(readOnly = true)
    public List<PipelineDto> list() {
        List<PipelineDto> out = new ArrayList<>();
        for (Pipeline p : pipelines.byName()) {
            out.add(dto(p));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public PipelineDto save(String admin, PipelineDto d) throws IOException {
        check(d);
        boolean fresh = d.id() == null || d.id().length() == 0;
        Pipeline p;
        if (fresh) {
            if (pipelines.count() >= MAX_PIPELINES) {
                throw new ResponseStatusException(400, "At most " + MAX_PIPELINES + " pipelines");
            }
            p = new Pipeline();
            p.setId(Ids.next());
            p.setCreatedBy(admin);
        } else {
            p = find(d.id());
        }
        p.setName(d.name().trim());
        p.setEnabled(d.enabled());
        p.setConditions(Audience.encode(d.conditions()));
        p.setAction(d.action());
        p.setTitle(d.title().trim());
        p.setBody(d.body().trim());
        p.setScreen(d.screen() == null ? "" : d.screen());
        p.setAmount(GEMS.equals(d.action()) ? d.amount() : 0);
        p.setCooldownDays(d.cooldownDays());
        if (fresh) {
            pipelines.insert(p);
        } else {
            pipelines.save(p);
        }
        record(admin, fresh ? "PIPELINE_NEW" : "PIPELINE_EDIT", p.getName() + ": " + p.getConditions());
        return dto(p);
    }

    @Transactional(rollbackFor = IOException.class)
    public PipelineDto delete(String admin, String id) throws IOException {
        Pipeline p = find(id);
        for (PipelineHit h : hits.findByPipeline(id)) {
            hits.deleteById(h.getId());
        }
        pipelines.deleteById(p.getId());
        record(admin, "PIPELINE_DELETE", p.getName());
        return dto(p);
    }

    private Pipeline find(String id) {
        Pipeline p = id == null ? null : pipelines.findById(id);
        if (p == null) {
            throw new ResponseStatusException(404, "No such pipeline");
        }
        return p;
    }

    private static void check(PipelineDto d) {
        if (d == null || d.name() == null || d.name().trim().length() == 0 || d.name().trim().length() > 40) {
            throw new ResponseStatusException(400, "A name of up to 40 characters");
        }
        Audience.check(d.conditions());
        if (d.conditions().isEmpty()) {
            throw new ResponseStatusException(400, "At least one condition: a pipeline for everyone is a broadcast");
        }
        if (Audience.encode(d.conditions()).length() > 500) {
            throw new ResponseStatusException(400, "The conditions are too long");
        }
        if (!PUSH.equals(d.action()) && !GEMS.equals(d.action())) {
            throw new ResponseStatusException(400, "The action is push or gems");
        }
        if (GEMS.equals(d.action()) && (d.amount() < 1 || d.amount() > MAX_GEMS)) {
            throw new ResponseStatusException(400, "Between 1 and " + MAX_GEMS + " gems");
        }
        int title = d.title() == null ? 0 : d.title().trim().length();
        int body = d.body() == null ? 0 : d.body().trim().length();
        if (title == 0 || title > 60 || body == 0 || body > 200) {
            throw new ResponseStatusException(400, "A title of up to 60 characters and a message of up to 200");
        }
        if (d.cooldownDays() < 1 || d.cooldownDays() > 365) {
            throw new ResponseStatusException(400, "The cool-down is between 1 and 365 days");
        }
        boolean known = false;
        for (String s : Promotion.SCREENS) {
            known |= s.equals(d.screen() == null ? "" : d.screen());
        }
        if (!known) {
            throw new ResponseStatusException(400, "Unknown screen " + d.screen());
        }
    }

    private static PipelineDto dto(Pipeline p) {
        List<ConditionDto> c = Audience.decode(p.getConditions());
        return new PipelineDto(p.getId(), p.getName(), p.isEnabled(), c, p.getAction(), p.getTitle(), p.getBody(),
                p.getScreen(), p.getAmount(), p.getCooldownDays(), p.getReached(), p.getLastRun().toEpochMilli());
    }

    private void record(String admin, String action, String reason) throws IOException {
        AdminAction a = new AdminAction();
        a.setId(Ids.next());
        a.setAdmin(admin);
        a.setUsername("promote");
        a.setAction(action);
        a.setReason(reason.length() > 255 ? reason.substring(0, 255) : reason);
        a.setCreatedAt(Instant.now());
        audit.insert(a);
    }
}
