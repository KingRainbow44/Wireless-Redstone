package moe.seikimo.wirelessredstone;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Representation of a redstone endpoint.
 * Contains both the UUID of the point and the position.
 */
@RequiredArgsConstructor
public final class RedstoneEndpoint implements RedstoneComponent {
    /**
     * Parses a string into a RedstoneEndpoint.
     *
     * @param endpoint The string to parse.
     * @return The parsed RedstoneEndpoint.
     */
    public static RedstoneEndpoint of(String endpoint) {
        return new RedstoneEndpoint(
                UUID.fromString(endpoint.split(",")[0]),
                UUID.fromString(endpoint.split(",")[1]),
                Serialize.parseLevel(endpoint.split(",")[2]),
                Serialize.parsePosition(endpoint, 3)
        );
    }

    @Getter private final UUID uuid;
    @Getter private final UUID owner;
    @Getter private final Level world;
    @Getter private final BlockPos position;

    @Getter private boolean enabled = false;
    @Getter private Player player = null;

    /**
     * Invoked once when the endpoint is loaded.
     */
    public void load(Player owner) {
        // Set the owner.
        this.player = owner;

        // Check the current state of the block.
        this.enabled = this.world.getBlockState(this.position)
                .getBlock() == Blocks.REDSTONE_BLOCK;
    }

    /**
     * Invoked once when the endpoint is unloaded.
     */
    public void unload() {
        // Set the owner to null.
        this.player = null;

        // Set the state to false.
        this.enabled = false;

        // Remove the block.
        this.world.setBlockAndUpdate(this.position, Blocks.AIR.defaultBlockState());
    }

    /**
     * Toggles the state of the endpoint.
     *
     * @return True if enabled, false if disabled.
     */
    public boolean toggleState() {
        // Check if the endpoint is loaded.
        if (this.player == null)
            throw new RuntimeException("Endpoint is not loaded.");

        // Toggle the state.
        this.enabled = !this.enabled;

        // Set the block state.
        this.world.setBlockAndUpdate(this.position,
                this.enabled ? Blocks.REDSTONE_BLOCK.defaultBlockState()
                        : Blocks.QUARTZ_BLOCK.defaultBlockState());

        return this.enabled;
    }

    /**
     * Saves the endpoint.
     */
    @SneakyThrows
    public void save() {
        // Add the endpoint to the list.
        WirelessRedstone.getEndpoints().put(this.getPosition(), this);
        WirelessRedstone.getUuidEndpoints().put(this.getUuid(), this);
        // Save the endpoint to a file.
        var file = new File(WirelessRedstone.getEndpointsDirectory(), this.uuid.toString());
        Files.writeString(file.toPath(), this.toString());
    }

    /**
     * Destroys the endpoint.
     */
    public void destroy() {
        // Set the block state.
        this.world.setBlockAndUpdate(this.position,
                Blocks.AIR.defaultBlockState());

        // Drop a redstone block and a quartz block at the position.
        var redstone = new ItemEntity(this.world, this.position.getX(),
                this.position.getY(), this.position.getZ(),
                Blocks.REDSTONE_BLOCK.asItem().getDefaultInstance());
        this.world.addFreshEntity(redstone);

        var quartz = new ItemEntity(this.world, this.position.getX(),
                this.position.getY(), this.position.getZ(),
                Blocks.QUARTZ_BLOCK.asItem().getDefaultInstance());
        this.world.addFreshEntity(quartz);

        // Remove the endpoint from the list.
        WirelessRedstone.getEndpoints().remove(this.getPosition());
        WirelessRedstone.getUuidEndpoints().remove(this.getUuid());
        // Remove the endpoint file.
        var file = new File(WirelessRedstone.getEndpointsDirectory(), this.uuid.toString());
        if (file.exists() && !file.delete())
            throw new RuntimeException("Failed to delete endpoint file.");

        // Send a message to the owner.
        this.getPlayer().sendSystemMessage(Component.literal("Your endpoint at " +
                this.position.getX() + ", " + this.position.getZ() + " was destroyed."));
    }

    /**
     * Places this waypoint.
     */
    public void place() {
        // Set the block state.
        this.world.setBlockAndUpdate(this.position,
                Blocks.QUARTZ_BLOCK.defaultBlockState());
    }

    /**
     * Checks if the endpoint is owned by the player.
     *
     * @param uuid The player's UUID.
     * @return Whether the endpoint is owned by the player.
     */
    public boolean isOwner(UUID uuid) {
        return this.owner.equals(uuid);
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
                + position;
    }
}
