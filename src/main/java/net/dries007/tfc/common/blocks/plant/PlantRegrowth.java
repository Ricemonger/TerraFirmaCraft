/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

 package net.dries007.tfc.common.blocks.plant;

 import net.dries007.tfc.common.TFCTags;
 import net.dries007.tfc.common.blocks.TFCBlockStateProperties;
 import net.dries007.tfc.common.blocks.TFCBlocks;
 import net.dries007.tfc.common.blocks.crop.DeadCropBlock;
 import net.dries007.tfc.common.blocks.rock.LooseRockBlock;
 import net.dries007.tfc.common.fluids.FluidHelpers;
 import net.dries007.tfc.config.TFCConfig;
 import net.dries007.tfc.util.Helpers;
 import net.dries007.tfc.util.calendar.Calendars;
 import net.dries007.tfc.util.calendar.Season;
 import net.dries007.tfc.util.climate.Climate;
 import net.dries007.tfc.world.chunkdata.ChunkData;
 import net.minecraft.core.BlockPos;
 import net.minecraft.server.level.ServerLevel;
 import net.minecraft.server.level.ServerPlayer;
 import net.minecraft.util.Mth;
 import net.minecraft.util.RandomSource;
 import net.minecraft.world.level.ChunkPos;
 import net.minecraft.world.level.Level;
 import net.minecraft.world.level.block.Block;
 import net.minecraft.world.level.block.state.BlockState;
 import net.minecraft.world.level.levelgen.WorldgenRandom;
 import org.jetbrains.annotations.Nullable;
 
 import java.util.function.BiPredicate;
 
 import static net.dries007.tfc.common.blocks.plant.PlantBlock.AGE;
 
 public final class PlantRegrowth {
     public static final BiPredicate<BlockState, BlockPos> DEFAULT_PLACEMENT_TEST = (s, p) -> s.isAir() || s.getBlock() instanceof DeadCropBlock;
 
     public static boolean canSpread(Level level, RandomSource random) {
         return random.nextFloat() < TFCConfig.SERVER.plantSpreadChance.get() && Calendars.get(level).getCalendarMonthOfYear().getSeason() != Season.WINTER;
     }
 
     /**
      * @param selfSpreadRange the max distance the plant will attempt to spread
      * @param radius          the square radius that the plant will check for tagged plants to prevent over-densification
      * @param maxPlants       the max amount of plants within the radius that are allowed before spreading is denied
      * @return a {@linkplain BlockPos} if we have a place to put it.
      */
     @Nullable
     public static BlockPos spreadSelf(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, int selfSpreadRange, int radius, int maxPlants) {
         return spreadSelf(state, level, pos, random, selfSpreadRange, radius, maxPlants, (s, p) -> Helpers.isBlock(s, TFCTags.Blocks.PLANTS), DEFAULT_PLACEMENT_TEST);
     }
 
     @Nullable
     public static BlockPos spreadSelf(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, int selfSpreadRange, int radius, int maxPlants, BiPredicate<BlockState, BlockPos> occlusionTest, BiPredicate<BlockState, BlockPos> placementTest) {
         BlockPos newPos = pos.offset(Mth.nextInt(random, 0, selfSpreadRange), 0, Mth.nextInt(random, 0, selfSpreadRange));
         if (newPos.equals(pos)) {
             newPos = pos.offset(Mth.nextInt(random, 1, selfSpreadRange), 0, Mth.nextInt(random, 1, selfSpreadRange));
         }
         state = FluidHelpers.fillWithFluid(state, level.getFluidState(newPos).getType());
         if (state != null && placementTest.test(level.getBlockState(newPos), newPos) && state.canSurvive(level, newPos)) {
             int plants = 0;
             final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
             for (int x = -radius; x <= radius; x++) {
                 for (int z = -radius; z <= radius; z++) {
                     cursor.setWithOffset(newPos, x, 0, z);
                     if (!level.isLoaded(cursor)) {
                         return null;
                     }
                     if (occlusionTest.test(level.getBlockState(cursor), cursor)) {
                         if (plants++ > maxPlants) {
                             return null;
                         }
                     }
                 }
             }
             return newPos;
         }
         return null;
     }
 
     /**
      * See <a href="https://en.wikipedia.org/wiki/Granular_convection">This article</a>
      * <p>
      * "Picking stones is a never-ending labor on one of those New England farms. Pick as closely as you may, the next plowing turns up a
      * fresh eruption of boulders and pebbles, from the size of a hickory nut to that of a tea-kettle"
      * </p>
      */
     @SuppressWarnings("deprecation")
     public static void placeRisingRock(ServerLevel level, BlockPos pos, RandomSource random) {
         /*
         if (random.nextFloat() < TFCConfig.SERVER.grassSpawningRocksChance.get() * 10
             && Calendars.get(level).getCalendarMonthOfYear().getSeason() != Season.WINTER
             && Climate.getRainfall(level, pos) > 150
             && level.isAreaLoaded(pos, 6)) {
             final BlockState currentState = level.getBlockState(pos);
             final boolean isInAir = currentState.isAir();
             BlockState state;
             if (isInAir) {
                 Plant plant;
                 int randomGrass = random.nextInt(11);
                 switch (randomGrass) {
                     case 0 -> plant = Plant.BEACHGRASS;
                     case 1 -> plant = Plant.BLUEGRASS;
                     case 2 -> plant = Plant.BROMEGRASS;
                     case 3 -> plant = Plant.FOUNTAIN_GRASS;
                     case 4 -> plant = Plant.MANATEE_GRASS;
                     case 5 -> plant = Plant.ORCHARD_GRASS;
                     case 6 -> plant = Plant.RYEGRASS;
                     case 7 -> plant = Plant.SCUTCH_GRASS;
                     case 8 -> plant = Plant.STAR_GRASS;
                     case 9 -> plant = Plant.TIMOTHY_GRASS;
                     case 10 -> plant = Plant.RADDIA_GRASS;
                     default -> plant = Plant.FOUNTAIN_GRASS;
                 }
 
                 state =  ShortGrassBlock.create(plant,
                     ExtendedProperties.of(nonSolid(plant)).flammable(60, 30).offsetType(BlockBehaviour.OffsetType.XZ)).getPlant()
 
                 if (state != null) {
                     state = FluidHelpers.fillWithFluid(state, level.getFluidState(pos).getType());
                     if (state != null && state.canSurvive(level, pos)) {
                         level.setBlockAndUpdate(pos, state);
                     }
                 }
             }
         }
 
          */
 
         if (random.nextFloat() > TFCConfig.SERVER.grassSpawningRocksChance.get()
             || Calendars.SERVER.getCalendarMonthOfYear().getSeason() != Season.SPRING
             || Climate.getAverageTemperature(level, pos) > 12f
             || !level.isAreaLoaded(pos, 6)) {
             return;
         }
         final ChunkPos chunkPos = new ChunkPos(pos);
         if (WorldgenRandom.seedSlimeChunk(chunkPos.x, chunkPos.z, level.getSeed(), 6942069420L).nextInt(5) != 0) {
             return;
         }
         final BlockState currentState = level.getBlockState(pos);
         final boolean isInAir = currentState.isAir();
         final boolean isInSelf = currentState.getBlock() instanceof LooseRockBlock;
         if (!isInSelf && !isInAir) {
             return;
         }
 
         BlockState state;
         if (isInAir) {
             final ChunkData data = ChunkData.get(level, pos);
             state = data.getRockData().getRock(pos).loose().map(Block::defaultBlockState).orElse(null);
             if (state == null) {
                 return;
             }
             state = FluidHelpers.fillWithFluid(state, level.getFluidState(pos).getType());
             if (state == null || !state.canSurvive(level, pos)) {
                 return;
             }
         } else if (currentState.getValue(LooseRockBlock.COUNT) < 3) {
             state = currentState.cycle(LooseRockBlock.COUNT);
         } else {
             return;
         }
 
         final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
         for (int x = -2; x <= 2; x++) {
             for (int z = -2; z <= 2; z++) {
                 cursor.setWithOffset(pos, x, 0, z);
                 if (level.getBlockState(cursor).getBlock() instanceof LooseRockBlock) {
                     return;
                 }
             }
         }
         level.setBlockAndUpdate(pos, state);
     }
 
     public static void placeShortGrass(ServerLevel level, BlockPos pos, RandomSource random) {
         if (random.nextFloat() > TFCConfig.SERVER.plantSpreadChance.get()
             || !level.isAreaLoaded(pos, 6)) {
             return;
         }
         final ChunkPos chunkPos = new ChunkPos(pos);
         if (WorldgenRandom.seedSlimeChunk(chunkPos.x, chunkPos.z, level.getSeed(), 6942069420L).nextInt(5) != 0) {
             return;
         }
         final BlockState currentState = level.getBlockState(pos);
 
         BlockState state;
 
         if (currentState.isAir()) {
 
             int timothyOrFescue = random.nextInt(3);
 
             if (timothyOrFescue == 0) {
                 ShortGrassBlock grass = (ShortGrassBlock) TFCBlocks.PLANTS.get(Plant.TIMOTHY_GRASS).get();
                 state = grass.updateStateWithCurrentMonth(grass.defaultBlockState().setValue(AGE, 0));
             } else {
                 ShortGrassBlock grass = (ShortGrassBlock) TFCBlocks.PLANTS.get(Plant.TALL_FESCUE_GRASS).get();
                 state = grass.updateStateWithCurrentMonth(grass.defaultBlockState().setValue(AGE, 0));
             }
 
             state = FluidHelpers.fillWithFluid(state, level.getFluidState(pos).getType());
             if (state == null || !state.canSurvive(level, pos)) {
                 return;
             }
         } else {
             return;
         }
 
         level.setBlockAndUpdate(pos, state);
     }
 
     private static boolean hasPlayerNearby(ServerLevel level, BlockPos pos, final int range) {
         for (ServerPlayer player : level.players()) {
             if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < range * range) {
                 return true;
             }
         }
         return false;
     }
 }
 