package ronin.muserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class SyncHandlerAdapter implements AsyncMuHandler {

	private final List<MuHandler> muHandlers;
	private final ExecutorService executor = Executors.newCachedThreadPool();

	public SyncHandlerAdapter(List<MuHandler> muHandlers) {
		this.muHandlers = muHandlers;
	}


	public boolean onHeaders(AsyncContext ctx, Headers headers) throws Exception {
		executor.submit(() -> {
			try {

				boolean handled = false;
				for (MuHandler muHandler : muHandlers) {
					handled = muHandler.handle(ctx.request, ctx.response);
					if (handled) {
						break;
					}
				}
				if (!handled) {
					ctx.response.status(404);
				}

			} catch (Exception ex) {
				System.out.println("Error from handler: " + ex.getMessage());
				ex.printStackTrace();
			} finally {
				ctx.complete();
			}
		});
		return true;
	}

	public void onRequestData(AsyncContext ctx, ByteBuffer buffer) throws Exception {
		GrowableByteBufferInputStream state = (GrowableByteBufferInputStream)ctx.state;
		if (state == null) {
			state = new GrowableByteBufferInputStream();
			((NettyRequestAdapter) ctx.request).inputStream(state);
			ctx.state = state;
		}
		state.handOff(buffer);
	}

	public void onRequestComplete(AsyncContext ctx) {
		try {
			GrowableByteBufferInputStream state = (GrowableByteBufferInputStream)ctx.state;
			if (state != null) {
				state.close();
			}
		} catch (IOException e) {
			System.out.println("This can't happen");
		}
	}

}