/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

#import "CodenameOne_GLViewController.h"
#import "CN1SmartHome.h"

#ifdef CN1_INCLUDE_HOMEKIT

#import <HomeKit/HomeKit.h>
#include "com_codename1_impl_ios_IOSHomeCallbacks.h"

// ---------------------------------------------------------------------
// Everything HomeKit happens on the main queue
//
// HMHomeManager must be created on the main thread, its delegate callbacks
// arrive there, and Apple documents no thread safety for HMHome, HMAccessory
// or HMCharacteristic. So every one of those objects is touched on main and
// nowhere else.
//
// That collides with the SPI, whose graph getters are synchronous and are
// called from the Codename One EDT. Rather than hop and block -- which
// deadlocks the moment the main thread is itself waiting on the EDT -- the
// bridge keeps an ENCODED SNAPSHOT of the graph, rebuilt on main whenever the
// home manager says something moved. The getters read strings out of that
// snapshot under a lock and never see a HomeKit object at all.
//
// It also happens to be what the Java side already assumes: com.codename1.home
// hands out immutable snapshots and re-fetches on a structure change, for its
// own reasons.
// ---------------------------------------------------------------------

@class CN1HomeDelegate;

static HMHomeManager *cn1homeManager = nil;
static CN1HomeDelegate *cn1homeDelegate = nil;

// The encoded graph, written on main and read from the EDT.
static NSLock *cn1homeSnapshotLock = nil;
static NSString *cn1homeStructuresLine = nil;
static NSMutableDictionary *cn1homeRoomsBy = nil;
static NSMutableDictionary *cn1homeZonesBy = nil;
static NSMutableDictionary *cn1homeAccessoriesBy = nil;
static NSMutableDictionary *cn1homeServicesBy = nil;
static NSMutableDictionary *cn1homeTraitsBy = nil;
static NSMutableDictionary *cn1homeScenesBy = nil;
static NSMutableDictionary *cn1homeSceneActionsBy = nil;

// Live HomeKit objects, touched on main only.
static NSMutableDictionary *cn1homeAccessoryObjects = nil;
static NSMutableDictionary *cn1homeHomeObjects = nil;

// subscriptionId -> NSSet of "accessoryId\tserviceId\ttraitId" keys.
static NSMutableDictionary *cn1homeWatches = nil;
// Changes gathered while nothing drained them, subscriptionId -> NSMutableArray
// of encoded reading records. HomeKit pushes, so this is normally empty; it
// exists because the SPI is shared with backends that do not push.
static NSMutableDictionary *cn1homeUndelivered = nil;

// The last value drainChanges() saw for a watched characteristic that cannot
// notify, keyed "subscriptionId \t accessoryId \t serviceId \t traitId".
// Without it a drain would report every polled trait as changed every time.
static NSMutableDictionary *cn1homeLastPolled = nil;

// The request id of a start() that is waiting for the first
// homeManagerDidUpdateHomes:. Zero when nothing is waiting.
static JAVA_INT cn1homePendingStart = 0;

// The request id of a requestAuthorization() that is waiting for the user to
// answer the system prompt. Zero when nothing is waiting.
static JAVA_INT cn1homePendingAuth = 0;

static BOOL cn1homeHomesLoaded = NO;

// ---------------------------------------------------------------------
// String helpers
// ---------------------------------------------------------------------

/// Makes a field safe to put in a record.
///
/// A user's accessory name is the only field carrying arbitrary text, and a
/// tab in it would split one record into two while a newline would split one
/// record list into two. Both become spaces: a name that loses a tab is worth
/// less than a record that survives.
static NSString *cn1homeSanitize(NSString *value) {
    if (value == nil) {
        return @"";
    }
    NSString *out = [value stringByReplacingOccurrencesOfString:@"\t"
                                                     withString:@" "];
    out = [out stringByReplacingOccurrencesOfString:@"\n" withString:@" "];
    out = [out stringByReplacingOccurrencesOfString:@"\r" withString:@" "];
    return out;
}

static NSString *cn1homeJoinFields(NSArray *fields) {
    NSMutableString *out = [NSMutableString string];
    for (NSUInteger i = 0; i < [fields count]; i++) {
        if (i > 0) {
            [out appendString:@"\t"];
        }
        [out appendString:cn1homeSanitize([fields objectAtIndex:i])];
    }
    return out;
}

static NSString *cn1homeJoinRecords(NSArray *records) {
    return [records componentsJoinedByString:@"\n"];
}

static NSString *cn1homeFlag(BOOL value) {
    return value ? @"1" : @"0";
}

static NSArray *cn1homeSplit(NSString *joined) {
    if (joined == nil || [joined length] == 0) {
        return [NSArray array];
    }
    return [joined componentsSeparatedByString:@"\n"];
}

/// Encodes a failure the way HomeWire.decodeError reads it: the HomeError
/// NAME, never its ordinal. A port built against a different version of that
/// enum would otherwise map every error past an inserted constant onto the
/// wrong one, and a mis-mapped authorization failure is indistinguishable from
/// a mis-mapped timeout to everything downstream.
static NSString *cn1homeError(NSString *name, NSError *err) {
    NSString *message = err == nil ? @"" : [err localizedDescription];
    return [NSString stringWithFormat:@"%@\t%@", name,
            cn1homeSanitize(message)];
}

/// The portable HomeError name for a HomeKit error.
///
/// The distinction that matters is recoverable versus not: an unreachable
/// accessory is worth retrying and a refused authorization is not, and an app
/// that treats them alike either retries forever or gives up on a light that
/// was merely asleep.
static NSString *cn1homeErrorName(NSError *err) {
    if (err == nil) {
        return @"UNKNOWN";
    }
    if (![[err domain] isEqualToString:HMErrorDomain]) {
        return @"UNKNOWN";
    }
    switch ([err code]) {
        case HMErrorCodeAccessoryNotReachable:
        case HMErrorCodeBridgedAccessoryNotReachable:
        case HMErrorCodeAccessoryIsBlocked:
            return @"ACCESSORY_UNREACHABLE";
        case HMErrorCodeNotFound:
            return @"ACCESSORY_NOT_FOUND";
        case HMErrorCodeInsufficientPrivileges:
        case HMErrorCodeAccessDenied:
            return @"UNAUTHORIZED";
        case HMErrorCodeUserDeclinedAddingUser:
        case HMErrorCodeUserDeclinedInvite:
        case HMErrorCodeOperationCancelled:
            return @"USER_CANCELED";
        case HMErrorCodeReadOnlyCharacteristic:
            return @"READ_ONLY_TRAIT";
        case HMErrorCodeWriteOnlyCharacteristic:
            return @"WRITE_ONLY_TRAIT";
        case HMErrorCodeNotificationNotSupported:
        case HMErrorCodeIncompatibleAccessory:
            return @"TRAIT_NOT_SUPPORTED";
        case HMErrorCodeInvalidValueType:
        case HMErrorCodeValueLowerThanMinimum:
        case HMErrorCodeValueHigherThanMaximum:
        case HMErrorCodeStringLongerThanMaximum:
        case HMErrorCodeStringShorterThanMinimum:
            return @"VALUE_OUT_OF_RANGE";
        case HMErrorCodeOperationTimedOut:
        case HMErrorCodeTimedOutWaitingForAccessory:
        case HMErrorCodeAccessoryPoweredOff:
            return @"TIMEOUT";
        case HMErrorCodeOperationInProgress:
        case HMErrorCodeActionSetExecutionInProgress:
        case HMErrorCodeCloudDataSyncInProgress:
            return @"BUSY";
        default:
            return @"UNKNOWN";
    }
}

// ---------------------------------------------------------------------
// The canonical trait table
//
// The ONLY place a HomeKit characteristic type appears. Nothing above this
// line and nothing in Java ever sees one; a test parses this table and the
// Java Trait constants and fails the build when they drift.
//
// Each entry carries the characteristic type, the portable value kind, the
// TraitUnit wire id, and a conversion opcode. The opcode exists because
// roughly a third of these traits are NOT a straight copy: two run in the
// opposite direction on the two backends, several are scaled, and every
// enum needs its own table.
// ---------------------------------------------------------------------

// Conversion opcodes.
#define CN1_HC_BOOL 0            // plain boolean
#define CN1_HC_DOUBLE 1          // plain number in the declared unit
#define CN1_HC_INT 2             // plain whole number
#define CN1_HC_BOOL_INVERTED 3   // HomeKit 0 means true here (contact sensor)
#define CN1_HC_BOOL_FROM_INT 4   // HomeKit uint8 0/1 for a portable boolean
#define CN1_HC_TILT 5            // HomeKit -90..90 degrees, portable 0..100 %
#define CN1_HC_LOCK_CURRENT 6
#define CN1_HC_LOCK_TARGET 7
#define CN1_HC_HEATCOOL 8        // identical ordinals, current and target
#define CN1_HC_DOOR 9            // identical ordinals, current and target
#define CN1_HC_POSITION 10
#define CN1_HC_AIR_QUALITY 11
#define CN1_HC_CHARGING 12
#define CN1_HC_ALARM 13
#define CN1_HC_FAN_MODE 14

// TraitUnit wire ids, from com.codename1.home.TraitUnit#getWireId().
#define CN1_HU_NONE 0
#define CN1_HU_PERCENT 1
#define CN1_HU_CELSIUS 2
#define CN1_HU_ARC_DEGREE 4
#define CN1_HU_MIRED 5
#define CN1_HU_LUX 6
#define CN1_HU_PPM 7
#define CN1_HU_MICROGRAM 9

typedef struct {
    const char *traitId;
    __unsafe_unretained NSString *characteristicType;
    int kind;
    int unitWireId;
    int conversion;
} CN1HomeTraitEntry;

static NSDictionary *cn1homeTraitTable(void) {
    static NSDictionary *table = nil;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        NSMutableDictionary *m = [NSMutableDictionary dictionary];
#define CN1_HOME_TRAIT(id, ct, kind, unit, conv) \
        [m setObject:[NSArray arrayWithObjects:(ct), \
                      [NSNumber numberWithInt:(kind)], \
                      [NSNumber numberWithInt:(unit)], \
                      [NSNumber numberWithInt:(conv)], nil] \
              forKey:(id)];

        // power
        CN1_HOME_TRAIT(@"on_off", HMCharacteristicTypePowerState,
                       CN1_HOME_KIND_BOOLEAN, CN1_HU_NONE, CN1_HC_BOOL)
        CN1_HOME_TRAIT(@"outlet_in_use", HMCharacteristicTypeOutletInUse,
                       CN1_HOME_KIND_BOOLEAN, CN1_HU_NONE, CN1_HC_BOOL)

        // lighting
        CN1_HOME_TRAIT(@"brightness", HMCharacteristicTypeBrightness,
                       CN1_HOME_KIND_DOUBLE, CN1_HU_PERCENT, CN1_HC_DOUBLE)
        CN1_HOME_TRAIT(@"hue", HMCharacteristicTypeHue,
                       CN1_HOME_KIND_DOUBLE, CN1_HU_ARC_DEGREE, CN1_HC_DOUBLE)
        CN1_HOME_TRAIT(@"saturation", HMCharacteristicTypeSaturation,
                       CN1_HOME_KIND_DOUBLE, CN1_HU_PERCENT, CN1_HC_DOUBLE)
        CN1_HOME_TRAIT(@"color_temperature",
                       HMCharacteristicTypeColorTemperature,
                       CN1_HOME_KIND_DOUBLE, CN1_HU_MIRED, CN1_HC_DOUBLE)
        CN1_HOME_TRAIT(@"current_light_level",
                       HMCharacteristicTypeCurrentLightLevel,
                       CN1_HOME_KIND_DOUBLE, CN1_HU_LUX, CN1_HC_DOUBLE)

        // climate
        CN1_HOME_TRAIT(@"current_temperature",
                       HMCharacteristicTypeCurrentTemperature,
                       CN1_HOME_KIND_DOUBLE, CN1_HU_CELSIUS, CN1_HC_DOUBLE)
        CN1_HOME_TRAIT(@"target_temperature",
                       HMCharacteristicTypeTargetTemperature,
                       CN1_HOME_KIND_DOUBLE, CN1_HU_CELSIUS, CN1_HC_DOUBLE)
        CN1_HOME_TRAIT(@"target_heating_temperature",
                       HMCharacteristicTypeHeatingThreshold,
                       CN1_HOME_KIND_DOUBLE, CN1_HU_CELSIUS, CN1_HC_DOUBLE)
        CN1_HOME_TRAIT(@"target_cooling_temperature",
                       HMCharacteristicTypeCoolingThreshold,
                       CN1_HOME_KIND_DOUBLE, CN1_HU_CELSIUS, CN1_HC_DOUBLE)
        CN1_HOME_TRAIT(@"current_heating_cooling",
                       HMCharacteristicTypeCurrentHeatingCooling,
                       CN1_HOME_KIND_ENUM, CN1_HU_NONE, CN1_HC_HEATCOOL)
        CN1_HOME_TRAIT(@"target_heating_cooling",
                       HMCharacteristicTypeTargetHeatingCooling,
                       CN1_HOME_KIND_ENUM, CN1_HU_NONE, CN1_HC_HEATCOOL)
        CN1_HOME_TRAIT(@"current_humidity",
                       HMCharacteristicTypeCurrentRelativeHumidity,
                       CN1_HOME_KIND_DOUBLE, CN1_HU_PERCENT, CN1_HC_DOUBLE)
        CN1_HOME_TRAIT(@"target_humidity",
                       HMCharacteristicTypeTargetRelativeHumidity,
                       CN1_HOME_KIND_DOUBLE, CN1_HU_PERCENT, CN1_HC_DOUBLE)

        // locks
        CN1_HOME_TRAIT(@"lock_state",
                       HMCharacteristicTypeCurrentLockMechanismState,
                       CN1_HOME_KIND_ENUM, CN1_HU_NONE, CN1_HC_LOCK_CURRENT)
        CN1_HOME_TRAIT(@"target_lock_state",
                       HMCharacteristicTypeTargetLockMechanismState,
                       CN1_HOME_KIND_ENUM, CN1_HU_NONE, CN1_HC_LOCK_TARGET)

        // doors
        CN1_HOME_TRAIT(@"door_state", HMCharacteristicTypeCurrentDoorState,
                       CN1_HOME_KIND_ENUM, CN1_HU_NONE, CN1_HC_DOOR)
        CN1_HOME_TRAIT(@"target_door_state",
                       HMCharacteristicTypeTargetDoorState,
                       CN1_HOME_KIND_ENUM, CN1_HU_NONE, CN1_HC_DOOR)
        CN1_HOME_TRAIT(@"obstruction_detected",
                       HMCharacteristicTypeObstructionDetected,
                       CN1_HOME_KIND_BOOLEAN, CN1_HU_NONE, CN1_HC_BOOL)

        // window coverings. HomeKit's position runs the same way this API's
        // does -- 100 is fully open -- which is why the canonical convention
        // is HomeKit's and the Android bridge is the one that inverts.
        CN1_HOME_TRAIT(@"covering_position",
                       HMCharacteristicTypeCurrentPosition,
                       CN1_HOME_KIND_DOUBLE, CN1_HU_PERCENT, CN1_HC_DOUBLE)
        CN1_HOME_TRAIT(@"target_covering_position",
                       HMCharacteristicTypeTargetPosition,
                       CN1_HOME_KIND_DOUBLE, CN1_HU_PERCENT, CN1_HC_DOUBLE)
        CN1_HOME_TRAIT(@"covering_tilt",
                       HMCharacteristicTypeCurrentHorizontalTilt,
                       CN1_HOME_KIND_DOUBLE, CN1_HU_PERCENT, CN1_HC_TILT)
        CN1_HOME_TRAIT(@"target_covering_tilt",
                       HMCharacteristicTypeTargetHorizontalTilt,
                       CN1_HOME_KIND_DOUBLE, CN1_HU_PERCENT, CN1_HC_TILT)
        CN1_HOME_TRAIT(@"covering_motion", HMCharacteristicTypePositionState,
                       CN1_HOME_KIND_ENUM, CN1_HU_NONE, CN1_HC_POSITION)

        // sensors
        CN1_HOME_TRAIT(@"motion_detected",
                       HMCharacteristicTypeMotionDetected,
                       CN1_HOME_KIND_BOOLEAN, CN1_HU_NONE, CN1_HC_BOOL)
        CN1_HOME_TRAIT(@"occupancy_detected",
                       HMCharacteristicTypeOccupancyDetected,
                       CN1_HOME_KIND_BOOLEAN, CN1_HU_NONE,
                       CN1_HC_BOOL_FROM_INT)
        // Inverted: HomeKit's ContactState is 0 for "detected", meaning the
        // door is CLOSED, and this API's boolean is true for closed.
        CN1_HOME_TRAIT(@"contact_detected", HMCharacteristicTypeContactState,
                       CN1_HOME_KIND_BOOLEAN, CN1_HU_NONE,
                       CN1_HC_BOOL_INVERTED)
        CN1_HOME_TRAIT(@"leak_detected", HMCharacteristicTypeLeakDetected,
                       CN1_HOME_KIND_BOOLEAN, CN1_HU_NONE,
                       CN1_HC_BOOL_FROM_INT)
        CN1_HOME_TRAIT(@"smoke_detected", HMCharacteristicTypeSmokeDetected,
                       CN1_HOME_KIND_ENUM, CN1_HU_NONE, CN1_HC_ALARM)
        CN1_HOME_TRAIT(@"co_detected",
                       HMCharacteristicTypeCarbonMonoxideDetected,
                       CN1_HOME_KIND_ENUM, CN1_HU_NONE, CN1_HC_ALARM)
        CN1_HOME_TRAIT(@"co_level", HMCharacteristicTypeCarbonMonoxideLevel,
                       CN1_HOME_KIND_DOUBLE, CN1_HU_PPM, CN1_HC_DOUBLE)
        CN1_HOME_TRAIT(@"co2_level", HMCharacteristicTypeCarbonDioxideLevel,
                       CN1_HOME_KIND_DOUBLE, CN1_HU_PPM, CN1_HC_DOUBLE)
        CN1_HOME_TRAIT(@"air_quality", HMCharacteristicTypeAirQuality,
                       CN1_HOME_KIND_ENUM, CN1_HU_NONE, CN1_HC_AIR_QUALITY)
        CN1_HOME_TRAIT(@"pm2_5_density", HMCharacteristicTypePM2_5Density,
                       CN1_HOME_KIND_DOUBLE, CN1_HU_MICROGRAM, CN1_HC_DOUBLE)
        CN1_HOME_TRAIT(@"pm10_density", HMCharacteristicTypePM10Density,
                       CN1_HOME_KIND_DOUBLE, CN1_HU_MICROGRAM, CN1_HC_DOUBLE)
        CN1_HOME_TRAIT(@"voc_density",
                       HMCharacteristicTypeVolatileOrganicCompoundDensity,
                       CN1_HOME_KIND_DOUBLE, CN1_HU_MICROGRAM, CN1_HC_DOUBLE)

        // power source
        CN1_HOME_TRAIT(@"battery_level", HMCharacteristicTypeBatteryLevel,
                       CN1_HOME_KIND_DOUBLE, CN1_HU_PERCENT, CN1_HC_DOUBLE)
        CN1_HOME_TRAIT(@"battery_charging", HMCharacteristicTypeChargingState,
                       CN1_HOME_KIND_ENUM, CN1_HU_NONE, CN1_HC_CHARGING)
        CN1_HOME_TRAIT(@"battery_low", HMCharacteristicTypeStatusLowBattery,
                       CN1_HOME_KIND_BOOLEAN, CN1_HU_NONE,
                       CN1_HC_BOOL_FROM_INT)

        // fans
        CN1_HOME_TRAIT(@"fan_speed", HMCharacteristicTypeRotationSpeed,
                       CN1_HOME_KIND_DOUBLE, CN1_HU_PERCENT, CN1_HC_DOUBLE)
        CN1_HOME_TRAIT(@"fan_mode", HMCharacteristicTypeTargetFanState,
                       CN1_HOME_KIND_ENUM, CN1_HU_NONE, CN1_HC_FAN_MODE)

        // speakers
        CN1_HOME_TRAIT(@"volume", HMCharacteristicTypeVolume,
                       CN1_HOME_KIND_DOUBLE, CN1_HU_PERCENT, CN1_HC_DOUBLE)
        CN1_HOME_TRAIT(@"mute", HMCharacteristicTypeMute,
                       CN1_HOME_KIND_BOOLEAN, CN1_HU_NONE, CN1_HC_BOOL)
#undef CN1_HOME_TRAIT
        // Retained, not the autoreleased literal. A static holding an
        // autoreleased object dangles the moment the pool drains, and under
        // ParparVM the resulting EXC_BAD_ACCESS surfaces as a bogus Java
        // exception with nothing pointing back here.
        table = [[NSDictionary dictionaryWithDictionary:m] retain];
    });
    return table;
}

