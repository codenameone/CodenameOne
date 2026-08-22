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

#import "CN1JailbreakDetector.h"
#import <TargetConditionals.h>
#import <dlfcn.h>
#import <errno.h>
#import <mach/mach.h>
#import <mach-o/dyld.h>
#import <mach-o/dyld_images.h>
#import <stdlib.h>
#import <string.h>
#import <sys/mount.h>
#import <sys/param.h>
#import <sys/stat.h>
#import <sys/syscall.h>
#import <sys/sysctl.h>
#import <unistd.h>

// Note: there is deliberately no fork() probe here. The classic form,
// "if (fork() == 0) { exit(0); }", terminates the *child* and lets the parent
// sail on, so it never did what it claimed. Reinstating it correctly would
// mean calling a restricted syscall that trips App Review static analysis, for
// a signal the dyld-image and restricted-path probes already carry.

// Everything below the simulator guard is compiled only for a device. The
// simulator answer is a constant empty string -- it runs on a Mac, where none of
// these questions mean what they mean on iOS -- and compiling the probes anyway
// left the build warning about a file full of functions nobody calls.
#if !(TARGET_IPHONE_SIMULATOR)

// The path probes describe an iOS sandbox that has been broken out of, and a Mac is
// not that sandbox: /bin/bash and /usr/sbin/sshd ship with macOS and /private is
// writable there, so on Mac Catalyst they fire on a stock machine -- and an app that
// asks isJailbrokenDevice() at startup, as ours does, then refuses to launch on every
// Mac. The instrumentation probes are outside this guard, because an injected dylib
// or a hooking library means the same thing wherever it is loaded.
#if !TARGET_OS_MACCATALYST && !TARGET_OS_OSX

/**
 * What a single existence probe concluded. The three-way answer is the point:
 * a sandbox denial is not the same answer as ENOENT, and reading it as "absent"
 * would make two probes that were both simply refused look like they disagreed.
 */
typedef NS_ENUM(int, CN1PathState) {
    CN1PathUnknown = 0,
    CN1PathAbsent  = 1,
    CN1PathPresent = 2
};

/**
 * The raw lstat syscall number. Reaching past libc matters because the ObjC and
 * libc entry points are precisely what a jailbreak-bypass tweak hooks; the two
 * answers are then compared rather than trusted.
 */
#if defined(SYS_lstat64)
#define CN1_SYS_LSTAT SYS_lstat64
#elif defined(SYS_lstat)
#define CN1_SYS_LSTAT SYS_lstat
#endif

static CN1PathState cn1StateFromResult(int rc, int savedErrno) {
    if (rc == 0) {
        return CN1PathPresent;
    }
    // Only ENOENT/ENOTDIR are the filesystem saying "there is nothing here".
    // Everything else -- EPERM from the sandbox, EACCES on a parent directory,
    // ENOSYS from a syscall this kernel does not implement -- means the probe
    // failed to answer, and treating that as absent both loses the signal and
    // manufactures disagreements between probes that were both just refused.
    if (savedErrno == ENOENT || savedErrno == ENOTDIR) {
        return CN1PathAbsent;
    }
    return CN1PathUnknown;
}

/**
 * lstat through libc. Deliberately lstat and not stat: on a rootless jailbreak
 * /var/jb is a symlink onto a mounted bootstrap, and a semi-tethered device that
 * has been rebooted without re-jailbreaking still carries the symlink while the
 * target is gone. stat() follows the link and reports nothing; lstat() sees the
 * link itself, which is the evidence that matters.
 */
static CN1PathState cn1LibcLstat(const char *path) {
    struct stat st;
    errno = 0;
    int rc = lstat(path, &st);
    return cn1StateFromResult(rc, errno);
}

/**
 * The same question asked below libc. A tweak that hooks lstat() but not the raw
 * trap answers differently here, and that difference is reported as hookedApi.
 */
static CN1PathState cn1RawLstat(const char *path) {
#ifdef CN1_SYS_LSTAT
    struct stat st;
    memset(&st, 0, sizeof(st));
    errno = 0;
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
    int rc = (int) syscall(CN1_SYS_LSTAT, path, &st);
#pragma clang diagnostic pop
    return cn1StateFromResult(rc == 0 ? 0 : -1, errno);
#else
    (void) path;
    return CN1PathUnknown;
#endif
}

