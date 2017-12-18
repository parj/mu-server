package ronin.muserver;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.After;
import org.junit.Test;
import scaffolding.ClientUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ronin.muserver.MuServerBuilder.muServer;

public class StreamingTest {

	private final OkHttpClient client = new OkHttpClient();
	private MuServer server;

	@Test
	public void theOutputStreamBuffersBasedOnSizeAskedFor() throws InterruptedException, IOException {
		CountDownLatch latch = new CountDownLatch(1);
		CountDownLatch latch2 = new CountDownLatch(1);
		server = muServer()
				.addHandler((request, response) -> {
					try (OutputStream outputStream = response.outputStream(4)) {
						outputStream.write(new byte[]{1, 2, 3, 4});
						latch.await();
						outputStream.write(new byte[]{5, 6});
						outputStream.write(new byte[]{7});
						latch2.await();
						outputStream.flush();
					}
					return true;
				}).start();

		Response resp = client.newCall(new Request.Builder()
				.url(server.url())
				.build()).execute();

		try (InputStream stream = resp.body().byteStream()) {
			latch.countDown();
			byte[] buffer = new byte[5];
			int read = stream.read(buffer);
			assertThat(read, is(4));
			assertThat(buffer, equalTo(new byte[]{1, 2, 3, 4, 0}));

			int available = stream.available();
			assertThat(available, is(0));
			latch2.countDown();

			buffer = new byte[5];
			read = stream.read(buffer);
			assertThat(read, is(3));
			assertThat(buffer, equalTo(new byte[]{5, 6, 7, 0, 0}));
		}

	}

	@Test
	public void textCanBeWrittenWithThePrintWriter() throws Exception {
		server = muServer()
				.addHandler((request, response) -> {
					try (PrintWriter writer = response.writer()) {
						writer.println("Hello, world");
						writer.print("What's happening?");
					}
					return true;
				}).start();

		Response resp = client.newCall(new Request.Builder()
				.url(server.url())
				.build()).execute();

		String actual = resp.body().string();
		assertThat(actual, equalTo(String.format("Hello, world%nWhat's happening?")));
	}

	@Test
	public void requestDataCanBeReadFromTheInputStream() throws Exception {
		server = muServer()
				.addHandler((request, response) -> {
					try (InputStream in = request.inputStream();
					     OutputStream out = response.outputStream()) {
						byte[] buffer = new byte[128];
						int read;
						while ((read = in.read(buffer)) > -1) {
							System.out.println("Read " + read + " - " + new String(buffer, 0, read, UTF_8).replace("\n", " "));
							Thread.sleep(10);
							out.write(buffer, 0, read);
						}
						out.flush();
					}
					return true;
				}).start();

		StringBuffer sentData = new StringBuffer();
		Response resp = client.newCall(new Request.Builder()
				.post(ClientUtils.largeRequestBody(sentData))
				.url(server.url())
				.build()).execute();

		String actual = new String(resp.body().bytes(), UTF_8);
		assertThat(actual, equalTo(sentData.toString()));
	}

	@After
	public void stopIt() throws InterruptedException {
		if (server != null) {
			server.stop();
		}
	}
}
