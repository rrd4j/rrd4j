package org.rrd4j.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Compression support for {@link RrdBackend}s
 * 
 * @author Paul Melici
 *
 */
public enum RrdBackendCompression {
	NONE(null), GZIP(new GzipCompressor());

	private final Compressor compressor;

	private RrdBackendCompression(Compressor compressor) {
		this.compressor = compressor;
	}

	public Compressor getCompressor() {
		return compressor;
	}

	/**
	 * Simple interface for any compression algorithm.
	 * 
	 * @author Paul Melici
	 *
	 */
	public static interface Compressor {
		/**
		 * Wraps {@link OutputStream} with compression {@link OutputStream}.
		 * 
		 * @param out
		 *            target {@link OutputStream}
		 * @return compressed {@link OutputStream}
		 * @throws IOException
		 *             if any
		 */
		OutputStream compress(OutputStream out) throws IOException;

		/**
		 * Wraps {@link InputStream} with decompression {@link InputStream}.
		 * 
		 * @param compressedData
		 *            {@link InputStream} containing the compressed data
		 * @return uncompressed {@link InputStream}
		 * @throws IOException
		 *             if any
		 */
		InputStream decompress(InputStream compressedData) throws IOException;
	}

	/**
	 * GZIP implementation of {@link Compressor}
	 * 
	 * @author Paul Melici
	 *
	 */
	public static class GzipCompressor implements Compressor {

		@Override
		public GZIPOutputStream compress(OutputStream out) throws IOException {
			return new GZIPOutputStream(out);
		}

		@Override
		public GZIPInputStream decompress(InputStream compressedData) throws IOException {
			return new GZIPInputStream(compressedData);
		}

	}

	private static final int BUFFER_SIZE = 1024 * 4;

	/**
	 * Reads {@link InputStream} to byte array. {@link InputStream} will be
	 * closed even if this method throws exception.
	 * 
	 * @param is
	 *            the {@link InputStream}
	 * @return byte contents of {@link InputStream}
	 * @throws IOException
	 *             if any
	 */
	static byte[] toByteArray(InputStream is) throws IOException {
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			byte[] b = new byte[BUFFER_SIZE];
			int n = 0;
			while ((n = is.read(b)) != -1) {
				output.write(b, 0, n);
			}
			return output.toByteArray();
		}
	}
}
