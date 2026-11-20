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
package com.codenameone.examples.bazaar.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.DELETE;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.Header;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;


/// A shopper's cart at one store.
///
/// Anyone may use a cart, signed in or not. A signed-in shopper has one cart
/// per store; a guest's cart is found by the token the first answer carries,
/// sent back in the `X-Cart-Token` header. [#merge] moves a guest cart into
/// the account once the guest signs in.
@RestClient
public interface CartApi {
    /// The cart, made empty on first use.
    @GET("/api/shop/cart/{storeId}")
    void get(@Path("storeId") String storeId, @Header("X-Cart-Token") String token,
            OnComplete<Response<CartDto>> callback);

    /// Adds `quantity` of a variant, to a line of its own or the one it has.
    @POST("/api/shop/cart/{storeId}/lines")
    void add(@Path("storeId") String storeId, @Header("X-Cart-Token") String token,
            @Body CartLineChangeDto line, OnComplete<Response<CartDto>> callback);

    /// Sets a line's quantity; 0 removes it.
    @POST("/api/shop/cart/{storeId}/lines/{lineId}")
    void update(@Path("storeId") String storeId, @Path("lineId") String lineId,
            @Header("X-Cart-Token") String token, @Body CartLineChangeDto line,
            OnComplete<Response<CartDto>> callback);

    @DELETE("/api/shop/cart/{storeId}/lines/{lineId}")
    void remove(@Path("storeId") String storeId, @Path("lineId") String lineId,
            @Header("X-Cart-Token") String token, OnComplete<Response<CartDto>> callback);

    /// Applies a discount code; a code that does nothing says why in `problems`.
    @POST("/api/shop/cart/{storeId}/discount")
    void applyDiscount(@Path("storeId") String storeId, @Header("X-Cart-Token") String token,
            @Body CodeDto code, OnComplete<Response<CartDto>> callback);

    @DELETE("/api/shop/cart/{storeId}/discount")
    void removeDiscount(@Path("storeId") String storeId, @Header("X-Cart-Token") String token,
            OnComplete<Response<CartDto>> callback);

    /// Applies a gift card; answers 404 for a code that is not one.
    @POST("/api/shop/cart/{storeId}/gift-card")
    void applyGiftCard(@Path("storeId") String storeId, @Header("X-Cart-Token") String token,
            @Body CodeDto code, OnComplete<Response<CartDto>> callback);

    @DELETE("/api/shop/cart/{storeId}/gift-card")
    void removeGiftCard(@Path("storeId") String storeId, @Header("X-Cart-Token") String token,
            OnComplete<Response<CartDto>> callback);

    /// The cart priced for an address: its shipping options, the chosen
    /// one's price (the cheapest when none is chosen) and the tax.
    @POST("/api/shop/cart/{storeId}/estimate")
    void estimate(@Path("storeId") String storeId, @Header("X-Cart-Token") String token,
            @Body EstimateDto estimate, OnComplete<Response<CartDto>> callback);

    /// Moves the guest cart of `X-Cart-Token` into the signed-in shopper's
    /// cart at the store, adding quantities where both have a variant.
    @POST("/api/cart/{storeId}/merge")
    void merge(@Path("storeId") String storeId, @Header("X-Cart-Token") String token,
            OnComplete<Response<CartDto>> callback);
}
