/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blocks.plant.fruit;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import net.dries007.tfc.common.blockentities.BerryBushBlockEntity;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.IForgeBlockExtension;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.soil.FarmlandBlock;
import net.dries007.tfc.common.blocks.soil.HoeOverlayBlock;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.climate.ClimateRange;

/**
 * Spreading bushes have two parts: a bush block, which is a full block which can grow up to three blocks tall, and a cane block, which is a horizontal protrusion that can output from the sides of a bush block.
 * The cane can then turn into more bush blocks, spreading the plant and allowing it to climb up hills.
 * Both the cane and the bush block use the "stage" property from {@link SeasonalPlantBlock} to determine and limit their growth.
 * <p>
 * Spreading:
 * <ul>
 *   <li>Cane blocks always convert to bush blocks, if they can.</li>
 *   <li>Bush blocks can grow up to three blocks upwards, but can only spread canes to adjacent blocks, meaning a single berry bush has four directions to spread in.</li>
 *   <li>Stage 0 is for newly planted bushes. Stage 1 is for all bush blocks that are bushes and grown naturally. Advancing to stage 2 means the bush is mature, and won't spread anymore.</li>
 *   <li>This means an individual horizontal position can spread up to three blocks adjacent, *but* unless the bush is climbing a hill, most of those canes won't be able to grow into bushes, because they're on solid ground. Meaning natural bush spreading will eventually stop, as the bush will reach all stage 2, where it is unable to spread.</li>
 * </ul>
 * The player can harvest bush blocks, stage 2 for a guaranteed drop, all other stages for 1/2 chance.
 */
public class SpreadingBushBlock extends StationaryBerryBushBlock implements IForgeBlockExtension, IBushBlock, HoeOverlayBlock
{
    protected final Supplier<? extends Block> companion;
    protected final int maxHeight;

    public SpreadingBushBlock(ExtendedProperties properties, Supplier<? extends Item> productItem, Lifecycle[] stages, Supplier<? extends Block> companion, int maxHeight, Supplier<ClimateRange> climateRange)
    {
        super(properties, productItem, stages, climateRange);
        this.companion = companion;
        this.maxHeight = maxHeight;
        registerDefaultState(getStateDefinition().any().setValue(STAGE, 0));
    }

    public Block getCane()
    {
        return companion.get();
    }

