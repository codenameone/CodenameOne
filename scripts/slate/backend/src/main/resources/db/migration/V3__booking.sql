-- The bookings: who can be booked, for what, when, and what has been booked.
--
-- As in every script here: no generated keys, a yes/no is a SMALLINT 0/1 and
-- a time is milliseconds since the epoch in a BIGINT.

-- The business and its booking rules. One row, id 'main'. Minutes are whole
-- minutes, and every one of them is a multiple of five: the slot grid is five
-- minutes wide (see slot_claim).
CREATE TABLE business (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    vertical VARCHAR(16) NOT NULL,
    about VARCHAR(1000) NOT NULL,
    time_zone VARCHAR(64) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    step_minutes INTEGER NOT NULL,
    min_notice_minutes INTEGER NOT NULL,
    horizon_days INTEGER NOT NULL,
    hold_minutes INTEGER NOT NULL,
    cancel_hours INTEGER NOT NULL,
    reschedule_hours INTEGER NOT NULL,
    deposit_percent INTEGER NOT NULL,
    no_show_fee_percent INTEGER NOT NULL,
    client_gap_minutes INTEGER NOT NULL,
    client_per_day INTEGER NOT NULL,
    min_usable_gap_minutes INTEGER NOT NULL,
    packing VARCHAR(16) NOT NULL,
    reminder_hours INTEGER NOT NULL,
    updated_at BIGINT NOT NULL
);

-- A place where appointments happen. An online appointment has a location too:
-- the one whose hours it is booked in.
CREATE TABLE location (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    address VARCHAR(255) NOT NULL,
    time_zone VARCHAR(64) NOT NULL,
    active SMALLINT NOT NULL
);

-- From one location to another, in minutes. Read both ways: a row for A to B
-- also answers B to A when there is no row for that.
CREATE TABLE travel_time (
    id VARCHAR(80) NOT NULL PRIMARY KEY,
    from_location VARCHAR(32) NOT NULL,
    to_location VARCHAR(32) NOT NULL,
    minutes INTEGER NOT NULL
);

-- A person who is booked. The account name is the key; the PROVIDER role is
-- what lets them in to their own schedule.
CREATE TABLE provider (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    display_name VARCHAR(120) NOT NULL,
    title VARCHAR(80) NOT NULL,
    bio VARCHAR(1000) NOT NULL,
    color VARCHAR(8) NOT NULL,
    default_location VARCHAR(32) NOT NULL,
    max_per_day INTEGER NOT NULL,
    max_continuous_minutes INTEGER NOT NULL,
    break_minutes INTEGER NOT NULL,
    feed_token VARCHAR(32) NOT NULL,
    active SMALLINT NOT NULL,
    created_at BIGINT NOT NULL
);

-- Something that can be booked. Values of -1 in the policy columns mean the
-- business's own value applies.
CREATE TABLE service (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    category VARCHAR(40) NOT NULL,
    minutes INTEGER NOT NULL,
    price_cents BIGINT NOT NULL,
    buffer_before INTEGER NOT NULL,
    buffer_after INTEGER NOT NULL,
    capacity INTEGER NOT NULL,
    providers_needed INTEGER NOT NULL,
    resource_kind VARCHAR(40) NOT NULL,
    online SMALLINT NOT NULL,
    min_notice_minutes INTEGER NOT NULL,
    deposit_percent INTEGER NOT NULL,
    color VARCHAR(8) NOT NULL,
    intake_prompt VARCHAR(255) NOT NULL,
    active SMALLINT NOT NULL,
    sort_order INTEGER NOT NULL
);

-- Which provider offers which service. The id is the two joined by '|'.
CREATE TABLE provider_service (
    id VARCHAR(240) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    service_id VARCHAR(32) NOT NULL
);

-- Something a service needs one of, at one location.
CREATE TABLE resource (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    location_id VARCHAR(32) NOT NULL,
    kind VARCHAR(40) NOT NULL,
    name VARCHAR(80) NOT NULL,
    active SMALLINT NOT NULL
);

-- A provider's weekly hours. weekday is 1 (Monday) to 7 (Sunday); the minutes
-- are after midnight in the location's own zone, so a shift keeps its clock
-- time across a change of daylight saving.
CREATE TABLE working_hours (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    location_id VARCHAR(32) NOT NULL,
    weekday INTEGER NOT NULL,
    start_minute INTEGER NOT NULL,
    end_minute INTEGER NOT NULL
);