/// The reverse table: characteristic type -> portable trait id. Built from the
/// forward one so the two cannot disagree.
static NSDictionary *cn1homeTraitByCharacteristic(void) {
    static NSDictionary *reverse = nil;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        NSMutableDictionary *m = [NSMutableDictionary dictionary];
        NSDictionary *forward = cn1homeTraitTable();
        for (NSString *traitId in forward) {
            NSArray *entry = [forward objectForKey:traitId];
            NSString *ct = [entry objectAtIndex:0];
            // First wins. Two portable traits can name one characteristic --
            // the tilt pair does not, but a future addition might -- and the
            // reverse direction only has to answer "which trait is this", so
            // the first is as good as any and stable across launches.
            if ([m objectForKey:ct] == nil) {
                [m setObject:traitId forKey:ct];
            }
        }
        reverse = [[NSDictionary dictionaryWithDictionary:m] retain];
    });
    return reverse;
}

/// Declared ahead of its definition below, because the per-service resolver
/// needs a trait's primary characteristic to know whether an alternate
/// applies.
static NSArray *cn1homeEntryFor(NSString *traitId);

/// Likewise: the delegate answers a pending start with the same availability
/// this reports, and it is defined further down with the rest of the native
/// entry points.
JAVA_INT com_codename1_impl_ios_IOSNative_homeAvailability___R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me);

/// The alternates cn1homeFindCharacteristic accepts when a service does not
/// carry the primary characteristic: characteristic type -> portable trait.
///
/// Applied per service rather than folded into the reverse table, because a
/// covering can carry BOTH tilt axes. Aliased globally, a vertical-axis
/// notification arrives as a horizontal covering_tilt change and overwrites
/// the value that was just read from the axis the read and write paths both
/// prefer.
static NSDictionary *cn1homeAlternateTraitByCharacteristic(void) {
    static NSDictionary *alternates = nil;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        alternates = [[NSDictionary alloc] initWithObjectsAndKeys:
            @"on_off", HMCharacteristicTypeActive,
            @"covering_tilt", HMCharacteristicTypeCurrentVerticalTilt,
            @"target_covering_tilt", HMCharacteristicTypeTargetVerticalTilt,
            nil];
    });
    return alternates;
}

/// The portable trait a characteristic stands for on the service it belongs
/// to, or nil.
///
/// An alternate answers only when the service does not carry the primary
/// characteristic for that trait -- which is the same rule
/// cn1homeFindCharacteristic reads by, so the graph describes exactly what
/// can be read and written and nothing else.
static NSString *cn1homeTraitFor(HMService *service, HMCharacteristic *c) {
    NSString *type = [c characteristicType];
    NSString *traitId = [cn1homeTraitByCharacteristic() objectForKey:type];
    if (traitId != nil) {
        return traitId;
    }
    traitId = [cn1homeAlternateTraitByCharacteristic() objectForKey:type];
    if (traitId == nil) {
        return nil;
    }
    NSArray *entry = cn1homeEntryFor(traitId);
    NSString *primary = entry == nil ? nil : [entry objectAtIndex:0];
    for (HMCharacteristic *other in [service characteristics]) {
        if ([[other characteristicType] isEqualToString:primary]) {
            return nil;
        }
    }
    return traitId;
}

static NSArray *cn1homeEntryFor(NSString *traitId) {
    return [cn1homeTraitTable() objectForKey:traitId];
}

// ---------------------------------------------------------------------
// Value conversion
// ---------------------------------------------------------------------

/// Maps a HomeKit value onto the portable numeric component.
///
/// Answers NO when there is nothing to report, which is a real outcome rather
/// than an error: HomeKit hands back nil for a characteristic it has not read
/// yet, and a light in white mode has no meaningful hue.
static BOOL cn1homeToPortable(int conversion, id value, double *out,
                              int *rawOut, BOOL *hasRawOut) {
    *hasRawOut = NO;
    *rawOut = 0;
    if (value == nil || value == [NSNull null]) {
        return NO;
    }
    if (![value isKindOfClass:[NSNumber class]]) {
        // Only TLV8 and string characteristics land here, and neither is in
        // the trait table -- see TraitValueKind's note on why there is no TLV
        // kind. Reported as absent rather than coerced.
        return NO;
    }
    NSNumber *n = (NSNumber *) value;
    switch (conversion) {
        case CN1_HC_BOOL:
            *out = [n boolValue] ? 1 : 0;
            return YES;
        case CN1_HC_BOOL_FROM_INT:
            *out = [n intValue] != 0 ? 1 : 0;
            return YES;
        case CN1_HC_BOOL_INVERTED:
            // HomeKit's contact sensor: 0 is "detected", which means closed.
            *out = [n intValue] == 0 ? 1 : 0;
            return YES;
        case CN1_HC_INT:
            *out = [n intValue];
            return YES;
        case CN1_HC_TILT: {
            // HomeKit tilts in degrees over -90..90; the portable trait is a
            // percentage, because that is Matter's model and one axis is all
            // this release claims. The degree range is published on the
            // constraint so a UI can still label it.
            double degrees = [n doubleValue];
            *out = (degrees + 90.0) * 100.0 / 180.0;
            return YES;
        }
        case CN1_HC_LOCK_CURRENT: {
            // HomeKit 0 unsecured, 1 secured, 2 jammed, 3 unknown.
            // Portable   0 SECURED, 1 UNSECURED, 2 PARTIALLY_LOCKED,
            //            3 JAMMED, 4 UNKNOWN.
            int hk = [n intValue];
            *rawOut = hk;
            *hasRawOut = YES;
            switch (hk) {
                case 0: *out = 1; break;
                case 1: *out = 0; break;
                case 2: *out = 3; break;
                default: *out = 4; break;
            }
            return YES;
        }
        case CN1_HC_LOCK_TARGET: {
            int hk = [n intValue];
            *out = hk == 1 ? 0 : 1;
            return YES;
        }
        case CN1_HC_HEATCOOL:
        case CN1_HC_DOOR:
            // The ordinals coincide, and that is checked rather than assumed:
            // HeatingCoolingMode and DoorState were both defined in HomeKit's
            // order for exactly this reason.
            *out = [n intValue];
            return YES;
        case CN1_HC_POSITION: {
            // HomeKit 0 decreasing (closing), 1 increasing (opening),
            // 2 stopped. Portable 0 STOPPED, 1 OPENING, 2 CLOSING.
            int hk = [n intValue];
            switch (hk) {
                case 0: *out = 2; break;
                case 1: *out = 1; break;
                default: *out = 0; break;
            }
            return YES;
        }
        case CN1_HC_AIR_QUALITY: {
            // HomeKit 0 unknown, 1 excellent, 2 good, 3 fair, 4 inferior,
            // 5 poor -- six levels against Matter's seven. Portable follows
            // Matter, so these line up one-for-one and EXTREMELY_POOR is
            // simply unreachable here. The raw value goes along so an app
            // that wants HomeKit's own scale can have it.
            int hk = [n intValue];
            *rawOut = hk;
            *hasRawOut = YES;
            *out = hk < 0 ? 0 : (hk > 5 ? 0 : hk);
            return YES;
        }
        case CN1_HC_CHARGING: {
            // HomeKit 0 not charging, 1 charging, 2 not chargeable.
            // Portable 0 UNKNOWN, 1 NOT_CHARGING, 2 CHARGING, 3 FULL,
            //          4 NOT_CHARGEABLE. FULL is unreachable here, which
            //          ChargingState's javadoc says: HomeKit cannot tell a
            //          full battery on a charger from one running down.
            int hk = [n intValue];
            switch (hk) {
                case 0: *out = 1; break;
                case 1: *out = 2; break;
                case 2: *out = 4; break;
                default: *out = 0; break;
            }
            return YES;
        }
        case CN1_HC_ALARM: {
            // Two-state on HomeKit, so WARNING is unreachable.
            *out = [n intValue] != 0 ? 2 : 0;
            return YES;
        }
        case CN1_HC_FAN_MODE: {
            // HomeKit 0 manual, 1 auto. Portable ON=4, AUTO=5. LOW, MEDIUM
            // and HIGH are never read back -- FanMode's javadoc says so, and
            // an app that needs the speed should read fan_speed.
            *out = [n intValue] == 1 ? 5 : 4;
            return YES;
        }
        default:
            *out = [n doubleValue];
            return YES;
    }
}

/// Maps a portable value onto what HomeKit will accept, or nil when the
/// portable value has no HomeKit meaning.
static id cn1homeToHomeKit(int conversion, double numeric) {
    switch (conversion) {
        case CN1_HC_BOOL:
            return [NSNumber numberWithBool:numeric != 0];
        case CN1_HC_BOOL_FROM_INT:
            return [NSNumber numberWithInt:numeric != 0 ? 1 : 0];
        case CN1_HC_BOOL_INVERTED:
            return [NSNumber numberWithInt:numeric != 0 ? 0 : 1];
        case CN1_HC_INT:
            return [NSNumber numberWithInt:(int) numeric];
        case CN1_HC_TILT:
            return [NSNumber numberWithDouble:
                    (numeric * 180.0 / 100.0) - 90.0];
        case CN1_HC_LOCK_TARGET: {
            // Portable SECURED=0 -> HomeKit 1, UNSECURED=1 -> HomeKit 0.
            //
            // Anything else is refused rather than folded into one of the
            // two. LockState.isWritable() already says only those two may be
            // written, but a value that gets past it must not open a door:
            // "not SECURED, so unlock" is the single worst reading of a bad
            // request this API can make.
            int portable = (int) numeric;
            if (portable != 0 && portable != 1) {
                return nil;
            }
            return [NSNumber numberWithInt:portable == 0 ? 1 : 0];
        }
        case CN1_HC_HEATCOOL: {
            int portable = (int) numeric;
            // OTHER is a value this API can report and cannot ask for -- it
            // exists so a Matter mode HomeKit cannot express is not
            // misreported as OFF. Refusing the write is what
            // HeatingCoolingMode.isWritable() promises.
            if (portable < 0 || portable > 3) {
                return nil;
            }
            return [NSNumber numberWithInt:portable];
        }
        case CN1_HC_DOOR: {
            int portable = (int) numeric;
            if (portable != 0 && portable != 1) {
                return nil;
            }
            return [NSNumber numberWithInt:portable];
        }
        case CN1_HC_FAN_MODE: {
            int portable = (int) numeric;
            // AUTO and SMART both ask HomeKit for auto; everything else is
            // manual. The speed half of LOW/MEDIUM/HIGH is applied separately
            // by the write path, which has the service in hand, and OFF never
            // reaches here at all -- that path writes the fan's power
            // characteristic instead, because no mode value means "stopped".
            return [NSNumber numberWithInt:(portable >= 5) ? 1 : 0];
        }
        case CN1_HC_LOCK_CURRENT:
        case CN1_HC_POSITION:
        case CN1_HC_AIR_QUALITY:
        case CN1_HC_CHARGING:
        case CN1_HC_ALARM:
            // Read-only traits. Reaching here means the Java side let a write
            // through that Trait.isReadOnly() should have stopped, so refuse
            // rather than write something arbitrary into an accessory.
            return nil;
        default:
            return [NSNumber numberWithDouble:numeric];
    }
}

/// The portable speed a fan mode implies, or a negative number when the mode
/// says nothing about speed.
static double cn1homeFanModeSpeed(int portableMode) {
    switch (portableMode) {
        case 1: return 33;   // LOW
        case 2: return 66;   // MEDIUM
        case 3: return 100;  // HIGH
        default: return -1;
    }
}

// ---------------------------------------------------------------------
// Category and service-type mapping
// ---------------------------------------------------------------------

/// com.codename1.home.AccessoryCategory ordinal for a HomeKit category.
static int cn1homeCategoryOrdinal(HMAccessoryCategory *category) {
    NSString *t = [category categoryType];
    if (t == nil) {
        return 16; // OTHER
    }
    if ([t isEqualToString:HMAccessoryCategoryTypeLightbulb]) return 0;
    if ([t isEqualToString:HMAccessoryCategoryTypeSwitch]) return 1;
    if ([t isEqualToString:HMAccessoryCategoryTypeProgrammableSwitch]) return 1;
    if ([t isEqualToString:HMAccessoryCategoryTypeOutlet]) return 2;
    if ([t isEqualToString:HMAccessoryCategoryTypeThermostat]) return 3;
    if ([t isEqualToString:HMAccessoryCategoryTypeDoorLock]) return 4;
    if ([t isEqualToString:HMAccessoryCategoryTypeGarageDoorOpener]) return 5;
    if ([t isEqualToString:HMAccessoryCategoryTypeWindowCovering]) return 6;
    if ([t isEqualToString:HMAccessoryCategoryTypeWindow]) return 6;
    if ([t isEqualToString:HMAccessoryCategoryTypeSensor]) return 7;
    if ([t isEqualToString:HMAccessoryCategoryTypeFan]) return 8;
    if ([t isEqualToString:HMAccessoryCategoryTypeAirPurifier]) return 9;
    if ([t isEqualToString:HMAccessoryCategoryTypeIPCamera]) return 12;
    if ([t isEqualToString:HMAccessoryCategoryTypeVideoDoorbell]) return 13;
    if ([t isEqualToString:HMAccessoryCategoryTypeBridge]) return 14;
    if ([t isEqualToString:HMAccessoryCategoryTypeSecuritySystem]) return 15;
    return 16; // OTHER
}

