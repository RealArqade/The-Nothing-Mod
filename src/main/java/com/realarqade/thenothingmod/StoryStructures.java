package com.realarqade.thenothingmod;

import com.realarqade.thenothingmod.platform.Platform;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.util.RandomSource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * The story-telling places. Each one is a scene somebody left in the middle of something: a table laid for four with one chair pushed
 * back, a Word0896 with the register open, a camp with the tents zipped and the fire still warm. There are no people and no signs
 * saying what happened. Every scene is in {@code the-nothing-mod.structures.json} (made by {@code tools/GenerateStructures.ps1}),
 * and each fits inside one chunk, so they can go down while a chunk is generated.
 * <p>
 * A scene is a list of operations in its own coordinates (x east, y up, z south, floor at y = 0):
 * {@code ["f", block, x1,y1,z1, x2,y2,z2]} fills a box, {@code ["s", block, x,y,z]} sets one block, and
 * {@code ["c", x,y,z, facing, [[item, count], ...]]} puts a chest with things in it. A block can carry properties, like
 * {@code minecraft:oak_stairs[facing=east,half=bottom]}. Scenes are placed turned a random quarter turn.
 */
public final class StoryStructures {
    public record Scene(String id, String dim, int w, int d, int h, float decay, String foundation, List<JsonArray> ops) {
    }

    public static final List<Scene> SCENES = load();
    private static final Map<String, List<Scene>> BY_DIM = new HashMap<>();
    private static final Queue<Pending> queue = new ArrayDeque<>();

    private record Pending(ServerLevel level, ChunkPos chunk) {
    }

    static {
        for (Scene scene : SCENES) {
            BY_DIM.computeIfAbsent(scene.dim(), k -> new ArrayList<>()).add(scene);
        }
    }

    private StoryStructures() {
    }

    public static void register() {
        Platform.get().onChunkGenerate((level, chunk) -> {
            if (StalkerProperties.STRUCTURES_ENABLED) {
                queue.add(new Pending(level, chunk.getPos()));
            }
        });
        // One at a time, on the main thread, a moment after the chunk exists.
        Platform.get().onServerTickEnd(server -> {
            Pending next = queue.poll();
            if (next != null && next.level().hasChunk(Compat.cx(next.chunk()), Compat.cz(next.chunk()))) {
                tryPlace(next.level(), next.chunk());
            }
        });
    }

    private static String dimOf(ServerLevel level) {
        if (Dimensions.isOurs(level)) {
            return Dimensions.idOf(level).startsWith("d") ? "overworld" : "";
        }
        if (level.dimension() == Level.OVERWORLD) {
            return "overworld";
        }
        return level.dimension() == Level.NETHER ? "nether" : level.dimension() == Level.END ? "end" : "";
    }

