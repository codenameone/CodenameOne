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
#import <Foundation/Foundation.h>

/// The Swift-to-Java seam for app intents.
///
/// Swift cannot call the translated Java directly, and the failure is silent rather than loud:
/// the ParparVM dead-code eliminator decides a Java method is reachable by scanning `.m` sources
/// for its mangled symbol, so a method named only from Swift is stripped to an empty stub and the
/// dispatch quietly does nothing. Routing through this Objective-C class is what keeps the Java
/// callbacks alive, which is why the file extension here is load-bearing rather than incidental.
///
/// It is also where the threading contract is enforced. The system invokes an App Intent's
/// `perform()` on its own concurrency pool, and blocking that thread while Java runs would both
/// stall the caller and hold a VM thread slot open. So every entry point here returns
/// immediately and the answer arrives later through `completeToken:resultJson:`.
@interface CN1IntentHost : NSObject

/// Starts an intent and stores `completion` against a fresh token. Returns at once; the
/// completion fires later, exactly once, from `completeToken:resultJson:`.
+ (void)performIntent:(NSString *)intentId
           paramsJson:(NSString *)paramsJson
             headless:(BOOL)headless
           completion:(void (^)(NSString *resultJson))completion;

/// Delivers the result for a token and forgets it. A second call for the same token is ignored,
/// because the Swift continuation waiting on the other side crashes the process if resumed twice.
+ (void)completeToken:(NSString *)token resultJson:(NSString *)resultJson;

/// Runs one of an entity type's queries and returns the serialized entities, or nil.
/// Synchronous: the platform calls this while building a picker and expects an answer.
+ (NSString *)queryEntities:(NSString *)entityType
                       kind:(NSString *)kind
                   argument:(NSString *)argument;

@end