    @Override
    public void onUpdate(Level level, BlockPos pos, BlockState state)
    {
        if (level.getBlockEntity(pos) instanceof BerryBushBlockEntity bush)
        {
            Lifecycle currentLifecycle = state.getValue(LIFECYCLE);
            Lifecycle expectedLifecycle = getLifecycleForCurrentMonth();
            // if we are not working with a plant that is or should be dormant
            if (!checkAndSetDormant(level, pos, state, currentLifecycle, expectedLifecycle))
            {
                // Otherwise, we do a month-by-month evaluation of how the bush should have grown.
                // We only do this up to a year. Why? Because eventually, it will have become dormant, and any 'progress' during that year would've been lost anyway because it would unconditionally become dormant.
                long deltaTicks = Math.min(bush.getTicksSinceBushUpdate(), Calendars.SERVER.getCalendarTicksInYear());
                long currentCalendarTick = Calendars.SERVER.getCalendarTicks();
                long nextCalendarTick = currentCalendarTick - deltaTicks;

                final BlockPos sourcePos = pos.below();
                final ClimateRange range = climateRange.get();
                final int hydration = getHydration(level, sourcePos, state);

                int monthsSpentDying = 0;
                do
                {
                    // This always runs at least once. It is called through random ticks, and calendar updates - although calendar updates will only call this if they've waited at least a day, or the average delta between random ticks.
                    // Otherwise it will just wait for the next random tick.

                    // Jump forward to nextTick.
                    // Advance both the stage (randomly, if the previous month was healthy), and lifecycle (if the at-the-time conditions were valid)
                    nextCalendarTick = Math.min(nextCalendarTick + Calendars.SERVER.getCalendarTicksInMonth(), currentCalendarTick);


                    float temperatureAtNextTick = Climate.getTemperature(level, pos, nextCalendarTick, Calendars.SERVER.getCalendarDaysInMonth());
                    Lifecycle lifecycleAtNextTick = getLifecycleForMonth(ICalendar.getMonthOfYear(nextCalendarTick, Calendars.SERVER.getCalendarDaysInMonth()));
                    if (range.checkBoth(hydration, temperatureAtNextTick, false))
                    {
                        if(currentLifecycle == Lifecycle.FLOWERING && Math.random() > 0.8f){
                            currentLifecycle = currentLifecycle.advanceTowards(lifecycleAtNextTick);
                        }
                    }
                    else
                    {
                        currentLifecycle = Lifecycle.DORMANT;
                    }

                    if (lifecycleAtNextTick != Lifecycle.DORMANT && currentLifecycle == Lifecycle.DORMANT)
                    {
                        monthsSpentDying++; // consecutive months spent where the conditions were invalid, but they shouldn't've been
                    }
                    else
                    {
                        monthsSpentDying = 0;
                    }

                } while (nextCalendarTick < currentCalendarTick);

                BlockState newState;

                if (mayDie(level, pos, state, monthsSpentDying))
                {
                    newState = getDeadState(state);
                }
                else
                {
                    // It's not dead! Now, perform the actual update over the time taken.
                    newState = growAndPropagate(level, pos, level.getRandom(), state.setValue(LIFECYCLE, currentLifecycle));
                }

                // And update the block
                if (state != newState)
                {
                    level.setBlock(pos, newState, 3);
                }
            }
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return state.getValue(STAGE) == 2 ? Shapes.block() : PLANT_SHAPE;
    }

    @Override
    protected BlockState getDeadState(BlockState state)
    {
        return TFCBlocks.DEAD_BERRY_BUSH.get().defaultBlockState().setValue(STAGE, state.getValue(STAGE));
    }

    @Override
    protected BlockState growAndPropagate(Level level, BlockPos pos, RandomSource random, BlockState state)
    {
        double growthMultiplier = TFCConfig.SERVER.globalFruitSaplingGrowthModifier.get() * 10;

        int randomInt = random.nextInt((int) growthMultiplier);

        if (!state.getValue(LIFECYCLE).active() || randomInt != 0) {
            // Only grow when active
            return state;
        }

        // Increment stage by one
        final int originalStage = state.getValue(STAGE);

        if (originalStage == 0)
        {
            // Stage 0 -> grow into stage 1
            return state.setValue(STAGE, 1);
        }
        if (originalStage == 1)
        {
            // Stage 1: either grow upwards, or attempt to grow a cane and move to stage 2
            // Grow a bush upwards
            final BlockPos abovePos = pos.above();
            if (level.isEmptyBlock(abovePos) && distanceToGround(level, pos, maxHeight) < maxHeight)
            {
                // Growing upwards grows at stage = 1, because stage = 0 is just newly planted bushes.
                level.setBlockAndUpdate(abovePos, state.setValue(STAGE, 1).setValue(LIFECYCLE, state.getValue(LIFECYCLE)));
                return state; // Stay in stage 1, if we only grew upwards.
            }

            if (random.nextBoolean())
            {
                // Optionally cause a cane to grow on an adjacent block
                final Direction offset = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                final BlockPos offsetPos = pos.relative(offset);
                if (level.isEmptyBlock(offsetPos))
                {
                    level.setBlockAndUpdate(offsetPos, companion.get().defaultBlockState().setValue(SpreadingCaneBlock.FACING, offset).setValue(LIFECYCLE, state.getValue(LIFECYCLE)));
                }
            }

            return state.setValue(STAGE, 2);
        }
        return state; // Stay at stage 2, and don't grow
    }

    @Override
    public void addHoeOverlayInfo(Level level, BlockPos pos, BlockState state, List<Component> text, boolean isDebug)
    {
        final BlockPos sourcePos = pos.below();
        final ClimateRange range = climateRange.get();

        text.add(FarmlandBlock.getHydrationTooltip(level, sourcePos, range, false));
        text.add(FarmlandBlock.getTemperatureTooltip(level, sourcePos, range, false));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        final BlockPos belowPos = pos.below();
        final BlockState belowState = level.getBlockState(belowPos);
        return mayPlaceOn(belowState, level, belowPos) || (belowState.getBlock() == this && belowState.getValue(STAGE) != 0);
    }
}
