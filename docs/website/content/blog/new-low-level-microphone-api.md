---
title: New Low-level Microphone API
slug: new-low-level-microphone-api
url: /blog/new-low-level-microphone-api/
original_url: https://www.codenameone.com/blog/new-low-level-microphone-api.html
aliases:
- /blog/new-low-level-microphone-api.html
date: '2020-01-02'
author: Steve Hannah
---

![Header Image](/blog/new-low-level-microphone-api/new-low-level-microphone-api.jpg)

Today’s blog post will delve further into our new media features. We’ve recently added an API to access raw PCM data from the device’s microphone. Previously, the media recording API could only be configured to save audio to a file. This is fine for most use cases, but sometimes it is necessary to access the the raw PCM stream directly. For example for voice recognition, or audio processing, or audio visualization.

### How it works

In order to access an audio PCM stream, you need to create an `AudioBuffer` object, which will be used as a destination for microphone input.
    
    
    AudioBuffer buffer = MediaManager.getAudioBuffer("mybuffer.pcm", true, 4096);

A couple of points here:

  1. The “mybuffer.pcm” is the virtual path to the buffer. Think of it like a file path that doesn’t correspond to an actual file. This can be any arbitrary string. We will be referencing it later when we construct the media recorder, to redirect its output to this audio buffer.

  2. The 2nd parameter (`true`) says to “create” the audio buffer object if it doesn’t already exist in the central registry.

  3. The 3rd parameter, is the buffer size. You can put anything here, and the API will adapt. I’m using 4096 here, but that was chosen rather arbitrarily.

Next, you add a callback which will be executed whenever the buffer’s contents are changed. This happens when a new chunk of PCM data is available from the microphone.
    
    
    final float[] sampleData = new float[buffer.getMaxSize()];
    buffer.addCallback(buf->{
    	buf.copyTo(sampleData);
        int sampleRate = buf.getSampleRate();
        int numChannels = buf.getNumChannels();
        int len = buf.getSize();
    
       sendDataToServerForProcessing(sampleData, 0, len, sampleRate, numChannels);
    
    });

Some key points here:

  1. The callback does NOT run on the EDT. It runs on its own thread.

  2. The `buf.copyTo()` method will copy all new data from the buffer into our own float[] array. It will only write values in the range `[0, buf.getSize())`. Each entry will be a float between -1 and 1.

  3. `buf.getSize()` may return a different value in each invocation, as the “size” of the buffer reflects the current data in the buffer. Not to be confused with the maxSize of the buffer, which is the original size of the buffer, as it was created in the `getAudioBuffer()` method.

  4. If you are processing the data in any way, you’ll need to know both the sampleRate, and the number of channels of the input. It is important to collect this data from the audioBuffer inside this callback, and not depend on the settings you provided originally to `createMediaRecorder()`.

We’ll use MediaRecorderBuilder to construct our media recorder now as follows:
    
    
    MediaRecorderBuilder mrb = new MediaRecorderBuilder()
        .path("mybuffer.pcm")
        .redirectToAudioBuffer(true);
    
    Media microphone = MediaManager.createMediaRecorder(mrb);

Notice that, for the `path()` parameter of the builder, we use the same value we used in `getAudioBuffer()`. This is extremely important, otherwise the media recorder won’t run the callback in your AudioBuffer instance.

We can start recording now using `microphone.play()`, and pause using `microphone.pause()`. Or use the new async APIs, `playAsync()` and `pauseAsync()` to gain more clarity about the recording state.

### Saving PCM Stream to a WAV File

In order to test the `AudioBuffer` class, we needed to be able to play the PCM stream that we capture to make sure that it is working correctly, and that it hasn’t been corrupted in any way. We added a class, `WAVWriter`, for writing a PCM stream to a WAV file to facilitate this testing. A WAV file, after all, just contains a raw stream of PCM data, with some headers to declare the data format, so this class is pretty minimal.

