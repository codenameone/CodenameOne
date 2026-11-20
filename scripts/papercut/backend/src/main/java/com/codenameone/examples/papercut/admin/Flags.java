/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.admin;

import com.codename1.appkit.server.Text;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.orm.repository.PageRequest;
import com.codenameone.examples.papercut.Ids;
import com.codenameone.examples.papercut.api.FlagDto;
import com.codenameone.examples.papercut.domain.Flag;
import com.codenameone.examples.papercut.domain.Profile;
import com.codenameone.examples.papercut.domain.ScoreRun;
import com.codenameone.examples.papercut.repository.FlagRepository;
import com.codenameone.examples.papercut.repository.ProfileRepository;
import com.codenameone.examples.papercut.repository.ScoreRunRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Runs held for a person's look. A flagged run stays on the boards until an
/// admin decides: a remarkable run is usually a good player, and taking it
/// down on arithmetic alone would punish exactly them.
@Component
public class Flags {
    public static final String KEEP = "KEEP";
    public static final String REMOVE = "REMOVE";

    private final FlagRepository flags;
    private final ProfileRepository profiles;
    private final ScoreRunRepository runs;

    public Flags(FlagRepository flags, ProfileRepository profiles, ScoreRunRepository runs) {
        this.flags = flags;
        this.profiles = profiles;
        this.runs = runs;
    }

    /// Files a flag for `run`, inside the caller's transaction.
    public Flag file(Profile p, ScoreRun run, String reason) throws IOException {
        Flag f = new Flag();
        f.setId(Ids.next());
        f.setUsername(p.getUsername());
        f.setRunId(run.getId());
        f.setMode(run.getMode());
        f.setScore(run.getScore());
        f.setKills(run.getKills());
        f.setSeconds(run.getSeconds());
        f.setReason(reason);
        f.setCreatedAt(run.getCreatedAt());
        f.setPending(true);
        flags.insert(f);
        return f;
    }

    @Transactional(readOnly = true)
    public List<FlagDto> list(boolean open) {
        List<FlagDto> out = new ArrayList<FlagDto>();
        for (Flag f : flags.inState(open, new PageRequest(0, 200))) {
            out.add(toDto(f));
        }
        return out;
    }

    /// Closes a flag. REMOVE strikes the run; the caller recomputes the
    /// player's bests, which needs [com.codenameone.examples.papercut.run.Runs].
    @Transactional(rollbackFor = IOException.class)
    public Flag resolve(String admin, String id, String verdict) throws IOException {
        String v = Text.oneOf(verdict, new String[] {KEEP, REMOVE}, "", "the verdict");
        Flag f = flags.findById(id);
        if (f == null) {
            throw new ResponseStatusException(404, "No such flag");
        }
        if (!f.isPending()) {
            throw new ResponseStatusException(409, "That flag is already closed");
        }
        f.setPending(false);
        f.setResolvedBy(admin);
        flags.save(f);
        if (REMOVE.equalsIgnoreCase(v)) {
            ScoreRun r = runs.findById(f.getRunId());
            if (r != null && !r.isStruck()) {
                r.setStruck(true);
                runs.save(r);
            }
        }
        // The player stays marked while anything about them is still open.
        Profile p = profiles.findById(f.getUsername());
        if (p != null && flags.countByUsernameAndPendingTrue(p.getUsername()) == 0) {
            p.setFlagged(false);
            profiles.save(p);
        }
        return f;
    }

    public long open() {
        return flags.countByPendingTrue();
    }

    public FlagDto toDto(Flag f) {
        Profile p = profiles.findById(f.getUsername());
        return new FlagDto(f.getId(), f.getUsername(), p == null ? f.getUsername() : p.getDisplayName(),
                f.getMode(), f.getScore(), f.getKills(), f.getSeconds(), f.getReason(),
                f.getCreatedAt().toEpochMilli(), f.isPending());
    }
}
