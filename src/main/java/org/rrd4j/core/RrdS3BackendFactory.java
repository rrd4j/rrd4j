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
	private static final RrdBackendCompression DEFAULT_COMPRESSION = null;

	private final AmazonS3 amazonS3;
	private final String s3Bucket;
	private final RrdBackendCompression.Compressor compressor;

	/**
	 * Creates a RrdS3BackendFactory using the standard S3 client configuration
	 * as provided by {@link AmazonS3ClientBuilder#standard()} and the default
	 * compression configuration.
	 *
	 * @param s3Bucket
	 *            <a href=
	 *            "http://docs.aws.amazon.com/AmazonS3/latest/dev/UsingBucket.html">S3
	 *            bucket</a> to use. The specified bucket must already exist and
	 *            the caller must have appropriate permissions to the bucket.
	 */
	public RrdS3BackendFactory(String s3Bucket) {
		this(s3Bucket, AmazonS3ClientBuilder.standard().build());
	}

	/**
	 * Creates a RrdS3BackendFactory using the provided {@link AmazonS3} and the
	 * default compression configuration.
	 * 
	 * @param s3Bucket
	 *            <a href=
	 *            "http://docs.aws.amazon.com/AmazonS3/latest/dev/UsingBucket.html">S3
	 *            bucket</a> to use. The specified bucket must already exist and
	 *            the caller must have appropriate permissions to the bucket.
	 * @param amazonS3
	 *            Amazon S3 client
	 */
	public RrdS3BackendFactory(String s3Bucket, AmazonS3 amazonS3) {
		this(s3Bucket, amazonS3, DEFAULT_COMPRESSION);
	}

	/**
	 * Creates a RrdS3BackendFactory using the provided {@link AmazonS3} and the
	 * specified {@link RrdBackendCompression} type.
	 * 
	 * @param s3Bucket
	 *            <a href=
	 *            "http://docs.aws.amazon.com/AmazonS3/latest/dev/UsingBucket.html">S3
	 *            bucket</a> to use. The specified bucket must already exist and
	 *            the caller must have appropriate permissions to the bucket.
	 * @param amazonS3
	 *            Amazon S3 client
	 * @param compressionType
	 *            Use the specified pre-defined compression type.
	 *            <code>null</code> to disable compression.
	 */
	public RrdS3BackendFactory(String s3Bucket, AmazonS3 amazonS3, RrdBackendCompression compressionType) {
		this(s3Bucket, amazonS3, compressionType == null ? null : compressionType.getCompressor());
	}

	/**
	 * Creates a RrdS3BackendFactory using the provided {@link AmazonS3} and a
	 * custom {@link RrdBackendCompression.Compressor}.
	 * 
	 * @param s3Bucket
	 *            <a href=
	 *            "http://docs.aws.amazon.com/AmazonS3/latest/dev/UsingBucket.html">S3
	 *            bucket</a> to use. The specified bucket must already exist and
	 *            the caller must have appropriate permissions to the bucket.
	 * @param amazonS3
	 *            Amazon S3 client
	 * @param compressor
	 *            Use the specified custom
	 *            {@link RrdBackendCompression.Compressor}. <code>null</code> to
	 *            disable compression.
	 */
	public RrdS3BackendFactory(String s3Bucket, AmazonS3 amazonS3, RrdBackendCompression.Compressor compressor) {
		this.amazonS3 = amazonS3;
		this.s3Bucket = s3Bucket;
		this.compressor = compressor;

		tryRegisterAndSetAsDefaultFactory(this);
	}

	/** {@inheritDoc} */
	@Override
	protected RrdBackend open(String path, boolean readOnly) throws IOException {
		return new RrdS3Backend(path, amazonS3, s3Bucket, compressor);
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
