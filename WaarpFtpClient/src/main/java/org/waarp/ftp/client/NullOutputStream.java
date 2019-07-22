/**
 * 
 */
package org.waarp.ftp.client;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Simple /dev/null output stream
 * 
 * @author frederic
 * 
 */
public class NullOutputStream extends OutputStream {

    /**
	 * 
	 */
    public NullOutputStream() {
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
    }

    @Override
    public void write(byte[] b) throws IOException {
    }

    @Override
    public void write(int b) throws IOException {
    }

}
