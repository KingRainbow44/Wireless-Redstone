package moe.seikimo.wirelessredstone;

import io.javalin.http.Context;

import java.util.UUID;

/**
 * Handles routing for redstone endpoints.
 */
public interface RedstoneRouting {
    /**
     * Handles the index route.
     *
     * @route GET /
     * @param ctx The context.
     */
    static void indexRoute(Context ctx) {
        ctx.redirect("https://seikimo.moe/");
    }

    /**
     * Handles the toggle route.
     *
     * @route GET /{id}
     * @param ctx The context.
     */
    static void toggleRoute(Context ctx) {
        try {
            // Get the UUID.
            var id = ctx.pathParam("id");
            var uuid = UUID.fromString(id);

            // Get the endpoint.
            var endpoint = WirelessRedstone.getUuidEndpoints().get(uuid);
            if (endpoint == null) {
                ctx.status(404).result("Endpoint not loaded.");
                return;
            }

            // Toggle the state.
            ctx.result(Boolean.toString(endpoint.toggleState()));
        } catch (IllegalArgumentException ignored) {
            ctx.status(404).result("Invalid UUID.");
        } catch (RuntimeException ignored) {
            ctx.status(400).result("Endpoint not loaded.");
        }
    }
}
