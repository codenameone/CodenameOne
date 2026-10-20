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
package com.codenameone.examples.parley.call;

import com.codename1.backend.Base64;
import com.codename1.backend.Crypto;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.parley.Ids;
import com.codenameone.examples.parley.api.CallDto;
import com.codenameone.examples.parley.api.CallSignalDto;
import com.codenameone.examples.parley.api.CallStartDto;
import com.codenameone.examples.parley.api.IceConfigDto;
import com.codenameone.examples.parley.api.IceServerDto;
import com.codenameone.examples.parley.chat.Conversations;
import com.codenameone.examples.parley.domain.CallRecord;
import com.codenameone.examples.parley.domain.CallSetup;
import com.codenameone.examples.parley.domain.CallLine;
import com.codenameone.examples.parley.domain.Conversation;
import com.codenameone.examples.parley.domain.Profile;
import com.codenameone.examples.parley.live.LiveHub;
import com.codenameone.examples.parley.push.MessagePushes;
import com.codenameone.examples.parley.repository.CallRecordRepository;
import com.codenameone.examples.parley.repository.CallSetupRepository;
import com.codenameone.examples.parley.repository.CallLineRepository;
import com.codenameone.examples.parley.repository.ContactBlockRepository;
import com.codenameone.examples.parley.repository.DeviceRepository;
import com.codenameone.examples.parley.repository.ProfileRepository;
import com.codenameone.examples.parley.chat.Messages;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// One-to-one voice and video calls: who is calling whom, and the relay
/// their apps negotiate the call over.
///
/// The server never touches the media. The two apps exchange a WebRTC offer,
/// an answer and their ICE candidates through [#signal], which passes each
/// one on as a `{"type":"call"}` live frame, and then talk to each other
/// directly, or through the TURN server when no direct path exists. The
/// media is encrypted by WebRTC itself (DTLS-SRTP), between the two devices.
///
/// A call goes RINGING, then ACCEPTED, then ENDED; or from RINGING to
/// DECLINED, to MISSED when the caller gives up or nobody answers in
/// [#RING_MILLIS], or straight to BUSY when the callee is already in a call.
@Component
public class Calls {
    public static final String RINGING = "RINGING";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String ENDED = "ENDED";
    public static final String MISSED = "MISSED";
    public static final String DECLINED = "DECLINED";
    public static final String BUSY = "BUSY";

    /// The signals the apps send. OFFER, ANSWER and ICE carry WebRTC's own
    /// text; ACCEPT, DECLINE and END move the call along.
    static final String OFFER = "OFFER";
    static final String ANSWER = "ANSWER";
    static final String ICE = "ICE";
    static final String ACCEPT = "ACCEPT";
    static final String DECLINE = "DECLINE";
    static final String END = "END";
    static final int MAX_PAYLOAD = 16384;
    /// How much of a ringing call's candidates is kept for a late callee.
    static final int MAX_SETUP = 16384;

    /// How long a call rings before it counts as missed.
    static final long RING_MILLIS = 45000L;
    /// How long a call may run with neither side heard from, before the
    /// server takes it as ended: an app that crashed mid-call sends no END.
    static final long STALE_MILLIS = 6 * 3600000L;

    private final CallRecordRepository calls;
    private final CallLineRepository lines;
    private final CallSetupRepository setups;
    private final ProfileRepository profiles;
    private final ContactBlockRepository blocks;
    private final Conversations conversations;
    private final LiveHub live;
    private final DeviceRepository devices;
    private final MessagePushes pushes;
    private final String stun;
    private final String turn;
    private final String turnSecret;
    private final int turnTtl;

    public Calls(CallRecordRepository calls, CallLineRepository lines,
            CallSetupRepository setups, ProfileRepository profiles,
            ContactBlockRepository blocks, Conversations conversations, LiveHub live,
            MessagePushes pushes, DeviceRepository devices,
            @Value("${parley.ice.stun:stun:stun.l.google.com:19302}") String stun,
            @Value("${parley.ice.turn:}") String turn,
            @Value("${parley.ice.secret:}") String turnSecret,
            @Value("${parley.ice.ttl:86400}") int turnTtl) {
        this.calls = calls;
        this.lines = lines;
        this.setups = setups;
        this.profiles = profiles;
        this.blocks = blocks;
        this.conversations = conversations;
        this.live = live;
        this.pushes = pushes;
        this.devices = devices;
        this.stun = stun;
        this.turn = turn;
        this.turnSecret = turnSecret;
        this.turnTtl = turnTtl;
    }

