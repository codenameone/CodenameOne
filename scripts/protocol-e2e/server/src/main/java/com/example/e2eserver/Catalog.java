package com.example.e2eserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Shared catalog domain + sample data, used by all three transports so the
 * REST / GraphQL / gRPC clients see consistent responses.
 */
public final class Catalog {

    public enum Category { BOOKS, ELECTRONICS, TOYS }

    public record Dimensions(double width, double height) { }

    public record Product(long id, String name, Category category,
                          List<String> tags, double rating, Dimensions dimensions) { }

    private static final List<Product> PRODUCTS = Arrays.asList(
            new Product(1, "The Hobbit", Category.BOOKS,
                    Arrays.asList("fantasy", "classic"), 4.8, new Dimensions(12.0, 20.0)),
            new Product(2, "Wireless Headphones", Category.ELECTRONICS,
                    Arrays.asList("audio", "bluetooth"), 4.2, new Dimensions(18.0, 18.0)),
            new Product(3, "Building Blocks", Category.TOYS,
                    Arrays.asList("kids"), 4.5, new Dimensions(30.0, 25.0)),
            new Product(4, "Clean Code", Category.BOOKS,
                    Arrays.asList("software", "classic"), 4.6, new Dimensions(15.0, 23.0))
    );

    private Catalog() { }

    public static List<Product> all() {
        return PRODUCTS;
    }

    public static Product byId(long id) {
        for (Product p : PRODUCTS) {
            if (p.id() == id) {
                return p;
            }
        }
        return null;
    }

    public static List<Product> byCategory(Category category) {
        List<Product> out = new ArrayList<>();
        for (Product p : PRODUCTS) {
            if (p.category() == category) {
                out.add(p);
            }
        }
        return out;
    }
}
