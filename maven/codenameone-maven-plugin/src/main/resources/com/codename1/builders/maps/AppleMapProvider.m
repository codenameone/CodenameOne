/*
 * Codename One maps provider -- Apple MapKit (iOS).
 *
 * BUILD TEMPLATE. Copied into the generated Xcode project's native sources as
 * CN1AppleMapKit.m when maps.provider=apple. Implements the native methods
 * declared by the injected com.codename1.maps.MapProviderImpl (ParparVM binds
 * them by the symbol names below) and forwards taps, long-presses and camera
 * changes back into Java via the static callbacks on
 * com.codename1.maps.NativeMap. MapKit is a free iOS system framework.
 *
 * watchOS note: MKMapView and the overlay renderers are unavailable on
 * watchOS, so on that platform we compile a set of no-op stubs that keep the
 * translated provider linkable. nativeCreate returns 0 there, which makes
 * MapProviderImpl.createPeer return null and NativeMap fall back to the
 * pure-vector MapView.
 */
#import <Foundation/Foundation.h>

#ifndef BRIDGE_CAST
#if __has_feature(objc_arc)
#define BRIDGE_CAST __bridge
#else
#define BRIDGE_CAST
#endif
#endif

#if TARGET_OS_WATCH

// ---- watchOS stubs ---------------------------------------------------------
// MapKit map views are unavailable on watchOS. These keep the symbols the
// translated MapProviderImpl references resolvable; the map degrades to the
// vector MapView at runtime because nativeCreate returns 0.

JAVA_LONG com_codename1_maps_MapProviderImpl_nativeCreate___int_double_double_float_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_DOUBLE lat, JAVA_DOUBLE lon, JAVA_FLOAT zoom) { return 0; }
void com_codename1_maps_MapProviderImpl_nativeDeinit___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId) {}
void com_codename1_maps_MapProviderImpl_nativeSetCamera___int_double_double_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_DOUBLE lat, JAVA_DOUBLE lon, JAVA_FLOAT zoom) {}
JAVA_DOUBLE com_codename1_maps_MapProviderImpl_nativeGetLat___int_R_double(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId) { return 0; }
JAVA_DOUBLE com_codename1_maps_MapProviderImpl_nativeGetLon___int_R_double(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId) { return 0; }
JAVA_FLOAT com_codename1_maps_MapProviderImpl_nativeGetZoom___int_R_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId) { return 0; }
JAVA_LONG com_codename1_maps_MapProviderImpl_nativeAddMarker___int_double_double_java_lang_String_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_DOUBLE lat, JAVA_DOUBLE lon, JAVA_OBJECT title) { return 0; }
JAVA_LONG com_codename1_maps_MapProviderImpl_nativeAddPolyline___int_double_1ARRAY_double_1ARRAY_int_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_OBJECT lats, JAVA_OBJECT lons, JAVA_INT color, JAVA_INT width) { return 0; }
JAVA_LONG com_codename1_maps_MapProviderImpl_nativeAddPolygon___int_double_1ARRAY_double_1ARRAY_int_int_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_OBJECT lats, JAVA_OBJECT lons, JAVA_INT fill, JAVA_INT stroke, JAVA_INT width) { return 0; }
JAVA_LONG com_codename1_maps_MapProviderImpl_nativeAddCircle___int_double_double_double_int_int_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_DOUBLE lat, JAVA_DOUBLE lon, JAVA_DOUBLE radius, JAVA_INT fill, JAVA_INT stroke, JAVA_INT width) { return 0; }
void com_codename1_maps_MapProviderImpl_nativeRemove___int_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_LONG elementId) {}
void com_codename1_maps_MapProviderImpl_nativeRemoveAll___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId) {}
JAVA_INT com_codename1_maps_MapProviderImpl_nativeScreenX___int_double_double_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_DOUBLE lat, JAVA_DOUBLE lon) { return 0; }
JAVA_INT com_codename1_maps_MapProviderImpl_nativeScreenY___int_double_double_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_DOUBLE lat, JAVA_DOUBLE lon) { return 0; }
JAVA_DOUBLE com_codename1_maps_MapProviderImpl_nativeLat___int_int_int_R_double(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_INT x, JAVA_INT y) { return 0; }
JAVA_DOUBLE com_codename1_maps_MapProviderImpl_nativeLon___int_int_int_R_double(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_INT x, JAVA_INT y) { return 0; }
void com_codename1_maps_MapProviderImpl_nativeSetShowMyLocation___int_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_BOOLEAN show) {}
void com_codename1_maps_MapProviderImpl_nativeSetRotateEnabled___int_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_BOOLEAN enabled) {}
void com_codename1_maps_MapProviderImpl_nativeSetMapType___int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_INT type) {}

