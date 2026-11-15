package com.codenameone.examples.fieldline.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.PUT;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;
import java.util.List;

/// What a technician does in the field: their day, each job, the evidence they
/// collect and their own availability. A technician reaches only the jobs
/// assigned to them; a dispatcher reaches every job.
@RestClient
public interface FieldApi {
    /// Today's route for the signed-in technician, in driving order.
    @GET("/api/my/day")
    void today(OnComplete<Response<TodayDto>> callback);

    @GET("/api/jobs/{id}")
    void job(@Path("id") String id, OnComplete<Response<JobDto>> callback);

    /// EN_ROUTE, ON_SITE or COMPLETED. Completing needs every checklist line
    /// answered and, for an inspection, the customer's signature.
    @POST("/api/jobs/{id}/status")
    void setStatus(@Path("id") String id, @Body StatusDto status, OnComplete<Response<JobDto>> callback);

    @PUT("/api/jobs/{id}/checklist")
    void saveChecklist(@Path("id") String id, @Body ChecklistDto checklist,
            OnComplete<Response<JobDto>> callback);

    /// Attaches a photo and answers with the assistant's diagnosis of it.
    @POST("/api/jobs/{id}/photos")
    void addPhoto(@Path("id") String id, @Body PhotoUploadDto photo, OnComplete<Response<PhotoDto>> callback);

    @GET("/api/jobs/{id}/photos")
    void photos(@Path("id") String id, OnComplete<Response<List<PhotoDto>>> callback);

    @POST("/api/jobs/{id}/signature")
    void sign(@Path("id") String id, @Body SignatureDto signature, OnComplete<Response<SignatureDto>> callback);

    /// The stored signature, verified again against the report as it is now.
    @GET("/api/jobs/{id}/signature")
    void signature(@Path("id") String id, OnComplete<Response<SignatureDto>> callback);

    /// A report the assistant drafts from what was recorded on site.
    @POST("/api/jobs/{id}/report")
    void draftReport(@Path("id") String id, OnComplete<Response<ReportDto>> callback);

    @GET("/api/my/availability")
    void availability(OnComplete<Response<AvailabilityDto>> callback);

    /// Saving availability re-plans the work it affects.
    @PUT("/api/my/availability")
    void saveAvailability(@Body AvailabilityDto availability, OnComplete<Response<AvailabilityDto>> callback);

    @GET("/api/my/timeoff")
    void myTimeOff(OnComplete<Response<List<TimeOffDto>>> callback);

    @POST("/api/my/timeoff")
    void requestTimeOff(@Body TimeOffDto request, OnComplete<Response<TimeOffDto>> callback);

    /// Clocks the signed-in technician in or out, and answers their day.
    @POST("/api/my/clock")
    void clock(@Body ClockDto clock, OnComplete<Response<TodayDto>> callback);

    @POST("/api/my/location")
    void reportLocation(@Body LocationDto where, OnComplete<Response<LocationDto>> callback);

    /// The assistant: diagnosis help and manuals for a technician, schedule
    /// changes for a dispatcher.
    @POST("/api/assistant")
    void ask(@Body AssistantRequestDto request, OnComplete<Response<AssistantReplyDto>> callback);
}
