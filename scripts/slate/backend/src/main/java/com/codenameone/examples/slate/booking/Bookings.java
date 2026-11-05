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
package com.codenameone.examples.slate.booking;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.slate.Ids;
import com.codenameone.examples.slate.Text;
import com.codenameone.examples.slate.api.AppointmentDto;
import com.codenameone.examples.slate.api.CancelDto;
import com.codenameone.examples.slate.api.ConfirmDto;
import com.codenameone.examples.slate.api.HoldRequestDto;
import com.codenameone.examples.slate.api.PartyDto;
import com.codenameone.examples.slate.api.RescheduleDto;
import com.codenameone.examples.slate.catalog.Catalog;
import com.codenameone.examples.slate.domain.Appointment;
import com.codenameone.examples.slate.domain.AppointmentParty;
import com.codenameone.examples.slate.domain.Business;
import com.codenameone.examples.slate.domain.GroupSlot;
import com.codenameone.examples.slate.domain.Location;
import com.codenameone.examples.slate.domain.Provider;
import com.codenameone.examples.slate.domain.Service;
import com.codenameone.examples.slate.domain.SlotClaim;
import com.codenameone.examples.slate.live.LiveHub;
import com.codenameone.examples.slate.pay.PaymentDeclinedException;
import com.codenameone.examples.slate.pay.Payments;
import com.codenameone.examples.slate.push.Notifier;
import com.codenameone.examples.slate.repository.AppointmentPartyRepository;
import com.codenameone.examples.slate.repository.AppointmentRepository;
import com.codenameone.examples.slate.repository.GroupSlotRepository;
import com.codenameone.examples.slate.repository.LocationRepository;
import com.codenameone.examples.slate.repository.ProviderRepository;
import com.codenameone.examples.slate.repository.ServiceRepository;
import com.codenameone.examples.slate.repository.SlotClaimRepository;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/// A booking's whole life: held, confirmed, moved, cancelled, finished.
///
/// ## Why two clients never get one slot
///
/// The engine decides whether a start is free from what it reads, and two
/// requests can read the same free afternoon. What makes the second one fail
/// is the table [SlotClaim]: every five-minute cell an appointment takes from
/// each provider and resource is a row, keyed by the party and the cell. Two
/// bookings that overlap anywhere want one same key, and the database refuses
/// the second insert. Its transaction rolls back whole, and the request answers
/// 409. Nothing here reads the database's error, which differs by database: if
/// the start is no longer offered once the transaction is over, that is why.
///
/// The rules that are not about one cell -- a daily cap, travel, a run without
/// a break -- are kept by holding the providers' rows for the length of the
/// booking transaction, so two bookings for one provider take turns.
///
/// ## Deposits
///
/// The processor is called with no transaction open, between one that reads
/// what the call needs and one that records what came of it (see `Payments`).
/// A deposit is held on the card at confirmation when the start is close
/// enough for a hold to last; further ahead it is PENDING and [#authorizeDue]
/// places it later. A free cancellation lets it go; a late one, a no-show or a
/// finished appointment takes it.
@Component
public class Bookings {
    /// How long ahead of the start a card hold is placed. Card holds lapse
    /// after about a week at most processors.
    static final long AUTHORIZE_AHEAD = 6L * 86400000L;
    private static final long HOUR = 3600000L;

    private final Catalog catalog;
    private final Availability availability;
    private final AppointmentRepository appointments;
    private final AppointmentPartyRepository parties;
    private final SlotClaimRepository claims;
    private final GroupSlotRepository groups;
    private final ProviderRepository providers;
    private final ServiceRepository services;
    private final LocationRepository locations;
    private final Payments payments;
    private final Notifier notifier;
    private final LiveHub live;
    private final Waitlists waitlists;

    public Bookings(Catalog catalog, Availability availability, AppointmentRepository appointments,
            AppointmentPartyRepository parties, SlotClaimRepository claims,
            GroupSlotRepository groups, ProviderRepository providers, ServiceRepository services,
            LocationRepository locations, Payments payments, Notifier notifier, LiveHub live,
            Waitlists waitlists) {
        this.catalog = catalog;
        this.availability = availability;
        this.appointments = appointments;
        this.parties = parties;
        this.claims = claims;
        this.groups = groups;
        this.providers = providers;
        this.services = services;
        this.locations = locations;
        this.payments = payments;
        this.notifier = notifier;
        this.live = live;
        this.waitlists = waitlists;
    }

    // ------------------------------------------------------------- reading

    @Transactional(readOnly = true)
    public List<AppointmentDto> upcoming(String client) {
        return dtos(appointments.upcoming(client, Instant.now()));
    }

