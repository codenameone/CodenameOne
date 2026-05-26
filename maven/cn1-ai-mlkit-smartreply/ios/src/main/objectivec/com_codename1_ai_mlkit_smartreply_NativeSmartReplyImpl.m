#import "com_codename1_ai_mlkit_smartreply_NativeSmartReplyImpl.h"
#import <UIKit/UIKit.h>
#import <MLKitSmartReply/MLKSmartReply.h>
#import <MLKitSmartReply/MLKTextMessage.h>
#import <MLKitSmartReply/MLKSmartReplySuggestion.h>
#import <MLKitSmartReply/MLKSmartReplySuggestionResult.h>
#import <arpa/inet.h>

@implementation com_codename1_ai_mlkit_smartreply_NativeSmartReplyImpl

-(NSData*)suggest:(NSString*)param {
    // param is a JSON array of {role,message,timestamp,userId}.
    NSError *err = nil;
    NSArray *items = [NSJSONSerialization JSONObjectWithData:
                      [param dataUsingEncoding:NSUTF8StringEncoding]
                      options:0 error:&err];
    NSMutableArray *messages = [NSMutableArray array];
    if ([items isKindOfClass:[NSArray class]]) {
        for (NSDictionary *d in items) {
            if (![d isKindOfClass:[NSDictionary class]]) continue;
            NSString *role = d[@"role"] ?: @"user";
            BOOL isLocalUser = [role isEqualToString:@"user"];
            NSString *text = d[@"message"] ?: @"";
            NSNumber *ts = d[@"timestamp"] ?: @0;
            MLKTextMessage *m = [[MLKTextMessage alloc]
                initWithText:text timestamp:[ts doubleValue]
                userID:(d[@"userId"] ?: @"u")
                isLocalUser:isLocalUser];
            [messages addObject:m];
        }
    }
    __block NSArray<NSString *> *out = @[];
    dispatch_semaphore_t sem = dispatch_semaphore_create(0);
    [[MLKSmartReply smartReply] suggestRepliesForMessages:messages
        completion:^(MLKSmartReplySuggestionResult * _Nullable result, NSError * _Nullable e) {
            NSMutableArray *m = [NSMutableArray array];
            for (MLKSmartReplySuggestion *s in result.suggestions ?: @[]) {
                if (s.text) [m addObject:s.text];
            }
            out = m;
            dispatch_semaphore_signal(sem);
        }];
    dispatch_semaphore_wait(sem, DISPATCH_TIME_FOREVER);
    return [self packStrings:out];
}

-(NSData*)packStrings:(NSArray<NSString *> *)strings {
    NSMutableData *out = [NSMutableData data];
    uint32_t count = htonl((uint32_t)strings.count);
    [out appendBytes:&count length:sizeof(count)];
    for (NSString *s in strings) {
        NSData *u = [s dataUsingEncoding:NSUTF8StringEncoding];
        uint32_t len = htonl((uint32_t)u.length);
        [out appendBytes:&len length:sizeof(len)];
        [out appendData:u];
    }
    return out;
}

-(BOOL)isSupported{
    return YES;
}

@end
