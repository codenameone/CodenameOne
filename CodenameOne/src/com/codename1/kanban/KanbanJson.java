/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.kanban;

import com.codename1.io.JSONParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Saves a board as JSON and reads it back.
///
/// ```java
/// String json = KanbanJson.toJson(board.getModel());
/// Storage.getInstance().writeObject("board", json);
/// // later
/// board.setModel(KanbanJson.fromJson(json));
/// ```
///
/// The document holds the lanes, and the columns in order, each with its
/// cards in order. A card's user data is the application's own object and is
/// left out unless a `UserDataCodec` says how to write it.
public final class KanbanJson {
    /// Turns the user data of a card into something JSON can hold, and back.
    public interface UserDataCodec {
        /// What to write for a card's user data: a string, a number, a
        /// boolean, a list or a map of those, or null to write nothing.
        Object encode(Object userData);

        /// The user data for what `encode` wrote, as the parser read it.
        Object decode(Object json);
    }

    private KanbanJson() {
    }

    /// The whole board as a JSON document.
    public static String toJson(KanbanModel model) {
        return toJson(model, null);
    }

    /// The whole board as a JSON document, with the cards' user data written
    /// by a codec.
    public static String toJson(KanbanModel model, UserDataCodec codec) {
        return JSONParser.toJson(toMap(model, codec));
    }

