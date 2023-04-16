package moe.seikimo.wirelessredstone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import okhttp3.Request;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

@RequiredArgsConstructor
public final class RedstoneWaypoint implements RedstoneComponent {
    /**
     * Parses a string into a RedstoneWaypoint.
     *
     * @param waypoint The string to parse.
     * @return The parsed RedstoneWaypoint.
     */
    public static RedstoneWaypoint of(String waypoint) {
        return new RedstoneWaypoint(
                UUID.fromString(waypoint.split(",")[0]),
                UUID.fromString(waypoint.split(",")[1]),
                waypoint.split(",")[2],
                Serialize.parseLevel(waypoint.split(",")[3]),
                Serialize.parsePosition(waypoint, 4)
        );
    }

    @Getter private final UUID uuid;
    @Getter private final UUID owner;
    @Getter private final String endpoint;
    @Getter private final Level world;
    @Getter private final BlockPos position;

    /**
     * Invokes this waypoint.
     */
    public void invoke() {
        new Thread(this::invoke$1).start();
    }

    /**
     * Invokes this waypoint.
     */
    private void invoke$1() {
        try {
            var request = new Request.Builder()
                    .url(this.endpoint).build();
            var response = WirelessRedstone.getHttpClient()
                    .newCall(request).execute();
            response.close();
        } catch (IllegalArgumentException | IOException ignored) { }
    }

    /**
     * Saves the waypoint.
     */
    @SneakyThrows
    public void save() {
        // Add the waypoint to the list.
        WirelessRedstone.getWaypoints().put(this.getPosition(), this);
        // Save the endpoint file.
        var file = new File(WirelessRedstone.getWaypointsDirectory(), this.uuid.toString());
        Files.writeString(file.toPath(), this.toString());
    }

    /**
     * Destroys this waypoint.
     */
    public void destroy() {
        // Set the block state.
        this.world.setBlockAndUpdate(this.position,
                Blocks.AIR.defaultBlockState());

        // Drop a redstone lamp block at the position.
        var redstoneLamp = new ItemEntity(this.world, this.position.getX(),
                this.position.getY(), this.position.getZ(),
                Blocks.REDSTONE_LAMP.asItem().getDefaultInstance());
        this.world.addFreshEntity(redstoneLamp);

        // Remove the waypoint from the list.
        WirelessRedstone.getWaypoints().remove(this.getPosition());
        // Remove the endpoint file.
        var file = new File(WirelessRedstone.getWaypointsDirectory(), this.uuid.toString());
        if (file.exists() && !file.delete())
            throw new RuntimeException("Failed to delete file: " + file.getPath());
    }

    /**
     * Places this waypoint.
     */
    public void place() {
        // Set the block state.
        this.world.setBlockAndUpdate(this.position,
                Blocks.REDSTONE_LAMP.defaultBlockState());
    }

    /**
     * Checks if the waypoint is owned by the player.
     *
     * @param uuid The player's UUID.
     * @return Whether the waypoint is owned by the player.
     */
    public boolean isOwner(UUID uuid) {
        return this.getOwner().equals(uuid);
    }

    @Override
    public String toString() {
        var dimension = this.world.dimension();
        var worldLocation = dimension.registry() + ":" + dimension.location();
        var position = "(" + this.position.getX() + ","
                + this.position.getY() + ","
                + this.position.getZ() + ")";

        return this.uuid.toString() + ","
                + this.owner.toString() + ","
                + worldLocation + ","
                + position + ","
                + this.endpoint;
    }
}