    /// Records a call and claims both lines for it. Nobody is rung yet: that
    /// is [#ring(CallDto)], once this transaction has committed. The line
    /// inserts are what decide a race between two callers, and they are only
    /// written at the commit -- so the loser finds out after this returns, and
    /// the endpoint records its call again with `busy` set.
    @Transactional(rollbackFor = IOException.class)
    public CallDto start(String username, CallStartDto request, boolean busy) throws IOException {
        if (request == null) {
            throw new ResponseStatusException(400, "Nothing to call");
        }
        conversations.member(request.conversationId(), username);
        Conversation chat = conversations.conversation(request.conversationId());
        if (!Conversations.DIRECT.equals(chat.getKind())) {
            throw new ResponseStatusException(400, "Calls are between two people");
        }
        String callee = null;
        for (String member : conversations.usernames(chat.getId())) {
            if (!member.equals(username)) {
                callee = member;
            }
        }
        if (callee == null || Conversations.BOT.equals(callee)) {
            throw new ResponseStatusException(400, "There is nobody to call here");
        }
        if (!calls.liveOf(username).isEmpty()) {
            throw new ResponseStatusException(409, "You are already in a call");
        }
        long now = System.currentTimeMillis();
        CallRecord row = new CallRecord();
        row.setId(Ids.next());
        row.setConversationId(chat.getId());
        row.setCaller(username);
        row.setCallerDevice("");
        row.setCallee(callee);
        row.setCalleeDevice("");
        row.setVideo(request.video());
        row.setStartedAt(now);
        if (busy || !calls.liveOf(callee).isEmpty() || lines.findById(callee) != null) {
            row.setState(BUSY);
            row.setEndedAt(now);
        } else {
            row.setState(RINGING);
        }
        calls.save(row);
        if (RINGING.equals(row.getState())) {
            // The check above reads; these inserts are what decide. Of two
            // callers ringing one callee at once, the second insert fails on
            // the callee's key and that call never rings.
            lines.insert(new CallLine(username, row.getId()));
            lines.insert(new CallLine(callee, row.getId()));
            CallSetup setup = new CallSetup();
            setup.setId(row.getId());
            setups.insert(setup);
        }
        return toDto(row);
    }

    /// Rings the callee of a call [#start] committed. A callee who blocked the
    /// caller is never rung, and the caller is not told why: to them it looks
    /// like a call nobody answered.
    @Transactional(readOnly = true)
    public void ring(CallDto call) throws IOException {
        CallRecord row = calls.findById(call.id());
        if (row == null || !RINGING.equals(row.getState())
                || blocks.findById(Messages.blockId(row.getCallee(), row.getCaller())) != null) {
            return;
        }
        live.toUsers(Arrays.asList(row.getCallee()), frame(row, "ring"));
        pushes.ring(row.getCallee(), row.getId(), uuid(row.getId()), name(row.getCaller()),
                row.getCaller(), row.isVideo());
    }

    /// Rings a device that has just connected for every call still ringing
    /// for its account. A phone woken by a push has no socket when the ring
    /// frame goes out, and the push itself says only which call: this is
    /// where it learns who is calling and that the call is still live.
    @Transactional(readOnly = true)
    public void ringDevice(String username, String device) throws IOException {
        for (CallRecord row : calls.liveOf(username)) {
            if (username.equals(row.getCallee()) && RINGING.equals(row.getState())
                    && blocks.findById(Messages.blockId(row.getCallee(), row.getCaller())) == null) {
                live.toDevice(device, frame(row, "ring"));
            }
        }
    }

