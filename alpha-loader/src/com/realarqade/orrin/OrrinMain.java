package com.realarqade.orrin;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * The entry point inside the game's own process (the loader starts a new Java process whose main class is this). It loads the mods in
 * the mods folder, lets each one run, then starts the game exactly as the old standalone client was started: Minecraft.main with the
 * user name and a session id. Must stay Java 8 compatible: Alpha needs an old Java.
 */
public final class OrrinMain {
    private OrrinMain() {
    }

    public static void main(String[] args) throws Exception {
        String modsDir = System.getProperty("orrin.mods", "mods");
        File[] jars = new File(modsDir).listFiles();
        List<URL> urls = new ArrayList<URL>();
        if (jars != null) {
            for (File jar : jars) {
                if (jar.getName().endsWith(".jar")) {
                    urls.add(jar.toURI().toURL());
                }
            }
        }
        ClassLoader game = OrrinMain.class.getClassLoader();
        URLClassLoader modLoader = new URLClassLoader(urls.toArray(new URL[0]), game);
        for (URL url : urls) {
            JarFile jar = new JarFile(new File(url.toURI()));
            try {
                JarEntry entry = jar.getJarEntry("orrin.mod");
                if (entry == null) {
                    continue;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry), "UTF-8"));
                String className = reader.readLine().trim();
                reader.close();
                OrrinMod mod = (OrrinMod) modLoader.loadClass(className).newInstance();
                System.out.println("placeholder text" + className);
                mod.onLaunch(game);
            } finally {
                jar.close();
            }
        }
        System.out.println("placeholder text");
        String version = System.getProperty("orrin.version", AlphaVersion.NIGHT);
        final String autoload = System.getProperty("orrin.autoload");
        if (autoload != null) {
            // Testing only: a few seconds after the game starts, open the named world by itself (what Singleplayer -> World does).
            Thread loader = new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(7000);
                        GameControl.openWorld(autoload);
                    } catch (Throwable t) {
                        System.out.println("placeholder text" + t);
                    }
                }
            }, "orrin-autoload");
            loader.setDaemon(true);
            loader.start();
        }
        System.out.println("placeholder text" + version + ": " + AlphaVersion.lore(version));
        // Once, a good while in, a fake crash report appears in a window of its own. The game is fine behind it; the window closes by itself.
        // Turn it off with -Word0943.fakecrash=off, or change when it shows with -Word0943.fakecrash=<seconds>.
        String when = System.getProperty("orrin.fakecrash", AlphaVersion.isAftermath(version) ? "900" : "540");
        if (!"off".equals(when)) {
            FakeCrash.schedule(version, Long.parseLong(when));
        }
        try {
            String tag = System.getProperty("orrin.version", AlphaVersion.NIGHT).replaceAll("[^0-9a-z]", "");
            Class.forName("OrrinSprites_" + tag).getMethod("register").invoke(null);
            System.out.println("placeholder text");
        } catch (Throwable t) {
            System.out.println("placeholder text" + t);
        }
        Class<?> minecraft = Class.forName("net.minecraft.client.Minecraft", true, game);
        Method main = minecraft.getMethod("main", String[].class);
        main.invoke(null, (Object) args);
    }
}
