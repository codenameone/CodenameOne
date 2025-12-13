#if 0
// tag::dispatchAsync[]
dispatch_async(dispatch_get_main_queue(), ^{
    // your native code here...
});
// end::dispatchAsync[]

// tag::dispatchSync[]
dispatch_sync(dispatch_get_main_queue(), ^{
    // your native code here...
});
// end::dispatchSync[]
#endif