    /// Passes a signal to the other side of the call, and moves the call on
    /// for those that do that.
    @Transactional(rollbackFor = IOException.class)
    public CallDto signal(String username, String id, CallSignalDto signal) throws IOException {
        CallRecord row = calls.findById(id);
        if (row == null || !(username.equals(row.getCaller()) || username.equals(row.getCallee()))) {
            throw new ResponseStatusException(404, "No such call");
        }
        if (signal == null || signal.type() == null) {
            throw new ResponseStatusException(400, "Say what the signal is");
        }
        if (signal.payload() != null && signal.payload().length() > MAX_PAYLOAD) {
            throw new ResponseStatusException(413, "That signal is too long");
        }
        if (row.getEndedAt() != 0) {
            // A late candidate for a call that is over is not an error the
            // app can do anything about.
            return toDto(row);
        }
        boolean caller = username.equals(row.getCaller());
        String device = signal.fromDevice() == null ? "" : signal.fromDevice();
        // The device becomes where the other side's signals are routed, and
        // device ids are visible to everyone in a conversation: one that is
        // not the sender's own would steer the call to a stranger's phone.
        if (device.length() > 0 && devices.findByIdAndUsername(device, username) == null) {
            throw new ResponseStatusException(400, "That is not one of your devices");
        }
        String type = signal.type();
        long now = System.currentTimeMillis();
        if (OFFER.equals(type) && caller) {
            row.setCallerDevice(device);
            remember(row, signal.payload(), true);
        } else if (ICE.equals(type) && caller && RINGING.equals(row.getState())) {
            remember(row, signal.payload(), false);
        } else if (ACCEPT.equals(type) && !caller && RINGING.equals(row.getState())) {
            row.setState(ACCEPTED);
            row.setAnsweredAt(now);
            row.setCalleeDevice(device);
            // The callee's other devices are still ringing.
            pushes.cancelRing(row.getCallee(), uuid(row.getId()));
            live.toUsers(Arrays.asList(row.getCallee()), frame(row, "answered"));
            replay(row);
        } else if (DECLINE.equals(type) && !caller && RINGING.equals(row.getState())) {
            end(row, DECLINED, now);
        } else if (END.equals(type)) {
            end(row, RINGING.equals(row.getState()) ? MISSED : ENDED, now);
        } else if (!OFFER.equals(type) && !ANSWER.equals(type) && !ICE.equals(type)
                && !ACCEPT.equals(type)) {
            throw new ResponseStatusException(400, "There is no such signal");
        }
        Map<String, Object> frame = frame(row, "signal");
        frame.put("signal", type);
        frame.put("payload", signal.payload() == null ? "" : signal.payload());
        frame.put("fromDevice", device);
        String other = caller ? row.getCallee() : row.getCaller();
        String otherDevice = caller ? row.getCalleeDevice() : row.getCallerDevice();
        // Once a device has answered, the call is between two devices; before
        // that the offer goes to every device the callee has.
        if (otherDevice.length() == 0 || !live.toDevice(otherDevice, frame)) {
            live.toUsers(Arrays.asList(other), frame);
        }
        return toDto(row);
    }

    @Transactional(readOnly = true)
    public List<CallDto> history(String username) {
        List<CallDto> out = new ArrayList<>();
        for (CallRecord row : calls.historyOf(username)) {
            out.add(toDto(row));
            if (out.size() == 100) {
                break;
            }
        }
        return out;
    }

    /// The STUN and TURN servers the apps should use.
    ///
    /// The TURN credentials are the time-limited kind coturn's
    /// `use-auth-secret` checks: the user name is the expiry time and the
    /// account, and the password the HMAC-SHA1 of the user name under a
    /// secret this server shares with the TURN server. Nothing is stored, and
    /// a leaked credential stops working on its own.
    public IceConfigDto ice(String username) {
        List<IceServerDto> servers = new ArrayList<>();
        if (stun.length() > 0) {
            servers.add(new IceServerDto(split(stun), "", ""));
        }
        if (turn.length() > 0 && turnSecret.length() > 0) {
            String user = (System.currentTimeMillis() / 1000L + turnTtl) + ":" + username;
            String password = Base64.encode(Crypto.hmac(Crypto.SHA1,
                    turnSecret.getBytes(StandardCharsets.UTF_8),
                    user.getBytes(StandardCharsets.UTF_8)));
            servers.add(new IceServerDto(split(turn), user, password));
        }
        return new IceConfigDto(servers, turnTtl);
    }

    /// Ends the calls nobody ended: ringing too long, or running far longer
    /// than any call does.
    @Scheduled(fixedDelay = 15000, initialDelay = 15000, lock = "parley-calls")
    @Transactional(rollbackFor = IOException.class)
    public void reap() throws IOException {
        long now = System.currentTimeMillis();
        for (CallRecord row : calls.openSince(now - RING_MILLIS)) {
            if (RINGING.equals(row.getState())) {
                end(row, MISSED, now);
                live.toUsers(Arrays.asList(row.getCaller(), row.getCallee()), frame(row, "signal"));
            } else if (row.getAnsweredAt() < now - STALE_MILLIS) {
                end(row, ENDED, now);
                // Both apps still hold the call; without this they keep it
                // open and turn every later call away as busy.
                live.toUsers(Arrays.asList(row.getCaller(), row.getCallee()), frame(row, "signal"));
            }
        }
    }

