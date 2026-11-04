package com.realarqade.orrin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Makes the world the loader starts the game in: Orrin's Bend, as a real Alpha save (chunk files in the old one-file-per-chunk
 * format). It is made by code, not copied from anywhere, so nothing of Mojang's is in it.
 * <p>
 * <b>The night (1.0.14)</b>: the gates have just closed, at Name 0054. The valley is dry and the whole town is there, lit:
 * 41 lots on a 14-block grid in a diamond, gravel streets with lamp posts, a church with nineteen pews and a steeple, houses with
 * lit windows, a store, trees, and the Word0753 Dam closing the east rim, where the world starts. In nineteen of the forty houses (the ones
 * with gravel out front, "placeholder text") somebody is standing. They are 2D sprites (see OrrinSprite).
 * <p>
 * <b>The aftermath (1.0.15)</b>: the same valley the next afternoon, flooded. The nineteen houses are gone down to their floors, the
 * church is sealed with no pews, the lights are out and the gates are open, and the water stands over everything. The nineteen are
 * still standing where their houses were, now in the water.
 * <p>
 * Alpha chunks are 16 x 16 x 128. Block (x, y, z) in a chunk is at index {@code x << 11 | z << 7 | y}.
 */
final class OrrinBend {
    // Alpha block ids
    static final int AIR = 0, STONE = 1, GRASS = 2, DIRT = 3, COBBLE = 4, PLANKS = 5, BEDROCK = 7, WATER = 9, SAND = 12, GRAVEL = 13,
            LOG = 17, LEAVES = 18, GLASS = 20, CLOTH = 35, STEP = 44, BOOKSHELF = 47, TORCH = 50, CHEST = 54, LADDER = 65;

    static final int HEIGHT = 128;
    /** The aftermath's water level. The night has none: the valley is dry. */
    static final int WATER_LEVEL_AFTER = 66;
    static final int RADIUS_CHUNKS = 13;
    static final long SEED = 19621014L;
    /** Name 0054: Alpha's day starts at sunrise (tick 0 = 6 a.m.), so 4 a.m. is 22000. */
    static final long TIME = 22000L;
    /** The aftermath is three in the afternoon on the fifteenth (3 p.m. is 9000). */
    static final long TIME_AFTER = 9000L;
    static final int DAM_X = 170;

    private static final String[] PEOPLE = {"id_0407", "id_0567", "id_0561", "id_0495", "id_0457", "id_0281"};

    private final int size = (RADIUS_CHUNKS * 2 + 1) * 16;
    private final byte[][][] blocks = new byte[RADIUS_CHUNKS * 2 + 1][RADIUS_CHUNKS * 2 + 1][];
    private final byte[][][] meta = new byte[RADIUS_CHUNKS * 2 + 1][RADIUS_CHUNKS * 2 + 1][];
    private final Map<Long, List<Nbt.Compound>> entities = new HashMap<Long, List<Nbt.Compound>>();
    private final int min = -RADIUS_CHUNKS * 16;
    private final int max = RADIUS_CHUNKS * 16 + 15;
    private final boolean after;
    private final int waterLevel;
    private final Random random = new Random(SEED);

    private OrrinBend(boolean after) {
        this.after = after;
        this.waterLevel = after ? WATER_LEVEL_AFTER : 0;
    }

    /**
     * @param aftermath false: the night of the fourteenth (1.0.14), dry and lit. true: the same valley the next afternoon (1.0.15),
     *                  flooded, with the crossed-out houses gone, the church sealed and the lights out.
     */
    static void write(File saveDir, boolean aftermath) throws IOException {
        new OrrinBend(aftermath).generate(saveDir);
    }

    // ---------------------------------------------------------------- blocks

    private byte[] chunk(byte[][][] store, int cx, int cz) {
        byte[] c = store[cx + RADIUS_CHUNKS][cz + RADIUS_CHUNKS];
        if (c == null) {
            c = new byte[16 * 16 * HEIGHT];
            store[cx + RADIUS_CHUNKS][cz + RADIUS_CHUNKS] = c;
        }
        return c;
    }

