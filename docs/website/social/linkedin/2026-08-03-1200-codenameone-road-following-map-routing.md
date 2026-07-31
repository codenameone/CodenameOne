---
title: "A polyline is not a route"
slug: 2026-08-03-1200-codenameone-road-following-map-routing
platform: linkedin
account: codenameone
source_slug: road-following-map-routing
publish_at: '2026-08-03T12:00:00'
timezone: Asia/Jerusalem
image: /blog/road-following-map-routing.jpg
---

A polyline joins the coordinates you already have. It cannot discover the road between them.

The new `com.codename1.maps.routing` API turns an origin, destination, and optional waypoints into road-following geometry, distance, duration, alternatives, legs, and steps.

The small path is two lines: create a map and call `Routing.showRoute(...)`.

The controlled path returns portable `Route` objects so the application can style the line, frame the bounds, show distance and ETA, compare alternatives, or retain encoded geometry.

The default OSRM service is keyless and useful for a first run. Its public demo endpoint has no production SLA and uses a car profile, so asking it for walking or cycling does not make the underlying road graph change.

Production applications can point at a controlled OSRM instance or install another `RouteService`.

{{canonical}}
