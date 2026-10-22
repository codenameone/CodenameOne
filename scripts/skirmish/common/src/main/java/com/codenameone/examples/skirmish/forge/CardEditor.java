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
package com.codenameone.examples.skirmish.forge;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.util.Base64;
import com.codenameone.examples.skirmish.api.AssetUploadDto;
import com.codenameone.examples.skirmish.battle.ForgeFiles;
import com.codenameone.examples.skirmish.battle.ModelPreview;
import com.codenameone.examples.skirmish.battle.Models;
import com.codenameone.examples.skirmish.net.Api;
import com.codenameone.examples.skirmish.net.Net;
import com.codenameone.examples.skirmish.rules.CardDef;
import com.codenameone.examples.skirmish.rules.CardSet;
import com.codenameone.examples.skirmish.rules.TargetKind;
import com.codenameone.examples.skirmish.rules.forge.AssetRef;
import com.codenameone.examples.skirmish.rules.forge.GlbInspector;
import com.codenameone.examples.skirmish.rules.forge.PackCodec;
import com.codenameone.examples.skirmish.rules.forge.Validator;
import com.codenameone.examples.skirmish.ui.Ui;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The character forge: a card's stats and behaviour, and how it looks -- a
/// character that ships with the game, or a glTF model of the author's own,
/// with its animations matched to walking, attacking and dying.
///
/// The card is edited as the map [PackCodec] writes it and read back after
/// every change, so the editor can never hold a card the codec would not.
public final class CardEditor {
    /// The characters that ship with the game.
    static final String[] BUNDLED = {"knight", "barbarian", "mage", "rogue", "rogue_hooded",
        "skeleton_warrior", "skeleton_minion", "skeleton_rogue", "crypt"};
    private static final String[] KINDS = {"Troop", "Building", "Spell", "Tower"};
    private static final int[] KIND_VALUES = {CardDef.TROOP, CardDef.BUILDING, CardDef.SPELL, CardDef.TOWER};
    private static final String[] TARGETS = {"Ground", "Ground and air", "Buildings only"};
    private static final int[] TARGET_VALUES = {TargetKind.GROUND, TargetKind.GROUND_AND_AIR, TargetKind.BUILDINGS};
    private static final String[] PARTS = {"idle", "run", "attack", "death"};
    private static final String[] PART_TITLES = {"Standing", "Walking", "Attacking", "Dying"};

    private final int index;
    private final Map<String, Object> card;

    private CardEditor(int index) {
        this.index = index;
        this.card = PackCodec.card(PackEditor.pack.cards.get(index));
    }

    static void create() {
        String id = "my-card";
        CardSet set = PackEditor.pack.cardSet();
        for (int n = 2; set.get(id) != null; n++) {
            id = "my-card-" + n;
        }
        CardDef c = new CardDef(id, "My card", CardDef.TROOP, 3).hp(800).damage(120).hitTicks(24).range(800)
                .speed(45).radius(500).model("knight");
        PackEditor.pack.cards.add(c);
        new CardEditor(PackEditor.pack.cards.size() - 1).show();
    }

    static void edit(int index) {
        new CardEditor(index).show();
    }

    private int num(String key) {
        Object v = card.get(key);
        return v instanceof Long ? ((Long) v).intValue() : 0;
    }

    private String str(String key) {
        Object v = card.get(key);
        return v instanceof String ? (String) v : null;
    }

    private void put(String key, int value) {
        card.put(key, Long.valueOf(value));
        commit();
    }

    private void put(String key, Object value) {
        card.put(key, value);
        commit();
    }

    /// Writes the edited card back into the pack.
    private void commit() {
        try {
            PackEditor.pack.cards.set(index, PackCodec.readCard(new PackCodec.Reader(card, "card")));
        } catch (IllegalArgumentException e) {
            Ui.toast(e.getMessage());
        }
    }

