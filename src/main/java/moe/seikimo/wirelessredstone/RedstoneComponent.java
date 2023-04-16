package moe.seikimo.wirelessredstone;

import java.util.UUID;

public interface RedstoneComponent {
    /**
     * Saves the component.
     */
    void save();

    /**
     * Deletes the component.
     */
    void destroy();

    /**
     * Checks if the component is owned by the player.
     *
     * @param uuid The player's UUID.
     * @return Whether the component is owned by the player.
     */
    boolean isOwner(UUID uuid);
}
