/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
#import "CodenameOne_GLViewController.h"
#import "xmlvm.h"

#if defined(INCLUDE_CN1_LANGUAGE) && !TARGET_OS_WATCH && !TARGET_OS_TV
#import <Foundation/Foundation.h>
#import "java_lang_String.h"

#if __has_include(<MLKitLanguageID/MLKitLanguageID.h>)
#import <MLKitLanguageID/MLKitLanguageID.h>
#define CN1_HAS_MLKIT_LANGUAGE_ID 1
#endif
#if __has_include(<MLKitTranslate/MLKitTranslate.h>)
#import <MLKitTranslate/MLKitTranslate.h>
#import <MLKitCommon/MLKitCommon.h>
#define CN1_HAS_MLKIT_TRANSLATE 1
#endif
#if __has_include(<MLKitSmartReply/MLKitSmartReply.h>)
#import <MLKitSmartReply/MLKitSmartReply.h>
#define CN1_HAS_MLKIT_SMART_REPLY 1
#endif

static NSString *cn1LanguageJSON(NSDictionary *value) {
    NSError *error = nil;
    NSData *data = [NSJSONSerialization dataWithJSONObject:value options:0
                                                     error:&error];
    if (data == nil) {
        return [NSString stringWithFormat:@"{\"error\":\"%@\"}",
                error.localizedDescription ?: @"Could not encode language result"];
    }
    return [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
}

static NSString *cn1LanguageError(NSError *error, NSString *fallback) {
    return cn1LanguageJSON(@{
        @"error": error.localizedDescription ?: fallback
    });
}

static NSString *cn1IdentifyLanguage(NSString *text, float threshold) {
#if defined(CN1_HAS_MLKIT_LANGUAGE_ID)
    MLKLanguageIdentificationOptions *options =
            [[MLKLanguageIdentificationOptions alloc]
                    initWithConfidenceThreshold:threshold];
    MLKLanguageIdentification *identifier =
            [MLKLanguageIdentification languageIdentificationWithOptions:options];
    __block NSArray<MLKIdentifiedLanguage *> *languages = nil;
    __block NSError *requestError = nil;
    dispatch_semaphore_t semaphore = dispatch_semaphore_create(0);
    [identifier identifyPossibleLanguagesForText:text ?: @""
            completion:^(NSArray<MLKIdentifiedLanguage *> *result,
                         NSError *error) {
        languages = result;
        requestError = error;
        dispatch_semaphore_signal(semaphore);
    }];
    dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER);
    if (requestError != nil) {
        return cn1LanguageError(requestError, @"Language identification failed");
    }
    NSMutableArray *items = [NSMutableArray array];
    for (MLKIdentifiedLanguage *language in languages ?: @[]) {
        [items addObject:@{
            @"language": language.languageTag ?: @"und",
            @"confidence": @(language.confidence)
        }];
    }
    return cn1LanguageJSON(@{@"items": items});
#else
    return cn1LanguageJSON(@{@"error": @"ML Kit Language ID is not linked"});
#endif
}

static NSString *cn1TranslateLanguage(NSString *text, NSString *source,
                                      NSString *target) {
#if defined(CN1_HAS_MLKIT_TRANSLATE)
    MLKTranslateLanguage sourceLanguage = source.lowercaseString;
    MLKTranslateLanguage targetLanguage = target.lowercaseString;
    NSSet<MLKTranslateLanguage> *supported = MLKTranslateAllLanguages();
    if (sourceLanguage == nil || targetLanguage == nil ||
            ![supported containsObject:sourceLanguage] ||
            ![supported containsObject:targetLanguage]) {
        return cn1LanguageJSON(@{@"error": @"Unsupported translation language"});
    }
    MLKTranslatorOptions *options = [[MLKTranslatorOptions alloc]
            initWithSourceLanguage:sourceLanguage targetLanguage:targetLanguage];
    MLKTranslator *translator = [MLKTranslator translatorWithOptions:options];
    MLKModelDownloadConditions *conditions = [[MLKModelDownloadConditions alloc]
            initWithAllowsCellularAccess:YES allowsBackgroundDownloading:YES];
    __block NSString *translation = nil;
    __block NSError *requestError = nil;
    dispatch_semaphore_t semaphore = dispatch_semaphore_create(0);
    [translator downloadModelIfNeededWithConditions:conditions
            completion:^(NSError *error) {
        if (error != nil) {
            requestError = error;
            dispatch_semaphore_signal(semaphore);
            return;
        }
        [translator translateText:text ?: @""
                completion:^(NSString *result, NSError *error) {
            translation = result;
            requestError = error;
            dispatch_semaphore_signal(semaphore);
        }];
    }];
    dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER);
    if (requestError != nil) {
        return cn1LanguageError(requestError, @"Translation failed");
    }
    return cn1LanguageJSON(@{@"text": translation ?: @""});