#else

#import <MapKit/MapKit.h>
#import <CoreLocation/CoreLocation.h>

extern NSString* toNSString(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);

extern void com_codename1_maps_NativeMap_fireTap___int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_INT mapId, JAVA_INT x, JAVA_INT y);
extern void com_codename1_maps_NativeMap_fireLongPress___int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_INT mapId, JAVA_INT x, JAVA_INT y);
extern void com_codename1_maps_NativeMap_fireCameraChange___int(CN1_THREAD_STATE_MULTI_ARG JAVA_INT mapId);

@interface CN1AppleMap : NSObject <MKMapViewDelegate>
@property (nonatomic, assign) int mapId;
@property (nonatomic, strong) MKMapView *mapView;
@property (nonatomic, strong) NSMutableDictionary *elements;
@property (nonatomic, assign) long nextId;
@end

@implementation CN1AppleMap

- (instancetype)initWithMapId:(int)mapId {
    self = [super init];
    if (self) {
        _mapId = mapId;
        _nextId = 1;
        _elements = [NSMutableDictionary dictionary];
        _mapView = [[MKMapView alloc] initWithFrame:CGRectMake(0, 0, 320, 480)];
        _mapView.delegate = self;
        UITapGestureRecognizer *tap = [[UITapGestureRecognizer alloc]
            initWithTarget:self action:@selector(onTap:)];
        [_mapView addGestureRecognizer:tap];
        UILongPressGestureRecognizer *lp = [[UILongPressGestureRecognizer alloc]
            initWithTarget:self action:@selector(onLongPress:)];
        [_mapView addGestureRecognizer:lp];
    }
    return self;
}

- (void)onTap:(UITapGestureRecognizer *)g {
    CGPoint p = [g locationInView:self.mapView];
    com_codename1_maps_NativeMap_fireTap___int_int_int(getThreadLocalData(), self.mapId, (int)p.x, (int)p.y);
}

- (void)onLongPress:(UILongPressGestureRecognizer *)g {
    if (g.state != UIGestureRecognizerStateBegan) {
        return;
    }
    CGPoint p = [g locationInView:self.mapView];
    com_codename1_maps_NativeMap_fireLongPress___int_int_int(getThreadLocalData(), self.mapId, (int)p.x, (int)p.y);
}

- (void)mapView:(MKMapView *)mapView regionDidChangeAnimated:(BOOL)animated {
    com_codename1_maps_NativeMap_fireCameraChange___int(getThreadLocalData(), self.mapId);
}

- (MKOverlayRenderer *)mapView:(MKMapView *)mapView rendererForOverlay:(id<MKOverlay>)overlay {
    if ([overlay isKindOfClass:[MKPolyline class]]) {
        MKPolylineRenderer *r = [[MKPolylineRenderer alloc] initWithPolyline:overlay];
        r.strokeColor = [UIColor blueColor];
        r.lineWidth = 4;
        return r;
    }
    if ([overlay isKindOfClass:[MKPolygon class]]) {
        MKPolygonRenderer *r = [[MKPolygonRenderer alloc] initWithPolygon:overlay];
        r.fillColor = [[UIColor blueColor] colorWithAlphaComponent:0.25];
        r.strokeColor = [UIColor blueColor];
        r.lineWidth = 2;
        return r;
    }
    if ([overlay isKindOfClass:[MKCircle class]]) {
        MKCircleRenderer *r = [[MKCircleRenderer alloc] initWithCircle:overlay];
        r.fillColor = [[UIColor greenColor] colorWithAlphaComponent:0.25];
        r.strokeColor = [UIColor greenColor];
        r.lineWidth = 2;
        return r;
    }
    return nil;
}

