-- The studio: where classes happen, who teaches them, who comes to them.
--
-- As in every script here: no generated keys, a yes/no is a SMALLINT 0/1, a
-- time is milliseconds since the epoch in a BIGINT and a date (a birthday, the
-- first day of a trip) is the day count since 1970-01-01 in a BIGINT.

-- A site. Opening hours are minutes after midnight in the site's own zone.
CREATE TABLE location (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    address VARCHAR(255) NOT NULL,
    time_zone VARCHAR(64) NOT NULL,
    opens_minute INTEGER NOT NULL,
    closes_minute INTEGER NOT NULL
);

CREATE TABLE room (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    location_id VARCHAR(32) NOT NULL,
    name VARCHAR(80) NOT NULL,
    capacity INTEGER NOT NULL
);

-- An account that works here. What it may do is its roles; this row is what
-- members are shown of a trainer, and when the certificate that lets them
-- teach runs out.
CREATE TABLE staff (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    location_id VARCHAR(32) NOT NULL,
    title VARCHAR(80) NOT NULL,
    bio VARCHAR(1000) NOT NULL,
    certified_until BIGINT NOT NULL,
    active SMALLINT NOT NULL
);

-- A kind of class. Ages are inclusive, in whole years on the day of the
-- class; 0 and 120 mean no bound.
CREATE TABLE class_type (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    category VARCHAR(24) NOT NULL,
    intensity INTEGER NOT NULL,
    min_age INTEGER NOT NULL,
    max_age INTEGER NOT NULL,
    capacity INTEGER NOT NULL,
    minutes INTEGER NOT NULL,
    color VARCHAR(8) NOT NULL,
    active SMALLINT NOT NULL
);

-- One class on the timetable. `booked` counts the places taken and is only
-- ever changed by a conditional update against `capacity`, which is what
-- makes overbooking impossible whatever arrives at once.
CREATE TABLE class_session (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    class_type_id VARCHAR(32) NOT NULL,
    location_id VARCHAR(32) NOT NULL,
    room_id VARCHAR(32) NOT NULL,
    trainer VARCHAR(190) NOT NULL,
    starts_at BIGINT NOT NULL,
    ends_at BIGINT NOT NULL,
    capacity INTEGER NOT NULL,
    booked INTEGER NOT NULL,
    waitlisted INTEGER NOT NULL,
    state VARCHAR(16) NOT NULL,
    note VARCHAR(255) NOT NULL
);
CREATE INDEX class_session_start ON class_session (location_id, starts_at);
CREATE INDEX class_session_trainer ON class_session (trainer, starts_at);

-- A person who trains here. One account can hold several: the adult who signs
-- in is a member themselves (relation SELF), and books for their children
-- (relation CHILD), who have no account of their own. Not called `member`:
-- MySQL reserves the word.
CREATE TABLE studio_member (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    account VARCHAR(190) NOT NULL,
    name VARCHAR(120) NOT NULL,
    birth_date BIGINT NOT NULL,
    relation VARCHAR(8) NOT NULL,
    waiver_at BIGINT NOT NULL,
    medical_note VARCHAR(500) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX studio_member_account ON studio_member (account);

-- A place in a class, or in the queue for one. `queue_position` orders the
-- waitlist; it is 0 for a place.
CREATE TABLE booking (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    session_id VARCHAR(32) NOT NULL,
    member_id VARCHAR(32) NOT NULL,
    account VARCHAR(190) NOT NULL,
    state VARCHAR(16) NOT NULL,
    queue_position BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    checked_in_at BIGINT NOT NULL,
    invited_by VARCHAR(190) NOT NULL
);
CREATE UNIQUE INDEX booking_once ON booking (session_id, member_id);
CREATE INDEX booking_account ON booking (account, created_at);

-- Coming through the door. Every check-in is kept: it is what attendance,
-- streaks and the "we miss you" nudge are worked out from.
CREATE TABLE check_in (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    member_id VARCHAR(32) NOT NULL,
    account VARCHAR(190) NOT NULL,
    location_id VARCHAR(32) NOT NULL,
    method VARCHAR(8) NOT NULL,
    session_id VARCHAR(32) NOT NULL,
    at BIGINT NOT NULL,
    staff VARCHAR(190) NOT NULL
);
CREATE INDEX check_in_member ON check_in (member_id, at);
CREATE INDEX check_in_location ON check_in (location_id, at);

-- The secret a member's pass codes are made from. The app computes a code
-- from it every 30 seconds with no network, so the door works when the
-- phone has no signal; the server keeps the codes it has accepted
-- (used_pass_code) so a screenshot of one gets in once.
CREATE TABLE pass_key (
    member_id VARCHAR(32) NOT NULL PRIMARY KEY,
    secret VARCHAR(64) NOT NULL,
    nfc_id VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX pass_key_nfc ON pass_key (nfc_id);

CREATE TABLE used_pass_code (
    code_hash VARCHAR(64) NOT NULL PRIMARY KEY,
    expires_at BIGINT NOT NULL
);

-- Someone at the desk without a membership: a guest, a day pass, a free
-- trial. A walk-in who joins keeps the row, pointing at their account.
CREATE TABLE walk_in (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    location_id VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(190) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    amount_cents BIGINT NOT NULL,
    paid_by VARCHAR(16) NOT NULL,
    staff VARCHAR(190) NOT NULL,
    at BIGINT NOT NULL,
    converted_account VARCHAR(190) NOT NULL
);
CREATE INDEX walk_in_location ON walk_in (location_id, at);

-- A block of work at a site. A trainer asks for a swap; another takes it.
CREATE TABLE shift (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    location_id VARCHAR(32) NOT NULL,
    staff VARCHAR(190) NOT NULL,
    duty VARCHAR(16) NOT NULL,
    starts_at BIGINT NOT NULL,
    ends_at BIGINT NOT NULL,
    state VARCHAR(16) NOT NULL
);
CREATE INDEX shift_staff ON shift (staff, starts_at);