    @Transactional(readOnly = true)
    public List<AppointmentDto> history(String client) {
        return dtos(appointments.history(client, Instant.now()));
    }

    @Transactional(readOnly = true)
    public AppointmentDto mine(String client, String id) {
        return dto(own(client, id));
    }

    /// An appointment as anyone may see it, for the provider's and owner's
    /// screens.
    @Transactional(readOnly = true)
    public AppointmentDto any(String id) {
        return dto(found(id));
    }

    @Transactional(readOnly = true)
    public List<AppointmentDto> dtos(List<Appointment> list) {
        List<AppointmentDto> out = new ArrayList<AppointmentDto>();
        Business business = catalog.main();
        for (int iter = 0; iter < list.size(); iter++) {
            out.add(dto(list.get(iter), business));
        }
        return out;
    }

    // ------------------------------------------------------------- holding

    // tag::slate-hold[]
    /// Holds a start for `client` while they finish booking. Answers 409 with
    /// the reason when the start is not free, or was taken a moment ago.
    public AppointmentDto hold(String client, String clientName, HoldRequestDto ask)
            throws IOException {
        if (ask == null) {
            throw new ResponseStatusException(400, "Nothing to hold");
        }
        AppointmentDto held;
        try {
            held = placeHold(client, clientName, ask);
        } catch (ResponseStatusException answered) {
            throw answered;
        } catch (IOException failed) {
            throw taken(client, ask.serviceId(), ask.locationId(), ask.start(), failed);
        } catch (RuntimeException failed) {
            throw taken(client, ask.serviceId(), ask.locationId(), ask.start(), failed);
        }
        changed(held, null);
        return held;
    }
    // end::slate-hold[]

    @Transactional(rollbackFor = IOException.class)
    private AppointmentDto placeHold(String client, String clientName, HoldRequestDto ask)
            throws IOException {
        Business business = catalog.main();
        Service service = catalog.offered(ask.serviceId());
        Location location = catalog.openLocation(ask.locationId());
        long now = System.currentTimeMillis();
        Appointment a = new Appointment();
        a.setId(Ids.next());
        a.setServiceId(service.getId());
        a.setLocationId(location.getId());
        a.setClient(client);
        a.setClientName(Text.cut(clientName, 120));
        a.setState(States.HELD);
        a.setHoldExpiresAt(Instant.ofEpochMilli(now + business.getHoldMinutes() * 60000L));
        a.setPriceCents(service.getPriceCents());
        a.setDepositCents(service.getPriceCents() * deposit(business, service) / 100);
        a.setCurrency(business.getCurrency());
        a.setPaymentState(States.PAY_NONE);
        a.setOnline(service.isOnline());
        a.setShareToken(Ids.next());
        a.setRemindedAt(Instant.EPOCH);
        a.setCreatedAt(Instant.ofEpochMilli(now));
        a.setUpdatedAt(Instant.ofEpochMilli(now));
        if (service.isOnline()) {
            a.setMeetingUrl("https://meet.example.com/slate-" + a.getId());
        }
        place(a, business, service, location, ask.start(), required(ask.providers()), now);
        appointments.insert(a);
        return dto(a, business);
    }

    // ------------------------------------------------------------ confirming

    /// Turns a hold into a booking, holding the deposit on the client's card.
    /// Answers 409 when the hold ran out first and 402 when the card refused.
    public AppointmentDto confirm(String client, String id, ConfirmDto how) throws IOException {
        Plan plan = planConfirm(client, id, how == null ? null : how.methodId());
        String reference = "";
        if (plan.authorizeNow) {
            try {
                reference = payments.authorize("slate-" + id, client, plan.methodId,
                        plan.amount, plan.currency, plan.description);
            } catch (PaymentDeclinedException declined) {
                throw new ResponseStatusException(402, declined.getMessage());
            }
        }
        AppointmentDto done;
        try {
            done = finishConfirm(client, id, plan, reference, how);
        } catch (ResponseStatusException lapsed) {
            letGo(reference);
            throw lapsed;
        }
        changed(done, Notifier.BOOKED);
        return done;
    }

