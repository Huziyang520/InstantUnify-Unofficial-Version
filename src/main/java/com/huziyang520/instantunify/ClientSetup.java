package com.huziyang520.instantunify;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client only entry point of the in-game screens.
 * <p>
 * The "Config" button of the mod list opens {@link ConfigHubScreen}, which leads to the loader's own settings
 * screen and to the list editor. The loader registers nothing for other mods, so this has to be registered here.
 */
public final class ClientSetup {

    private ClientSetup() {
    }

    public static void init(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> new ConfigHubScreen(parent, container));
    }
}
