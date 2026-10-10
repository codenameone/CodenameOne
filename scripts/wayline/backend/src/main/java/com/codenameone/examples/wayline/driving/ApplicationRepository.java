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
package com.codenameone.examples.wayline.driving;

import com.codename1.backend.annotations.Component;
import com.codename1.orm.session.Identifier;
import com.codename1.orm.session.Session;
import com.codenameone.examples.wayline.domain.DriverApplication;
import com.codenameone.examples.wayline.domain.DriverDocument;

import java.util.List;

/// Where applications to drive and their documents are read and written.
///
/// A document's row carries the picture, as Base64 text that can run to a
/// couple of megabytes. Only [#document(String, String)] reads it. Everything
/// that wants to know which documents there are and how they stand selects
/// those fields alone, so listing two hundred applications does not pull two
/// hundred sets of photographs through the server.
@Component
public class ApplicationRepository {
    private final Session session;

    public ApplicationRepository(Session session) {
        this.session = session;
    }

    // ---------------------------------------------------------- applications

    /// An account's application, or null when it has none.
    public DriverApplication find(String username) {
        if (username == null || username.length() == 0 || username.length() > 190) {
            return null;
        }
        return session.find(DriverApplication.class, username);
    }

    public void add(DriverApplication application) {
        session.persist(application);
    }

    /// The name and status of every application, as `{username, status}`.
    public List<Object[]> statuses() {
        return session.createQuery("select a.username, a.status from DriverApplication a",
                Object[].class).list();
    }

    /// How many applications are in a status.
    public long count(String status) {
        return session.query(DriverApplication.class).eq("status", status).count();
    }

    /// Applications of every status, the latest handed in first. Only those of
    /// accounts that still have a profile, as the list names the applicant.
    public List<DriverApplication> newest(int limit) {
        return session.createQuery("select a from DriverApplication a where a.username in "
                + "(select p.username from Profile p) "
                + "order by a.submittedAt desc, a.username", DriverApplication.class)
                .limit(limit).list();
    }

    /// Applications in one status, the longest waiting first.
    public List<DriverApplication> inStatus(String status, int limit) {
        return session.createQuery("select a from DriverApplication a where a.status = :status "
                + "and a.username in (select p.username from Profile p) "
                + "order by a.submittedAt, a.username", DriverApplication.class)
                .setParameter("status", status).limit(limit).list();
    }

    /// The display names of some accounts, as `{username, displayName}`. The
    /// two fields and not the profiles: a list of applications shows nothing
    /// else of them.
    public List<Object[]> displayNames(List<String> usernames) {
        return session.createQuery("select p.username, p.displayName from Profile p "
                + "where p.username in :names", Object[].class)
                .setParameter("names", usernames).list();
    }

    /// Moves an application from one status to another, and answers how many
    /// rows that changed. One statement, so that of two callers moving the
    /// same application out of the same status the count is 1 for one of them
    /// and 0 for the other. It empties the session: an application read before
    /// it has to be read again.
    public int move(String username, String from, String to, String reason, long at, String by) {
        return session.createQuery("update DriverApplication a set a.status = :to, "
                + "a.rejectionReason = :reason, a.reviewedAt = :at, a.reviewedBy = :by "
                + "where a.username = :username and a.status = :from")
                .setParameter("to", to).setParameter("reason", reason)
                .setParameter("at", Long.valueOf(at)).setParameter("by", by)
                .setParameter("username", username).setParameter("from", from).executeUpdate();
    }

    /// Marks an application approved unless it already is, and answers how
    /// many rows that changed. Empties the session, as [#move] does.
    public int approveUnlessApproved(String username, String approved, long at, String by) {
        return session.createQuery("update DriverApplication a set a.status = :to, "
                + "a.rejectionReason = '', a.reviewedAt = :at, a.reviewedBy = :by "
                + "where a.username = :username and a.status <> :to")
                .setParameter("to", approved).setParameter("at", Long.valueOf(at))
                .setParameter("by", by).setParameter("username", username).executeUpdate();
    }

    /// Hands an application in: from the status it was read in to `to`, with
    /// the last rejection's reason cleared. Answers how many rows that changed
    /// and empties the session, as [#move] does.
    public int submit(String username, String from, String to, long at) {
        return session.createQuery("update DriverApplication a set a.status = :to, "
                + "a.submittedAt = :at, a.rejectionReason = '' "
                + "where a.username = :username and a.status = :from")
                .setParameter("to", to).setParameter("at", Long.valueOf(at))
                .setParameter("username", username).setParameter("from", from).executeUpdate();
    }

    public int delete(String username) {
        return session.createQuery("delete from DriverApplication a where a.username = :username")
                .setParameter("username", username).executeUpdate();
    }

    // ------------------------------------------------------------- documents

    /// One stored document, picture included, or null.
    public DriverDocument document(String username, String kind) {
        return session.find(DriverDocument.class, Identifier.of(username, kind));
    }

    public void add(DriverDocument document) {
        session.persist(document);
    }

    /// Which documents an account has uploaded, as `{kind, status,
    /// uploadedAt}`, without the pictures.
    public List<Object[]> documentSummaries(String username) {
        return session.createQuery("select d.kind, d.status, d.uploadedAt from DriverDocument d "
                + "where d.username = :username", Object[].class)
                .setParameter("username", username).list();
    }

    /// The same for several accounts at once, as `{username, kind, status,
    /// uploadedAt}`.
    public List<Object[]> documentSummaries(List<String> usernames) {
        return session.createQuery("select d.username, d.kind, d.status, d.uploadedAt "
                + "from DriverDocument d where d.username in :names", Object[].class)
                .setParameter("names", usernames).list();
    }

    /// Puts a new picture in place of a stored document's, and answers how
    /// many rows that changed: 0 says there was none of that kind to replace.
    /// A statement, so that the picture being replaced is never read. Empties
    /// the session, as [#move] does.
    public int replace(String username, String kind, String contentType, String status, long at,
            String data) {
        return session.createQuery("update DriverDocument d set d.contentType = :type, "
                + "d.status = :status, d.uploadedAt = :at, d.data = :data "
                + "where d.username = :username and d.kind = :kind")
                .setParameter("type", contentType).setParameter("status", status)
                .setParameter("at", Long.valueOf(at)).setParameter("data", data)
                .setParameter("username", username).setParameter("kind", kind).executeUpdate();
    }

    /// Sets the status of all of an account's documents.
    public int markDocuments(String username, String status) {
        return session.createQuery("update DriverDocument d set d.status = :status "
                + "where d.username = :username")
                .setParameter("status", status).setParameter("username", username)
                .executeUpdate();
    }

    public int deleteDocuments(String username) {
        return session.createQuery("delete from DriverDocument d where d.username = :username")
                .setParameter("username", username).executeUpdate();
    }
}