@end

static NSMutableDictionary *cn1AppleMaps() {
    static NSMutableDictionary *maps = nil;
    static dispatch_once_t once;
    dispatch_once(&once, ^{ maps = [NSMutableDictionary dictionary]; });
    return maps;
}

static CN1AppleMap *cn1MapFor(int mapId) {
    return [cn1AppleMaps() objectForKey:[NSNumber numberWithInt:mapId]];
}

static float spanToZoom(double lonDelta) {
    if (lonDelta <= 0) {
        return 0;
    }
    return (float)(log(360.0 / lonDelta) / log(2.0));
}

static double zoomToSpan(float zoom) {
    return 360.0 / pow(2.0, zoom);
}

JAVA_LONG com_codename1_maps_MapProviderImpl_nativeCreate___int_double_double_float_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_DOUBLE lat, JAVA_DOUBLE lon, JAVA_FLOAT zoom) {
    __block CN1AppleMap *m = nil;
    double span = zoomToSpan((float)zoom);
    void (^createBlock)(void) = ^{
        m = [[CN1AppleMap alloc] initWithMapId:(int)mapId];
        MKCoordinateRegion region = MKCoordinateRegionMake(
            CLLocationCoordinate2DMake(lat, lon), MKCoordinateSpanMake(span, span));
        [m.mapView setRegion:region animated:NO];
        [cn1AppleMaps() setObject:m forKey:[NSNumber numberWithInt:(int)mapId]];
    };
    // The Codename One iOS EDT runs on the main thread, so a dispatch_sync to
    // the main queue from here would deadlock. Create inline when already on
    // the main thread; only marshal when invoked from a background thread.
    if ([NSThread isMainThread]) {
        createBlock();
    } else {
        dispatch_sync(dispatch_get_main_queue(), createBlock);
    }
    return (JAVA_LONG)((BRIDGE_CAST void*)m.mapView);
}

void com_codename1_maps_MapProviderImpl_nativeDeinit___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId) {
    [cn1AppleMaps() removeObjectForKey:[NSNumber numberWithInt:(int)mapId]];
}

void com_codename1_maps_MapProviderImpl_nativeSetCamera___int_double_double_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_DOUBLE lat, JAVA_DOUBLE lon, JAVA_FLOAT zoom) {
    CN1AppleMap *m = cn1MapFor((int)mapId);
    if (!m) { return; }
    double span = zoomToSpan((float)zoom);
    dispatch_async(dispatch_get_main_queue(), ^{
        MKCoordinateRegion region = MKCoordinateRegionMake(
            CLLocationCoordinate2DMake(lat, lon), MKCoordinateSpanMake(span, span));
        [m.mapView setRegion:region animated:YES];
    });
}

JAVA_DOUBLE com_codename1_maps_MapProviderImpl_nativeGetLat___int_R_double(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId) {
    CN1AppleMap *m = cn1MapFor((int)mapId);
    return m ? m.mapView.centerCoordinate.latitude : 0;
}

JAVA_DOUBLE com_codename1_maps_MapProviderImpl_nativeGetLon___int_R_double(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId) {
    CN1AppleMap *m = cn1MapFor((int)mapId);
    return m ? m.mapView.centerCoordinate.longitude : 0;
}

JAVA_FLOAT com_codename1_maps_MapProviderImpl_nativeGetZoom___int_R_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId) {
    CN1AppleMap *m = cn1MapFor((int)mapId);
    return m ? spanToZoom(m.mapView.region.span.longitudeDelta) : 0;
}

