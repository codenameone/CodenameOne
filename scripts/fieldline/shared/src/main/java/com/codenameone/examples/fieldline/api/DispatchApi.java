package com.codenameone.examples.fieldline.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;
import java.util.List;

/// The dispatcher's console: the board, the optimizer and the exceptions that
/// force a re-plan. Everything here needs the DISPATCHER role.
@RestClient
public interface DispatchApi {
    /// Routes for `days` days from `day`, epoch milliseconds of a midnight.
    @GET("/api/dispatch/board")
    void board(@Query("day") long day, @Query("days") int days, OnComplete<Response<BoardDto>> callback);

    @GET("/api/dispatch/technicians")
    void technicians(OnComplete<Response<List<TechnicianDto>>> callback);

    @GET("/api/dispatch/sites")
    void sites(OnComplete<Response<List<SiteDto>>> callback);

    @POST("/api/dispatch/jobs")
    void createJob(@Body NewJobDto job, OnComplete<Response<JobDto>> callback);

    /// Re-plans the whole horizon, keeping work that has started.
    /// Moves a job between the intake states (UNASSIGNED, AWAITING_PARTS,
    /// BLOCKED). It never schedules; that is the board's.
    @POST("/api/dispatch/jobs/{id}/intake")
    void intake(@Path("id") String id, @Body IntakeDto intake, OnComplete<Response<JobDto>> callback);

    @POST("/api/dispatch/optimize")
    void optimize(OnComplete<Response<ScheduleRunDto>> callback);

    /// Somebody is out: cut the time from their availability and repair the plan.
    @POST("/api/dispatch/absence")
    void absence(@Body AbsenceDto absence, OnComplete<Response<ScheduleRunDto>> callback);

    /// A drag on the board, checked against every hard constraint first.
    @POST("/api/dispatch/move")
    void move(@Body MoveDto move, OnComplete<Response<ValidationDto>> callback);

    @GET("/api/dispatch/timeoff")
    void timeOff(OnComplete<Response<List<TimeOffDto>>> callback);

    /// Approving re-plans around the absence and answers with that run.
    @POST("/api/dispatch/timeoff/{id}")
    void decide(@Path("id") String id, @Body DecisionDto decision, OnComplete<Response<ScheduleRunDto>> callback);

    @GET("/api/dispatch/staffing")
    void staffing(@Query("days") int days, OnComplete<Response<StaffingDto>> callback);

    @GET("/api/dispatch/runs")
    void runs(OnComplete<Response<List<ScheduleRunDto>>> callback);
}