    private void show() {
        final Form f = Ui.form("Card", "cardEditor");
        Ui.back(f, PackEditor::show);
        String model = str("model");
        int tint = num("tint") < 0 ? 0xffffffff : 0xff000000 | num("tint");
        if (model != null && (Models.isBundled(model) || model.startsWith("asset:")
                && ForgeFiles.has(model.substring(6)))) {
            final ModelPreview preview = new ModelPreview(model, tint, clipOr("idle", "Idle"));
            preview.setName("cardPreview");
            f.add(Ui.centered(preview));
            f.add(Ui.row(Ui.stone("Walk", "previewWalk", e -> preview.play(clipOr("run", "Running_A"))),
                    Ui.stone("Attack", "previewAttack", e -> preview.play(clipOr("attack", "1H_Melee_Attack_Chop"))),
                    Ui.stone("Idle", "previewIdle", e -> preview.play(clipOr("idle", "Idle")))));
            f.addShowListener(e -> preview.start());
        }
        f.add(Fields.text("Name", "cardName", str("name"), 40, v -> put("name", (Object) v)));
        f.add(Fields.area("Description", "cardDescription", str("description"), Validator.MAX_TEXT,
                v -> put("description", (Object) v)));
        final int kind = num("kind");
        f.add(Fields.choice("Kind", "cardKind", KINDS, kind, v -> {
            put("kind", KIND_VALUES[v]);
            show();
        }));
        f.add(Fields.stepper("Elixir", "cardCost", num("cost"), kind == CardDef.TOWER ? 0 : 1, 10, 1, "",
                v -> put("cost", v)));
        if (kind != CardDef.SPELL) {
            f.add(Fields.stepper("Hit points", "cardHp", num("hp"), 50, 8000, 50, "", v -> put("hp", v)));
            f.add(Fields.tenths("Size", "cardRadius", num("radius") / 100, 2, 30, 1, " tiles",
                    v -> put("radius", v * 100)));
        }
        f.add(Fields.stepper("Damage", "cardDamage", num("damage"), 0, 2000, 10, "", v -> put("damage", v)));
        if (kind != CardDef.SPELL) {
            f.add(Fields.stepper("Attack every", "cardHitTicks", num("hitTicks"), 5, 200, 1, " ticks",
                    v -> put("hitTicks", v)));
            f.add(Fields.help("A battle runs 20 ticks a second."));
            f.add(Fields.tenths("Range", "cardRange", num("range") / 100, 0, 120, 1, " tiles",
                    v -> put("range", v * 100)));
            f.add(Fields.choice("Attacks", "cardTargets", TARGETS, targetIndex(num("targets")),
                    v -> put("targets", TARGET_VALUES[v])));
        }
        if (kind == CardDef.TROOP) {
            f.add(Fields.stepper("Speed", "cardSpeed", num("speed"), 0, 150, 5, "", v -> put("speed", v)));
            f.add(Fields.stepper("How many", "cardCount", Math.max(1, num("count")), 1, 12, 1, "",
                    v -> put("count", v)));
            f.add(Fields.check("Flies", "cardFlying", Boolean.TRUE.equals(card.get("flying")),
                    v -> put("flying", (Object) Boolean.valueOf(v))));
        }
        f.add(Fields.tenths("Splash", "cardSplash", num("splash") / 100, 0, 60, 5, " tiles",
                v -> put("splash", v * 100)));
        if (kind == CardDef.BUILDING) {
            f.add(Fields.stepper("Lasts", "cardLifetime", num("lifetimeTicks") / 20, 0, 300, 5, " s",
                    v -> put("lifetimeTicks", v * 20)));
        }
        if (kind == CardDef.TROOP || kind == CardDef.BUILDING) {
            looks(f, model);
        }
        f.add(Ui.row(Ui.stone("Done", "cardDone", e -> PackEditor.show()),
                Ui.stone("Delete card", "cardDelete", e -> {
                    PackEditor.pack.cards.remove(index);
                    PackEditor.show();
                })));
        f.show();
    }

    private void looks(final Form f, String model) {
        f.add(Ui.label("Looks", "SectionTitle"));
        boolean uploaded = model != null && model.startsWith("asset:");
        String[] options = new String[BUNDLED.length + (uploaded ? 1 : 0)];
        System.arraycopy(BUNDLED, 0, options, 0, BUNDLED.length);
        if (uploaded) {
            options[BUNDLED.length] = "Your model";
        }
        f.add(Fields.choice("Character", "cardModel", options,
                uploaded ? BUNDLED.length : Fields.indexOf(BUNDLED, model), v -> {
                    if (v < BUNDLED.length) {
                        put("model", (Object) BUNDLED[v]);
                        put("clips", (Object) null);
                        show();
                    }
                }));
        f.add(Ui.stone("Import a glTF model (.glb)", "cardImport", e -> importModel()));
        f.add(Fields.help("A rigged, animated .glb up to " + Validator.MAX_MODEL_BYTES / (1024 * 1024)
                + " MB. A moderator looks at every uploaded model before others see it in a listed pack."));
        f.add(Fields.stepper("Scale", "cardScale", Math.max(25, num("scalePercent")), 25, 400, 5, "%",
                v -> put("scalePercent", v)));
        f.add(Fields.choice("Tint", "cardTint", new String[] {"None", "Red", "Green", "Blue", "Gold", "Shadow"},
                tintIndex(num("tint")), v -> {
                    put("tint", new int[] {-1, 0xff8080, 0x80ff80, 0x8090ff, 0xffd060, 0x606070}[v]);
                    show();
                }));
        if (uploaded) {
            AssetRef a = PackEditor.pack.asset(model.substring(6));
            List<String> clips = a == null ? new ArrayList<String>() : animations(model.substring(6));
            if (!clips.isEmpty()) {
                final String[] names = clips.toArray(new String[clips.size()]);
                for (int i = 0; i < PARTS.length; i++) {
                    final String part = PARTS[i];
                    String now = GlbInspector.clip(str("clips"), part);
                    f.add(Fields.choice(PART_TITLES[i], "clip-" + part, names, Fields.indexOf(names, now),
                            v -> setClip(part, names[v])));
                }
            }
        }
    }

