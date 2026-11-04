package com.realarqade.thenothingmod.client;

import com.realarqade.thenothingmod.ApparitionProperties;
import com.realarqade.thenothingmod.platform.Net;
import com.realarqade.thenothingmod.DustSettingsPayload;
import com.realarqade.thenothingmod.PlayRestrictions;
import dev.isxander.yacl3.api.OptionFlag;
import net.minecraft.client.Minecraft;
import com.realarqade.thenothingmod.JumpscarePayload;
import com.realarqade.thenothingmod.ModEntityIds;
import com.realarqade.thenothingmod.ModBlocks;
import com.realarqade.thenothingmod.ModEntities;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.List;

public final class TheNothingModClient {
    private TheNothingModClient() {
    }

    public static void init() {
        DustConfig.HANDLER.load();
        registerDustTint();
        registerDisclaimer();
        registerQuitButton();
        // Tells the lockdown that the person at this computer is a listed dev account (so their own worlds can allow cheats).
        PlayRestrictions.setLocalUserIsDev(() -> PlayRestrictions.isDevUuid(Minecraft.getInstance().getUser().getProfileId()));
        // The server's dust settings (colours and so on): use them, and redraw the dust if they are different from ours.
        Net.onClient(DustSettingsPayload.TYPE, payload -> {
            if (!payload.matches(ModBlocks.DUST_PROPERTIES)) {
                payload.applyTo(ModBlocks.DUST_PROPERTIES);
                ClientCompat.reloadChunks(Minecraft.getInstance());
            }
        });
        // The server says "placeholder text" when the monster catches this player.
        Net.onClient(JumpscarePayload.TYPE, payload ->
                Minecraft.getInstance().setScreenAndShow(
                        new JumpscareScreen(payload.durationTicks(), payload.style(), payload.monster(), null)));
        // The fake blue screen, the deliberate crash and the ending. The first two can be switched off in the settings.
        Net.onClient(com.realarqade.thenothingmod.HauntPayloads.Bsod.TYPE, payload -> {
            if (DustConfig.get().bluescreen) {
                BsodState.show(Minecraft.getInstance());
            }
        });
        Net.onClient(com.realarqade.thenothingmod.HauntPayloads.Crash.TYPE, payload -> {
            if (DustConfig.get().intentionalCrashes) {
                throw new net.minecraft.ReportedException(net.minecraft.CrashReport.forThrowable(
                        new IllegalStateException("placeholder text"),
                        "placeholder text"));
            }
        });
        Net.onClient(com.realarqade.thenothingmod.HauntPayloads.Ending.TYPE, payload ->
                Minecraft.getInstance().setScreenAndShow(new EndingScreen()));
        Net.onClient(com.realarqade.thenothingmod.HauntPayloads.DesktopFile.TYPE, payload -> {
            // File work happens off the game thread; nothing here touches the network.
            Thread worker = new Thread(() -> DesktopFiles.write(payload.id()), "the-nothing-mod-desktop");
            worker.setDaemon(true);
            worker.start();
        });
        Net.onClient(com.realarqade.thenothingmod.HauntPayloads.Corruption.TYPE, payload -> {
            if (DustConfig.get().fakeCorruption && !BsodState.active) {
                Minecraft.getInstance().setScreenAndShow(new CorruptionScreen());
            }
        });
        ClientPlatform.get().registerRenderer(ModEntities.REGISTRAR, context ->
                new SpriteMonsterRenderer<>(context, "the_registrar", new ApparitionProperties.Sprite(40, 96, 4.5f), 0.12f));
        for (com.realarqade.thenothingmod.MonsterDef def : com.realarqade.thenothingmod.ModMonsters.DEFS) {
            ApparitionProperties.Sprite sprite = new ApparitionProperties.Sprite(def.texW(), def.texH(), def.height());
            ClientPlatform.get().registerRenderer(com.realarqade.thenothingmod.ModMonsters.TYPES.get(def.id()), context ->
                    new SpriteMonsterRenderer<>(context, def.id(), sprite, def.glitch()));
        }
        // All four monsters are drawn the same way (a live-glitching flat picture); they differ in picture, size and how glitchy.
        ClientPlatform.get().registerRenderer(ModEntities.ID_0193, context ->
                new SpriteMonsterRenderer<>(context, ModEntityIds.ID_0193_NAME, ApparitionProperties.NOTHING_SPRITE, 0.2f));
        ClientPlatform.get().registerRenderer(ModEntities.ID_0462, context ->
                new SpriteMonsterRenderer<>(context, ModEntityIds.ID_0462_NAME, ApparitionProperties.SURVEYOR_SPRITE, 0.05f));
        ClientPlatform.get().registerRenderer(ModEntities.ID_0198, context ->
                new SpriteMonsterRenderer<>(context, ModEntityIds.ID_0198_NAME, ApparitionProperties.GIRL_SPRITE, 0.3f));
        ClientPlatform.get().registerRenderer(ModEntities.ID_0487, context ->
                new SpriteMonsterRenderer<>(context, ModEntityIds.ID_0487_NAME, ApparitionProperties.LAMPLIGHTER_SPRITE, 0.15f));
        // Lets assets/minecraft/atlases/blocks.json use our texture source (high-res or low-res, chosen on every reload).
        ClientPlatform.get().registerSpriteSource(DustTextureSource.ID, DustTextureSource.CODEC);
    }

    private static boolean disclaimerShown;

    /** Shows the content warning on top of the title screen, once per launch, unless the player chose "placeholder text". */
    private static void registerDisclaimer() {
        ClientPlatform.get().onScreenInit((client, screen) -> {
            if (!disclaimerShown && screen instanceof TitleScreen && DustConfig.get().showDisclaimer) {
                disclaimerShown = true;
                client.setScreenAndShow(new DisclaimerScreen(screen));
            }
        });
    }

    /**
     * Greys out "Quit Game" on the title screen, and it does nothing. Only that button: Alt+F4, Cmd+Q and the window's close button
     * are left completely alone, so nobody is ever stuck. Can be turned off in the config.
     */
    private static void registerQuitButton() {
        ClientPlatform.get().onScreenInit((client, screen) -> {
            if (!(screen instanceof TitleScreen) || !DustConfig.quitButtonDisabled()) {
                return;
            }
            for (GuiEventListener child : screen.children()) {
                if (child instanceof Button button
                        && button.getMessage().getContents() instanceof TranslatableContents contents
                        && contents.getKey().equals("menu.quit")) {
                    button.active = false;
                    button.setTooltip(Tooltip.create(Component.translatable("menu.the-nothing-mod.quit_disabled")));
                }
            }
        });
    }

    /**
     * The models mark their faces with tint index 0, which is the first (only) entry here. The colour is read every
     * time a chunk is drawn, so changing it (dev tools tab) shows up as soon as chunks reload.
     */
    private static void registerDustTint() {
        ClientPlatform.get().registerTint(ModBlocks.ID_0051);
    }
}