    @Transactional(readOnly = true)
    private Plan planConfirm(String client, String id, String wanted) throws IOException {
        Appointment a = own(client, id);
        if (!States.HELD.equals(a.getState())) {
            throw new ResponseStatusException(409, "That booking is already "
                    + a.getState().replace('_', ' '));
        }
        if (a.getHoldExpiresAt().toEpochMilli() < System.currentTimeMillis()) {
            throw new ResponseStatusException(409, "The hold ran out. Pick the time again");
        }
        Business business = catalog.main();
        Plan plan = new Plan();
        plan.currency = a.getCurrency();
        plan.amount = holdAmount(a, business);
        plan.description = "Deposit for " + serviceName(a.getServiceId());
        if (plan.amount > 0) {
            plan.methodId = payments.choose(client, wanted)[0];
            if (Payments.CASH.equals(plan.methodId)) {
                // Paid in person: there is nothing to hold.
                plan.amount = 0;
            }
        }
        plan.pending = plan.amount > 0
                && a.getStartsAt().toEpochMilli() - System.currentTimeMillis() > AUTHORIZE_AHEAD;
        plan.authorizeNow = plan.amount > 0 && !plan.pending;
        return plan;
    }

    @Transactional(rollbackFor = IOException.class)
    private AppointmentDto finishConfirm(String client, String id, Plan plan, String reference,
            ConfirmDto how) throws IOException {
        Instant now = Instant.now();
        if (appointments.move(id, States.HELD, States.CONFIRMED, now) == 0) {
            throw new ResponseStatusException(409, "The hold ran out. Pick the time again");
        }
        Appointment a = own(client, id);
        a.setState(States.CONFIRMED);
        a.setUpdatedAt(now);
        if (plan.amount > 0) {
            a.setMethodId(plan.methodId);
            a.setPaymentRef(reference);
            a.setPaymentState(plan.pending ? States.PAY_PENDING : States.PAY_AUTHORIZED);
        } else {
            a.setDepositCents(0);
        }
        if (how != null && how.note() != null) {
            a.setNote(Text.cut(how.note().trim(), 500));
        }
        if (how != null && how.guests() != null) {
            for (int iter = 0; iter < how.guests().size() && iter < 8; iter++) {
                String guest = how.guests().get(iter) == null ? "" : how.guests().get(iter).trim();
                if (guest.length() > 0) {
                    party(a.getId(), States.GUEST, Text.cut(guest, 190),
                            Text.cut(guest, 120));
                }
            }
        }
        appointments.save(a);
        return dto(a, catalog.main());
    }

    // ------------------------------------------------------------- moving

    /// Moves a client's booking to another start, keeping its deposit. Answers
    /// 409 when it is too close to move, or the new start is not free.
    public AppointmentDto reschedule(String client, String id, RescheduleDto to)
            throws IOException {
        if (to == null) {
            throw new ResponseStatusException(400, "Say when to move it to");
        }
        Appointment before = ownRead(client, id);
        AppointmentDto moved;
        try {
            moved = move(client, id, to);
        } catch (ResponseStatusException answered) {
            throw answered;
        } catch (IOException failed) {
            throw taken(client, before.getServiceId(), before.getLocationId(), to.start(), failed);
        } catch (RuntimeException failed) {
            throw taken(client, before.getServiceId(), before.getLocationId(), to.start(), failed);
        }
        live.slotsChanged(before.getServiceId(), day(before));
        changed(moved, Notifier.CHANGED);
        return moved;
    }

    @Transactional(rollbackFor = IOException.class)
    private AppointmentDto move(String client, String id, RescheduleDto to) throws IOException {
        Appointment a = own(client, id);
        Business business = catalog.main();
        if (!States.CONFIRMED.equals(a.getState()) && !States.HELD.equals(a.getState())) {
            throw new ResponseStatusException(409, "Only an upcoming booking can be moved");
        }
        long now = System.currentTimeMillis();
        if (now > a.getStartsAt().toEpochMilli() - business.getRescheduleHours() * HOUR) {
            throw new ResponseStatusException(409, "It is too close to the start to move it."
                    + " Call to change it");
        }
        List<String> who = required(to.providers());
        if (who.isEmpty()) {
            who = providerRefs(a.getId());
        }
        free(a);
        parties.deleteByAppointmentId(a.getId());
        Service service = catalog.offered(a.getServiceId());
        Location location = catalog.openLocation(a.getLocationId());
        place(a, business, service, location, to.start(), who, now);
        a.setRemindedAt(Instant.EPOCH);
        a.setUpdatedAt(Instant.ofEpochMilli(now));
        appointments.save(a);
        return dto(a, business);
    }

    // ----------------------------------------------------------- cancelling

    /// Cancels a client's own booking. Later than the business's cancellation
    /// window, the deposit is kept.
    public AppointmentDto cancelByClient(String client, String id, CancelDto why)
            throws IOException {
        ownRead(client, id);
        return cancel(id, true, why == null ? "" : why.reason());
    }

    /// Cancels a booking for the business. The client is never charged for a
    /// cancellation they did not make.
    public AppointmentDto cancelByBusiness(String provider, boolean owner, String id,
            CancelDto why) throws IOException {
        checkWorks(provider, owner, id);
        return cancel(id, false, why == null ? "" : why.reason());
    }