    private String clipOr(String part, String fallback) {
        String c = GlbInspector.clip(str("clips"), part);
        return c == null ? fallback : c;
    }

    private void setClip(String part, String clip) {
        StringBuilder sb = new StringBuilder();
        for (String p : PARTS) {
            String v = p.equals(part) ? clip : GlbInspector.clip(str("clips"), p);
            if (v != null) {
                if (sb.length() > 0) {
                    sb.append(';');
                }
                sb.append(p).append('=').append(v);
            }
        }
        put("clips", (Object) sb.toString());
    }

    private static int targetIndex(int targets) {
        for (int i = 0; i < TARGET_VALUES.length; i++) {
            if (TARGET_VALUES[i] == targets) {
                return i;
            }
        }
        return 0;
    }

    private static int tintIndex(int tint) {
        int[] t = {-1, 0xff8080, 0x80ff80, 0x8090ff, 0xffd060, 0x606070};
        for (int i = 0; i < t.length; i++) {
            if (t[i] == tint) {
                return i;
            }
        }
        return 0;
    }

    /// The clips of a downloaded model, read from the file itself.
    private static List<String> animations(String sha) {
        byte[] b = ForgeFiles.bytes(sha);
        return b == null ? new ArrayList<String>() : GlbInspector.inspect(b).animations;
    }

    /// Picks a .glb, checks it here with the same inspector the server uses,
    /// uploads it, and makes it this card's model with its clips matched by
    /// name where the names make it obvious.
    private void importModel() {
        CN.openFileChooser(e -> {
            if (e == null || !(e.getSource() instanceof String)) {
                return;
            }
            byte[] bytes;
            try (InputStream in = FileSystemStorage.getInstance().openInputStream((String) e.getSource())) {
                bytes = Util.readInputStream(in);
            } catch (java.io.IOException err) {
                Ui.failed(0, "That file could not be read");
                return;
            }
            final GlbInspector.Report report = GlbInspector.inspect(bytes);
            if (!report.ok()) {
                Ui.failed(0, "That model cannot be used: " + report.problem);
                return;
            }
            final byte[] data = bytes;
            String path = (String) e.getSource();
            final String name = path.substring(Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\')) + 1);
            Api.forge().upload(new AssetUploadDto(name, AssetRef.MODEL, Base64.encodeNoNewline(data)),
                    Net.to(asset -> Ui.later(() -> {
                        ForgeFiles.put(asset.sha256(), data);
                        if (PackEditor.pack.asset(asset.sha256()) == null) {
                            AssetRef r = new AssetRef();
                            r.sha256 = asset.sha256();
                            r.kind = AssetRef.MODEL;
                            r.name = name.length() > 60 ? name.substring(0, 60) : name;
                            r.bytes = data.length;
                            PackEditor.pack.assets.add(r);
                        }
                        card.put("model", "asset:" + asset.sha256());
                        card.put("clips", guessClips(report.animations));
                        commit();
                        show();
                    }), Ui::failed));
        }, ".glb");
    }

    /// "Idle", "Walk", "Attack_1", "Die" -- most rigs name their clips close
    /// enough to match without asking.
    static String guessClips(List<String> clips) {
        String[][] hints = {{"idle", "stand"}, {"run", "walk"}, {"attack", "hit", "shoot", "punch", "slash"},
            {"death", "die", "dead"}};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < PARTS.length; i++) {
            String found = null;
            for (int c = 0; c < clips.size() && found == null; c++) {
                String lower = asciiLower(clips.get(c));
                for (String h : hints[i]) {
                    if (lower.indexOf(h) >= 0) {
                        found = clips.get(c);
                        break;
                    }
                }
            }
            if (found == null && !clips.isEmpty()) {
                found = clips.get(0);
            }
            if (found != null) {
                if (sb.length() > 0) {
                    sb.append(';');
                }
                sb.append(PARTS[i]).append('=').append(found);
            }
        }
        return sb.toString();
    }

    /// Clip names are compared without the device's language: toLowerCase()
    /// would fold a Turkish dotted I differently.
    private static String asciiLower(String s) {
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] >= 'A' && c[i] <= 'Z') {
                c[i] = (char) (c[i] + 32);
            }
        }
        return new String(c);
    }
}
