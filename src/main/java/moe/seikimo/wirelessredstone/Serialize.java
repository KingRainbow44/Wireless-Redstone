package moe.seikimo.wirelessredstone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public interface Serialize {
    /**
     * Parses a string into a Vec3.
     *
     * @param position The string to parse.
     * @param offset The offset of the Vec3.
     * @return The parsed Vec3.
     * @throws NumberFormatException If the string is not a valid Vec3.
     */
    static BlockPos parsePosition(String position, int offset)
            throws NumberFormatException {
        return new BlockPos(
                Integer.parseInt(position.split(",")[offset]
                        .replace("(", "")),
                Integer.parseInt(position.split(",")[offset + 1]),
                Integer.parseInt(position.split(",")[offset + 2]
                        .replace(")", ""))
        );
    }

    /**
     * Parses a level from a string.
     *
     * @param level The string to parse.
     * @return The parsed level.
     */
    static Level parseLevel(String level) {
        return WirelessRedstone.getServer().getLevel(
                ResourceKey.create(Registries.DIMENSION, new ResourceLocation(
                        level.split(":")[0],
                        level.split(":")[1]
                ))
        );
    }
}