    private AppointmentDto cancel(String id, boolean byClient, String reason) throws IOException {
        Settle settle = cancelTx(id, byClient, reason);
        settle(settle);
        AppointmentDto done = anyRead(id);
        changed(done, Notifier.CANCELLED);
        waitlists.freed(done.serviceId(), providerNames(done), done.start());
        return done;
    }

    @Transactional(rollbackFor = IOException.class)
    private Settle cancelTx(String id, boolean byClient, String reason) {
        Appointment a = found(id);
        String was = a.getState();
        if (!States.CONFIRMED.equals(was) && !States.HELD.equals(was)) {
            throw new ResponseStatusException(409, "That booking is already "
                    + was.replace('_', ' '));
        }
        Instant now = Instant.now();
        if (appointments.move(id, was, States.CANCELLED, now) == 0) {
            throw new ResponseStatusException(409, "That booking just changed. Look again");
        }
        Business business = catalog.main();
        boolean late = byClient && now.toEpochMilli()
                > a.getStartsAt().toEpochMilli() - business.getCancelHours() * HOUR;
        a.setState(States.CANCELLED);
        a.setCancelReason(Text.cut(reason == null ? "" : reason.trim(), 200));
        a.setUpdatedAt(now);
        free(a);
        Settle settle = settlement(a, late ? a.getDepositCents() : 0);
        appointments.save(a);
        return settle;
    }

    // ------------------------------------------------------------ finishing

    /// Marks a booking done and takes its deposit.
    public AppointmentDto complete(String provider, boolean owner, String id) throws IOException {
        return finish(provider, owner, id, States.COMPLETED);
    }

    /// Marks that the client did not come, and takes the no-show fee.
    public AppointmentDto noShow(String provider, boolean owner, String id) throws IOException {
        return finish(provider, owner, id, States.NO_SHOW);
    }

    private AppointmentDto finish(String provider, boolean owner, String id, String state)
            throws IOException {
        checkWorks(provider, owner, id);
        Settle settle = finishTx(id, state);
        settle(settle);
        AppointmentDto done = anyRead(id);
        changed(done, null);
        return done;
    }

    @Transactional(rollbackFor = IOException.class)
    private Settle finishTx(String id, String state) {
        Appointment a = found(id);
        if (a.getStartsAt().toEpochMilli() > System.currentTimeMillis()) {
            throw new ResponseStatusException(409, "It has not started yet");
        }
        Instant now = Instant.now();
        if (appointments.move(id, States.CONFIRMED, state, now) == 0) {
            throw new ResponseStatusException(409, "Only a confirmed booking can be finished");
        }
        a.setState(state);
        a.setUpdatedAt(now);
        long take = States.NO_SHOW.equals(state)
                ? Math.max(a.getDepositCents(), noShowFee(a, catalog.main()))
                : a.getDepositCents();
        Settle settle = settlement(a, take);
        appointments.save(a);
        return settle;
    }

    // ----------------------------------------------------------------- jobs

    /// Lets holds that ran out go, so their time is offered again.
    @Scheduled(fixedDelay = 60000, initialDelay = 30000, lock = "slate-holds")
    public void sweepHolds() throws IOException {
        List<String[]> freed = sweepTx();
        for (int iter = 0; iter < freed.size(); iter++) {
            live.slotsChanged(freed.get(iter)[0], freed.get(iter)[1]);
        }
    }

    @Transactional(rollbackFor = IOException.class)
    private List<String[]> sweepTx() {
        Instant now = Instant.now();
        List<Appointment> lapsed = appointments.lapsedHolds(now);
        List<String[]> out = new ArrayList<String[]>();
        for (int iter = 0; iter < lapsed.size(); iter++) {
            Appointment a = lapsed.get(iter);
            if (appointments.move(a.getId(), States.HELD, States.CANCELLED, now) == 0) {
                continue;
            }
            a.setState(States.CANCELLED);
            a.setCancelReason("The hold ran out");
            a.setUpdatedAt(now);
            free(a);
            appointments.save(a);
            out.add(new String[] {a.getServiceId(), day(a)});
        }
        return out;
    }