/// com.codename1.home.ServiceType ordinal for a HomeKit service type.
static int cn1homeServiceTypeOrdinal(NSString *t) {
    if (t == nil) {
        return 22; // OTHER
    }
    if ([t isEqualToString:HMServiceTypeLightbulb]) return 0;
    if ([t isEqualToString:HMServiceTypeSwitch]) return 1;
    if ([t isEqualToString:HMServiceTypeOutlet]) return 2;
    if ([t isEqualToString:HMServiceTypeThermostat]) return 3;
    if ([t isEqualToString:HMServiceTypeLockMechanism]) return 4;
    if ([t isEqualToString:HMServiceTypeDoor]) return 5;
    if ([t isEqualToString:HMServiceTypeGarageDoorOpener]) return 6;
    if ([t isEqualToString:HMServiceTypeWindowCovering]) return 7;
    if ([t isEqualToString:HMServiceTypeWindow]) return 7;
    // Both fan services. HomeKit spells the modern one -- the "fan v2"
    // profile, the service that carries Active rather than PowerState --
    // HMServiceTypeVentilationFan; there is no HMServiceTypeFanV2 to look
    // for. Falling through to OTHER left such a fan controllable and
    // unrecognizable at the same time: the trait code handles its Active
    // characteristic, but a UI grouping or picking icons by service type
    // could not see it was a fan.
    if ([t isEqualToString:HMServiceTypeFan]) return 8;
    if ([t isEqualToString:HMServiceTypeVentilationFan]) return 8;
    if ([t isEqualToString:HMServiceTypeAirPurifier]) return 9;
    if ([t isEqualToString:HMServiceTypeMotionSensor]) return 10;
    if ([t isEqualToString:HMServiceTypeOccupancySensor]) return 11;
    if ([t isEqualToString:HMServiceTypeContactSensor]) return 12;
    if ([t isEqualToString:HMServiceTypeTemperatureSensor]) return 13;
    if ([t isEqualToString:HMServiceTypeHumiditySensor]) return 14;
    if ([t isEqualToString:HMServiceTypeLightSensor]) return 15;
    if ([t isEqualToString:HMServiceTypeSmokeSensor]) return 16;
    if ([t isEqualToString:HMServiceTypeCarbonMonoxideSensor]) return 17;
    if ([t isEqualToString:HMServiceTypeLeakSensor]) return 18;
    if ([t isEqualToString:HMServiceTypeAirQualitySensor]) return 19;
    if ([t isEqualToString:HMServiceTypeBattery]) return 20;
    if ([t isEqualToString:HMServiceTypeSpeaker]) return 21;
    return 22; // OTHER
}

/// com.codename1.home.SceneType ordinal for a HomeKit action-set type.
static int cn1homeSceneTypeOrdinal(NSString *t) {
    if (t == nil) {
        return 4; // USER_DEFINED
    }
    if ([t isEqualToString:HMActionSetTypeWakeUp]) return 0;
    if ([t isEqualToString:HMActionSetTypeSleep]) return 1;
    if ([t isEqualToString:HMActionSetTypeHomeArrival]) return 2;
    if ([t isEqualToString:HMActionSetTypeHomeDeparture]) return 3;
    if ([t isEqualToString:HMActionSetTypeTriggerOwned]) return 5;
    return 4; // USER_DEFINED
}

// ---------------------------------------------------------------------
// Lookups, main queue only
// ---------------------------------------------------------------------

static NSString *cn1homeUuid(NSUUID *identifier) {
    return identifier == nil ? @"" : [identifier UUIDString];
}

static HMHome *cn1homeFindHome(NSString *structureId) {
    if (structureId == nil || [structureId length] == 0) {
        // The first home, not the primary one. Apple deprecated
        // HMHomeManager.primaryHome in iOS 16.1 with "no longer supported"
        // and shipped no replacement, so there is no way left to ask which
        // home the user thinks of as theirs. Reporting the first is the same
        // fallback com.codename1.home.SmartHome.getPrimaryStructure() makes,
        // which keeps the two ends agreeing about what "no structure id"
        // means.
        return [[cn1homeManager homes] firstObject];
    }
    for (HMHome *home in [cn1homeManager homes]) {
        if ([cn1homeUuid([home uniqueIdentifier]) isEqualToString:structureId]) {
            return home;
        }
    }
    return nil;
}

static HMAccessory *cn1homeFindAccessory(NSString *accessoryId) {
    return [cn1homeAccessoryObjects objectForKey:accessoryId];
}

static HMService *cn1homeFindService(NSString *accessoryId,
                                     NSString *serviceId) {
    HMAccessory *accessory = cn1homeFindAccessory(accessoryId);
    if (accessory == nil) {
        return nil;
    }
    for (HMService *service in [accessory services]) {
        if ([cn1homeUuid([service uniqueIdentifier])
             isEqualToString:serviceId]) {
            return service;
        }
    }
    return nil;
}

/// Finds the characteristic a portable trait names on a service.
///
/// The tilt fallback is why this is not a one-line lookup: HomeKit models tilt
/// as two characteristics on two axes and this API exposes one, so an
/// accessory that only tilts vertically has to answer a request for the
/// horizontal one. See Trait.COVERING_TILT.
static HMCharacteristic *cn1homeFindCharacteristic(HMService *service,
                                                   NSString *traitId) {
    if (service == nil) {
        return nil;
    }
    NSArray *entry = cn1homeEntryFor(traitId);
    if (entry == nil) {
        return nil;
    }
    NSString *wanted = [entry objectAtIndex:0];
    HMCharacteristic *found = nil;
    for (HMCharacteristic *c in [service characteristics]) {
        if ([[c characteristicType] isEqualToString:wanted]) {
            found = c;
            break;
        }
    }
    if (found != nil) {
        return found;
    }
    NSString *alternate = nil;
    if ([traitId isEqualToString:@"covering_tilt"]) {
        alternate = HMCharacteristicTypeCurrentVerticalTilt;
    } else if ([traitId isEqualToString:@"target_covering_tilt"]) {
        alternate = HMCharacteristicTypeTargetVerticalTilt;
    } else if ([traitId isEqualToString:@"on_off"]) {
        // A fan v2, air purifier or heater-cooler spells power as Active
        // rather than PowerState. Folding both onto one portable trait is
        // deliberate: the split is a HomeKit implementation detail and
        // leaking it would make portable code ask which kind of switch it is
        // looking at.
        alternate = HMCharacteristicTypeActive;
    }
    if (alternate == nil) {
        return nil;
    }
    for (HMCharacteristic *c in [service characteristics]) {
        if ([[c characteristicType] isEqualToString:alternate]) {
            return c;
        }
    }
    return nil;
}

/// A reading record with its timestamp blanked, for comparing two polls.
///
/// The timestamp is field 9 and moves on every read, so whole-record equality
/// would call every poll a change.
static NSString *cn1homeReadingWithoutTimestamp(NSString *record) {
    NSMutableArray *fields = [[[record componentsSeparatedByString:@"\t"]
                               mutableCopy] autorelease];
    if ([fields count] > 9) {
        [fields replaceObjectAtIndex:9 withObject:@""];
    }
    return [fields componentsJoinedByString:@"\t"];
}

static NSString *cn1homeReadingKey(NSString *accessoryId, NSString *serviceId,
                                   NSString *traitId) {
    return [NSString stringWithFormat:@"%@\t%@\t%@", accessoryId, serviceId,
            traitId];
}

/// Encodes one reading the way HomeWire.decodeReading reads it.
/// Whether a trait has no value on this service right now, whatever its
/// characteristic says.
///
/// One case, and it is in the trait's own contract: a thermostat in AUTO has
/// two setpoints and no single target, so TARGET_TEMPERATURE reports nothing.
/// HomeKit still keeps a TargetTemperature characteristic and still answers
/// with a number, and passing that number on is how a one-number thermostat
/// UI shows a target the accessory is not aiming for.
static BOOL cn1homeHasNoValueNow(HMService *service, NSString *traitId) {
    if (![traitId isEqualToString:@"target_temperature"]) {
        return NO;
    }
    HMCharacteristic *mode = cn1homeFindCharacteristic(
        service, @"target_heating_cooling");
    if (mode == nil) {
        return NO;
    }
    id value = [mode value];
    // 3 is HomeKit's auto. Read from whatever it last saw: this is a
    // question about what the thermostat is doing, and a stale answer here
    // is the same staleness the mode's own reading carries.
    return [value isKindOfClass:[NSNumber class]]
            && [value intValue] == 3;
}

/// Encodes a reading, with an explicit "when was this true" stamp.
///
/// Zero means "the backend cannot say", which TraitReading.getTimestampMillis
/// documents and which is the honest answer for a value HomeKit had lying
/// around: stamping a cached value with the current time tells every
/// freshness check that a reading of unknown age was observed just now.
static NSString *cn1homeEncodeReadingAt(NSString *accessoryId,
                                        NSString *serviceId,
                                        NSString *traitId, id value,
                                        NSString *errorName,
                                        NSString *errorMessage,
                                        BOOL known);

static NSString *cn1homeEncodeReading(NSString *accessoryId,
                                      NSString *serviceId, NSString *traitId,
                                      id value, NSString *errorName,
                                      NSString *errorMessage) {
    return cn1homeEncodeReadingAt(accessoryId, serviceId, traitId, value,
                                  errorName, errorMessage, YES);
}

static NSString *cn1homeEncodeReadingAt(NSString *accessoryId,
                                        NSString *serviceId,
                                        NSString *traitId, id value,
                                        NSString *errorName,
                                        NSString *errorMessage,
                                        BOOL known) {
    NSArray *entry = cn1homeEntryFor(traitId);
    int kind = entry == nil ? CN1_HOME_KIND_DOUBLE
            : [[entry objectAtIndex:1] intValue];
    int unit = entry == nil ? CN1_HU_NONE : [[entry objectAtIndex:2] intValue];
    int conversion = entry == nil ? CN1_HC_DOUBLE
            : [[entry objectAtIndex:3] intValue];
    if (errorName != nil) {
        return cn1homeJoinFields([NSArray arrayWithObjects:accessoryId,
                                  serviceId, traitId,
                                  [NSString stringWithFormat:@"%d", kind],
                                  @"0", @"", @"", @"", @"0", @"0", errorName,
                                  errorMessage == nil ? @"" : errorMessage,
                                  nil]);
    }
    double numeric = 0;
    int raw = 0;
    BOOL hasRaw = NO;
    BOOL hasValue = cn1homeToPortable(conversion, value, &numeric, &raw,
                                      &hasRaw);
    return cn1homeJoinFields([NSArray arrayWithObjects:accessoryId, serviceId,
                              traitId,
                              [NSString stringWithFormat:@"%d", kind],
                              [NSString stringWithFormat:@"%f", numeric], @"",
                              [NSString stringWithFormat:@"%d", unit],
                              hasRaw ? [NSString stringWithFormat:@"%d", raw]
                                     : @"",
                              cn1homeFlag(hasValue),
                              known ? [NSString stringWithFormat:@"%lld",
                                       (long long) ([[NSDate date]
                                            timeIntervalSince1970] * 1000)]
                                    : @"0",
                              @"", @"", nil]);
}

// ---------------------------------------------------------------------
// Snapshot building, main queue only
// ---------------------------------------------------------------------

static void cn1homeEncodeTraitsForService(HMAccessory *accessory,
                                          HMService *service,
                                          NSMutableDictionary *into) {
    NSMutableArray *records = [NSMutableArray array];
    NSMutableSet *seen = [NSMutableSet set];
    for (HMCharacteristic *c in [service characteristics]) {
        NSString *traitId = cn1homeTraitFor(service, c);
        if (traitId == nil) {
            // A characteristic outside the canonical vocabulary. Skipped
            // rather than surfaced with its HomeKit identifier: exposing one
            // would put an iOS-only value into a cross-platform API.
            continue;
        }
        if ([seen containsObject:traitId]) {
            continue;
        }
        [seen addObject:traitId];
        NSArray *entry = cn1homeEntryFor(traitId);
        int conversion = [[entry objectAtIndex:3] intValue];
        BOOL readable = [[c properties]
                         containsObject:HMCharacteristicPropertyReadable];
        BOOL writable = [[c properties]
                         containsObject:HMCharacteristicPropertyWritable];
        BOOL notifies = [[c properties]
                         containsObject:HMCharacteristicPropertySupportsEventNotification];
        HMCharacteristicMetadata *meta = [c metadata];
        BOOL hasRange = NO;
        double minimum = 0;
        double maximum = 0;
        double step = 0;
        if (meta != nil && [meta minimumValue] != nil
                && [meta maximumValue] != nil) {
            hasRange = YES;
            minimum = [[meta minimumValue] doubleValue];
            maximum = [[meta maximumValue] doubleValue];
            step = [meta stepValue] == nil ? 0 : [[meta stepValue] doubleValue];
            if (conversion == CN1_HC_TILT) {
                // The accessory's range is in degrees and the portable trait
                // is a percentage, so the bounds have to travel through the
                // same conversion the value does or a slider built from them
                // offers -90 on a 0..100 scale.
                double lo = (minimum + 90.0) * 100.0 / 180.0;
                double hi = (maximum + 90.0) * 100.0 / 180.0;
                minimum = lo;
                maximum = hi;
                step = step * 100.0 / 180.0;
            }
        }
        [records addObject:cn1homeJoinFields(
            [NSArray arrayWithObjects:traitId, cn1homeFlag(readable),
             cn1homeFlag(writable), cn1homeFlag(notifies),
             cn1homeFlag(hasRange),
             [NSString stringWithFormat:@"%f", minimum],
             [NSString stringWithFormat:@"%f", maximum],
             [NSString stringWithFormat:@"%f", step], @"", nil])];
    }
    [into setObject:cn1homeJoinRecords(records)
             forKey:[NSString stringWithFormat:@"%@\t%@",
                     cn1homeUuid([accessory uniqueIdentifier]),
                     cn1homeUuid([service uniqueIdentifier])]];
}

