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
#import "CN1Health.h"

#ifdef CN1_INCLUDE_HEALTH

#import <HealthKit/HealthKit.h>
#include "com_codename1_impl_ios_IOSHealth.h"

// ---------------------------------------------------------------------
// Shared state
//
// One store for the app's lifetime: HKObserverQuery and background
// delivery are bound to the store instance and die with it, so a
// short-lived store would silently stop delivering.
// ---------------------------------------------------------------------

static HKHealthStore *cn1hkStore = nil;
static dispatch_queue_t cn1hkQueue = nil;
static void *cn1hkQueueKey = &cn1hkQueueKey;

static void cn1hkInit(void) {
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        cn1hkStore = [[HKHealthStore alloc] init];
        cn1hkQueue = dispatch_queue_create("com.codename1.health",
                                           DISPATCH_QUEUE_SERIAL);
        dispatch_queue_set_specific(cn1hkQueue, cn1hkQueueKey,
                                    cn1hkQueueKey, NULL);
    });
}

// Runs a block on the health queue. Executes inline when already on it,
// so a Java callback that re-enters the bridge cannot deadlock -- the same
// pattern CN1Bluetooth.m uses.
static void cn1hkAsync(void (^block)(void)) {
    cn1hkInit();
    if (dispatch_get_specific(cn1hkQueueKey) == cn1hkQueueKey) {
        block();
    } else {
        dispatch_async(cn1hkQueue, block);
    }
}

// ---------------------------------------------------------------------
// Type mapping
//
// Portable data-type ids to HealthKit identifiers. Returns nil for a type
// this iOS version does not know, which the callers report as
// NOT_SUPPORTED rather than crashing.
// ---------------------------------------------------------------------

static NSDictionary *cn1hkTypeMap(void) {
    static NSDictionary *map = nil;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        map = [@{
            @"steps": HKQuantityTypeIdentifierStepCount,
            @"distance_walking_running":
                HKQuantityTypeIdentifierDistanceWalkingRunning,
            @"distance_cycling": HKQuantityTypeIdentifierDistanceCycling,
            @"flights_climbed": HKQuantityTypeIdentifierFlightsClimbed,
            @"active_energy": HKQuantityTypeIdentifierActiveEnergyBurned,
            @"basal_energy": HKQuantityTypeIdentifierBasalEnergyBurned,
            @"heart_rate": HKQuantityTypeIdentifierHeartRate,
            @"resting_heart_rate": HKQuantityTypeIdentifierRestingHeartRate,
            @"heart_rate_variability_sdnn":
                HKQuantityTypeIdentifierHeartRateVariabilitySDNN,
            @"oxygen_saturation": HKQuantityTypeIdentifierOxygenSaturation,
            @"respiratory_rate": HKQuantityTypeIdentifierRespiratoryRate,
            @"body_temperature": HKQuantityTypeIdentifierBodyTemperature,
            @"vo2_max": HKQuantityTypeIdentifierVO2Max,
            @"blood_glucose": HKQuantityTypeIdentifierBloodGlucose,
            @"body_mass": HKQuantityTypeIdentifierBodyMass,
            @"lean_body_mass": HKQuantityTypeIdentifierLeanBodyMass,
            @"body_fat_percentage":
                HKQuantityTypeIdentifierBodyFatPercentage,
            @"body_mass_index": HKQuantityTypeIdentifierBodyMassIndex,
            @"height": HKQuantityTypeIdentifierHeight,
            @"hydration": HKQuantityTypeIdentifierDietaryWater,
            @"dietary_energy": HKQuantityTypeIdentifierDietaryEnergyConsumed
        } retain];
    });
    return map;
}

static HKQuantityType *cn1hkQuantityType(NSString *portableId) {
    NSString *identifier = [cn1hkTypeMap() objectForKey:portableId];
    if (identifier == nil) {
        return nil;
    }
    return [HKQuantityType quantityTypeForIdentifier:identifier];
}

