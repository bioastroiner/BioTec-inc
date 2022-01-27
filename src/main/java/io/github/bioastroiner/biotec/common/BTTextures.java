package io.github.bioastroiner.biotec.common;

import io.github.bioastroiner.biotec.BioTecValues;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = BioTecValues.MODID, value = Side.CLIENT)
public class BTTextures {
    public static ResourceLocation HTMLTECH_CAPE;
    //public static SimpleOverlayRenderer LASER_INPUT;
    //public static SimpleOverlayRenderer LASER_OUTPUT;

    public static void preInit() {
        //LASER_INPUT = new SimpleOverlayRenderer("overlay/machine/overlay_laser_in");
        //LASER_OUTPUT = new SimpleOverlayRenderer("overlay/machine/overlay_laser_out");
        //HTMLTECH_CAPE = new ResourceLocation(MODID, "textures/htmltech_cape.png");
    }
}