static void cn1homeRebuildSnapshot(void) {
    NSMutableArray *structures = [NSMutableArray array];
    NSMutableDictionary *rooms = [NSMutableDictionary dictionary];
    NSMutableDictionary *zones = [NSMutableDictionary dictionary];
    NSMutableDictionary *accessories = [NSMutableDictionary dictionary];
    NSMutableDictionary *services = [NSMutableDictionary dictionary];
    NSMutableDictionary *traits = [NSMutableDictionary dictionary];
    NSMutableDictionary *scenes = [NSMutableDictionary dictionary];
    NSMutableDictionary *sceneActions = [NSMutableDictionary dictionary];
    NSMutableDictionary *accessoryObjects = [NSMutableDictionary dictionary];
    NSMutableDictionary *homeObjects = [NSMutableDictionary dictionary];

    for (HMHome *home in [cn1homeManager homes]) {
        NSString *homeId = cn1homeUuid([home uniqueIdentifier]);
        [homeObjects setObject:home forKey:homeId];
        // Never primary on iOS. HMHomeManager.primaryHome was deprecated in
        // iOS 16.1 as "no longer supported" with nothing to replace it, so
        // the platform genuinely cannot answer which home is the default.
        // Reporting false for every home rather than guessing lets
        // getPrimaryStructure() fall back to the first, which is documented
        // and is at least not a claim about what the user prefers.
        BOOL isPrimary = NO;
        BOOL owner = [home homeAccessControlForUser:[home currentUser]] != nil
                ? [[home homeAccessControlForUser:[home currentUser]]
                   isAdministrator] : NO;
        [structures addObject:cn1homeJoinFields(
            [NSArray arrayWithObjects:homeId, [home name],
             cn1homeFlag(isPrimary), cn1homeFlag(owner),
             cn1homeFlag(owner), nil])];

        NSMutableArray *roomRecords = [NSMutableArray array];
        for (HMRoom *room in [home rooms]) {
            [roomRecords addObject:cn1homeJoinFields(
                [NSArray arrayWithObjects:cn1homeUuid([room uniqueIdentifier]),
                 [room name], nil])];
        }
        [rooms setObject:cn1homeJoinRecords(roomRecords) forKey:homeId];

        NSMutableArray *zoneRecords = [NSMutableArray array];
        for (HMZone *zone in [home zones]) {
            NSMutableArray *roomIds = [NSMutableArray array];
            for (HMRoom *room in [zone rooms]) {
                [roomIds addObject:cn1homeUuid([room uniqueIdentifier])];
            }
            [zoneRecords addObject:cn1homeJoinFields(
                [NSArray arrayWithObjects:cn1homeUuid([zone uniqueIdentifier]),
                 [zone name], [roomIds componentsJoinedByString:@","], nil])];
        }
        [zones setObject:cn1homeJoinRecords(zoneRecords) forKey:homeId];

        NSMutableArray *accessoryRecords = [NSMutableArray array];
        for (HMAccessory *accessory in [home accessories]) {
            NSString *accessoryId = cn1homeUuid([accessory uniqueIdentifier]);
            [accessoryObjects setObject:accessory forKey:accessoryId];
            NSString *roomId = [accessory room] == nil ? @""
                    : cn1homeUuid([[accessory room] uniqueIdentifier]);
            NSString *bridgeId = @"";
            for (HMAccessory *candidate in [home accessories]) {
                if ([candidate uniqueIdentifiersForBridgedAccessories] != nil
                        && [[candidate uniqueIdentifiersForBridgedAccessories]
                            containsObject:[accessory uniqueIdentifier]]) {
                    bridgeId = cn1homeUuid([candidate uniqueIdentifier]);
                    break;
                }
            }
            [accessoryRecords addObject:cn1homeJoinFields(
                [NSArray arrayWithObjects:accessoryId, [accessory name],
                 roomId,
                 [NSString stringWithFormat:@"%d",
                  cn1homeCategoryOrdinal([accessory category])],
                 [accessory manufacturer] == nil ? @""
                                                 : [accessory manufacturer],
                 [accessory model] == nil ? @"" : [accessory model],
                 [accessory firmwareVersion] == nil ? @""
                                                    : [accessory firmwareVersion],
                 cn1homeFlag([accessory isReachable]), bridgeId, nil])];

            NSMutableArray *serviceRecords = [NSMutableArray array];
            HMService *primaryService = nil;
            for (HMService *service in [accessory services]) {
                if ([service isPrimaryService]) {
                    primaryService = service;
                    break;
                }
            }
            for (HMService *service in [accessory services]) {
                BOOL isPrimaryService = primaryService == nil
                        ? [service isEqual:[[accessory services] firstObject]]
                        : [service isEqual:primaryService];
                [serviceRecords addObject:cn1homeJoinFields(
                    [NSArray arrayWithObjects:
                     cn1homeUuid([service uniqueIdentifier]), [service name],
                     [NSString stringWithFormat:@"%d",
                      cn1homeServiceTypeOrdinal([service serviceType])],
                     cn1homeFlag(isPrimaryService), nil])];
                cn1homeEncodeTraitsForService(accessory, service, traits);
            }
            [services setObject:cn1homeJoinRecords(serviceRecords)
                         forKey:accessoryId];
        }
        [accessories setObject:cn1homeJoinRecords(accessoryRecords)
                        forKey:homeId];

        NSMutableArray *sceneRecords = [NSMutableArray array];
        for (HMActionSet *actionSet in [home actionSets]) {
            NSString *sceneId = cn1homeUuid([actionSet uniqueIdentifier]);
            BOOL triggerOwned = [[actionSet actionSetType]
                                 isEqualToString:HMActionSetTypeTriggerOwned];
            [sceneRecords addObject:cn1homeJoinFields(
                [NSArray arrayWithObjects:sceneId, [actionSet name],
                 [NSString stringWithFormat:@"%d",
                  cn1homeSceneTypeOrdinal([actionSet actionSetType])],
                 cn1homeFlag(!triggerOwned), nil])];

            NSMutableArray *actionRecords = [NSMutableArray array];
            for (HMAction *action in [actionSet actions]) {
                if (![action isKindOfClass:[HMCharacteristicWriteAction class]]) {
                    continue;
                }
                HMCharacteristicWriteAction *write =
                        (HMCharacteristicWriteAction *) action;
                HMCharacteristic *c = [write characteristic];
                NSString *traitId = cn1homeTraitFor([c service], c);
                if (traitId == nil) {
                    continue;
                }
                NSArray *entry = cn1homeEntryFor(traitId);
                int kind = [[entry objectAtIndex:1] intValue];
                int unit = [[entry objectAtIndex:2] intValue];
                int conversion = [[entry objectAtIndex:3] intValue];
                double numeric = 0;
                int raw = 0;
                BOOL hasRaw = NO;
                if (!cn1homeToPortable(conversion, [write targetValue],
                                       &numeric, &raw, &hasRaw)) {
                    continue;
                }
                HMService *owning = [c service];
                [actionRecords addObject:cn1homeJoinFields(
                    [NSArray arrayWithObjects:
                     cn1homeUuid([[owning accessory] uniqueIdentifier]),
                     cn1homeUuid([owning uniqueIdentifier]), traitId,
                     [NSString stringWithFormat:@"%d", kind],
                     [NSString stringWithFormat:@"%f", numeric], @"",
                     [NSString stringWithFormat:@"%d", unit], nil])];
            }
            [sceneActions setObject:cn1homeJoinRecords(actionRecords)
                             forKey:[NSString stringWithFormat:@"%@\t%@",
                                     homeId, sceneId]];
        }
        [scenes setObject:cn1homeJoinRecords(sceneRecords) forKey:homeId];
    }

    [cn1homeSnapshotLock lock];
    [cn1homeStructuresLine release];
    cn1homeStructuresLine = [cn1homeJoinRecords(structures) retain];
    [cn1homeRoomsBy release];
    cn1homeRoomsBy = [rooms retain];
    [cn1homeZonesBy release];
    cn1homeZonesBy = [zones retain];
    [cn1homeAccessoriesBy release];
    cn1homeAccessoriesBy = [accessories retain];
    [cn1homeServicesBy release];
    cn1homeServicesBy = [services retain];
    [cn1homeTraitsBy release];
    cn1homeTraitsBy = [traits retain];
    [cn1homeScenesBy release];
    cn1homeScenesBy = [scenes retain];
    [cn1homeSceneActionsBy release];
    cn1homeSceneActionsBy = [sceneActions retain];
    [cn1homeSnapshotLock unlock];

    [cn1homeAccessoryObjects release];
    cn1homeAccessoryObjects = [accessoryObjects retain];
    [cn1homeHomeObjects release];
    cn1homeHomeObjects = [homeObjects retain];
}

static NSString *cn1homeSnapshotString(NSMutableDictionary *from,
                                       NSString *key) {
    [cn1homeSnapshotLock lock];
    NSString *value = from == nil || key == nil ? nil
            : [[[from objectForKey:key] retain] autorelease];
    [cn1homeSnapshotLock unlock];
    return value == nil ? @"" : value;
}

// ---------------------------------------------------------------------
// The delegate
// ---------------------------------------------------------------------

@interface CN1HomeDelegate : NSObject <HMHomeManagerDelegate, HMHomeDelegate,
        HMAccessoryDelegate>
@end

static void cn1homeNotifyStructure(int kind, NSString *structureId,
                                   NSString *accessoryId) {
    com_codename1_impl_ios_IOSHomeCallbacks_structureChanged___int_java_lang_String_java_lang_String(
        getThreadLocalData(), kind,
        structureId == nil ? JAVA_NULL
                           : fromNSString(getThreadLocalData(), structureId),
        accessoryId == nil ? JAVA_NULL
                           : fromNSString(getThreadLocalData(), accessoryId));
}

/// The authorization status as the Java side names it.
///
/// Split out of the native accessor so the delegate can reach it: the delegate
/// is what turns an asynchronous prompt into an answer, and it is defined
/// above the accessor.
static JAVA_INT cn1homeAuthStatus(void) {
    if (cn1homeManager == nil) {
        return CN1_HOME_AUTH_UNKNOWN;
    }
    HMHomeManagerAuthorizationStatus status =
            [cn1homeManager authorizationStatus];
    if ((status & HMHomeManagerAuthorizationStatusRestricted) != 0) {
        return CN1_HOME_AUTH_RESTRICTED;
    }
    if ((status & HMHomeManagerAuthorizationStatusAuthorized) != 0) {
        return CN1_HOME_AUTH_AUTHORIZED;
    }
    if ((status & HMHomeManagerAuthorizationStatusDetermined) != 0) {
        return CN1_HOME_AUTH_DENIED;
    }
    return CN1_HOME_AUTH_NOT_DETERMINED;
}

/// Answers a waiting requestAuthorization(), if the answer is in.
///
/// HomeKit has no request-and-callback API: creating the manager is what
/// prompts, and the prompt is answered by the user at their leisure. Reading
/// authorizationStatus straight after creating the manager therefore reports
/// NOT_DETERMINED while the sheet is still on screen -- which is not what the
/// caller was promised.
///
/// So the answer comes from the delegate instead. Determined is the bit that
/// says the user has decided; homeManagerDidUpdateHomes: passes `force`
/// because the database only finishes loading once that decision is made, and
/// on a build too old for the status bits it is the only signal there is.
static void cn1homeResolvePendingAuth(BOOL force) {
    JAVA_INT pending = cn1homePendingAuth;
    if (pending == 0) {
        return;
    }
    JAVA_INT status = cn1homeAuthStatus();
    if (!force && status == CN1_HOME_AUTH_NOT_DETERMINED) {
        return;
    }
    cn1homePendingAuth = 0;
    com_codename1_impl_ios_IOSHomeCallbacks_authorization___int_int_java_lang_String(
        getThreadLocalData(), pending, status, JAVA_NULL);
}

/// Re-attaches this bridge as the delegate of every accessory in every home.
///
/// Has to run after every graph change, not just at start: an accessory added
/// later arrives with no delegate, and one whose home was re-created loses the
/// one it had. Missing it is silent -- the accessory simply never reports a
/// change -- which is why it is a single function called from one place.
static void cn1homeAttachDelegates(void) {
    for (HMHome *home in [cn1homeManager homes]) {
        [home setDelegate:cn1homeDelegate];
        for (HMAccessory *accessory in [home accessories]) {
            [accessory setDelegate:cn1homeDelegate];
        }
    }
}

/// Sends one reading to every subscription watching it.
static void cn1homeDeliverChange(NSString *accessoryId, NSString *serviceId,
                                 NSString *traitId, id value) {
    NSString *key = cn1homeReadingKey(accessoryId, serviceId, traitId);
    NSString *record = cn1homeEncodeReading(accessoryId, serviceId, traitId,
                                            value, nil, nil);
    for (NSString *subscriptionId in cn1homeWatches) {
        NSSet *keys = [cn1homeWatches objectForKey:subscriptionId];
        if (![keys containsObject:key]) {
            continue;
        }
        // Delivered straight away: HomeKit is the one backend that pushes,
        // and the framework does the coalescing, so holding it here would
        // only add latency without reducing the number of EDT hops.
        com_codename1_impl_ios_IOSHomeCallbacks_changes___java_lang_String_java_lang_String(
            getThreadLocalData(),
            fromNSString(getThreadLocalData(), subscriptionId),
            fromNSString(getThreadLocalData(), record));
    }
}

@implementation CN1HomeDelegate

- (void)homeManagerDidUpdateHomes:(HMHomeManager *)manager {
    cn1homeHomesLoaded = YES;
    cn1homeRebuildSnapshot();
    cn1homeAttachDelegates();
    // The database has loaded, which HomeKit does not do until the user has
    // answered the prompt -- so whatever the status says now is final.
    cn1homeResolvePendingAuth(YES);
    JAVA_INT pending = cn1homePendingStart;
    if (pending != 0) {
        cn1homePendingStart = 0;
        // Through the same accessor the availability call uses, so a refusal
        // reads as DENIED here too rather than as an empty house.
        int availability =
                com_codename1_impl_ios_IOSNative_homeAvailability___R_int(
                    getThreadLocalData(), JAVA_NULL);
        com_codename1_impl_ios_IOSHomeCallbacks_started___int_int_java_lang_String(
            getThreadLocalData(), pending, availability, JAVA_NULL);
        return;
    }
    cn1homeNotifyStructure(CN1_HOME_CHANGE_STRUCTURES, nil, nil);
}

- (void)homeManager:(HMHomeManager *)manager
        didUpdateAuthorizationStatus:(HMHomeManagerAuthorizationStatus)status {
    // Usually the first of the two to fire, and the one that carries a denial:
    // a user who refuses gets no database load worth waiting for.
    cn1homeResolvePendingAuth(NO);
    // And it also fires when the user changes their mind in Settings while
    // the app is running, with nothing else to announce it. An app that
    // greyed its controls out on a refusal would leave them grey after the
    // user granted access, which is the state AVAILABILITY_CHANGED exists
    // to correct.
    cn1homeNotifyStructure(CN1_HOME_CHANGE_AVAILABILITY, nil, nil);
}

- (void)homeManager:(HMHomeManager *)manager didAddHome:(HMHome *)home {
    cn1homeRebuildSnapshot();
    cn1homeAttachDelegates();
    cn1homeNotifyStructure(CN1_HOME_CHANGE_STRUCTURES,
                           cn1homeUuid([home uniqueIdentifier]), nil);
}

- (void)homeManager:(HMHomeManager *)manager didRemoveHome:(HMHome *)home {
    cn1homeRebuildSnapshot();
    cn1homeNotifyStructure(CN1_HOME_CHANGE_STRUCTURES,
                           cn1homeUuid([home uniqueIdentifier]), nil);
}

- (void)home:(HMHome *)home didAddAccessory:(HMAccessory *)accessory {
    cn1homeRebuildSnapshot();
    [accessory setDelegate:cn1homeDelegate];
    cn1homeNotifyStructure(CN1_HOME_CHANGE_ACCESSORY_ADDED,
                           cn1homeUuid([home uniqueIdentifier]),
                           cn1homeUuid([accessory uniqueIdentifier]));
}

- (void)home:(HMHome *)home didRemoveAccessory:(HMAccessory *)accessory {
    cn1homeRebuildSnapshot();
    cn1homeNotifyStructure(CN1_HOME_CHANGE_ACCESSORY_REMOVED,
                           cn1homeUuid([home uniqueIdentifier]),
                           cn1homeUuid([accessory uniqueIdentifier]));
}

- (void)home:(HMHome *)home
        didUpdateRoom:(HMRoom *)room
         forAccessory:(HMAccessory *)accessory {
    cn1homeRebuildSnapshot();
    cn1homeNotifyStructure(CN1_HOME_CHANGE_ACCESSORY_MOVED,
                           cn1homeUuid([home uniqueIdentifier]),
                           cn1homeUuid([accessory uniqueIdentifier]));
}

// The rest of the topology. Renaming a home, adding or renaming a room,
// creating a zone or moving a room into one are ordinary things to do in the
// Apple Home app, and each of them changes what getStructures() should
// return. Without these the snapshot kept whatever it was built with and a
// HomeStructureListener heard nothing, so a room list stayed wrong until
// something unrelated forced a refresh.
//
// All of them report STRUCTURES: the listener's contract is "re-read the
// graph", not "here is what changed", and the graph is a snapshot.
- (void)homeDidUpdateName:(HMHome *)home {
    cn1homeRebuildSnapshot();
    cn1homeNotifyStructure(CN1_HOME_CHANGE_STRUCTURES,
                           cn1homeUuid([home uniqueIdentifier]), nil);
}

- (void)home:(HMHome *)home didAddRoom:(HMRoom *)room {
    cn1homeRebuildSnapshot();
    cn1homeNotifyStructure(CN1_HOME_CHANGE_STRUCTURES,
                           cn1homeUuid([home uniqueIdentifier]), nil);
}

- (void)home:(HMHome *)home didRemoveRoom:(HMRoom *)room {
    cn1homeRebuildSnapshot();
    cn1homeNotifyStructure(CN1_HOME_CHANGE_STRUCTURES,
                           cn1homeUuid([home uniqueIdentifier]), nil);
}

- (void)home:(HMHome *)home didUpdateNameForRoom:(HMRoom *)room {
    cn1homeRebuildSnapshot();
    cn1homeNotifyStructure(CN1_HOME_CHANGE_STRUCTURES,
                           cn1homeUuid([home uniqueIdentifier]), nil);
}

- (void)home:(HMHome *)home didAddZone:(HMZone *)zone {
    cn1homeRebuildSnapshot();
    cn1homeNotifyStructure(CN1_HOME_CHANGE_STRUCTURES,
                           cn1homeUuid([home uniqueIdentifier]), nil);
}

- (void)home:(HMHome *)home didRemoveZone:(HMZone *)zone {
    cn1homeRebuildSnapshot();
    cn1homeNotifyStructure(CN1_HOME_CHANGE_STRUCTURES,
                           cn1homeUuid([home uniqueIdentifier]), nil);
}

- (void)home:(HMHome *)home didUpdateNameForZone:(HMZone *)zone {
    cn1homeRebuildSnapshot();
    cn1homeNotifyStructure(CN1_HOME_CHANGE_STRUCTURES,
                           cn1homeUuid([home uniqueIdentifier]), nil);
}

- (void)home:(HMHome *)home didAddRoom:(HMRoom *)room toZone:(HMZone *)zone {
    cn1homeRebuildSnapshot();
    cn1homeNotifyStructure(CN1_HOME_CHANGE_STRUCTURES,
                           cn1homeUuid([home uniqueIdentifier]), nil);
}

- (void)home:(HMHome *)home
        didRemoveRoom:(HMRoom *)room fromZone:(HMZone *)zone {
    cn1homeRebuildSnapshot();
    cn1homeNotifyStructure(CN1_HOME_CHANGE_STRUCTURES,
                           cn1homeUuid([home uniqueIdentifier]), nil);
}

- (void)home:(HMHome *)home didAddActionSet:(HMActionSet *)actionSet {
    cn1homeRebuildSnapshot();
    cn1homeNotifyStructure(CN1_HOME_CHANGE_SCENES,
                           cn1homeUuid([home uniqueIdentifier]), nil);
}

- (void)home:(HMHome *)home didRemoveActionSet:(HMActionSet *)actionSet {
    cn1homeRebuildSnapshot();
    cn1homeNotifyStructure(CN1_HOME_CHANGE_SCENES,
                           cn1homeUuid([home uniqueIdentifier]), nil);
}

// A scene edited in the Apple Home app -- renamed, or its actions changed --
// arrives through these rather than through add and remove. Without them the
// cached name and actions stay as they were until something unrelated forced
// a refresh, so an app's scene list showed the old name indefinitely.
- (void)home:(HMHome *)home
        didUpdateNameForActionSet:(HMActionSet *)actionSet {
    cn1homeRebuildSnapshot();
    cn1homeNotifyStructure(CN1_HOME_CHANGE_SCENES,
                           cn1homeUuid([home uniqueIdentifier]), nil);
}

- (void)home:(HMHome *)home
        didUpdateActionsForActionSet:(HMActionSet *)actionSet {
    cn1homeRebuildSnapshot();
    cn1homeNotifyStructure(CN1_HOME_CHANGE_SCENES,
                           cn1homeUuid([home uniqueIdentifier]), nil);
}

