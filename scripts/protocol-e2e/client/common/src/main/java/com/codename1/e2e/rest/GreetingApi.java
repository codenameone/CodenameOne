// Mirrors the output of cn1:generate-openapi for the e2e openapi.json.
package com.codename1.e2e.rest;

import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.Header;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.io.rest.RestClients;
import com.codename1.util.OnComplete;

@RestClient
public interface GreetingApi {

    @GET("/api/greeting")
    void greeting(@Query("name") String name,
                  @Header("Authorization") String bearerToken,
                  OnComplete<Response<Greeting>> callback);

    static GreetingApi of(String baseUrl) {
        return RestClients.create(GreetingApi.class, baseUrl);
    }
}
