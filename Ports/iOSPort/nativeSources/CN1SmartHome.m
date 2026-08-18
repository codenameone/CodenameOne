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

// Characteristics nobody subscribed to, whose notifications a subscription
// nonetheless depends on: subscriptionId -> NSSet of
// "accessoryId \t serviceId \t traitId". Today that is the thermostat mode
// behind a TARGET_TEMPERATURE watch -- crossing into AUTO changes what the
// setpoint means without touching the setpoint's own characteristic, and
// HomeKit notifies per characteristic, so with no registration on the mode
// the derived update below never fires and the listener keeps a number the
// thermostat has stopped aiming for. Kept apart from cn1homeWatches because
// these keys must NOT match a delivery: the app asked for a setpoint, not a
// mode.
static NSMutableDictionary *cn1homeDependencies = nil;

// Watched characteristics whose enableNotification: failed, keyed
// "subscriptionId \t accessoryId \t serviceId \t traitId". HomeKit is the
// backend the framework reports as push, so one of these cannot simply be
// left to drainChanges(): an app told isPushDelivery() is true never calls
// it. They are polled and retried on a timer instead, until the registration
// takes.
static NSMutableSet *cn1homeNotifyFailed = nil;
static BOOL cn1homeRecoveryArmed = NO;

// Seconds between recovery passes. Long enough that a permanently broken
// accessory costs almost nothing, short enough that a light that dropped off
// Wi-Fi for a moment feels live again when it comes back.
#define CN1_HOME_RECOVERY_SECONDS 10

// The request id of a start() that is waiting for the first
// homeManagerDidUpdateHomes:. Zero when nothing is waiting.
static JAVA_INT cn1homePendingStart = 0;

// The request ids of every requestAuthorization() waiting for the user to
// answer the system prompt, in the order they asked. Empty when nothing is
// waiting.
//
// All of them, because creating the manager is what prompts and HomeKit shows
// that prompt once: a caller who arrives while it is on screen has no second
// prompt to wait for. Answered UNKNOWN on the spot, an app whose startup and
// whose first screen both ask -- which is the ordinary shape -- had one of
// them react to a refusal moments before the user granted it.
static NSMutableArray *cn1homePendingAuth = nil;

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
    // Guarded: the constant arrived in iOS 18, and this port deploys lower.
    // It is weak-imported, so on an older system it is simply null and the
    // comparison would answer NO -- the guard says that on purpose rather
    // than relying on it.
    if (@available(iOS 18.0, watchOS 11.0, tvOS 18.0, *)) {
        if ([t isEqualToString:HMAccessoryCategoryTypeTelevision]) {
            return 11;
        }
    }
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

// HeatingCoolingMode.AUTO's ordinal, which is what a write of the mode
// arrives as: the wire carries the canonical ordinal, and cn1homeToHomeKit
// turns it into HomeKit's own value on the way out. Named because reading a
// bare 3 out of a batch, next to the separate 3 that HomeKit's characteristic
// reports, is how the two get confused.
#define CN1_HOME_MODE_AUTO 3

/// Whether a scene leaves this thermostat in AUTO, where its single setpoint
/// means nothing.
///
/// The scene's own mode action decides it wherever it sits in the list, and
/// the thermostat's current mode decides it when the scene sets none.
///
/// #### Parameters
///
/// - `accessories`, `services`, `traits`, `numbers`: the scene's actions
///
/// - `index`: the setpoint action being judged
///
/// - `service`: the service that action names
///
/// #### Returns
///
/// `true` when the setpoint would be meaningless once the scene has run
static BOOL cn1homeSceneEndsInAuto(NSArray *accessories, NSArray *services,
                                   NSArray *traits, NSArray *numbers,
                                   NSUInteger index, HMService *service) {
    for (NSUInteger j = 0; j < [traits count]; j++) {
        if (![[traits objectAtIndex:j]
              isEqualToString:@"target_heating_cooling"]
                || ![[accessories objectAtIndex:j]
                     isEqualToString:[accessories objectAtIndex:index]]
                || ![[services objectAtIndex:j]
                     isEqualToString:[services objectAtIndex:index]]) {
            continue;
        }
        return j < [numbers count]
                && (int) [[numbers objectAtIndex:j] doubleValue]
                    == CN1_HOME_MODE_AUTO;
    }
    return cn1homeHasNoValueNow(service, @"target_temperature");
}

