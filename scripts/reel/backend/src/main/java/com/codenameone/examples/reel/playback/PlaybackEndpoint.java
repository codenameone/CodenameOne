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
package com.codenameone.examples.reel.playback;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.reel.Caller;
import com.codenameone.examples.reel.ads.Ads;
import com.codenameone.examples.reel.api.AdDto;
import com.codenameone.examples.reel.api.AdEventDto;
import com.codenameone.examples.reel.api.HeartbeatDto;
import com.codenameone.examples.reel.api.HeartbeatReplyDto;
import com.codenameone.examples.reel.api.PlaybackApiServer;
import com.codenameone.examples.reel.api.PlaybackDto;
import com.codenameone.examples.reel.api.PlaybackRequestDto;
import com.codenameone.examples.reel.api.StopDto;
import com.codenameone.examples.reel.account.Devices;

import java.util.List;

@Component
public class PlaybackEndpoint implements PlaybackApiServer {
    private final Playbacks playbacks;
    private final Ads ads;
    private final Devices devices;

    public PlaybackEndpoint(Playbacks playbacks, Ads ads, Devices devices) {
        this.playbacks = playbacks;
        this.ads = ads;
        this.devices = devices;
    }

    @Override
    public PlaybackDto start(PlaybackRequestDto request) throws Exception {
        String who = Caller.name();
        devices.touch(who);
        return playbacks.start(who, request);
    }

    @Override
    public HeartbeatReplyDto heartbeat(HeartbeatDto heartbeat) throws Exception {
        return playbacks.heartbeat(Caller.name(), heartbeat);
    }

    @Override
    public String stop(StopDto stop) throws Exception {
        playbacks.stop(Caller.name(), stop);
        return "ok";
    }

    @Override
    public List<AdDto> ads(String breakId, String session) throws Exception {
        return playbacks.adsFor(Caller.name(), session, breakId);
    }

    @Override
    public String adEvent(AdEventDto event) throws Exception {
        String who = Caller.name();
        // The event must name a session of the caller's, or anybody could spend
        // a campaign's budget by posting impressions.
        playbacks.liveOrRecent(who, event == null ? null : event.sessionId());
        ads.record(who, event);
        return "ok";
    }
}
