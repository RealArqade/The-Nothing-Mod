package com.realarqade.thenothingmod;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Works out whether a player is looking at something. */
public final class Watching {
    private Watching() {
    }

    /**
     * Whether any player within {@code range} blocks has {@code self} in view: roughly centred on their screen and with a clear
     * line of sight.
     *
     * @param cone how central it must be: 1.0 is dead centre, 0.0 is 90 degrees off to the side
     */
    public static boolean isWatched(Entity self, double range, double cone) {
        double rangeSqr = range * range;
        for (Player player : self.level().players()) {
            if (player.isSpectator() || self.distanceToSqr(player) > rangeSqr) {
                continue;
            }
            Vec3 look = player.getViewVector(1.0F).normalize();
            Vec3 toSelf = new Vec3(self.getX() - player.getX(), self.getEyeY() - player.getEyeY(), self.getZ() - player.getZ());
            double distance = toSelf.length();
            if (distance < 1.0E-4) {
                return true;
            }
            if (look.dot(toSelf.scale(1.0 / distance)) > cone && player.hasLineOfSight(self)) {
                return true;
            }
        }
        return false;
    }
}