// The unit each portable type is normalised to. These are the canonical
// units declared by HealthDataType, and HKUnit parses the same symbols the
// Java side uses -- which is why the symbol is public API there.
static HKUnit *cn1hkUnit(NSString *portableId) {
    if ([portableId isEqualToString:@"steps"]
        || [portableId isEqualToString:@"flights_climbed"]) {
        return [HKUnit countUnit];
    }
    if ([portableId isEqualToString:@"heart_rate"]
        || [portableId isEqualToString:@"resting_heart_rate"]
        || [portableId isEqualToString:@"respiratory_rate"]) {
        return [[HKUnit countUnit] unitDividedByUnit:[HKUnit minuteUnit]];
    }
    if ([portableId hasPrefix:@"distance"]
        || [portableId isEqualToString:@"height"]) {
        return [HKUnit meterUnit];
    }
    if ([portableId isEqualToString:@"body_mass"]
        || [portableId isEqualToString:@"lean_body_mass"]) {
        return [HKUnit gramUnitWithMetricPrefix:HKMetricPrefixKilo];
    }
    if ([portableId hasSuffix:@"energy"]) {
        return [HKUnit kilocalorieUnit];
    }
    if ([portableId isEqualToString:@"body_temperature"]) {
        return [HKUnit degreeCelsiusUnit];
    }
    if ([portableId isEqualToString:@"heart_rate_variability_sdnn"]) {
        return [HKUnit secondUnitWithMetricPrefix:HKMetricPrefixMilli];
    }
    if ([portableId isEqualToString:@"oxygen_saturation"]
        || [portableId isEqualToString:@"body_fat_percentage"]) {
        return [HKUnit percentUnit];
    }
    if ([portableId isEqualToString:@"blood_glucose"]) {
        return [[HKUnit moleUnitWithMetricPrefix:HKMetricPrefixMilli
                                molarMass:HKUnitMolarMassBloodGlucose]
                unitDividedByUnit:[HKUnit literUnit]];
    }
    if ([portableId isEqualToString:@"hydration"]) {
        return [HKUnit literUnit];
    }
    if ([portableId isEqualToString:@"vo2_max"]) {
        return [[[HKUnit literUnitWithMetricPrefix:HKMetricPrefixMilli]
                 unitDividedByUnit:[HKUnit gramUnitWithMetricPrefix:
                                    HKMetricPrefixKilo]]
                unitDividedByUnit:[HKUnit minuteUnit]];
    }
    if ([portableId isEqualToString:@"body_mass_index"]) {
        return [HKUnit countUnit];
    }
    return [HKUnit countUnit];
}

// The symbol string the Java side expects back, matching HealthUnit.
static NSString *cn1hkUnitSymbol(NSString *portableId) {
    if ([portableId isEqualToString:@"heart_rate"]
        || [portableId isEqualToString:@"resting_heart_rate"]
        || [portableId isEqualToString:@"respiratory_rate"]) {
        return @"count/min";
    }
    if ([portableId hasPrefix:@"distance"]
        || [portableId isEqualToString:@"height"]) {
        return @"m";
    }
    if ([portableId isEqualToString:@"body_mass"]
        || [portableId isEqualToString:@"lean_body_mass"]) {
        return @"kg";
    }
    if ([portableId hasSuffix:@"energy"]) {
        return @"kcal";
    }
    if ([portableId isEqualToString:@"body_temperature"]) {
        return @"degC";
    }
    if ([portableId isEqualToString:@"heart_rate_variability_sdnn"]) {
        return @"ms";
    }
    if ([portableId isEqualToString:@"oxygen_saturation"]
        || [portableId isEqualToString:@"body_fat_percentage"]) {
        return @"%";
    }
    if ([portableId isEqualToString:@"blood_glucose"]) {
        return @"mmol/L";
    }
    if ([portableId isEqualToString:@"hydration"]) {
        return @"L";
    }
    if ([portableId isEqualToString:@"vo2_max"]) {
        return @"mL/(kg*min)";
    }
    return @"count";
}