    /// Places the deposits of bookings made far ahead, once their start is
    /// close enough for a card hold to last until it.
    @Scheduled(fixedDelay = 600000, initialDelay = 90000, lock = "slate-deposits")
    public void authorizeDue() throws IOException {
        List<Appointment> due = dueTx();
        Business business = catalog.main();
        for (int iter = 0; iter < due.size(); iter++) {
            Appointment a = due.get(iter);
            String reference = "";
            String state = States.PAY_AUTHORIZED;
            try {
                reference = payments.authorize("slate-" + a.getId(), a.getClient(),
                        a.getMethodId(), holdAmount(a, business), a.getCurrency(),
                        "Deposit for " + serviceName(a.getServiceId()));
            } catch (IOException failed) {
                System.err.println("deposit hold for " + a.getId() + " failed: "
                        + failed.getMessage());
                state = States.PAY_FAILED;
            }
            if (!recordPayment(a.getId(), States.PAY_PENDING, state, reference)) {
                letGo(reference);
            } else if (States.PAY_FAILED.equals(state)) {
                notifier.tell(a.getClient(), Notifier.PAYMENT, a.getId(),
                        "Your card was refused",
                        "Add another card to keep your booking for " + serviceName(a.getServiceId()));
            }
        }
    }

    @Transactional(readOnly = true)
    private List<Appointment> dueTx() {
        return appointments.toAuthorize(Instant.ofEpochMilli(System.currentTimeMillis()
                + AUTHORIZE_AHEAD));
    }

    /// Reminds clients of a booking the business's reminder window ahead.
    @Scheduled(fixedDelay = 300000, initialDelay = 120000, lock = "slate-reminders")
    public void remind() throws IOException {
        List<Appointment> due = remindTx();
        for (int iter = 0; iter < due.size(); iter++) {
            Appointment a = due.get(iter);
            notifier.tell(a.getClient(), Notifier.REMINDER, a.getId(),
                    serviceName(a.getServiceId()) + " is coming up",
                    "See you at " + locationName(a.getLocationId()));
        }
    }

    @Transactional(rollbackFor = IOException.class)
    private List<Appointment> remindTx() {
        Business business = catalog.main();
        Instant now = Instant.now();
        List<Appointment> due = appointments.toRemind(now,
                Instant.ofEpochMilli(now.toEpochMilli() + business.getReminderHours() * HOUR),
                Instant.EPOCH);
        for (int iter = 0; iter < due.size(); iter++) {
            due.get(iter).setRemindedAt(now);
            appointments.save(due.get(iter));
        }
        return due;
    }

    // ------------------------------------------------------------ internals

    // tag::slate-place[]
    /// Puts `a` at `start`: asks the engine, writes who and what it takes, and
    /// claims their cells. A group service joins the slot's existing seat
    /// count, or opens it.
    private void place(Appointment a, Business business, Service service, Location location,
            long start, List<String> required, long now) throws IOException {
        List<Provider> candidates = availability.candidates(service, required);
        for (int iter = 0; iter < candidates.size(); iter++) {
            providers.hold(candidates.get(iter).getUsername());
        }
        ZoneId zone = ZoneId.of(location.getTimeZone());
        java.time.LocalDate day = Instant.ofEpochMilli(start).atZone(zone).toLocalDate();
        SlotEngine.Request request = availability.request(business, service, location, day,
                candidates, required, a.getClient(), now, a.getId());
        if (!offered(request.starts, start)) {
            throw new ResponseStatusException(400, "That is not a start this day offers");
        }
        request.starts = new long[] {start};
        request.explain = true;
        SlotEngine.Result result = SlotEngine.evaluate(request);
        SlotEngine.Slot slot = result.at(start);
        if (slot == null) {
            throw new ResponseStatusException(409, say(result.reason(start)));
        }
        a.setStartsAt(Instant.ofEpochMilli(start));
        a.setEndsAt(Instant.ofEpochMilli(slot.end));
        a.setBlockStart(Instant.ofEpochMilli(start - service.getBufferBefore() * 60000L));
        a.setBlockEnd(Instant.ofEpochMilli(slot.end + service.getBufferAfter() * 60000L));
        List<String> refs = new ArrayList<String>();
        for (int iter = 0; iter < slot.providers.size(); iter++) {
            String username = slot.providers.get(iter);
            refs.add(username);
            party(a.getId(), States.PROVIDER, username, displayName(candidates, username));
        }
        if (slot.resourceId.length() > 0) {
            refs.add(slot.resourceId);
            party(a.getId(), States.RESOURCE, slot.resourceId, slot.resourceId);
        }
        if (service.getCapacity() > 1) {
            String key = SlotEngine.groupKey(service.getId(), slot.providers.get(0), start);
            a.setGroupKey(key);
            if (request.groupSeats.containsKey(key)) {
                if (groups.take(key) == 0) {
                    throw new ResponseStatusException(409, say(SlotEngine.GROUP_FULL));
                }
                return;
            }
            GroupSlot group = groups.findById(key);
            if (group != null) {
                // Every seat of an earlier slot was given back; it is opened again.
                if (groups.take(key) == 0) {
                    throw new ResponseStatusException(409, say(SlotEngine.GROUP_FULL));
                }
            } else {
                group = new GroupSlot();
                group.setId(key);
                group.setAppointmentId(a.getId());
                group.setSeats(1);
                group.setCapacity(service.getCapacity());
                groups.insert(group);
            }
            claim(key, refs, a.getBlockStart().toEpochMilli(), a.getBlockEnd().toEpochMilli());
            return;
        }
        a.setGroupKey("");
        claim(a.getId(), refs, a.getBlockStart().toEpochMilli(), a.getBlockEnd().toEpochMilli());
    }

