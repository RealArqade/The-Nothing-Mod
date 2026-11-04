package com.realarqade.thenothingmod;

import com.realarqade.thenothingmod.platform.Platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

/**
 * What the ten void dimensions (v01 to v10) are made of. Their world generator makes nothing; this fills each new chunk with a
 * repeating, quiet, built-looking place instead: rooms and corridors with no one in them, an empty plaza of pillars, a page with ruled
 * lines, aisles of Name 0047, a field of stepped pyramids. Every style is local to one chunk (neighbouring chunks agree on
 * where the doorways are), so it goes on for ever in every direction, and some of them have gaps so you can fall out again.
 * <p>
 * A generated chunk is marked with a bedrock block at the very bottom, so it is only ever generated once (also across restarts).
 * Landmarks that belong to one dimension are placed on top of this by {@link StoryStructures}.
 */
public final class VoidWorlds {
    /** The height of the floor in the void dimensions. */
    public static final int FLOOR = 64;

    private record Style(String kind, String floor, String wall, String accent) {
    }

    private static final Map<String, Style> STYLES = Map.of(
            "v01", new Style("islands", "id_0119", "id_0297", "id_0304"),
            "v02", new Style("plaza", "id_0447", "id_0488", "id_0484"),
            "v03", new Style("page", "id_0366", "id_0469", "id_0467"),
            "v04", new Style("rooms", "id_0429", "id_0089", "id_0464"),
            "v05", new Style("scatter", "id_0518", "id_0460", "id_0154"),
            "v06", new Style("aisles", "id_0229", "id_0160", "id_0304"),
            "v07", new Style("cells", "id_0248", "id_0532", "id_0464"),
            "v08", new Style("erased", "id_0575", "id_0321", "id_0484"),
            "v09", new Style("Word0940", "id_0119", "id_0093", "id_0323"),
            "v10", new Style("steps", "id_0131", "id_0119", "id_0289"));

    private static final Queue<Pending> queue = new ArrayDeque<>();

    private record Pending(ServerLevel level, ChunkPos chunk) {
    }

    private VoidWorlds() {
    }

    public static void register() {
        Platform.get().onChunkGenerate((level, chunk) -> {
            if (isVoid(level)) {
                queue.add(new Pending(level, chunk.getPos()));
            }
        });
        // A few per tick, on the main thread, a moment after the chunk exists.
        Platform.get().onServerTickEnd(server -> {
            for (int i = 0; i < 6; i++) {
                Pending next = queue.poll();
                if (next == null) {
                    return;
                }
                if (next.level().hasChunk(Compat.cx(next.chunk()), Compat.cz(next.chunk()))) {
                    ensure(next.level(), Compat.cx(next.chunk()), Compat.cz(next.chunk()));
                }
            }
        });
        Platform.get().onServerStopped(server -> queue.clear());
    }

    public static boolean isVoid(ServerLevel level) {
        String id = Dimensions.idOf(level);
        return id != null && id.startsWith("v");
    }

    /** Fills the chunk once. Safe to call as often as you like. */
    public static void ensure(ServerLevel level, int cx, int cz) {
        Style style = STYLES.get(Dimensions.idOf(level));
        if (style == null) {
            return;
        }
        BlockPos marker = new BlockPos(cx * 16, level.getMinY(), cz * 16);
        level.getChunk(cx, cz);
        if (level.getBlockState(marker).is(Blocks.BEDROCK)) {
            return;
        }
        level.setBlock(marker, Blocks.BEDROCK.defaultBlockState(), 2);
        Random r = new Random(Dimensions.idOf(level).hashCode() * 31L + cx * 341873128712L + cz * 132897987541L);
        Gen g = new Gen(level, cx * 16, cz * 16, style, r);
        switch (style.kind()) {
            case "islands" -> g.islands();
            case "plaza" -> g.plaza();
            case "page" -> g.page();
            case "rooms" -> g.rooms(5, 3, 3);
            case "cells" -> g.rooms(3, 2, 3);
            case "scatter" -> g.scatter();
            case "aisles" -> g.aisles();
            case "erased" -> g.erased();
            case "Word0940" -> g.draft();
            case "steps" -> g.steps();
            default -> g.plaza();
        }
    }

    private static Block block(String name) {
        return BuiltInRegistries.BLOCK.getValue(TheNothingMod.id(name));
    }

