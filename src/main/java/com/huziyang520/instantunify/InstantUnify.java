package com.huziyang520.instantunify;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(InstantUnify.MOD_ID)
public class InstantUnify {

    public static final String MOD_ID = "instantunify";
    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    // NeoForge injects the mod event bus and this mod's container, the static accessors of the Forge versions
    // are gone.
    public InstantUnify(IEventBus modEventBus, ModContainer modContainer) {
        InstantUnifyConfig.register(modContainer);
        LootUnifyModifiers.register(modEventBus);
        modEventBus.register(new ConfigReloadEventHandler());

        IEventBus forgeBus = NeoForge.EVENT_BUS;
        forgeBus.register(new TagReloadEventHandler());
        forgeBus.register(new DropUnifyEventHandler());
        forgeBus.register(new DeathUnifyEventHandler());
        forgeBus.register(new InventoryUnifyEventHandler());
        forgeBus.register(new ContainerUnifyEventHandler());
        forgeBus.register(new UnifyCommand());

        // The in-game screen is an optional client-only integration.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init(modContainer);
        }
    }
}
