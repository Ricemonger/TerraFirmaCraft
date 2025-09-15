/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

 package net.dries007.tfc.common.capabilities.stamina;

 import net.minecraft.client.player.LocalPlayer;
 import net.minecraft.server.level.ServerPlayer;
 import net.minecraft.world.entity.player.Player;
 import org.jetbrains.annotations.NotNull;
 import tictim.paraglider.api.stamina.Stamina;
 import tictim.paraglider.api.stamina.StaminaFactory;
 
 public class TFCStaminaFactory implements StaminaFactory {
     @Override
     public @NotNull Stamina createServerInstance(@NotNull ServerPlayer serverPlayer) {
         return new ServerTFCStamina(serverPlayer);
     }
 
     @Override
     public @NotNull Stamina createRemoteInstance(@NotNull Player player) {
         return new TFCStamina(player);
     }
 
     @Override
     public @NotNull Stamina createLocalClientInstance(@NotNull LocalPlayer localPlayer) {
         return new TFCStamina(localPlayer);
     }
 }
 