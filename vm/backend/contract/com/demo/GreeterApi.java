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
package com.demo;

import java.util.List;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.Cookie;
import com.codename1.annotations.rest.DELETE;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.Header;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

/**
 * THE contract. This one interface is the single source of truth for both ends:
 * the Codename One app gets a typed client generated from it by
 * RestClientAnnotationProcessor, and the backend gets a synchronous server
 * interface plus a dispatcher generated from it by RestServerAnnotationProcessor.
 * Change a path or a parameter here and whichever side did not follow stops
 * compiling.
 */
@RestClient
public interface GreeterApi {
    @GET("/greet/{name}")
    void greet(@Path("name") String name,
               @Query("loud") String loud,
               OnComplete<Response<String>> callback);

    /** Reads the caller's identity out of a header and a cookie. */
    @GET("/whoami")
    void whoami(@Header("X-User") String user,
                @Cookie("session") String session,
                OnComplete<Response<String>> callback);

    /** Persists a pet and returns it with the id the database assigned. */
    @POST("/pet")
    void addPet(@Body Pet pet, OnComplete<Response<Pet>> callback);

    @GET("/pet/{id}")
    void getPet(@Path("id") long id, OnComplete<Response<Pet>> callback);

    @GET("/pets")
    void listPets(@Query("species") String species,
                  OnComplete<Response<List<Pet>>> callback);

    /** Exchanges a username and password for a bearer token. */
    @POST("/login")
    void login(@Body Credentials credentials, OnComplete<Response<String>> callback);

    /** Stores the request body as a BLOB against the pet. */
    @POST("/pet/{id}/photo")
    void setPhoto(@Path("id") long id, @Body String data, OnComplete<Response<String>> callback);

    /** Reads the BLOB back and reports what came out of the database. */
    @GET("/pet/{id}/photo")
    void getPhoto(@Path("id") long id, OnComplete<Response<String>> callback);

    /**
     * Inserts every pet in one transaction. If any of them is invalid the whole
     * batch is rolled back, so the request is all-or-nothing.
     */
    @POST("/pets/bulk")
    void addPets(@Header("Authorization") String authorization,
                 @Body java.util.List<Pet> pets,
                 OnComplete<Response<String>> callback);

    /**
     * Returns the pet it was given, unchanged. Nothing is persisted, so this is
     * the one route that round trips a DTO -- nested collections included --
     * through the generated codec and back out with nothing else in the way.
     */
    @POST("/echo")
    void echo(@Body Pet pet, OnComplete<Response<Pet>> callback);

    /** Calls a URL over TLS and returns the status and body it got back. */
    @GET("/fetch")
    void fetch(@Query("url") String url, OnComplete<Response<String>> callback);

    @DELETE("/pet/{id}")
    void deletePet(@Header("Authorization") String authorization,
                   @Path("id") long id,
                   OnComplete<Response<String>> callback);
}