/**
 * Existence according to the two Foundation/POSIX entry points a tweak is most
 * likely to hook. Both follow symlinks, so neither can take part in the
 * disagreement check -- an unreachable symlink target makes them legitimately
 * differ from lstat on a device with no tweak at all. They contribute presence
 * only: another way for a path to be seen, never a reason to cry hook.
 */
static CN1PathState cn1FollowingProbes(const char *path) {
    errno = 0;
    CN1PathState viaAccess = cn1StateFromResult(access(path, F_OK), errno);
    if (viaAccess == CN1PathPresent) {
        return CN1PathPresent;
    }
    NSString *objcPath = [NSString stringWithUTF8String:path];
    if (objcPath != nil && [[NSFileManager defaultManager] fileExistsAtPath:objcPath]) {
        return CN1PathPresent;
    }
    return viaAccess;
}

/**
 * Whether `path` exists, asked four ways. `hookSuspected` is set when the two
 * lstat probes -- which must agree on any untampered device, since they ask the
 * kernel the identical question -- did not.
 */
static BOOL cn1PathPresent(const char *path, BOOL *hookSuspected) {
    CN1PathState viaLibc = cn1LibcLstat(path);
    CN1PathState viaRaw = cn1RawLstat(path);
    if (hookSuspected != NULL
            && ((viaLibc == CN1PathPresent && viaRaw == CN1PathAbsent)
                || (viaLibc == CN1PathAbsent && viaRaw == CN1PathPresent))) {
        *hookSuspected = YES;
    }
    if (viaLibc == CN1PathPresent || viaRaw == CN1PathPresent) {
        return YES;
    }
    return cn1FollowingProbes(path) == CN1PathPresent;
}

#endif // !TARGET_OS_MACCATALYST && !TARGET_OS_OSX

/**
 * Appends a signal code once. The probes below can reach the same conclusion
 * from several directions -- three rootless paths, or a /var/jb mount and the
 * symlink that names it -- and a caller splitting the result on commas should
 * not have to fold duplicates back out.
 */
static void cn1AddSignal(NSMutableArray *signals, NSString *code) {
    if (![signals containsObject:code]) {
        [signals addObject:code];
    }
}

/**
 * Lowercased fragments of the file name of anything that hooks, injects, or
 * exists only to defeat this file. Matched as substrings against the lowercased
 * path of every loaded image, so a version suffix or an install prefix does not
 * matter. `/var/jb/` is in the list because an image loaded out of the rootless
 * bootstrap is conclusive regardless of what it is called.
 */
static NSArray *cn1HookingImageNames(void) {
    // Autoreleased and rebuilt per scan, deliberately. This file is compiled
    // without ARC -- the app target sets CLANG_ENABLE_OBJC_ARC = NO -- so caching
    // the literal in a dispatch_once static stores an object nobody retained, and
    // iosJailbreakSignals() wraps its call in POOL_BEGIN/POOL_END. The first call
    // would leave the static dangling and the foreground recheck would scan the
    // loaded images through freed memory. The cost this avoids is one array per
    // scan, not per image: the scan hoists it and passes it down.
    return @[
        @"/var/jb/",
        @"substrate.dylib",          // also MobileSubstrate/CydiaSubstrate
        @"substrateinserter.dylib",
        @"substrateloader.dylib",
        @"libsubstrate.dylib",
        @"libsubstitute.dylib",      // Substitute, the Electra-era injector
        @"libhooker.dylib",          // libhooker, the Chimera/Odyssey injector
        @"libblackjack",             // libhooker's loader
        @"ellekit",                  // the rootless injector palera1n ships
        @"tweakinject",              // ElleKit's dylib list / loader
        @"libertylite.dylib",
        @"tsprotector",
        @"fridagadget",
        @"frida-agent",
        @"libcycript",
        @"cynject",
        @"rocketbootstrap",
        @"sslkillswitch",
        @"shadow.dylib",             // the bypass tweak, not a system name
        @"choicy",
        @"libsandy",
        @"flyjb",
        @"preferenceloader"
    ];
}