- (void)accessoryDidUpdateName:(HMAccessory *)accessory {
    cn1homeRebuildSnapshot();
    cn1homeNotifyStructure(CN1_HOME_CHANGE_ACCESSORY_RENAMED, nil,
                           cn1homeUuid([accessory uniqueIdentifier]));
}

- (void)accessoryDidUpdateReachability:(HMAccessory *)accessory {
    // Not a full rebuild's worth of work in principle, but the reachability
    // flag lives in the encoded accessory record, so the snapshot is stale
    // until it is rebuilt. This is the most frequent callback by a wide
    // margin; if it ever shows up in a profile, the fix is a targeted patch
    // of the one record rather than dropping the notification.
    cn1homeRebuildSnapshot();
    cn1homeNotifyStructure(CN1_HOME_CHANGE_REACHABILITY, nil,
                           cn1homeUuid([accessory uniqueIdentifier]));
}

- (void)accessory:(HMAccessory *)accessory
                     service:(HMService *)service
  didUpdateValueForCharacteristic:(HMCharacteristic *)characteristic {
    NSString *traitId = cn1homeTraitFor(service, characteristic);
    if (traitId == nil) {
        return;
    }
    NSString *accessoryId = cn1homeUuid([accessory uniqueIdentifier]);
    NSString *serviceId = cn1homeUuid([service uniqueIdentifier]);
    cn1homeDeliverChange(accessoryId, serviceId, traitId,
        cn1homeHasNoValueNow(service, traitId) ? nil
                                               : [characteristic value]);
    // A thermostat crossing into or out of AUTO changes what
    // TARGET_TEMPERATURE means without touching the TargetTemperature
    // characteristic, and HomeKit notifies per characteristic -- so a
    // listener watching the setpoint hears nothing and keeps showing a
    // number the accessory is no longer aiming for. The mode's own
    // notification is the only signal there is, so the setpoint's update is
    // sent from here too.
    if ([traitId isEqualToString:@"target_heating_cooling"]) {
        HMCharacteristic *setpoint = cn1homeFindCharacteristic(
            service, @"target_temperature");
        if (setpoint != nil) {
            cn1homeDeliverChange(accessoryId, serviceId, @"target_temperature",
                cn1homeHasNoValueNow(service, @"target_temperature")
                    ? nil : [setpoint value]);
        }
    }
}

@end

// ---------------------------------------------------------------------
// Bootstrapping
// ---------------------------------------------------------------------

static void cn1homeInit(void) {
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        cn1homeSnapshotLock = [[NSLock alloc] init];
        cn1homeWatches = [[NSMutableDictionary alloc] init];
        cn1homeUndelivered = [[NSMutableDictionary alloc] init];
        cn1homeLastPolled = [[NSMutableDictionary alloc] init];
        cn1homeAccessoryObjects = [[NSMutableDictionary alloc] init];
        cn1homeHomeObjects = [[NSMutableDictionary alloc] init];
        cn1homeDelegate = [[CN1HomeDelegate alloc] init];
    });
}

/// Runs a block on the main queue.
///
/// Inline when already there, so a Java callback that re-enters the bridge
/// cannot deadlock -- and because the CN1 iOS EDT IS the main thread, so
/// dispatch_sync from it would deadlock outright. CN1AppleMapKit learned that
/// one the hard way.
static void cn1homeOnMain(void (^block)(void)) {
    if ([NSThread isMainThread]) {
        block();
        return;
    }
    dispatch_async(dispatch_get_main_queue(), block);
}

// ---------------------------------------------------------------------
// Natives
// ---------------------------------------------------------------------

JAVA_BOOLEAN
com_codename1_impl_ios_IOSNative_homeSupported___R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_TRUE;
}

JAVA_INT
com_codename1_impl_ios_IOSNative_homeAvailability___R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    POOL_BEGIN();
    cn1homeInit();
    JAVA_INT result;
    if (cn1homeManager == nil || !cn1homeHomesLoaded) {
        // Nothing has connected yet. Reported as needing permission rather
        // than as unavailable, because that is the state an app should react
        // to by calling refresh() -- which is what connects.
        result = CN1_HOME_AVAIL_PERMISSION_REQUIRED;
    } else {
        HMHomeManagerAuthorizationStatus status =
                [cn1homeManager authorizationStatus];
        if ((status & HMHomeManagerAuthorizationStatusRestricted) != 0) {
            result = CN1_HOME_AVAIL_RESTRICTED;
        } else if ((status & HMHomeManagerAuthorizationStatusAuthorized) == 0) {
            // Asked and refused is not the same as not yet asked. iOS shows
            // the HomeKit prompt once, so an app that answers a refusal by
            // calling requestAuthorization() gets the refusal back with
            // nothing on screen; DENIED points it at openHomeSettings()
            // instead, which is the only way back.
            result = (status & HMHomeManagerAuthorizationStatusDetermined) != 0
                    ? CN1_HOME_AVAIL_PERMISSION_DENIED
                    : CN1_HOME_AVAIL_PERMISSION_REQUIRED;
        } else if ([[cn1homeManager homes] count] == 0) {
            // Its own state, because HomeKit exists on every device and
            // "supported" tells a user who has never opened the Home app
            // nothing they can act on.
            result = CN1_HOME_AVAIL_NOT_CONFIGURED;
        } else {
            result = CN1_HOME_AVAIL_AVAILABLE;
        }
    }
    POOL_END();
    return result;
}

JAVA_INT
com_codename1_impl_ios_IOSNative_homeAuthorizationStatus___R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    POOL_BEGIN();
    cn1homeInit();
    JAVA_INT result = cn1homeAuthStatus();
    POOL_END();
    return result;
}

JAVA_OBJECT
com_codename1_impl_ios_IOSNative_homeConfigurationProblems___R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    POOL_BEGIN();
    NSMutableArray *problems = [NSMutableArray array];
    // The usage description is the one thing a build can be missing that
    // HomeKit refuses to work without and that nothing else reports. iOS
    // terminates the app when HMHomeManager is created without it, so this is
    // worth naming rather than letting the app die on launch.
    if ([[NSBundle mainBundle]
         objectForInfoDictionaryKey:@"NSHomeKitUsageDescription"] == nil) {
        [problems addObject:@"This build has no ios.NSHomeKitUsageDescription "
         "build hint. iOS terminates an app that reaches HomeKit without one."];
    }
    JAVA_OBJECT result = fromNSString(CN1_THREAD_STATE_PASS_ARG
                                      cn1homeJoinRecords(problems));
    POOL_END();
    return result;
}

void com_codename1_impl_ios_IOSNative_homeStart___int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId) {
    cn1homeInit();
    JAVA_INT rid = requestId;
    cn1homeOnMain(^{
        if (cn1homeManager != nil && cn1homeHomesLoaded) {
            // Already connected, so answer from what is already loaded rather
            // than waiting for a delegate callback that will not come again.
            int availability =
                    com_codename1_impl_ios_IOSNative_homeAvailability___R_int(
                        getThreadLocalData(), JAVA_NULL);
            com_codename1_impl_ios_IOSHomeCallbacks_started___int_int_java_lang_String(
                getThreadLocalData(), rid, availability, JAVA_NULL);
            return;
        }
        cn1homePendingStart = rid;
        if (cn1homeManager == nil) {
            cn1homeManager = [[HMHomeManager alloc] init];
            [cn1homeManager setDelegate:cn1homeDelegate];
        }
        // The answer comes from homeManagerDidUpdateHomes:, which HomeKit
        // calls once the database has loaded. There is no synchronous way to
        // ask; homes is empty until it fires, and an app that read it
        // immediately would report an empty house on every cold start.
    });
}

void com_codename1_impl_ios_IOSNative_homeStop__(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    cn1homeOnMain(^{
        if (cn1homeManager != nil) {
            [cn1homeManager setDelegate:nil];
            for (HMHome *home in [cn1homeManager homes]) {
                [home setDelegate:nil];
                for (HMAccessory *accessory in [home accessories]) {
                    [accessory setDelegate:nil];
                }
            }
            [cn1homeManager release];
            cn1homeManager = nil;
        }
        cn1homeHomesLoaded = NO;
        cn1homePendingStart = 0;
        cn1homePendingAuth = 0;
        [cn1homeWatches removeAllObjects];
        [cn1homeUndelivered removeAllObjects];
        [cn1homeLastPolled removeAllObjects];
    });
}

void com_codename1_impl_ios_IOSNative_homeRequestAuthorization___int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId) {
    cn1homeInit();
    JAVA_INT rid = requestId;
    cn1homeOnMain(^{
        // HomeKit has no explicit request call: creating the manager is what
        // prompts. The prompt is asynchronous, so the status read straight
        // afterwards is still NOT_DETERMINED with the sheet on screen -- the
        // answer has to come from the delegate.
        if (cn1homeManager != nil) {
            JAVA_INT status = cn1homeAuthStatus();
            if (status != CN1_HOME_AUTH_NOT_DETERMINED) {
                // Already answered once. No second prompt is coming, so
                // waiting for a delegate callback would wait forever.
                com_codename1_impl_ios_IOSHomeCallbacks_authorization___int_int_java_lang_String(
                    getThreadLocalData(), rid, status, JAVA_NULL);
                return;
            }
        }
        if (cn1homePendingAuth != 0) {
            // A second request while the first is still on screen. Answered
            // as UNKNOWN rather than left hanging: only one of the two can be
            // held, and a caller waiting forever is the worse failure.
            com_codename1_impl_ios_IOSHomeCallbacks_authorization___int_int_java_lang_String(
                getThreadLocalData(), rid, CN1_HOME_AUTH_UNKNOWN, JAVA_NULL);
            return;
        }
        cn1homePendingAuth = rid;
        if (cn1homeManager == nil) {
            cn1homeManager = [[HMHomeManager alloc] init];
            [cn1homeManager setDelegate:cn1homeDelegate];
        } else {
            // The manager exists but nobody has decided yet, which means the
            // prompt is either open or never appeared. Re-check now in case
            // the delegate callback landed before this request did.
            cn1homeResolvePendingAuth(NO);
        }
    });
}

JAVA_BOOLEAN
com_codename1_impl_ios_IOSNative_homeOpenSettings___R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    POOL_BEGIN();
    NSURL *url = [NSURL URLWithString:UIApplicationOpenSettingsURLString];
    __block BOOL opened = NO;
    if (url != nil) {
        opened = YES;
        cn1homeOnMain(^{
            [[UIApplication sharedApplication] openURL:url options:@{}
                                     completionHandler:nil];
        });
    }
    POOL_END();
    return opened ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN
com_codename1_impl_ios_IOSNative_homeOpenEcosystemApp___R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    POOL_BEGIN();
    NSURL *url = [NSURL URLWithString:@"com.apple.Home://"];
    // canOpenURL: answers false for any scheme the app has not declared in
    // LSApplicationQueriesSchemes, so this is only a real answer because the
    // builder injects com.apple.Home for a smart-home build. A build made any
    // other way reports the Home app missing on a device that has it -- and
    // this is the documented fallback for every platform where commissioning
    // is unsupported, so getting it wrong takes away the last thing the user
    // could have done.
    BOOL canOpen = url != nil
            && [[UIApplication sharedApplication] canOpenURL:url];
    if (canOpen) {
        cn1homeOnMain(^{
            [[UIApplication sharedApplication] openURL:url options:@{}
                                     completionHandler:nil];
        });
    }
    POOL_END();
    return canOpen ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_OBJECT
com_codename1_impl_ios_IOSNative_homeStructures___R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    POOL_BEGIN();
    cn1homeInit();
    [cn1homeSnapshotLock lock];
    NSString *value = cn1homeStructuresLine == nil ? @""
            : [[cn1homeStructuresLine retain] autorelease];
    [cn1homeSnapshotLock unlock];
    JAVA_OBJECT result = fromNSString(CN1_THREAD_STATE_PASS_ARG value);
    POOL_END();
    return result;
}

/// The five snapshot getters below share this body.
///
/// The functions themselves are written out rather than generated by a macro,
/// which is what they used to be. ParparVM encodes the Java signature in the
/// C name, and a name assembled by token pasting is invisible to every tool
/// that reads this file as text -- including the native signature check,
/// which reported all five as declared and unimplemented, and grep, which is
/// how anyone finds them.
static JAVA_OBJECT cn1homeSnapshotFor(CN1_THREAD_STATE_MULTI_ARG
                                      NSMutableDictionary *dict,
                                      JAVA_OBJECT key) {
    POOL_BEGIN();
    cn1homeInit();
    NSString *k = toNSString(CN1_THREAD_STATE_PASS_ARG key);
    JAVA_OBJECT result = fromNSString(CN1_THREAD_STATE_PASS_ARG
                                      cn1homeSnapshotString(dict, k));
    POOL_END();
    return result;
}

JAVA_OBJECT
com_codename1_impl_ios_IOSNative_homeRooms___java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT key) {
    return cn1homeSnapshotFor(CN1_THREAD_STATE_PASS_ARG cn1homeRoomsBy, key);
}

JAVA_OBJECT
com_codename1_impl_ios_IOSNative_homeZones___java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT key) {
    return cn1homeSnapshotFor(CN1_THREAD_STATE_PASS_ARG cn1homeZonesBy, key);
}

JAVA_OBJECT
com_codename1_impl_ios_IOSNative_homeAccessories___java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT key) {
    return cn1homeSnapshotFor(CN1_THREAD_STATE_PASS_ARG cn1homeAccessoriesBy, key);
}

JAVA_OBJECT
com_codename1_impl_ios_IOSNative_homeServices___java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT key) {
    return cn1homeSnapshotFor(CN1_THREAD_STATE_PASS_ARG cn1homeServicesBy, key);
}

JAVA_OBJECT
com_codename1_impl_ios_IOSNative_homeScenes___java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT key) {
    return cn1homeSnapshotFor(CN1_THREAD_STATE_PASS_ARG cn1homeScenesBy, key);
}


JAVA_OBJECT
com_codename1_impl_ios_IOSNative_homeTraits___java_lang_String_java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT accessoryId,
        JAVA_OBJECT serviceId) {
    POOL_BEGIN();
    cn1homeInit();
    NSString *key = [NSString stringWithFormat:@"%@\t%@",
                     toNSString(CN1_THREAD_STATE_PASS_ARG accessoryId),
                     toNSString(CN1_THREAD_STATE_PASS_ARG serviceId)];
    JAVA_OBJECT result = fromNSString(CN1_THREAD_STATE_PASS_ARG
                                      cn1homeSnapshotString(cn1homeTraitsBy,
                                                            key));
    POOL_END();
    return result;
}

JAVA_OBJECT
com_codename1_impl_ios_IOSNative_homeSceneActions___java_lang_String_java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT structureId,
        JAVA_OBJECT sceneId) {
    POOL_BEGIN();
    cn1homeInit();
    NSString *key = [NSString stringWithFormat:@"%@\t%@",
                     toNSString(CN1_THREAD_STATE_PASS_ARG structureId),
                     toNSString(CN1_THREAD_STATE_PASS_ARG sceneId)];
    JAVA_OBJECT result = fromNSString(CN1_THREAD_STATE_PASS_ARG
                                      cn1homeSnapshotString(
                                          cn1homeSceneActionsBy, key));
    POOL_END();
    return result;
}

void com_codename1_impl_ios_IOSNative_homeRefresh___int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId) {
    cn1homeInit();
    JAVA_INT rid = requestId;
    cn1homeOnMain(^{
        if (cn1homeManager == nil) {
            com_codename1_impl_ios_IOSHomeCallbacks_refreshed___int_java_lang_String(
                getThreadLocalData(), rid,
                fromNSString(getThreadLocalData(),
                             cn1homeError(@"NOT_CONFIGURED", nil)));
            return;
        }
        cn1homeRebuildSnapshot();
        cn1homeAttachDelegates();
        com_codename1_impl_ios_IOSHomeCallbacks_refreshed___int_java_lang_String(
            getThreadLocalData(), rid, JAVA_NULL);
    });
}

