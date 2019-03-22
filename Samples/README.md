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