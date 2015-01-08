package net.montoyo.mcef.utilities;

import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream with the length data.
 * @author montoyo
 *
 */
public class SizedInputStream extends InputStream {
	
	private InputStream source;
	private long length;
	
	/**
	 * Constructs a new sized input stream.
	 * 
	 * @param is The original input stream.
	 * @param len The estimated length of the data that can be retrieved from is.
	 */
	public SizedInputStream(InputStream is, long len) {
		source = is;
		length = len;
	}
	
	/**
	 * Call this to know the estimated length of this stream.
	 * @return The estimated length of the data that can be retrieved from is.
	 */
	public long getContentLength() {
		return length;
	}

	@Override
	public int read() throws IOException {
		return source.read();
	}
	
	@Override
	public int read(byte[] data) throws IOException {
		return source.read(data);
	}
	
	@Override
	public int read(byte[] data, int off, int len) throws IOException {
		return source.read(data, off, len);
	}
	
	@Override
	public long skip(long s) throws IOException {
		return source.skip(s);
	}
	
	@Override
	public int available() throws IOException {
		return source.available();
	}
	
	@Override
	public void close() throws IOException {
		source.close();
	}
	
	@Override
	public synchronized void mark(int limit) {
		source.mark(limit);
	}
	
	@Override
	public synchronized void reset() throws IOException {
		source.reset();
	}
	
	@Override
	public boolean markSupported() {
		return source.markSupported();
	}

}

