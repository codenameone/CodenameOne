# Codename One Samples

This directory contains a number of runnable code samples that demonstrate the various Codename One APIs.  This is a work in progress.  Currently there is only a small number of samples, but more will be added over time.  It also contains a Netbeans Java project that allows you to launch, view source, and add new samples.

![SampleRunner](images/screenshot.png)

## Usage

**From the Command Line**:

~~~~
git clone https://github.com/codenameone/CodenameOne
cd CodenameOne
ant samples
~~~~

**From NetBeans**

Open the "Samples" project in Netbeans and run it.

## Where are the Samples located

The samples are located inside the "samples" directory.

## Adding Additional Samples

1. Open the Sample Runner (e.g. using `ant run`, or by opening the Samples project in Netbeans and running it).
2. Select "File" > "Create New Sample".
3. Enter a name for the sample in the prompt.

This will create a new directory at "samples/YourSampleName", with a single .java file YourSampleName.java.

You can edit this file with the contents of your sample.

NOTE:  Don't change the package name or the class name of this sample.  Also the enter sample code must be contained inside this file.  You cannot add additional files to this directory, and expect them to work.

## Adding Custom Build Hints

If you are building samples for targets other than the simulator, you may need to provide your own build hints (e.g. android key store, iOS certificates, etc..).  You can add your own build hints both on a global level, and at a per-sample level.

### Editing Global Build Hints

Select "File" > "Edit Global Build Hints"

This will open the global build hints file (located in config/codenameone_settings.properties).  Here you can add any build hints that you need to add to all builds.  

NOTE: Remember to use the full property name for build hints, e.g. include prefix `codename1.arg` for the property keys.

### Editing Per-Sample Build hints

In the row for a particular sample, press the "More..." button, then select "Edit Build Hints".  This will open the build hints file (located in config/SAMPLE_NAME/codenameone_settings.properties).

