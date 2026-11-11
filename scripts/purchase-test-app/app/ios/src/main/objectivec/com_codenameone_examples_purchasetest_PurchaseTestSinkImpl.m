/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
#import "com_codenameone_examples_purchasetest_PurchaseTestSinkImpl.h"

// Persist submitted receipt transactionIds in NSUserDefaults so the hosted
// XCTest (PurchaseStoreKitTests) can read them back in-process after driving a
// purchase through SKTestSession. Key is shared with the test.
static NSString * const CN1IAPTestSubmittedKey = @"CN1IAPTestSubmittedReceipts";

// Outcome of the commerce check that runs after the purchase synchronized.
static NSString * const CN1IAPTestCommerceKey = @"CN1IAPTestCommerceResult";

@implementation com_codenameone_examples_purchasetest_PurchaseTestSinkImpl

-(void)recordSubmittedReceipt:(NSString*)transactionId {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSArray *existing = [defaults arrayForKey:CN1IAPTestSubmittedKey];
    NSMutableArray *updated = existing ? [existing mutableCopy] : [NSMutableArray array];
    [updated addObject:(transactionId != nil ? transactionId : @"<null>")];
    [defaults setObject:updated forKey:CN1IAPTestSubmittedKey];
    [defaults synchronize];
}

-(NSString*)recordedSubmissions {
    NSArray *existing = [[NSUserDefaults standardUserDefaults] arrayForKey:CN1IAPTestSubmittedKey];
    return existing ? [existing componentsJoinedByString:@","] : @"";
}

-(void)recordCommerceResult:(NSString*)result {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:(result != nil ? result : @"<null>") forKey:CN1IAPTestCommerceKey];
    [defaults synchronize];
}

-(void)reset {
    [[NSUserDefaults standardUserDefaults] removeObjectForKey:CN1IAPTestCommerceKey];
    [[NSUserDefaults standardUserDefaults] removeObjectForKey:CN1IAPTestSubmittedKey];
    [[NSUserDefaults standardUserDefaults] synchronize];
}

-(BOOL)isSupported {
    return YES;
}

@end
