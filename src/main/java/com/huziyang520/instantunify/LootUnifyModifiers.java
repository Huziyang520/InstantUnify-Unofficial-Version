package com.huziyang520.instantunify;

import com.mojang.serialization.MapCodec;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Registers the loot modifier codec that the {@code instantunify:unify} data file refers to.
 */
public final class LootUnifyModifiers {

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, InstantUnify.MOD_ID);

    private LootUnifyModifiers() {
    }

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register("unify", () -> LootUnifyModifier.CODEC);
        SERIALIZERS.register(modEventBus);
    }
}
