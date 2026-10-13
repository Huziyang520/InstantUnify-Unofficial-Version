package com.huziyang520.instantunify;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Global loot modifier that unifies the output of a loot table, which covers block drops, mob drops,
 * chest loot and fishing.
 * <p>
 * Registered through {@code data/instantunify/loot_modifiers/unify_loot.json} and listed in
 * {@code data/neoforge/loot_modifiers/global_loot_modifiers.json}. The modifier is always attached, the
 * {@code harvest} option is checked per generation so it can be toggled without reloading anything.
 * <p>
 * Since this loader generation the serializers are codecs: the field that used to be a {@code Codec} is a
 * {@link MapCodec}, which is what the global loot modifier registry holds now. The base class also carries a
 * priority, which decides the order the modifiers of one loot table are applied in.
 */
public class LootUnifyModifier extends LootModifier {

    public static final MapCodec<LootUnifyModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, LootUnifyModifier::new));

    public LootUnifyModifier(LootItemCondition[] conditions, int priority) {
        super(conditions, priority);
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
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