    private static void tryPlace(ServerLevel level, ChunkPos chunk) {
        // Each of the fifty dimensions has landmarks of its own, on top of everything else.
        if (Dimensions.isOurs(level)) {
            List<Scene> landmarks = BY_DIM.get("dim_" + Dimensions.idOf(level));
            RandomSource lr = RandomSource.create(level.getSeed() ^ (Compat.cx(chunk) * 91823L + Compat.cz(chunk) * 479001599L) ^ 0x1A7DL);
            if (landmarks != null && lr.nextInt(StalkerProperties.LANDMARK_CHANCE) == 0
                    && (Math.abs(Compat.cx(chunk)) > 1 || Math.abs(Compat.cz(chunk)) > 1)) {
                place(level, landmarks.get(lr.nextInt(landmarks.size())), chunk, lr.nextInt(4), lr);
                return;
            }
        }
        String dim = dimOf(level);
        RandomSource random = RandomSource.create(level.getSeed() ^ (Compat.cx(chunk) * 341873128712L + Compat.cz(chunk) * 132897987541L) ^ 0x5CE4E5L);
        int chance = switch (dim) {
            case "overworld" -> StalkerProperties.STRUCTURE_CHANCE_OVERWORLD;
            case "nether" -> StalkerProperties.STRUCTURE_CHANCE_NETHER;
            case "end" -> StalkerProperties.STRUCTURE_CHANCE_END;
            default -> 0;
        };
        // Gates and the void holes: their own, rarer roll, so the gates are findable but not everywhere.
        if (!dim.isEmpty() && random.nextInt(StalkerProperties.GATE_CHANCE) == 0
                && !(Math.abs(Compat.cx(chunk)) < 6 && Math.abs(Compat.cz(chunk)) < 6)) {
            List<Scene> gates = new ArrayList<>(BY_DIM.getOrDefault("gate_" + (Dimensions.isOurs(level) ? "overworld" : dim), List.of()));
            if (dim.equals("overworld") && !Dimensions.isOurs(level)) {
                gates.addAll(BY_DIM.getOrDefault("gate_cellar", List.of()));
                gates.addAll(SCENES.stream().filter(s -> s.id().startsWith("id_0565")).toList());
            }
            if (!gates.isEmpty()) {
                Scene gate = gates.get(random.nextInt(gates.size()));
                if (gate.dim().equals("gate_cellar")) {
                    gate = new Scene(gate.id(), "cellar", gate.w(), gate.d(), gate.h(), gate.decay(), gate.foundation(), gate.ops());
                } else if (gate.dim().startsWith("gate_")) {
                    gate = new Scene(gate.id(), dim.equals("overworld") ? "overworld" : dim, gate.w(), gate.d(), gate.h(), gate.decay(), gate.foundation(), gate.ops());
                }
                place(level, gate, chunk, random.nextInt(4), random);
                return;
            }
        }
        // Some overworld scenes go underground; they are listed with the dimension "cellar".
        List<Scene> pool = BY_DIM.getOrDefault(dim, List.of());
        if (dim.equals("overworld") && random.nextInt(4) == 0 && BY_DIM.containsKey("cellar")) {
            pool = BY_DIM.get("cellar");
        }
        if (chance <= 0 || pool.isEmpty() || random.nextInt(chance) != 0) {
            return;
        }
        // Never right next to the world spawn, so a new world isn't greeted by one on the first step.
        if (Math.abs(Compat.cx(chunk)) < 6 && Math.abs(Compat.cz(chunk)) < 6) {
            return;
        }
        place(level, pool.get(random.nextInt(pool.size())), chunk, random.nextInt(4), random);
    }