void
com_codename1_impl_ios_IOSNative_homeReadTraits___int_java_lang_String_java_lang_String_java_lang_String_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT accessoryIds, JAVA_OBJECT serviceIds, JAVA_OBJECT traitIds,
        JAVA_BOOLEAN allowCached) {
    cn1homeInit();
    JAVA_INT rid = requestId;
    BOOL cached = allowCached != JAVA_FALSE;
    NSArray *accessories = [cn1homeSplit(
        toNSString(CN1_THREAD_STATE_PASS_ARG accessoryIds)) retain];
    NSArray *services = [cn1homeSplit(
        toNSString(CN1_THREAD_STATE_PASS_ARG serviceIds)) retain];
    NSArray *traits = [cn1homeSplit(
        toNSString(CN1_THREAD_STATE_PASS_ARG traitIds)) retain];
    cn1homeOnMain(^{
        NSUInteger count = [traits count];
        NSMutableArray *records = [NSMutableArray arrayWithCapacity:count];
        for (NSUInteger i = 0; i < count; i++) {
            [records addObject:@""];
        }
        __block NSUInteger finished = 0;
        void (^answer)(void) = ^{
            com_codename1_impl_ios_IOSHomeCallbacks_readings___int_java_lang_String_java_lang_String(
                getThreadLocalData(), rid,
                fromNSString(getThreadLocalData(),
                             cn1homeJoinRecords(records)),
                JAVA_NULL);
            [accessories release];
            [services release];
            [traits release];
        };
        // Two passes, and the split matters. Everything that can be answered
        // without touching the radio is settled first, which fixes how many
        // live reads there will be BEFORE any of them is issued -- a
        // completion handler has to know whether it was the last, and a total
        // that was still growing would let the first one answer for the whole
        // batch.
        //
        // Counting eligibility in a pass that did not also apply the error
        // cases is how this hangs: an unreachable accessory whose
        // characteristic exists would be counted as a pending read and then
        // never issued, so the count could never be reached and the caller
        // waited forever.
        NSMutableArray *live = [NSMutableArray array];
        for (NSUInteger i = 0; i < count; i++) {
            NSString *accessoryId = [accessories objectAtIndex:i];
            NSString *serviceId = [services objectAtIndex:i];
            NSString *traitId = [traits objectAtIndex:i];
            HMAccessory *accessory = cn1homeFindAccessory(accessoryId);
            if (accessory == nil) {
                [records replaceObjectAtIndex:i withObject:
                    cn1homeEncodeReading(accessoryId, serviceId, traitId, nil,
                                         @"ACCESSORY_NOT_FOUND",
                                         @"no such accessory in this home")];
                continue;
            }
            HMService *service = cn1homeFindService(accessoryId, serviceId);
            HMCharacteristic *c = cn1homeFindCharacteristic(service, traitId);
            if (![accessory isReachable]) {
                // Unreachable, but a cached read is exactly the request that
                // tolerates that -- setAllowCached documents riding out a
                // brief outage with the last value HomeKit saw. Refused
                // before consulting the cache, the option did nothing in the
                // one case it exists for.
                if (cached && c != nil
                        && [[c properties] containsObject:
                            HMCharacteristicPropertyReadable]
                        && [c value] != nil
                        && !cn1homeHasNoValueNow(service, traitId)) {
                    [records replaceObjectAtIndex:i withObject:
                        cn1homeEncodeReadingAt(accessoryId, serviceId, traitId,
                                               [c value], nil, nil, NO)];
                    continue;
                }
                [records replaceObjectAtIndex:i withObject:
                    cn1homeEncodeReading(accessoryId, serviceId, traitId, nil,
                                         @"ACCESSORY_UNREACHABLE",
                                         @"the accessory is not responding")];
                continue;
            }
            if (c == nil) {
                [records replaceObjectAtIndex:i withObject:
                    cn1homeEncodeReading(accessoryId, serviceId, traitId, nil,
                                         @"TRAIT_NOT_SUPPORTED",
                                         @"this service does not have that "
                                         "trait")];
                continue;
            }
            if (![[c properties]
                    containsObject:HMCharacteristicPropertyReadable]) {
                // The graph says so through TraitConstraint.isReadable, and
                // the local backend answers WRITE_ONLY_TRAIT -- so an app
                // that reads one by identifier rather than from the graph
                // gets the same answer on both, instead of a cached number
                // HomeKit never promised to keep.
                [records replaceObjectAtIndex:i withObject:
                    cn1homeEncodeReading(accessoryId, serviceId, traitId, nil,
                                         @"WRITE_ONLY_TRAIT",
                                         @"this trait can be set but not "
                                         "read")];
                continue;
            }
            if (cn1homeHasNoValueNow(service, traitId)) {
                [records replaceObjectAtIndex:i withObject:
                    cn1homeEncodeReading(accessoryId, serviceId, traitId, nil,
                                         nil, nil)];
                continue;
            }
            if (cached && [c value] != nil) {
                // HomeKit keeps the last value it saw, and answering from it
                // is instant and costs a battery-powered accessory nothing.
                // TraitReadRequest.setAllowCached documents the trade.
                //
                // Only when there IS one. allowCached defaults to true, so
                // taking this branch with an empty cache would answer the
                // very first read of a real sensor with "no value" and never
                // go to the accessory at all.
                //
                // Stamped unknown rather than now: this value is of whatever
                // age HomeKit's cache is, and calling it fresh is the one
                // thing that would make a freshness check useless.
                [records replaceObjectAtIndex:i withObject:
                    cn1homeEncodeReadingAt(accessoryId, serviceId, traitId,
                                           [c value], nil, nil, NO)];
                continue;
            }
            [live addObject:[NSNumber numberWithUnsignedInteger:i]];
        }
        NSUInteger outstanding = [live count];
        if (outstanding == 0) {
            answer();
            return;
        }
        for (NSNumber *boxed in live) {
            NSUInteger index = [boxed unsignedIntegerValue];
            NSString *accessoryId = [accessories objectAtIndex:index];
            NSString *serviceId = [services objectAtIndex:index];
            NSString *traitId = [traits objectAtIndex:index];
            HMService *service = cn1homeFindService(accessoryId, serviceId);
            HMCharacteristic *c = cn1homeFindCharacteristic(service, traitId);
            [c readValueWithCompletionHandler:^(NSError *error) {
                if (error != nil) {
                    [records replaceObjectAtIndex:index withObject:
                        cn1homeEncodeReading(accessoryId, serviceId, traitId,
                                             nil, cn1homeErrorName(error),
                                             [error localizedDescription])];
                } else {
                    [records replaceObjectAtIndex:index withObject:
                        cn1homeEncodeReading(accessoryId, serviceId, traitId,
                            cn1homeHasNoValueNow(service, traitId) ? nil
                                                                   : [c value],
                            nil, nil)];
                }
                finished++;
                if (finished == outstanding) {
                    answer();
                }
            }];
        }
    });
}

void
com_codename1_impl_ios_IOSNative_homeWriteTraits___int_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT accessoryIds, JAVA_OBJECT serviceIds, JAVA_OBJECT traitIds,
        JAVA_OBJECT kinds, JAVA_OBJECT numericValues, JAVA_OBJECT stringValues,
        JAVA_OBJECT unitWireIds, JAVA_OBJECT authorizationData) {
    // authorizationData is deliberately unread. It carries one door-lock
    // credential per write for the Matter backends; HomeKit has no per-write
    // credential at all -- iOS asks the user itself when a lock wants one --
    // so there is nowhere here to put it.
    cn1homeInit();
    JAVA_INT rid = requestId;
    NSArray *accessories = [cn1homeSplit(
        toNSString(CN1_THREAD_STATE_PASS_ARG accessoryIds)) retain];
    NSArray *services = [cn1homeSplit(
        toNSString(CN1_THREAD_STATE_PASS_ARG serviceIds)) retain];
    NSArray *traits = [cn1homeSplit(
        toNSString(CN1_THREAD_STATE_PASS_ARG traitIds)) retain];
    NSArray *numbers = [cn1homeSplit(
        toNSString(CN1_THREAD_STATE_PASS_ARG numericValues)) retain];
    cn1homeOnMain(^{
        NSUInteger count = [traits count];
        NSMutableArray *records = [NSMutableArray arrayWithCapacity:count];
        for (NSUInteger i = 0; i < count; i++) {
            [records addObject:@""];
        }
        __block NSUInteger outstanding = 0;
        __block NSUInteger finished = 0;
        NSMutableArray *pending = [NSMutableArray array];
        for (NSUInteger i = 0; i < count; i++) {
            HMService *service = cn1homeFindService(
                [accessories objectAtIndex:i], [services objectAtIndex:i]);
            HMCharacteristic *c = cn1homeFindCharacteristic(
                service, [traits objectAtIndex:i]);
            if (c != nil) {
                [pending addObject:[NSNumber numberWithUnsignedInteger:i]];
            }
        }
        outstanding = [pending count];
        void (^answer)(void) = ^{
            com_codename1_impl_ios_IOSHomeCallbacks_writeResults___int_java_lang_String_java_lang_String(
                getThreadLocalData(), rid,
                fromNSString(getThreadLocalData(),
                             cn1homeJoinRecords(records)),
                JAVA_NULL);
            [accessories release];
            [services release];
            [traits release];
            [numbers release];
        };
        for (NSUInteger i = 0; i < count; i++) {
            NSString *accessoryId = [accessories objectAtIndex:i];
            NSString *serviceId = [services objectAtIndex:i];
            NSString *traitId = [traits objectAtIndex:i];
            NSArray *base = [NSArray arrayWithObjects:accessoryId, serviceId,
                             traitId, nil];
            HMService *service = cn1homeFindService(accessoryId, serviceId);
            HMCharacteristic *c = cn1homeFindCharacteristic(service, traitId);
            if (c == nil) {
                [records replaceObjectAtIndex:i withObject:cn1homeJoinFields(
                    [base arrayByAddingObjectsFromArray:
                     [NSArray arrayWithObjects:@"0", @"TRAIT_NOT_SUPPORTED",
                      @"this service does not have that trait", nil]])];
                continue;
            }
            NSArray *entry = cn1homeEntryFor(traitId);
            int conversion = [[entry objectAtIndex:3] intValue];
            double numeric = i < [numbers count]
                    ? [[numbers objectAtIndex:i] doubleValue] : 0;
            // FanMode.OFF means not running, and HomeKit's mode
            // characteristic cannot say that -- "manual" is still on. The
            // power characteristic is the write, then, not a side effect of
            // one: done the other way the fan keeps running and the caller is
            // told it stopped, which is the one outcome they cannot detect.
            // It also drops out of the fan handling below, which is about
            // getting a fan running rather than stopping one.
            if (conversion == CN1_HC_FAN_MODE && (int) numeric == 0) {
                HMCharacteristic *power = cn1homeFindCharacteristic(
                    service, @"on_off");
                if (power == nil) {
                    [records replaceObjectAtIndex:i
                                       withObject:cn1homeJoinFields(
                        [base arrayByAddingObjectsFromArray:
                         [NSArray arrayWithObjects:@"0",
                          @"TRAIT_NOT_SUPPORTED",
                          @"this fan cannot be switched off", nil]])];
                    outstanding--;
                    continue;
                }
                c = power;
                conversion = CN1_HC_BOOL;
                numeric = 0;
            }
            id target = cn1homeToHomeKit(conversion, numeric);
            if (target == nil) {
                [records replaceObjectAtIndex:i withObject:cn1homeJoinFields(
                    [base arrayByAddingObjectsFromArray:
                     [NSArray arrayWithObjects:@"0", @"INVALID_ARGUMENT",
                      @"HomeKit cannot express that value", nil]])];
                outstanding--;
                continue;
            }
            // A running fan mode is three characteristics on HomeKit, not
            // one: the fan has to be on, at the speed the mode names, in
            // manual rather than auto. They are independent, so writing only
            // the mode reports success while the fan stays stopped or keeps
            // its old speed. All of them are written, and all of them are
            // reported -- the caller asked for LOW, and a LOW that did not
            // take is not a success.
            NSMutableArray *chars = [NSMutableArray arrayWithObject:c];
            NSMutableArray *values = [NSMutableArray arrayWithObject:target];
            if (conversion == CN1_HC_FAN_MODE) {
                HMCharacteristic *power = cn1homeFindCharacteristic(
                    service, @"on_off");
                if (power != nil) {
                    [chars addObject:power];
                    [values addObject:[NSNumber numberWithBool:YES]];
                }
                double speed = cn1homeFanModeSpeed((int) numeric);
                HMCharacteristic *speedChar = speed < 0 ? nil
                        : cn1homeFindCharacteristic(service, @"fan_speed");
                if (speed >= 0 && speedChar == nil) {
                    // LOW, MEDIUM and HIGH are a speed, and RotationSpeed is
                    // optional on a HomeKit fan. Without it the rest of this
                    // write would select manual and switch the fan on at
                    // whatever speed it was already at, and report the
                    // requested mode as applied.
                    [records replaceObjectAtIndex:i
                                       withObject:cn1homeJoinFields(
                        [base arrayByAddingObjectsFromArray:
                         [NSArray arrayWithObjects:@"0",
                          @"TRAIT_NOT_SUPPORTED",
                          @"this fan has no speed control", nil]])];
                    outstanding--;
                    continue;
                }
                if (speedChar != nil) {
                    [chars addObject:speedChar];
                    [values addObject:[NSNumber numberWithDouble:speed]];
                }
            }
            NSUInteger index = i;
            __block NSUInteger partsLeft = [chars count];
            __block BOOL reported = NO;
            for (NSUInteger part = 0; part < [chars count]; part++) {
                [[chars objectAtIndex:part]
                        writeValue:[values objectAtIndex:part]
                 completionHandler:^(NSError *error) {
                    if (error != nil && !reported) {
                        // The first failure is the one reported. A second
                        // would only describe the same broken accessory.
                        reported = YES;
                        [records replaceObjectAtIndex:index
                                           withObject:cn1homeJoinFields(
                            [base arrayByAddingObjectsFromArray:
                             [NSArray arrayWithObjects:@"0",
                              cn1homeErrorName(error),
                              cn1homeSanitize([error localizedDescription]),
                              nil]])];
                    }
                    partsLeft--;
                    if (partsLeft > 0) {
                        return;
                    }
                    if (!reported) {
                        [records replaceObjectAtIndex:index
                                           withObject:cn1homeJoinFields(
                            [base arrayByAddingObjectsFromArray:
                             [NSArray arrayWithObjects:@"1", @"", @"", nil]])];
                    }
                    finished++;
                    if (finished == outstanding) {
                        answer();
                    }
                }];
            }
        }
        if (outstanding == 0) {
            answer();
        }
    });
}

void
com_codename1_impl_ios_IOSNative_homeSubscribe___int_java_lang_String_java_lang_String_java_lang_String_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT subscriptionId, JAVA_OBJECT accessoryIds,
        JAVA_OBJECT serviceIds, JAVA_OBJECT traitIds) {
    cn1homeInit();
    NSString *subId = [toNSString(CN1_THREAD_STATE_PASS_ARG subscriptionId)
                       retain];
    NSArray *accessories = [cn1homeSplit(
        toNSString(CN1_THREAD_STATE_PASS_ARG accessoryIds)) retain];
    NSArray *services = [cn1homeSplit(
        toNSString(CN1_THREAD_STATE_PASS_ARG serviceIds)) retain];
    NSArray *traits = [cn1homeSplit(
        toNSString(CN1_THREAD_STATE_PASS_ARG traitIds)) retain];
    cn1homeOnMain(^{
        NSMutableSet *keys = [NSMutableSet set];
        for (NSUInteger i = 0; i < [traits count]; i++) {
            NSString *accessoryId = [accessories objectAtIndex:i];
            NSString *serviceId = [services objectAtIndex:i];
            NSString *traitId = [traits objectAtIndex:i];
            [keys addObject:cn1homeReadingKey(accessoryId, serviceId,
                                              traitId)];
            HMService *service = cn1homeFindService(accessoryId, serviceId);
            HMCharacteristic *c = cn1homeFindCharacteristic(service, traitId);
            if (c == nil) {
                continue;
            }
            if (![[c properties] containsObject:
                  HMCharacteristicPropertySupportsEventNotification]) {
                // The accessory will not report this one. The key stays in
                // the watch set anyway so a later read still routes here, and
                // TraitConstraint.notifiesOnChange already told the caller.
                continue;
            }
            [c enableNotification:YES completionHandler:^(NSError *error) {
                if (error == nil) {
                    return;
                }
                // The subscription is still registered -- failing the whole
                // thing because one sensor refused would take the other
                // nineteen down with it -- but the caller has to be told
                // that what it is watching may now be stale. This is the one
                // backend with no polling fallback: if the registration
                // failed and nothing said so, the listener would sit on the
                // last value it saw for as long as the screen is open, even
                // after the accessory came back.
                com_codename1_impl_ios_IOSHomeCallbacks_resyncRequired___java_lang_String(
                    getThreadLocalData(),
                    fromNSString(getThreadLocalData(), subId));
            }];
        }
        [cn1homeWatches setObject:keys forKey:subId];
        [subId release];
        [accessories release];
        [services release];
        [traits release];
    });
}

void com_codename1_impl_ios_IOSNative_homeUnsubscribe___java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT subscriptionId) {
    cn1homeInit();
    NSString *subId = [toNSString(CN1_THREAD_STATE_PASS_ARG subscriptionId)
                       retain];
    cn1homeOnMain(^{
        NSSet *keys = [cn1homeWatches objectForKey:subId];
        [cn1homeWatches removeObjectForKey:subId];
        [cn1homeUndelivered removeObjectForKey:subId];
        if (keys != nil) {
            // Notifications are turned off only for characteristics no other
            // subscription still watches. Disabling one another watcher needs
            // is how a second screen silently stops updating.
            for (NSString *key in keys) {
                BOOL stillWatched = NO;
                for (NSString *other in cn1homeWatches) {
                    if ([[cn1homeWatches objectForKey:other]
                         containsObject:key]) {
                        stillWatched = YES;
                        break;
                    }
                }
                if (stillWatched) {
                    continue;
                }
                NSArray *parts = [key componentsSeparatedByString:@"\t"];
                if ([parts count] < 3) {
                    continue;
                }
                HMService *service = cn1homeFindService(
                    [parts objectAtIndex:0], [parts objectAtIndex:1]);
                HMCharacteristic *c = cn1homeFindCharacteristic(
                    service, [parts objectAtIndex:2]);
                if (c != nil) {
                    [c enableNotification:NO
                        completionHandler:^(NSError *ignored) { }];
                }
            }
        }
        [subId release];
    });
}

