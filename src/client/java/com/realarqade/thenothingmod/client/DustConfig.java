package com.realarqade.thenothingmod.client;

import com.realarqade.thenothingmod.platform.Platform;

import com.realarqade.thenothingmod.TheNothingMod;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import net.minecraft.client.gui.screens.Screen;


/**
 * Client settings, stored in {@code config/the-nothing-mod.json}, plus the config screen (reached through Mod Menu).
 * In a development environment the screen gets an extra "Dev tools" tab for live tuning; see {@link DustDevTools}.
 */
public class DustConfig {
    /** Loads and saves the settings: through YetAnotherConfigLib when it is installed, with plain Gson when it is not. */
    public static final ConfigHandler<DustConfig> HANDLER = ConfigHandler.create(DustConfig.class,
            Platform.get().configDir().resolve(TheNothingMod.MOD_ID + ".json"));

    /** Detailed 64x64 textures (default) or the plain 16x16 ones. Read on every resource reload, which saving this option triggers. */
    @SerialEntry
    public boolean highResTextures = true;

    /** Show the content warning each time the game starts. "Don't show again" on the warning screen turns this off. */
    @SerialEntry
    public boolean showDisclaimer = true;

    /**
     * Grey out the Quit Game button on the title screen. Only a dev user or a development environment can turn this off, from the Dev
     * tools tab: for everyone else this saved value is ignored (see {@link #quitButtonDisabled()}), so editing the file does nothing.
     * The window's close button, Alt+F4 and Cmd+Q work either way.
     */
    @SerialEntry
    public boolean disableQuitButton = true;

    /**
     * Word0946 Mode. Opt-in, off by default. It is meant to switch off anything that uses personal details about the player, so the
     * mod is safe to stream. It does nothing yet: no feature uses personal details so far, so there is nothing for it to switch off.
     * Whatever gets added later that does must check this first.
     */
    @SerialEntry
    public boolean streamerMode = false;

    /** Accessibility: no flashes, calmer shaking and static, and a much quieter sound in the jumpscare. */
    @SerialEntry
    public boolean toneDownJumpscare = false;

    /** Allows the rare, saved, deliberate game crash. Everyone can switch it off. */
    @SerialEntry
    public boolean intentionalCrashes = true;

    /** Allows the notes the mod may leave on the Desktop (plain text files; see DesktopFiles). */
    @SerialEntry
    public boolean desktopFiles = true;

    /** Allows the fake system-corruption effect inside the game. */
    @SerialEntry
    public boolean fakeCorruption = true;

    /** Allows effects to show details read from this computer (user name and so on), only to the player, only locally. See PersonalDetails. */
    @SerialEntry
    public boolean personalDetails = true;

    /** Allows the fake blue screen. Everyone can switch it off. */
    @SerialEntry
    public boolean bluescreen = true;

    public static DustConfig get() {
        return HANDLER.instance();
    }

    /** Whether the Quit Game button is greyed out. Normal players can't change this: the saved setting only counts for dev users and dev mode. */
    public static boolean quitButtonDisabled() {
        return !DustDevTools.hasDevControls() || get().disableQuitButton;
    }

    /** The settings screen, or null when there is no config library to build it with (the file can still be edited by hand). */
    public static Screen createScreen(Screen parent) {
        return ConfigHandler.hasScreenLibrary() ? DustConfigScreen.create(parent) : null;
    }
}
