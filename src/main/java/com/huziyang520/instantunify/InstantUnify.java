package com.huziyang520.instantunify;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(InstantUnify.MOD_ID)
public class InstantUnify {

    public static final String MOD_ID = "instantunify";
    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    // The static accessors are deprecated for removal in later Forge versions, but they are the API this
    // Minecraft version offers; the constructor injection used on newer versions does not exist yet here.
    @SuppressWarnings("removal")
    public InstantUnify() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        InstantUnifyConfig.register();
        LootUnifyModifiers.register(modEventBus);
        modEventBus.register(new ConfigReloadEventHandler());

        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.register(new TagReloadEventHandler());
        forgeBus.register(new DropUnifyEventHandler());
        forgeBus.register(new DeathUnifyEventHandler());
        forgeBus.register(new InventoryUnifyEventHandler());
        forgeBus.register(new ContainerUnifyEventHandler());
        forgeBus.register(new UnifyCommand());

        // The in-game screen is an optional client-only integration.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init();
        }
    }
}
