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
package com.codenameone.examples.brickyard.play;

import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Configuration;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.brickyard.Ids;
import com.codenameone.examples.brickyard.repository.PlayServerRepository;
import java.io.IOException;

/// This instance's place among the game servers:
///
/// - `brickyard.play.instance-id`: a name unique among the instances sharing
///   the database; unset, a random one per start.
/// - `brickyard.play.socket-url`: the game socket players reach this instance
///   on, such as `wss://play-2.example.com/ws/play`. Unset, the app uses the
///   API host it is talking to, which is right for a single instance.
@Configuration
public class PlayConfig {
    @Bean
    public PlayServers playServers(PlayServerRepository rows, PlayEvents events,
            @Value("${brickyard.play.instance-id:}") String instanceId,
            @Value("${brickyard.play.socket-url:}") String socketUrl) throws IOException {
        String id = instanceId.trim().length() > 0 ? instanceId.trim() : "i-" + Ids.next();
        return new PlayServers(rows, events, id, socketUrl.trim());
    }
}