/// The value to report for a trait, honouring the modes that erase one.
///
/// Every path that turns a live characteristic into a reading goes through
/// here, because the rule is about the trait rather than about the path: a
/// setpoint read one way and polled another must not disagree about whether
/// the thermostat has a single target at all.
///
/// #### Parameters
///
/// - `service`: the service the characteristic belongs to
///
/// - `traitId`: the canonical trait
///
/// - `c`: the characteristic
///
/// #### Returns
///
/// the characteristic's value, or nil when the trait has none right now
static id cn1homeValueForReading(HMService *service, NSString *traitId,
                                 HMCharacteristic *c) {
    return cn1homeHasNoValueNow(service, traitId) ? nil : [c value];
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
        // Administrator, which is NOT ownership: HomeKit has no owner concept
        // at all -- HMHomeAccessControl offers isAdministrator and nothing
        // else -- and an invited resident can be made an administrator. It is
        // exactly the right answer for whether this user may author scenes,
        // and the wrong one for isOwner(), which is reported false on iOS the
        // same way isPrimary is, and for the same reason: the platform cannot
        // say, and a guess is worse than an honest no.
        HMHomeAccessControl *access =
                [home homeAccessControlForUser:[home currentUser]];
        BOOL administrator = access != nil ? [access isAdministrator] : NO;
        [structures addObject:cn1homeJoinFields(
            [NSArray arrayWithObjects:homeId, [home name],
             cn1homeFlag(isPrimary), cn1homeFlag(NO),
             cn1homeFlag(administrator), nil])];

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
    if ([cn1homePendingAuth count] == 0) {
        return;
    }
    JAVA_INT status = cn1homeAuthStatus();
    if (!force && status == CN1_HOME_AUTH_NOT_DETERMINED) {
        return;
    }
    // Copied and cleared before anything is delivered: a callback runs Java,
    // and Java can call back in here -- requestAuthorization() again, or
    // stop() -- while this is still walking the list.
    NSArray *waiting = [[cn1homePendingAuth copy] autorelease];
    [cn1homePendingAuth removeAllObjects];
    for (NSNumber *pending in waiting) {
        com_codename1_impl_ios_IOSHomeCallbacks_authorization___int_int_java_lang_String(
            getThreadLocalData(), (JAVA_INT) [pending intValue], status,
            JAVA_NULL);
    }
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

// Defined with the recovery machinery it shares state with, below the
// delegate that calls it.
static void cn1homeRebindWatches(NSString *accessoryId);

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
    JAVA_INT auth = cn1homeAuthStatus();
    if (cn1homePendingStart != 0
            && (auth == CN1_HOME_AUTH_DENIED
                || auth == CN1_HOME_AUTH_RESTRICTED)) {
        // start() waits for homeManagerDidUpdateHomes:, and on a refusal
        // there may be no database load left to deliver it -- so without this
        // the first refresh() and everything afterStart() deferred behind it
        // would sit pending for the life of the process. The availability is
        // built here rather than read back through homeAvailability(),
        // because that one answers PERMISSION_REQUIRED while the homes have
        // not loaded, which on a refusal they never will.
        JAVA_INT pending = cn1homePendingStart;
        cn1homePendingStart = 0;
        com_codename1_impl_ios_IOSHomeCallbacks_started___int_int_java_lang_String(
            getThreadLocalData(), pending,
            auth == CN1_HOME_AUTH_RESTRICTED
                    ? CN1_HOME_AVAIL_RESTRICTED
                    : CN1_HOME_AVAIL_PERMISSION_DENIED,
            JAVA_NULL);
    }
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

// An accessory whose services changed -- a firmware update that adds one, a
// reconfiguration that renames one. The encoded services and their trait
// constraints are built from exactly this, so without these the app keeps
// offering controls that no longer exist and misses the ones that appeared.
//
// The delegates have to be reattached too: a service added now brings
// characteristics this bridge has never registered for.
- (void)accessoryDidUpdateServices:(HMAccessory *)accessory {
    cn1homeRebuildSnapshot();
    cn1homeAttachDelegates();
    // And the watches, because an update can hand back a different
    // HMCharacteristic object under the same identifier and a notification
    // registration belongs to the object -- see cn1homeRebindWatches.
    cn1homeRebindWatches(cn1homeUuid([accessory uniqueIdentifier]));
    cn1homeNotifyStructure(CN1_HOME_CHANGE_STRUCTURES, nil,
                           cn1homeUuid([accessory uniqueIdentifier]));
}

- (void)accessory:(HMAccessory *)accessory
        didUpdateNameForService:(HMService *)service {
    cn1homeRebuildSnapshot();
    cn1homeNotifyStructure(CN1_HOME_CHANGE_STRUCTURES, nil,
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
        cn1homePendingAuth = [[NSMutableArray alloc] init];
        cn1homeWatches = [[NSMutableDictionary alloc] init];
        cn1homeUndelivered = [[NSMutableDictionary alloc] init];
        cn1homeLastPolled = [[NSMutableDictionary alloc] init];
        cn1homeNotifyFailed = [[NSMutableSet alloc] init];
        cn1homeDependencies = [[NSMutableDictionary alloc] init];
        cn1homeAccessoryObjects = [[NSMutableDictionary alloc] init];
        cn1homeHomeObjects = [[NSMutableDictionary alloc] init];
        cn1homeDelegate = [[CN1HomeDelegate alloc] init];
        // HomeKit's delegate callbacks are a foreground stream: an app that
        // is suspended while a light is switched hears nothing about it and
        // wakes holding the old value. The subscription is push, so the app
        // is not polling either -- it would show that value until the
        // accessory happened to move again.
        //
        // Coming back to the foreground is therefore a resync, for every live
        // subscription: the flag says "what you are holding may be stale",
        // which is exactly what a gap in the stream means. Registered once,
        // for the life of the process, because the alternative is a
        // registration per subscription and the answer is the same for all of
        // them.
        [[NSNotificationCenter defaultCenter]
            addObserverForName:UIApplicationDidBecomeActiveNotification
                        object:nil
                         queue:[NSOperationQueue mainQueue]
                    usingBlock:^(NSNotification *note) {
            if (cn1homeManager == nil || !cn1homeHomesLoaded) {
                // Nothing has connected yet, so there is nothing to be stale
                // about and nobody to tell.
                return;
            }
            // The graph first, and unconditionally: an accessory added, a
            // room renamed or a scene created while the app slept is a change
            // no delegate reported either, and a structure listener is not
            // tied to a subscription -- an app with no subscriptions at all
            // still has a graph on screen.
            cn1homeRebuildSnapshot();
            cn1homeNotifyStructure(CN1_HOME_CHANGE_STRUCTURES, nil, nil);
            for (NSString *subscriptionId in [cn1homeWatches allKeys]) {
                com_codename1_impl_ios_IOSHomeCallbacks_resyncRequired___java_lang_String(
                    getThreadLocalData(),
                    fromNSString(getThreadLocalData(), subscriptionId));
            }
        }];
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

static void cn1homeArmRecovery(void);

/// Re-registers every live watch that names `accessoryId`.
///
/// A firmware update can replace an accessory's HMCharacteristic objects
/// while keeping their identifiers, and a registration belongs to the object,
/// not to the id. The watch key still matches, so nothing looks wrong: the
/// replacement is not in cn1homeNotifyFailed, and drainChanges() skips it
/// because it advertises SupportsEventNotification. That subscription then
/// delivers nothing at all, for as long as the screen is open.
///
/// So the registration is made again against whatever object is in the graph
/// now. A failure here lands on the recovery path like any other, and the
/// caller is told to resync either way -- the swap itself may have moved the
/// value, and no notification could have reported that.
static void cn1homeRebindWatches(NSString *accessoryId) {
    // The dependency registrations first, and quietly: they belong to no
    // trait the caller named, so a failure is not its business -- but a
    // replaced mode characteristic with no registration leaves a setpoint
    // watch deaf to the only signal that says the setpoint stopped meaning
    // anything.
    for (NSString *subscriptionId in [cn1homeDependencies allKeys]) {
        for (NSString *key in [cn1homeDependencies
                               objectForKey:subscriptionId]) {
            NSArray *parts = [key componentsSeparatedByString:@"\t"];
            if ([parts count] != 3
                    || ![[parts objectAtIndex:0] isEqualToString:accessoryId]) {
                continue;
            }
            HMService *service = cn1homeFindService(accessoryId,
                                                    [parts objectAtIndex:1]);
            HMCharacteristic *dep = cn1homeFindCharacteristic(
                service, [parts objectAtIndex:2]);
            if (dep == nil) {
                continue;
            }
            NSString *depStateKey = [NSString stringWithFormat:@"%@\t%@",
                                     subscriptionId, key];
            NSString *depBaseline = cn1homeReadingWithoutTimestamp(
                cn1homeEncodeReading(accessoryId, [parts objectAtIndex:1],
                                     [parts objectAtIndex:2], [dep value],
                                     nil, nil));
            if (![[dep properties] containsObject:
                  HMCharacteristicPropertySupportsEventNotification]) {
                // The replacement cannot notify. Same answer as a mode that
                // never could: the recovery pass polls it, because nothing
                // else will ever tell this subscription that AUTO began or
                // ended.
                [cn1homeNotifyFailed addObject:depStateKey];
                [cn1homeLastPolled setObject:depBaseline forKey:depStateKey];
                cn1homeArmRecovery();
                continue;
            }
            [dep enableNotification:YES completionHandler:^(NSError *error) {
                if (error == nil) {
                    [cn1homeNotifyFailed removeObject:depStateKey];
                    [cn1homeLastPolled removeObjectForKey:depStateKey];
                    return;
                }
                // And a failure here is not swallowed either. Discarded, the
                // setpoint watch this dependency exists for hears nothing
                // when the thermostat crosses into AUTO -- for good, since
                // nothing retries.
                [cn1homeNotifyFailed addObject:depStateKey];
                [cn1homeLastPolled setObject:depBaseline forKey:depStateKey];
                cn1homeArmRecovery();
            }];
        }
    }
    for (NSString *subscriptionId in [cn1homeWatches allKeys]) {
        NSSet *keys = [cn1homeWatches objectForKey:subscriptionId];
        BOOL touched = NO;
        for (NSString *key in keys) {
            NSArray *parts = [key componentsSeparatedByString:@"\t"];
            if ([parts count] != 3
                    || ![[parts objectAtIndex:0] isEqualToString:accessoryId]) {
                continue;
            }
            touched = YES;
            NSString *serviceId = [parts objectAtIndex:1];
            NSString *traitId = [parts objectAtIndex:2];
            HMService *service = cn1homeFindService(accessoryId, serviceId);
            HMCharacteristic *c = cn1homeFindCharacteristic(service, traitId);
            if (c == nil) {
                continue;
            }
            NSString *stateKey = [NSString stringWithFormat:@"%@\t%@",
                                  subscriptionId, key];
            if (![[c properties] containsObject:
                  HMCharacteristicPropertySupportsEventNotification]) {
                // Polled rather than pushed, now or still. Its baseline
                // belongs to the object that is gone, so it is taken again
                // here -- keeping the old one would report the swap itself
                // as a change on the next drain.
                [cn1homeNotifyFailed removeObject:stateKey];
                [cn1homeLastPolled setObject:cn1homeReadingWithoutTimestamp(
                     cn1homeEncodeReading(accessoryId, serviceId, traitId,
                                          cn1homeValueForReading(service,
                                                                 traitId, c),
                                          nil, nil))
                                      forKey:stateKey];
                continue;
            }
            [c enableNotification:YES completionHandler:^(NSError *error) {
                if (error == nil) {
                    [cn1homeNotifyFailed removeObject:stateKey];
                    [cn1homeLastPolled removeObjectForKey:stateKey];
                    return;
                }
                [cn1homeNotifyFailed addObject:stateKey];
                [cn1homeLastPolled setObject:cn1homeReadingWithoutTimestamp(
                     cn1homeEncodeReading(accessoryId, serviceId, traitId,
                                          cn1homeValueForReading(service,
                                                                 traitId, c),
                                          nil, nil))
                                      forKey:stateKey];
                cn1homeArmRecovery();
            }];
        }
        if (touched) {
            com_codename1_impl_ios_IOSHomeCallbacks_resyncRequired___java_lang_String(
                getThreadLocalData(),
                fromNSString(getThreadLocalData(), subscriptionId));
        }
    }
}

/// Whether anything still needs this characteristic's notifications.
///
/// #### Parameters
///
/// - `subscriptionId`: the subscription the state key belongs to
///
/// - `key`: "accessoryId \t serviceId \t traitId"
///
/// #### Returns
///
/// `true` while that subscription still watches it, or depends on it
static BOOL cn1homeStillNeeded(NSString *subscriptionId, NSString *key) {
    return [[cn1homeWatches objectForKey:subscriptionId] containsObject:key]
            || [[cn1homeDependencies objectForKey:subscriptionId]
                containsObject:key];
}

/// Tries the notification registration again, and forgets the polling state
/// once it takes.
///
/// Separated out so the caller can order it after a live read: the two
/// completions ran concurrently once, and clearing the baseline out from
/// under an in-flight read is how a change went missing.
///
/// The liveness check is not the caller's: a read can be in flight when the
/// screen closes, and unsubscribe turns the notification off on its way out.
/// Retrying afterwards would turn it back on for a characteristic nobody
/// watches, and nothing would ever turn it off again -- so an open-and-close
/// during a recovery pass leaves HomeKit traffic running for the life of the
/// process.
static void cn1homeRetryNotification(HMCharacteristic *c,
                                     NSString *subscriptionId, NSString *key,
                                     NSString *stateKey) {
    if (!cn1homeStillNeeded(subscriptionId, key)) {
        [cn1homeNotifyFailed removeObject:stateKey];
        [cn1homeLastPolled removeObjectForKey:stateKey];
        return;
    }
    [c enableNotification:YES completionHandler:^(NSError *error) {
        if (error != nil) {
            return;
        }
        [cn1homeNotifyFailed removeObject:stateKey];
        [cn1homeLastPolled removeObjectForKey:stateKey];
    }];
}

/// Polls and re-registers the characteristics whose notification failed.
///
/// enableNotification: failing is not rare -- a bridge that is busy, an
/// accessory that just came back on the network -- and the subscription that
/// hit it was already handed to the caller as push, because
/// SupportsEventNotification said the characteristic reports changes. So
/// there is nobody to fall back on: an app that trusts isPushDelivery() never
/// calls drainChanges(), and without this the listener would sit on the value
/// it had when registration failed for as long as the screen stays open, even
/// after the accessory recovered.
///
/// Both halves matter. The read is what keeps the subscription delivering
/// while notifications are off, and the retry is what ends the polling: once
/// HomeKit accepts the registration the entry is dropped and the ordinary
/// push path takes over again.
static void cn1homeRunRecovery(void) {
    cn1homeRecoveryArmed = NO;
    if ([cn1homeNotifyFailed count] == 0) {
        return;
    }
    for (NSString *stateKey in [cn1homeNotifyFailed allObjects]) {
        NSArray *parts = [stateKey componentsSeparatedByString:@"\t"];
        if ([parts count] != 4) {
            [cn1homeNotifyFailed removeObject:stateKey];
            continue;
        }
        NSString *subscriptionId = [parts objectAtIndex:0];
        NSString *accessoryId = [parts objectAtIndex:1];
        NSString *serviceId = [parts objectAtIndex:2];
        NSString *traitId = [parts objectAtIndex:3];
        NSString *key = cn1homeReadingKey(accessoryId, serviceId, traitId);
        NSSet *watched = [cn1homeWatches objectForKey:subscriptionId];
        NSSet *depends = [cn1homeDependencies objectForKey:subscriptionId];
        // A dependency is recovered like anything else, but it is not the
        // trait the caller asked about: what its movement means to them is
        // the setpoint's update, so that is what gets delivered below.
        BOOL isDependency = ![watched containsObject:key]
                && [depends containsObject:key];
        if (!cn1homeStillNeeded(subscriptionId, key)) {
            // The subscription went away. Nothing to recover, and leaving it
            // here would keep the timer alive for the life of the process.
            [cn1homeNotifyFailed removeObject:stateKey];
            [cn1homeLastPolled removeObjectForKey:stateKey];
            continue;
        }
        HMService *service = cn1homeFindService(accessoryId, serviceId);
        HMCharacteristic *c = cn1homeFindCharacteristic(service, traitId);
        if (c == nil) {
            // Out of the graph for the moment -- an accessory being
            // re-added, a home still loading. Kept, because the next pass is
            // exactly when it is worth looking again.
            continue;
        }
        if (![[c properties] containsObject:HMCharacteristicPropertyReadable]) {
            cn1homeRetryNotification(c, subscriptionId, key, stateKey);
            continue;
        }
        // The read first, and the retry only once it has answered. Issued
        // side by side they raced: a registration that succeeded before the
        // read came back dropped the baseline, the read then found none to
        // compare against, and the change that happened while notifications
        // were off was swallowed -- leaving the listener on a stale value
        // until the accessory happened to move again.
        [c readValueWithCompletionHandler:^(NSError *error) {
            if (error == nil) {
                NSString *record = cn1homeEncodeReading(accessoryId, serviceId,
                                                        traitId,
                                                        cn1homeValueForReading(
                                                            service, traitId,
                                                            c),
                                                        nil, nil);
                NSString *current = cn1homeReadingWithoutTimestamp(record);
                NSString *previous = [cn1homeLastPolled objectForKey:stateKey];
                [cn1homeLastPolled setObject:current forKey:stateKey];
                if (previous != nil && ![previous isEqualToString:current]) {
                    if (isDependency) {
                        // The mode moved. The subscription never asked about
                        // it -- what it asked about is the setpoint, which
                        // has just started or stopped meaning something.
                        HMCharacteristic *setpoint =
                                cn1homeFindCharacteristic(
                                    service, @"target_temperature");
                        if (setpoint != nil) {
                            com_codename1_impl_ios_IOSHomeCallbacks_changes___java_lang_String_java_lang_String(
                                getThreadLocalData(),
                                fromNSString(getThreadLocalData(),
                                             subscriptionId),
                                fromNSString(getThreadLocalData(),
                                    cn1homeEncodeReading(accessoryId,
                                        serviceId, @"target_temperature",
                                        cn1homeValueForReading(service,
                                            @"target_temperature", setpoint),
                                        nil, nil)));
                        }
                    } else {
                        com_codename1_impl_ios_IOSHomeCallbacks_changes___java_lang_String_java_lang_String(
                            getThreadLocalData(),
                            fromNSString(getThreadLocalData(), subscriptionId),
                            fromNSString(getThreadLocalData(), record));
                    }
                }
            }
            cn1homeRetryNotification(c, subscriptionId, key, stateKey);
        }];
    }
    cn1homeArmRecovery();
}

/// Schedules the next recovery pass, if one is wanted and none is scheduled.
///
/// dispatch_after rather than an NSTimer: the timer would have to be owned,
/// invalidated and released by hand under MRC on every path that empties the
/// set, and the armed flag is the whole of the state this needs.
static void cn1homeArmRecovery(void) {
    if (cn1homeRecoveryArmed || [cn1homeNotifyFailed count] == 0) {
        return;
    }
    cn1homeRecoveryArmed = YES;
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW,
                                 (int64_t) (CN1_HOME_RECOVERY_SECONDS
                                            * NSEC_PER_SEC)),
                   dispatch_get_main_queue(), ^{
        cn1homeRunRecovery();
    });
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
        [cn1homePendingAuth removeAllObjects];
        [cn1homeWatches removeAllObjects];
        [cn1homeUndelivered removeAllObjects];
        [cn1homeLastPolled removeAllObjects];
        [cn1homeNotifyFailed removeAllObjects];
        [cn1homeDependencies removeAllObjects];
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
        BOOL alreadyPrompting = [cn1homePendingAuth count] > 0;
        [cn1homePendingAuth addObject:[NSNumber numberWithInt:(int) rid]];
        if (alreadyPrompting) {
            // A second request while the first prompt is still on screen.
            // Queued behind it and answered with the user's decision when it
            // arrives: there is no second prompt to wait for, and answering
            // UNKNOWN here reported a failure at a moment when the user had
            // not yet been asked -- so an app that asks from startup and
            // again from its first screen showed one of them the wrong thing
            // moments before the grant.
            return;
        }
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
        // Setpoints waiting on the caller's own mode write, keyed
        // "accessoryId \t serviceId". A setpoint that only makes sense once
        // the thermostat leaves AUTO used to write the mode itself before
        // writing the setpoint -- which sent the accessory the same command
        // twice, and let the two answers disagree: the copy could succeed
        // and apply the mode while the caller's own write failed, so the
        // batch reported a mode change that had in fact happened as a
        // failure. The caller's write is the prerequisite now, and this is
        // what it releases.
        NSMutableDictionary *afterMode = [NSMutableDictionary dictionary];
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
            // The accessory first, so a stale id is reported as one. Rolled
            // into the trait answer it said "this accessory does not have
            // that capability" about an accessory that is not there at all,
            // and ACCESSORY_NOT_FOUND is the one that tells the caller their
            // snapshot is old and a refresh will fix it. The read path
            // already draws this distinction.
            if (cn1homeFindAccessory(accessoryId) == nil) {
                [records replaceObjectAtIndex:i withObject:cn1homeJoinFields(
                    [base arrayByAddingObjectsFromArray:
                     [NSArray arrayWithObjects:@"0", @"ACCESSORY_NOT_FOUND",
                      @"no such accessory in this home", nil]])];
                continue;
            }
            HMService *service = cn1homeFindService(accessoryId, serviceId);
            HMCharacteristic *c = cn1homeFindCharacteristic(service, traitId);
            if (c == nil) {
                [records replaceObjectAtIndex:i withObject:cn1homeJoinFields(
                    [base arrayByAddingObjectsFromArray:
                     [NSArray arrayWithObjects:@"0", @"TRAIT_NOT_SUPPORTED",
                      @"this service does not have that trait", nil]])];
                continue;
            }
            // The single setpoint means nothing in AUTO -- the thermostat
            // is working to the two thresholds -- so a read of it answers
            // absent and the local store refuses the write. HomeKit would
            // take it: it would change a characteristic this API cannot
            // report back, and tell the caller their target was applied.
            // Refused here too, so the simulator and the device agree.
            //
            // Unless the same batch is also leaving AUTO. Setting the mode
            // and its setpoint together is the ordinary way to ask for
            // "heat, to 21", and HomeKit applies the writes as a batch --
            // refusing on the mode the thermostat is about to leave would
            // make that impossible to express.
            // The index of the mode write this setpoint waits on, not just
            // "some mode write on this service": a batch may carry several,
            // and only the one that leaves AUTO justifies the setpoint.
            // Keyed by service, a successful AUTO write finishing first
            // released the setpoint anyway -- and which of them finished
            // first was down to the accessory, so the same batch behaved
            // differently from run to run.
            //
            // What the batch asks for is every mode write in it, not the
            // first one that happens not to be AUTO. Taking the first,
            // "HEAT, then AUTO, and a target" bound the setpoint to HEAT
            // and applied it, while the AUTO write left the thermostat in
            // AUTO holding a target nothing can read back -- exactly the
            // outcome this refusal exists to prevent. And a batch that
            // names two different modes for one thermostat has no answer
            // at all: HomeKit runs the writes independently, so which mode
            // the accessory ends in is whichever landed last. Nothing here
            // can honestly say the setpoint applied, so it is refused.
            NSUInteger waitsForEntry = NSNotFound;
            if ([traitId isEqualToString:@"target_temperature"]) {
                // Whether it is in AUTO NOW is only half of it. Judged on
                // that alone, a thermostat in HEAT given "AUTO, and 21"
                // passed both writes: the mode landed, and the target was
                // reported as applied to a thermostat that no longer has
                // one -- an immediate read answers absent. What the batch
                // LEAVES it in is the question, so the scan runs whatever
                // state it starts from.
                BOOL inAutoNow = cn1homeHasNoValueNow(service, traitId);
                NSUInteger lastMode = NSNotFound;
                BOOL modesDisagree = NO;
                int requestedMode = 0;
                for (NSUInteger j = 0; j < count; j++) {
                    if (j == i
                            || ![[traits objectAtIndex:j]
                                 isEqualToString:@"target_heating_cooling"]
                            || ![[accessories objectAtIndex:j]
                                 isEqualToString:accessoryId]
                            || ![[services objectAtIndex:j]
                                 isEqualToString:serviceId]
                            || j >= [numbers count]) {
                        continue;
                    }
                    int mode = (int) [[numbers objectAtIndex:j] doubleValue];
                    if (lastMode != NSNotFound && mode != requestedMode) {
                        modesDisagree = YES;
                    }
                    requestedMode = mode;
                    lastMode = j;
                }
                // A mode write to a service with no mode characteristic
                // cannot land -- its own record says TRAIT_NOT_SUPPORTED --
                // so it changes nothing and this setpoint is judged as if
                // the batch had not asked.
                if (cn1homeFindCharacteristic(
                        service, @"target_heating_cooling") == nil) {
                    lastMode = NSNotFound;
                }
                // The caller's own mode write becomes this write's
                // prerequisite. Launched independently, a mode write the
                // accessory refuses -- unreachable, read-only, out of
                // range -- left the thermostat in AUTO while the setpoint
                // landed and was reported as applied.
                if (lastMode != NSNotFound && !modesDisagree
                        && requestedMode != CN1_HOME_MODE_AUTO) {
                    waitsForEntry = lastMode;
                }
                if (waitsForEntry == NSNotFound
                        && (inAutoNow || lastMode != NSNotFound)) {
                    NSString *why = modesDisagree
                            ? @"this batch asks this thermostat for more than"
                              " one mode, so which one it ends in is undefined"
                              " and a single target cannot be applied with it"
                            : lastMode != NSNotFound
                            ? @"this batch puts this thermostat in AUTO, where"
                              " there is no single target; set the heating and"
                              " cooling thresholds instead"
                            : @"this thermostat is in AUTO, where there is no"
                              " single target; set the heating and cooling"
                              " thresholds instead";
                    [records replaceObjectAtIndex:i
                                       withObject:cn1homeJoinFields(
                        [base arrayByAddingObjectsFromArray:
                         [NSArray arrayWithObjects:@"0", @"INVALID_ARGUMENT",
                          why, nil]])];
                    outstanding--;
                    continue;
                }
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
            // Set for a mode write other entries are waiting on, and read in
            // this entry's completion: the setpoints queued against it go out
            // once it has actually landed.
            __block NSString *releaseAfterMode = nil;
            void (^writeParts)(void) = ^{
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
                                 [NSArray arrayWithObjects:@"1", @"", @"",
                                  nil]])];
                        }
                        if (releaseAfterMode != nil) {
                            NSArray *queued = [afterMode
                                               objectForKey:releaseAfterMode];
                            [afterMode removeObjectForKey:releaseAfterMode];
                            for (void (^waiting)(BOOL) in queued) {
                                waiting(!reported);
                            }
                        }
                        finished++;
                        if (finished == outstanding) {
                            answer();
                        }
                    }];
                }
            };
            if (waitsForEntry != NSNotFound) {
                NSString *serviceKey = [NSString stringWithFormat:@"%lu",
                                        (unsigned long) waitsForEntry];
                void (^waiting)(BOOL) = ^(BOOL modeApplied) {
                    if (modeApplied) {
                        writeParts();
                        return;
                    }
                    // The mode this target depends on did not land, so the
                    // thermostat is in whatever mode it was already in --
                    // reporting the target as applied would be the lie this
                    // whole branch exists to avoid. The mode's own record
                    // reports its failure on its own account.
                    [records replaceObjectAtIndex:index
                                       withObject:cn1homeJoinFields(
                        [base arrayByAddingObjectsFromArray:
                         [NSArray arrayWithObjects:@"0", @"INVALID_ARGUMENT",
                          @"the mode change this target depends on failed, so"
                          " the target was not applied", nil]])];
                    finished++;
                    if (finished == outstanding) {
                        answer();
                    }
                };
                NSMutableArray *queued = [afterMode objectForKey:serviceKey];
                if (queued == nil) {
                    queued = [NSMutableArray array];
                    [afterMode setObject:queued forKey:serviceKey];
                }
                [queued addObject:[[waiting copy] autorelease]];
                continue;
            }
            if ([traitId isEqualToString:@"target_heating_cooling"]) {
                // This write may be somebody's prerequisite -- the queue is
                // keyed by THIS entry, so a second mode write in the same
                // batch releases nothing that was not waiting on it. Its own
                // answer is reported by writeParts as usual.
                releaseAfterMode = [NSString stringWithFormat:@"%lu",
                                    (unsigned long) i];
            }
            writeParts();
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
        NSMutableSet *deps = [NSMutableSet set];
        for (NSUInteger i = 0; i < [traits count]; i++) {
            NSString *accessoryId = [accessories objectAtIndex:i];
            NSString *serviceId = [services objectAtIndex:i];
            NSString *traitId = [traits objectAtIndex:i];
            [keys addObject:cn1homeReadingKey(accessoryId, serviceId,
                                              traitId)];
            if ([traitId isEqualToString:@"target_temperature"]) {
                // The mode is what decides whether this setpoint means
                // anything at all -- see cn1homeDependencies. Registered
                // even though nobody asked for it, and kept out of the
                // watch set so its own changes are never delivered as if
                // they had been.
                [deps addObject:cn1homeReadingKey(accessoryId, serviceId,
                                                  @"target_heating_cooling")];
            }
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
                //
                // Its polling baseline is taken HERE, at registration, not on
                // the first drain. Established on the first poll instead, a
                // change between subscribing and that poll became the
                // baseline and was never delivered -- and unless the value
                // moved a second time the listener sat on the old one for
                // good. Recorded even when the characteristic has no cached
                // value yet, so the baseline exists either way and the first
                // real change is a change.
                NSString *baselineKey = [NSString stringWithFormat:@"%@\t%@",
                                         subId,
                                         cn1homeReadingKey(accessoryId,
                                                           serviceId,
                                                           traitId)];
                NSString *baseline = cn1homeReadingWithoutTimestamp(
                    cn1homeEncodeReading(accessoryId, serviceId, traitId,
                                         cn1homeValueForReading(service,
                                                                traitId, c),
                                         nil, nil));
                [cn1homeLastPolled setObject:baseline forKey:baselineKey];
                continue;
            }
            [c enableNotification:YES completionHandler:^(NSError *error) {
                if (error == nil) {
                    return;
                }
                // The subscription is still registered -- failing the whole
                // thing because one sensor refused would take the other
                // nineteen down with it -- but the caller has to be told
                // that what it is watching may now be stale.
                //
                // And told once is not enough. This handle was already
                // reported as push, so the app will never call
                // drainChanges(); a resync flag on its own would get the
                // caller a single re-read and then silence for the life of
                // the screen. The characteristic goes onto the recovery
                // path, which polls it and keeps retrying the registration
                // until HomeKit takes it.
                NSString *stateKey = [NSString stringWithFormat:@"%@\t%@",
                                      subId,
                                      cn1homeReadingKey(accessoryId,
                                                        serviceId, traitId)];
                [cn1homeNotifyFailed addObject:stateKey];
                [cn1homeLastPolled setObject:cn1homeReadingWithoutTimestamp(
                     cn1homeEncodeReading(accessoryId, serviceId, traitId,
                                          cn1homeValueForReading(service,
                                                                 traitId, c),
                                          nil, nil))
                                      forKey:stateKey];
                cn1homeArmRecovery();
                com_codename1_impl_ios_IOSHomeCallbacks_resyncRequired___java_lang_String(
                    getThreadLocalData(),
                    fromNSString(getThreadLocalData(), subId));
            }];
        }
        [cn1homeWatches setObject:keys forKey:subId];
        // The dependencies, registered after the watch set exists so a
        // failure inside the completion handler finds the subscription in
        // place. A failure here is not reported to the caller: the trait it
        // supports is still registered and still delivering, and the derived
        // update it exists for is the recovery pass's business.
        [deps minusSet:keys];
        for (NSString *key in deps) {
            NSArray *parts = [key componentsSeparatedByString:@"\t"];
            if ([parts count] != 3) {
                continue;
            }
            HMService *service = cn1homeFindService([parts objectAtIndex:0],
                                                    [parts objectAtIndex:1]);
            HMCharacteristic *c = cn1homeFindCharacteristic(
                service, [parts objectAtIndex:2]);
            if (c == nil) {
                continue;
            }
            NSString *depStateKey = [NSString stringWithFormat:@"%@\t%@",
                                     subId, key];
            NSString *depBaseline = cn1homeReadingWithoutTimestamp(
                cn1homeEncodeReading([parts objectAtIndex:0],
                                     [parts objectAtIndex:1],
                                     [parts objectAtIndex:2],
                                     [c value], nil, nil));
            if (![[c properties] containsObject:
                  HMCharacteristicPropertySupportsEventNotification]) {
                // A mode that cannot notify -- and the recovery pass is how
                // the setpoint keeps moving anyway, since nothing else will
                // ever tell this subscription that AUTO began or ended.
                [cn1homeNotifyFailed addObject:depStateKey];
                [cn1homeLastPolled setObject:depBaseline forKey:depStateKey];
                cn1homeArmRecovery();
                continue;
            }
            [c enableNotification:YES completionHandler:^(NSError *error) {
                if (error == nil) {
                    return;
                }
                // Same recovery path as a watched trait, and for the same
                // reason: with no registration on the mode, crossing into
                // AUTO produces no derived setpoint update at all, and the
                // subscription this belongs to is push -- nobody is going to
                // drain it.
                [cn1homeNotifyFailed addObject:depStateKey];
                [cn1homeLastPolled setObject:depBaseline forKey:depStateKey];
                cn1homeArmRecovery();
            }];
        }
        [cn1homeDependencies setObject:deps forKey:subId];
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
        NSSet *deps = [cn1homeDependencies objectForKey:subId];
        if (deps != nil) {
            keys = keys == nil ? deps
                               : [keys setByAddingObjectsFromSet:deps];
        }
        [cn1homeWatches removeObjectForKey:subId];
        [cn1homeDependencies removeObjectForKey:subId];
        [cn1homeUndelivered removeObjectForKey:subId];
        // And this subscription's polling baselines, which are keyed by
        // subscription id and would otherwise outlive it. Subscription ids
        // only ever go up and nothing stops the bridge, so a screen opened
        // and closed a few hundred times would hold every baseline string it
        // ever recorded for the life of the process.
        if (keys != nil) {
            NSString *prefix = [subId stringByAppendingString:@"\t"];
            for (NSString *polled in [cn1homeLastPolled allKeys]) {
                if ([polled hasPrefix:prefix]) {
                    [cn1homeLastPolled removeObjectForKey:polled];
                }
            }
            for (NSString *failed in [cn1homeNotifyFailed allObjects]) {
                if ([failed hasPrefix:prefix]) {
                    [cn1homeNotifyFailed removeObject:failed];
                }
            }
        }
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
                // Including as somebody else's dependency: turning the
                // thermostat mode's notification off would leave their
                // setpoint watch deaf to the one change that matters to it.
                if (!stillWatched) {
                    for (NSString *other in cn1homeDependencies) {
                        if ([[cn1homeDependencies objectForKey:other]
                             containsObject:key]) {
                            stillWatched = YES;
                            break;
                        }
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
        // Kept alongside the characteristic because the encoding needs it:
        // whether a setpoint has a value at all is a question about the
        // service's mode, not about the characteristic -- see
        // cn1homeValueForReading.
        NSMutableArray *pollServices = [NSMutableArray array];
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
                // A characteristic whose notification registration failed is
                // polled here too, not only by the recovery timer: an app
                // that drains anyway should not have to wait out the timer
                // for the one trait that lost its registration.
                BOOL degraded = [cn1homeNotifyFailed containsObject:
                                 [NSString stringWithFormat:@"%@\t%@",
                                  subscriptionId, key]];
                if (c == nil
                        || (!degraded && [[c properties] containsObject:
                            HMCharacteristicPropertySupportsEventNotification])
                        || ![[c properties] containsObject:
                            HMCharacteristicPropertyReadable]) {
                    continue;
                }
                [pollSubs addObject:subscriptionId];
                [pollKeys addObject:key];
                [pollChars addObject:c];
                [pollServices addObject:service];
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
            HMService *polledService = [pollServices objectAtIndex:i];
            NSString *subscriptionId = [pollSubs objectAtIndex:i];
            NSString *key = [pollKeys objectAtIndex:i];
            [c readValueWithCompletionHandler:^(NSError *error) {
                NSString *stateKey = [NSString stringWithFormat:@"%@\t%@",
                                      subscriptionId, key];
                if (error != nil) {
                    // Polling is this subscription's only update path, so a
                    // failed poll is not nothing: the listener is now sitting
                    // on a value that may have moved, with no way to know.
                    // The resync flag is exactly that statement.
                    com_codename1_impl_ios_IOSHomeCallbacks_resyncRequired___java_lang_String(
                        getThreadLocalData(),
                        fromNSString(getThreadLocalData(), subscriptionId));
                } else if (cn1homeStillNeeded(subscriptionId, key)) {
                    NSArray *parts =
                            [key componentsSeparatedByString:@"\t"];
                    NSString *record = cn1homeEncodeReading(
                        [parts objectAtIndex:0], [parts objectAtIndex:1],
                        [parts objectAtIndex:2],
                        cn1homeValueForReading(polledService,
                                               [parts objectAtIndex:2], c),
                        nil, nil);
                    // Compared without the timestamp, which is the last
                    // field and moves on every read: comparing whole records
                    // would report every poll as a change.
                    NSString *previous = [cn1homeLastPolled
                                          objectForKey:stateKey];
                    NSString *current = cn1homeReadingWithoutTimestamp(record);
                    // Checked again here, not only at the top: this read
                    // was in flight while the screen closed, unsubscribe
                    // deleted the baseline on its way out, and writing one
                    // back leaves an entry no watch and no recovery record
                    // will ever clean up -- one per screen, for the life of
                    // the process.
                    if (previous == nil) {
                        // No baseline, which normally cannot happen: it is
                        // taken when the subscription registers, precisely so
                        // a change between then and now is a change and not a
                        // baseline. This is the leftover case -- a
                        // characteristic that appeared after registration --
                        // where there is nothing to compare against and
                        // announcing the current value would be delivering
                        // the initial value the caller did not ask for.
                        [cn1homeLastPolled setObject:current forKey:stateKey];
                    } else if (![previous isEqualToString:current]) {
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
            // Actions that actually made it into the set. A scene whose only
            // action was a setpoint the thermostat's mode makes meaningless
            // is left with none at all, and HomeKit is perfectly happy to
            // keep an empty action set -- so the caller would be told their
            // scene exists and it would do nothing, for ever.
            __block NSUInteger addedActions = 0;
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
                if (failedActions == 0 && addedActions > 0) {
                    answer(nil);
                    return;
                }
                NSString *encoded = failedActions == 0
                        ? cn1homeJoinFields([NSArray arrayWithObjects:
                            @"INVALID_ARGUMENT",
                            @"every action in this scene sets a thermostat's"
                             " single target while that thermostat is in"
                             " AUTO, where it has none -- so the scene would"
                             " have done nothing at all", nil])
                        : [[firstFailure retain] autorelease];
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
            // Counted before the loop rather than inside it: the filter below
            // and the failures above both reduce what lands, and the answer
            // has to distinguish "nothing was asked for" from "everything
            // asked for was dropped".

            for (NSUInteger i = 0; i < [traits count]; i++) {
                HMService *service = cn1homeFindService(
                    [accessories objectAtIndex:i], [services objectAtIndex:i]);
                HMCharacteristic *c = cn1homeFindCharacteristic(
                    service, [traits objectAtIndex:i]);
                // A scene is one instant, not a sequence, so the setpoint is
                // judged against the mode the scene LEAVES the thermostat in
                // -- wherever in the list that mode action happens to be. In
                // AUTO the single setpoint means nothing, and writing it
                // anyway hides a number in a characteristic this API reports
                // as absent, ready to surface the next time the thermostat
                // leaves AUTO. The local store drops the same action.
                if ([[traits objectAtIndex:i]
                     isEqualToString:@"target_temperature"]
                        && cn1homeSceneEndsInAuto(accessories, services,
                                                  traits, numbers, i,
                                                  service)) {
                    remaining--;
                    if (remaining == 0) {
                        done();
                        return;
                    }
                    continue;
                }
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
                    } else {
                        addedActions++;
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
    //
    // Unless this build commissions onto a fabric of its own. Then a flow
    // that finished is one the extension's own commissioning step completed,
    // because that step throwing is what fails perform() -- so the accessory
    // IS this app's, even though MatterSupport still declines to name it.
#ifdef CN1_MATTER_OWN_FABRIC
    JAVA_INT mine = error == nil ? 1 : 0;
#else
    JAVA_INT mine = accessoryId == nil ? 0 : 1;
#endif
    com_codename1_impl_ios_IOSHomeCallbacks_commissioningResult___int_java_lang_String_java_lang_String_java_lang_String_int_java_lang_String(
        getThreadLocalData(), rid,
        accessoryId == nil ? JAVA_NULL
                           : fromNSString(getThreadLocalData(), accessoryId),
        accessoryName == nil ? JAVA_NULL
                             : fromNSString(getThreadLocalData(),
                                            accessoryName),
        structureId == nil ? JAVA_NULL
                           : fromNSString(getThreadLocalData(), structureId),
        mine,
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

#if defined(CN1_INCLUDE_MATTER_SETUP) && defined(CN1_MATTER_OWN_FABRIC)
/// Whether this OS can load the generated setup extension.
///
/// An own-fabric build's extension is compiled for iOS 16.4: the Matter
/// framework's controller factory does not exist before it. The APP still
/// runs from 16.1, because raising its floor would cost every user on 16.1
/// through 16.3 the whole application over a commissioning feature -- but
/// those three releases cannot load the extension, so the sheet would open on
/// nothing. Reported as unavailable instead, which is what an app checks
/// before offering the button.
static BOOL cn1homeSetupExtensionLoadable(void) {
    if (@available(iOS 16.4, *)) {
        return YES;
    }
    return NO;
}
#endif

JAVA_INT
com_codename1_impl_ios_IOSNative_homeCommissioningStyle___R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef CN1_INCLUDE_MATTER_SETUP
#ifdef CN1_MATTER_OWN_FABRIC
    if (!cn1homeSetupExtensionLoadable()) {
        // The same answer a build with no extension gives: the Apple Home app
        // can still add the accessory, and this OS cannot run our own flow.
        return CN1_HOME_COMMISSION_APP_HANDOFF;
    }
#endif
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
#ifdef CN1_MATTER_OWN_FABRIC
    if (!cn1homeSetupExtensionLoadable()) {
        // Refused here as well as reported by homeCommissioningStyle: an app
        // that asks anyway gets a failure it can show, rather than a sheet
        // that opens and dies on an extension this OS cannot load.
        com_codename1_impl_ios_IOSHomeCallbacks_commissioningResult___int_java_lang_String_java_lang_String_java_lang_String_int_java_lang_String(
            CN1_THREAD_STATE_PASS_ARG requestId, JAVA_NULL, JAVA_NULL,
            JAVA_NULL, 0,
            fromNSString(CN1_THREAD_STATE_PASS_ARG
                         @"COMMISSIONING_UNAVAILABLE\tthis build commissions "
                         "onto its own fabric, which needs iOS 16.4; add the "
                         "accessory in the Home app instead"));
        POOL_END();
        return;
    }
#endif
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
