#import "SocketImpl.h"
#include <ifaddrs.h>
#include <arpa/inet.h>
#include "xmlvm.h"
#import "CodenameOne_GLViewController.h"

@implementation SocketImpl

-(BOOL)connect:(NSString*)host port:(int)port {
    CFReadStreamRef readStream;
    CFWriteStreamRef writeStream;
    CFStreamCreatePairWithSocketToHost(NULL, (BRIDGE_CAST CFStringRef)host, port, &readStream, &writeStream);
    inputStream = (BRIDGE_CAST NSInputStream *)readStream;
    outputStream = (BRIDGE_CAST NSOutputStream *)writeStream;
    [inputStream setDelegate:self];
    [outputStream setDelegate:self];
    [inputStream scheduleInRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
    [outputStream scheduleInRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
    [inputStream open];
    [outputStream open];
    connected = YES;
    return YES;
}

-(int)getAvailableInput{
    //return availableValue;
    return [inputStream hasBytesAvailable] ? 1 : 0;
}

-(NSString*)getErrorMessage{
    return errorMessage;
}

+(NSString*)getIP{
  NSString *address = @"error";
  struct ifaddrs *interfaces = NULL;
  struct ifaddrs *temp_addr = NULL;
  int success = 0;

  // retrieve the current interfaces - returns 0 on success
  success = getifaddrs(&interfaces);
  if (success == 0) {
    // Loop through linked list of interfaces
    temp_addr = interfaces;
    while (temp_addr != NULL) {
      if( temp_addr->ifa_addr->sa_family == AF_INET) {
        // Check if interface is en0 which is the wifi connection on the iPhone
        if ([[NSString stringWithUTF8String:temp_addr->ifa_name] isEqualToString:@"en0"]) {
          // Get NSString from C String
          address = [NSString stringWithUTF8String:inet_ntoa(((struct sockaddr_in *)temp_addr->ifa_addr)->sin_addr)];
        }
      }

      temp_addr = temp_addr->ifa_next;
    }
  }

  // Free memory
  freeifaddrs(interfaces);

  return address;
}

-(NSData*)readFromStream{
    uint8_t buffer[8192];
    int len;
    
    if([inputStream hasBytesAvailable]) {
        len = [inputStream read:buffer maxLength:sizeof(buffer)];
        if (len > 0) {
            return [NSData dataWithBytes:buffer length:len];
        }
    }
    return nil;
}

-(void)writeToStream:(NSData*)param{
    [outputStream write:[param bytes] maxLength:[param length]];
}

-(void)disconnect{
    if(inputStream != nil) {
        [outputStream close];
        [outputStream removeFromRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
        [inputStream close];
        [inputStream removeFromRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
        inputStream = nil;
        outputStream = nil;
    }
}

-(BOOL)listen:(int)param{
    return NO;
}

-(BOOL)isConnected{
    return connected;
}

-(int)getErrorCode{
    return errorCode;
}

-(BOOL)isSupported{
    return YES;
}

- (void)stream:(NSStream *)theStream handleEvent:(NSStreamEvent)streamEvent {
	switch (streamEvent) {
            
		case NSStreamEventOpenCompleted:
            connected = YES;
			break;
            
		case NSStreamEventHasBytesAvailable:
            availableValue = 1;
			break;
            
		case NSStreamEventErrorOccurred:
            errorCode = 1;
            errorMessage = @"General Error";
            connected = NO;
			break;
            
		case NSStreamEventEndEncountered:
            [theStream close];
            [theStream removeFromRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
 			break;
            
		default:
			CN1Log(@"Unknown event");
	}
}

@end
