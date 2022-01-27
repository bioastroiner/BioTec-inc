package io.github.bioastroiner.biotec.proxy;

import io.github.bioastroiner.biotec.common.BTTextures;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.UUID;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
//        for (BlockLaserPipe pipe : LASER_PIPES) {
//            ModelLoader.setCustomStateMapper(pipe, new DefaultStateMapper() {
//                @Nonnull
//                @Override
//                protected ModelResourceLocation getModelResourceLocation(@Nonnull IBlockState state) {
//                    return LaserPipeRenderer.MODEL_LOCATION;
//                }
//            });
//            ModelLoader.setCustomMeshDefinition(Item.getItemFromBlock(pipe), stack -> LaserPipeRenderer
//            .MODEL_LOCATION);
//        }
    }

    @SubscribeEvent
    public static void onPlayerRender(RenderPlayerEvent.Pre event) {
        AbstractClientPlayer clientPlayer = (AbstractClientPlayer) event.getEntityPlayer();
        UUID capedUUID = UUID.fromString("5d7073e3-882f-4c4a-94b3-0e5ba1c11e02");
//        if (capedUUID.equals(clientPlayer.getUniqueID()) && clientPlayer.hasPlayerInfo() && clientPlayer.getLocationCape() == null) {
//            NetworkPlayerInfo playerInfo = ObfuscationReflectionHelper.getPrivateValue(AbstractClientPlayer.class,
//                    clientPlayer, 0);
//            playerTextures.put(MinecraftProfileTexture.Type.CAPE, BTTextures.HTMLTECH_CAPE);
//        }
    }

    @Override
    public void preLoad() {
        super.preLoad();
        BTTextures.preInit();
        //LaserPipeRenderer.preInit();
    }
}
