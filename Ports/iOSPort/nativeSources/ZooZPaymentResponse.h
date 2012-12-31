//
//  ZooZResponse.h
// 
//
//  Created by Ronen Morecki on 6/22/11.
//  Copyright 2011 ZooZ.comAll rights reserved.
//

#import <Foundation/Foundation.h>


@interface ZooZPaymentResponse : NSObject 

@property(nonatomic, retain) NSString *fundSourceType;
@property(nonatomic, retain) NSString *lastFourDigits;
//The transaction ID in display form, can be used to to search and filter in the zooz portal in the transaction reports
@property(nonatomic, retain) NSString *transactionDisplayID;
//The transaction ID as a token, to be used in Server extended APIs, and for tracking in your system.
@property(nonatomic, retain) NSString *transactionID;
@property(nonatomic) float paidAmount;
@end
