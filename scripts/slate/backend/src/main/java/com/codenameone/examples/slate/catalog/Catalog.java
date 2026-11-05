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
package com.codenameone.examples.slate.catalog;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.slate.Ids;
import com.codenameone.examples.slate.Text;
import com.codenameone.examples.slate.api.BusinessDto;
import com.codenameone.examples.slate.api.LocationDto;
import com.codenameone.examples.slate.api.PolicyDto;
import com.codenameone.examples.slate.api.ProviderDto;
import com.codenameone.examples.slate.api.ResourceDto;
import com.codenameone.examples.slate.api.ServiceDto;
import com.codenameone.examples.slate.booking.SlotEngine;
import com.codenameone.examples.slate.domain.Business;
import com.codenameone.examples.slate.domain.Location;
import com.codenameone.examples.slate.domain.Provider;
import com.codenameone.examples.slate.domain.ProviderService;
import com.codenameone.examples.slate.domain.Resource;
import com.codenameone.examples.slate.domain.Service;
import com.codenameone.examples.slate.repository.BusinessRepository;
import com.codenameone.examples.slate.repository.LocationRepository;
import com.codenameone.examples.slate.repository.ProviderRepository;
import com.codenameone.examples.slate.repository.ProviderServiceRepository;
import com.codenameone.examples.slate.repository.ResourceRepository;
import com.codenameone.examples.slate.repository.ServiceRepository;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// What the business offers and who offers it: the business and its rules,
/// the locations, the services, the providers and the resources.
///
/// Read by everyone signed in and changed by owners. A provider changes their
/// own description and limits here too, but not which services they offer,
/// which is the owner's to say.
@Component
public class Catalog {
    public static final String MAIN = "main";
    public static final String ANY = "ANY";
    public static final String TIGHT = "TIGHT";

    private final BusinessRepository businesses;
    private final LocationRepository locations;
    private final ServiceRepository services;
    private final ProviderRepository providers;
    private final ProviderServiceRepository offers;
    private final ResourceRepository resources;

    public Catalog(BusinessRepository businesses, LocationRepository locations,
            ServiceRepository services, ProviderRepository providers,
            ProviderServiceRepository offers, ResourceRepository resources) {
        this.businesses = businesses;
        this.locations = locations;
        this.services = services;
        this.providers = providers;
        this.offers = offers;
        this.resources = resources;
    }

    // --------------------------------------------------------------- business

    /// The business row. Answers 503 before the business has been set up,
    /// which on a development profile the demo data does at start-up.
    @Transactional(readOnly = true)
    public Business main() {
        Business row = businesses.findById(MAIN);
        if (row == null) {
            throw new ResponseStatusException(503, "The business has not been set up yet");
        }
        return row;
    }

    @Transactional(readOnly = true)
    public BusinessDto business() {
        return business(main());
    }

    public static BusinessDto business(Business row) {
        String[] words = Verticals.words(row.getVertical());
        return new BusinessDto(row.getName(), row.getVertical(), row.getAbout(),
                row.getTimeZone(), row.getCurrency(), words[1], words[2], words[3], words[4],
                words[5], words[6]);
    }

    @Transactional(readOnly = true)
    public PolicyDto policy() {
        return policy(main());
    }

    public static PolicyDto policy(Business b) {
        return new PolicyDto(b.getStepMinutes(), b.getMinNoticeMinutes(), b.getHorizonDays(),
                b.getHoldMinutes(), b.getCancelHours(), b.getRescheduleHours(),
                b.getDepositPercent(), b.getNoShowFeePercent(), b.getClientGapMinutes(),
                b.getClientPerDay(), b.getMinUsableGapMinutes(), b.getPacking(),
                b.getReminderHours());
    }