    /** Places a scene inside the chunk at the given rotation. Returns false if there was no suitable spot. */
    public static boolean place(ServerLevel level, Scene scene, ChunkPos chunk, int rotation, RandomSource random) {
        int fw = rotation % 2 == 0 ? scene.w() : scene.d();
        int fd = rotation % 2 == 0 ? scene.d() : scene.w();
        int x0 = chunk.getMinBlockX() + (16 - fw) / 2;
        int z0 = chunk.getMinBlockZ() + (16 - fd) / 2;
        int y0 = findFloor(level, scene, x0, z0, fw, fd, random);
        if (y0 == Integer.MIN_VALUE) {
            return false;
        }
        BlockState foundation = parse(scene.foundation(), 0);
        // Clear the space, lay a foundation under it so nothing floats.
        for (int x = 0; x < fw; x++) {
            for (int z = 0; z < fd; z++) {
                for (int y = -4; y < scene.h(); y++) {
                    BlockPos pos = new BlockPos(x0 + x, y0 + y, z0 + z);
                    boolean fill = y < 0 && !scene.dim().startsWith("dim_v") && !level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP);
                    level.setBlock(pos, y >= 0 ? Blocks.AIR.defaultBlockState() : fill ? foundation : level.getBlockState(pos), 2);
                }
            }
        }
        for (JsonArray op : scene.ops()) {
            String type = op.get(0).getAsString();
            switch (type) {
                case "f" -> {
                    for (int x = op.get(2).getAsInt(); x <= op.get(5).getAsInt(); x++) {
                        for (int y = op.get(3).getAsInt(); y <= op.get(6).getAsInt(); y++) {
                            for (int z = op.get(4).getAsInt(); z <= op.get(7).getAsInt(); z++) {
                                setBlock(level, scene, rotation, x0, y0, z0, x, y, z, op.get(1).getAsString(), random);
                            }
                        }
                    }
                }
                case "s" -> setBlock(level, scene, rotation, x0, y0, z0, op.get(2).getAsInt(), op.get(3).getAsInt(),
                        op.get(4).getAsInt(), op.get(1).getAsString(), random);
                case "c" -> chest(level, scene, rotation, x0, y0, z0, op);
                case "h" -> shaft(level, x0 + fw / 2, z0 + fd / 2, y0, op.get(1).getAsInt());
                default -> {
                }
            }
        }
        // Blocks that connect to their neighbours (glass panes, fences, iron bars, walls) were placed with default shapes; let them look
        // at what is around them now. Two passes so connections that depend on already-updated neighbours settle.
        for (int pass = 0; pass < 2; pass++) {
            for (int x = -1; x <= fw; x++) {
                for (int z = -1; z <= fd; z++) {
                    for (int y = 0; y < scene.h(); y++) {
                        BlockPos pos = new BlockPos(x0 + x, y0 + y, z0 + z);
                        BlockState old = level.getBlockState(pos);
                        if (old.isAir()) {
                            continue;
                        }
                        BlockState updated = Block.updateFromNeighbourShapes(old, level, pos);
                        if (updated != old) {
                            level.setBlock(pos, updated, 2);
                        }
                    }
                }
            }
        }
        return true;
    }

    /** Digs a square shaft of the given radius straight down through everything, bedrock included, to the bottom of the world. */
    private static void shaft(ServerLevel level, int cx, int cz, int topY, int radius) {
        for (int y = topY; y >= level.getMinY(); y--) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = new BlockPos(cx + x, y, cz + z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private static int findFloor(ServerLevel level, Scene scene, int x0, int z0, int fw, int fd, RandomSource random) {
        String dim = scene.dim();
        if (dim.startsWith("dim_v")) {
            return VoidWorlds.FLOOR; // the scene floor takes the place of the dimension floor
        }
        if (dim.equals("overworld") || dim.startsWith("dim_d")) {
            int[] heights = {height(level, x0, z0), height(level, x0 + fw - 1, z0), height(level, x0, z0 + fd - 1),
                    height(level, x0 + fw - 1, z0 + fd - 1), height(level, x0 + fw / 2, z0 + fd / 2)};
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (int h : heights) {
                min = Math.min(min, h);
                max = Math.max(max, h);
            }
            BlockPos centre = new BlockPos(x0 + fw / 2, max - 1, z0 + fd / 2);
            if (max - min > 3 || !level.getFluidState(centre).isEmpty() || !level.getFluidState(centre.above()).isEmpty()) {
                return Integer.MIN_VALUE;
            }
            return max;
        }
        if (dim.equals("cellar")) {
            return level.getMinY() + 20 + random.nextInt(50);
        }
        // Nether and end: look for solid floor with room above it.
        int lo = dim.equals("nether") ? 36 : 40;
        int hi = dim.equals("nether") ? 110 : 90;
        for (int attempt = 0; attempt < 24; attempt++) {
            int y = lo + random.nextInt(hi - lo);
            BlockPos floor = new BlockPos(x0 + fw / 2, y, z0 + fd / 2);
            boolean solid = level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
            boolean room = true;
            for (int dy = 1; dy <= scene.h() && room; dy++) {
                room = level.getBlockState(floor.above(dy)).isAir();
            }
            if (solid && room) {
                return y + 1;
            }
        }
        return Integer.MIN_VALUE;
    }

    private static int height(ServerLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
    }

    private static BlockPos world(Scene scene, int rotation, int x0, int y0, int z0, int x, int y, int z) {
        int w = scene.w();
        int d = scene.d();
        return switch (rotation) {
            case 1 -> new BlockPos(x0 + d - 1 - z, y0 + y, z0 + x);
            case 2 -> new BlockPos(x0 + w - 1 - x, y0 + y, z0 + d - 1 - z);
            case 3 -> new BlockPos(x0 + z, y0 + y, z0 + w - 1 - x);
            default -> new BlockPos(x0 + x, y0 + y, z0 + z);
        };
    }

    private static void setBlock(ServerLevel level, Scene scene, int rotation, int x0, int y0, int z0, int x, int y, int z,
                                 String spec, RandomSource random) {
        if (spec.startsWith("~") ) { // "~block" is part of the shell: it may have crumbled
            if (random.nextFloat() < scene.decay()) {
                return;
            }
            spec = spec.substring(1);
        }
        level.setBlock(world(scene, rotation, x0, y0, z0, x, y, z), parse(spec, rotation), 2);
    }

    private static void chest(ServerLevel level, Scene scene, int rotation, int x0, int y0, int z0, JsonArray op) {
        BlockPos pos = world(scene, rotation, x0, y0, z0, op.get(1).getAsInt(), op.get(2).getAsInt(), op.get(3).getAsInt());
        level.setBlock(pos, parse("minecraft:chest[facing=" + op.get(4).getAsString() + "]", rotation), 2);
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof Container container) {
            int slot = 0;
            for (JsonElement e : op.get(5).getAsJsonArray()) {
                JsonArray pair = e.getAsJsonArray();
                Item item = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(
                        pair.get(0).getAsString().contains(":") ? pair.get(0).getAsString() : TheNothingMod.MOD_ID + ":" + pair.get(0).getAsString()));
                container.setItem(slot++, new ItemStack(item, pair.get(1).getAsInt()));
            }
        }
    }

    /** "namespace:id[prop=value,...]" to a block state, with horizontal "facing" turned by the scene's rotation. */
    static BlockState parse(String spec, int rotation) {
        String name = spec;
        String props = "";
        int bracket = spec.indexOf('[');
        if (bracket >= 0) {
            name = spec.substring(0, bracket);
            props = spec.substring(bracket + 1, spec.length() - 1);
        }
        Identifier id = Identifier.tryParse(name.contains(":") ? name : TheNothingMod.MOD_ID + ":" + name);
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        BlockState state = block.defaultBlockState();
        if (props.isEmpty()) {
            return state;
        }
        for (String pair : props.split(",")) {
            String[] kv = pair.split("=");
            Property<?> property = block.getStateDefinition().getProperty(kv[0]);
            if (property == null) {
                continue;
            }
            String value = kv[1];
            if (kv[0].equals("facing") && Direction.byName(value) != null && Direction.byName(value).getAxis().isHorizontal()) {
                Direction d = Direction.byName(value);
                for (int i = 0; i < rotation; i++) {
                    d = d.getClockWise();
                }
                value = d.getName();
            }
            state = withValue(state, property, value);
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState withValue(BlockState state, Property<T> property, String value) {
        return property.getValue(value).map(v -> state.setValue(property, v)).orElse(state);
    }

    private static List<Scene> load() {
        List<Scene> scenes = new ArrayList<>();
        for (String file : new String[]{"/the-nothing-mod.structures.json", "/the-nothing-mod.structures2.json"}) {
            readScenes(file, scenes);
        }
        return List.copyOf(scenes);
    }

    private static void readScenes(String file, List<Scene> scenes) {
        try (Reader reader = new InputStreamReader(
                StoryStructures.class.getResourceAsStream(file), StandardCharsets.UTF_8)) {
            for (JsonElement element : new Gson().fromJson(reader, JsonElement.class).getAsJsonArray()) {
                JsonObject o = element.getAsJsonObject();
                List<JsonArray> ops = new ArrayList<>();
                for (JsonElement op : o.getAsJsonArray("ops")) {
                    ops.add(op.getAsJsonArray());
                }
                scenes.add(new Scene(o.get("id").getAsString(), o.get("dim").getAsString(), o.get("w").getAsInt(),
                        o.get("d").getAsInt(), o.get("h").getAsInt(),
                        o.has("decay") ? o.get("decay").getAsFloat() : 0.1f,
                        o.has("foundation") ? o.get("foundation").getAsString() : "minecraft:dirt", ops));
            }
        } catch (IOException | RuntimeException e) {
            TheNothingMod.LOGGER.error("placeholder text", file, e);
        }
    }
}
