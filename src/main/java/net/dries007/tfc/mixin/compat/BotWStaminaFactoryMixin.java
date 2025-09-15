/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

 package net.dries007.tfc.mixin.compat;

 import net.dries007.tfc.TerraFirmaCraft;
 import net.dries007.tfc.common.capabilities.stamina.ServerTFCStamina;
 import net.minecraft.client.player.LocalPlayer;
 import net.minecraft.server.level.ServerPlayer;
 import net.minecraft.world.entity.player.Player;
 import org.spongepowered.asm.mixin.Mixin;
 import org.spongepowered.asm.mixin.injection.At;
 import org.spongepowered.asm.mixin.injection.Inject;
 import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
 import tictim.paraglider.api.stamina.Stamina;
 import tictim.paraglider.impl.stamina.BotWStaminaFactory;
 
 @Mixin(BotWStaminaFactory.class)
 public abstract class BotWStaminaFactoryMixin {
     @Inject(method = "createServerInstance", at = @At(value = "RETURN"), remap = false, cancellable = true)
     public void createServerInstance(ServerPlayer player, CallbackInfoReturnable<Stamina> cir) {
         TerraFirmaCraft.LOGGER.debug("Creating TFC stamina for player via Mixin {}", player.getName().getString());
         cir.setReturnValue(new ServerTFCStamina(player));
     }
 
     @Inject(method = "createRemoteInstance", at = @At(value = "RETURN"), remap = false, cancellable = true)
     public void createRemoteInstance(Player player, CallbackInfoReturnable<Stamina> cir) {
         TerraFirmaCraft.LOGGER.debug("Creating TFC stamina for player via Mixin {}", player.getName().getString());
         cir.setReturnValue(new ServerTFCStamina(player));
     }
 
     @Inject(method = "createLocalClientInstance", at = @At(value = "RETURN"), remap = false, cancellable = true)
     public void createLocalClientInstance(LocalPlayer player, CallbackInfoReturnable<Stamina> cir) {
         TerraFirmaCraft.LOGGER.debug("Creating TFC stamina for player via Mixin {}", player.getName().getString());
         cir.setReturnValue(new ServerTFCStamina(player));
     }
 }
 