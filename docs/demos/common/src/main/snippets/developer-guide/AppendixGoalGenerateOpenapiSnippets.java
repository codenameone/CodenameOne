// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

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

// tag::appendix-goal-generate-openapi-java-002[]
PetApi api = PetApi.of("https://petstore3.swagger.io/api/v3");
api.getPetById(123L, "MY_TOKEN", response -> {
    if (response.getResponseCode() == 200) {
        Pet pet = response.getResponseData();
        renderPet(pet);
    }
});
// end::appendix-goal-generate-openapi-java-002[]
