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
package com.codenameone.examples.bazaar.channel;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.ChannelDto;
import com.codenameone.examples.bazaar.api.ChannelListingDto;
import com.codenameone.examples.bazaar.api.ChannelSetupDto;
import com.codenameone.examples.bazaar.api.ChannelsApiServer;
import com.codenameone.examples.bazaar.api.SyncLogDto;
import java.util.List;

/// The REST face of [Channels]: each call names its caller and hands over to
/// the service, which checks access.
@Component
public class ChannelEndpoint implements ChannelsApiServer {
    private final Channels channels;

    public ChannelEndpoint(Channels channels) {
        this.channels = channels;
    }

    @Override
    public List<ChannelDto> channels(String s) throws Exception {
        return channels.list(Caller.name(), s);
    }

    @Override
    public List<ChannelDto> connectChannel(String s, ChannelSetupDto b) throws Exception {
        return channels.connect(Caller.name(), s, b);
    }

    @Override
    public List<ChannelDto> removeChannel(String s, String id) throws Exception {
        return channels.remove(Caller.name(), s, id);
    }

    @Override
    public List<SyncLogDto> syncChannel(String s, String id) throws Exception {
        return channels.syncNow(Caller.name(), s, id);
    }

    @Override
    public List<ChannelListingDto> channelListings(String s, String id) throws Exception {
        return channels.listings(Caller.name(), s, id);
    }

    @Override
    public List<ChannelListingDto> listOnChannel(String s, String id, ChannelListingDto b)
            throws Exception {
        return channels.listVariant(Caller.name(), s, id, b);
    }

    @Override
    public List<SyncLogDto> channelLog(String s, String id) throws Exception {
        return channels.log(Caller.name(), s, id);
    }
}