static void cn1hkReportError(JAVA_INT requestId, int code, NSString *msg) {
    com_codename1_impl_ios_IOSHealth_nativeHkRequestError___int_int_java_lang_String(
        getThreadLocalData(), requestId, code,
        fromNSString(getThreadLocalData(), msg));
}

// The portable code for a HealthKit error.
//
// The distinction that matters most here is retryable versus not. The
// store is encrypted at rest and unreadable before the device's first
// unlock -- exactly when a background drain runs -- and reporting that as
// a denial sends the user to the permission screen for a condition that
// will clear on its own, or makes an app discard buffered sensor and
// workout data it should simply have written again a moment later.
//
// Anything unrecognised stays UNKNOWN rather than being guessed at: the
// shared layer treats that as a plain failure, which is the honest answer
// for an error this build has never seen.
static int cn1hkErrorCode(NSError *error) {
    if (error == nil) {
        return CN1_HK_ERR_UNKNOWN;
    }
    switch ([error code]) {
        case HKErrorDatabaseInaccessible:
            return CN1_HK_ERR_DATABASE_INACCESSIBLE;
        case HKErrorAuthorizationDenied:
        case HKErrorAuthorizationNotDetermined:
            return CN1_HK_ERR_AUTH_DENIED;
        default:
            return CN1_HK_ERR_UNKNOWN;
    }
}

// ---------------------------------------------------------------------
// Natives
// ---------------------------------------------------------------------

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_hkIsAvailable___R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return [HKHealthStore isHealthDataAvailable] ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN
com_codename1_impl_ios_IOSNative_hkIsTypeSupported___java_lang_String_R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT typeId) {
    if (typeId == JAVA_NULL) {
        return JAVA_FALSE;
    }
    // The map is the authority. Deriving support from the portable type's
    // canonical unit instead would advertise types HealthKit has never
    // heard of, and the failure would surface only at query time.
    NSString *portableId = toNSString(threadStateData, typeId);
    return [cn1hkTypeMap() objectForKey:portableId] != nil
            ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_INT
com_codename1_impl_ios_IOSNative_hkShareAuthorizationStatus___java_lang_String_R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT typeId) {
    cn1hkInit();
    NSString *portableId = toNSString(threadStateData, typeId);
    HKQuantityType *type = cn1hkQuantityType(portableId);
    if (type == nil) {
        return 0;
    }
    // Meaningful for share (write) types only. HealthKit deliberately
    // reports nothing useful for read types, which is why there is no
    // corresponding read query -- adding one would require lying.
    return (JAVA_INT)[cn1hkStore authorizationStatusForType:type];
}

void
com_codename1_impl_ios_IOSNative_hkRequestAuthorization___int_java_lang_String_1ARRAY_java_lang_String_1ARRAY(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT readTypes, JAVA_OBJECT shareTypes) {
    cn1hkInit();
    // Convert while the Java frame is still live: once it returns, the
    // JAVA_OBJECTs are no longer rooted and must not be touched from the
    // dispatched block.
    NSMutableSet *readSet = [NSMutableSet set];
    NSMutableSet *shareSet = [NSMutableSet set];
    if (readTypes != JAVA_NULL) {
        JAVA_ARRAY arr = (JAVA_ARRAY)readTypes;
        JAVA_ARRAY_OBJECT *data = (JAVA_ARRAY_OBJECT *)arr->data;
        for (int i = 0; i < arr->length; i++) {
            HKQuantityType *t = cn1hkQuantityType(
                toNSString(threadStateData, (JAVA_OBJECT)data[i]));
            if (t != nil) {
                [readSet addObject:t];
            }
        }
    }
    if (shareTypes != JAVA_NULL) {
        JAVA_ARRAY arr = (JAVA_ARRAY)shareTypes;
        JAVA_ARRAY_OBJECT *data = (JAVA_ARRAY_OBJECT *)arr->data;
        for (int i = 0; i < arr->length; i++) {
            HKQuantityType *t = cn1hkQuantityType(
                toNSString(threadStateData, (JAVA_OBJECT)data[i]));
            if (t != nil) {
                [shareSet addObject:t];
            }
        }
    }
    JAVA_INT rid = requestId;
    cn1hkAsync(^{
        [cn1hkStore requestAuthorizationToShareTypes:shareSet
                                           readTypes:readSet
            completion:^(BOOL success, NSError *error) {
                    // success means the sheet completed, NOT that anything was
                // granted -- the portable contract says the same.
                com_codename1_impl_ios_IOSHealth_nativeHkAuthorizationResult___int_boolean_int_java_lang_String(
                    getThreadLocalData(), rid,
                    success ? JAVA_TRUE : JAVA_FALSE,
                    error == nil ? -1 : cn1hkErrorCode(error),
                    error == nil ? JAVA_NULL
                        : fromNSString(getThreadLocalData(),
                                       [error localizedDescription]));
            }];
    });
}

