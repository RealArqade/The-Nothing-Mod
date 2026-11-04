package com.realarqade.thenothingmod.client.fabric;

import com.realarqade.thenothingmod.client.DustConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Adds the config button in Mod Menu. */
public class DustModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return DustConfig::createScreen;
    }
}

