#import <Foundation/Foundation.h>

@interface SocketImpl : NSObject<NSStreamDelegate> {
    NSInputStream *inputStream;
    NSOutputStream *outputStream;
    int availableValue;
    NSString* errorMessage;
    int errorCode;
    BOOL connected;
}

-(BOOL)connect:(NSString*)host port:(int)port;
-(int)getAvailableInput;
-(NSString*)getErrorMessage;
+(NSString*)getIP;
-(NSData*)readFromStream;
-(void)writeToStream:(NSData*)param;
-(void)disconnect;
-(BOOL)listen:(int)param;
-(BOOL)isConnected;
-(int)getErrorCode;
-(BOOL)isSupported;

@end
