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
package com.codenameone.examples.bazaar.trade;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Days;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.BulkOrderResultDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.CompanyBuyerDto;
import com.codenameone.examples.bazaar.api.CompanyDto;
import com.codenameone.examples.bazaar.api.CompanySiteDto;
import com.codenameone.examples.bazaar.api.CsvErrorDto;
import com.codenameone.examples.bazaar.api.NetTermDto;
import com.codenameone.examples.bazaar.api.PriceListDto;
import com.codenameone.examples.bazaar.api.PriceListEntryDto;
import com.codenameone.examples.bazaar.api.TradeQuoteDto;
import com.codenameone.examples.bazaar.api.TradeQuoteLineDto;
import com.codenameone.examples.bazaar.api.TradeQuoteRequestDto;
import com.codenameone.examples.bazaar.api.VatCheckDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.Company;
import com.codenameone.examples.bazaar.domain.CompanyBuyer;
import com.codenameone.examples.bazaar.domain.CompanySite;
import com.codenameone.examples.bazaar.domain.NetTerm;
import com.codenameone.examples.bazaar.domain.PriceList;
import com.codenameone.examples.bazaar.domain.PriceListEntry;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.TradeQuote;
import com.codenameone.examples.bazaar.domain.TradeQuoteLine;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.marketing.Shoppers;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.CompanyBuyerRepository;
import com.codenameone.examples.bazaar.repository.CompanyRepository;
import com.codenameone.examples.bazaar.repository.CompanySiteRepository;
import com.codenameone.examples.bazaar.repository.NetTermRepository;
import com.codenameone.examples.bazaar.repository.PriceListEntryRepository;
import com.codenameone.examples.bazaar.repository.PriceListRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.TradeQuoteLineRepository;
import com.codenameone.examples.bazaar.repository.TradeQuoteRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// B2B selling: companies with sites and buyers, price lists with quantity
/// rules, quotes moving through a pipeline and orders on net terms.
///
/// **Prices.** A company may have a price list. A variant with an entry on
/// it sells at the entry's fixed price; any other variant at the list's
/// percentage off. An entry also carries the variant's quantity rules -- a
/// minimum, a maximum and a step (cases of 6, say) -- that every quote line
/// is held to.
///
/// **Quotes.** A quote (a draft order) is DRAFT, SENT, ACCEPTED, ORDERED or
/// DECLINED; see [#allowed] for who may move it where. Every move is a
/// conditional update on the status it starts from, so two people moving
/// one card at once cannot both win, and a move the pipeline does not allow
/// answers 409. Ordering an accepted quote places the order at the quote's
/// prices with the stock reserved.
///
/// **Net terms.** A company on net 15, 30 or 60 gets its orders placed
/// unpaid, with a due date kept beside the order in `net_term`; the invoice
/// shows it, and the merchant marks it paid when the transfer arrives.
@Component
public class Trade {
    public static final String DRAFT = "DRAFT";
    public static final String SENT = "SENT";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String ORDERED = "ORDERED";
    public static final String DECLINED = "DECLINED";
    public static final String OPEN = "OPEN";
    public static final String PAID = "PAID";
    /// The order channel quotes are ordered on.
    public static final String CHANNEL = "B2B";
    static final long DAY = 24L * 3600 * 1000;
    static final int MAX_CSV_ROWS = 500;
    static final int MAX_LINE_QTY = 10000;

    private final CompanyRepository companies;
    private final CompanySiteRepository sites;
    private final CompanyBuyerRepository buyers;
    private final PriceListRepository priceLists;
    private final PriceListEntryRepository entries;
    private final TradeQuoteRepository quotes;
    private final TradeQuoteLineRepository quoteLines;
    private final NetTermRepository netTerms;
    private final VariantRepository variants;
    private final ProductRepository products;
    private final StoreOrderRepository orderRows;
    private final StoreAccess access;
    private final Shoppers shoppers;
    private final Orders orders;
    private final LineOrders lineOrders;

    public Trade(CompanyRepository companies, CompanySiteRepository sites,
            CompanyBuyerRepository buyers, PriceListRepository priceLists,
            PriceListEntryRepository entries, TradeQuoteRepository quotes,
            TradeQuoteLineRepository quoteLines, NetTermRepository netTerms,
            VariantRepository variants, ProductRepository products,
            StoreOrderRepository orderRows, StoreAccess access, Shoppers shoppers, Orders orders,
            LineOrders lineOrders) {
        this.companies = companies;
        this.sites = sites;
        this.buyers = buyers;
        this.priceLists = priceLists;
        this.entries = entries;
        this.quotes = quotes;
        this.quoteLines = quoteLines;
        this.netTerms = netTerms;
        this.variants = variants;
        this.products = products;
        this.orderRows = orderRows;
        this.access = access;
        this.shoppers = shoppers;
        this.orders = orders;
        this.lineOrders = lineOrders;
    }

    // ------------------------------------------------------------ companies