static BOOL cn1ImagePathIsHooking(const char *imagePath, NSArray *needles) {
    if (imagePath == NULL) {
        return NO;
    }
    NSString *path = [NSString stringWithUTF8String:imagePath];
    if (path == nil) {
        return NO;
    }
    NSString *lowered = [path lowercaseString];
    for (NSString *needle in needles) {
        if ([lowered containsString:needle]) {
            return YES;
        }
    }
    return NO;
}

/**
 * The loaded-image scan, run twice.
 *
 * The public form walks _dyld_get_image_name(), which is the documented API and
 * therefore the one a bypass tweak hooks. The second walk reads the same table
 * out of dyld_all_image_infos, reached through task_info(TASK_DYLD_INFO), so a
 * tweak has to hook both to stay hidden. When the raw walk sees an injected
 * library that the public walk does not, that gap is reported as hookedApi in
 * addition to hookLib: it says not only that something is injected but that
 * something is actively lying about it.
 *
 * Counts are deliberately not compared. The two tables legitimately differ by an
 * entry or two across OS versions -- dyld itself is in one and not always the
 * other -- so a count check is a false positive waiting for the next iOS release.
 */
static void cn1ScanLoadedImages(NSMutableArray *signals) {
    NSArray *needles = cn1HookingImageNames();
    BOOL publicFound = NO;
    for (uint32_t i = 0; i < _dyld_image_count(); i++) {
        if (cn1ImagePathIsHooking(_dyld_get_image_name(i), needles)) {
            publicFound = YES;
            break;
        }
    }

    BOOL rawFound = NO;
    struct task_dyld_info dyldInfo;
    mach_msg_type_number_t infoCount = TASK_DYLD_INFO_COUNT;
    if (task_info(mach_task_self(), TASK_DYLD_INFO,
                  (task_info_t) &dyldInfo, &infoCount) == KERN_SUCCESS) {
        const struct dyld_all_image_infos *all =
                (const struct dyld_all_image_infos *) (uintptr_t) dyldInfo.all_image_info_addr;
        // infoArray is briefly NULL while dyld rewrites the table, which is a
        // normal transient state and not a signal.
        if (all != NULL && all->infoArray != NULL) {
            for (uint32_t i = 0; i < all->infoArrayCount; i++) {
                if (cn1ImagePathIsHooking(all->infoArray[i].imageFilePath, needles)) {
                    rawFound = YES;
                    break;
                }
            }
        }
    }

    if (publicFound || rawFound) {
        cn1AddSignal(signals, @"hookLib");
    }
    if (rawFound && !publicFound) {
        cn1AddSignal(signals, @"hookedApi");
    }
}

#if !TARGET_OS_MACCATALYST && !TARGET_OS_OSX // rationale above, at the first region
/**
 * Rootful jailbreak artifacts: files that only exist once the sealed system
 * volume has been modified.
 */
static NSArray *cn1RootfulPaths(void) {
    return @[
        @"/Applications/Cydia.app",
        @"/Applications/Sileo.app",
        @"/Applications/Zebra.app",
        @"/Applications/Filza.app",
        @"/Library/MobileSubstrate/MobileSubstrate.dylib",
        @"/usr/lib/libsubstitute.dylib",
        @"/usr/lib/libhooker.dylib",
        @"/usr/lib/substrate/SubstrateLoader.dylib",
        @"/usr/libexec/cydia",
        @"/usr/sbin/sshd",
        @"/bin/bash",
        @"/etc/apt",
        @"/private/var/lib/apt/",
        @"/var/lib/undecimus",
        @"/var/checkra1n.dmg",
        @"/var/binpack",
        @"/.installed_unc0ver",
        @"/.bootstrapped_electra"
    ];
}

/**
 * Rootless jailbreak artifacts. A rootless jailbreak leaves the signed system
 * volume sealed and bootstraps Procursus into /var/jb instead, so every path
 * above is genuinely absent and the sandbox-escape write genuinely fails. Before
 * this list a palera1n/Dopamine device produced no signals at all, which is
 * exactly the miss a customer reported on palera1n 2.1.1 rootless.
 *
 * /var/jb comes first and is the one that matters; the rest are here so that
 * deleting or hiding the symlink alone is not enough.
 */
static NSArray *cn1RootlessPaths(void) {
    return @[
        @"/var/jb",
        @"/var/jb/usr/lib/TweakInject.dylib",
        @"/var/jb/usr/lib/libellekit.dylib",
        @"/var/jb/Applications/Sileo.app",
        @"/var/jb/etc/apt",
        @"/var/jb/usr/bin/dpkg",
        @"/var/jb/bin/sh",
        @"/private/preboot/jb",
        @"/private/preboot/procursus"
    ];
}

