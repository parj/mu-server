package io.muserver.rest;

import io.muserver.MuException;
import io.muserver.Mutils;

import javax.ws.rs.core.*;
import javax.ws.rs.ext.RuntimeDelegate;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>The JAX-RS runtime delegate for mu-server.</p>
 * <p>In most cases this class should not be used, however in cases where you want to test JaxRS classes outside of
 * mu-server you may need to make sure a JAX-RS RuntimeDelegate is set, in which case you can call {@link #ensureSet()}.</p>
 */
public class MuRuntimeDelegate extends RuntimeDelegate {

    private static MuRuntimeDelegate singleton;

    /**
     * Registers the mu RuntimeDelegate with jax-rs, if it was not already.
     * @return Returns the runtime delegate.
     */
    public static synchronized RuntimeDelegate ensureSet() {
        if (singleton == null) {
            singleton = new MuRuntimeDelegate();
            RuntimeDelegate.setInstance(singleton);
        }
        return singleton;
    }

    private final Map<Class<?>, HeaderDelegate> headerDelegates = new HashMap<>();

    private MuRuntimeDelegate() {
        headerDelegates.put(MediaType.class, new MediaTypeHeaderDelegate());
        headerDelegates.put(CacheControl.class, new CacheControlHeaderDelegate());
        headerDelegates.put(NewCookie.class, new NewCookieHeaderDelegate());
        headerDelegates.put(Cookie.class, new CookieHeaderDelegate());
        headerDelegates.put(EntityTag.class, new EntityTagDelegate());
        headerDelegates.put(Link.class, new LinkHeaderDelegate());
    }

    /**
     * @param broadcaster An MuServer SSE broadcaster
     * @return the number of SSE clients currently connected to the broadcaster
     */
    public static int connectedSinksCount(SseBroadcaster broadcaster) {
        Mutils.notNull("broadcaster", broadcaster);
        if (broadcaster instanceof SseBroadcasterImpl) {
            return ((SseBroadcasterImpl)broadcaster).connectedSinksCount();
        } else {
            throw new IllegalArgumentException("The given broadcaster was not created by MuServer. It was of type " + broadcaster.getClass());
        }
    }

    @Override
    public UriBuilder createUriBuilder() {
        return new MuUriBuilder();
    }

    @Override
    public Response.ResponseBuilder createResponseBuilder() {
        return new JaxRSResponse.Builder();
    }

    @Override
    public Variant.VariantListBuilder createVariantListBuilder() {
        throw NotImplementedException.notYet();
    }

    @Override
    public <T> T createEndpoint(Application application, Class<T> endpointType) throws IllegalArgumentException, UnsupportedOperationException {
        throw new MuException("MuServer does not support instantiation of application classes");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> HeaderDelegate<T> createHeaderDelegate(Class<T> type) throws IllegalArgumentException {
        HeaderDelegate headerDelegate = headerDelegates.get(type);
        if (headerDelegate != null) {
            return (HeaderDelegate<T>) headerDelegate;
        }
        throw new MuException("MuServer does not support converting " + type.getName());
    }

    @Override
    public Link.Builder createLinkBuilder() {
        return new LinkHeaderDelegate.MuLinkBuilder();
    }

    /**
     * <p>Creates a new SSE interface that can create SSE Events and Broadcasters.</p>
     * <p>This can be useful if you want to have a singleton broadcaster in your class that you can declare as a final field.</p>
     * @return A broadcaster that can be used to publish SSE events to clients.
     */
    public static Sse createSseFactory() {
        return new JaxSseImpl();
    }
}