    @Transactional(rollbackFor = IOException.class)
    public BusinessDto saveBusiness(BusinessDto dto) {
        if (dto == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        businesses.hold(MAIN);
        Business row = main();
        row.setName(Text.required(dto.name(), 120, "the business's name"));
        row.setAbout(Text.optional(dto.about(), 1000, "the description"));
        if (!Verticals.known(dto.vertical())) {
            throw new ResponseStatusException(400, "That is not a kind of business Slate knows");
        }
        row.setVertical(dto.vertical());
        row.setTimeZone(zone(dto.timeZone()).getId());
        row.setCurrency(currency(dto.currency()));
        row.setUpdatedAt(Instant.now());
        return business(row);
    }

    // tag::slate-policy-validation[]
    /// Checks a set of rules and copies them onto `row`. Answers 400 naming
    /// the first value that cannot be a rule. Shared by saving the rules and
    /// previewing them, so a preview never shows rules that could not be saved.
    public static void apply(PolicyDto p, Business row) {
        if (p == null) {
            throw new ResponseStatusException(400, "No rules to apply");
        }
        int step = grid(p.stepMinutes(), 5, 240, "the time between starts");
        row.setStepMinutes(step);
        row.setMinNoticeMinutes(range(p.minNoticeMinutes(), 0, 60 * 24 * 30, "the notice needed"));
        row.setHorizonDays(range(p.horizonDays(), 1, 366, "how far ahead people can book"));
        row.setHoldMinutes(range(p.holdMinutes(), 1, 60, "how long a slot is held"));
        row.setCancelHours(range(p.cancelHours(), 0, 24 * 14, "the cancellation window"));
        row.setRescheduleHours(range(p.rescheduleHours(), 0, 24 * 14, "the rescheduling window"));
        row.setDepositPercent(range(p.depositPercent(), 0, 100, "the deposit"));
        row.setNoShowFeePercent(range(p.noShowFeePercent(), 0, 100, "the no-show fee"));
        row.setClientGapMinutes(grid(p.clientGapMinutes(), 0, 60 * 24, "the gap between a client's bookings"));
        row.setClientPerDay(range(p.clientPerDay(), 0, 20, "the bookings a client may make a day"));
        row.setMinUsableGapMinutes(grid(p.minUsableGapMinutes(), 0, 240, "the smallest useful gap"));
        String packing = p.packing() == null ? ANY : p.packing();
        if (!ANY.equals(packing) && !TIGHT.equals(packing)) {
            throw new ResponseStatusException(400, "Packing is either ANY or TIGHT");
        }
        row.setPacking(packing);
        row.setReminderHours(range(p.reminderHours(), 0, 24 * 7, "when reminders go out"));
    }
    // end::slate-policy-validation[]

    @Transactional(rollbackFor = IOException.class)
    public PolicyDto savePolicy(PolicyDto dto) {
        businesses.hold(MAIN);
        Business row = main();
        apply(dto, row);
        row.setUpdatedAt(Instant.now());
        return policy(row);
    }

    /// The zone the business counts its days in.
    public ZoneId zone() {
        return ZoneId.of(main().getTimeZone());
    }

    // -------------------------------------------------------------- locations

    @Transactional(readOnly = true)
    public List<LocationDto> locations(boolean activeOnly) {
        List<Location> rows = activeOnly ? locations.findByActiveOrderByNameAsc(true)
                : locations.everyByName();
        List<LocationDto> out = new ArrayList<LocationDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(location(rows.get(iter)));
        }
        return out;
    }

    public static LocationDto location(Location row) {
        return new LocationDto(row.getId(), row.getName(), row.getAddress(), row.getTimeZone(),
                row.isActive());
    }

    /// A location by id. Answers 404 for one that does not exist or is closed.
    @Transactional(readOnly = true)
    public Location openLocation(String id) {
        Location row = id == null || id.length() == 0 ? null : locations.findById(id);
        if (row == null || !row.isActive()) {
            throw new ResponseStatusException(404, "No such location");
        }
        return row;
    }

    @Transactional(rollbackFor = IOException.class)
    public LocationDto saveLocation(LocationDto dto) throws IOException {
        if (dto == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        Location row = dto.id() == null || dto.id().length() == 0 ? null
                : locations.findById(dto.id());
        boolean fresh = row == null;
        if (fresh) {
            row = new Location();
            row.setId(Ids.next());
        }
        row.setName(Text.required(dto.name(), 120, "the location's name"));
        row.setAddress(Text.optional(dto.address(), 255, "the address"));
        String zone = dto.timeZone() == null || dto.timeZone().length() == 0
                ? main().getTimeZone() : dto.timeZone();
        row.setTimeZone(zone(zone).getId());
        row.setActive(dto.active());
        if (fresh) {
            locations.insert(row);
        }
        return location(row);
    }

    // --------------------------------------------------------------- services

    @Transactional(readOnly = true)
    public List<ServiceDto> services(boolean activeOnly) {
        List<Service> rows = activeOnly ? services.findByActiveOrderBySortOrderAsc(true)
                : services.everyInOrder();
        Map<String, List<String>> who = offersByService();
        String currency = main().getCurrency();
        List<ServiceDto> out = new ArrayList<ServiceDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(service(rows.get(iter), currency, who.get(rows.get(iter).getId())));
        }
        return out;
    }

