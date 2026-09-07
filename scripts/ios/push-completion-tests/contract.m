#import <Foundation/Foundation.h>
#import <dispatch/dispatch.h>
#include <assert.h>
#include <stdlib.h>
#if PUSH_ENABLED
#include "caller.h"
#endif

extern void com_codename1_impl_ios_IOSNative_firePushCompletionHandler___long(void *, long long);
extern void com_codename1_impl_ios_IOSNative_releaseOldestPushCompletionHandler__(void *);

int main(void) {
    // Production routing and completion bookkeeping both run on the main queue.
    dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool {
#if PUSH_ENABLED
            __block int first = 0, second = 0, empty = 0;
            long long a = CN1PushCompletionBegin(^{ first++; });
            long long b = CN1PushCompletionBegin(^{ second++; });
            assert(a != 0 && b != 0 && a != b);
            assert(CN1PushCompletionBegin(nil) == 0);
            // A type-3 notification has two callbacks; an overlapping push has one.
            CN1PushCompletionRetain(a);
            CN1PushCompletionRetain(a);
            CN1PushCompletionRetain(b);
            CN1PushCompletionFinishRouting(a);
            CN1PushCompletionFinishRouting(b);
            assert(first == 0 && second == 0);
            CN1PushCompletionFinishRouting(CN1PushCompletionBegin(^{ empty++; }));
            assert(empty == 1);

            com_codename1_impl_ios_IOSNative_firePushCompletionHandler___long(NULL, b);
            com_codename1_impl_ios_IOSNative_releaseOldestPushCompletionHandler__(NULL);
            dispatch_async(dispatch_get_main_queue(), ^{
                assert(second == 1 && first == 0);
                com_codename1_impl_ios_IOSNative_releaseOldestPushCompletionHandler__(NULL);
                dispatch_async(dispatch_get_main_queue(), ^{
                    assert(first == 1 && second == 1);
                    // Late/duplicate completions must not fire a grant twice.
                    com_codename1_impl_ios_IOSNative_firePushCompletionHandler___long(NULL, a);
                    com_codename1_impl_ios_IOSNative_firePushCompletionHandler___long(NULL, b);
                    com_codename1_impl_ios_IOSNative_releaseOldestPushCompletionHandler__(NULL);
                    dispatch_async(dispatch_get_main_queue(), ^{
                        assert(first == 1 && second == 1);
                        exit(0);
                    });
                });
            });
#else
            // Push-disabled apps still link the Java entry points as no-op stubs.
            com_codename1_impl_ios_IOSNative_firePushCompletionHandler___long(NULL, 0);
            com_codename1_impl_ios_IOSNative_releaseOldestPushCompletionHandler__(NULL);
            exit(0);
#endif
        }
    });
    dispatch_main();
}