    /// One row per party and five-minute cell. The key is the party and the
    /// cell, so an overlapping booking cannot be written.
    private void claim(String owner, List<String> refs, long from, long to) {
        for (int r = 0; r < refs.size(); r++) {
            // Cell numbers, not millis: the last cell is the one `to - 1` is in.
            long last = SlotEngine.cell(to - 1);
            for (long cell = SlotEngine.cell(from); cell <= last; cell++) {
                SlotClaim c = new SlotClaim();
                c.setId(refs.get(r) + "|" + cell);
                c.setAppointmentId(owner);
                c.setRef(refs.get(r));
                c.setCell(cell);
                claims.insert(c);
            }
        }
    }
    // end::slate-place[]

    /// Gives back what `a` took. A seat of a group slot gives back the seat;
    /// the cells go with the last one.
    private void free(Appointment a) {
        if (a.getGroupKey().length() == 0) {
            claims.deleteByAppointmentId(a.getId());
            return;
        }
        groups.give(a.getGroupKey());
        GroupSlot group = groups.findById(a.getGroupKey());
        if (group == null || group.getSeats() == 0) {
            claims.deleteByAppointmentId(a.getGroupKey());
        }
    }

    /// The 409 for a booking whose transaction failed because the start was
    /// taken meanwhile, or the failure itself when it was something else.
    private <T extends Exception> T taken(String client, String serviceId, String locationId,
            long start, T failed) {
        if (!stillFree(client, serviceId, locationId, start)) {
            throw new ResponseStatusException(409, "Someone just took that time. Pick another");
        }
        return failed;
    }

    @Transactional(readOnly = true)
    private boolean stillFree(String client, String serviceId, String locationId, long start) {
        try {
            Business business = catalog.main();
            Service service = catalog.offered(serviceId);
            Location location = catalog.openLocation(locationId);
            java.time.LocalDate day = Instant.ofEpochMilli(start)
                    .atZone(ZoneId.of(location.getTimeZone())).toLocalDate();
            SlotEngine.Request request = availability.request(business, service, location, day,
                    availability.candidates(service, new ArrayList<String>()),
                    new ArrayList<String>(), client, System.currentTimeMillis(), null);
            request.starts = new long[] {start};
            return SlotEngine.evaluate(request).at(start) != null;
        } catch (ResponseStatusException gone) {
            return false;
        }
    }

    private Settle settlement(Appointment a, long take) {
        Settle settle = new Settle();
        settle.id = a.getId();
        settle.was = a.getPaymentState();
        if (States.PAY_AUTHORIZED.equals(a.getPaymentState())) {
            settle.reference = a.getPaymentRef();
            settle.take = take;
        } else if (States.PAY_PENDING.equals(a.getPaymentState())) {
            // Never placed: there is nothing to take or let go.
            a.setPaymentState(States.PAY_NONE);
            settle.was = "";
        } else {
            settle.was = "";
        }
        return settle;
    }

    private void settle(Settle settle) throws IOException {
        if (settle.was.length() == 0) {
            return;
        }
        String state;
        try {
            if (settle.take > 0) {
                payments.capture(settle.reference, settle.take);
                state = States.PAY_CAPTURED;
            } else {
                payments.release(settle.reference);
                state = States.PAY_RELEASED;
            }
        } catch (IOException failed) {
            // Left for the owner to settle by hand: the hold lapses by itself
            // when it was a release, and a capture can be asked for again.
            System.err.println("deposit of " + settle.id + " not settled: "
                    + failed.getMessage());
            state = States.PAY_FAILED;
        }
        recordPayment(settle.id, settle.was, state, null);
    }

    @Transactional(rollbackFor = IOException.class)
    private boolean recordPayment(String id, String from, String to, String reference) {
        Appointment a = appointments.findById(id);
        if (a == null || !from.equals(a.getPaymentState())) {
            return false;
        }
        if (States.PAY_PENDING.equals(from) && !States.CONFIRMED.equals(a.getState())) {
            // Cancelled while the hold was being placed.
            return false;
        }
        a.setPaymentState(to);
        if (reference != null) {
            a.setPaymentRef(reference);
        }
        a.setUpdatedAt(Instant.now());
        appointments.save(a);
        return true;
    }

