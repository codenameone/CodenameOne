// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::motion-sensors-java-001[]
MotionSensorManager m = MotionSensorManager.getInstance();
MotionSensor accelerometer = m.getSensor(MotionSensorManager.TYPE_ACCELEROMETER);
if (accelerometer != null) {
    accelerometer.addListener(new MotionSensorListener() {
        public void motionReceived(MotionEvent evt) {
            // x, y and z are in meters per second squared
            label.setText(evt.getX() + ", " + evt.getY() + ", " + evt.getZ());
            label.getParent().revalidate();
        }
    });
}
// end::motion-sensors-java-001[]

// tag::motion-sensors-java-002[]
accelerometer.removeListener(listener);
// end::motion-sensors-java-002[]

// tag::motion-sensors-java-003[]
MotionSensorManager.getInstance().addGestureListener(GestureEvent.TYPE_SHAKE, new GestureListener() {
    public void gestureDetected(GestureEvent evt) {
        Dialog.show("Shaken", "You shook the device", "OK", null);
    }
});
// end::motion-sensors-java-003[]

// tag::motion-sensors-java-004[]
MotionSensorManager m = MotionSensorManager.getInstance();
m.setShakeThreshold(15.0);     // require a more vigorous shake
m.setTiltThreshold(0.5);       // tilt angle in radians
m.setSamplingInterval(33);     // sample at about 30Hz for snappier gestures
// end::motion-sensors-java-004[]