void com_codename1_impl_ios_IOSNative_homeDrainChanges___int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId) {
    cn1homeInit();
    JAVA_INT rid = requestId;
    cn1homeOnMain(^{
        __block int delivered = 0;
        for (NSString *subscriptionId in [cn1homeUndelivered allKeys]) {
            NSArray *records = [cn1homeUndelivered
                                objectForKey:subscriptionId];
            delivered += (int) [records count];
            com_codename1_impl_ios_IOSHomeCallbacks_changes___java_lang_String_java_lang_String(
                getThreadLocalData(),
                fromNSString(getThreadLocalData(), subscriptionId),
                fromNSString(getThreadLocalData(),
                             cn1homeJoinRecords(records)));
        }
        [cn1homeUndelivered removeAllObjects];

        // And then the traits HomeKit will not report on its own.
        //
        // A characteristic without SupportsEventNotification never fires the
        // delegate, so a subscription to one produced its initial value and
        // then nothing, for the life of the screen. TraitConstraint's
        // notifiesOnChange says such a subscription still works through
        // drainChanges(), and this is what makes that true: a live read of
        // each one, delivered only where the value actually moved.
        NSMutableArray *pollSubs = [NSMutableArray array];
        NSMutableArray *pollKeys = [NSMutableArray array];
        NSMutableArray *pollChars = [NSMutableArray array];
        for (NSString *subscriptionId in cn1homeWatches) {
            for (NSString *key in [cn1homeWatches
                                   objectForKey:subscriptionId]) {
                NSArray *parts = [key componentsSeparatedByString:@"\t"];
                if ([parts count] != 3) {
                    continue;
                }
                HMService *service = cn1homeFindService(
                    [parts objectAtIndex:0], [parts objectAtIndex:1]);
                HMCharacteristic *c = cn1homeFindCharacteristic(
                    service, [parts objectAtIndex:2]);
                if (c == nil
                        || [[c properties] containsObject:
                            HMCharacteristicPropertySupportsEventNotification]
                        || ![[c properties] containsObject:
                            HMCharacteristicPropertyReadable]) {
                    continue;
                }
                [pollSubs addObject:subscriptionId];
                [pollKeys addObject:key];
                [pollChars addObject:c];
            }
        }
        if ([pollChars count] == 0) {
            com_codename1_impl_ios_IOSHomeCallbacks_drained___int_int_java_lang_String(
                getThreadLocalData(), rid, delivered, JAVA_NULL);
            return;
        }
        __block NSUInteger remaining = [pollChars count];
        __block int polled = 0;
        for (NSUInteger i = 0; i < [pollChars count]; i++) {
            HMCharacteristic *c = [pollChars objectAtIndex:i];
            NSString *subscriptionId = [pollSubs objectAtIndex:i];
            NSString *key = [pollKeys objectAtIndex:i];
            [c readValueWithCompletionHandler:^(NSError *error) {
                if (error == nil) {
                    NSArray *parts =
                            [key componentsSeparatedByString:@"\t"];
                    NSString *record = cn1homeEncodeReading(
                        [parts objectAtIndex:0], [parts objectAtIndex:1],
                        [parts objectAtIndex:2], [c value], nil, nil);
                    // Compared without the timestamp, which is the last
                    // field and moves on every read: comparing whole records
                    // would report every poll as a change.
                    NSString *stateKey = [NSString stringWithFormat:@"%@\t%@",
                                          subscriptionId, key];
                    NSString *previous = [cn1homeLastPolled
                                          objectForKey:stateKey];
                    NSString *current = cn1homeReadingWithoutTimestamp(record);
                    if (previous == nil
                            || ![previous isEqualToString:current]) {
                        [cn1homeLastPolled setObject:current forKey:stateKey];
                        polled++;
                        com_codename1_impl_ios_IOSHomeCallbacks_changes___java_lang_String_java_lang_String(
                            getThreadLocalData(),
                            fromNSString(getThreadLocalData(), subscriptionId),
                            fromNSString(getThreadLocalData(), record));
                    }
                }
                remaining--;
                if (remaining == 0) {
                    com_codename1_impl_ios_IOSHomeCallbacks_drained___int_int_java_lang_String(
                        getThreadLocalData(), rid, delivered + polled,
                        JAVA_NULL);
                }
            }];
        }
    });
}

void
com_codename1_impl_ios_IOSNative_homeExecuteScene___int_java_lang_String_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT structureId, JAVA_OBJECT sceneId) {
    cn1homeInit();
    JAVA_INT rid = requestId;
    NSString *homeId = [toNSString(CN1_THREAD_STATE_PASS_ARG structureId)
                        retain];
    NSString *scene = [toNSString(CN1_THREAD_STATE_PASS_ARG sceneId) retain];
    cn1homeOnMain(^{
        HMHome *home = cn1homeFindHome(homeId);
        HMActionSet *actionSet = nil;
        if (home != nil) {
            for (HMActionSet *candidate in [home actionSets]) {
                if ([cn1homeUuid([candidate uniqueIdentifier])
                     isEqualToString:scene]) {
                    actionSet = candidate;
                    break;
                }
            }
        }
        if (actionSet == nil) {
            com_codename1_impl_ios_IOSHomeCallbacks_sceneResult___int_java_lang_String_java_lang_String_java_lang_String(
                getThreadLocalData(), rid, JAVA_NULL,
                fromNSString(getThreadLocalData(), homeId),
                fromNSString(getThreadLocalData(),
                             cn1homeError(@"INVALID_ARGUMENT", nil)));
            [homeId release];
            [scene release];
            return;
        }
        NSString *line = cn1homeJoinFields(
            [NSArray arrayWithObjects:scene, [actionSet name],
             [NSString stringWithFormat:@"%d",
              cn1homeSceneTypeOrdinal([actionSet actionSetType])], @"1", nil]);
        [home executeActionSet:actionSet completionHandler:^(NSError *error) {
            com_codename1_impl_ios_IOSHomeCallbacks_sceneResult___int_java_lang_String_java_lang_String_java_lang_String(
                getThreadLocalData(), rid,
                error == nil ? fromNSString(getThreadLocalData(), line)
                             : JAVA_NULL,
                fromNSString(getThreadLocalData(), homeId),
                error == nil ? JAVA_NULL
                             : fromNSString(getThreadLocalData(),
                                            cn1homeError(
                                                cn1homeErrorName(error),
                                                error)));
            [homeId release];
            [scene release];
        }];
    });
}

void
com_codename1_impl_ios_IOSNative_homeCreateScene___int_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT structureId, JAVA_OBJECT name, JAVA_OBJECT accessoryIds,
        JAVA_OBJECT serviceIds, JAVA_OBJECT traitIds, JAVA_OBJECT kinds,
        JAVA_OBJECT numericValues, JAVA_OBJECT stringValues,
        JAVA_OBJECT unitWireIds) {
    cn1homeInit();
    JAVA_INT rid = requestId;
    NSString *homeId = [toNSString(CN1_THREAD_STATE_PASS_ARG structureId)
                        retain];
    NSString *sceneName = [toNSString(CN1_THREAD_STATE_PASS_ARG name) retain];
    NSArray *accessories = [cn1homeSplit(
        toNSString(CN1_THREAD_STATE_PASS_ARG accessoryIds)) retain];
    NSArray *services = [cn1homeSplit(
        toNSString(CN1_THREAD_STATE_PASS_ARG serviceIds)) retain];
    NSArray *traits = [cn1homeSplit(
        toNSString(CN1_THREAD_STATE_PASS_ARG traitIds)) retain];
    NSArray *numbers = [cn1homeSplit(
        toNSString(CN1_THREAD_STATE_PASS_ARG numericValues)) retain];
    cn1homeOnMain(^{
        // One place to hand back the retained arguments, so every exit --
        // and there are six -- releases exactly once.
        void (^releaseArgs)(void) = ^{
            [homeId release];
            [sceneName release];
            [accessories release];
            [services release];
            [traits release];
            [numbers release];
        };
        HMHome *home = cn1homeFindHome(homeId);
        if (home == nil) {
            com_codename1_impl_ios_IOSHomeCallbacks_sceneResult___int_java_lang_String_java_lang_String_java_lang_String(
                getThreadLocalData(), rid, JAVA_NULL,
                fromNSString(getThreadLocalData(), homeId),
                fromNSString(getThreadLocalData(),
                             cn1homeError(@"INVALID_ARGUMENT", nil)));
            releaseArgs();
            return;
        }
        [home addActionSetWithName:sceneName
                 completionHandler:^(HMActionSet *actionSet, NSError *error) {
            if (error != nil || actionSet == nil) {
                com_codename1_impl_ios_IOSHomeCallbacks_sceneResult___int_java_lang_String_java_lang_String_java_lang_String(
                    getThreadLocalData(), rid, JAVA_NULL,
                    fromNSString(getThreadLocalData(), homeId),
                    fromNSString(getThreadLocalData(),
                                 cn1homeError(cn1homeErrorName(error),
                                              error)));
                releaseArgs();
                return;
            }
            __block NSUInteger remaining = [traits count];
            // A scene is all-or-nothing on purpose. A half-built "Good night"
            // that silently drops the lock is worse than one that failed:
            // the user is told it exists and runs it every night. So a failed
            // action fails the whole request, and the action set HomeKit has
            // already created is removed rather than left in their home.
            __block NSUInteger failedActions = 0;
            __block NSString *firstFailure = nil;
            void (^answer)(NSString *) = ^(NSString *encodedError) {
                cn1homeRebuildSnapshot();
                NSString *line = encodedError != nil ? nil
                        : cn1homeJoinFields(
                            [NSArray arrayWithObjects:
                             cn1homeUuid([actionSet uniqueIdentifier]),
                             sceneName, @"4", @"1", nil]);
                com_codename1_impl_ios_IOSHomeCallbacks_sceneResult___int_java_lang_String_java_lang_String_java_lang_String(
                    getThreadLocalData(), rid,
                    line == nil ? JAVA_NULL
                                : fromNSString(getThreadLocalData(), line),
                    fromNSString(getThreadLocalData(), homeId),
                    encodedError == nil
                        ? JAVA_NULL
                        : fromNSString(getThreadLocalData(), encodedError));
                [firstFailure release];
                firstFailure = nil;
                releaseArgs();
            };
            void (^done)(void) = ^{
                if (failedActions == 0) {
                    answer(nil);
                    return;
                }
                NSString *encoded = [[firstFailure retain] autorelease];
                [home removeActionSet:actionSet
                    completionHandler:^(NSError *removeError) {
                    if (removeError == nil) {
                        answer(encoded);
                        return;
                    }
                    // The rollback failed too, so a half-built scene IS in
                    // the user's home. Saying only "creation failed" invites
                    // a retry that leaves a second one beside it, and the
                    // caller cannot see either. The scene id goes back with
                    // the error so it can be found and removed.
                    answer(cn1homeError(@"IO_ERROR",
                        [NSError errorWithDomain:@"CN1SmartHome" code:0
                            userInfo:[NSDictionary dictionaryWithObject:
                                [NSString stringWithFormat:
                                    @"the scene could not be created and the "
                                     "partly built one could not be removed "
                                     "either; it is in the home as \"%@\" "
                                     "and has to be deleted", sceneName]
                                forKey:NSLocalizedDescriptionKey]]));
                }];
            };
            if (remaining == 0) {
                done();
                return;
            }
            for (NSUInteger i = 0; i < [traits count]; i++) {
                HMService *service = cn1homeFindService(
                    [accessories objectAtIndex:i], [services objectAtIndex:i]);
                HMCharacteristic *c = cn1homeFindCharacteristic(
                    service, [traits objectAtIndex:i]);
                NSArray *entry = cn1homeEntryFor([traits objectAtIndex:i]);
                int conversion = entry == nil ? -1
                        : [[entry objectAtIndex:3] intValue];
                double numeric = i < [numbers count]
                        ? [[numbers objectAtIndex:i] doubleValue] : 0;
                // The same fan handling the direct write path does, for the
                // same reason: HomeKit's mode characteristic cannot say
                // "stopped", and cannot carry a speed. A scene that only set
                // the mode left the fan running at whatever speed it was on,
                // every time it ran.
                id target = nil;
                if (conversion == CN1_HC_FAN_MODE && (int) numeric == 0) {
                    c = cn1homeFindCharacteristic(service, @"on_off");
                    conversion = CN1_HC_BOOL;
                    numeric = 0;
                }
                if (c != nil && entry != nil) {
                    target = cn1homeToHomeKit(conversion, numeric);
                }
                if (target != nil && conversion == CN1_HC_FAN_MODE) {
                    // A running fan mode is three characteristics, as in the
                    // direct write path: on, at the speed the mode names, in
                    // manual. A scene that set only the mode left the fan
                    // stopped, or at its old speed, every time it ran.
                    double speed = cn1homeFanModeSpeed((int) numeric);
                    NSMutableArray *extraChars = [NSMutableArray array];
                    NSMutableArray *extraValues = [NSMutableArray array];
                    HMCharacteristic *power = cn1homeFindCharacteristic(
                        service, @"on_off");
                    if (power != nil) {
                        [extraChars addObject:power];
                        [extraValues addObject:[NSNumber numberWithBool:YES]];
                    }
                    HMCharacteristic *speedChar = speed < 0 ? nil
                            : cn1homeFindCharacteristic(service, @"fan_speed");
                    if (speed >= 0 && speedChar == nil) {
                        // As in the direct write path: a speed-naming mode on
                        // a fan with no RotationSpeed is a scene that would
                        // run every night and never set the speed asked for.
                        failedActions++;
                        if (firstFailure == nil) {
                            firstFailure = [cn1homeError(
                                @"TRAIT_NOT_SUPPORTED", nil) retain];
                        }
                    }
                    if (speedChar != nil) {
                        [extraChars addObject:speedChar];
                        [extraValues addObject:
                            [NSNumber numberWithDouble:speed]];
                    }
                    for (NSUInteger e = 0; e < [extraChars count]; e++) {
                        HMCharacteristicWriteAction *extra =
                                [[HMCharacteristicWriteAction alloc]
                                 initWithCharacteristic:
                                     [extraChars objectAtIndex:e]
                                            targetValue:
                                     [extraValues objectAtIndex:e]];
                        remaining++;
                        [actionSet addAction:extra
                           completionHandler:^(NSError *actionError) {
                            if (actionError != nil) {
                                failedActions++;
                                if (firstFailure == nil) {
                                    firstFailure = [cn1homeError(
                                        cn1homeErrorName(actionError),
                                        actionError) retain];
                                }
                            }
                            remaining--;
                            if (remaining == 0) {
                                done();
                            }
                        }];
                        [extra release];
                    }
                }
                if (target == nil) {
                    failedActions++;
                    if (firstFailure == nil) {
                        firstFailure = [cn1homeError(
                            c == nil ? @"TRAIT_NOT_SUPPORTED"
                                     : @"INVALID_ARGUMENT", nil) retain];
                    }
                    remaining--;
                    if (remaining == 0) {
                        done();
                    }
                    continue;
                }
                HMCharacteristicWriteAction *action =
                        [[HMCharacteristicWriteAction alloc]
                         initWithCharacteristic:c targetValue:target];
                [actionSet addAction:action
                   completionHandler:^(NSError *actionError) {
                    if (actionError != nil) {
                        failedActions++;
                        if (firstFailure == nil) {
                            firstFailure = [cn1homeError(
                                cn1homeErrorName(actionError),
                                actionError) retain];
                        }
                    }
                    remaining--;
                    if (remaining == 0) {
                        done();
                    }
                }];
                [action release];
            }
        }];
    });
}

void
com_codename1_impl_ios_IOSNative_homeDeleteScene___int_java_lang_String_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT structureId, JAVA_OBJECT sceneId) {
    cn1homeInit();
    JAVA_INT rid = requestId;
    NSString *homeId = [toNSString(CN1_THREAD_STATE_PASS_ARG structureId)
                        retain];
    NSString *scene = [toNSString(CN1_THREAD_STATE_PASS_ARG sceneId) retain];
    cn1homeOnMain(^{
        HMHome *home = cn1homeFindHome(homeId);
        HMActionSet *actionSet = nil;
        if (home != nil) {
            for (HMActionSet *candidate in [home actionSets]) {
                if ([cn1homeUuid([candidate uniqueIdentifier])
                     isEqualToString:scene]) {
                    actionSet = candidate;
                    break;
                }
            }
        }
        if (actionSet == nil) {
            com_codename1_impl_ios_IOSHomeCallbacks_sceneResult___int_java_lang_String_java_lang_String_java_lang_String(
                getThreadLocalData(), rid, JAVA_NULL,
                fromNSString(getThreadLocalData(), homeId), JAVA_NULL);
            [homeId release];
            [scene release];
            return;
        }
        [home removeActionSet:actionSet completionHandler:^(NSError *error) {
            cn1homeRebuildSnapshot();
            com_codename1_impl_ios_IOSHomeCallbacks_sceneResult___int_java_lang_String_java_lang_String_java_lang_String(
                getThreadLocalData(), rid, JAVA_NULL,
                fromNSString(getThreadLocalData(), homeId),
                error == nil ? JAVA_NULL
                             : fromNSString(getThreadLocalData(),
                                            cn1homeError(
                                                cn1homeErrorName(error),
                                                error)));
            [homeId release];
            [scene release];
        }];
    });
}