/**
 * Paths that exist on every stock device. A probe reporting all of these absent
 * -- absent, not denied -- is not reading the real filesystem, which means a
 * filter is sitting in front of it. Framework bundles and SystemVersion.plist
 * are on disk even on modern iOS, unlike the dylibs that now live only in the
 * shared cache and would make a terrible canary.
 */
static NSArray *cn1CanaryPaths(void) {
    return @[
        @"/System/Library/CoreServices/SystemVersion.plist",
        @"/System/Library/Frameworks/Foundation.framework"
    ];
}

static void cn1ScanFilesystem(NSMutableArray *signals) {
    BOOL hookSuspected = NO;

    for (NSString *path in cn1RootfulPaths()) {
        if (cn1PathPresent([path fileSystemRepresentation], &hookSuspected)) {
            cn1AddSignal(signals, @"jailbreakFile");
        }
    }
    for (NSString *path in cn1RootlessPaths()) {
        if (cn1PathPresent([path fileSystemRepresentation], &hookSuspected)) {
            cn1AddSignal(signals, @"rootlessPath");
        }
    }
    // Every canary has to come back absent, not just one. A blanket filter hides
    // them all, whereas a future iOS that merely relocates one file would, under
    // an "any" rule, make this fatal signal fire on every stock device -- turning
    // an OS update into an app that refuses to launch. The conjunction is what
    // makes it safe to derive a fatal signal from a hard-coded path list.
    NSArray *canaries = cn1CanaryPaths();
    BOOL everyCanaryAbsent = canaries.count > 0;
    for (NSString *path in canaries) {
        const char *canaryPath = [path fileSystemRepresentation];
        BOOL canaryHook = NO;
        if (cn1PathPresent(canaryPath, &canaryHook)) {
            everyCanaryAbsent = NO;
        } else if (cn1LibcLstat(canaryPath) != CN1PathAbsent
                && cn1RawLstat(canaryPath) != CN1PathAbsent) {
            // Denied rather than missing. A sandbox refusal also leaves
            // cn1PathPresent returning NO, and counting that as evidence of a
            // filter would accuse a device where nothing is wrong.
            everyCanaryAbsent = NO;
        }
        if (canaryHook) {
            hookSuspected = YES;
        }
    }
    if (everyCanaryAbsent) {
        hookSuspected = YES;
    }

    if (hookSuspected) {
        cn1AddSignal(signals, @"hookedApi");
    }

    // Writing outside the sandbox should be impossible. This still only fires on
    // a rootful jailbreak -- a rootless one leaves / read-only, which is the
    // whole point of the name -- so it is a corroborating signal, not a primary.
    NSString *testPath = @"/private/cn1JailbreakTest.txt";
    NSError *error = nil;
    BOOL wroteFile = [@"Test" writeToFile:testPath atomically:YES
                                 encoding:NSUTF8StringEncoding error:&error];
    if (wroteFile && error == nil) {
        [[NSFileManager defaultManager] removeItemAtPath:testPath error:nil];
        cn1AddSignal(signals, @"restrictedWrite");
    }
}

/**
 * The mount table. Two independent things are visible here and neither needs a
 * path to be guessed correctly:
 *
 * - A writable root filesystem. Stock iOS seals / read-only; a rootful jailbreak
 *   has to remount it to install anything.
 * - An extra mounted filesystem belonging to a bootstrap. A rootless jailbreak
 *   mounts its Procursus image and binds it in, and the mount point says so even
 *   if every file under it were somehow hidden from lstat.
 */
static void cn1ScanMounts(NSMutableArray *signals) {
    struct statfs rootFs;
    if (statfs("/", &rootFs) == 0 && (rootFs.f_flags & MNT_RDONLY) == 0) {
        cn1AddSignal(signals, @"mountRW");
    }

    int count = getfsstat(NULL, 0, MNT_NOWAIT);
    if (count <= 0) {
        return;
    }
    size_t bytes = (size_t) count * sizeof(struct statfs);
    struct statfs *mounts = (struct statfs *) malloc(bytes);
    if (mounts == NULL) {
        return;
    }
    count = getfsstat(mounts, (int) bytes, MNT_NOWAIT);
    for (int i = 0; i < count; i++) {
        const char *mountPoint = mounts[i].f_mntonname;
        if (strstr(mountPoint, "/var/jb") != NULL
                || strstr(mountPoint, "procursus") != NULL) {
            cn1AddSignal(signals, @"rootlessPath");
            break;
        }
    }
    free(mounts);
}
#endif // !TARGET_OS_MACCATALYST && !TARGET_OS_OSX

