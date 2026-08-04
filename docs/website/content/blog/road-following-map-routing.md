---
title: "A Polyline Is Not a Route"
slug: road-following-map-routing
url: /blog/road-following-map-routing/
date: '2026-08-03'
author: Shai Almog
description: "The new Codename One maps routing API turns coordinates into road-following geometry, distance, duration, legs, and steps through OSRM or an application-defined service."
feed_html: '<img src="https://www.codenameone.com/blog/road-following-map-routing.jpg" alt="A road-following route replaces a straight line between two map pins" /> The new Codename One maps routing API turns coordinates into road-following geometry, distance, duration, legs, and steps through OSRM or an application-defined service.'
series: ["release-2026-07-31"]
---

![A road-following route replaces a straight line between two map pins](/blog/road-following-map-routing.jpg)

A polyline can draw the points you give it. It cannot discover the road between them.

[PR #5480](https://github.com/codenameone/CodenameOne/pull/5480) adds `com.codename1.maps.routing`, a portable route model and service layer for road geometry, distance, duration, waypoints, alternatives, legs, steps, and encoded polylines.

We talked about routing in [Friday's release post](/blog/push-v3-new-cloud/).

## The two-line route

For the common case, give `Routing` a map and two coordinates:

```java
MapView map = new MapView();
Routing.showRoute(
        map,
        new LatLng(38.8977, -77.0365),
        new LatLng(38.8894, -77.0352)
);
```

The call returns immediately. The routing service finds the best route, the API adds its polyline, and the map frames the result.

{{< mermaid >}}
flowchart LR
    A["Origin and destination"] --> B["RouteRequest"]
    B --> C["RouteService"]
    C --> D["Road network calculation"]
    D --> E["Route, legs, steps, distance, duration"]
    E --> F["Polyline on MapSurface"]
    E --> G["Application UI"]
{{< /mermaid >}}

`showRoute(...)` is intentionally small. It is useful when the line itself is the result. An application that needs error UI, custom styling, ETA, or alternatives should use the callback route.

## Own the result when it matters

```java
RouteRequest request = new RouteRequest(origin, destination)
        .setTravelMode(TravelMode.DRIVING)
        .addWaypoint(coffeeStop)
        .setAlternatives(true)
        .setSteps(true);

Routing.findRoute(request, new RouteCallback() {
    public void routesFound(List routes) {
        Route best = (Route) routes.get(0);

        map.addPolyline(best.toPolyline()
                .setStrokeColor(0xff5722)
                .setStrokeWidth(6));
        map.fitBounds(best.getBounds(), 40);

        distanceLabel.setText(
                Math.round(best.getDistanceMeters() / 1000.0) + " km");
        etaLabel.setText(
                Math.round(best.getDurationSeconds() / 60.0) + " min");
    }

    public void routeFailed(String message, Throwable error) {
        ToastBar.showErrorMessage(message);
    }
});
```

The callback is invoked exactly once and always later on the Codename One EDT, even if a custom service responds synchronously, responds from a worker thread, responds twice, or throws after responding. The facade contains those service errors so application timing does not depend on the provider implementation.

The model exposes:

- One or more route alternatives
- Total distance and duration
- Geographic bounds
- Route geometry
- Legs between waypoints
- Step instructions and maneuver locations
- Provider metadata

`PolylineCodec` supports precision 5 and precision 6 encoded geometry. That lets a service retain the compact wire format and decode it only when a `Polyline` is needed.

## OSRM makes the first run easy

The default `RouteService` is `OsrmRouteService`. It needs no API key, so the two-line example can work without provider signup.

That default points to the public OSRM demonstration server. The server has no production SLA and asks clients to keep usage light. It can reject large or abusive workloads, and its public profile is configured for cars.

This means:

- `DRIVING` is appropriate for a quick test.
- `WALKING` or `CYCLING` against the default demo can still return car routing.
- A shipping application should use a provider and capacity it controls.
- Offline routing requires another implementation.

Travel mode is a request, not a guarantee. A `RouteService` reports what it supports, and an application should not label a result “walking” when the active backend only has a car graph.

## Point OSRM at infrastructure you control

OSRM is open source and can be self-hosted. If your server exposes an OSRM-compatible route endpoint, install it as the application service:

```java
Routing.setService(new OsrmRouteService(
        "https://routing.example.com"
));
```

Use the exact constructor and endpoint configuration supported by your selected release. The architectural point is that application code still consumes `Route`, not provider JSON.

For a different provider, implement `RouteService`:

```java
public final class CompanyRouteService implements RouteService {
    public String getId() {
        return "company-routing";
    }

    public boolean isAvailable() {
        return credentialsAreReady();
    }

    public void findRoutes(RouteRequest request, RouteCallback callback) {
        // Translate the portable request, call the provider,
        // then return portable Route objects through the callback.
    }
}
```

```java
Routing.setService(new CompanyRouteService());
```

That seam supports commercial providers, a company gateway, an offline engine, or a route service with domain constraints such as truck height and hazardous materials.

## The maps API and routing API stay separate

`MapSurface` displays geometry. `RouteService` discovers geometry. Keeping them separate avoids tying a route provider to one renderer.

The same route can be:

- Drawn on a native or vector map
- Styled according to traffic or accessibility
- Summarized as distance and ETA
- Saved as an encoded polyline
- Compared with alternatives
- Sent to another screen without retaining the map component

The API does not yet claim turn-by-turn navigation, rerouting, traffic prediction, offline map packages, or voice guidance. Those are stateful products with location updates and provider-specific rules. This release provides the route result that those systems need, without naming a static polyline “navigation.”

## Production checklist

Before shipping:

1. Choose a routing service with terms, capacity, data coverage, and travel profiles that fit the application.
2. Report route failures to the user. The no-callback `showRoute(...)` convenience method cannot do that.
3. Verify units before displaying them. The model uses meters and seconds.
4. Test dateline crossing, unreachable points, ferries, tolls, and waypoint order for the markets you serve.
5. Keep provider keys out of client code when the provider expects a server-side secret.
6. Attribute map and routing data according to each provider's license.

A route is an answer from a changing road graph, not a decorative line. The new API finally represents it that way.

Next in the series: [why Codename One is starting a staged move beyond Maven Central](/blog/maven-central-cloudflare-r2/).

---

## Discussion

_Which routing provider or offline engine should receive the next `RouteService` implementation?_

{{< giscus >}}