    private void letGo(String reference) {
        if (reference == null || reference.length() == 0) {
            return;
        }
        try {
            payments.release(reference);
        } catch (IOException failed) {
            // An unused hold lapses at the processor by itself.
            System.err.println("unused deposit hold not released: " + failed.getMessage());
        }
    }

    /// Tells everyone a change concerns: the client, the providers, the owners
    /// watching, and anyone looking at that day's slots.
    private void changed(AppointmentDto a, String kind) throws IOException {
        List<String> accounts = new ArrayList<String>();
        accounts.add(a.client());
        List<String> who = providerNames(a);
        accounts.addAll(who);
        live.appointmentChanged(accounts, a.id(), a.state());
        live.slotsChanged(a.serviceId(), localDay(a.start(), a.locationId()));
        for (int iter = 0; iter < who.size(); iter++) {
            live.scheduleChanged(who.get(iter));
            if (kind != null) {
                notifier.tell(who.get(iter), Notifier.CALENDAR, a.id(),
                        title(kind, a.serviceName()), a.clientName());
            }
        }
        if (Notifier.CHANGED.equals(kind) || Notifier.BOOKED.equals(kind)) {
            notifier.tell(a.client(), kind, a.id(), title(kind, a.serviceName()),
                    a.locationName());
        }
    }

    private static String title(String kind, String service) {
        if (Notifier.BOOKED.equals(kind)) {
            return service + " booked";
        }
        if (Notifier.CHANGED.equals(kind)) {
            return service + " moved";
        }
        return service + " cancelled";
    }

