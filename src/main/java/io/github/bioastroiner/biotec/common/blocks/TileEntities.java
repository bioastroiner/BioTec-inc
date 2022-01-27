package io.github.bioastroiner.biotec.common.blocks;

import gregtech.api.GTValues;
import gregtech.client.renderer.texture.Textures;
import gregtech.common.metatileentities.MetaTileEntities;
import io.github.bioastroiner.biotec.BioTec;
import io.github.bioastroiner.biotec.BioTecValues;
import net.minecraft.util.ResourceLocation;

public class TileEntities {
    public static MetaTileEntitySlaughterHouse[] SLAUGHTER_HOUSE = new MetaTileEntitySlaughterHouse[GTValues.V.length - 1];

    public static void init(){
        BioTec.logger.info("Registering BioTec Inc TEs");

        SLAUGHTER_HOUSE[0] = MetaTileEntities.registerMetaTileEntity(9100,new MetaTileEntitySlaughterHouse(location("slaughter_house.lv"), Textures.MACERATOR_OVERLAY,1));
        SLAUGHTER_HOUSE[1] = MetaTileEntities.registerMetaTileEntity(9101,new MetaTileEntitySlaughterHouse(location("slaughter_house.MV"), Textures.MACERATOR_OVERLAY,2));
        SLAUGHTER_HOUSE[2] = MetaTileEntities.registerMetaTileEntity(9102,new MetaTileEntitySlaughterHouse(location("slaughter_house.HV"), Textures.MACERATOR_OVERLAY,3));
    }

    public static ResourceLocation location(String name) {
        return new ResourceLocation(BioTecValues.MODID, name);
    }
}