    @Transactional(readOnly = true)
    public List<CompanyDto> companies(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<Company> rows = companies.findByStoreIdOrderByCreatedAtAsc(storeId);
        List<CompanyDto> out = new ArrayList<CompanyDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter)));
        }
        return out;
    }

    static CompanyDto dto(Company c) {
        return new CompanyDto(c.getId(), c.getName(), c.getVatId(), c.getTermsDays(),
                c.getPriceListId());
    }

    @Transactional(rollbackFor = IOException.class)
    public CompanyDto saveCompany(String username, String storeId, String companyId,
            CompanyDto in) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return writeCompany(storeId, companyId, in);
    }

    /// Creates the company (`companyId` null) or updates it. A VAT id, when
    /// given, has to have a valid EU format ([VatIds]).
    public CompanyDto writeCompany(String storeId, String companyId, CompanyDto in)
            throws IOException {
        if (in == null || Codes.cut(in.name(), 120).length() == 0) {
            throw new ResponseStatusException(400, "Name the company");
        }
        String vat = VatIds.normalize(in.vatId());
        if (vat.length() > 0) {
            VatCheckDto check = VatIds.check(vat);
            if (!check.valid()) {
                throw new ResponseStatusException(400, check.message());
            }
        }
        int terms = in.termsDays();
        if (terms != 0 && terms != 15 && terms != 30 && terms != 60) {
            throw new ResponseStatusException(400, "Terms are 0, 15, 30 or 60 days");
        }
        String listId = in.priceListId() == null ? "" : in.priceListId();
        if (listId.length() > 0) {
            priceList(storeId, listId);
        }
        Company c = companyId == null ? null : company(storeId, companyId);
        boolean fresh = c == null;
        if (fresh) {
            c = new Company();
            c.setId(Ids.next());
            c.setStoreId(storeId);
            c.setCreatedAt(Instant.now());
        }
        c.setName(Codes.cut(in.name(), 120));
        c.setVatId(vat);
        c.setTermsDays(terms);
        c.setPriceListId(listId);
        if (fresh) {
            companies.insert(c);
        } else {
            companies.save(c);
        }
        return dto(c);
    }

    Company company(String storeId, String companyId) {
        Company c = companyId == null ? null : companies.findById(companyId);
        if (c == null || !storeId.equals(c.getStoreId())) {
            throw new ResponseStatusException(404, "No such company");
        }
        return c;
    }

    @Transactional(readOnly = true)
    public List<CompanySiteDto> sites(String username, String storeId, String companyId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return siteDtos(company(storeId, companyId).getId());
    }

    private List<CompanySiteDto> siteDtos(String companyId) {
        List<CompanySite> rows = sites.findByCompanyId(companyId);
        List<CompanySiteDto> out = new ArrayList<CompanySiteDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            CompanySite s = rows.get(iter);
            out.add(new CompanySiteDto(s.getId(), s.getName(), s.getLine1(), s.getCity(),
                    s.getPostal(), s.getCountry()));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public List<CompanySiteDto> addSite(String username, String storeId, String companyId,
            CompanySiteDto in) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Company c = company(storeId, companyId);
        if (in == null || Codes.cut(in.name(), 120).length() == 0) {
            throw new ResponseStatusException(400, "Name the location");
        }
        CompanySite s = new CompanySite();
        s.setId(Ids.next());
        s.setStoreId(storeId);
        s.setCompanyId(c.getId());
        s.setName(Codes.cut(in.name(), 120));
        s.setLine1(Codes.cut(in.line1(), 200));
        s.setCity(Codes.cut(in.city(), 120));
        s.setPostal(Codes.cut(in.postal(), 20));
        s.setCountry(Codes.cut(Codes.normalize(in.country()), 2));
        sites.insert(s);
        return siteDtos(c.getId());
    }

    @Transactional(readOnly = true)
    public List<CompanyBuyerDto> buyers(String username, String storeId, String companyId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return buyerDtos(company(storeId, companyId).getId());
    }

    private List<CompanyBuyerDto> buyerDtos(String companyId) {
        List<CompanyBuyer> rows = buyers.findByCompanyId(companyId);
        List<CompanyBuyerDto> out = new ArrayList<CompanyBuyerDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            CompanyBuyer b = rows.get(iter);
            out.add(new CompanyBuyerDto(b.getId(), b.getUsername(), b.getSiteId()));
        }
        return out;
    }

    /// Lets a shopper buy for the company. A shopper buys for one company
    /// per store at most.
    @Transactional(rollbackFor = IOException.class)
    public List<CompanyBuyerDto> addBuyer(String username, String storeId, String companyId,
            CompanyBuyerDto in) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Company c = company(storeId, companyId);
        String who = in == null ? "" : Codes.cut(in.username(), 190);
        if (who.length() == 0) {
            throw new ResponseStatusException(400, "Name the buyer's account");
        }
        if (!buyers.findByStoreIdAndUsername(storeId, who).isEmpty()) {
            throw new ResponseStatusException(409, "That shopper already buys for a company here");
        }
        String siteId = in.siteId() == null ? "" : in.siteId();
        if (siteId.length() > 0) {
            CompanySite s = sites.findById(siteId);
            if (s == null || !c.getId().equals(s.getCompanyId())) {
                throw new ResponseStatusException(404, "No such location");
            }
        }
        CompanyBuyer b = new CompanyBuyer();
        b.setId(Ids.next());
        b.setStoreId(storeId);
        b.setCompanyId(c.getId());
        b.setUsername(who);
        b.setSiteId(siteId);
        b.setCreatedAt(Instant.now());
        buyers.insert(b);
        return buyerDtos(c.getId());
    }

    @Transactional(rollbackFor = IOException.class)
    public List<CompanyBuyerDto> removeBuyer(String username, String storeId, String companyId,
            String buyerId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Company c = company(storeId, companyId);
        CompanyBuyer b = buyerId == null ? null : buyers.findById(buyerId);
        if (b == null || !c.getId().equals(b.getCompanyId())) {
            throw new ResponseStatusException(404, "No such buyer");
        }
        buyers.delete(b);
        return buyerDtos(c.getId());
    }

    // ---------------------------------------------------------- price lists

    @Transactional(readOnly = true)
    public List<PriceListDto> priceLists(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<PriceList> rows = priceLists.findByStoreIdOrderByCreatedAtAsc(storeId);
        List<PriceListDto> out = new ArrayList<PriceListDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            PriceList p = rows.get(iter);
            out.add(new PriceListDto(p.getId(), p.getName(), p.getPercentOffBps()));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public PriceListDto savePriceList(String username, String storeId, String listId,
            PriceListDto in) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return writePriceList(storeId, listId, in);
    }

    public PriceListDto writePriceList(String storeId, String listId, PriceListDto in)
            throws IOException {
        if (in == null || Codes.cut(in.name(), 120).length() == 0) {
            throw new ResponseStatusException(400, "Name the price list");
        }
        if (in.percentOffBps() < 0 || in.percentOffBps() > 9000) {
            throw new ResponseStatusException(400, "The discount is from 0 to 90%");
        }
        PriceList p = listId == null ? null : priceList(storeId, listId);
        boolean fresh = p == null;
        if (fresh) {
            p = new PriceList();
            p.setId(Ids.next());
            p.setStoreId(storeId);
            p.setCreatedAt(Instant.now());
        }
        p.setName(Codes.cut(in.name(), 120));
        p.setPercentOffBps(in.percentOffBps());
        if (fresh) {
            priceLists.insert(p);
        } else {
            priceLists.save(p);
        }
        return new PriceListDto(p.getId(), p.getName(), p.getPercentOffBps());
    }

    PriceList priceList(String storeId, String listId) {
        PriceList p = listId == null ? null : priceLists.findById(listId);
        if (p == null || !storeId.equals(p.getStoreId())) {
            throw new ResponseStatusException(404, "No such price list");
        }
        return p;
    }

    @Transactional(readOnly = true)
    public List<PriceListEntryDto> entries(String username, String storeId, String listId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return entryDtos(priceList(storeId, listId).getId());
    }

    private List<PriceListEntryDto> entryDtos(String listId) {
        List<PriceListEntry> rows = entries.findByPriceListId(listId);
        List<PriceListEntryDto> out = new ArrayList<PriceListEntryDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            PriceListEntry e = rows.get(iter);
            out.add(new PriceListEntryDto(e.getId(), e.getVariantId(), e.getFixedCents(),
                    e.getMinQty(), e.getMaxQty(), e.getStepQty()));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public List<PriceListEntryDto> putEntry(String username, String storeId, String listId,
            PriceListEntryDto in) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return writeEntry(storeId, priceList(storeId, listId).getId(), in);
    }

    /// Sets a variant's fixed price and quantity rules on a list, replacing
    /// the entry it had.
    public List<PriceListEntryDto> writeEntry(String storeId, String listId, PriceListEntryDto in)
            throws IOException {
        if (in == null) {
            throw new ResponseStatusException(400, "Choose a variant");
        }
        Variant v = in.variantId() == null ? null : variants.findById(in.variantId());
        if (v == null || !storeId.equals(v.getStoreId())) {
            throw new ResponseStatusException(404, "No such variant");
        }
        if (in.fixedCents() < 0 || in.fixedCents() > 100000000L || in.minQty() < 0
                || in.maxQty() < 0 || in.stepQty() < 0 || in.minQty() > MAX_LINE_QTY
                || in.maxQty() > MAX_LINE_QTY || in.stepQty() > MAX_LINE_QTY
                || (in.maxQty() > 0 && in.maxQty() < in.minQty())) {
            throw new ResponseStatusException(400, "Those numbers are out of range");
        }
        PriceListEntry e = entry(listId, v.getId());
        boolean fresh = e == null;
        if (fresh) {
            e = new PriceListEntry();
            e.setId(Ids.next());
            e.setStoreId(storeId);
            e.setPriceListId(listId);
            e.setVariantId(v.getId());
        }
        e.setFixedCents(in.fixedCents());
        e.setMinQty(in.minQty());
        e.setMaxQty(in.maxQty());
        e.setStepQty(in.stepQty());
        if (fresh) {
            entries.insert(e);
        } else {
            entries.save(e);
        }
        return entryDtos(listId);
    }

    private PriceListEntry entry(String listId, String variantId) {
        if (listId == null || listId.length() == 0) {
            return null;
        }
        List<PriceListEntry> rows = entries.findByPriceListId(listId);
        for (int iter = 0; iter < rows.size(); iter++) {
            if (rows.get(iter).getVariantId().equals(variantId)) {
                return rows.get(iter);
            }
        }
        return null;
    }

    @Transactional(rollbackFor = IOException.class)
    public List<PriceListEntryDto> removeEntry(String username, String storeId, String listId,
            String entryId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        PriceList p = priceList(storeId, listId);
        PriceListEntry e = entryId == null ? null : entries.findById(entryId);
        if (e == null || !p.getId().equals(e.getPriceListId())) {
            throw new ResponseStatusException(404, "No such entry");
        }
        entries.delete(e);
        return entryDtos(p.getId());
    }

    // -------------------------------------------------------------- pricing

    /// What a quantity rule says about `qty`, or null when it is allowed.
    static String ruleProblem(PriceListEntry e, int qty) {
        if (qty < 1 || qty > MAX_LINE_QTY) {
            return "Quantity must be from 1 to " + MAX_LINE_QTY;
        }
        if (e == null) {
            return null;
        }
        if (e.getMinQty() > 0 && qty < e.getMinQty()) {
            return "At least " + e.getMinQty() + " must be ordered";
        }
        if (e.getMaxQty() > 0 && qty > e.getMaxQty()) {
            return "At most " + e.getMaxQty() + " can be ordered";
        }
        if (e.getStepQty() > 1 && qty % e.getStepQty() != 0) {
            return "Order in multiples of " + e.getStepQty();
        }
        return null;
    }

    /// The company's price of one unit of `v`.
    static long unitPrice(Variant v, PriceList list, PriceListEntry e) {
        long price = v.getPriceCents();
        if (e != null && e.getFixedCents() > 0) {
            return e.getFixedCents();
        }
        if (list == null) {
            return price;
        }
        return price - price * list.getPercentOffBps() / 10000;
    }

    /// A priced line, or a problem with it.
    static final class Priced {
        TradeQuoteLine line;
        String problem;
    }

    private Priced price(String storeId, Company c, PriceList list, Variant v, int qty)
            throws IOException {
        Priced out = new Priced();
        if (v == null || !storeId.equals(v.getStoreId())) {
            out.problem = "No such item";
            return out;
        }
        Product p = products.findById(v.getProductId());
        if (p == null || !"ACTIVE".equals(p.getStatus())) {
            out.problem = "That item is not sold";
            return out;
        }
        PriceListEntry e = list == null ? null : entry(list.getId(), v.getId());
        out.problem = ruleProblem(e, qty);
        if (out.problem != null) {
            return out;
        }
        TradeQuoteLine l = new TradeQuoteLine();
        l.setId(Ids.next());
        l.setStoreId(storeId);
        l.setVariantId(v.getId());
        l.setSku(Codes.cut(v.getSku(), 64));
        String title = p.getTitle() + (v.getTitle().length() > 0 ? " - " + v.getTitle() : "");
        l.setTitle(Codes.cut(title, 200));
        l.setQuantity(qty);
        l.setListCents(v.getPriceCents());
        l.setUnitCents(unitPrice(v, list, e));
        out.line = l;
        return out;
    }

    private PriceList listOf(Company c) {
        return c.getPriceListId().length() == 0 ? null : priceLists.findById(c.getPriceListId());
    }

    // --------------------------------------------------------------- quotes

    @Transactional(readOnly = true)
    public List<TradeQuoteDto> quotes(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<TradeQuote> rows = quotes.findByStoreIdOrderByCreatedAtDesc(storeId);
        List<Company> cs = companies.findByStoreIdOrderByCreatedAtAsc(storeId);
        List<TradeQuoteDto> out = new ArrayList<TradeQuoteDto>();
        for (int iter = 0; iter < rows.size() && iter < 500; iter++) {
            TradeQuote q = rows.get(iter);
            String name = "";
            for (int c = 0; c < cs.size(); c++) {
                if (cs.get(c).getId().equals(q.getCompanyId())) {
                    name = cs.get(c).getName();
                }
            }
            out.add(dto(q, name, null));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public TradeQuoteDto quote(String username, String storeId, String quoteId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return full(inStore(storeId, quoteId));
    }

    TradeQuote inStore(String storeId, String quoteId) {
        TradeQuote q = quoteId == null ? null : quotes.findById(quoteId);
        if (q == null || !storeId.equals(q.getStoreId())) {
            throw new ResponseStatusException(404, "No such quote");
        }
        return q;
    }

    private TradeQuoteDto full(TradeQuote q) {
        Company c = companies.findById(q.getCompanyId());
        return dto(q, c == null ? "" : c.getName(), quoteLines.findByQuoteId(q.getId()));
    }

    static TradeQuoteDto dto(TradeQuote q, String companyName, List<TradeQuoteLine> ls) {
        List<TradeQuoteLineDto> lines = new ArrayList<TradeQuoteLineDto>();
        for (int iter = 0; ls != null && iter < ls.size(); iter++) {
            TradeQuoteLine l = ls.get(iter);
            lines.add(new TradeQuoteLineDto(l.getVariantId(), l.getSku(), l.getTitle(),
                    l.getQuantity(), l.getListCents(), l.getUnitCents()));
        }
        return new TradeQuoteDto(q.getId(), q.getCompanyId(), companyName, q.getBuyerUsername(),
                q.getStatus(), q.getNote(), q.getSubtotalCents(), q.getDiscountCents(),
                q.getTotalCents(), q.getCurrency(), q.getOrderId(), q.getUpdatedAt().toEpochMilli(),
                lines);
    }

    /// A merchant's draft quote for a company.
    public TradeQuoteDto createQuote(String username, String storeId, TradeQuoteRequestDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (in == null) {
            throw new ResponseStatusException(400, "Choose a company");
        }
        return draft(storeId, company(storeId, in.companyId()), "", in);
    }

    /// Prices `in`'s lines for the company and writes a DRAFT quote. Any line
    /// that breaks a rule answers 400 naming it.
    TradeQuoteDto draft(String storeId, Company c, String buyer, TradeQuoteRequestDto in)
            throws IOException {
        if (in.lines() == null || in.lines().isEmpty() || in.lines().size() > MAX_CSV_ROWS) {
            throw new ResponseStatusException(400, "A quote needs from 1 to " + MAX_CSV_ROWS
                    + " lines");
        }
        PriceList list = listOf(c);
        List<TradeQuoteLine> ls = new ArrayList<TradeQuoteLine>();
        for (int iter = 0; iter < in.lines().size(); iter++) {
            CartLineChangeDto want = in.lines().get(iter);
            Variant v = want == null || want.variantId() == null ? null
                    : variants.findById(want.variantId());
            Priced p = price(storeId, c, list, v, want == null ? 0 : want.quantity());
            if (p.problem != null) {
                throw new ResponseStatusException(400, "Line " + (iter + 1) + ": " + p.problem);
            }
            ls.add(p.line);
        }
        return write(storeId, c, buyer, Codes.cut(in.note(), 1000), ls);
    }

    @Transactional(rollbackFor = IOException.class)
    TradeQuoteDto write(String storeId, Company c, String buyer, String note,
            List<TradeQuoteLine> ls) throws IOException {
        Store s = shoppers.store(storeId);
        TradeQuote q = new TradeQuote();
        q.setId(Ids.next());
        q.setStoreId(storeId);
        q.setCompanyId(c.getId());
        q.setBuyerUsername(buyer);
        q.setStatus(DRAFT);
        q.setNote(note);
        long subtotal = 0;
        long total = 0;
        for (int iter = 0; iter < ls.size(); iter++) {
            TradeQuoteLine l = ls.get(iter);
            l.setQuoteId(q.getId());
            subtotal += l.getListCents() * l.getQuantity();
            total += l.getUnitCents() * l.getQuantity();
        }
        q.setSubtotalCents(subtotal);
        q.setDiscountCents(subtotal - total);
        q.setTotalCents(total);
        q.setCurrency(s.getCurrency());
        q.setCreatedAt(Instant.now());
        q.setUpdatedAt(Instant.now());
        quotes.insert(q);
        for (int iter = 0; iter < ls.size(); iter++) {
            quoteLines.insert(ls.get(iter));
        }
        return dto(q, c.getName(), ls);
    }

    /// Whether the pipeline lets the merchant (or a buyer) move a quote from
    /// `from` to `to`. The merchant sends a draft, takes a sent one back to
    /// revise it, declines it, and orders an accepted one; the buyer accepts
    /// or declines what was sent and orders what they accepted.
    public static boolean allowed(String from, String to, boolean merchant) {
        if (merchant) {
            return (DRAFT.equals(from) && SENT.equals(to))
                    || (SENT.equals(from) && DRAFT.equals(to))
                    || ((DRAFT.equals(from) || SENT.equals(from)) && DECLINED.equals(to))
                    || (ACCEPTED.equals(from) && ORDERED.equals(to));
        }
        return (SENT.equals(from) && (ACCEPTED.equals(to) || DECLINED.equals(to)))
                || (ACCEPTED.equals(from) && ORDERED.equals(to));
    }

    public TradeQuoteDto moveQuote(String username, String storeId, String quoteId, String to)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return move(inStore(storeId, quoteId), to, true, username);
    }

    TradeQuoteDto move(TradeQuote q, String typed, boolean merchant, String actor)
            throws IOException {
        String to = Codes.normalize(typed);
        if (!DRAFT.equals(to) && !SENT.equals(to) && !ACCEPTED.equals(to) && !ORDERED.equals(to)
                && !DECLINED.equals(to)) {
            throw new ResponseStatusException(400, "No such quote status");
        }
        String from = q.getStatus();
        if (!allowed(from, to, merchant)) {
            throw new ResponseStatusException(409, "A " + from + " quote cannot move to " + to);
        }
        if (quotes.move(q.getId(), from, to, Instant.now()) != 1) {
            throw new ResponseStatusException(409, "The quote changed meanwhile; reload it");
        }
        if (ORDERED.equals(to)) {
            try {
                order(q, actor);
            } catch (IOException | RuntimeException failed) {
                quotes.move(q.getId(), ORDERED, ACCEPTED, Instant.now());
                throw failed;
            }
        }
        return full(quotes.findById(q.getId()));
    }

    /// Places the order of an accepted quote at its prices. On net terms it
    /// is left unpaid with a due date; otherwise it waits for payment like
    /// any order placed by hand.
    private void order(TradeQuote q, String actor) throws IOException {
        Company c = companies.findById(q.getCompanyId());
        List<TradeQuoteLine> ls = quoteLines.findByQuoteId(q.getId());
        List<LineOrders.Item> items = new ArrayList<LineOrders.Item>();
        for (int iter = 0; iter < ls.size(); iter++) {
            TradeQuoteLine l = ls.get(iter);
            items.add(new LineOrders.Item(l.getVariantId(), l.getQuantity(), l.getListCents(),
                    l.getUnitCents()));
        }
        LineOrders.Buyer b = new LineOrders.Buyer();
        b.username = q.getBuyerUsername();
        b.email = q.getBuyerUsername().indexOf('@') > 0 ? q.getBuyerUsername() : "";
        b.shipName = c.getName();
        CompanyBuyer buyer = q.getBuyerUsername().length() == 0 ? null
                : first(buyers.findByStoreIdAndUsername(q.getStoreId(), q.getBuyerUsername()));
        CompanySite site = buyer == null || buyer.getSiteId().length() == 0 ? null
                : sites.findById(buyer.getSiteId());
        if (site != null) {
            b.shipLine1 = site.getLine1();
            b.shipCity = site.getCity();
            b.shipPostal = site.getPostal();
            b.shipCountry = site.getCountry();
        }
        StoreOrder order = lineOrders.place(q.getStoreId(), CHANNEL, b, items, "quote-"
                + q.getId().substring(0, 26), actor, "Ordered from quote");
        ordered(q.getId(), order, c, actor);
    }

    @Transactional(rollbackFor = IOException.class)
    void ordered(String quoteId, StoreOrder order, Company c, String actor) throws IOException {
        TradeQuote q = quotes.findById(quoteId);
        q.setOrderId(order.getId());
        q.setUpdatedAt(Instant.now());
        quotes.save(q);
        terms(order, c, actor);
    }

    /// Keeps the due date of an order placed for a company on net terms.
    @Transactional(rollbackFor = IOException.class)
    void terms(StoreOrder order, Company c, String actor) throws IOException {
        if (c.getTermsDays() > 0) {
            NetTerm t = new NetTerm();
            t.setId(order.getId());
            t.setStoreId(order.getStoreId());
            t.setCompanyId(c.getId());
            t.setTermsDays(c.getTermsDays());
            t.setDueAt(order.getCreatedAt().plusMillis(c.getTermsDays() * DAY));
            t.setStatus(OPEN);
            netTerms.insert(t);
            orders.event(order.getId(), "NET_TERMS", "Net " + c.getTermsDays() + ", due "
                    + Days.iso(t.getDueAt().toEpochMilli()), actor);
        }
    }

    private static <T> T first(List<T> rows) {
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    // ------------------------------------------------------------ net terms

    @Transactional(readOnly = true)
    public List<NetTermDto> netTerms(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return netTermDtos(storeId, null);
    }

    private List<NetTermDto> netTermDtos(String storeId, String companyId) {
        List<NetTerm> rows = netTerms.findByStoreIdOrderByDueAtAsc(storeId);
        List<NetTermDto> out = new ArrayList<NetTermDto>();
        long now = System.currentTimeMillis();
        for (int iter = 0; iter < rows.size() && out.size() < 500; iter++) {
            NetTerm t = rows.get(iter);
            if (companyId == null || companyId.equals(t.getCompanyId())) {
                out.add(dto(t, orderRows.findById(t.getId()), now));
            }
        }
        return out;
    }

    static NetTermDto dto(NetTerm t, StoreOrder o, long now) {
        return new NetTermDto(t.getId(), o == null ? 0 : o.getNumber(), t.getCompanyId(),
                t.getTermsDays(), o == null ? 0 : o.getTotalCents(), t.getDueAt().toEpochMilli(),
                t.getPaidAt().toEpochMilli(), t.getStatus(),
                OPEN.equals(t.getStatus()) && t.getDueAt().toEpochMilli() < now);
    }

    NetTerm netTerm(String storeId, String orderId) {
        NetTerm t = orderId == null ? null : netTerms.findById(orderId);
        if (t == null || !storeId.equals(t.getStoreId())) {
            throw new ResponseStatusException(404, "No such net-terms order");
        }
        return t;
    }

    /// Records the transfer for a net-terms order: the order is marked paid
    /// ([Orders#markPaid]) and the due date settled.
    public NetTermDto markPaid(String username, String storeId, String orderId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        NetTerm t = netTerm(storeId, orderId);
        if (PAID.equals(t.getStatus())) {
            throw new ResponseStatusException(409, "That order is already paid");
        }
        orders.markPaid(username, storeId, orderId);
        return settled(orderId);
    }

    @Transactional(rollbackFor = IOException.class)
    NetTermDto settled(String orderId) {
        NetTerm t = netTerms.findById(orderId);
        t.setStatus(PAID);
        t.setPaidAt(Instant.now());
        netTerms.save(t);
        return dto(t, orderRows.findById(orderId), System.currentTimeMillis());
    }

    /// The due line printed on a net-terms invoice, or null for an order not
    /// on terms.
    public String dueLine(String storeId, String orderId) {
        NetTerm t = orderId == null ? null : netTerms.findById(orderId);
        if (t == null || !storeId.equals(t.getStoreId())) {
            return null;
        }
        return "Net " + t.getTermsDays() + ", due " + Days.iso(t.getDueAt().toEpochMilli());
    }

    public VatCheckDto checkVat(String username, String storeId, VatCheckDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return VatIds.check(in == null ? "" : in.vatId());
    }

    // ---------------------------------------------------------------- buyer

    /// The caller's place as a buyer for a company of the store; 403 for a
    /// shopper who buys for none.
    public CompanyBuyer buyer(String username, String storeId) {
        shoppers.store(storeId);
        if (username == null) {
            throw new ResponseStatusException(401, "Sign in first");
        }
        CompanyBuyer b = first(buyers.findByStoreIdAndUsername(storeId, username));
        if (b == null) {
            throw new ResponseStatusException(403, "You do not buy for a company here");
        }
        return b;
    }

    @Transactional(readOnly = true)
    public List<TradeQuoteDto> myQuotes(String username, String storeId) {
        CompanyBuyer b = buyer(username, storeId);
        Company c = companies.findById(b.getCompanyId());
        List<TradeQuote> rows = quotes.findByCompanyIdOrderByCreatedAtDesc(b.getCompanyId());
        List<TradeQuoteDto> out = new ArrayList<TradeQuoteDto>();
        for (int iter = 0; iter < rows.size() && iter < 200; iter++) {
            out.add(dto(rows.get(iter), c.getName(), quoteLines.findByQuoteId(rows.get(iter)
                    .getId())));
        }
        return out;
    }

    /// A buyer asks for a quote: a DRAFT at the company's prices that the
    /// merchant reviews and sends.
    public TradeQuoteDto requestQuote(String username, String storeId, TradeQuoteRequestDto in)
            throws IOException {
        CompanyBuyer b = buyer(username, storeId);
        if (in == null) {
            throw new ResponseStatusException(400, "A quote needs lines");
        }
        return draft(storeId, companies.findById(b.getCompanyId()), username, in);
    }

    /// The buyer's move of one of their company's quotes; 404 for another
    /// company's.
    public TradeQuoteDto moveMyQuote(String username, String storeId, String quoteId,
            String to) throws IOException {
        CompanyBuyer b = buyer(username, storeId);
        TradeQuote q = inStore(storeId, quoteId);
        if (!b.getCompanyId().equals(q.getCompanyId())) {
            throw new ResponseStatusException(404, "No such quote");
        }
        return move(q, to, false, username);
    }

    @Transactional(readOnly = true)
    public List<NetTermDto> myNetTerms(String username, String storeId) {
        CompanyBuyer b = buyer(username, storeId);
        return netTermDtos(storeId, b.getCompanyId());
    }

    /// A net-terms order of the buyer's company, for its invoice; 404 for
    /// anybody else's.
    public NetTerm myNetTerm(String username, String storeId, String orderId) {
        CompanyBuyer b = buyer(username, storeId);
        NetTerm t = netTerm(storeId, orderId);
        if (!b.getCompanyId().equals(t.getCompanyId())) {
            throw new ResponseStatusException(404, "No such net-terms order");
        }
        return t;
    }

    /// Reads a CSV of `sku,quantity` rows into a DRAFT quote. Every row is
    /// checked and every problem reported with its line number; the quote
    /// is written only when no row failed. A first row whose SKU cell says
    /// `sku` is taken as a header. Quoted cells are unwrapped; a cell with
    /// a comma inside quotes is not supported (a SKU has none).
    public BulkOrderResultDto bulkOrder(String username, String storeId, String csv)
            throws IOException {
        CompanyBuyer b = buyer(username, storeId);
        Company c = companies.findById(b.getCompanyId());
        PriceList list = listOf(c);
        String text = csv == null ? "" : csv;
        List<CsvErrorDto> errors = new ArrayList<CsvErrorDto>();
        List<TradeQuoteLine> ls = new ArrayList<TradeQuoteLine>();
        int lineNo = 0;
        int rows = 0;
        int start = 0;
        while (start < text.length()) {
            int nl = text.indexOf('\n', start);
            int end = nl < 0 ? text.length() : nl;
            String row = text.substring(start, end);
            start = end + 1;
            lineNo++;
            if (row.endsWith("\r")) {
                row = row.substring(0, row.length() - 1);
            }
            if (row.trim().length() == 0) {
                continue;
            }
            int comma = row.indexOf(',');
            String sku = cell(comma < 0 ? row : row.substring(0, comma));
            if (rows == 0 && ls.isEmpty() && errors.isEmpty() && "sku".equalsIgnoreCase(sku)) {
                continue;
            }
            rows++;
            if (rows > MAX_CSV_ROWS) {
                errors.add(new CsvErrorDto(lineNo, "Upload at most " + MAX_CSV_ROWS + " rows"));
                break;
            }
            if (comma < 0) {
                errors.add(new CsvErrorDto(lineNo, "Expected sku,quantity"));
                continue;
            }
            String rest = row.substring(comma + 1);
            int extra = rest.indexOf(',');
            String qtyText = cell(extra < 0 ? rest : rest.substring(0, extra));
            int qty = quantity(qtyText);
            if (sku.length() == 0) {
                errors.add(new CsvErrorDto(lineNo, "The SKU is empty"));
                continue;
            }
            if (qty <= 0) {
                errors.add(new CsvErrorDto(lineNo, "\"" + Codes.cut(qtyText, 20)
                        + "\" is not a quantity"));
                continue;
            }
            List<Variant> found = variants.findByStoreIdAndSku(storeId, sku);
            if (found.isEmpty()) {
                errors.add(new CsvErrorDto(lineNo, "Unknown SKU " + Codes.cut(sku, 64)));
                continue;
            }
            Priced p = price(storeId, c, list, found.get(0), qty);
            if (p.problem != null) {
                errors.add(new CsvErrorDto(lineNo, Codes.cut(sku, 64) + ": " + p.problem));
                continue;
            }
            ls.add(p.line);
        }
        if (errors.isEmpty() && ls.isEmpty()) {
            errors.add(new CsvErrorDto(0, "The file has no rows"));
        }
        if (!errors.isEmpty()) {
            return new BulkOrderResultDto(null, ls.size(), errors);
        }
        TradeQuoteDto q = write(storeId, c, username, "CSV upload", ls);
        return new BulkOrderResultDto(q, ls.size(), errors);
    }

    private static String cell(String raw) {
        String t = raw.trim();
        if (t.length() >= 2 && t.charAt(0) == '"' && t.charAt(t.length() - 1) == '"') {
            t = t.substring(1, t.length() - 1).trim();
        }
        return t;
    }

    /// A whole number from 1 to [#MAX_LINE_QTY], or 0.
    private static int quantity(String text) {
        if (text.length() == 0 || text.length() > 6) {
            return 0;
        }
        int n = 0;
        for (int iter = 0; iter < text.length(); iter++) {
            char ch = text.charAt(iter);
            if (ch < '0' || ch > '9') {
                return 0;
            }
            n = n * 10 + (ch - '0');
        }
        return n > MAX_LINE_QTY ? 0 : n;
    }

    // ------------------------------------------------------ web storefront

    /// One line of a buyer's price sheet: the list price, the company's
    /// price and the quantity rule in words (empty when there is none).
    public static final class BuyerPrice {
        public String variantId = "";
        public String sku = "";
        public String title = "";
        public long listCents;
        public long unitCents;
        public String rule = "";
    }

    /// What a company buyer sees of their terms in one store.
    public static final class BuyerTerms {
        public String storeId = "";
        public String companyId = "";
        public String companyName = "";
        public int termsDays;
        public String priceListName = "";
        /// Basis points off list for anything without an entry.
        public int percentOffBps;
        public List<BuyerPrice> prices = new ArrayList<BuyerPrice>();
    }

    /// The stores `username` buys in for a company, for pages that span
    /// every store (the account).
    @Transactional(readOnly = true)
    public List<String> buyerStores(String username) {
        List<String> out = new ArrayList<String>();
        if (username == null) {
            return out;
        }
        List<CompanyBuyer> rows = buyers.findByUsername(username);
        for (int iter = 0; iter < rows.size(); iter++) {
            if (!out.contains(rows.get(iter).getStoreId())) {
                out.add(rows.get(iter).getStoreId());
            }
        }
        return out;
    }

    /// The buyer's company, terms and price sheet; 403 for anybody who does
    /// not buy for a company of the store. The sheet lists the variants the
    /// price list has an entry for; everything else sells at
    /// [BuyerTerms#percentOffBps] off.
    @Transactional(readOnly = true)
    public BuyerTerms myTerms(String username, String storeId) {
        CompanyBuyer b = buyer(username, storeId);
        Company c = companies.findById(b.getCompanyId());
        BuyerTerms out = new BuyerTerms();
        out.storeId = storeId;
        out.companyId = c.getId();
        out.companyName = c.getName();
        out.termsDays = c.getTermsDays();
        PriceList list = listOf(c);
        if (list == null) {
            return out;
        }
        out.priceListName = list.getName();
        out.percentOffBps = list.getPercentOffBps();
        List<PriceListEntry> rows = entries.findByPriceListId(list.getId());
        for (int iter = 0; iter < rows.size() && iter < 200; iter++) {
            PriceListEntry e = rows.get(iter);
            Variant v = variants.findById(e.getVariantId());
            if (v == null || !storeId.equals(v.getStoreId())) {
                continue;
            }
            Product p = products.findById(v.getProductId());
            BuyerPrice bp = new BuyerPrice();
            bp.variantId = v.getId();
            bp.sku = v.getSku();
            bp.title = (p == null ? "" : p.getTitle())
                    + (v.getTitle().length() > 0 ? " - " + v.getTitle() : "");
            bp.listCents = v.getPriceCents();
            bp.unitCents = unitPrice(v, list, e);
            bp.rule = ruleText(e.getMinQty(), e.getMaxQty(), e.getStepQty());
            out.prices.add(bp);
        }
        return out;
    }

    /// A quantity rule in words: "min 6, max 600, in steps of 6".
    /// Empty when there is no rule.
    public static String ruleText(int min, int max, int step) {
        StringBuilder sb = new StringBuilder();
        if (min > 0) {
            sb.append("min ").append(min);
        }
        if (max > 0) {
            sb.append(sb.length() > 0 ? ", " : "").append("max ").append(max);
        }
        if (step > 1) {
            sb.append(sb.length() > 0 ? ", " : "").append("in steps of ").append(step);
        }
        return sb.toString();
    }

    /// Places a cart's lines as an order on the buyer company's net terms,
    /// at the company's prices and held to its quantity rules: the checkout's
    /// "buy on account" choice. A company not on terms answers 409, a line a
    /// rule refuses 400 naming the item. `clientId` is the checkout's
    /// idempotency key.
    public StoreOrder placeOnTerms(String username, String storeId, List<CartLineChangeDto> lines,
            LineOrders.Buyer ship, String clientId) throws IOException {
        CompanyBuyer b = buyer(username, storeId);
        Company c = companies.findById(b.getCompanyId());
        if (c.getTermsDays() <= 0) {
            throw new ResponseStatusException(409, c.getName() + " is not on net terms");
        }
        if (lines == null || lines.isEmpty()) {
            throw new ResponseStatusException(400, "The cart is empty");
        }
        PriceList list = listOf(c);
        List<LineOrders.Item> items = new ArrayList<LineOrders.Item>();
        for (int iter = 0; iter < lines.size(); iter++) {
            CartLineChangeDto l = lines.get(iter);
            Variant v = l.variantId() == null ? null : variants.findById(l.variantId());
            Priced p = price(storeId, c, list, v, l.quantity());
            if (p.problem != null) {
                String name = v == null || v.getSku().length() == 0 ? "An item" : v.getSku();
                throw new ResponseStatusException(400, name + ": " + p.problem);
            }
            items.add(new LineOrders.Item(v.getId(), l.quantity(), p.line.getListCents(),
                    p.line.getUnitCents()));
        }
        LineOrders.Buyer who = ship == null ? new LineOrders.Buyer() : ship;
        who.username = username;
        if (who.shipName == null || who.shipName.length() == 0) {
            who.shipName = c.getName();
        }
        StoreOrder order = lineOrders.place(storeId, CHANNEL, who, items, clientId, username,
                "Ordered on net " + c.getTermsDays());
        terms(order, c, username);
        return order;
    }
}
