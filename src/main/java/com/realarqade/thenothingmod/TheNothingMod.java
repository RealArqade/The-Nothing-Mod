package com.realarqade.thenothingmod;

import com.realarqade.thenothingmod.platform.Platform;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The loader-independent start-up. Each loader's entrypoint sets {@link Platform} and then calls {@link #init()}. */
public final class TheNothingMod {
	public static final String MOD_ID = "the-nothing-mod";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private TheNothingMod() {
	}

	/** Registers all the content (blocks, items, entities). Loaders that only allow this inside their register event call it from there. */
	public static void registerContent() {
		ModBlocks.initialize();
		ModStoryBlocks.initialize();
		ModLoreItems.initialize();
		ModEntities.initialize();
		ModMonsters.initialize();
		ModItems.initialize();
	}

	/** Everything that is not content registration: events, networking, settings. Call {@link #registerContent()} separately. */
	public static void init() {
		MonsterEvents.register();
		StoryStructures.register();
		HauntPayloads.register();
		HauntEvents.register();
		Dimensions.register();
		VoidWorlds.register();
		SelfTest.registerIfRequested();
		Platform.get().registerClientbound(JumpscarePayload.TYPE, JumpscarePayload.CODEC);
		ServerSettings.load();
		StalkerEvents.register();
		DevSync.register();
		LOGGER.info("placeholder text");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