JAVA_LONG com_codename1_maps_MapProviderImpl_nativeAddMarker___int_double_double_java_lang_String_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_DOUBLE lat, JAVA_DOUBLE lon, JAVA_OBJECT title) {
    CN1AppleMap *m = cn1MapFor((int)mapId);
    if (!m) { return 0; }
    NSString *t = title ? toNSString(threadStateData, title) : nil;
    MKPointAnnotation *a = [[MKPointAnnotation alloc] init];
    a.coordinate = CLLocationCoordinate2DMake(lat, lon);
    a.title = t;
    long eid = m.nextId++;
    [m.elements setObject:a forKey:[NSNumber numberWithLong:eid]];
    dispatch_async(dispatch_get_main_queue(), ^{ [m.mapView addAnnotation:a]; });
    return (JAVA_LONG)eid;
}

static void cn1Coords(JAVA_OBJECT lats, JAVA_OBJECT lons, CLLocationCoordinate2D **out, int *count) {
    int n = (int)((JAVA_ARRAY)lats)->length;
    JAVA_ARRAY_DOUBLE *la = (JAVA_ARRAY_DOUBLE*)((JAVA_ARRAY)lats)->data;
    JAVA_ARRAY_DOUBLE *lo = (JAVA_ARRAY_DOUBLE*)((JAVA_ARRAY)lons)->data;
    CLLocationCoordinate2D *c = malloc(sizeof(CLLocationCoordinate2D) * (n > 0 ? n : 1));
    for (int i = 0; i < n; i++) {
        c[i] = CLLocationCoordinate2DMake(la[i], lo[i]);
    }
    *out = c;
    *count = n;
}

JAVA_LONG com_codename1_maps_MapProviderImpl_nativeAddPolyline___int_double_1ARRAY_double_1ARRAY_int_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_OBJECT lats, JAVA_OBJECT lons, JAVA_INT color, JAVA_INT width) {
    CN1AppleMap *m = cn1MapFor((int)mapId);
    if (!m) { return 0; }
    CLLocationCoordinate2D *c; int n;
    cn1Coords(lats, lons, &c, &n);
    MKPolyline *line = [MKPolyline polylineWithCoordinates:c count:n];
    free(c);
    long eid = m.nextId++;
    [m.elements setObject:line forKey:[NSNumber numberWithLong:eid]];
    dispatch_async(dispatch_get_main_queue(), ^{ [m.mapView addOverlay:line]; });
    return (JAVA_LONG)eid;
}

JAVA_LONG com_codename1_maps_MapProviderImpl_nativeAddPolygon___int_double_1ARRAY_double_1ARRAY_int_int_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_OBJECT lats, JAVA_OBJECT lons, JAVA_INT fill, JAVA_INT stroke, JAVA_INT width) {
    CN1AppleMap *m = cn1MapFor((int)mapId);
    if (!m) { return 0; }
    CLLocationCoordinate2D *c; int n;
    cn1Coords(lats, lons, &c, &n);
    MKPolygon *poly = [MKPolygon polygonWithCoordinates:c count:n];
    free(c);
    long eid = m.nextId++;
    [m.elements setObject:poly forKey:[NSNumber numberWithLong:eid]];
    dispatch_async(dispatch_get_main_queue(), ^{ [m.mapView addOverlay:poly]; });
    return (JAVA_LONG)eid;
}

JAVA_LONG com_codename1_maps_MapProviderImpl_nativeAddCircle___int_double_double_double_int_int_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_DOUBLE lat, JAVA_DOUBLE lon, JAVA_DOUBLE radius, JAVA_INT fill, JAVA_INT stroke, JAVA_INT width) {
    CN1AppleMap *m = cn1MapFor((int)mapId);
    if (!m) { return 0; }
    MKCircle *circle = [MKCircle circleWithCenterCoordinate:CLLocationCoordinate2DMake(lat, lon) radius:radius];
    long eid = m.nextId++;
    [m.elements setObject:circle forKey:[NSNumber numberWithLong:eid]];
    dispatch_async(dispatch_get_main_queue(), ^{ [m.mapView addOverlay:circle]; });
    return (JAVA_LONG)eid;
}

