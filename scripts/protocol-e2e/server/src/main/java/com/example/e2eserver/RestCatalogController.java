package com.example.e2eserver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** REST transport (described by openapi.json). */
@RestController
public class RestCatalogController {

    @GetMapping("/api/products")
    public List<Catalog.Product> listProducts() {
        return Catalog.all();
    }

    @GetMapping("/api/products/{id}")
    public Catalog.Product getProduct(@PathVariable long id) {
        return Catalog.byId(id);
    }

    @GetMapping("/api/products/category/{category}")
    public List<Catalog.Product> getProductsByCategory(@PathVariable Catalog.Category category) {
        return Catalog.byCategory(category);
    }
}
