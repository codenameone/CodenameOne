#import "com_codenameone_examples_hellocodenameone_TestDiagnosticsNativeImpl.h"
#include <execinfo.h>
#include <signal.h>
#include <string.h>
#include <unistd.h>

static volatile sig_atomic_t cn1ssSignalHandlersInstalled = 0;

static void cn1ss_writeLine(const char *line) {
    if (line == NULL) {
        return;
    }
    write(STDERR_FILENO, line, strlen(line));
    write(STDERR_FILENO, "\n", 1);
}

static void cn1ss_signalHandler(int signo) {
    cn1ss_writeLine("CN1SS:NATIVE:SIGNAL:BEGIN");
    switch (signo) {
        case SIGABRT: cn1ss_writeLine("CN1SS:NATIVE:SIGNAL:type=SIGABRT"); break;
        case SIGSEGV: cn1ss_writeLine("CN1SS:NATIVE:SIGNAL:type=SIGSEGV"); break;
        case SIGBUS: cn1ss_writeLine("CN1SS:NATIVE:SIGNAL:type=SIGBUS"); break;
        case SIGILL: cn1ss_writeLine("CN1SS:NATIVE:SIGNAL:type=SIGILL"); break;
        case SIGFPE: cn1ss_writeLine("CN1SS:NATIVE:SIGNAL:type=SIGFPE"); break;
        case SIGTRAP: cn1ss_writeLine("CN1SS:NATIVE:SIGNAL:type=SIGTRAP"); break;
        default: cn1ss_writeLine("CN1SS:NATIVE:SIGNAL:type=OTHER"); break;
    }

    void *stack[64];
    int frames = backtrace(stack, 64);
    backtrace_symbols_fd(stack, frames, STDERR_FILENO);
    cn1ss_writeLine("CN1SS:NATIVE:SIGNAL:END");

    signal(signo, SIG_DFL);
    raise(signo);
}

@implementation com_codenameone_examples_hellocodenameone_TestDiagnosticsNativeImpl

-(void)enableNativeCrashSignalLogging:(NSString*)reason {
    if (cn1ssSignalHandlersInstalled) {
        return;
    }
    cn1ssSignalHandlersInstalled = 1;
    NSLog(@"CN1SS:NATIVE:SIGNAL:install handlers reason=%@", reason != nil ? reason : @"unspecified");
    signal(SIGABRT, cn1ss_signalHandler);
    signal(SIGSEGV, cn1ss_signalHandler);
    signal(SIGBUS, cn1ss_signalHandler);
    signal(SIGILL, cn1ss_signalHandler);
    signal(SIGFPE, cn1ss_signalHandler);
    signal(SIGTRAP, cn1ss_signalHandler);
}

-(void)dumpNativeThreads:(NSString*)reason {
    @try {
        NSString *label = reason != nil ? reason : @"unspecified";
        NSLog(@"CN1SS:NATIVE:THREAD_DUMP:BEGIN reason=%@", label);
        NSLog(@"CN1SS:NATIVE:THREAD_DUMP:current=%@ isMain=%@", [NSThread currentThread], [NSThread isMainThread] ? @"true" : @"false");
        NSArray *symbols = [NSThread callStackSymbols];
        for (NSString *line in symbols) {
            NSLog(@"CN1SS:NATIVE:STACK:%@", line);
        }
        NSLog(@"CN1SS:NATIVE:THREAD_DUMP:END reason=%@", label);
    } @catch (NSException *ex) {
        NSLog(@"CN1SS:NATIVE:THREAD_DUMP:ERROR reason=%@ exception=%@", reason, ex);
    }
}

-(void)failFastWithNativeThreadDump:(NSString*)reason {
    NSString *label = reason != nil ? reason : @"unspecified";
    NSLog(@"CN1SS:NATIVE:FAIL_FAST:BEGIN reason=%@", label);
    [self dumpNativeThreads:label];
    NSLog(@"CN1SS:NATIVE:FAIL_FAST:ABORT reason=%@", label);
    abort();
}

-(BOOL)isSupported{
    return YES;
}

@end
