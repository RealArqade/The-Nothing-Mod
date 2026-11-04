package com.realarqade.thenothingmod.client;

import com.realarqade.thenothingmod.DustDebug;
import com.realarqade.thenothingmod.ModBlocks;
import com.realarqade.thenothingmod.ServerSettings;
import com.realarqade.thenothingmod.StalkerProperties;
import com.realarqade.thenothingmod.SuspiciousDustProperties;
import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionFlag;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Color;

/**
 * The settings screen, built with YetAnotherConfigLib. Only loaded when that library is installed (see {@link DustConfig#createScreen}),
 * so loaders without it (Forge) simply have no settings screen and the file {@code config/the-nothing-mod.json} is edited by hand instead.
 */
final class DustConfigScreen {
    private DustConfigScreen() {
    }
    static Screen create(Screen parent) {
        return YetAnotherConfigLib.create(YaclConfigHandler.<DustConfig>classHandler(), (defaults, config, builder) -> {
            builder.title(Component.translatable("config.the-nothing-mod.title"))
                    .category(generalCategory(defaults, config));
            if (DustDevTools.hasDevControls()) {
                builder.category(devCategory());
            }
            builder.save(() -> {
                DustConfig.HANDLER.save();
                ServerSettings.save();
                if (DustDevTools.inDevelopmentEnvironment()) {
                    DustDevTools.bakeForRelease();
                }
                if (DustDevTools.hasDevControls()) {
                    DustDevTools.pushToServer();
                }
            });
            return builder;
        }).generateScreen(parent);
    }

