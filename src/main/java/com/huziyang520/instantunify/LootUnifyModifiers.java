package com.huziyang520.instantunify;

import com.mojang.serialization.Codec;

import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Registers the loot modifier codec that the {@code instantunify:unify} data file refers to.
 */
public final class LootUnifyModifiers {

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, InstantUnify.MOD_ID);

    private LootUnifyModifiers() {
    }

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register("unify", () -> LootUnifyModifier.CODEC);
        SERIALIZERS.register(modEventBus);
    }
}