#else
    return cn1LanguageJSON(@{@"error": @"ML Kit Translate is not linked"});
#endif
}

static NSString *cn1SmartReply(NSString *conversationJSON) {
#if defined(CN1_HAS_MLKIT_SMART_REPLY)
    NSError *jsonError = nil;
    NSDictionary *root = [NSJSONSerialization JSONObjectWithData:
            [conversationJSON dataUsingEncoding:NSUTF8StringEncoding]
            options:0 error:&jsonError];
    if (jsonError != nil || ![root isKindOfClass:[NSDictionary class]]) {
        return cn1LanguageError(jsonError, @"Invalid smart-reply conversation");
    }
    NSMutableArray<MLKTextMessage *> *messages = [NSMutableArray array];
    for (NSDictionary *value in root[@"items"] ?: @[]) {
        if (![value isKindOfClass:[NSDictionary class]]) continue;
        MLKTextMessage *message = [[MLKTextMessage alloc]
                initWithText:value[@"text"] ?: @""
                timestamp:[value[@"timestamp"] doubleValue]
                userID:value[@"participant"] ?: @"remote"
                isLocalUser:[value[@"local"] boolValue]];
        [messages addObject:message];
    }
    __block MLKSmartReplySuggestionResult *suggestions = nil;
    __block NSError *requestError = nil;
    dispatch_semaphore_t semaphore = dispatch_semaphore_create(0);
    [[MLKSmartReply smartReply] suggestRepliesForMessages:messages
            completion:^(MLKSmartReplySuggestionResult *result,
                         NSError *error) {
        suggestions = result;
        requestError = error;
        dispatch_semaphore_signal(semaphore);
    }];
    dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER);
    if (requestError != nil) {
        return cn1LanguageError(requestError, @"Smart Reply failed");
    }
    NSMutableArray *items = [NSMutableArray array];
    for (MLKSmartReplySuggestion *suggestion in suggestions.suggestions ?: @[]) {
        if (suggestion.text != nil) [items addObject:suggestion.text];
    }
    return cn1LanguageJSON(@{@"items": items});
#else
    return cn1LanguageJSON(@{@"error": @"ML Kit Smart Reply is not linked"});
#endif
}
#endif

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_cn1LanguageIsSupported___int_R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT feature) {
#if defined(INCLUDE_CN1_LANGUAGE) && !TARGET_OS_WATCH && !TARGET_OS_TV
    switch (feature) {
        case 0:
#if defined(CN1_HAS_MLKIT_LANGUAGE_ID)
            return 1;
#else
            return 0;
#endif
        case 1:
#if defined(CN1_HAS_MLKIT_TRANSLATE)
            return 1;
#else
            return 0;
#endif
        case 2:
#if defined(CN1_HAS_MLKIT_SMART_REPLY)
            return 1;
#else
            return 0;
#endif
        default:
            return 0;
    }
#else
    return 0;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_cn1LanguageIdentify___java_lang_String_float_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT text,
        JAVA_FLOAT minimumConfidence) {
#if defined(INCLUDE_CN1_LANGUAGE) && !TARGET_OS_WATCH && !TARGET_OS_TV
    return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
            cn1IdentifyLanguage(
                    toNSString(CN1_THREAD_GET_STATE_PASS_ARG text),
                    minimumConfidence));
#else
    return JAVA_NULL;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_cn1LanguageTranslate___java_lang_String_java_lang_String_java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT text,
        JAVA_OBJECT sourceLanguage, JAVA_OBJECT targetLanguage) {
#if defined(INCLUDE_CN1_LANGUAGE) && !TARGET_OS_WATCH && !TARGET_OS_TV
    return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
            cn1TranslateLanguage(
                    toNSString(CN1_THREAD_GET_STATE_PASS_ARG text),
                    toNSString(CN1_THREAD_GET_STATE_PASS_ARG sourceLanguage),
                    toNSString(CN1_THREAD_GET_STATE_PASS_ARG targetLanguage)));
#else
    return JAVA_NULL;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_cn1LanguageSmartReply___java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject,
        JAVA_OBJECT conversationJSON) {
#if defined(INCLUDE_CN1_LANGUAGE) && !TARGET_OS_WATCH && !TARGET_OS_TV
    return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
            cn1SmartReply(toNSString(CN1_THREAD_GET_STATE_PASS_ARG
                    conversationJSON)));
#else
    return JAVA_NULL;
#endif
}
