package com.mcmoddev.orespawn.crafttweaker;
import com.blamejared.crafttweaker.api.plugin.CraftTweakerPlugin;
import com.blamejared.crafttweaker.api.plugin.IBracketParserRegistrationHandler;
import com.blamejared.crafttweaker.api.plugin.ICommandRegistrationHandler;
import com.blamejared.crafttweaker.api.plugin.ICraftTweakerPlugin;
import com.blamejared.crafttweaker.api.plugin.IListenerRegistrationHandler;
import com.blamejared.crafttweaker.api.plugin.ILoaderRegistrationHandler;
import com.blamejared.crafttweaker.api.plugin.IScriptLoadSourceRegistrationHandler;
import com.blamejared.crafttweaker.api.plugin.IScriptRunModuleConfiguratorRegistrationHandler;
import com.blamejared.crafttweaker.api.zencode.scriptrun.IScriptRunModuleConfigurator;
import com.mcmoddev.orespawn.OreSpawn;

/*
TODO: Find docs on how to actually register a loader and work out the various @ZenClass/@ZenMethod requirements for
      making things work.
 */
@CraftTweakerPlugin(OreSpawn.MODID+":orespawn")
public class OreSpawnPlugin implements ICraftTweakerPlugin {
    @Override
    public void registerLoaders(final ILoaderRegistrationHandler handler) {}

    @Override
    public void initialize() {}
}
