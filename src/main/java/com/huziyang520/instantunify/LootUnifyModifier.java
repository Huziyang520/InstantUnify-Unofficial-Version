package com.huziyang520.instantunify;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/**
 * Forge global loot modifier that unifies the output of a loot table, which covers block drops, mob drops,
 * chest loot and fishing.
 * <p>
 * Registered through {@code data/instantunify/loot_modifiers/unify_loot.json} and listed in
 * {@code data/forge/loot_modifiers/global_loot_modifiers.json}. The modifier is always attached, the
 * {@code harvest} option is checked per generation so it can be toggled without reloading anything.
 */
public class LootUnifyModifier extends LootModifier {

    public static final Codec<LootUnifyModifier> CODEC = RecordCodecBuilder.create(
            instance -> codecStart(instance).apply(instance, LootUnifyModifier::new));

    public LootUnifyModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!InstantUnifyConfig.harvest()) {
            return generatedLoot;
        }
        for (int i = 0; i < generatedLoot.size(); i++) {
            Optional<ItemStack> replaced = ItemUnifier.replace(generatedLoot.get(i));
            if (replaced.isPresent()) {
                generatedLoot.set(i, replaced.get());
            }
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