The following example records directly from the PCM stream to a WAV file in file system storage.
    
    
    WAVWriter wavWriter;
    AudioBuffer audioBuffer;
    private void record() throws IOException {
         audioBuffer = MediaManager.getAudioBuffer(bufferPath, true, 4096);
         final float[] floatSamples = new float[audioBuffer.getMaxSize()];
         audioBuffer.addCallback(buf->{
            synchronized(clipLock) {
                if (wavWriter == null) {
                    try {
                        wavWriter = new WAVWriter(
                            new File(fileName),
                            buf.getSampleRate(),
                            buf.getNumChannels(),
                            16
    					);
                    } catch (IOException ex) {
                        Log.e(ex);
                        return;
                    }
                }
    
                buf.copyTo(floatSamples);
                try {
                    wavWriter.write(floatSamples, 0, buf.getSize());
                } catch (IOException ex) {
                    Log.e(ex);
                }
            }
        });
        }
        MediaRecorderBuilder builder = new MediaRecorderBuilder()
            .audioChannels(1)
            .path(bufferPath)
            .redirectToAudioBuffer(true);
    
        MediaManager.createMediaRecorder(builder));
    
    
        synchronized(clipLock) {
            wavWriter = null;
        }
    }
    
    // … And when you’re finished recording, just close the WAVWriter
    // for the file to be written.
    wavWriter.close();

The key parts of this example are:

  1. We don’t necessarily need to instantiate the WAVWriter object inside the `AudioBuffer` callback, but we do need some information that the callback provides: the sample rate, and number of channels. This information is supplied in the audiobuffer callback, and won’t change, so you could also just fetch this information in the first callback, and store it for when and where you do instantiate the `WAVWriter` object.

  2. The `WAVWriter.write(float[] samples, int offset, int len)` method is where you can pass the PCM samples directly to WAV file.

  3. Remember to call `close()` on the WAVWriter to ensure that the file is written.

You can find some examples using the `AudioBuffer` and `WAVWriter` classes to write PCM streams to a WAV File in the [Samples project](https://github.com/codenameone/CodenameOne/tree/master/Samples). Specifically, the [AsyncMediaSample](https://github.com/codenameone/CodenameOne/blob/master/Samples/samples/AsyncMediaSample/AsyncMediaSample.java) and the [AudioBufferSample](https://github.com/codenameone/CodenameOne/blob/master/Samples/samples/AudioBufferSample/AudioBufferSample.java).

### Sample Rates and Downsampling

A PCM data stream is a digital approximation of a sound wave form. The sample rate, usually expressed in Hz (hertz) is the number of samples we extract per second. A sample rate of 16000 Hz indicates that we are extracting 16000 floating point values (per channel) per second. The higher the sample rate, the better wave approximation will be, and therefore the better quality the sound will be. But higher sample rates also correspond to larger file sizes.

When you construct a media recorder, you can request a specific sample rate, but there is no guarantee that the underlying platform will comply with your request. Some platforms only support the native sample rate of the audio hardware, so you’re at the mercy of the audio chip to a certain extent. You can find out the actual sample rate by calling `audioBuffer.getSampleRate()`, any time after the first callback is executed – as this is where the platform informs the audio buffer about the underlying sample rate.

Some common sample rates you’ll see are 16000, 22050, 44100, and 48000. If you are passing the PCM data stream to service that only accepts a certain sample rate, then you may need to downsample the data. The AudioBuffer class includes a downsample() method with a rudimentary algorithm that may be sufficient for some cases. It is lacking some of the features of high-end down-sampling algorithms, such as low pass filtering, and it does noticeably lower the audio quality, but if your application doesn’t need “perfect sound”, then it might be appropriate for your needs. If you do need perfect sound, you should either perform the downsampling server-side, or use a 3rd-party sound library.

The `downSample()` method works as follows:
    
    
    audioBuffer.downSample(16000);  // downsample to 16000Hz)

You should call this method inside your callback, before copying the data to your float samples buffer. This is because it will modify the data in the audio buffer, and update both the “size” property, and the “sampleRate” property of the buffer, to accurately reflect the new sample rate.

The [AudioBufferSample](https://github.com/codenameone/CodenameOne/blob/master/Samples/samples/AudioBufferSample/AudioBufferSample.java) includes an example usage of this method.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
