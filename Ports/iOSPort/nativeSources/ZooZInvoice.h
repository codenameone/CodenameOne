//
//  ZooZInvoice.h
//  ZooZSDK
//
//  Created by Ronen Morecki on 3/15/12.
//  Copyright (c) 2012 ZooZ.com All rights reserved.
//

#import <Foundation/Foundation.h>
@interface ZooZInvoiceItem : NSObject

+ (ZooZInvoiceItem *)invoiceItem:(float)priceValue quantity:(float)itemQuantity name:(NSString *)itemName;

@property(nonatomic, retain) NSString *name;
@property(nonatomic, retain) NSString *itemId;
@property(nonatomic) float price;
@property(nonatomic) float quantity;
@property(nonatomic, retain) NSString *additionalDetails; //(200 chars)

@end


@interface ZooZInvoice : NSObject

//invoiceNumber is a customize refernce number that can be freely set to the invoice. It usually is some tracking refernce to the app own server.
+(ZooZInvoice *)invoiceWithRefNumber:(NSString *)invoiceNumber;

//Add item to the invoice.
-(void)addItem:(ZooZInvoiceItem *)item;
-(NSArray *)getItems;

@property(nonatomic, retain) NSString *invoiceNumber;
//Free text for custom description on the trasnaction (200 chars)
@property(nonatomic, retain) NSString *additionalDetails;

@end