#endif // !(TARGET_IPHONE_SIMULATOR)

NSString *cn1JailbreakSignals(void) {
#if (TARGET_IPHONE_SIMULATOR)
    return @"";
#else
    NSMutableArray *signals = [NSMutableArray array];

    // Dynamic library injection, as used by Frida/Objection and friends.
    if (getenv("DYLD_INSERT_LIBRARIES") != NULL) {
        cn1AddSignal(signals, @"dyldInsert");
    }

    // Substrate's safe mode. Set by the injector itself, so it says a tweak
    // loader is present even in the run where it declined to inject.
    if (getenv("_MSSafeMode") != NULL || getenv("_SafeMode") != NULL) {
        cn1AddSignal(signals, @"hookLib");
    }

    // Known hooking / jailbreak-bypass libraries loaded into the process.
    cn1ScanLoadedImages(signals);

    // Skipped on Mac Catalyst -- see the rationale on the region these two live in.
#if !TARGET_OS_MACCATALYST && !TARGET_OS_OSX
    cn1ScanFilesystem(signals);
    cn1ScanMounts(signals);
#endif

    // A debugger or instrumentation tool attached to the process.
    struct kinfo_proc info;
    size_t size = sizeof(info);
    int name[4] = {CTL_KERN, KERN_PROC, KERN_PROC_PID, getpid()};
    if (sysctl(name, 4, &info, &size, NULL, 0) == 0
            && (info.kp_proc.p_flag & P_TRACED) != 0) {
        cn1AddSignal(signals, @"traced");
    }

    return [signals componentsJoinedByString:@","];
#endif
}

#ifdef CN1_DETECT_JAILBREAK
/**
 * Whether a signal says the DEVICE is compromised, as opposed to saying somebody is
 * looking at the app.
 *
 * The probe list grew a `traced` signal so DeviceIntegrity could report a debugger,
 * which is worth reporting -- attaching one is how a build gets instrumented. Exiting on
 * it is a different matter: a clean physical device launched from Xcode is traced, so the
 * launch gate terminated every ordinary debug session on a project that leaves
 * ios.detectJailbreak on. That reads as "the app crashes on device", and the usual fix a
 * developer reaches for is turning the protection off.
 *
 * Everything else in the list is fatal, `hookedApi` included: a probe disagreeing with
 * itself, or a stock system path reporting absent, only happens when something is
 * rewriting the answers, and a device carrying a detection-bypass tweak is precisely the
 * device this gate exists to refuse.
 */
static BOOL cn1IsJailbreakSignal(NSString *signal) {
    return [signal isEqualToString:@"dyldInsert"]
        || [signal isEqualToString:@"hookLib"]
        || [signal isEqualToString:@"hookedApi"]
        || [signal isEqualToString:@"jailbreakFile"]
        || [signal isEqualToString:@"rootlessPath"]
        || [signal isEqualToString:@"mountRW"]
        || [signal isEqualToString:@"restrictedWrite"];
}

void cn1DetectJailbreakBypassesAndExit(void) {
    NSString *signals = cn1JailbreakSignals();
    NSMutableArray *fatal = [NSMutableArray array];
    for (NSString *signal in [signals componentsSeparatedByString:@","]) {
        if (cn1IsJailbreakSignal(signal)) {
            [fatal addObject:signal];
        }
    }
    if (fatal.count > 0) {
        NSLog(@"Jailbreak bypass detected: %@", [fatal componentsJoinedByString:@","]);
        // Non-zero, because exit(0) tells the OS and any crash reporter that the app
        // finished normally -- so a refused launch on a compromised device was
        // indistinguishable in the logs from a clean one, which is the one place this
        // gate is supposed to leave a trace.
        exit(EXIT_FAILURE);
    }
}
#endif
