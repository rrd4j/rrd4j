package org.rrd4j.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;

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
	private final int S3_STATUS_CODE_NOT_FOUND = 404;

	private final AmazonS3 amazonS3;
	private final String s3Bucket;

	private volatile boolean dirty = false;

	/**
	 * <p>
	 * Constructor for RrdMongoDBBackend.
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
	 *            the caller must have
	 *            {@link com.amazonaws.services.s3.model.Permission#Write}
	 *            permission to the bucket to upload an object.
	 * @throws IOException
	 */
	public RrdS3Backend(String path, AmazonS3 amazonS3, String s3Bucket) throws IOException {
		super(path);
		this.amazonS3 = amazonS3;
		this.s3Bucket = s3Bucket;

		try (S3Object s3Object = amazonS3.getObject(s3Bucket, path)) {
			this.buffer = IOUtils.toByteArray(s3Object.getObjectContent());
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
			ObjectMetadata objectMetadata = new ObjectMetadata();
			objectMetadata.setContentLength(buffer.length);

			PutObjectResult putObject = amazonS3.putObject(s3Bucket, getPath(), new ByteArrayInputStream(buffer), objectMetadata);

		}
	}
}