/// Runs the sample query and reports the page. Takes ownership of
/// `portableId`, releasing it once the results handler has run.
static void cn1hkRunSampleQuery(JAVA_INT rid, NSString *portableId,
        HKQuantityType *type, NSPredicate *pred, int lim, BOOL asc) {
    NSSortDescriptor *sort = [NSSortDescriptor
        sortDescriptorWithKey:HKSampleSortIdentifierStartDate
                    ascending:asc];
    HKSampleQuery *q = [[HKSampleQuery alloc]
        initWithSampleType:type predicate:pred limit:lim
           sortDescriptors:@[sort]
            resultsHandler:^(HKSampleQuery *query, NSArray *results,
                             NSError *error) {
        if (error != nil) {
            // A locked device makes the store unreadable, which is
            // exactly when a background observer fires. It must stay
            // distinguishable from "no data" so callers can retry.
            cn1hkReportError(rid, cn1hkErrorCode(error),
                [error localizedDescription]);
            [portableId release];
            return;
        }
        HKUnit *unit = cn1hkUnit(portableId);
        NSString *symbol = cn1hkUnitSymbol(portableId);
        NSMutableString *tsv = [NSMutableString string];
        for (HKQuantitySample *sample in results) {
            double value = [[sample quantity] doubleValueForUnit:unit];
            // No percent rescaling. doubleValueForUnit:percentUnit
            // already answers in percent -- 97% comes back as 97 -- so
            // multiplying turned every oxygen saturation and body fat
            // reading into 9700, and the matching division on the
            // write path stored 97% as 0.97%.
            HKSource *src = [[sample sourceRevision] source];
            // Only the user-entered flag is reported. HealthKit does
            // not distinguish an automatic reading from an actively
            // recorded one, and answering AUTOMATIC for everything
            // else would be a guess dressed as a fact -- an empty
            // field decodes to UNKNOWN, which is what we know.
            NSNumber *entered =
                [[sample metadata] objectForKey:HKMetadataKeyWasUserEntered];
            [tsv appendFormat:@"%@\t%@\t%lld\t%lld\t%f\t%@\t%@\t%@\t%@\t%@\n",
                [[sample UUID] UUIDString], portableId,
                (long long)([[sample startDate] timeIntervalSince1970]
                            * 1000.0),
                (long long)([[sample endDate] timeIntervalSince1970]
                            * 1000.0),
                value, symbol,
                src ? [src bundleIdentifier] : @"",
                src && [src name] ? [src name] : @"",
                [sample device] && [[sample device] name]
                    ? [[sample device] name] : @"",
                (entered && [entered boolValue]) ? @"MANUAL_ENTRY" : @""];
        }
        com_codename1_impl_ios_IOSHealth_nativeHkSamples___int_java_lang_String(
            getThreadLocalData(), rid,
            fromNSString(getThreadLocalData(), tsv));
        [portableId release];
    }];
    [cn1hkStore executeQuery:q];
    [q release];
}

