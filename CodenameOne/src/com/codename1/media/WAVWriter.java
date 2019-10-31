package com.codename1.media;

import java.io.InputStream;
import com.codename1.io.Util;
import java.io.IOException;
import com.codename1.io.FileSystemStorage;
import java.io.OutputStream;
import com.codename1.io.File;

/**
 * A class that can write raw PCM data to a WAV file.
 * 
 * 
 * @since 7.0
 * @author shannah
 */
public class WAVWriter implements AutoCloseable
{
    private File outputFile;
    private OutputStream out;
    private int samplingRate;
    private int channels;
    private int numBits;
    private long dataLength;
    
    /**
     * Creates a new writer for writing a WAV file.
     * @param outputFile The output file.
     * @param samplingRate The sampling rate.  E.g. 44100
     * @param channels The number of channels.  E.g. 1 or 2
     * @param numBits 8 or 16
     * @throws IOException 
     */
    public WAVWriter(final File outputFile, final int samplingRate, final int channels, final int numBits) throws IOException {
        this.outputFile = outputFile;
        this.out = FileSystemStorage.getInstance().openOutputStream(outputFile.getAbsolutePath());
        this.samplingRate = samplingRate;
        this.channels = channels;
        this.numBits = numBits;
    }
    
    private void writeHeader() throws IOException {
        final byte[] header = new byte[44];
        long totalDataLen = dataLength + 36;
        final long bitrate = this.samplingRate * this.channels * this.numBits;
        header[0] = 82;
        header[1] = 73;
        header[3] = (header[2] = 70);
        header[4] = (byte)(totalDataLen & 0xFFL);
        header[5] = (byte)(totalDataLen >> 8 & 0xFFL);
        header[6] = (byte)(totalDataLen >> 16 & 0xFFL);
        header[7] = (byte)(totalDataLen >> 24 & 0xFFL);
        header[8] = 87;
        header[9] = 65;
        header[10] = 86;
        header[11] = 69;
        header[12] = 102;
        header[13] = 109;
        header[14] = 116;
        header[15] = 32;
        header[16] = (byte)this.numBits;
        header[17] = 0;
        header[19] = (header[18] = 0);
        header[20] = 1;
        header[21] = 0;
        header[22] = (byte)this.channels;
        header[23] = 0;
        header[24] = (byte)(this.samplingRate & 0xFF);
        header[25] = (byte)(this.samplingRate >> 8 & 0xFF);
        header[26] = (byte)(this.samplingRate >> 16 & 0xFF);
        header[27] = (byte)(this.samplingRate >> 24 & 0xFF);
        header[28] = (byte)(bitrate / 8L & 0xFFL);
        header[29] = (byte)(bitrate / 8L >> 8 & 0xFFL);
        header[30] = (byte)(bitrate / 8L >> 16 & 0xFFL);
        header[31] = (byte)(bitrate / 8L >> 24 & 0xFFL);
        header[32] = (byte)(this.channels * this.numBits / 8);
        header[33] = 0;
        header[34] = 16;
        header[35] = 0;
        header[36] = 100;
        header[37] = 97;
        header[38] = 116;
        header[39] = 97;
        header[40] = (byte) (dataLength  & 0xff);
        header[41] = (byte) ((dataLength >> 8) & 0xff);
        header[42] = (byte) ((dataLength >> 16) & 0xff);
        header[43] = (byte) ((dataLength >> 24) & 0xff);
        this.out.write(header);
    }
    
    /**
     * Writes PCM data to the file.
     * @param pcmData PCM data to write.  These are float values between -1 and 1.
     * @param offset Offset in pcmData array to start writing.
     * @param len Length in pcmData array to write.
     * @throws IOException 
     */
    public void write(final float[] pcmData, final int offset, final int len) throws IOException {
        for (int i = 0; i < len; ++i) {
            final float sample = pcmData[offset + i];
            if (this.numBits == 8) {
                final byte byteSample = (byte)(sample * 127.0f);
                this.out.write(byteSample & 0xff);
                ++this.dataLength;
            }
            else {
                if (this.numBits != 16) {
                    throw new IllegalArgumentException("numBits must be 8 or 16 but found " + this.numBits);
                }
                final short shortSample = (short)(sample * 32767.0f);
                this.out.write(shortSample & 0xff);
                this.out.write((shortSample >> 8) & 0xff);
                this.dataLength += 2L;
            }
        }
    }
    
    private String getPCMFile() {
        return this.outputFile.getAbsolutePath() + ".pcm";
    }
    
    /**
     * Closes the writer, and writes the WAV file. 
     * @throws Exception 
     */
    @Override
    public void close() throws Exception {
        this.out.close();
        final FileSystemStorage fs = FileSystemStorage.getInstance();
        fs.rename(this.outputFile.getAbsolutePath(), new File(this.getPCMFile()).getName());
        try {
            this.out = fs.openOutputStream(this.outputFile.getAbsolutePath());
            final InputStream in = fs.openInputStream(this.getPCMFile());
            this.writeHeader();
            Util.copy(in, this.out);
            try {
                this.out.close();
            }
            catch (Throwable t) {}
            try {
                in.close();
            }
            catch (Throwable t2) {}
        }
        finally {
            fs.delete(this.getPCMFile());
        }
    }
}