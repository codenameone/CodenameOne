package com.example.petstore;

import com.codename1.components.ToastBar;
import com.example.petstore.model.Pet;

/// Call site for the `@RestClient` interface `cn1:generate-openapi` emits.
/// Included by the developer guide's `generate-openapi` appendix.
class PetApiCallSite {

    // tag::appendix-goal-generate-openapi-java-003[]
    void loadPet(String bearerToken) {
        PetApi api = PetApi.of("https://petstore3.swagger.io/api/v3");

        api.getPetById(10L, bearerToken, response -> {
            Pet pet = response.getResponseData();
            if (response.getResponseCode() != 200) {
                ToastBar.showErrorMessage(response.getResponseErrorMessage());
            } else if (pet == null) {
                // A 2xx with an empty or unmappable body leaves this null.
                ToastBar.showErrorMessage("The server returned no pet");
            } else {
                ToastBar.showInfoMessage(pet.name());
            }
        });
    }
    // end::appendix-goal-generate-openapi-java-003[]
}