CREATE TABLE time_off (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    starts_at BIGINT NOT NULL,
    ends_at BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL
);

-- A calendar a provider connected. credential is the refresh token or the
-- feed address and is never sent back to any client.
CREATE TABLE calendar_link (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    label VARCHAR(120) NOT NULL,
    calendars VARCHAR(1000) NOT NULL,
    write_calendar VARCHAR(255) NOT NULL,
    credential VARCHAR(2000) NOT NULL,
    last_synced_at BIGINT NOT NULL,
    last_error VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL
);

-- Busy time read from a connected calendar. Replaced whole on every sync of
-- its link.
CREATE TABLE busy_block (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    link_id VARCHAR(32) NOT NULL,
    starts_at BIGINT NOT NULL,
    ends_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

-- A booking. The providers and resources it takes are appointment_party rows,
-- and the time it takes is slot_claim rows.
CREATE TABLE appointment (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    service_id VARCHAR(32) NOT NULL,
    location_id VARCHAR(32) NOT NULL,
    client VARCHAR(190) NOT NULL,
    client_name VARCHAR(120) NOT NULL,
    starts_at BIGINT NOT NULL,
    ends_at BIGINT NOT NULL,
    block_start BIGINT NOT NULL,
    block_end BIGINT NOT NULL,
    state VARCHAR(16) NOT NULL,
    hold_expires_at BIGINT NOT NULL,
    price_cents BIGINT NOT NULL,
    deposit_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    method_id VARCHAR(32) NOT NULL,
    payment_ref VARCHAR(64) NOT NULL,
    payment_state VARCHAR(16) NOT NULL,
    note VARCHAR(1000) NOT NULL,
    group_key VARCHAR(120) NOT NULL,
    online SMALLINT NOT NULL,
    meeting_url VARCHAR(255) NOT NULL,
    share_token VARCHAR(32) NOT NULL,
    reminded_at BIGINT NOT NULL,
    rescheduled_from VARCHAR(32) NOT NULL,
    cancel_reason VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

-- PROVIDER and RESOURCE rows are what the appointment blocks; GUEST rows are
-- people invited to it, by e-mail.
CREATE TABLE appointment_party (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    appointment_id VARCHAR(32) NOT NULL,
    kind VARCHAR(12) NOT NULL,
    ref VARCHAR(190) NOT NULL,
    name VARCHAR(120) NOT NULL
);

-- The cells of the five-minute grid an appointment takes from each provider
-- and resource. The primary key is what makes a double booking impossible.
CREATE TABLE slot_claim (
    id VARCHAR(240) NOT NULL PRIMARY KEY,
    appointment_id VARCHAR(32) NOT NULL,
    ref VARCHAR(190) NOT NULL,
    cell BIGINT NOT NULL
);

CREATE TABLE group_slot (
    id VARCHAR(120) NOT NULL PRIMARY KEY,
    appointment_id VARCHAR(32) NOT NULL,
    seats INTEGER NOT NULL,
    capacity INTEGER NOT NULL
);

-- A client waiting for a slot. Days are day numbers in the business's zone;
-- minutes are after midnight.
CREATE TABLE waitlist (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    service_id VARCHAR(32) NOT NULL,
    provider VARCHAR(190) NOT NULL,
    client VARCHAR(190) NOT NULL,
    from_day BIGINT NOT NULL,
    to_day BIGINT NOT NULL,
    from_minute INTEGER NOT NULL,
    to_minute INTEGER NOT NULL,
    state VARCHAR(12) NOT NULL,
    offered VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX working_hours_provider ON working_hours (username, weekday);
CREATE INDEX time_off_provider ON time_off (username, starts_at);
CREATE INDEX busy_block_provider ON busy_block (username, starts_at);
CREATE INDEX busy_block_link ON busy_block (link_id);
CREATE INDEX calendar_link_provider ON calendar_link (username);
CREATE INDEX appointment_client ON appointment (client, starts_at);
CREATE INDEX appointment_start ON appointment (starts_at, state);
CREATE INDEX appointment_share ON appointment (share_token);
CREATE INDEX appointment_party_appointment ON appointment_party (appointment_id);
CREATE INDEX appointment_party_ref ON appointment_party (ref, kind);
CREATE INDEX slot_claim_appointment ON slot_claim (appointment_id);
CREATE INDEX slot_claim_ref ON slot_claim (ref, cell);
CREATE INDEX waitlist_service ON waitlist (service_id, state);
CREATE INDEX provider_service_service ON provider_service (service_id);
