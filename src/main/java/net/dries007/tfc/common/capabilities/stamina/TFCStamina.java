/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

 package net.dries007.tfc.common.capabilities.stamina;

 import net.dries007.tfc.common.capabilities.food.TFCFoodData;
 import net.minecraft.nbt.CompoundTag;
 import net.minecraft.world.entity.player.Player;
 import net.minecraft.world.food.FoodData;
 import org.jetbrains.annotations.NotNull;
 import tictim.paraglider.api.Copy;
 import tictim.paraglider.api.Serde;
 import tictim.paraglider.api.movement.Movement;
 import tictim.paraglider.api.movement.PlayerState;
 import tictim.paraglider.api.stamina.Stamina;
 import tictim.paraglider.api.vessel.VesselContainer;
 import tictim.paraglider.config.Cfg;
 
 public class TFCStamina implements Stamina, Copy, Serde {
 
     private final Player player;
     private final VesselContainer vessels;
     private int stamina;
     private boolean depleted;
 
     public TFCStamina(@NotNull Player player) {
         this.player = player;
         this.vessels = VesselContainer.get(player);
         this.stamina = maxStamina();
     }
 
     @Override
     public int stamina() {
         return stamina;
     }
 
     @Override
     public void setStamina(int stamina) {
         this.stamina = stamina;
     }
 
     @Override
     public int maxStamina() {
         FoodData foodData = player.getFoodData();
 
         double staminaMultiplier = 1;
 
         if (foodData instanceof TFCFoodData tfcFoodData) {
             staminaMultiplier = Math.max(0.7, tfcFoodData.getHealthModifier());
         }
 
         int maxStamina = (int) (Cfg.get().maxStamina(vessels.staminaVessel()) * staminaMultiplier);
 
         return maxStamina;
     }
 
     @Override
     public boolean isDepleted() {
         return depleted;
     }
 
     @Override
     public void setDepleted(boolean depleted) {
         this.depleted = depleted;
     }
 
     @Override
     public void update(@NotNull Movement movement) {
         PlayerState state = movement.state();
         int recoveryDelay = movement.recoveryDelay();
         int newRecoveryDelay = recoveryDelay;
         int delta = movement.getActualStaminaDelta();
         if (delta < 0) {
             if (!isDepleted()) takeStamina(-delta, false, false);
         } else {
             if (recoveryDelay > 0) newRecoveryDelay--;
             else if (delta > 0) giveStamina(delta, false);
         }
         //noinspection DataFlowIssue
         newRecoveryDelay = Math.max(0, Math.max(newRecoveryDelay, state.recoveryDelay()));
         if (recoveryDelay != newRecoveryDelay) movement.setRecoveryDelay(newRecoveryDelay);
     }
 
     @Override
     public int giveStamina(int amount, boolean simulate) {
         if (amount <= 0) return 0;
         int staminaToGive = Math.min(amount, maxStamina() - this.stamina);
         if (staminaToGive <= 0) return 0;
         if (!simulate) this.stamina += staminaToGive;
         return staminaToGive;
     }
 
     @Override
     public int takeStamina(int amount, boolean simulate, boolean ignoreDepletion) {
         if (amount <= 0 || (isDepleted() && !ignoreDepletion)) return 0;
         int staminaToTake = Math.min(amount, this.stamina);
         if (staminaToTake <= 0) return 0;
         if (!simulate) this.stamina -= staminaToTake;
         return staminaToTake;
     }
 
     @Override
     public void copyFrom(@NotNull Object from) {
         if (!(from instanceof Stamina stamina)) return;
         this.stamina = stamina.stamina();
         this.depleted = stamina.isDepleted();
     }
 
     @Override
     public void read(@NotNull CompoundTag tag) {
         this.stamina = tag.getInt("stamina");
         this.depleted = tag.getBoolean("depleted");
     }
 
     @Override
     @NotNull
     public CompoundTag write() {
         CompoundTag tag = new CompoundTag();
         tag.putInt("stamina", stamina);
         tag.putBoolean("depleted", depleted);
         return tag;
     }
 }
 