    /// Keeps the caller's offer, or one of its candidates, for a callee that
    /// is not connected yet.
    private void remember(CallRecord row, String payload, boolean offer) {
        CallSetup setup = setups.findById(row.getId());
        if (setup == null || payload == null) {
            return;
        }
        if (offer) {
            setup.setOffer(payload);
        } else if (setup.getCandidates().length() + payload.length() < MAX_SETUP) {
            setup.setCandidates(setup.getCandidates() + payload + "\n");
        }
        setups.save(setup);
    }

    /// Sends the device that accepted what the caller sent while it rang. The
    /// app takes an offer it already has once only, so a callee that was
    /// connected all along and saw these live loses nothing.
    private void replay(CallRecord row) {
        CallSetup setup = setups.findById(row.getId());
        if (setup == null || setup.getOffer().length() == 0) {
            return;
        }
        live.toDevice(row.getCalleeDevice(), replayed(row, OFFER, setup.getOffer()));
        // One candidate a line. Walked by hand: the compiled backend has no
        // String.split, which is a regular expression.
        String all = setup.getCandidates();
        int start = 0;
        while (start < all.length()) {
            int end = all.indexOf('\n', start);
            if (end < 0) {
                end = all.length();
            }
            if (end > start) {
                live.toDevice(row.getCalleeDevice(), replayed(row, ICE, all.substring(start, end)));
            }
            start = end + 1;
        }
        setups.deleteById(row.getId());
    }

    private Map<String, Object> replayed(CallRecord row, String type, String payload) {
        Map<String, Object> frame = frame(row, "signal");
        frame.put("signal", type);
        frame.put("payload", payload);
        frame.put("fromDevice", row.getCallerDevice());
        return frame;
    }

    private void end(CallRecord row, String state, long now) {
        boolean ringing = RINGING.equals(row.getState());
        row.setState(state);
        row.setEndedAt(now);
        lines.deleteByCallId(row.getId());
        if (setups.findById(row.getId()) != null) {
            setups.deleteById(row.getId());
        }
        if (ringing) {
            pushes.cancelRing(row.getCallee(), uuid(row.getId()));
        }
    }

    private Map<String, Object> frame(CallRecord row, String event) {
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("type", "call");
        frame.put("event", event);
        frame.put("id", row.getId());
        frame.put("conversationId", row.getConversationId());
        frame.put("caller", row.getCaller());
        frame.put("callerName", name(row.getCaller()));
        frame.put("callee", row.getCallee());
        frame.put("video", row.isVideo());
        frame.put("state", row.getState());
        frame.put("uuid", uuid(row.getId()));
        return frame;
    }

    private String name(String username) {
        Profile profile = profiles.findById(username);
        return profile == null ? username : profile.getDisplayName();
    }

    /// The UUID CallKit and ConnectionService know the call by: derived from
    /// the call's id, so every device and every push names the same call.
    static String uuid(String id) {
        byte[] digest = Crypto.sha256(id.getBytes(StandardCharsets.UTF_8));
        String digits = "0123456789abcdef";
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < 16; iter++) {
            if (iter == 4 || iter == 6 || iter == 8 || iter == 10) {
                out.append('-');
            }
            int b = digest[iter] & 255;
            if (iter == 6) {
                b = 0x40 | (b & 15);
            } else if (iter == 8) {
                b = 0xa0 | (b & 15);
            }
            out.append(digits.charAt(b >> 4)).append(digits.charAt(b & 15));
        }
        return out.toString();
    }

    private static List<String> split(String urls) {
        List<String> out = new ArrayList<>();
        int start = 0;
        while (start <= urls.length()) {
            int comma = urls.indexOf(',', start);
            int end = comma < 0 ? urls.length() : comma;
            String url = urls.substring(start, end).trim();
            if (url.length() > 0) {
                out.add(url);
            }
            start = end + 1;
        }
        return out;
    }

    static CallDto toDto(CallRecord row) {
        return new CallDto(row.getId(), row.getConversationId(), row.getCaller(),
                row.getCallee(), row.isVideo(), row.getState(), row.getStartedAt(),
                row.getAnsweredAt(), row.getEndedAt(), uuid(row.getId()));
    }
}