void com_codename1_maps_MapProviderImpl_nativeRemove___int_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_LONG elementId) {
    CN1AppleMap *m = cn1MapFor((int)mapId);
    if (!m) { return; }
    id e = [m.elements objectForKey:[NSNumber numberWithLong:(long)elementId]];
    if (!e) { return; }
    [m.elements removeObjectForKey:[NSNumber numberWithLong:(long)elementId]];
    dispatch_async(dispatch_get_main_queue(), ^{
        if ([e isKindOfClass:[MKPointAnnotation class]]) {
            [m.mapView removeAnnotation:e];
        } else if ([e conformsToProtocol:@protocol(MKOverlay)]) {
            [m.mapView removeOverlay:e];
        }
    });
}

void com_codename1_maps_MapProviderImpl_nativeRemoveAll___int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId) {
    CN1AppleMap *m = cn1MapFor((int)mapId);
    if (!m) { return; }
    [m.elements removeAllObjects];
    dispatch_async(dispatch_get_main_queue(), ^{
        [m.mapView removeAnnotations:m.mapView.annotations];
        [m.mapView removeOverlays:m.mapView.overlays];
    });
}

JAVA_INT com_codename1_maps_MapProviderImpl_nativeScreenX___int_double_double_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_DOUBLE lat, JAVA_DOUBLE lon) {
    CN1AppleMap *m = cn1MapFor((int)mapId);
    if (!m) { return 0; }
    CGPoint p = [m.mapView convertCoordinate:CLLocationCoordinate2DMake(lat, lon) toPointToView:m.mapView];
    return (JAVA_INT)p.x;
}

JAVA_INT com_codename1_maps_MapProviderImpl_nativeScreenY___int_double_double_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_DOUBLE lat, JAVA_DOUBLE lon) {
    CN1AppleMap *m = cn1MapFor((int)mapId);
    if (!m) { return 0; }
    CGPoint p = [m.mapView convertCoordinate:CLLocationCoordinate2DMake(lat, lon) toPointToView:m.mapView];
    return (JAVA_INT)p.y;
}

JAVA_DOUBLE com_codename1_maps_MapProviderImpl_nativeLat___int_int_int_R_double(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_INT x, JAVA_INT y) {
    CN1AppleMap *m = cn1MapFor((int)mapId);
    if (!m) { return 0; }
    CLLocationCoordinate2D c = [m.mapView convertPoint:CGPointMake(x, y) toCoordinateFromView:m.mapView];
    return c.latitude;
}

JAVA_DOUBLE com_codename1_maps_MapProviderImpl_nativeLon___int_int_int_R_double(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_INT x, JAVA_INT y) {
    CN1AppleMap *m = cn1MapFor((int)mapId);
    if (!m) { return 0; }
    CLLocationCoordinate2D c = [m.mapView convertPoint:CGPointMake(x, y) toCoordinateFromView:m.mapView];
    return c.longitude;
}

void com_codename1_maps_MapProviderImpl_nativeSetShowMyLocation___int_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_BOOLEAN show) {
    CN1AppleMap *m = cn1MapFor((int)mapId);
    if (m) {
        dispatch_async(dispatch_get_main_queue(), ^{ m.mapView.showsUserLocation = show ? YES : NO; });
    }
}

void com_codename1_maps_MapProviderImpl_nativeSetRotateEnabled___int_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_BOOLEAN enabled) {
    CN1AppleMap *m = cn1MapFor((int)mapId);
    if (m) {
        // MKMapView.rotateEnabled is unavailable on tvOS (the Apple TV map has
        // no user-driven rotation gesture), so this is a no-op there.
#if !TARGET_OS_TV
        dispatch_async(dispatch_get_main_queue(), ^{ m.mapView.rotateEnabled = enabled ? YES : NO; });
#endif
    }
}

void com_codename1_maps_MapProviderImpl_nativeSetMapType___int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT mapId, JAVA_INT type) {
    CN1AppleMap *m = cn1MapFor((int)mapId);
    if (!m) { return; }
    MKMapType t = MKMapTypeStandard;
    if (type == 1) { t = MKMapTypeSatellite; }
    else if (type == 2) { t = MKMapTypeHybrid; }
    dispatch_async(dispatch_get_main_queue(), ^{ m.mapView.mapType = t; });
}

#endif
