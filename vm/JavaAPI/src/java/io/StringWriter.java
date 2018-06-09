/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package java.io;

/**
 *
 * @author shannah
 */
public class StringWriter extends Writer implements Appendable {
    
    private StringBuffer buf;
    
    public StringWriter() {
        buf = new StringBuffer();
    }
    
    public StringWriter(int initialSize) {
        buf = new StringBuffer(initialSize);
    }

    @Override
    public void close() throws IOException {
        
    }

    @Override
    public void flush() throws IOException {
        
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        buf.append(cbuf, off, len);
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        buf.append(str, off, off + len);
    }

    @Override
    public void write(int c) throws IOException {
        buf.append((char)c);
    }

    @Override
    public void write(String str) throws IOException {
        buf.append(str);
    }

    @Override
    public void write(char[] cbuf) throws IOException {
        buf.append(cbuf);
    }

    @Override
    public String toString() {
        return buf.toString();
    }
    
    public StringBuffer getBuffer() {
        return buf;
    }
    
    
    public StringWriter append(char c) {
        buf.append(c);
        return this;
    }
    
    public StringWriter append(CharSequence csq) {
        buf.append(csq);
        return this;
    }
    
    public StringWriter append(CharSequence csq, int start, int end) {
        buf.append(csq, start, end);
        return this;
    }
    
    
    
    
    
    
    
}
