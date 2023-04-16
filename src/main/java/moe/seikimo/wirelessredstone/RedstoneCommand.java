package moe.seikimo.wirelessredstone;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static net.minecraft.commands.Commands.*;

public interface RedstoneCommand {
    String USAGE_MESSAGE = "Usage: /redstone <create|delete> [endpoint|waypoint] [url]";
    String WRONG_PLATFORM = "This command can only be used by players.";
    String NEED_ENDPOINT_MATERIALS = "This requires a redstone block and a quartz block.";
    String NEED_WAYPOINT_MATERIALS = "This requires a redstone lamp.";

    /**
     * Registers the '/redstone' command.
     *
     * @param dispatcher The command dispatcher.
     */
    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                literal("redstone")
                        .then(argument("create", string())
                                .then(argument("type", string())
                                        .then(argument("url", string())
                                                .executes(RedstoneCommand::create))
                                        .executes(RedstoneCommand::create))
                                .executes(RedstoneCommand::create))
                        .then(literal("delete")
                                .executes(RedstoneCommand::delete))
                        .executes(RedstoneCommand::base)
        );
    }

    /**
     * Handler for '/redstone'.
     *
     * @param ctx The command context.
     */
    static int base(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendFailure(Component.literal(RedstoneCommand.USAGE_MESSAGE));

        return 1;
    }

    /**
     * Handler for '/redstone list'.
     *
     * @param ctx The command context.
     */
    static int create(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();

        // Check if the command is being executed by a player.
        if (!source.isPlayer())
            source.sendFailure(Component.literal(RedstoneCommand.WRONG_PLATFORM));
        var player = source.getPlayer(); assert player != null;
        var inventory = player.getInventory();

        // Get the type of redstone component to make.
        var type = getString(ctx, "type");
        // Get the current position of the player.
        var position = player.getOnPos().above();

        switch (type) {
            default -> source.sendFailure(Component.literal(RedstoneCommand.USAGE_MESSAGE));
            case "endpoint" -> {
                // Check if the player has a redstone block and a quartz block.
                if (
                        !inventory.hasAnyMatching(item -> item.getItem() == Items.REDSTONE_BLOCK) ||
                        !inventory.hasAnyMatching(item -> item.getItem() == Items.QUARTZ_BLOCK)
                ) {
                    source.sendFailure(Component.literal(RedstoneCommand.NEED_ENDPOINT_MATERIALS));
                    return 1;
                }

                // Create an endpoint.
                var endpoint = new RedstoneEndpoint(
                        UUID.randomUUID(), player.getUUID(),
                        player.getLevel(), position
                ); endpoint.save(); endpoint.load(player);
                endpoint.place();

                // Remove the materials from the player's inventory.
                inventory.clearOrCountMatchingItems(item -> item.getItem() == Items.REDSTONE_BLOCK, 1, inventory);
                inventory.clearOrCountMatchingItems(item -> item.getItem() == Items.QUARTZ_BLOCK, 1, inventory);

                player.sendSystemMessage(Component.literal("Endpoint created! Use UUID: " + endpoint.getUuid())
                        .setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, endpoint.getUuid().toString()))));
            }
            case "waypoint" -> {
                // Check if the player has a redstone lamp.
                if (!inventory.hasAnyMatching(item -> item.getItem() == Items.REDSTONE_LAMP)) {
                    source.sendFailure(Component.literal(RedstoneCommand.NEED_WAYPOINT_MATERIALS));
                    return 1;
                }

                // Get the waypoint URL.
                var endpoint = getString(ctx, "url");
                if (endpoint == null) {
                    source.sendFailure(Component.literal(RedstoneCommand.USAGE_MESSAGE));
                    return 1;
                }

                // Create a waypoint.
                var waypoint = new RedstoneWaypoint(
                        UUID.randomUUID(), player.getUUID(),
                        endpoint, player.getLevel(), position
                ); waypoint.save(); waypoint.place();

                // Remove the materials from the player's inventory.
                inventory.clearOrCountMatchingItems(item -> item.getItem() == Items.REDSTONE_LAMP, 1, inventory);

                player.sendSystemMessage(Component.literal("Waypoint created! Use UUID: " + waypoint.getUuid())
                        .setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, waypoint.getUuid().toString()))));
            }
        }

        return 1;
    }

    /**
     * Handler for '/redstone delete'.
     *
     * @param ctx The command context.
     */
    static int delete(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();

        // Check if the command is being executed by a player.
        if (!source.isPlayer())
            source.sendFailure(Component.literal(RedstoneCommand.WRONG_PLATFORM));
        var player = source.getPlayer(); assert player != null;

        // Get the block below the player.
        var position = player.getOnPos();

        // Check if there is a redstone component at the player's feet.
        RedstoneComponent component = WirelessRedstone.getEndpoints().get(position);
        if (component == null) component = WirelessRedstone.getWaypoints().get(position);
        if (component == null) {
            source.sendFailure(Component.literal("There is no redstone component here."));
            return 1;
        }

        // Check if the player is the owner of the component.
        if (!component.isOwner(player.getUUID())) {
            source.sendFailure(Component.literal("You are not the owner of this component."));
            return 1;
        }

        // Delete the component.
        component.destroy();

        player.sendSystemMessage(Component.literal("Component deleted!"));
        return 1;
    }
}