    private boolean inside(int x, int y, int z) {
        return x >= min && x <= max && z >= min && z <= max && y >= 0 && y < HEIGHT;
    }

    private static int index(int x, int y, int z) {
        return (Math.floorMod(x, 16) << 11) | (Math.floorMod(z, 16) << 7) | y;
    }

    private void set(int x, int y, int z, int id) {
        set(x, y, z, id, 0);
    }

    private void set(int x, int y, int z, int id, int data) {
        if (!inside(x, y, z)) {
            return;
        }
        int cx = Math.floorDiv(x, 16);
        int cz = Math.floorDiv(z, 16);
        chunk(blocks, cx, cz)[index(x, y, z)] = (byte) id;
        chunk(meta, cx, cz)[index(x, y, z)] = (byte) data;
    }

    private int get(int x, int y, int z) {
        if (!inside(x, y, z)) {
            return AIR;
        }
        return chunk(blocks, Math.floorDiv(x, 16), Math.floorDiv(z, 16))[index(x, y, z)] & 0xFF;
    }

    private void fill(int id, int x1, int y1, int z1, int x2, int y2, int z2) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(x, y, z, id);
                }
            }
        }
    }

    /** A torch standing on the block below it (metadata 5). Not in the aftermath: the lights are out. */
    private void torch(int x, int y, int z) {
        if (!after) {
            set(x, y, z, TORCH, 5);
        }
    }

    /** The ground height at a column: a bowl, 40 on the floor of the valley, rising towards the rim. */
    private static int ground(int x, int z) {
        double d = Math.sqrt((double) x * x + (double) z * z);
        double g = 40 + Math.max(0, d - 100) * 0.5 + Math.sin(x * 0.11) * Math.cos(z * 0.13) * 1.5;
        return (int) Math.min(g, 96);
    }

    // ---------------------------------------------------------------- generation

    private void generate(File saveDir) throws IOException {
        for (int x = min; x <= max; x++) {
            for (int z = min; z <= max; z++) {
                int g = ground(x, z);
                boolean wet = g < waterLevel;
                set(x, 0, z, BEDROCK);
                for (int y = 1; y <= g; y++) {
                    int id = STONE;
                    if (y > g - 4) {
                        id = wet ? (y == g || y == g - 1 ? SAND : GRAVEL) : (y == g ? GRASS : DIRT);
                    }
                    set(x, y, z, id);
                }
            }
        }
        streets();
        town();
        trees();
        dam();
        if (after) {
            // Water over everything still air below the surface of the lake: the streets and the insides of the houses.
            for (int x = min; x <= max; x++) {
                for (int z = min; z <= max; z++) {
                    if (ground(x, z) < waterLevel) {
                        for (int y = 1; y <= waterLevel; y++) {
                            if (get(x, y, z) == AIR) {
                                set(x, y, z, WATER);
                            }
                        }
                    }
                }
            }
        }
        save(saveDir);
    }

    /** Gravel streets on the grid between the lots, with a lamp post at every crossing. */
    private void streets() {
        for (int x = -76; x <= 76; x++) {
            for (int z = -76; z <= 76; z++) {
                boolean onX = Math.floorMod(x, 14) >= 6 && Math.floorMod(x, 14) <= 8;
                boolean onZ = Math.floorMod(z, 14) >= 6 && Math.floorMod(z, 14) <= 8;
                if ((onX || onZ) && Math.abs(x) + Math.abs(z) <= 14 * 4 + 14 + 10) {
                    set(x, ground(x, z), z, GRAVEL);
                }
            }
        }
        for (int i = -5; i <= 5; i++) {
            for (int j = -5; j <= 5; j++) {
                int x = i * 14 + 7;
                int z = j * 14 + 7;
                if (Math.abs(i * 14 + 7) + Math.abs(j * 14 + 7) <= 14 * 4 + 14 + 6) {
                    int g = ground(x, z);
                    fill(LOG, x, g + 1, z, x, g + 3, z);
                    torch(x, g + 4, z);
                }
            }
        }
    }

    /** 41 lots on a 14-block grid in a diamond (|i| + |j| <= 4). The middle one is the church; every fifth is the store. */
    private void town() {
        int index = 0;
        for (int i = -4; i <= 4; i++) {
            for (int j = -4; j <= 4; j++) {
                if (Math.abs(i) + Math.abs(j) > 4) {
                    continue;
                }
                int cx = i * 14;
                int cz = j * 14;
                int base = ground(cx, cz);
                fill(STONE, cx - 5, 1, cz - 5, cx + 5, base, cz + 5);
                fill(AIR, cx - 5, base + 1, cz - 5, cx + 5, base + 14, cz + 5);
                fill(GRASS, cx - 5, base, cz - 5, cx + 5, base, cz + 5);
                if (i == 0 && j == 0) {
                    church(cx, cz, base);
                } else {
                    boolean crossedOut = isCrossedOut(index);
                    if (after && crossedOut) {
                        fill(PLANKS, cx - 3, base, cz - 3, cx + 3, base, cz + 3); // struck from Name 0548: nothing left but the floor
                    } else if (index % 5 == 4) {
                        store(cx, cz, base);
                    } else {
                        house(cx, cz, base, index);
                        if (!after && crossedOut) {
                            fill(GRAVEL, cx - 1, base, cz + 4, cx + 1, base, cz + 5); // crossed out in pencil
                        }
                    }
                    // A path from Name 0599 to the street.
                    fill(GRAVEL, cx, base, cz + 4, cx, base, cz + 5);
                    if (crossedOut) {
                        // Somebody is standing in it. In the aftermath they are still there, in the water, on the bare floor.
                        addSprite(cx + 1.5, base + 1, cz + 1.5, PEOPLE[index % PEOPLE.length]);
                    }
                    index++;
                }
            }
        }
    }

    /** Which of the forty houses (counted in order) are crossed out: nineteen of them, spread out, always the same ones. */
    private static boolean isCrossedOut(int houseIndex) {
        return (houseIndex * 19) / 40 != ((houseIndex + 1) * 19) / 40;
    }

    private void house(int cx, int cz, int base, int index) {
        int y = base + 1;
        boolean wide = index % 3 == 1;
        int hw = wide ? 4 : 3;
        fill(PLANKS, cx - hw, base, cz - 3, cx + hw, base, cz + 3);                                  // floor
        int wall = index % 4 == 0 ? COBBLE : PLANKS;
        fill(wall, cx - hw, y, cz - 3, cx + hw, y + 3, cz - 3);
        fill(wall, cx - hw, y, cz + 3, cx + hw, y + 3, cz + 3);
        fill(wall, cx - hw, y, cz - 3, cx - hw, y + 3, cz + 3);
        fill(wall, cx + hw, y, cz - 3, cx + hw, y + 3, cz + 3);
        fill(LOG, cx - hw, y, cz - 3, cx - hw, y + 3, cz - 3);                                       // corner posts
        fill(LOG, cx + hw, y, cz - 3, cx + hw, y + 3, cz - 3);
        fill(LOG, cx - hw, y, cz + 3, cx - hw, y + 3, cz + 3);
        fill(LOG, cx + hw, y, cz + 3, cx + hw, y + 3, cz + 3);
        fill(PLANKS, cx - hw, y + 4, cz - 3, cx + hw, y + 4, cz + 3);                                // roof, in steps
        fill(PLANKS, cx - hw + 1, y + 5, cz - 2, cx + hw - 1, y + 5, cz + 2);
        fill(PLANKS, cx - hw + 2, y + 6, cz - 1, cx + hw - 2, y + 6, cz + 1);
        fill(AIR, cx, y, cz + 3, cx, y + 1, cz + 3);                                                 // door
        fill(GLASS, cx - hw, y + 1, cz, cx - hw, y + 2, cz);                                         // windows
        fill(GLASS, cx + hw, y + 1, cz, cx + hw, y + 2, cz);
        fill(GLASS, cx - 2, y + 1, cz - 3, cx - 2, y + 2, cz - 3);
        fill(GLASS, cx + 2, y + 1, cz + 3, cx + 2, y + 2, cz + 3);
        fill(AIR, cx - hw + 1, y, cz - 2, cx + hw - 1, y + 3, cz + 2);                               // inside
        if (index % 2 == 0) {                                                                         // a chimney
            fill(COBBLE, cx + hw - 1, y + 4, cz - 2, cx + hw - 1, y + 8, cz - 2);
        }
        // A table, two chairs pulled out, a chest, and a lamp in the corner by the window. The next day: the table, and nothing else.
        set(cx, y, cz, PLANKS);
        if (!after) {
            set(cx - 1, y, cz + 1, STEP);
            set(cx + 1, y, cz - 1, STEP);
            set(cx + hw - 1, y, cz - 2, CHEST);
            torch(cx - hw + 1, y, cz - 2);
            torch(cx + hw - 1, y, cz + 2);
        }
        // Flower box and a log at Name 0599.
        set(cx - 1, base, cz + 4, LOG);
    }

    /** The store: shelves of books, a Word0893, a chest, two lamps. */
    private void store(int cx, int cz, int base) {
        int y = base + 1;
        fill(PLANKS, cx - 4, base, cz - 3, cx + 4, base, cz + 3);
        fill(COBBLE, cx - 4, y, cz - 3, cx + 4, y + 3, cz - 3);
        fill(COBBLE, cx - 4, y, cz + 3, cx + 4, y + 3, cz + 3);
        fill(COBBLE, cx - 4, y, cz - 3, cx - 4, y + 3, cz + 3);
        fill(COBBLE, cx + 4, y, cz - 3, cx + 4, y + 3, cz + 3);
        fill(PLANKS, cx - 4, y + 4, cz - 3, cx + 4, y + 4, cz + 3);
        fill(PLANKS, cx - 3, y + 5, cz - 2, cx + 3, y + 5, cz + 2);
        fill(AIR, cx - 3, y, cz - 2, cx + 3, y + 3, cz + 2);
        fill(AIR, cx, y, cz + 3, cx + 1, y + 1, cz + 3);
        fill(GLASS, cx - 3, y + 1, cz + 3, cx - 2, y + 2, cz + 3);
        fill(GLASS, cx + 3, y + 1, cz + 3, cx + 3, y + 2, cz + 3);
        fill(BOOKSHELF, cx - 3, y, cz - 2, cx + 3, y + 1, cz - 2);
        fill(PLANKS, cx - 2, y, cz + 1, cx + 2, y, cz + 1);
        if (!after) {
            set(cx + 3, y, cz, CHEST);
            torch(cx - 3, y, cz + 2);
            torch(cx + 3, y, cz + 2);
        }
    }

    private void church(int cx, int cz, int base) {
        int y = base + 1;
        // 9 wide (x), 11 long (z), door at the south end, a steeple at the front.
        fill(PLANKS, cx - 4, base, cz - 5, cx + 4, base, cz + 5);
        fill(COBBLE, cx - 4, y, cz - 5, cx + 4, y + 6, cz - 5);
        fill(COBBLE, cx - 4, y, cz + 5, cx + 4, y + 6, cz + 5);
        fill(COBBLE, cx - 4, y, cz - 5, cx - 4, y + 6, cz + 5);
        fill(COBBLE, cx + 4, y, cz - 5, cx + 4, y + 6, cz + 5);
        for (int s = 0; s < 4; s++) {                                                               // pitched roof, in steps
            fill(PLANKS, cx - 4 + s, y + 7 + s, cz - 5, cx + 4 - s, y + 7 + s, cz + 5);
        }
        fill(AIR, cx - 3, y, cz - 4, cx + 3, y + 6, cz + 4);
        fill(after ? COBBLE : AIR, cx - 1, y, cz + 5, cx + 1, y + 3, cz + 5);                      // double door (sealed the next day)
        for (int z = -3; z <= 3; z += 2) {                                                          // tall windows
            fill(GLASS, cx - 4, y + 2, cz + z, cx - 4, y + 4, cz + z);
            fill(GLASS, cx + 4, y + 2, cz + z, cx + 4, y + 4, cz + z);
        }
        // The steeple: a tower over Name 0599, with a belfry Word0778 and a point.
        fill(COBBLE, cx - 2, y, cz + 5, cx + 2, y + 13, cz + 8);
        fill(AIR, cx - 1, y + 7, cz + 6, cx + 1, y + 10, cz + 7);
        fill(AIR, cx - 1, y + 11, cz + 5, cx + 1, y + 11, cz + 8);
        fill(AIR, cx - 1, y, cz + 6, cx + 1, y + 6, cz + 7);
        fill(AIR, cx, y, cz + 8, cx, y + 1, cz + 8);
        for (int s = 0; s < 3; s++) {
            fill(PLANKS, cx - 2 + s, y + 14 + s, cz + 5 + s, cx + 2 - s, y + 14 + s, cz + 8 - s);
        }
        set(cx, y + 17, cz + 6, LOG);
        if (after) {
            fill(COBBLE, cx - 1, y, cz + 8, cx + 1, y + 1, cz + 8);
            return; // the pews, the altar and the lights are gone
        }
        // Nineteen pews, facing the altar: eight rows on each side of the aisle, and three across the back. Two blocks each.
        int placed = 0;
        for (int row = 0; row < 8; row++) {
            int z = cz - 2 + row;
            fill(STEP, cx - 3, y, z, cx - 2, y, z);
            fill(STEP, cx + 2, y, z, cx + 3, y, z);
            placed += 2;
        }
        for (int k = -1; k <= 1; k++) {
            set(cx + k, y, cz + 4, STEP);
            placed++;
        }
        // placed == 19.
        fill(CLOTH, cx - 1, y, cz - 4, cx + 1, y, cz - 4);                                         // the altar
        set(cx, y + 1, cz - 4, CHEST);
        torch(cx - 2, y, cz - 4);
        torch(cx + 2, y, cz - 4);
        torch(cx - 3, y, cz + 4);
        torch(cx + 3, y, cz + 4);
        torch(cx - 3, y, cz - 3);
        torch(cx + 3, y, cz - 3);
    }

    /** A few trees on the grass between the town and the rim. */
    private void trees() {
        for (int n = 0; n < 60; n++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 90 + random.nextDouble() * 70;
            int x = (int) (Math.cos(angle) * distance);
            int z = (int) (Math.sin(angle) * distance);
            if (x > DAM_X - 12 && Math.abs(z) < 55) {
                continue;
            }
            int g = ground(x, z);
            if (g < waterLevel + 2 || get(x, g, z) != GRASS) {
                continue;
            }
            int h = 4 + random.nextInt(2);
            fill(LOG, x, g + 1, z, x, g + h, z);
            for (int dy = h - 1; dy <= h + 1; dy++) {
                int r = dy == h + 1 ? 1 : 2;
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if ((dx != 0 || dz != 0 || dy > h) && get(x + dx, g + dy, z + dz) == AIR) {
                            set(x + dx, g + dy, z + dz, LEAVES);
                        }
                    }
                }
            }
        }
    }

    /** The Word0753 Dam: a wall across the east rim, with a walkway on top. The world starts here. */
    private void dam() {
        for (int z = -45; z <= 45; z++) {
            for (int x = DAM_X - 5; x <= DAM_X + 5; x++) {
                int g = ground(x, z);
                fill(COBBLE, x, Math.max(1, g - 2), z, x, 84, z);
            }
            set(DAM_X - 5, 85, z, COBBLE);
            set(DAM_X + 5, 85, z, COBBLE);
        }
        for (int z = -30; z <= 30; z += 10) {
            if (after) {
                continue; // the gates are open
            }
            fill(LOG, DAM_X, 85, z, DAM_X, 88, z);
            torch(DAM_X, 89, z);
        }
    }

    /** For testing only (-Word0943.spawn=town, set by the loader's --spawn-town): start in the town instead of on the dam. */
    private static boolean townSpawn() {
        return "town".equals(System.getProperty("orrin.spawn"));
    }

    static int spawnX() {
        return townSpawn() ? 14 : DAM_X - 2;
    }

    static int spawnY() {
        return townSpawn() ? ground(14, 20) + 3 : 86;
    }

    static int spawnZ() {
        return townSpawn() ? 20 : 0;
    }

    // ---------------------------------------------------------------- 2D people

    /** Puts one of the 2D figures in the world (see OrrinSprite in the game's own process). */
    private void addSprite(double x, double y, double z, String sprite) {
        Nbt.Compound e = new Nbt.Compound();
        e.put("id", "OrrinSprite");
        Nbt.NList pos = new Nbt.NList(6);
        pos.items.add(x);
        pos.items.add(y);
        pos.items.add(z);
        e.put("Pos", pos);
        Nbt.NList motion = new Nbt.NList(6);
        motion.items.add(0.0);
        motion.items.add(0.0);
        motion.items.add(0.0);
        e.put("Motion", motion);
        Nbt.NList rotation = new Nbt.NList(5);
        rotation.items.add(0.0f);
        rotation.items.add(0.0f);
        e.put("Rotation", rotation);
        e.put("FallDistance", 0.0f);
        e.put("Fire", (short) -1);
        e.put("Air", (short) 300);
        e.put("OnGround", (byte) 1);
        e.put("Sprite", sprite);
        long key = ((long) Math.floorDiv((int) Math.floor(x), 16) << 32) ^ (Math.floorDiv((int) Math.floor(z), 16) & 0xFFFFFFFFL);
        List<Nbt.Compound> list = entities.get(key);
        if (list == null) {
            list = new ArrayList<Nbt.Compound>();
            entities.put(key, list);
        }
        list.add(e);
    }

    static int spriteCountIn(Nbt.Compound chunkRoot) {
        Nbt.Compound level = (Nbt.Compound) chunkRoot.get("Level");
        Object list = level.get("Entities");
        return list instanceof Nbt.NList ? ((Nbt.NList) list).items.size() : 0;
    }

    // ---------------------------------------------------------------- saving

    private void save(File saveDir) throws IOException {
        saveDir.mkdirs();
        Nbt.Compound data = new Nbt.Compound()
                .with("RandomSeed", SEED)
                .with("SpawnX", spawnX()).with("SpawnY", spawnY()).with("SpawnZ", spawnZ())
                .with("Time", after ? TIME_AFTER : TIME)
                .with("LastPlayed", System.currentTimeMillis())
                .with("SizeOnDisk", 0L);
        Nbt.Compound root = new Nbt.Compound().with("Data", data);
        OutputStream out = new FileOutputStream(new File(saveDir, "level.dat"));
        try {
            Nbt.writeGzip(out, root);
        } finally {
            out.close();
        }
        byte[] blockLight = blockLight();
        for (int cx = -RADIUS_CHUNKS; cx <= RADIUS_CHUNKS; cx++) {
            for (int cz = -RADIUS_CHUNKS; cz <= RADIUS_CHUNKS; cz++) {
                writeChunk(saveDir, cx, cz, blockLight);
            }
        }
    }

    private static boolean opaque(int id) {
        return id != AIR && id != GLASS && id != TORCH && id != LEAVES;
    }

    /** Light from the torches, spreading through anything that is not solid, one level weaker with each block. */
    private byte[] blockLight() {
        byte[] light = new byte[size * size * HEIGHT];
        int[] queue = new int[1 << 22];
        int head = 0;
        int tail = 0;
        for (int x = min; x <= max; x++) {
            for (int z = min; z <= max; z++) {
                for (int y = 1; y < HEIGHT; y++) {
                    if (get(x, y, z) == TORCH) {
                        int i = ((x - min) * size + (z - min)) * HEIGHT + y;
                        light[i] = 14;
                        queue[tail++ & (queue.length - 1)] = i;
                    }
                }
            }
        }
        int[] dx = {1, -1, 0, 0, 0, 0};
        int[] dy = {0, 0, 1, -1, 0, 0};
        int[] dz = {0, 0, 0, 0, 1, -1};
        while (head != tail) {
            int i = queue[head++ & (queue.length - 1)];
            int y = i % HEIGHT;
            int z = (i / HEIGHT) % size;
            int x = i / HEIGHT / size;
            int level = light[i];
            if (level <= 1) {
                continue;
            }
            for (int d = 0; d < 6; d++) {
                int nx = x + dx[d];
                int ny = y + dy[d];
                int nz = z + dz[d];
                if (nx < 0 || nz < 0 || nx >= size || nz >= size || ny < 0 || ny >= HEIGHT) {
                    continue;
                }
                int id = get(nx + min, ny, nz + min);
                if (opaque(id) && id != WATER) {
                    continue;
                }
                int ni = (nx * size + nz) * HEIGHT + ny;
                int next = level - (id == WATER ? 3 : 1);
                if (next > light[ni]) {
                    light[ni] = (byte) next;
                    queue[tail++ & (queue.length - 1)] = ni;
                }
            }
        }
        return light;
    }

    private void writeChunk(File saveDir, int cx, int cz, byte[] globalLight) throws IOException {
        byte[] b = chunk(blocks, cx, cz);
        byte[] m = chunk(meta, cx, cz);
        byte[] sky = new byte[b.length / 2];
        byte[] light = new byte[b.length / 2];
        byte[] data = new byte[b.length / 2];
        byte[] height = new byte[256];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int level = 15;
                int top = 0;
                boolean found = false;
                for (int y = HEIGHT - 1; y >= 0; y--) {
                    int idx = (x << 11) | (z << 7) | y;
                    int id = b[idx] & 0xFF;
                    if (id == WATER) {
                        level = Math.max(0, level - 3);
                    } else if (opaque(id)) {
                        level = 0;
                    }
                    if (!found && (opaque(id) || id == WATER)) {
                        top = y + 1;
                        found = true;
                    }
                    int half = idx >> 1;
                    int gi = ((cx * 16 + x - min) * size + (cz * 16 + z - min)) * HEIGHT + y;
                    int bl = globalLight[gi];
                    if ((idx & 1) == 0) {
                        sky[half] = (byte) ((sky[half] & 0xF0) | level);
                        light[half] = (byte) ((light[half] & 0xF0) | bl);
                        data[half] = (byte) ((data[half] & 0xF0) | (m[idx] & 15));
                    } else {
                        sky[half] = (byte) ((sky[half] & 0x0F) | (level << 4));
                        light[half] = (byte) ((light[half] & 0x0F) | (bl << 4));
                        data[half] = (byte) ((data[half] & 0x0F) | ((m[idx] & 15) << 4));
                    }
                }
                height[z << 4 | x] = (byte) top;
            }
        }
        Nbt.NList entityList = new Nbt.NList(10);
        long key = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
        List<Nbt.Compound> inChunk = entities.get(key);
        if (inChunk != null) {
            entityList.items.addAll(inChunk);
        }
        Nbt.Compound levelTag = new Nbt.Compound()
                .with("Blocks", b).with("Data", data).with("SkyLight", sky).with("BlockLight", light)
                .with("HeightMap", height)
                .with("Entities", entityList).with("TileEntities", new Nbt.NList(10))
                .with("LastUpdate", 0L).with("xPos", cx).with("zPos", cz).with("TerrainPopulated", (byte) 1);
        Nbt.Compound root = new Nbt.Compound().with("Level", levelTag);
        File dir = new File(new File(saveDir, Integer.toString(Math.floorMod(cx, 64), 36)), Integer.toString(Math.floorMod(cz, 64), 36));
        dir.mkdirs();
        File file = new File(dir, "c." + Integer.toString(cx, 36) + "." + Integer.toString(cz, 36) + ".dat");
        OutputStream out = new FileOutputStream(file);
        try {
            Nbt.writeGzip(out, root);
        } finally {
            out.close();
        }
    }
}
