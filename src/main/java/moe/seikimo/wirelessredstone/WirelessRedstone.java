package moe.seikimo.wirelessredstone;

import com.mojang.brigadier.CommandDispatcher;
import io.javalin.Javalin;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WirelessRedstone implements DedicatedServerModInitializer {
    @Getter private static final Logger logger
            = LoggerFactory.getLogger("Wireless Redstone");
    @Getter private static final Javalin javalin
            = Javalin.create();
    @Getter private static final OkHttpClient httpClient
            = new OkHttpClient();

    @Getter private static final File dataDirectory
            = new File("redstone");
    @Getter private static final File endpointsDirectory
            = new File(dataDirectory, "endpoints");
    @Getter private static final File waypointsDirectory
            = new File(dataDirectory, "waypoints");

    @Getter private static final Map<BlockPos, RedstoneEndpoint> endpoints
            = new HashMap<>();
    @Getter private static final Map<BlockPos, RedstoneWaypoint> waypoints
            = new HashMap<>();
    @Getter private static final Map<UUID, RedstoneEndpoint> uuidEndpoints
            = new HashMap<>();

    @Getter private static WirelessRedstone instance;
    @Getter @Setter private static MinecraftServer server;

    /**
     * Invoked when the mod is loaded with the server.
     */
    @Override public void onInitializeServer() {
        WirelessRedstone.instance = this;

        // Check if the data directory exists.
        if (!dataDirectory.exists() && !dataDirectory.mkdir())
            throw new RuntimeException("Failed to create data directory.");
        // Make the endpoint and waypoint directories.
        if (!endpointsDirectory.exists() && !endpointsDirectory.mkdir())
            throw new RuntimeException("Failed to create endpoints directory.");
        if (!waypointsDirectory.exists() && !waypointsDirectory.mkdir())
            throw new RuntimeException("Failed to create waypoints directory.");

        // Add a listener for registering commands.
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) ->
                this.registerCommands(dispatcher));
        // Add a listener for new player joins.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                this.onPlayerJoin(handler.getPlayer()));
        // Add a listener for player disconnects.
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                this.onPlayerDisconnect(handler.getPlayer()));
    }

    /**
     * Invoked when the server has completed startup.
     */
    public void onServerStart() {
        // Load all endpoints & waypoints.
        new Thread(() -> {
            this.loadEndpoints();
            this.loadWaypoints();
        }).start();

        // Apply HTTP server routes.
        this.applyRoutes();
        // Start the HTTP server.
        WirelessRedstone.getJavalin().start(25564);
    }

    /**
     * Invoked after the server has finished stopping.
     */
    public void onServerStop() {
        // Stop the HTTP server.
        WirelessRedstone.getJavalin().close();
    }

    /**
     * Applies routes to the Javalin instance.
     */
    private void applyRoutes() {
        var app = WirelessRedstone.getJavalin();

        app.get("/", RedstoneRouting::indexRoute);
        app.get("/{id}", RedstoneRouting::toggleRoute);
    }

    /**
     * Loads all redstone endpoints on the disk.
     */
    private void loadEndpoints() {
        var files = WirelessRedstone.getEndpointsDirectory().listFiles();
        if (files == null) throw new RuntimeException("Failed to list files.");

        for (var file : files) {
            try {
                var endpoint = RedstoneEndpoint.of(Files.readString(file.toPath()));
                WirelessRedstone.getEndpoints().put(endpoint.getPosition(), endpoint);
                WirelessRedstone.getUuidEndpoints().put(endpoint.getUuid(), endpoint);
            } catch (IOException ignored) {
                WirelessRedstone.getLogger().error("Failed to load endpoint '{}'.", file.getName());
            }
        }
    }

    /**
     * Loads all redstone endpoints on the disk.
     */
    private void loadWaypoints() {
        var files = WirelessRedstone.getWaypointsDirectory().listFiles();
        if (files == null) throw new RuntimeException("Failed to list files.");

        for (var file : files) {
            try {
                var waypoint = RedstoneWaypoint.of(Files.readString(file.toPath()));
                WirelessRedstone.getWaypoints().put(waypoint.getPosition(), waypoint);
            } catch (IOException ignored) {
                WirelessRedstone.getLogger().error("Failed to load waypoint '{}'.", file.getName());
            }
        }
    }

    /**
     * Registers all available commands.
     *
     * @param dispatcher The command dispatcher.
     */
    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        RedstoneCommand.register(dispatcher);
    }

    /**
     * Invoked when a player joins the server.
     *
     * @param player The player.
     */
    private void onPlayerJoin(Player player) {
        // Load all endpoints for the player.
        WirelessRedstone.getEndpoints().values().stream()
                .filter(endpoint -> endpoint.getOwner().equals(player.getUUID()))
                .forEach(endpoint -> endpoint.load(player));
    }

    /**
     * Invoked when a player disconnects from the server.
     *
     * @param player The player.
     */
    private void onPlayerDisconnect(Player player) {
        // Unload all endpoints for the player.
        WirelessRedstone.getEndpoints().values().stream()
                .filter(endpoint -> endpoint.getOwner().equals(player.getUUID()))
                .forEach(RedstoneEndpoint::unload);
    }
}
