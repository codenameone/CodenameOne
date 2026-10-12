/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package example;

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.*;
import com.codename1.backend.mvc.*;

import java.util.*;

/** In-memory demo: restart resets data. All storage access is synchronized. */
@Controller
public class Catalog {
    private final Map<Integer, Product> products = new LinkedHashMap<Integer, Product>();
    private int nextId = 1;

    public Catalog() {
        products.put(nextId, new Product(nextId++, "Notebook", 12, true));
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/products";
    }

    @GetMapping("/products")
    public synchronized String list(Model model, HttpServer.Request request) {
        populate(model);
        return Htmx.isRequest(request) ? "products :: catalog" : "products";
    }

    @GetMapping("/products/new")
    public String create(Model model, HttpServer.Request request) {
        ProductForm form = new ProductForm();
        form.quantity = 1;
        form.active = true;
        model.addAttribute("form", form)
                .addAttribute("action", "/products")
                .addAttribute("heading", "Add product");
        return Htmx.isRequest(request) ? "edit :: editor" : "edit";
    }

    @GetMapping("/products/{id}")
    public synchronized String edit(
            @PathVariable("id") int id, Model model, HttpServer.Request request) {
        Product p = products.get(id);
        if (p == null) return null;
        ProductForm form = new ProductForm();
        form.name = p.getName();
        form.quantity = p.getQuantity();
        form.active = p.isActive();
        model.addAttribute("form", form)
                .addAttribute("action", "/products/" + id)
                .addAttribute("heading", "Edit product");
        return Htmx.isRequest(request) ? "edit :: editor" : "edit";
    }

    @PostMapping("/products")
    public synchronized String add(
            @ModelAttribute("form") ProductForm form,
            BindingResult errors,
            Model model,
            HttpServer.Request request) {
        validate(form, errors);
        if (errors.hasErrors()) return invalid(model, request, "/products", "Add product");
        int id = nextId++;
        products.put(id, new Product(id, form.name.trim(), form.quantity, form.active));
        return saved(model, request);
    }

    @PostMapping("/products/{id}")
    public synchronized String update(
            @PathVariable("id") int id,
            @ModelAttribute("form") ProductForm form,
            BindingResult errors,
            Model model,
            HttpServer.Request request) {
        if (!products.containsKey(id)) return null;
        validate(form, errors);
        if (errors.hasErrors()) return invalid(model, request, "/products/" + id, "Edit product");
        products.put(id, new Product(id, form.name.trim(), form.quantity, form.active));
        return saved(model, request);
    }

    @PostMapping("/products/{id}/delete")
    public synchronized String delete(
            @PathVariable("id") int id, Model model, HttpServer.Request request) {
        if (products.remove(id) == null) return null;
        return saved(model, request);
    }

    private static void validate(ProductForm form, BindingResult errors) {
        if (form.name == null || form.name.trim().isEmpty())
            errors.rejectValue("name", "Enter a product name.");
        if (form.quantity < 0) errors.rejectValue("quantity", "Quantity must be zero or more.");
    }

    private String invalid(Model model, HttpServer.Request request, String action, String heading) {
        model.addAttribute("action", action).addAttribute("heading", heading);
        return Htmx.isRequest(request) ? "edit :: editor" : "edit";
    }

    private String saved(Model model, HttpServer.Request request) {
        if (!Htmx.isRequest(request)) return "redirect:/products";
        populate(model);
        return "products :: catalog";
    }

    private void populate(Model model) {
        model.addAttribute("products", new ArrayList<Product>(products.values()));
    }
}
