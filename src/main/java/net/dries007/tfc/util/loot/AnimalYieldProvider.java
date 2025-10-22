/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.util.loot;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.LootNumberProviderType;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import net.dries007.tfc.common.entities.livestock.TFCAnimalProperties;

public class AnimalYieldProvider extends MinMaxProvider {
    public AnimalYieldProvider(NumberProvider min, NumberProvider max) {
        super(min, max);
    }

    @Override
    public float getFloat(LootContext context) {
        final Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (entity instanceof TFCAnimalProperties properties) {
            // max adjusted size is 80f
            return Mth.lerp(Mth.clampedMap(properties.getAdjustedSize(), 1f, 81f, 0f, 1f), min.getFloat(context), max.getFloat(context));
        }
        return min.getFloat(context);
    }

    @Override
    public LootNumberProviderType getType() {
        return TFCLoot.ANIMAL_YIELD.get();
    }
}
