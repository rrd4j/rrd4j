package org.rrd4j.core;

import java.io.IOException;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

public class RrdS3BackendTest {

	private static final String BUCKET = "test-rrd-s3-backend";
	private static final String PATH = "test.rrd";

	private AmazonS3 amazonS3;

	@Before
	public void setup() {
		try {
			amazonS3 = AmazonS3ClientBuilder.standard().build();
		} catch (Exception e) {
			Assume.assumeNoException(
					"RrdS3Backend tests skipped due to missing AmazonS3 env config. "
							+ "To enable these tests, set AWS_REGION, AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY system "
							+ "enviornmental variables or use any of the other configuration options described by "
							+ "http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/AmazonS3ClientBuilder.html#standard--",
					e);
		}

	}

	@After
	public void cleanup() {
		if (amazonS3 != null) {
			try {
				// amazonS3.deleteObject(BUCKET, PATH);
			} catch (Exception e) {
				// suppressed to avoid confusing test failures. If test creating
				// rrd failed, then this will also fail because there is no file
				// to delete in S3.
			}
		}
	}

	@Test
	public void testBuild2() throws IOException {
		RrdS3BackendFactory rrdBackendFactory = new RrdS3BackendFactory(BUCKET);
		RrdDb rrdDb = RrdDbTest.testBuild2(PATH, rrdBackendFactory);
		rrdDb.close(); // flushes data to S3

		// download from s3
		rrdDb = new RrdDb(PATH, rrdBackendFactory);
		RrdDbTest.testRrdDb(rrdDb);

	}

}
