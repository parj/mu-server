package ronin.muserver.rest;

import okhttp3.Request;
import okhttp3.Response;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;
import ronin.muserver.MuServer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ronin.muserver.MuServerBuilder.httpsServer;
import static scaffolding.ClientUtils.call;
import static scaffolding.ClientUtils.request;

public class MediaTypesTest {

    private MuServer server;

    @Test
    public void blah() throws IOException {

        @Path("pictures")
        class Widget {

            @GET
            @Produces({"image/jpeg, image/gif ", " image/png"})
            public String all() {
                return ";)";
            }

            @GET
            @Produces("application/json")
            public String json() {
                return "[]";
            }
        }

        muServer(new Widget());

        assertSelected("/pictures", asList("image/gif"), ";)");
        assertSelected("/pictures", asList("image/jpeg"), ";)");
        assertSelected("/pictures", asList("image/png"), ";)");
        assertSelected("/pictures", asList("image/png", "application/text"), ";)");
        assertNotSelected("/pictures", asList("text/plain"));
        assertSelected("/pictures", asList("application/json"), "[]");

    }

    private void assertSelected(String path, List<String> accept, String expectedBody) throws IOException {
        Request.Builder rb = request()
            .url(server.uri().resolve(path).toString());
        for (String s : accept) {
            rb.addHeader("Accept", s);
        }
        try (Response resp = call(rb)) {
            assertThat(resp.code(), Matchers.is(200));
            assertThat(resp.body().string(), equalTo(expectedBody));
        }
    }

    private void assertNotSelected(String path, List<String> accept) {
        Request.Builder rb = request()
            .url(server.uri().resolve(path).toString());
        for (String s : accept) {
            rb.addHeader("Accept", s);
        }
        try (Response resp = call(rb)) {
            assertThat(resp.code(), Matchers.is(406));
        }
    }


    private void muServer(Object... resources) {
        this.server = httpsServer()
            .addHandler(new RestHandler(resources))
            .start();
    }

    @After
    public void stop() {
        server.stop();
    }

}