    public static ServiceDto service(Service s, String currency, List<String> who) {
        return new ServiceDto(s.getId(), s.getName(), s.getDescription(), s.getCategory(),
                s.getMinutes(), s.getPriceCents(), currency, s.getBufferBefore(),
                s.getBufferAfter(), s.getCapacity(), s.getProvidersNeeded(), s.getResourceKind(),
                s.isOnline(), s.getMinNoticeMinutes(), s.getDepositPercent(), s.getColor(),
                s.getIntakePrompt(), s.isActive(), who == null ? new ArrayList<String>() : who);
    }

    /// A service by id. Answers 404 for one that does not exist or is not
    /// offered any more.
    @Transactional(readOnly = true)
    public Service offered(String id) {
        Service row = id == null || id.length() == 0 ? null : services.findById(id);
        if (row == null || !row.isActive()) {
            throw new ResponseStatusException(404, "No such service");
        }
        return row;
    }

    /// The providers who offer a service, active ones only, by name.
    @Transactional(readOnly = true)
    public List<Provider> providersOf(String serviceId) {
        List<ProviderService> rows = offers.findByServiceId(serviceId);
        List<Provider> out = new ArrayList<Provider>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Provider p = providers.findById(rows.get(iter).getUsername());
            if (p != null && p.isActive()) {
                out.add(p);
            }
        }
        for (int i = 1; i < out.size(); i++) {
            Provider moving = out.get(i);
            int j = i - 1;
            while (j >= 0 && out.get(j).getUsername().compareTo(moving.getUsername()) > 0) {
                out.set(j + 1, out.get(j));
                j--;
            }
            out.set(j + 1, moving);
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public ServiceDto saveService(ServiceDto dto) throws IOException {
        if (dto == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        Service row = dto.id() == null || dto.id().length() == 0 ? null
                : services.findById(dto.id());
        boolean fresh = row == null;
        if (fresh) {
            row = new Service();
            row.setId(Ids.next());
            row.setSortOrder(services.everyInOrder().size());
        }
        row.setName(Text.required(dto.name(), 80, "the service's name"));
        row.setDescription(Text.optional(dto.description(), 1000, "the description"));
        row.setCategory(Text.optional(dto.category(), 40, "the category"));
        row.setMinutes(grid(dto.minutes(), 5, 12 * 60, "how long it takes"));
        if (dto.priceCents() < 0 || dto.priceCents() > 100000000L) {
            throw new ResponseStatusException(400, "That price cannot be right");
        }
        row.setPriceCents(dto.priceCents());
        row.setBufferBefore(grid(dto.bufferBefore(), 0, 240, "the time before"));
        row.setBufferAfter(grid(dto.bufferAfter(), 0, 240, "the time after"));
        row.setCapacity(range(dto.capacity(), 1, 200, "how many can book one slot"));
        row.setProvidersNeeded(range(dto.providersNeeded(), 1, 5, "how many it takes"));
        if (row.getCapacity() > 1 && row.getProvidersNeeded() > 1) {
            // A group slot is keyed by its one provider, and joining it checks
            // nobody else's time; see SlotEngine.groupKey.
            throw new ResponseStatusException(400, "A group is run by one provider");
        }
        row.setResourceKind(Text.optional(dto.resourceKind(), 40, "what it needs"));
        row.setOnline(dto.online());
        row.setMinNoticeMinutes(dto.minNoticeMinutes() < 0 ? -1
                : range(dto.minNoticeMinutes(), 0, 60 * 24 * 30, "the notice needed"));
        row.setDepositPercent(dto.depositPercent() < 0 ? -1
                : range(dto.depositPercent(), 0, 100, "the deposit"));
        row.setColor(color(dto.color()));
        row.setIntakePrompt(Text.optional(dto.intakePrompt(), 255, "the question asked"));
        row.setActive(dto.active());
        if (fresh) {
            services.insert(row);
        }
        if (dto.providers() != null) {
            offers.deleteByServiceId(row.getId());
            for (int iter = 0; iter < dto.providers().size(); iter++) {
                String who = dto.providers().get(iter);
                if (providers.findById(who) == null) {
                    throw new ResponseStatusException(400, who + " is not a provider");
                }
                offer(who, row.getId());
            }
        }
        return service(row, main().getCurrency(), offersByService().get(row.getId()));
    }

    public void offer(String username, String serviceId) {
        ProviderService link = new ProviderService();
        link.setId(username + "|" + serviceId);
        link.setUsername(username);
        link.setServiceId(serviceId);
        offers.insert(link);
    }

    private Map<String, List<String>> offersByService() {
        Map<String, List<String>> out = new HashMap<String, List<String>>();
        List<ProviderService> all = offers.everyById();
        for (int iter = 0; iter < all.size(); iter++) {
            ProviderService row = all.get(iter);
            List<String> list = out.get(row.getServiceId());
            if (list == null) {
                list = new ArrayList<String>();
                out.put(row.getServiceId(), list);
            }
            list.add(row.getUsername());
        }
        return out;
    }

    // -------------------------------------------------------------- providers

    @Transactional(readOnly = true)
    public List<ProviderDto> providers(boolean activeOnly) {
        List<Provider> rows = activeOnly ? providers.findByActiveOrderByDisplayNameAsc(true)
                : providers.everyByName();
        List<ProviderDto> out = new ArrayList<ProviderDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(provider(rows.get(iter)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public ProviderDto provider(String username) {
        Provider row = providers.findById(username);
        if (row == null) {
            throw new ResponseStatusException(404, "No such provider");
        }
        return provider(row);
    }

    private ProviderDto provider(Provider row) {
        List<ProviderService> mine = offers.findByUsername(row.getUsername());
        List<String> ids = new ArrayList<String>();
        for (int iter = 0; iter < mine.size(); iter++) {
            ids.add(mine.get(iter).getServiceId());
        }
        return new ProviderDto(row.getUsername(), row.getDisplayName(), row.getTitle(),
                row.getBio(), row.getColor(), row.getDefaultLocation(), row.getMaxPerDay(),
                row.getMaxContinuousMinutes(), row.getBreakMinutes(), row.isActive(), ids);
    }

    /// Saves a provider's description and limits. `byOwner` also saves who
    /// they are and which services they offer; a provider saving their own
    /// row changes neither.
    @Transactional(rollbackFor = IOException.class)
    public ProviderDto saveProvider(ProviderDto dto, boolean byOwner) {
        if (dto == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        Provider row = providers.findById(dto.username());
        if (row == null) {
            throw new ResponseStatusException(404,
                    "Give the account the provider role first, from Accounts");
        }
        providers.hold(row.getUsername());
        row.setDisplayName(Text.required(dto.displayName(), 120, "the name clients see"));
        row.setTitle(Text.optional(dto.title(), 80, "the title"));
        row.setBio(Text.optional(dto.bio(), 1000, "the description"));
        row.setColor(color(dto.color()));
        if (dto.defaultLocation() != null && dto.defaultLocation().length() > 0) {
            openLocation(dto.defaultLocation());
            row.setDefaultLocation(dto.defaultLocation());
        }
        row.setMaxPerDay(range(dto.maxPerDay(), 0, 50, "the most appointments a day"));
        row.setMaxContinuousMinutes(grid(dto.maxContinuousMinutes(), 0, 12 * 60,
                "the longest run without a break"));
        row.setBreakMinutes(grid(dto.breakMinutes(), 0, 240, "the break"));
        if (row.getMaxContinuousMinutes() > 0 && row.getBreakMinutes() == 0) {
            throw new ResponseStatusException(400, "Say how long the break is");
        }
        if (byOwner) {
            row.setActive(dto.active());
            if (dto.services() != null) {
                offers.deleteByUsername(row.getUsername());
                for (int iter = 0; iter < dto.services().size(); iter++) {
                    String id = dto.services().get(iter);
                    if (services.findById(id) == null) {
                        throw new ResponseStatusException(400, "No such service");
                    }
                    offer(row.getUsername(), id);
                }
            }
        }
        return provider(row);
    }

    /// `dto` as saved by `username` themselves: whatever name it carries, it
    /// is their own row that is saved.
    public static ProviderDto asSelf(String username, ProviderDto dto) {
        if (dto == null) {
            return null;
        }
        return new ProviderDto(username, dto.displayName(), dto.title(), dto.bio(), dto.color(),
                dto.defaultLocation(), dto.maxPerDay(), dto.maxContinuousMinutes(),
                dto.breakMinutes(), dto.active(), dto.services());
    }

    /// Makes a provider row for an account given the role, or brings an old
    /// one back. Kept when the role is withdrawn, inactive, so past
    /// appointments still name who they were with.
    public void providerRow(String username, String displayName, boolean active)
            throws IOException {
        Provider row = providers.findById(username);
        if (row == null) {
            if (!active) {
                return;
            }
            row = new Provider();
            row.setUsername(username);
            row.setDisplayName(displayName);
            row.setTitle(Verticals.words(main().getVertical())[1]);
            row.setColor("#5c6bc0");
            row.setFeedToken(Ids.next());
            row.setActive(true);
            row.setCreatedAt(Instant.now());
            providers.insert(row);
            return;
        }
        row.setActive(active);
    }

    // -------------------------------------------------------------- resources

    @Transactional(readOnly = true)
    public List<ResourceDto> resources() {
        List<Resource> rows = resources.everyByLocation();
        List<ResourceDto> out = new ArrayList<ResourceDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Resource r = rows.get(iter);
            out.add(new ResourceDto(r.getId(), r.getLocationId(), r.getKind(), r.getName(),
                    r.isActive()));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public ResourceDto saveResource(ResourceDto dto) throws IOException {
        if (dto == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        Resource row = dto.id() == null || dto.id().length() == 0 ? null
                : resources.findById(dto.id());
        boolean fresh = row == null;
        if (fresh) {
            row = new Resource();
            row.setId(Ids.next());
        }
        openLocation(dto.locationId());
        row.setLocationId(dto.locationId());
        row.setKind(Text.required(dto.kind(), 40, "what kind of thing it is"));
        row.setName(Text.required(dto.name(), 80, "its name"));
        row.setActive(dto.active());
        if (fresh) {
            resources.insert(row);
        }
        return new ResourceDto(row.getId(), row.getLocationId(), row.getKind(), row.getName(),
                row.isActive());
    }

    // ------------------------------------------------------------- the checks

    /// A whole number of minutes on the slot grid, between `min` and `max`.
    public static int grid(int value, int min, int max, String what) {
        int checked = range(value, min, max, what);
        if (checked % SlotEngine.CELL_MINUTES != 0) {
            throw new ResponseStatusException(400, "Make " + what + " a multiple of "
                    + SlotEngine.CELL_MINUTES + " minutes");
        }
        return checked;
    }

    public static int range(int value, int min, int max, String what) {
        if (value < min || value > max) {
            throw new ResponseStatusException(400, "Keep " + what + " between " + min + " and "
                    + max);
        }
        return value;
    }

    public static ZoneId zone(String id) {
        try {
            return ZoneId.of(id == null ? "" : id.trim());
        } catch (DateTimeException bad) {
            throw new ResponseStatusException(400, "That is not a time zone");
        }
    }

    private static String currency(String code) {
        String text = code == null ? "" : code.trim();
        if (text.length() != 3) {
            throw new ResponseStatusException(400, "A currency is three letters, such as USD");
        }
        for (int iter = 0; iter < 3; iter++) {
            char c = text.charAt(iter);
            if (c < 'A' || c > 'Z') {
                throw new ResponseStatusException(400, "A currency is three capital letters");
            }
        }
        return text;
    }

    private static String color(String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() == 0) {
            return "#5c6bc0";
        }
        if (text.length() != 7 || text.charAt(0) != '#') {
            throw new ResponseStatusException(400, "A colour is written #rrggbb");
        }
        for (int iter = 1; iter < 7; iter++) {
            char c = text.charAt(iter);
            if (!(c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F')) {
                throw new ResponseStatusException(400, "A colour is written #rrggbb");
            }
        }
        return text;
    }
}