void com_codename1_impl_ios_IOSNative_homeIdentify___int_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT accessoryId) {
    cn1homeInit();
    JAVA_INT rid = requestId;
    NSString *identifier = [toNSString(CN1_THREAD_STATE_PASS_ARG accessoryId)
                            retain];
    cn1homeOnMain(^{
        HMAccessory *accessory = cn1homeFindAccessory(identifier);
        if (accessory == nil) {
            com_codename1_impl_ios_IOSHomeCallbacks_identifyResult___int_java_lang_String(
                getThreadLocalData(), rid,
                fromNSString(getThreadLocalData(),
                             cn1homeError(@"ACCESSORY_NOT_FOUND", nil)));
            [identifier release];
            return;
        }
        [accessory identifyWithCompletionHandler:^(NSError *error) {
            com_codename1_impl_ios_IOSHomeCallbacks_identifyResult___int_java_lang_String(
                getThreadLocalData(), rid,
                error == nil ? JAVA_NULL
                             : fromNSString(getThreadLocalData(),
                                            cn1homeError(
                                                cn1homeErrorName(error),
                                                error)));
            [identifier release];
        }];
    });
}

// ---------------------------------------------------------------------
// Commissioning
//
// MatterSupport is a Swift-only framework: MatterAddDeviceRequest is a Swift
// struct with an async perform(), and there is no Objective-C interface to
// call. So the actual flow lives in CN1MatterCommissioning.swift, which
// exposes an @objc shim, and this file only forwards to it -- guarded, so an
// app that never references com.codename1.home.commissioning does not carry
// the framework, the entitlement, the app group or the generated extension
// target.
// ---------------------------------------------------------------------

#ifdef CN1_INCLUDE_MATTER_SETUP

/// The Swift half calls back through this, found by name.
///
/// #### Why both directions go through NSClassFromString
///
/// Objective-C reaches Swift in a mixed target through a generated
/// `<ProductModuleName>-Swift.h`, and Swift reaches Objective-C through a
/// bridging header. Both names depend on the *application's* module name,
/// which is whatever the developer called their project -- and this file
/// ships inside the port, long before that name exists.
///
/// So neither header is used. Each side looks the other up by name and
/// exchanges one NSDictionary, which `performSelector:withObject:` can carry
/// in a single argument. Dynamic dispatch costs a lookup per commissioning
/// flow, which happens once per accessory a user adds by hand.
@interface CN1MatterBridge : NSObject
+ (void)deliver:(NSDictionary *)result;
@end

@implementation CN1MatterBridge

+ (void)deliver:(NSDictionary *)result {
    POOL_BEGIN();
    JAVA_INT rid = (JAVA_INT) [[result objectForKey:@"requestId"] intValue];
    NSString *accessoryId = [result objectForKey:@"accessoryId"];
    NSString *accessoryName = [result objectForKey:@"accessoryName"];
    NSString *structureId = [result objectForKey:@"structureId"];
    NSString *error = [result objectForKey:@"error"];
    // MatterSupport tells the app that a device was added and does not say
    // which. The accessory joins the user's HomeKit home and turns up in the
    // graph on the next refresh, so this reports "not mine" whenever the id
    // is absent -- which is what CommissioningResult.wasCommissionedToThisApp
    // exists to make un-missable.
    com_codename1_impl_ios_IOSHomeCallbacks_commissioningResult___int_java_lang_String_java_lang_String_java_lang_String_int_java_lang_String(
        getThreadLocalData(), rid,
        accessoryId == nil ? JAVA_NULL
                           : fromNSString(getThreadLocalData(), accessoryId),
        accessoryName == nil ? JAVA_NULL
                             : fromNSString(getThreadLocalData(),
                                            accessoryName),
        structureId == nil ? JAVA_NULL
                           : fromNSString(getThreadLocalData(), structureId),
        accessoryId == nil ? 0 : 1,
        error == nil ? JAVA_NULL : fromNSString(getThreadLocalData(), error));
    // A commissioned accessory is a graph change, and the app has no other
    // way to learn about it: HMHomeManagerDelegate does fire, but only once
    // the home database has caught up, and an app that refreshed on the
    // result alone would find nothing.
    //
    // Only on success. A cancelled sheet -- by far the most common outcome --
    // changes nothing, and announcing ACCESSORY_ADDED for it makes every
    // structure listener refresh, and some of them show a device that was
    // never added.
    if (error == nil) {
        cn1homeRebuildSnapshot();
        cn1homeNotifyStructure(CN1_HOME_CHANGE_ACCESSORY_ADDED, structureId,
                               accessoryId);
    }
    POOL_END();
}

@end
#endif

JAVA_INT
com_codename1_impl_ios_IOSNative_homeCommissioningStyle___R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef CN1_INCLUDE_MATTER_SETUP
    return CN1_HOME_COMMISSION_OS_UI;
#else
    // Not "none": the Apple Home app can still add the accessory, and telling
    // the app that is more useful than telling it nothing is possible.
    return CN1_HOME_COMMISSION_APP_HANDOFF;
#endif
}

void
com_codename1_impl_ios_IOSNative_homeCommission___int_java_lang_String_java_lang_String_java_lang_String_java_lang_String_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT setupPayload, JAVA_OBJECT structureId, JAVA_OBJECT roomId,
        JAVA_OBJECT suggestedName, JAVA_INT timeoutMillis) {
#ifdef CN1_INCLUDE_MATTER_SETUP
    POOL_BEGIN();
    NSString *payload = toNSString(CN1_THREAD_STATE_PASS_ARG setupPayload);
    NSString *home = toNSString(CN1_THREAD_STATE_PASS_ARG structureId);
    NSString *room = toNSString(CN1_THREAD_STATE_PASS_ARG roomId);
    NSString *name = toNSString(CN1_THREAD_STATE_PASS_ARG suggestedName);
    NSMutableDictionary *request = [NSMutableDictionary dictionary];
    [request setObject:[NSNumber numberWithInt:(int) requestId]
                forKey:@"requestId"];
    [request setObject:payload == nil ? @"" : payload forKey:@"setupPayload"];
    [request setObject:home == nil ? @"" : home forKey:@"structureId"];
    [request setObject:room == nil ? @"" : room forKey:@"roomId"];
    [request setObject:name == nil ? @"" : name forKey:@"suggestedName"];
    [request setObject:[NSNumber numberWithInt:(int) timeoutMillis]
                forKey:@"timeoutMillis"];
    // Named at runtime; see CN1MatterBridge for why neither side imports the
    // other's generated header.
    Class swiftSide = NSClassFromString(@"CN1MatterCommissioning");
    SEL commission = NSSelectorFromString(@"commission:");
    if (swiftSide == nil || ![swiftSide respondsToSelector:commission]) {
        // The define is on and the Swift half is missing, which means the
        // build enabled commissioning and did not add the source. Reported
        // rather than crashed, and named clearly enough to be actionable --
        // a silent no-op here would look like a user who cancelled.
        com_codename1_impl_ios_IOSHomeCallbacks_commissioningResult___int_java_lang_String_java_lang_String_java_lang_String_int_java_lang_String(
            CN1_THREAD_STATE_PASS_ARG requestId, JAVA_NULL, JAVA_NULL,
            JAVA_NULL, 0,
            fromNSString(CN1_THREAD_STATE_PASS_ARG
                         @"NOT_CONFIGURED\tthis build enabled Matter setup "
                         "but did not include CN1MatterCommissioning.swift"));
        POOL_END();
        return;
    }
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Warc-performSelector-leaks"
    [swiftSide performSelector:commission withObject:request];
#pragma clang diagnostic pop
    POOL_END();
#else
    POOL_BEGIN();
    com_codename1_impl_ios_IOSHomeCallbacks_commissioningResult___int_java_lang_String_java_lang_String_java_lang_String_int_java_lang_String(
        CN1_THREAD_STATE_PASS_ARG requestId, JAVA_NULL, JAVA_NULL, JAVA_NULL, 0,
        fromNSString(CN1_THREAD_STATE_PASS_ARG
                     cn1homeError(@"COMMISSIONING_UNAVAILABLE", nil)));
    POOL_END();
#endif
}

#else // CN1_INCLUDE_HOMEKIT

// ---------------------------------------------------------------------
// Trampolines for a build that never touched com.codename1.home
//
// Every native declared in IOSNative.java has to resolve or the app will not
// link, and each one answers "unsupported" so SmartHome reports
// NOT_SUPPORTED and every operation fails fast. Nothing here imports HomeKit,
// so a home-free app carries none of its symbols and needs none of its
// entitlement.
// ---------------------------------------------------------------------

#include "com_codename1_impl_ios_IOSHomeCallbacks.h"

JAVA_BOOLEAN
com_codename1_impl_ios_IOSNative_homeSupported___R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_FALSE;
}

JAVA_INT
com_codename1_impl_ios_IOSNative_homeAvailability___R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return CN1_HOME_AVAIL_NOT_SUPPORTED;
}

JAVA_INT
com_codename1_impl_ios_IOSNative_homeAuthorizationStatus___R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return CN1_HOME_AUTH_UNKNOWN;
}

JAVA_OBJECT
com_codename1_impl_ios_IOSNative_homeConfigurationProblems___R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_NULL;
}

void com_codename1_impl_ios_IOSNative_homeStart___int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId) {
    com_codename1_impl_ios_IOSHomeCallbacks_started___int_int_java_lang_String(
        CN1_THREAD_STATE_PASS_ARG requestId, CN1_HOME_AVAIL_NOT_SUPPORTED,
        fromNSString(CN1_THREAD_STATE_PASS_ARG
                     @"NOT_SUPPORTED\tthis build did not link HomeKit"));
}

void com_codename1_impl_ios_IOSNative_homeStop__(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
}

void com_codename1_impl_ios_IOSNative_homeRequestAuthorization___int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId) {
    com_codename1_impl_ios_IOSHomeCallbacks_authorization___int_int_java_lang_String(
        CN1_THREAD_STATE_PASS_ARG requestId, CN1_HOME_AUTH_UNKNOWN,
        fromNSString(CN1_THREAD_STATE_PASS_ARG
                     @"NOT_SUPPORTED\tthis build did not link HomeKit"));
}

JAVA_BOOLEAN
com_codename1_impl_ios_IOSNative_homeOpenSettings___R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_FALSE;
}

JAVA_BOOLEAN
com_codename1_impl_ios_IOSNative_homeOpenEcosystemApp___R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_FALSE;
}

JAVA_OBJECT
com_codename1_impl_ios_IOSNative_homeStructures___R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_NULL;
}

// Written out rather than generated, for the reason given at the HomeKit
// copies of these: a name assembled by token pasting is invisible to the
// native signature check and to grep.
JAVA_OBJECT
com_codename1_impl_ios_IOSNative_homeRooms___java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT key) {
    return JAVA_NULL;
}

JAVA_OBJECT
com_codename1_impl_ios_IOSNative_homeZones___java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT key) {
    return JAVA_NULL;
}

JAVA_OBJECT
com_codename1_impl_ios_IOSNative_homeAccessories___java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT key) {
    return JAVA_NULL;
}

JAVA_OBJECT
com_codename1_impl_ios_IOSNative_homeServices___java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT key) {
    return JAVA_NULL;
}

JAVA_OBJECT
com_codename1_impl_ios_IOSNative_homeScenes___java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT key) {
    return JAVA_NULL;
}


JAVA_OBJECT
com_codename1_impl_ios_IOSNative_homeTraits___java_lang_String_java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT accessoryId,
        JAVA_OBJECT serviceId) {
    return JAVA_NULL;
}

JAVA_OBJECT
com_codename1_impl_ios_IOSNative_homeSceneActions___java_lang_String_java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT structureId,
        JAVA_OBJECT sceneId) {
    return JAVA_NULL;
}

void com_codename1_impl_ios_IOSNative_homeRefresh___int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId) {
    com_codename1_impl_ios_IOSHomeCallbacks_refreshed___int_java_lang_String(
        CN1_THREAD_STATE_PASS_ARG requestId,
        fromNSString(CN1_THREAD_STATE_PASS_ARG
                     @"NOT_SUPPORTED\tthis build did not link HomeKit"));
}

void
com_codename1_impl_ios_IOSNative_homeReadTraits___int_java_lang_String_java_lang_String_java_lang_String_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT accessoryIds, JAVA_OBJECT serviceIds, JAVA_OBJECT traitIds,
        JAVA_BOOLEAN allowCached) {
    com_codename1_impl_ios_IOSHomeCallbacks_readings___int_java_lang_String_java_lang_String(
        CN1_THREAD_STATE_PASS_ARG requestId, JAVA_NULL,
        fromNSString(CN1_THREAD_STATE_PASS_ARG
                     @"NOT_SUPPORTED\tthis build did not link HomeKit"));
}

void
com_codename1_impl_ios_IOSNative_homeWriteTraits___int_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT accessoryIds, JAVA_OBJECT serviceIds, JAVA_OBJECT traitIds,
        JAVA_OBJECT kinds, JAVA_OBJECT numericValues, JAVA_OBJECT stringValues,
        JAVA_OBJECT unitWireIds, JAVA_OBJECT authorizationData) {
    com_codename1_impl_ios_IOSHomeCallbacks_writeResults___int_java_lang_String_java_lang_String(
        CN1_THREAD_STATE_PASS_ARG requestId, JAVA_NULL,
        fromNSString(CN1_THREAD_STATE_PASS_ARG
                     @"NOT_SUPPORTED\tthis build did not link HomeKit"));
}

void
com_codename1_impl_ios_IOSNative_homeSubscribe___int_java_lang_String_java_lang_String_java_lang_String_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT subscriptionId, JAVA_OBJECT accessoryIds,
        JAVA_OBJECT serviceIds, JAVA_OBJECT traitIds) {
}

void com_codename1_impl_ios_IOSNative_homeUnsubscribe___java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me,
        JAVA_OBJECT subscriptionId) {
}

void com_codename1_impl_ios_IOSNative_homeDrainChanges___int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId) {
    com_codename1_impl_ios_IOSHomeCallbacks_drained___int_int_java_lang_String(
        CN1_THREAD_STATE_PASS_ARG requestId, 0, JAVA_NULL);
}

void
com_codename1_impl_ios_IOSNative_homeExecuteScene___int_java_lang_String_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT structureId, JAVA_OBJECT sceneId) {
    com_codename1_impl_ios_IOSHomeCallbacks_sceneResult___int_java_lang_String_java_lang_String_java_lang_String(
        CN1_THREAD_STATE_PASS_ARG requestId, JAVA_NULL, JAVA_NULL,
        fromNSString(CN1_THREAD_STATE_PASS_ARG
                     @"NOT_SUPPORTED\tthis build did not link HomeKit"));
}

void
com_codename1_impl_ios_IOSNative_homeCreateScene___int_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT structureId, JAVA_OBJECT name, JAVA_OBJECT accessoryIds,
        JAVA_OBJECT serviceIds, JAVA_OBJECT traitIds, JAVA_OBJECT kinds,
        JAVA_OBJECT numericValues, JAVA_OBJECT stringValues,
        JAVA_OBJECT unitWireIds) {
    com_codename1_impl_ios_IOSHomeCallbacks_sceneResult___int_java_lang_String_java_lang_String_java_lang_String(
        CN1_THREAD_STATE_PASS_ARG requestId, JAVA_NULL, JAVA_NULL,
        fromNSString(CN1_THREAD_STATE_PASS_ARG
                     @"NOT_SUPPORTED\tthis build did not link HomeKit"));
}

void
com_codename1_impl_ios_IOSNative_homeDeleteScene___int_java_lang_String_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT structureId, JAVA_OBJECT sceneId) {
    com_codename1_impl_ios_IOSHomeCallbacks_sceneResult___int_java_lang_String_java_lang_String_java_lang_String(
        CN1_THREAD_STATE_PASS_ARG requestId, JAVA_NULL, JAVA_NULL,
        fromNSString(CN1_THREAD_STATE_PASS_ARG
                     @"NOT_SUPPORTED\tthis build did not link HomeKit"));
}

JAVA_INT
com_codename1_impl_ios_IOSNative_homeCommissioningStyle___R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return CN1_HOME_COMMISSION_NONE;
}

void
com_codename1_impl_ios_IOSNative_homeCommission___int_java_lang_String_java_lang_String_java_lang_String_java_lang_String_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT setupPayload, JAVA_OBJECT structureId, JAVA_OBJECT roomId,
        JAVA_OBJECT suggestedName, JAVA_INT timeoutMillis) {
    com_codename1_impl_ios_IOSHomeCallbacks_commissioningResult___int_java_lang_String_java_lang_String_java_lang_String_int_java_lang_String(
        CN1_THREAD_STATE_PASS_ARG requestId, JAVA_NULL, JAVA_NULL, JAVA_NULL, 0,
        fromNSString(CN1_THREAD_STATE_PASS_ARG
                     @"COMMISSIONING_UNAVAILABLE\tthis build did not link "
                     "MatterSupport"));
}

void com_codename1_impl_ios_IOSNative_homeIdentify___int_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT accessoryId) {
    com_codename1_impl_ios_IOSHomeCallbacks_identifyResult___int_java_lang_String(
        CN1_THREAD_STATE_PASS_ARG requestId,
        fromNSString(CN1_THREAD_STATE_PASS_ARG
                     @"NOT_SUPPORTED\tthis build did not link HomeKit"));
}

#endif // CN1_INCLUDE_HOMEKIT
