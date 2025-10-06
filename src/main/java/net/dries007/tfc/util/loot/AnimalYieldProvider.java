/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.util.loot;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.LootNumberProviderType;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import net.dries007.tfc.common.entities.livestock.TFCAnimalProperties;

public class AnimalYieldProvider extends MinMaxProvider
{
    public AnimalYieldProvider(NumberProvider min, NumberProvider max)
    {
        super(min, max);
    }

    @Override
    public float getFloat(LootContext context)
    {
        final Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        final Player player = context.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER);
        if (entity instanceof TFCAnimalProperties properties) {
            float adjustedSize = properties.getGeneticSize();
            if (player != null) {
                // 1 attack damage is equivalent to 0.5 extra animal size in terms of drops. may need to be adjusted.
                adjustedSize += Mth.clampedMap(player.getAttributeValue(Attributes.ATTACK_DAMAGE), 0, 16, 0, 8);
            }

            final float familiarity = properties.getFamiliarity();
            // 0 -> 1 familiarity scaled to 0 -> 20f extra size
            adjustedSize += Mth.clampedMap(familiarity, 0f, 0.25f, 0f, 20f);

            if (properties.getUsesToElderly() > 0) {
                final float leftUses = 1f - properties.getUses() / properties.getUsesToElderly();
                adjustedSize += Mth.clampedMap(leftUses, 0f, 0.66f, 0f, 20f);
            }

            // max adjusted size is 80f
            final float scaledSize = Mth.clampedMap(adjustedSize, 1f, 81f, 0f, 1f);
            return Mth.lerp(scaledSize, min.getFloat(context), max.getFloat(context));
        }
        return min.getFloat(context);
    }

    @Override
    public LootNumberProviderType getType()
    {
        return TFCLoot.ANIMAL_YIELD.get();
    }
}
