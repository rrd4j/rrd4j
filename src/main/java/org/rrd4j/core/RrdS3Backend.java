package org.rrd4j.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;

/**
 * {@link org.rrd4j.core.RrdBackend} that uses
 * <a href="https://aws.amazon.com/s3/">Amazon S3</a> for data storage.
 *
 * @author Paul Melici
 */
public class RrdS3Backend extends RrdByteArrayBackend {
	static final int S3_STATUS_CODE_NOT_FOUND = 404;

	private final AmazonS3 amazonS3;
	private final String s3Bucket;
	private final RrdBackendCompression.Compressor compressor;

	private volatile boolean dirty = false;

	/**
	 * <p>
	 * Constructor for RrdS3Backend.
	 * </p>
	 *
	 * @param path
	 *            a {@link java.lang.String} object.
	 * @param amazonS3
	 *            Amazon S3 client
	 * @param s3Bucket
	 *            <a href=
	 *            "http://docs.aws.amazon.com/AmazonS3/latest/dev/UsingBucket.html">S3
	 *            bucket</a> to use. The specified bucket must already exist and
	 *            the caller must have appropriate permissions to the bucket.
	 * @param compressor
	 *            Use the specified {@link RrdBackendCompression.Compressor}.
	 *            <code>null</code> to disable compression.
	 * @throws IOException
	 *             if any
	 */
	public RrdS3Backend(String path, AmazonS3 amazonS3, String s3Bucket, RrdBackendCompression.Compressor compressor)
			throws IOException {
		super(path);
		this.amazonS3 = amazonS3;
		this.s3Bucket = s3Bucket;
		this.compressor = compressor;

		try (S3Object s3Object = amazonS3.getObject(s3Bucket, path)) {
			InputStream in = s3Object.getObjectContent();
			if (compressor != null) {
				in = compressor.decompress(in);
			}
			this.buffer = IOUtils.toByteArray(in);
		} catch (AmazonServiceException ase) {
			if (ase.getStatusCode() == S3_STATUS_CODE_NOT_FOUND) {
				// this is OK on creation. means the object doesn't exist.
				this.buffer = null;
			} else {
				// rethrow exception if due to something other than object not
				// found.
				throw ase;
			}
		}
	}

	/**
	 * <p>
	 * write.
	 * </p>
	 *
	 * @param offset
	 *            a long.
	 * @param bytes
	 *            an array of byte.
	 * @throws java.io.IOException
	 *             if any.
	 */
	@Override
	protected synchronized void write(long offset, byte[] bytes) throws IOException {
		super.write(offset, bytes);
		dirty = true;
	}

	/** {@inheritDoc} */
	@Override
	public void close() throws IOException {
		if (dirty) {

			// get data to write to S3. Will be raw RRD data if no compression,
			// or compressed data if there is a compressor.
			//
			// In the case of compressed data, we need to compress it up-front
			// (as opposed to streaming compressing) because S3 needs content
			// size for upload.
			byte[] bytesToWrite;
			if (compressor == null) {
				bytesToWrite = buffer;
			} else {
				// assume reasonable compression ratio for buffer sizing
				int size = (int) (buffer.length * 0.7);
				ByteArrayOutputStream baos = new ByteArrayOutputStream(size);

				try (OutputStream compressedOut = compressor.compress(baos)) {
					compressedOut.write(buffer);
				}
				bytesToWrite = baos.toByteArray();
			}

			ObjectMetadata objectMetadata = new ObjectMetadata();
			objectMetadata.setContentLength(bytesToWrite.length);

			PutObjectResult putObject = amazonS3.putObject(s3Bucket, getPath(), new ByteArrayInputStream(bytesToWrite),
					objectMetadata);
		}
	}
}
