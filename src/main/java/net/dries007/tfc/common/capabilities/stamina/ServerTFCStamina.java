/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

 package net.dries007.tfc.common.capabilities.stamina;

 import net.minecraft.world.entity.player.Player;
 import org.jetbrains.annotations.NotNull;
 import tictim.paraglider.api.movement.Movement;
 import tictim.paraglider.impl.movement.ServerPlayerMovement;
 
 public class ServerTFCStamina extends TFCStamina{
 
     public ServerTFCStamina(@NotNull Player player) {
         super(player);
     }
 
     @Override public void update(@NotNull Movement movement){
         boolean wasDepleted = isDepleted();
         super.update(movement);
         if(isDepleted()){
             if(stamina()>=maxStamina()){
                 setDepleted(false);
                 if(movement instanceof ServerPlayerMovement spm) spm.markMovementChanged();
             }
         }else if(stamina()<=0){
             setDepleted(true);
             if(movement instanceof ServerPlayerMovement spm){
                 spm.resetPanicParaglidingState();
                 spm.markMovementChanged();
             }
         }
         if(wasDepleted!=isDepleted()&&movement instanceof ServerPlayerMovement spm) spm.markMovementChanged();
     }
 }
 