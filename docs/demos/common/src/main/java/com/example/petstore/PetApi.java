package com.example.petstore;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.Header;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.io.rest.RestClients;
import com.codename1.util.OnComplete;

// tag::appendix-goal-generate-openapi-java-001[]
@RestClient
public interface PetApi {

    @GET("/pet/{petId}")
    void getPetById(@Path("petId") Long petId,
                    @Header("Authorization") String bearerToken,
                    OnComplete<Response<com.example.petstore.model.Pet>> callback);

    @POST("/pet")
    void addPet(@Body com.example.petstore.model.Pet body,
                @Header("Authorization") String bearerToken,
                OnComplete<Response<com.example.petstore.model.Pet>> callback);

    static PetApi of(String baseUrl) {
        return RestClients.create(PetApi.class, baseUrl);
    }
}
// end::appendix-goal-generate-openapi-java-001[]
