package io.github.bioastroiner.biotec;

import io.github.bioastroiner.biotec.common.blocks.MetaBlocks;
import io.github.bioastroiner.biotec.common.item.BTMetaItems;
import io.github.bioastroiner.biotec.proxy.CommonProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

import static io.github.bioastroiner.biotec.BioTecValues.*;

@Mod(modid = MODID, name = NAME, version = VERSION,dependencies = "required-after:gregtech@[2.0,);")
public class BioTec
{
    private static Logger logger;

    @SidedProxy(modId = MODID, serverSide = "io.github.bioastroiner.biotec.proxy.CommonProxy", clientSide = "io.github.bioastroiner.biotec.proxy.ClientProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        //TileEntities.init();
        MetaBlocks.init();
        BTMetaItems.init();
        proxy.preLoad();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }
}
