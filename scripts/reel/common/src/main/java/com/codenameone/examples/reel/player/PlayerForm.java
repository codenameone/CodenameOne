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
package com.codenameone.examples.reel.player;

import com.codename1.media.DrmConfig;
import com.codename1.media.DrmScheme;
import com.codename1.media.Media;
import com.codename1.media.MediaManager;
import com.codename1.media.StreamRequest;
import com.codename1.io.FileSystemStorage;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.reel.Prefs;
import com.codenameone.examples.reel.Telemetry;
import com.codenameone.examples.reel.api.AdBreakDto;
import com.codenameone.examples.reel.api.ChapterDto;
import com.codenameone.examples.reel.api.DownloadDto;
import com.codenameone.examples.reel.api.HeartbeatDto;
import com.codenameone.examples.reel.api.InteractionDto;
import com.codenameone.examples.reel.api.PlaybackDto;
import com.codenameone.examples.reel.api.PlaybackRequestDto;
import com.codenameone.examples.reel.api.StopDto;
import com.codenameone.examples.reel.net.Account;
import com.codenameone.examples.reel.net.Api;
import com.codenameone.examples.reel.net.Device;
import com.codenameone.examples.reel.net.Net;
import com.codenameone.examples.reel.ui.Art;
import com.codenameone.examples.reel.ui.Lang;
import com.codenameone.examples.reel.ui.Ui;
import com.codenameone.examples.reel.viewer.Downloads;
import com.codenameone.examples.reel.viewer.TitleForm;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Playback: one session on the server, one platform player here.
///
/// The app tells the server which DRM schemes this device plays, strongest
/// first, and the server picks one the title was packaged for and the plan
/// allows ([com.codename1.media.StreamRequest] does the rest). While it plays
/// the app reports a heartbeat; the server answers `ok=false` when the session
/// was ended elsewhere (an admin, a suspension, a device sign-out, too many
/// streams), and the player closes. Ad breaks, quizzes, polls and branches
/// are laid out by the server on the timeline and handled here as the
/// position crosses them.
public final class PlayerForm {
    private final Form previous;
    private final String titleId;
    private final Form form;
    private final Container stage = new Container(new BorderLayout());
    private final Container overlay = new Container(new BorderLayout());
    private final Label status = new Label("", "RlPlayerStatus");
    private PlaybackDto session;
    private Media media;
    private Media ad;
    private UITimer ticker;
    private java.util.Timer heart;
    private boolean closed;
    private final Set<String> shown = new HashSet<String>();
    private final List<AdBreakDto> pendingBreaks = new ArrayList<AdBreakDto>();