    private static ConfigCategory generalCategory(DustConfig defaults, DustConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("config.the-nothing-mod.general"))
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.the-nothing-mod.high_res.name"))
                        .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.high_res.description")))
                        .binding(defaults.highResTextures, () -> config.highResTextures, value -> config.highResTextures = value)
                        .controller(TickBoxControllerBuilder::create)
                        .flag(OptionFlag.ASSET_RELOAD)
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.the-nothing-mod.show_disclaimer.name"))
                        .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.show_disclaimer.description")))
                        .binding(defaults.showDisclaimer, () -> config.showDisclaimer, value -> config.showDisclaimer = value)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.the-nothing-mod.Word0949.name"))
                        .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.Word0949.description")))
                        .binding(defaults.streamerMode, () -> config.streamerMode, value -> config.streamerMode = value)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.the-nothing-mod.tone_down.name"))
                        .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.tone_down.description")))
                        .binding(defaults.toneDownJumpscare, () -> config.toneDownJumpscare, value -> config.toneDownJumpscare = value)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.the-nothing-mod.orrin_start_a.name"))
                        .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.orrin_start_a.description")))
                        .action((screen, button) -> OrrinSetup.askToStart(screen, "a1.0.14"))
                        .build())
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.the-nothing-mod.orrin_start_b.name"))
                        .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.orrin_start_b.description")))
                        .action((screen, button) -> OrrinSetup.askToStart(screen, "a1.0.15"))
                        .build())
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.the-nothing-mod.orrin.name"))
                        .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.orrin.description")))
                        .action((screen, button) -> OrrinSetup.ask(screen))
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.the-nothing-mod.crashes.name"))
                        .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.crashes.description")))
                        .binding(defaults.intentionalCrashes, () -> config.intentionalCrashes, value -> config.intentionalCrashes = value)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.the-nothing-mod.desktop.name"))
                        .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.desktop.description")))
                        .binding(defaults.desktopFiles, () -> config.desktopFiles, value -> config.desktopFiles = value)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.the-nothing-mod.desktop_remove.name"))
                        .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.desktop_remove.description")))
                        .action((screen, button) -> DesktopFiles.removeAll())
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.the-nothing-mod.corruption.name"))
                        .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.corruption.description")))
                        .binding(defaults.fakeCorruption, () -> config.fakeCorruption, value -> config.fakeCorruption = value)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.the-nothing-mod.personal.name"))
                        .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.personal.description")))
                        .binding(defaults.personalDetails, () -> config.personalDetails, value -> config.personalDetails = value)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.the-nothing-mod.bluescreen.name"))
                        .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.bluescreen.description")))
                        .binding(defaults.bluescreen, () -> config.bluescreen, value -> config.bluescreen = value)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                // Server-side setting, kept in its own file so a dedicated server can use it too. In single player this edits it.
                // Deliberately no option for the death itself: only the kick can be turned off.
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.the-nothing-mod.kick.name"))
                        .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.kick.description")))
                        .binding(true, ServerSettings::kickPlayers, ServerSettings::setKickPlayers)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .build();
    }

    /**
     * Development only. Changes apply live when you press Save, and are written into the project's source
     * resources so they ship in the next build (see {@link DustDevTools#bakeForRelease()}).
     */
    private static ConfigCategory devCategory() {
        SuspiciousDustProperties dust = ModBlocks.DUST_PROPERTIES;

        Option<Color> color = Option.<Color>createBuilder()
                .name(Component.translatable("config.the-nothing-mod.dev.color.name"))
                .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.dev.color.description")))
                .binding(new Color(SuspiciousDustProperties.DEFAULT_COLOR), () -> new Color(dust.color()), value -> dust.setColor(value.getRGB()))
                .controller(option -> ColorControllerBuilder.create(option).allowAlpha(false))
                .flag(OptionFlag.RELOAD_CHUNKS)
                .build();

        // The colour of powered dust. Greyed out (and its reset button too) while redstone conductivity is off.
        Option<Color> poweredColor = Option.<Color>createBuilder()
                .name(Component.translatable("config.the-nothing-mod.dev.powered_color.name"))
                .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.dev.powered_color.description")))
                .binding(new Color(SuspiciousDustProperties.brighten(SuspiciousDustProperties.DEFAULT_COLOR)),
                        () -> new Color(dust.poweredColor()), value -> dust.setPoweredColor(value.getRGB()))
                .controller(option -> ColorControllerBuilder.create(option).allowAlpha(false))
                .available(dust.conductsRedstone())
                .flag(OptionFlag.RELOAD_CHUNKS)
                .build();

        ButtonOption resetPoweredColor = ButtonOption.createBuilder()
                .name(Component.translatable("config.the-nothing-mod.dev.reset_powered_color.name"))
                .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.dev.reset_powered_color.description")))
                .available(dust.conductsRedstone())
                // Based on the colour as currently shown in Name 0512, even if it isn't saved yet.
                .action((screen, button) -> poweredColor.requestSet(
                        new Color(SuspiciousDustProperties.brighten(color.pendingValue().getRGB() & 0xFFFFFF))))
                .build();

        Option<Boolean> powersDiagonally = Option.<Boolean>createBuilder()
                .name(Component.translatable("config.the-nothing-mod.dev.powers_diagonally.name"))
                .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.dev.powers_diagonally.description")))
                .binding(SuspiciousDustProperties.DEFAULT_POWERS_DIAGONALLY, dust::powersDiagonally, dust::setPowersDiagonally)
                .controller(TickBoxControllerBuilder::create)
                .available(dust.conductsRedstone())
                .build();

        Option<Boolean> conducts = Option.<Boolean>createBuilder()
                .name(Component.translatable("config.the-nothing-mod.dev.conducts.name"))
                .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.dev.conducts.description")))
                .binding(SuspiciousDustProperties.DEFAULT_CONDUCTS_REDSTONE, dust::conductsRedstone, dust::setConductsRedstone)
                .controller(TickBoxControllerBuilder::create)
                .flag(OptionFlag.RELOAD_CHUNKS)
                .listener((option, value) -> {
                    poweredColor.setAvailable(value);
                    resetPoweredColor.setAvailable(value);
                    powersDiagonally.setAvailable(value);
                })
                .build();

        return ConfigCategory.createBuilder()
                .name(Component.translatable("config.the-nothing-mod.dev"))
                .tooltip(Component.translatable("config.the-nothing-mod.dev.tooltip"))
                .option(color)
                .option(conducts)
                .option(poweredColor)
                .option(resetPoweredColor)
                .option(powersDiagonally)
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.the-nothing-mod.dev.diagonals.name"))
                        .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.dev.diagonals.description")))
                        .binding(SuspiciousDustProperties.DEFAULT_CONNECT_DIAGONALLY, dust::connectDiagonally, dust::setConnectDiagonally)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.the-nothing-mod.dev.log.name"))
                        .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.dev.log.description")))
                        .binding(false, () -> DustDebug.logConnections, value -> DustDebug.logConnections = value)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.the-nothing-mod.disable_quit.name"))
                        .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.disable_quit.description")))
                        .binding(true, () -> DustConfig.get().disableQuitButton, value -> DustConfig.get().disableQuitButton = value)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.the-nothing-mod.dev.jumpscare.name"))
                        .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.dev.jumpscare.description")))
                        .action((screen, button) -> Minecraft.getInstance().setScreenAndShow(new JumpscareScreen(StalkerProperties.JUMPSCARE_TICKS, screen)))
                        .build())
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.the-nothing-mod.dev.corruption.name"))
                        .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.dev.corruption.description")))
                        .action((screen, button) -> Minecraft.getInstance().setScreenAndShow(new CorruptionScreen()))
                        .build())
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.the-nothing-mod.dev.desktop.name"))
                        .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.dev.desktop.description")))
                        .action((screen, button) -> DesktopFiles.write(1))
                        .build())
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.the-nothing-mod.dev.bluescreen.name"))
                        .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.dev.bluescreen.description")))
                        .action((screen, button) -> BsodState.show(Minecraft.getInstance()))
                        .build())
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.the-nothing-mod.dev.reload.name"))
                        .description(OptionDescription.of(Component.translatable("config.the-nothing-mod.dev.reload.description")))
                        .action((screen, button) -> Minecraft.getInstance().reloadResourcePacks())
                        .build())
                .build();
    }
}

