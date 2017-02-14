package org.rrd4j.core;

import java.io.IOException;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

/**
 * {@link org.rrd4j.core.RrdBackendFactory} that uses
 * <a href="https://aws.amazon.com/s3/">Amazon S3</a> for data storage.
 *
 * @author Paul Melici
 */
public class RrdS3BackendFactory extends RrdBackendFactory {
	private final AmazonS3 amazonS3;
	private final String s3Bucket;

	/**
	 * Creates a RrdS3BackendFactory using the standard S3 client configuration
	 * as provided by {@link AmazonS3ClientBuilder#standard()}
	 *
	 * @param s3Bucket
	 *            <a href=
	 *            "http://docs.aws.amazon.com/AmazonS3/latest/dev/UsingBucket.html">S3
	 *            bucket</a> to use. The specified bucket must already exist and
	 *            the caller must have
	 *            {@link com.amazonaws.services.s3.model.Permission#Write}
	 *            permission to the bucket to upload an object.
	 */
	public RrdS3BackendFactory(String s3Bucket) {
		this(AmazonS3ClientBuilder.standard().build(), s3Bucket);
	}

	/**
	 * Creates a RrdS3BackendFactory using the provided {@link AmazonS3}.
	 *
	 * @param amazonS3
	 *            Amazon S3 client
	 * @param s3Bucket
	 *            <a href=
	 *            "http://docs.aws.amazon.com/AmazonS3/latest/dev/UsingBucket.html">S3
	 *            bucket</a> to use. The specified bucket must already exist and
	 *            the caller must have
	 *            {@link com.amazonaws.services.s3.model.Permission#Write}
	 *            permission to the bucket to upload an object.
	 */
	public RrdS3BackendFactory(AmazonS3 amazonS3, String s3Bucket) {
		this.amazonS3 = amazonS3;
		this.s3Bucket = s3Bucket;

		tryRegisterAndSetAsDefaultFactory(this);
	}

	/** {@inheritDoc} */
	@Override
	protected RrdBackend open(String path, boolean readOnly) throws IOException {
		return new RrdS3Backend(path, amazonS3, s3Bucket);
	}

	/** {@inheritDoc} */
	@Override
	protected boolean exists(String path) throws IOException {
		return amazonS3.doesObjectExist(s3Bucket, path);
	}

	/** {@inheritDoc} */
	@Override
	protected boolean shouldValidateHeader(String path) throws IOException {
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public String getName() {
		return "S3";
	}
}