    private PlayerForm(Form previous, String titleId) {
        this.previous = previous;
        this.titleId = titleId;
        form = new Form("", new LayeredLayout());
        form.setName("Player");
        form.setUIID("RlPlayer");
        form.getContentPane().setUIID("RlPlayer");
        form.getToolbar().hideToolbar();
        form.add(stage);
        overlay.setUIID("Container");
        form.add(overlay);
        Container top = new Container(new BorderLayout());
        top.setSafeArea(true);
        top.add(BorderLayout.WEST, Ui.flat(FontImage.MATERIAL_ARROW_BACK, "close", e -> close(false)));
        top.add(BorderLayout.CENTER, status);
        overlay.add(BorderLayout.NORTH, top);
        form.setBackCommand(new com.codename1.ui.Command("") {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                close(false);
            }
        });
    }

    /// The schemes this device can play, strongest first: hardware DRM, then
    /// ClearKey, then AES-128 HLS, which every player has.
    public static List<String> schemes() {
        List<String> out = new ArrayList<String>();
        DrmScheme[] order = {DrmScheme.WIDEVINE, DrmScheme.FAIRPLAY, DrmScheme.CLEARKEY, DrmScheme.AES_128};
        for (int iter = 0; iter < order.length; iter++) {
            if (MediaManager.isDrmSupported(order[iter])) {
                out.add(order[iter].name());
            }
        }
        if (!out.contains(DrmScheme.AES_128.name())) {
            out.add(DrmScheme.AES_128.name());
        }
        return out;
    }

    /// Starts a title, or one episode or lesson of it.
    public static void start(Form from, String titleId, String episodeId) {
        new PlayerForm(from, titleId).begin(episodeId);
    }

    private void begin(String episodeId) {
        status.setText(Lang.tr("Starting"));
        form.show();
        PlaybackRequestDto request = new PlaybackRequestDto(titleId, episodeId, Account.profileId(), Device.id(),
                schemes(), Prefs.dataSaver() ? "480" : null, false);
        Api.playback().start(request, Net.to(this::play, (code, message) -> {
            Ui.fail(code, message);
            previous.showBack();
        }));
    }

    private void play(PlaybackDto p) {
        if (closed) {
            // The viewer left while the session was being opened: end it
            // rather than leave it holding one of the plan's streams.
            Api.playback().stop(new StopDto(p.sessionId(), 0, false), Net.to(ok -> { }, (code, message) -> { }));
            return;
        }
        session = p;
        shown.clear();
        pendingBreaks.clear();
        if (p.adBreaks() != null) {
            pendingBreaks.addAll(p.adBreaks());
        }
        if (p.watermark() != null && p.watermark().length() > 0) {
            Label mark = new Label(p.watermark(), "RlWatermark");
            mark.setName("watermark");
            overlay.add(BorderLayout.SOUTH, mark);
        }
        if (p.chapters() != null && !p.chapters().isEmpty()) {
            Container right = new Container(BoxLayout.y());
            right.add(Ui.flat(FontImage.MATERIAL_LIST, "chapters", e -> chapters()));
            overlay.add(BorderLayout.EAST, right);
        }
        // Protected video is kept out of screenshots and screen recordings
        // where the platform allows it.
        Display.getInstance().setProperty("DisableScreenshots", "true");
        Telemetry.played(titleId, p.scheme(), !pendingBreaks.isEmpty());
        AdBreakDto preroll = breakAt(0);
        if (preroll != null) {
            AdPlayer.play(this, preroll, this::startMain);
        } else {
            startMain();
        }
    }

    private AdBreakDto breakAt(int position) {
        for (int iter = 0; iter < pendingBreaks.size(); iter++) {
            AdBreakDto b = pendingBreaks.get(iter);
            if (b.offsetSeconds() <= position) {
                pendingBreaks.remove(iter);
                return b;
            }
        }
        return null;
    }

    private void startMain() {
        if (closed) {
            return;
        }
        StreamRequest req = new StreamRequest(Art.url(session.manifestUrl()))
                .setStartPosition(session.resumeSeconds() * 1000)
                .setOnCompletion(() -> CN.callSerially(() -> close(true)));
        DrmConfig drm = drm(session.scheme(), session.licenseUrl(), session.certificateUrl(), session.licenseToken(),
                session.contentId());
        if (drm != null) {
            req.setDrm(drm);
        }
        open(req);
    }

    static DrmConfig drm(String scheme, String licenseUrl, String certificateUrl, String token, String contentId) {
        if (scheme == null || "AES_128".equals(scheme)) {
            // The key is fetched by the player itself, from the same tokened
            // path as the playlists.
            return null;
        }
        DrmConfig drm = new DrmConfig(DrmScheme.valueOf(scheme), Art.url(licenseUrl));
        if (certificateUrl != null && certificateUrl.length() > 0) {
            drm.setCertificateUrl(Art.url(certificateUrl));
        }
        if (contentId != null) {
            drm.setContentId(contentId);
        }
        if (token != null) {
            drm.addLicenseHeader("X-Reel-License", token);
        }
        return drm;
    }

    private void open(StreamRequest req) {
        req.createAsync().ready(m -> CN.callSerially(() -> {
            if (closed) {
                m.cleanup();
                return;
            }
            media = m;
            stage.removeAll();
            stage.add(BorderLayout.CENTER, m.getVideoComponent());
            status.setText("");
            form.revalidate();
            m.play();
            ticker = UITimer.timer(500, true, form, this::tick);
            startHeartbeat();
        })).except(err -> CN.callSerially(() -> {
            Ui.say(Lang.tr("This video cannot play here: {0}", String.valueOf(err.getMessage())));
            close(false);
        }));
    }

    /// The heartbeat runs on its own timer, not the form's: a UITimer stops
    /// while another form is showing, and the chapters list and the quiz and
    /// poll dialogs are other forms. A session that goes quiet that long is
    /// ended by the server as abandoned.
    private void startHeartbeat() {
        long every = Math.max(5, session.heartbeatSeconds()) * 1000L;
        heart = new java.util.Timer();
        heart.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                CN.callSerially(() -> {
                    if (closed || media == null) {
                        return;
                    }
                    // During an ad, and while an overlay waits for an answer,
                    // the main video is paused; the beat says so.
                    beat(media.getTime() / 1000, ad != null || !media.isPlaying());
                });
            }
        }, every, every);
    }

    /// Twice a second: ad breaks and interactions due at this position.
    private void tick() {
        if (media == null || closed || ad != null) {
            return;
        }
        int position = media.getTime() / 1000;
        AdBreakDto due = breakAt(position);
        if (due != null) {
            media.pause();
            AdPlayer.play(this, due, () -> {
                if (media != null) {
                    media.play();
                }
            });
            return;
        }
        if (session.interactions() != null) {
            for (int iter = 0; iter < session.interactions().size(); iter++) {
                InteractionDto i = session.interactions().get(iter);
                if (position >= i.atSeconds() && !shown.contains(i.id())) {
                    shown.add(i.id());
                    media.pause();
                    Interactions.show(this, i);
                    return;
                }
            }
        }
    }

    private void beat(int position, boolean paused) {
        Api.playback().heartbeat(new HeartbeatDto(session.sessionId(), position, paused, 0, 0), Net.to(reply -> {
            if (!reply.ok()) {
                Ui.say(reply.reason());
                close(false);
            }
        }, (code, message) -> {
            if (code == 404 || code == 409 || code == 403) {
                Ui.say(message);
                close(false);
            }
        }));
    }

    private void chapters() {
        final Form list = Ui.form("Chapters", "Chapters");
        Ui.back(list, form);
        Container page = Ui.page();
        for (int iter = 0; iter < session.chapters().size(); iter++) {
            final ChapterDto c = session.chapters().get(iter);
            page.add(Ui.tap(Ui.row(FontImage.MATERIAL_BOOKMARK_BORDER, c.title(), TitleForm.clock(c.startSeconds()),
                    null), "chapter-" + iter, e -> {
                        form.showBack();
                        if (media != null) {
                            media.setTime(c.startSeconds() * 1000);
                        }
                    }));
        }
        list.add(BorderLayout.CENTER, page);
        list.show();
    }

    // --------------------------------------------------- for the overlays

    Form form() {
        return form;
    }

    String sessionId() {
        return session.sessionId();
    }

    Container stage() {
        return stage;
    }

    void setAd(Media playing) {
        ad = playing;
    }

    Media currentAd() {
        return ad;
    }

    Media main() {
        return media;
    }

    boolean closed() {
        return closed;
    }

    /// The overlay's answer led elsewhere (a branch): this session ends and
    /// one for that episode starts.
    void jump(String episodeId) {
        close(false, false);
        new PlayerForm(previous, titleId).begin(episodeId);
    }

    void resume() {
        if (media != null && !closed) {
            media.play();
        }
    }

    private void close(boolean completed) {
        close(completed, true);
    }

    private void close(boolean completed, boolean back) {
        if (closed) {
            return;
        }
        closed = true;
        if (ticker != null) {
            ticker.cancel();
        }
        if (heart != null) {
            heart.cancel();
        }
        int position = media == null ? 0 : media.getTime() / 1000;
        if (media != null) {
            media.cleanup();
        }
        if (ad != null) {
            ad.cleanup();
        }
        Display.getInstance().setProperty("DisableScreenshots", "false");
        if (session != null && session.sessionId() != null) {
            Api.playback().stop(new StopDto(session.sessionId(), position, completed),
                    Net.to(ok -> { }, (code, message) -> { }));
            if (completed) {
                Telemetry.finished(titleId);
            }
        }
        if (back) {
            previous.showBack();
        }
    }

    // ---------------------------------------------------------- offline

    /// Plays a download from this device's storage. The license is still the
    /// server's: ClearKey asks for the key with the download's token.
    public static void offline(final Form from, DownloadDto d) {
        String file = Downloads.path(d.id());
        if (!FileSystemStorage.getInstance().exists(file)) {
            Ui.say("The download is not on this device");
            return;
        }
        if (!MediaManager.isDrmSupported(DrmScheme.CLEARKEY)) {
            Ui.say("This device cannot play protected downloads");
            return;
        }
        StreamRequest req = new StreamRequest(file)
                .setDrm(drm("CLEARKEY", "/license/clearkey", null, Downloads.token(d.id()), null));
        final Form form = Ui.form(d.titleName(), "OfflinePlayer");
        // The same protections as streaming: no screenshots while it shows,
        // and the account's mark over the picture.
        final Media[] playing = new Media[1];
        final boolean[] gone = new boolean[1];
        final Runnable leave = () -> {
            if (gone[0]) {
                return;
            }
            gone[0] = true;
            if (playing[0] != null) {
                playing[0].cleanup();
            }
            Display.getInstance().setProperty("DisableScreenshots", "false");
            from.showBack();
        };
        Ui.back(form, leave);
        com.codenameone.examples.reel.api.AccountDto who = com.codenameone.examples.reel.net.Session.user();
        Label mark = new Label(who == null || who.username() == null ? "" : who.username(), "RlWatermark");
        mark.setName("watermark");
        form.add(BorderLayout.SOUTH, mark);
        Display.getInstance().setProperty("DisableScreenshots", "true");
        form.show();
        req.setOnCompletion(() -> CN.callSerially(leave));
        req.createAsync().ready(m -> CN.callSerially(() -> {
            if (gone[0]) {
                // Left before the player was ready.
                m.cleanup();
                return;
            }
            playing[0] = m;
            form.add(BorderLayout.CENTER, m.getVideoComponent());
            form.revalidate();
            m.play();
        })).except(err -> CN.callSerially(() -> {
            Ui.say(String.valueOf(err.getMessage()));
            leave.run();
        }));
    }
}
