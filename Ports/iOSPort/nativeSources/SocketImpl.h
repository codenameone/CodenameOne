#import <Foundation/Foundation.h>

@interface SocketImpl : NSObject<NSStreamDelegate> {
    NSInputStream *inputStream;
    NSOutputStream *outputStream;
    int availableValue;
    NSString* errorMessage;
    int errorCode;
    BOOL connected;
    /// Accepted server-side sockets do raw descriptor I/O rather than going through
    /// NSStream: they are serviced on a background thread whose run loop never runs, and
    /// a scheduled CFStream would simply never deliver anything. -1 when unused.
    int rawFd;
}

-(BOOL)connect:(NSString*)host port:(int)port timeout:(int)timeout;
-(int)getAvailableInput;
-(NSString*)getErrorMessage;
+(NSString*)getIP;
-(NSData*)readFromStream;
-(void)writeToStream:(NSData*)param;
-(void)disconnect;
-(BOOL)listen:(int)param;
-(BOOL)listenLoopback:(int)param;
-(BOOL)isConnected;
-(int)getErrorCode;
-(BOOL)isSupported;

@end
