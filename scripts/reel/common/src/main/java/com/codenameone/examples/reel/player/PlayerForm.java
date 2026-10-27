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
import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Slider;
import com.codename1.ui.layouts.FlowLayout;
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
import com.codenameone.examples.reel.api.AudioTrackDto;
import com.codenameone.examples.reel.api.SubtitleDto;
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
    /// How long the controls stay up while the video plays untouched.
    private static final int HIDE_AFTER = 4000;

    private final Form previous;
    private final String titleId;
    private final Form form;
    private final Container stage = new Container(new BorderLayout());
    private final Container overlay = new Container(new BorderLayout());
    private final Label status = new Label("", "RlPlayerStatus");
    private final Label heading = new Label("", "RlPlayerTitle");
    private final Label subheading = new Label("", "RlPlayerSubtitle");
    private final SpanLabel caption = new SpanLabel("", "RlCaptionText");
    private final Slider seek = new Slider();
    private final Label elapsed = new Label("0:00", "RlPlayerTime");
    private final Label remaining = new Label("0:00", "RlPlayerTime");
    private final Container chrome = new Container(BoxLayout.y());
    private final Container top = new Container(new BorderLayout());
    private final Container middle = new Container(new FlowLayout(Component.CENTER, Component.CENTER));
    private final Container bottomBar = new Container(new BorderLayout());
    private final Container south = new Container(BoxLayout.y());
    private Button playPause;
    private Container sheet;
    private PlaybackDto session;
    private Media media;
    private Media ad;
    private UITimer ticker;
    private java.util.Timer heart;
    private boolean closed;
    private boolean dragging;
    private long touched = System.currentTimeMillis();
    private final Set<String> shown = new HashSet<String>();
    private final List<AdBreakDto> pendingBreaks = new ArrayList<AdBreakDto>();

    /// What the viewer picked in the Audio and subtitles sheet. Subtitles are
    /// drawn here, so changing them is immediate; audio and quality are the
    /// server's, and changing either reopens the stream at `startAt`.
    private String subtitleLanguage = Prefs.subtitles();
    private Captions captions;
    private final String audioTrack;
    private final String maxQuality;
    private final int startAt;
    private String episodeId;

    private PlayerForm(Form previous, String titleId, String audioTrack, String maxQuality, int startAt) {
        this.previous = previous;
        this.titleId = titleId;
        this.audioTrack = audioTrack;
        this.maxQuality = maxQuality;
        this.startAt = startAt;
        form = new Form("", new LayeredLayout());
        form.setName("Player");
        form.setUIID("RlPlayer");
        form.getContentPane().setUIID("RlPlayer");
        form.getToolbar().hideToolbar();
        form.add(stage);
        overlay.setUIID("Container");
        form.add(overlay);

        top.setUIID("RlPlayerTop");
        top.setSafeArea(true);
        top.add(BorderLayout.WEST, Ui.flat(FontImage.MATERIAL_ARROW_BACK, "close", e -> close(false)));
        Container titles = BoxLayout.encloseY(heading, subheading);
        top.add(BorderLayout.CENTER, titles);
        overlay.add(BorderLayout.NORTH, top);

        middle.setUIID("Container");
        overlay.add(BorderLayout.CENTER, middle);

        elapsed.setName("elapsed");
        status.setName("status");
        seek.setEditable(true);
        seek.setUIID("RlSeek");
        seek.setName("seek");
        seek.setRenderValueOnTop(false);
        seek.addDataChangedListener((type, index) -> dragging = true);
        seek.addActionListener(e -> {
            dragging = false;
            if (media != null) {
                media.setTime(seek.getProgress() * 1000);
            }
            touch();
        });
        Container scrub = new Container(new BorderLayout());
        scrub.setUIID("RlScrub");
        scrub.add(BorderLayout.WEST, elapsed);
        scrub.add(BorderLayout.CENTER, seek);
        scrub.add(BorderLayout.EAST, remaining);
        bottomBar.setUIID("RlPlayerBar");
        chrome.setUIID("RlPlayerChrome");
        // The status (starting, or that the stream cannot play) sits over
        // the scrub bar, the full width of the screen.
        status.getAllStyles().setAlignment(Component.CENTER);
        chrome.add(status);
        chrome.add(scrub);
        chrome.add(bottomBar);
        caption.setUIID("RlCaption");
        if ("small".equals(Prefs.captionSize())) {
            caption.setTextUIID("RlCaptionTextSmall");
        } else if ("large".equals(Prefs.captionSize())) {
            caption.setTextUIID("RlCaptionTextLarge");
        }
        caption.setName("caption");
        caption.setTextBlockAlign(Component.CENTER);
        caption.setHidden(true);
        south.setSafeArea(true);
        south.add(FlowLayout.encloseCenter(caption));
        south.add(chrome);
        overlay.add(BorderLayout.SOUTH, south);
        // A tap anywhere brings the controls back.
        form.addPointerPressedListener(e -> touch());
        form.setBackCommand(new com.codename1.ui.Command("") {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                if (sheet != null) {
                    closeSheet();
                } else {
                    close(false);
                }
            }
        });
    }

    /// The schemes this device can play, strongest first: hardware DRM, then
    /// ClearKey, then AES-128 HLS, which every player has.
    public static List<String> schemes() {
        // tag::reel-schemes[]
        List<String> out = new ArrayList<String>();
        DrmScheme[] order = {DrmScheme.WIDEVINE, DrmScheme.FAIRPLAY, DrmScheme.CLEARKEY, DrmScheme.AES_128};
        for (int iter = 0; iter < order.length; iter++) {
            if (MediaManager.isDrmSupported(order[iter])) {
                out.add(order[iter].name());
            }
        }
        // end::reel-schemes[]
        if (!out.contains(DrmScheme.AES_128.name())) {
            out.add(DrmScheme.AES_128.name());
        }
        return out;
    }

    /// Starts a title, or one episode or lesson of it.
    public static void start(Form from, String titleId, String episodeId) {
        new PlayerForm(from, titleId, null, Prefs.dataSaver() ? "480"
                : Prefs.quality().length() == 0 ? null : Prefs.quality(), -1).begin(episodeId);
    }

    private void begin(String episode) {
        episodeId = episode;
        status.setText(Lang.tr("Starting"));
        form.show();
        PlaybackRequestDto request = new PlaybackRequestDto(titleId, episode, Account.profileId(), Device.id(),
                schemes(), maxQuality, audioTrack, false);
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
            for (int iter = 0; iter < p.adBreaks().size(); iter++) {
                // A reopen after a track change starts past the breaks
                // already behind it; they were watched the first time.
                if (startAt < 0 || p.adBreaks().get(iter).offsetSeconds() > startAt) {
                    pendingBreaks.add(p.adBreaks().get(iter));
                }
            }
        }
        heading.setText(p.titleName() == null ? "" : p.titleName());
        subheading.setText(p.episodeName() == null ? "" : p.episodeName());
        if (p.posterUrl() != null) {
            stage.add(BorderLayout.CENTER, new Poster(p.posterUrl()));
        }
        controls(p);
        if (p.watermark() != null && p.watermark().length() > 0) {
            Label mark = new Label(p.watermark(), "RlWatermark");
            mark.setName("watermark");
            south.add(mark);
        }
        showCaptions(subtitleLanguage);
        form.revalidate();
        // Protected video is kept out of screenshots and screen recordings
        // where the platform allows it.
        Display.getInstance().setProperty("DisableScreenshots", "true");
        Telemetry.played(titleId, p.scheme(), !pendingBreaks.isEmpty());
        AdBreakDto preroll = breakAt(startAt < 0 ? 0 : startAt);
        if (preroll != null) {
            AdPlayer.play(this, preroll, this::startMain);
        } else {
            startMain();
        }
    }

    /// The transport: back and forward ten seconds and play or pause in the
    /// middle; under the picture the scrub bar, then chapters, the tracks
    /// sheet and the next episode.
    private void controls(final PlaybackDto p) {
        middle.removeAll();
        middle.add(styled(Ui.round(FontImage.MATERIAL_REPLAY_10, "back10", e -> skip(-10)), "RlTransport",
                FontImage.MATERIAL_REPLAY_10));
        playPause = Ui.round(FontImage.MATERIAL_PAUSE, "playPause", e -> toggle());
        playPause.setUIID("RlPlayButton");
        Ui.icon(playPause, FontImage.MATERIAL_PAUSE, 7f);
        middle.add(playPause);
        middle.add(styled(Ui.round(FontImage.MATERIAL_FORWARD_10, "forward10", e -> skip(10)), "RlTransport",
                FontImage.MATERIAL_FORWARD_10));
        seek.setMinValue(0);
        seek.setMaxValue(Math.max(1, p.durationSeconds()));
        seek.setProgress(Math.max(0, startAt < 0 ? p.resumeSeconds() : startAt));
        times(seek.getProgress());

        bottomBar.removeAll();
        Container left = new Container(BoxLayout.x());
        if (p.chapters() != null && !p.chapters().isEmpty()) {
            left.add(styled(Ui.action(FontImage.MATERIAL_LIST, Lang.tr("Chapters"), "chapters", e -> chapters()),
                    "RlPlayerAction", FontImage.MATERIAL_LIST));
        }
        left.add(styled(Ui.action(FontImage.MATERIAL_SUBTITLES, Lang.tr("Audio & subtitles"), "tracks",
                e -> tracks()), "RlPlayerAction", FontImage.MATERIAL_SUBTITLES));
        bottomBar.add(BorderLayout.WEST, left);
        if (p.nextEpisodeId() != null) {
            bottomBar.add(BorderLayout.EAST, styled(Ui.action(FontImage.MATERIAL_SKIP_NEXT, Lang.tr("Next episode"),
                    "nextEpisode", e -> jump(p.nextEpisodeId())), "RlPlayerAction",
                    FontImage.MATERIAL_SKIP_NEXT));
        }
    }

    /// The shared buttons, restyled for the dark of the player. The icon is
    /// drawn again so it takes the new style's colour.
    private static Button styled(Button b, String uiid, char icon) {
        b.setUIID(uiid);
        if ("RlPlayerAction".equals(uiid)) {
            b.setTextPosition(Component.RIGHT);
        }
        FontImage.setMaterialIcon(b, icon, "RlPlayerAction".equals(uiid) ? 3.6f : 4.6f);
        return b;
    }

    private void skip(int seconds) {
        touch();
        if (media == null) {
            return;
        }
        int to = Math.max(0, media.getTime() / 1000 + seconds);
        media.setTime(to * 1000);
        seek.setProgress(Math.min(to, seek.getMaxValue()));
        times(seek.getProgress());
    }

    private void toggle() {
        touch();
        if (media == null) {
            return;
        }
        if (media.isPlaying()) {
            media.pause();
        } else {
            media.play();
        }
        playing(media.isPlaying());
    }

    private void playing(boolean on) {
        if (playPause != null) {
            Ui.icon(playPause, on ? FontImage.MATERIAL_PAUSE : FontImage.MATERIAL_PLAY_ARROW, 7f);
        }
    }

    private void times(int position) {
        int length = seek.getMaxValue();
        elapsed.setText(TitleForm.clock(position));
        remaining.setText("-" + TitleForm.clock(Math.max(0, length - position)));
    }

    /// Shows the controls again and restarts the wait before they hide.
    private void touch() {
        touched = System.currentTimeMillis();
        if (top.isHidden()) {
            chromeShown(true);
        }
    }

    private void chromeShown(boolean on) {
        top.setHidden(!on);
        middle.setHidden(!on);
        chrome.setHidden(!on);
        form.revalidate();
    }

    /// Picks the subtitles in `language`, or none for "" or a language the
    /// video has no track in.
    private void showCaptions(String language) {
        subtitleLanguage = language == null ? "" : language;
        captions = null;
        caption.setText("");
        caption.setHidden(true);
        SubtitleDto track = subtitle(subtitleLanguage);
        if (track == null) {
            return;
        }
        final String wanted = subtitleLanguage;
        Captions.load(track.url(), loaded -> {
            if (wanted.equals(subtitleLanguage)) {
                captions = loaded;
                updateCaption();
            }
        });
    }

    private void updateCaption() {
        String text = captions == null ? "" : captions.at(media == null ? seek.getProgress() * 1000
                : media.getTime());
        if (!text.equals(caption.getText())) {
            caption.setText(text);
            caption.setHidden(text.length() == 0);
            south.revalidateLater();
        }
    }

    private SubtitleDto subtitle(String language) {
        if (session == null || session.subtitles() == null || language.length() == 0) {
            return null;
        }
        for (int iter = 0; iter < session.subtitles().size(); iter++) {
            if (language.equals(session.subtitles().get(iter).language())) {
                return session.subtitles().get(iter);
            }
        }
        return null;
    }

    /// The Audio and subtitles sheet, over the bottom of the picture.
    private void tracks() {
        if (sheet != null) {
            closeSheet();
            return;
        }
        Container card = new Container(BoxLayout.y());
        card.setUIID("RlTracks");
        card.setName("tracksSheet");
        Container head = new Container(new BorderLayout());
        head.add(BorderLayout.CENTER, Ui.label(Lang.tr("Audio & subtitles"), "RlTracksTitle"));
        head.add(BorderLayout.EAST, Ui.flat(FontImage.MATERIAL_CLOSE, "tracksClose", e -> closeSheet()));
        card.add(head);

        List<SubtitleDto> subs = session.subtitles() == null ? new ArrayList<SubtitleDto>() : session.subtitles();
        String[] subLabels = new String[subs.size() + 1];
        String[] subValues = new String[subs.size() + 1];
        subLabels[0] = Lang.tr("Off");
        subValues[0] = "";
        for (int iter = 0; iter < subs.size(); iter++) {
            subLabels[iter + 1] = subs.get(iter).label();
            subValues[iter + 1] = subs.get(iter).language();
        }
        card.add(Ui.label(Lang.tr("Subtitles"), "RlTracksSection"));
        card.add(Ui.choice("subtitle", subLabels, subValues, subtitle(subtitleLanguage) == null ? ""
                : subtitleLanguage, this::showCaptions, Math.min(subValues.length, 3)));

        List<AudioTrackDto> audio = session.audioTracks() == null ? new ArrayList<AudioTrackDto>()
                : session.audioTracks();
        if (!audio.isEmpty()) {
            String[] labels = new String[audio.size()];
            String[] values = new String[audio.size()];
            for (int iter = 0; iter < audio.size(); iter++) {
                labels[iter] = audio.get(iter).label();
                values[iter] = audio.get(iter).id();
            }
            card.add(Ui.label(Lang.tr("Audio"), "RlTracksSection"));
            card.add(Ui.choice("audio", labels, values, session.audioTrack(), chosen -> {
                if (!chosen.equals(session.audioTrack())) {
                    reopen(chosen, maxQuality);
                }
            }, Math.min(values.length, 3)));
        }

        List<Integer> heights = session.qualities() == null ? new ArrayList<Integer>() : session.qualities();
        String[] qLabels = new String[heights.size() + 1];
        String[] qValues = new String[heights.size() + 1];
        qLabels[0] = Lang.tr("Auto");
        qValues[0] = "";
        for (int iter = 0; iter < heights.size(); iter++) {
            int h = heights.get(iter).intValue();
            qLabels[iter + 1] = quality(h);
            qValues[iter + 1] = String.valueOf(h);
        }
        card.add(Ui.label(Lang.tr("Quality"), "RlTracksSection"));
        final String currentQuality = maxQuality == null ? "" : maxQuality;
        card.add(Ui.choice("quality", qLabels, qValues, currentQuality, chosen -> {
            if (!chosen.equals(currentQuality)) {
                reopen(session.audioTrack(), chosen.length() == 0 ? null : chosen);
            }
        }, Math.min(qValues.length, 4)));

        sheet = new Container(new BorderLayout());
        Button outside = new Button();
        outside.setUIID("Container");
        outside.setName("tracksOutside");
        outside.addActionListener(e -> closeSheet());
        sheet.add(BorderLayout.CENTER, outside);
        sheet.add(BorderLayout.SOUTH, card);
        form.add(sheet);
        form.revalidate();
    }

    /// What a quality is called: 2160 is 4K, 720 and up are HD.
    public static String quality(int height) {
        if (height >= 2160) {
            return "4K";
        }
        if (height >= 1080) {
            return "1080p HD";
        }
        if (height >= 720) {
            return "720p HD";
        }
        return height + "p";
    }

    private void closeSheet() {
        if (sheet != null) {
            sheet.remove();
            sheet = null;
            form.revalidate();
            touch();
        }
    }

    /// Audio and quality are the server's choice of stream, so changing one
    /// ends this session and opens another at the same position.
    private void reopen(String audio, String quality) {
        int position = media == null ? seek.getProgress() : media.getTime() / 1000;
        String language = subtitleLanguage;
        close(false, false);
        PlayerForm next = new PlayerForm(previous, titleId, audio, quality, position);
        next.subtitleLanguage = language;
        next.begin(episodeId);
    }

    /// The end of the video: the next episode when autoplay is on and there
    /// is one, else back where the viewer came from.
    private void finished() {
        if (media == null) {
            // Some players report a stream that never opened as one that
            // ended; the reason is on screen and the viewer leaves from it.
            return;
        }
        int length = session == null ? 0 : session.durationSeconds();
        if (length > 10 && media.getTime() / 1000 < length - 5) {
            // A stream that "ends" far from its end failed to load: keep the
            // controls up with the reason rather than leaving the screen.
            status.setText(Lang.tr("This stream cannot play here"));
            playing(false);
            return;
        }
        String next = session == null ? null : session.nextEpisodeId();
        if (next != null && Prefs.autoplay()) {
            close(true, false);
            new PlayerForm(previous, titleId, audioTrack, maxQuality, -1).begin(next);
        } else {
            close(true);
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
                .setStartPosition((startAt < 0 ? session.resumeSeconds() : startAt) * 1000)
                .setOnCompletion(() -> CN.callSerially(this::finished));
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
            playing(true);
            touch();
            ticker = UITimer.timer(250, true, form, this::tick);
            startHeartbeat();
        })).except(err -> CN.callSerially(() -> {
            // The controls stay over the poster with the reason, so the
            // viewer can still pick another quality or leave.
            com.codename1.io.Log.e(err);
            status.setText(Lang.tr("This stream cannot play here"));
            playing(false);
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

    /// Four times a second: the scrub bar, the subtitles, hiding the
    /// controls, and the ad breaks and interactions due at this position.
    private void tick() {
        if (media == null || closed || ad != null) {
            return;
        }
        int position = media.getTime() / 1000;
        if (!dragging) {
            int length = media.getDuration() / 1000;
            if (length > 0 && length != seek.getMaxValue()) {
                seek.setMaxValue(length);
            }
            seek.setProgress(Math.min(position, seek.getMaxValue()));
            times(position);
        }
        updateCaption();
        if (sheet == null && !top.isHidden() && media.isPlaying()
                && System.currentTimeMillis() - touched > HIDE_AFTER) {
            chromeShown(false);
        }
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
        new PlayerForm(previous, titleId, audioTrack, maxQuality, -1).begin(episodeId);
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
