/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

 package net.dries007.tfc.common.capabilities.stamina;

 import net.dries007.tfc.TerraFirmaCraft;
 import org.jetbrains.annotations.Nullable;
 import tictim.paraglider.api.plugin.ParagliderPlugin;
 import tictim.paraglider.api.stamina.StaminaFactory;
 import tictim.paraglider.api.stamina.StaminaPlugin;
 
 import java.util.logging.Logger;
 
 //@ParagliderPlugin
 public class TFCStaminaPlugin implements StaminaPlugin {
 
     public @Nullable StaminaFactory getStaminaFactory() {
         TerraFirmaCraft.LOGGER.info("Providing TFCStaminaFactory");
         //return new TFCStaminaFactory();
         return null;
     }
 }
 