    private static List<String> providerNames(AppointmentDto a) {
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < a.parties().size(); iter++) {
            if (States.PROVIDER.equals(a.parties().get(iter).kind())) {
                out.add(a.parties().get(iter).ref());
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    private void checkWorks(String provider, boolean owner, String id) {
        found(id);
        if (owner) {
            return;
        }
        if (!providerRefs(id).contains(provider)) {
            throw new ResponseStatusException(404, "No such appointment");
        }
    }

    @Transactional(readOnly = true)
    private Appointment ownRead(String client, String id) {
        return own(client, id);
    }

    @Transactional(readOnly = true)
    private AppointmentDto anyRead(String id) {
        return dto(found(id));
    }

    private Appointment own(String client, String id) {
        Appointment a = appointments.findFirstByIdAndClient(id, client);
        if (a == null) {
            throw new ResponseStatusException(404, "No such appointment");
        }
        return a;
    }

    private Appointment found(String id) {
        Appointment a = appointments.findById(id);
        if (a == null) {
            throw new ResponseStatusException(404, "No such appointment");
        }
        return a;
    }

    private List<String> providerRefs(String id) {
        List<AppointmentParty> list = parties.findByAppointmentId(id);
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < list.size(); iter++) {
            if (States.PROVIDER.equals(list.get(iter).getKind())) {
                out.add(list.get(iter).getRef());
            }
        }
        return out;
    }

    private void party(String appointmentId, String kind, String ref, String name)
            throws IOException {
        AppointmentParty p = new AppointmentParty();
        p.setId(Ids.next());
        p.setAppointmentId(appointmentId);
        p.setKind(kind);
        p.setRef(ref);
        p.setName(name);
        parties.insert(p);
    }

    AppointmentDto dto(Appointment a) {
        return dto(a, catalog.main());
    }

    AppointmentDto dto(Appointment a, Business business) {
        Service service = services.findById(a.getServiceId());
        List<AppointmentParty> list = parties.findByAppointmentId(a.getId());
        List<PartyDto> who = new ArrayList<PartyDto>();
        for (int iter = 0; iter < list.size(); iter++) {
            AppointmentParty p = list.get(iter);
            who.add(new PartyDto(p.getKind(), p.getRef(), p.getName()));
        }
        long start = a.getStartsAt().toEpochMilli();
        return new AppointmentDto(a.getId(), a.getServiceId(),
                service == null ? "" : service.getName(), a.getLocationId(),
                locationName(a.getLocationId()), a.getClient(), a.getClientName(), start,
                a.getEndsAt().toEpochMilli(), a.getBlockStart().toEpochMilli(),
                a.getBlockEnd().toEpochMilli(), a.getState(), a.getHoldExpiresAt().toEpochMilli(),
                a.getPriceCents(), a.getDepositCents(), a.getCurrency(), a.getPaymentState(),
                a.getNote(), a.isOnline(), a.getMeetingUrl(), who,
                start - business.getCancelHours() * HOUR,
                start - business.getRescheduleHours() * HOUR,
                "/ics/a/" + a.getShareToken() + ".ics",
                service == null ? "" : service.getColor());
    }

    private String serviceName(String id) {
        Service s = services.findById(id);
        return s == null ? "Your appointment" : s.getName();
    }

    private String locationName(String id) {
        Location l = locations.findById(id);
        return l == null ? "" : l.getName();
    }

    private String day(Appointment a) {
        return localDay(a.getStartsAt().toEpochMilli(), a.getLocationId());
    }

    @Transactional(readOnly = true)
    private String localDay(long start, String locationId) {
        Location l = locations.findById(locationId);
        ZoneId zone = l == null ? catalog.zone() : ZoneId.of(l.getTimeZone());
        return Instant.ofEpochMilli(start).atZone(zone).toLocalDate().toString();
    }

    static int deposit(Business business, Service service) {
        return service.getDepositPercent() >= 0 ? service.getDepositPercent()
                : business.getDepositPercent();
    }

    static long noShowFee(Appointment a, Business business) {
        return a.getPriceCents() * business.getNoShowFeePercent() / 100;
    }

    /// What is held on the card: enough for the deposit or the no-show fee,
    /// whichever is more, since either may be what is taken.
    static long holdAmount(Appointment a, Business business) {
        if (a.getDepositCents() <= 0) {
            return 0;
        }
        return Math.max(a.getDepositCents(), noShowFee(a, business));
    }

    private static boolean offered(long[] starts, long start) {
        for (int iter = 0; iter < starts.length; iter++) {
            if (starts[iter] == start) {
                return true;
            }
        }
        return false;
    }

    private static List<String> required(List<String> named) {
        List<String> out = new ArrayList<String>();
        if (named == null) {
            return out;
        }
        for (int iter = 0; iter < named.size(); iter++) {
            String one = named.get(iter) == null ? "" : named.get(iter).trim();
            if (one.length() > 0 && !Catalog.ANY.equals(one) && !out.contains(one)) {
                out.add(one);
            }
        }
        return out;
    }

    private static String displayName(List<Provider> candidates, String username) {
        for (int iter = 0; iter < candidates.size(); iter++) {
            if (candidates.get(iter).getUsername().equals(username)) {
                return candidates.get(iter).getDisplayName();
            }
        }
        return username;
    }

    // tag::slate-reasons[]
    /// What a client is told when a start is turned down.
    public static String say(String reason) {
        if (SlotEngine.TOO_SOON.equals(reason)) {
            return "That is too soon to book";
        }
        if (SlotEngine.TOO_FAR.equals(reason)) {
            return "That is further ahead than bookings open";
        }
        if (SlotEngine.OUTSIDE_HOURS.equals(reason)) {
            return "Nobody is working then";
        }
        if (SlotEngine.BOOKED.equals(reason) || SlotEngine.TIME_OFF.equals(reason)
                || SlotEngine.CALENDAR.equals(reason)) {
            return "That time is taken";
        }
        if (SlotEngine.TRAVEL.equals(reason)) {
            return "There is not enough time to travel between locations";
        }
        if (SlotEngine.DAY_FULL.equals(reason)) {
            return "That day is fully booked";
        }
        if (SlotEngine.NEEDS_BREAK.equals(reason)) {
            return "That time is kept for a break";
        }
        if (SlotEngine.LEAVES_GAP.equals(reason) || SlotEngine.NOT_ADJACENT.equals(reason)) {
            return "That time would leave a gap too short to book. Try a time next to"
                    + " another booking";
        }
        if (SlotEngine.NO_RESOURCE.equals(reason)) {
            return "Every room is in use then";
        }
        if (SlotEngine.CLIENT_BUSY.equals(reason)) {
            return "You already have a booking then";
        }
        if (SlotEngine.CLIENT_GAP.equals(reason)) {
            return "That is too close to another of your bookings";
        }
        if (SlotEngine.CLIENT_DAY_FULL.equals(reason)) {
            return "You already have as many bookings that day as you can";
        }
        if (SlotEngine.GROUP_FULL.equals(reason)) {
            return "Every place is taken";
        }
        return "Nobody who offers that is free then";
    }
    // end::slate-reasons[]

    /// What a confirmation will do about money, read before the processor is
    /// called.
    private static final class Plan {
        String methodId = "";
        long amount;
        String currency = "";
        String description = "";
        boolean pending;
        boolean authorizeNow;
    }

    /// What a cancellation or a finish owes the processor.
    private static final class Settle {
        String id = "";
        String was = "";
        String reference = "";
        long take;
    }
}