    private static final class Gen {
        final ServerLevel level;
        final int x0;
        final int z0;
        final Style style;
        final Random r;
        final BlockState floor;
        final BlockState wall;
        final BlockState accent;

        Gen(ServerLevel level, int x0, int z0, Style style, Random r) {
            this.level = level;
            this.x0 = x0;
            this.z0 = z0;
            this.style = style;
            this.r = r;
            this.floor = block(style.floor()).defaultBlockState();
            this.wall = block(style.wall()).defaultBlockState();
            this.accent = block(style.accent()).defaultBlockState();
        }

        void set(int lx, int y, int lz, BlockState state) {
            level.setBlock(new BlockPos(x0 + lx, y, z0 + lz), state, 2);
        }

        void floorAll() {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    set(x, FLOOR, z, floor);
                }
            }
        }

        /** Whether the doorway on a chunk edge is open. Both chunks sharing the edge work out the same answer. */
        boolean open(int ax, int az, int bx, int bz) {
            long h = (Math.min(ax * 73856093L ^ az * 19349663L, bx * 73856093L ^ bz * 19349663L) * 83492791L)
                    ^ (Math.max(ax * 73856093L ^ az * 19349663L, bx * 73856093L ^ bz * 19349663L) * 2654435761L);
            return Math.floorMod(h ^ Dimensions.idOf(level).hashCode(), 10) < 7;
        }

        /** Empty rooms: one per chunk, doorways in the walls, a light in the ceiling. */
        void rooms(int height, int doorHeight, int doorWidth) {
            int cx = x0 >> 4;
            int cz = z0 >> 4;
            boolean north = open(cx, cz, cx, cz - 1);
            boolean south = open(cx, cz, cx, cz + 1);
            boolean west = open(cx, cz, cx - 1, cz);
            boolean east = open(cx, cz, cx + 1, cz);
            int lo = 8 - doorWidth / 2 - 1;
            int hi = lo + doorWidth;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    set(x, FLOOR, z, floor);
                    set(x, FLOOR + height + 1, z, wall);
                    boolean edge = x == 0 || x == 15 || z == 0 || z == 15;
                    if (edge) {
                        boolean door = (z == 0 && north && x > lo && x <= hi) || (z == 15 && south && x > lo && x <= hi)
                                || (x == 0 && west && z > lo && z <= hi) || (x == 15 && east && z > lo && z <= hi);
                        for (int y = 1; y <= height; y++) {
                            set(x, FLOOR + y, z, door && y <= doorHeight ? Blocks.AIR.defaultBlockState() : wall);
                        }
                    }
                }
            }
            // The light: flush in the ceiling, middle of the room.
            for (int x = 7; x <= 8; x++) {
                for (int z = 7; z <= 8; z++) {
                    set(x, FLOOR + height + 1, z, accent);
                }
            }
        }

        /** Floating discs of floor at different heights, with nothing between them. */
        void islands() {
            int count = 1 + r.nextInt(3);
            for (int i = 0; i < count; i++) {
                int cx = 3 + r.nextInt(10);
                int cz = 3 + r.nextInt(10);
                int radius = 2 + r.nextInt(4);
                int y = FLOOR - 10 + r.nextInt(22);
                for (int layer = 0; layer < 4; layer++) {
                    int rr = radius - layer;
                    if (rr < 0) {
                        break;
                    }
                    for (int x = -rr; x <= rr; x++) {
                        for (int z = -rr; z <= rr; z++) {
                            if (x * x + z * z <= rr * rr + 1 && cx + x >= 0 && cx + x < 16 && cz + z >= 0 && cz + z < 16) {
                                set(cx + x, y - layer, cz + z, layer == 0 ? floor : wall);
                            }
                        }
                    }
                }
                if (r.nextInt(3) == 0) {
                    set(cx, y + 1, cz, accent);
                }
            }
            // The arrival Word0827 lives in chunk 0,0 (see Dimensions); the chunk it is in gets a bigger disc.
            if (x0 == 0 && z0 == 0) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        if ((x - 7.5) * (x - 7.5) + (z - 7.5) * (z - 7.5) < 36) {
                            set(x, FLOOR, z, floor);
                        }
                    }
                }
            }
        }

        /** An empty plaza of pillars, no ceiling, no walls. */
        void plaza() {
            floorAll();
            for (int x = 4; x < 16; x += 8) {
                for (int z = 4; z < 16; z += 8) {
                    for (int y = 1; y <= 10; y++) {
                        set(x, FLOOR + y, z, wall);
                    }
                    set(x, FLOOR + 11, z, accent);
                }
            }
        }

        /** A page: ruled lines across the floor and the odd tower of index cards. */
        void page() {
            floorAll();
            for (int z = 0; z < 16; z += 4) {
                for (int x = 0; x < 16; x++) {
                    set(x, FLOOR, z, wall);
                }
            }
            if (r.nextInt(3) == 0) {
                int h = 6 + r.nextInt(9);
                for (int y = 1; y <= h; y++) {
                    for (int x = 7; x <= 8; x++) {
                        for (int z = 7; z <= 8; z++) {
                            set(x, FLOOR + y, z, accent);
                        }
                    }
                }
            }
        }

        /** Not much: a few single pillars and short stairs hanging in the dark. No floor. */
        void scatter() {
            int count = 3 + r.nextInt(5);
            for (int i = 0; i < count; i++) {
                int x = r.nextInt(16);
                int z = r.nextInt(16);
                int y = FLOOR - 6 + r.nextInt(14);
                int h = 1 + r.nextInt(5);
                for (int k = 0; k < h; k++) {
                    set(x, y + k, z, k == h - 1 && r.nextInt(3) == 0 ? accent : floor);
                }
            }
            if (r.nextInt(2) == 0) {
                int x = 2 + r.nextInt(8);
                int z = 2 + r.nextInt(12);
                int y = FLOOR - 4 + r.nextInt(8);
                for (int k = 0; k < 5; k++) {
                    set(x + k, y + k, z, wall);
                }
            }
        }

        /** Long rows of Name 0047, tall, with aisles between them and no ceiling. */
        void aisles() {
            floorAll();
            for (int z : new int[]{3, 4, 11, 12}) {
                for (int x = 0; x < 16; x++) {
                    if (x == 7 || x == 8) {
                        continue; // a gap to cross through
                    }
                    for (int y = 1; y <= 12; y++) {
                        set(x, FLOOR + y, z, wall);
                    }
                }
            }
            set(7, FLOOR + 1, 7, accent);
        }

        /** A floor with pieces taken out and patches rubbed out. */
        void erased() {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    long h = Math.floorMod((x / 3 + (x0 >> 2)) * 7919L + (z / 3 + (z0 >> 2)) * 104729L + r.nextInt(2), 11);
                    if (h == 0 || h == 5) {
                        continue; // a hole
                    }
                    set(x, FLOOR, z, h == 3 || h == 8 ? wall : floor);
                }
            }
            if (x0 == 0 && z0 == 0) {
                for (int x = 5; x <= 10; x++) {
                    for (int z = 5; z <= 10; z++) {
                        set(x, FLOOR, z, floor);
                    }
                }
            }
        }

        /** Outlines of houses on bare floor: a rectangle one block high, a post at each corner. */
        void draft() {
            floorAll();
            if (r.nextInt(3) == 0) {
                return;
            }
            int w = 5 + r.nextInt(5);
            int d = 5 + r.nextInt(4);
            int ox = r.nextInt(16 - w);
            int oz = r.nextInt(16 - d);
            for (int x = 0; x < w; x++) {
                set(ox + x, FLOOR + 1, oz, wall);
                set(ox + x, FLOOR + 1, oz + d - 1, wall);
            }
            for (int z = 0; z < d; z++) {
                set(ox, FLOOR + 1, oz + z, wall);
                set(ox + w - 1, FLOOR + 1, oz + z, wall);
            }
            for (int[] c : new int[][]{{0, 0}, {w - 1, 0}, {0, d - 1}, {w - 1, d - 1}}) {
                for (int y = 2; y <= 4; y++) {
                    set(ox + c[0], FLOOR + y, oz + c[1], accent);
                }
            }
        }

        /** A stepped pyramid in the middle of each chunk, glowing at the top. */
        void steps() {
            floorAll();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    double d = Math.max(Math.abs(x - 7.5), Math.abs(z - 7.5));
                    int height = Math.max(0, 7 - (int) Math.floor(d));
                    for (int y = 1; y <= height; y++) {
                        set(x, FLOOR + y, z, wall);
                    }
                }
            }
            set(7, FLOOR + 8, 7, accent);
            set(8, FLOOR + 8, 8, accent);
        }
    }
}