    /// The board as the tree a JSON writer takes: maps, lists, strings,
    /// numbers and booleans.
    public static Map<String, Object> toMap(KanbanModel model, UserDataCodec codec) {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        List<KanbanLane> lanes = model.getLanes();
        List<Object> laneList = new ArrayList<Object>();
        for (KanbanLane l : lanes) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", l.getId());
            m.put("title", l.getTitle());
            if (l.isCollapsed()) {
                m.put("collapsed", Boolean.TRUE);
            }
            laneList.add(m);
        }
        if (!laneList.isEmpty()) {
            root.put("lanes", laneList);
        }
        List<Object> columnList = new ArrayList<Object>();
        List<KanbanColumn> columns = model.getColumns();
        for (KanbanColumn c : columns) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", c.getId());
            m.put("title", c.getTitle());
            if (c.getWipLimit() > 0) {
                m.put("limit", Integer.valueOf(c.getWipLimit()));
            }
            if (c.isCollapsed()) {
                m.put("collapsed", Boolean.TRUE);
            }
            m.put("style", c.getStyleKey());
            List<Object> cardList = new ArrayList<Object>();
            for (int l = 0; l < Math.max(1, lanes.size()); l++) {
                KanbanLane lane = lanes.isEmpty() ? null : lanes.get(l);
                List<KanbanCard> cards = model.getCards(c, lane);
                for (KanbanCard kc : cards) {
                    // the card's own lane id, not the lane it is shown in: a
                    // card naming no lane is shown in the first and still names none
                    cardList.add(cardToMap(kc, kc.getLaneId(), codec));
                }
            }
            m.put("cards", cardList);
            columnList.add(m);
        }
        root.put("columns", columnList);
        return root;
    }

    /// Cards alone, as a board puts them on the clipboard.
    public static Map<String, Object> cardsToMap(Collection<KanbanCard> cards, UserDataCodec codec) {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        List<Object> list = new ArrayList<Object>();
        for (KanbanCard c : cards) {
            list.add(cardToMap(c, c.getLaneId(), codec));
        }
        root.put("cards", list);
        return root;
    }

    private static Map<String, Object> cardToMap(KanbanCard c, String laneId, UserDataCodec codec) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", c.getId());
        m.put("title", c.getTitle());
        m.put("description", c.getDescription());
        if (!c.getTags().isEmpty()) {
            m.put("tags", new ArrayList<Object>(c.getTags()));
        }
        m.put("badge", c.getBadge());
        m.put("style", c.getStyleKey());
        m.put("lane", laneId);
        if (codec != null && c.getUserData() != null) {
            m.put("data", codec.encode(c.getUserData()));
        }
        return m;
    }

    /// Reads a board `toJson` wrote.
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the text is not a board document, or a column,
    ///   a lane or a card in it has no id
    public static DefaultKanbanModel fromJson(String json) throws IOException {
        DefaultKanbanModel model = new DefaultKanbanModel();
        read(json, model, null);
        return model;
    }

    /// Reads a board `toJson` wrote into a model, adding to what it holds.
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the text is not a board document, or a column,
    ///   a lane or a card in it has no id or one that is taken; the model is
    ///   then as it was
    public static void read(String json, DefaultKanbanModel into, UserDataCodec codec) throws IOException {
        Map<String, Object> root = parse(json);
        if (!root.containsKey("columns")) {
            throw new IOException("Not a board document");
        }
        // everything is read and checked before the model is touched
        List<KanbanLane> newLanes = new ArrayList<KanbanLane>();
        List<KanbanColumn> newColumns = new ArrayList<KanbanColumn>();
        List<List<KanbanCard>> newCards = new ArrayList<List<KanbanCard>>();
        Map<String, Object> seen = new HashMap<String, Object>();
        List<Object> lanes = list(root.get("lanes"));
        for (Object raw : lanes) {
            Map<String, Object> m = map(raw);
            KanbanLane lane = new KanbanLane(required(m, "id"), string(m, "title"))
                    .setCollapsed(bool(m.get("collapsed")));
            if (into.getLane(lane.getId()) != null || seen.put("l" + lane.getId(), lane) != null) {
                throw new IOException("Board document: duplicate lane " + lane.getId());
            }
            newLanes.add(lane);
        }
        List<Object> columns = list(root.get("columns"));
        for (Object raw : columns) {
            Map<String, Object> m = map(raw);
            KanbanColumn c = new KanbanColumn(required(m, "id"), string(m, "title"));
            if (into.getColumn(c.getId()) != null || seen.put("c" + c.getId(), c) != null) {
                throw new IOException("Board document: duplicate column " + c.getId());
            }
            Object limit = m.get("limit");
            if (limit instanceof Number) {
                c.setWipLimit(((Number) limit).intValue());
            }
            c.setCollapsed(bool(m.get("collapsed")));
            c.setStyleKey(string(m, "style"));
            List<KanbanCard> cards = readCards(m, codec);
            for (KanbanCard kc : cards) {
                if (into.getCard(kc.getId()) != null || seen.put("k" + kc.getId(), kc) != null) {
                    throw new IOException("Board document: duplicate card " + kc.getId());
                }
            }
            newColumns.add(c);
            newCards.add(cards);
        }
        for (KanbanLane lane : newLanes) {
            into.addLane(lane);
        }
        for (int i = 0; i < newColumns.size(); i++) {
            KanbanColumn c = into.addColumn(newColumns.get(i));
            for (KanbanCard kc : newCards.get(i)) {
                into.addCard(c.getId(), kc);
            }
        }
    }

    /// Parses the text of a document `toJson` or a board's clipboard wrote.
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the text is not a well-formed JSON object
    public static Map<String, Object> parse(String json) throws IOException {
        // the strict parser: what it answers does not depend on how the
        // application configured the lenient one
        Object parsed = JSONParser.parseStrict(json);
        if (!(parsed instanceof Map)) {
            throw new IOException("Not a board document");
        }
        return map(parsed);
    }

    /// Reads the cards of a map `cardsToMap` built, or of one column of a
    /// board document, as a JSON parser returns it.
    ///
    /// #### Throws
    ///
    /// - `IOException`: when a card has no id
    public static List<KanbanCard> readCards(Map<String, Object> holder, UserDataCodec codec) throws IOException {
        List<KanbanCard> out = new ArrayList<KanbanCard>();
        List<Object> cards = list(holder.get("cards"));
        for (Object raw : cards) {
            Map<String, Object> m = map(raw);
            KanbanCard c = new KanbanCard(required(m, "id"), string(m, "title"));
            c.setDescription(string(m, "description"));
            List<Object> tags = list(m.get("tags"));
            for (Object tag : tags) {
                if (tag instanceof String) {
                    c.addTag((String) tag);
                }
            }
            c.setBadge(string(m, "badge"));
            c.setStyleKey(string(m, "style"));
            c.setLaneId(string(m, "lane"));
            if (codec != null && m.get("data") != null) {
                c.setUserData(codec.decode(m.get("data")));
            }
            out.add(c);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) throws IOException {
        if (o instanceof Map) {
            return (Map<String, Object>) o;
        }
        throw new IOException("Not a board document");
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object o) {
        if (o instanceof List) {
            return (List<Object>) o;
        }
        return new ArrayList<Object>();
    }

    private static String string(Map<String, Object> m, String key) {
        Object o = m.get(key);
        return o instanceof String ? (String) o : null;
    }

    private static String required(Map<String, Object> m, String key) throws IOException {
        String s = string(m, key);
        if (s == null) {
            throw new IOException("Board document: missing " + key);
        }
        return s;
    }

    /// The parser hands back a Boolean or the word, depending on how the
    /// application configured it.
    private static boolean bool(Object o) {
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue();
        }
        return "true".equals(o);
    }
}
