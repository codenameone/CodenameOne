// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::video-capture-constraints-java-001[]
VideoCaptureConstraints cnst = new VideoCaptureConstraint()
    .preferredQuality(VideoCaptureConstraints.QUALITY_LOW)
    .preferredMaxLength(5);
// end::video-capture-constraints-java-001[]

// tag::video-capture-constraints-java-002[]
String videoPath = Capture.captureVideo(cnst);
// end::video-capture-constraints-java-002[]

// tag::video-capture-constraints-java-003[]
if (cnst.isMaxLengthSupported()) {
    // The max length constraint that we specified is supported on this platform.
} else{
    // The max length constraint is NOT supported.
    // Check the effective max length constraint value to see if it may be partially
    // supported
    int effectiveMaxLength = cnst.getMaxLength();
    if (effectiveMaxLength == 0) {
        // Max length is not supported at all... The user will be able
        // to capture a video without duration restrictions
    } else {
        // Max length was set to some different value than we set in our
        // preferredMaxLength, but the platform is at least trying to accommodate us.
    }
}
// end::video-capture-constraints-java-003[]

// tag::video-capture-constraints-java-004[]
VideoCaptureConstraints cnst = new VideoCaptureConstraints()
    .preferredWidth(320)
    .preferredHeight(240);
// end::video-capture-constraints-java-004[]

// tag::video-capture-constraints-java-005[]
if (cnst.isSizeSupported()) {
   // Yay! This platform supports our constraint, so the captured video will
   // be exactly 320×240.
} else {
   // Not supported... let's see if the platform will at least try to accommodate us
   int effectiveWidth = cnst.getWidth();
   int effectiveHeight = cnst.getHeight();
   int quality = cnst.getQuality();
   if (effectiveWidth == 0 && effectiveHeight == 0) {
       // This platform has no control over width and height
       // In many cases it will try to at least set the quality approximate
       if (quality != 0) {
          //  The platform set the quality for us to try to comply.
          // Since 320×240 is pretty small, the quality would probably
          // be set to QUALITY_LOW
       }
   } else {
       // The platform couldn't capture at 320×240, but it has provided an
       // alternate size that is as close to that as possible.
   }
}
// end::video-capture-constraints-java-005[]
