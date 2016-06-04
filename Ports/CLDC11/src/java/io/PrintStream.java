package java.io;
/**
 * A PrintStream adds functionality to another output stream, namely the ability to print representations of various data values conveniently. Two other features are provided as well. Unlike other output streams, a PrintStream never throws an IOException; instead, exceptional situations merely set an internal flag that can be tested via the checkError method.
 * All characters printed by a PrintStream are converted into bytes using the platform's default character encoding.
 * Since: JDK1.0, CLDC 1.0
 */
public class PrintStream extends java.io.OutputStream{
    /**
     * Create a new print stream. This stream will not flush automatically.
     * out - The output stream to which values and objects will be printed
     */
    public PrintStream(java.io.OutputStream out){
         //TODO codavaj!!
    }

    /**
     * Flush the stream and check its error state. The internal error state is set to true when the underlying output stream throws an IOException, and when the setError method is invoked.
     */
    public boolean checkError(){
        return false; //TODO codavaj!!
    }

    /**
     * Close the stream. This is done by flushing the stream and then closing the underlying output stream.
     */
    public void close(){
        return; //TODO codavaj!!
    }

    /**
     * Flush the stream. This is done by writing any buffered output bytes to the underlying output stream and then flushing that stream.
     */
    public void flush(){
        return; //TODO codavaj!!
    }

    /**
     * Print a boolean value. The string produced by
     * is translated into bytes according to the platform's default character encoding, and these bytes are written in exactly the manner of the
     * method.
     */
    public void print(boolean b){
        return; //TODO codavaj!!
    }

    /**
     * Print an array of characters. The characters are converted into bytes according to the platform's default character encoding, and these bytes are written in exactly the manner of the
     * method.
     */
    public void print(char c){
        return; //TODO codavaj!!
    }

    void print(char[] s){
        return; //TODO codavaj!!
    }

    /**
     * Print a double-precision floating point number. The string produced by
     * is translated into bytes according to the platform's default character encoding, and these bytes are written in exactly the manner of the
     * method.
     */
    public void print(double d){
        return; //TODO codavaj!!
    }

    /**
     * Print a floating point number. The string produced by
     * is translated into bytes according to the platform's default character encoding, and these bytes are written in exactly the manner of the
     * method.
     */
    public void print(float f){
        return; //TODO codavaj!!
    }

    /**
     * Print an integer. The string produced by
     * is translated into bytes according to the platform's default character encoding, and these bytes are written in exactly the manner of the
     * method.
     */
    public void print(int i){
        return; //TODO codavaj!!
    }

    /**
     * Print a long integer. The string produced by
     * is translated into bytes according to the platform's default character encoding, and these bytes are written in exactly the manner of the
     * method.
     */
    public void print(long l){
        return; //TODO codavaj!!
    }

    /**
     * Print an object. The string produced by the
     * method is translated into bytes according to the platform's default character encoding, and these bytes are written in exactly the manner of the
     * method.
     */
    public void print(java.lang.Object obj){
        return; //TODO codavaj!!
    }

    /**
     * Print a string. If the argument is null then the string "null" is printed. Otherwise, the string's characters are converted into bytes according to the platform's default character encoding, and these bytes are written in exactly the manner of the
     * method.
     */
    public void print(java.lang.String s){
        return; //TODO codavaj!!
    }

    /**
     * Terminate the current line by writing the line separator string. The line separator string is defined by the system property line.separator, and is not necessarily a single newline character ('\n').
     */
    public void println(){
        return; //TODO codavaj!!
    }

    /**
     * Print a boolean and then terminate the line. This method behaves as though it invokes
     * and then
     * .
     */
    public void println(boolean x){
        return; //TODO codavaj!!
    }

    /**
     * Print an array of characters and then terminate the line. This method behaves as though it invokes
     * and then
     * .
     */
    public void println(char x){
        return; //TODO codavaj!!
    }

    void println(char[] x){
        return; //TODO codavaj!!
    }

    /**
     * Print a double and then terminate the line. This method behaves as though it invokes
     * and then
     * .
     */
    public void println(double x){
        return; //TODO codavaj!!
    }

    /**
     * Print a float and then terminate the line. This method behaves as though it invokes
     * and then
     * .
     */
    public void println(float x){
        return; //TODO codavaj!!
    }

    /**
     * Print an integer and then terminate the line. This method behaves as though it invokes
     * and then
     * .
     */
    public void println(int x){
        return; //TODO codavaj!!
    }

    /**
     * Print a long and then terminate the line. This method behaves as though it invokes
     * and then
     * .
     */
    public void println(long x){
        return; //TODO codavaj!!
    }

    /**
     * Print an Object and then terminate the line. This method behaves as though it invokes
     * and then
     * .
     */
    public void println(java.lang.Object x){
        return; //TODO codavaj!!
    }

    /**
     * Print a String and then terminate the line. This method behaves as though it invokes
     * and then
     * .
     */
    public void println(java.lang.String x){
        return; //TODO codavaj!!
    }

    /**
     * Set the error state of the stream to true.
     */
    protected void setError(){
        return; //TODO codavaj!!
    }

    /**
     * Write len bytes from the specified byte array starting at offset off to this stream.
     * Note that the bytes will be written as given; to write characters that will be translated according to the platform's default character encoding, use the print(char) or println(char) methods.
     */
    public void write(byte[] buf, int off, int len){
        return; //TODO codavaj!!
    }

    /**
     * Write the specified byte to this stream.
     * Note that the byte is written as given; to write a character that will be translated according to the platform's default character encoding, use the print(char) or println(char) methods.
     */
    public void write(int b){
        return; //TODO codavaj!!
    }

}
