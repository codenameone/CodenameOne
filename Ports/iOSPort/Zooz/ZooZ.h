//
//  ZooZ.h
// 
//
//  Created by Ronen Morecki on 6/16/11.
//  Copyright 2011 ZooZ.com. All rights reserved.
//

#import  <UIKit/UIKit.h>
#import "ZooZPaymentRequest.h"
#import "ZooZPaymentResponse.h"


@protocol ZooZPaymentCallbackDelegate<NSObject>

@required
//The payment finished successfully call back to dialog is on background thread, no need to auto release pool, as this been taken care of. (The dialog is still open since v1.3.2). You shouldn't update your UI on this, just process the payment data
- (void)paymentSuccessWithResponse:(ZooZPaymentResponse *)response; 

//Dialog is closed after payment finished successfully (see paymentSuccessWithResponse:) - this is where you should update your UI on success transaction
- (void)paymentSuccessDialogClosed; 

//User closed the dialog without paying
- (void)paymentCanceled; 

//Some error occured in calling ZooZ to open the payment request
- (void)openPaymentRequestFailed:(ZooZPaymentRequest *)request withErrorCode:(int)errorCode andErrorMessage:(NSString *)errorMessage;

@optional
//Return YES or NO if to show alert message or not
-(BOOL)serviceErrorOccuredWithCode:(int)errorCode andMessage:(NSString *)msg;
@end

@interface ZooZ : NSObject <UIAlertViewDelegate>

+(ZooZ *)sharedInstance;

//Create here the payment meta data.  The returned object is needed later for opening the Payment dialog.
-(ZooZPaymentRequest *)createPaymentRequestWithTotal:(float)amount invoiceRefNumber:(NSString *)invoiceNumber delegate:(id<ZooZPaymentCallbackDelegate>)del;

-(ZooZPaymentRequest *)createManageFundSourcesRequestWithDelegate:(id<ZooZPaymentCallbackDelegate>)del;


//To fasten the dialog opening in openPayment:forAppKey: you can call this method in the background when your app starts or anytime else before the payment
//If you want to call open dialog again after successful payment then this functions should be called again. Returns YES or NO if succeeded or failed in init.
//@autorleasepool is handled inside. 
-(BOOL)preInitialize:(NSString *)appKey isSandboxEnv:(BOOL)isSandbox;

//Opens the payment dialog
-(void)openPayment:(ZooZPaymentRequest *)request forAppKey:(NSString *)appKey;

-(void)cancelPaymentDialog;

//Flag if to process real payments or test mode.
@property (nonatomic) BOOL sandbox;
//Tint color for the ZooZ dialog NavBar
@property (nonatomic, retain) UIColor *tintColor;
//Tint color for the ZooZ dialog NavBar buttons
@property (nonatomic, retain) UIColor *barButtonTintColor;
//Image for the zooz dialog navbar title
@property (nonatomic, retain) UIImage *barTitleImage;
//Used for iPad only if special window structure is applied.
@property (assign) UIView *rootView;
@property (nonatomic, readonly) ZooZPaymentRequest *currentRequest;




@end