void
com_codename1_impl_ios_IOSNative_hkQuerySamples___int_java_lang_String_double_double_int_boolean_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT typeId, JAVA_DOUBLE startMs, JAVA_DOUBLE endMs,
        JAVA_INT limit, JAVA_BOOLEAN ascending, JAVA_OBJECT sourceIds) {
    cn1hkInit();
    NSString *portableId = [toNSString(threadStateData, typeId) retain];
    NSString *wantedSources = sourceIds == JAVA_NULL ? nil
        : [toNSString(threadStateData, sourceIds) retain];
    JAVA_INT rid = requestId;
    double s = startMs, e = endMs;
    int lim = limit;
    BOOL asc = ascending == JAVA_TRUE;

    cn1hkAsync(^{
        HKQuantityType *type = cn1hkQuantityType(portableId);
        if (type == nil) {
            cn1hkReportError(rid, CN1_HK_ERR_NOT_SUPPORTED,
                             @"type unavailable on this iOS version");
            [portableId release];
            [wantedSources release];
            return;
        }
        NSDate *from = [NSDate dateWithTimeIntervalSince1970:s / 1000.0];
        NSDate *to = [NSDate dateWithTimeIntervalSince1970:e / 1000.0];
        NSPredicate *pred = [HKQuery predicateForSamplesWithStartDate:from
                                                              endDate:to
                                                              options:
            // Overlap, not strict-start. The portable contract -- and
            // LocalHealthStore.matches() -- includes an interval that
            // straddles a boundary, so steps recorded 11:55-12:05 belong to
            // a query starting at noon. HKQueryOptionStrictStartDate would
            // drop them and make the same SampleQuery mean different things
            // on iOS than everywhere else.
            HKQueryOptionNone];
        if (wantedSources == nil || [wantedSources length] == 0) {
            [wantedSources release];
            cn1hkRunSampleQuery(rid, portableId, type, pred, lim, asc);
            return;
        }
        // Source filtering has to be part of the query. HealthKit applies
        // the limit itself, so filtering the returned page afterwards --
        // which is what the shared post-filter does -- discards records
        // that were already counted against the limit, and with no
        // continuation token the ones behind them are unreachable. A read
        // whose first page happened to be another app's data came back
        // empty even though matching samples existed.
        //
        // Sources are named by bundle identifier, and only HKSourceQuery
        // can turn one into the HKSource that a predicate needs.
        NSSet *wanted = [NSSet setWithArray:
            [wantedSources componentsSeparatedByString:@"\t"]];
        HKSourceQuery *sq = [[HKSourceQuery alloc]
            initWithSampleType:type samplePredicate:pred
             completionHandler:^(HKSourceQuery *q, NSSet *sources,
                                 NSError *error) {
            if (error != nil) {
                cn1hkReportError(rid, cn1hkErrorCode(error),
                    [error localizedDescription]);
                [portableId release];
                [wantedSources release];
                return;
            }
            NSMutableSet *matched = [NSMutableSet set];
            for (HKSource *src in sources) {
                if ([wanted containsObject:[src bundleIdentifier]]) {
                    [matched addObject:src];
                }
            }
            [wantedSources release];
            if ([matched count] == 0) {
                // No source in this store matches, so the answer is an
                // empty page rather than an unfiltered one.
                com_codename1_impl_ios_IOSHealth_nativeHkSamples___int_java_lang_String(
                    getThreadLocalData(), rid,
                    fromNSString(getThreadLocalData(), @""));
                [portableId release];
                return;
            }
            NSPredicate *both = [NSCompoundPredicate
                andPredicateWithSubpredicates:@[pred,
                    [HKQuery predicateForObjectsFromSources:matched]]];
            cn1hkRunSampleQuery(rid, portableId, type, both, lim, asc);
        }];
        [cn1hkStore executeQuery:sq];
        [sq release];
    });
}

