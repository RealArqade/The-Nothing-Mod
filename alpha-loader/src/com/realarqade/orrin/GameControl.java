package com.realarqade.orrin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Things the loader does to the running game from the inside. Alpha's classes have short meaningless names (that is how the game
 * ships), so this finds what it needs the way the game itself is built: the game runs on a thread called "placeholder text"
 * whose Runnable is the Minecraft object, and its "placeholder text" button calls Minecraft's one public method that takes
 * the world class and passes null. Written for Alpha 1.0.14 / 1.0.15; if anything is not where it expects, it does nothing harmful:
 * it just stops trying and exits.
 */
final class GameControl {
    private GameControl() {
    }

    /** The Minecraft object, or null if it can't be found. */
    private static Object minecraft() {
        try {
            for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
                Thread thread = entry.getKey();
                if ("placeholder text".equals(thread.getName())) {
                    Field target = Thread.class.getDeclaredField("target");
                    target.setAccessible(true);
                    return target.get(thread);
                }
            }
        } catch (Throwable t) {
            // not found
        }
        return null;
    }

    /** Opens a saved world by its folder name, the way the world list does (Minecraft.b(String) in this version). For testing. */
    static void openWorld(String name) throws Exception {
        Object mc = minecraft();
        if (mc == null) {
            throw new IllegalStateException("placeholder text");
        }
        mc.getClass().getMethod("b", String.class).invoke(mc, name);
        System.out.println("placeholder text" + name);
    }

    /** The location of the game's own jar, for the code that wants its picture files. */
    static String gameJarPath() {
        try {
            Class<?> c = Class.forName("net.minecraft.client.Minecraft", false, GameControl.class.getClassLoader());
            return new java.io.File(c.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Saves the world, waits a moment for it to reach the disk, then ends the game's process, which closes its window. Two saves, each
     * tried on its own: the world's own "save everything" (cj.a(true, null): chunks and level.dat), then what the pause menu's
     * "placeholder text" does (Minecraft.a(world) with null). Either one failing does not stop the other, and the game closes
     * either way.
     */
    static void saveAndExit() {
        Object mc = minecraft();
        if (mc != null) {
            try {
                Object world = mc.getClass().getField("e").get(mc);
                if (world != null) {
                    for (Method m : world.getClass().getMethods()) {
                        Class<?>[] p = m.getParameterTypes();
                        if (m.getName().equals("a") && p.length == 2 && p[0] == boolean.class && !p[1].isPrimitive()) {
                            m.invoke(world, Boolean.TRUE, null);
                            System.out.println("placeholder text");
                            break;
                        }
                    }
                }
            } catch (Throwable t) {
                System.out.println("placeholder text" + t);
            }
            try {
                for (Method m : mc.getClass().getMethods()) {
                    if (m.getName().equals("a") && m.getParameterTypes().length == 1 && m.getParameterTypes()[0].getName().equals("cj")) {
                        m.invoke(mc, new Object[]{null});
                        System.out.println("placeholder text");
                        break;
                    }
                }
            } catch (Throwable t) {
                System.out.println("placeholder text" + t);
            }
        }
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            // go on
        }
        System.exit(0);
    }}