void com_codename1_impl_ios_IOSNative_hkSaveSamples___int_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT tsv) {
    cn1hkInit();
    NSString *payload = [toNSString(threadStateData, tsv) retain];
    JAVA_INT rid = requestId;
    cn1hkAsync(^{
        NSMutableArray *samples = [NSMutableArray array];
        NSMutableString *ids = [NSMutableString string];
        for (NSString *line in [payload componentsSeparatedByString:@"\n"]) {
            if ([line length] == 0) {
                continue;
            }
            NSArray *f = [line componentsSeparatedByString:@"\t"];
            if ([f count] < 6) {
                continue;
            }
            NSString *portableId = [f objectAtIndex:1];
            HKQuantityType *type = cn1hkQuantityType(portableId);
            if (type == nil) {
                continue;
            }
            double value = [[f objectAtIndex:4] doubleValue];
            // Percent values pass through unchanged; see the read path.
            HKQuantity *quantity = [HKQuantity
                quantityWithUnit:cn1hkUnit(portableId) doubleValue:value];
            NSDate *from = [NSDate dateWithTimeIntervalSince1970:
                [[f objectAtIndex:2] doubleValue] / 1000.0];
            NSDate *to = [NSDate dateWithTimeIntervalSince1970:
                [[f objectAtIndex:3] doubleValue] / 1000.0];
            // Field 6 is the recording method. HealthKit has no general
            // notion of one, but it does have the single distinction that
            // matters to every other app reading the sample: whether a
            // person typed it in. Dropping it made a hand-entered weight
            // indistinguishable from a scale reading, and a later read of
            // our own sample reported UNKNOWN.
            NSDictionary *metadata = nil;
            if ([f count] > 6
                    && [[f objectAtIndex:6] isEqualToString:@"MANUAL_ENTRY"]) {
                metadata = @{HKMetadataKeyWasUserEntered: @YES};
            }
            HKQuantitySample *sample = [HKQuantitySample
                quantitySampleWithType:type quantity:quantity
                             startDate:from endDate:to
                              metadata:metadata];
            [samples addObject:sample];
            [ids appendFormat:@"%@\n", [[sample UUID] UUIDString]];
        }
        [cn1hkStore saveObjects:samples withCompletion:
            ^(BOOL success, NSError *error) {
            if (!success) {
                // Every failure used to be reported as a denial, so a
                // save attempted before first unlock sent the app to the
                // permission screen -- or made it discard buffered sensor
                // and workout data -- for a condition that clears itself.
                cn1hkReportError(rid, cn1hkErrorCode(error),
                    error ? [error localizedDescription] : @"save failed");
            } else {
                com_codename1_impl_ios_IOSHealth_nativeHkSaveResult___int_java_lang_String(
                    getThreadLocalData(), rid,
                    fromNSString(getThreadLocalData(), ids));
            }
            [payload release];
        }];
    });
}

#else

// ---------------------------------------------------------------------
// Health compiled out.
//
// ParparVM links every native declared in IOSNative.java, so each one
// needs a symbol even when the feature is absent. These trampolines let a
// health-free app -- and the tvOS and Mac Catalyst slices, where HealthKit
// does not exist -- link cleanly. The Java side never calls them, because
// Health.getInstance() reports the feature unsupported first.
// ---------------------------------------------------------------------

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_hkIsAvailable___R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_FALSE;
}

JAVA_BOOLEAN
com_codename1_impl_ios_IOSNative_hkIsTypeSupported___java_lang_String_R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT typeId) {
    return JAVA_FALSE;
}


JAVA_INT
com_codename1_impl_ios_IOSNative_hkShareAuthorizationStatus___java_lang_String_R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT typeId) {
    return 0;
}

void
com_codename1_impl_ios_IOSNative_hkRequestAuthorization___int_java_lang_String_1ARRAY_java_lang_String_1ARRAY(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT readTypes, JAVA_OBJECT shareTypes) {
}

void
com_codename1_impl_ios_IOSNative_hkQuerySamples___int_java_lang_String_double_double_int_boolean_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT typeId, JAVA_DOUBLE startMs, JAVA_DOUBLE endMs,
        JAVA_INT limit, JAVA_BOOLEAN ascending, JAVA_OBJECT sourceIds) {
}

void com_codename1_impl_ios_IOSNative_hkSaveSamples___int_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT tsv) {
}

#endif
