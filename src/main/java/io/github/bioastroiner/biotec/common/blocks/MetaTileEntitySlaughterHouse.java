package io.github.bioastroiner.biotec.common.blocks;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.mojang.authlib.GameProfile;
import gregtech.api.GTValues;
import gregtech.api.capability.IControllable;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.NotifiableItemStackHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.SimpleMachineMetaTileEntity;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.unification.material.Materials;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.client.renderer.texture.Textures;
import io.github.bioastroiner.biotec.api.RecipeMaps;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static gregtech.api.capability.GregtechDataCodes.SYNC_TILE_MODE;

public class MetaTileEntitySlaughterHouse extends SimpleMachineMetaTileEntity implements IControllable {

    private final int outputAmount;

    private boolean lootingMode = false;
    private boolean isActive = false;
    private int lastTick;

    public MetaTileEntitySlaughterHouse(ResourceLocation metaTileEntityId, ICubeRenderer renderer, int tier) {
        super(metaTileEntityId, RecipeMaps.SLAUGHTER_HOUSE_RECIPES, renderer, tier, true);
        this.outputAmount = 20 + getTier() * 10;
        this.lastTick = 0;
        initializeInventory();
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return super.getRecipeMap();
    }

    @Override
    protected IItemHandlerModifiable createExportItemHandler() {
        return new NotifiableItemStackHandler(outputAmount, this, true);
    }

    @Override
    protected IItemHandlerModifiable createImportItemHandler() {
        return super.createImportItemHandler();
    }

    @Override
    protected FluidTankList createExportFluidHandler() {
        return new FluidTankList(true, new FluidTank(Materials.Methane.getFluid(0), 32000));
    }

//    @Override
//    protected FluidTankList createImportFluidHandler() {
//        return null;
//    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntitySlaughterHouse(metaTileEntityId, renderer, getTier());
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, @Nonnull List<String> tooltip,
                               boolean advanced) {
        tooltip.add(I18n.format("gregtech.universal.tooltip.voltage_in", energyContainer.getInputVoltage(),
                GTValues.VNF[getTier()]));
        tooltip.add(I18n.format("gregtech.universal.tooltip.energy_storage_capacity",
                energyContainer.getEnergyCapacity()));
        if (getTier() > GTValues.MV) {
            tooltip.add(I18n.format("gregtech.machine.slaughter_house.tooltip2"));
        } else {
            tooltip.add(I18n.format("gregtech.machine.slaughter_house.tooltip1"));
        }
    }

    @Override
    public void update() {
        super.update();
        if (!getWorld().isRemote) {
            int currentTick = FMLCommonHandler.instance().getMinecraftServerInstance().getTickCounter();
            if (currentTick != lastTick) {
                World world = getWorld();
                BlockPos currentPos = getPos();
                lastTick = currentTick;
                int area = getTier() * 2;
                doMobKill(world, currentPos, area);
            }
        }
    }

    private void doMobKill(World world, BlockPos currentPos, int area) {
        List<EntityMob> mobs =
                world.getEntitiesWithinAABB(EntityMob.class, new AxisAlignedBB(currentPos.add(-area, -area,
                        -area), currentPos.add(area, area, area)));
        isActive = !mobs.isEmpty();
        for (EntityMob m : mobs) {
            if (m.getMaxHealth() > GTValues.V[getTier()] - 5 * getTier()) continue;
            // TODO: probably use AT in future cause the loot table field is private, but for now we can
            //  access it via NBT data
            if (m.serializeNBT().hasKey("DeathLootTable") && m.serializeNBT().hasKey("DeathLootTableSeed")) {
                ResourceLocation resourcelocation;
                Long deathLootTableSeed = m.serializeNBT().getLong("DeathLootTableSeed");

                resourcelocation = new ResourceLocation(m.serializeNBT().getString("DeathLootTable"));
                LootTable loottable = m.world.getLootTableManager().getLootTableFromLocation(resourcelocation);

                LootContext.Builder lootcontext$builder =
                        (new LootContext.Builder((WorldServer) m.world)).withLootedEntity(m).withDamageSource(DamageSource.causePlayerDamage(FakePlayerFactory.get((WorldServer) m.world, new GameProfile(UUID.randomUUID(), UUID.randomUUID().toString()))));
                if (lootingMode) {
                    lootcontext$builder.withLuck(getTier() * 2f);
                } else {
                    lootcontext$builder.withLuck(getTier());
                }
                List<ItemStack> droppedLoot =
                        loottable.generateLootForPools(deathLootTableSeed == 0L ? world.rand :
                                new Random(deathLootTableSeed), lootcontext$builder.build());
                if (!addItemsToItemHandler(exportItems, true, droppedLoot))
                    continue; // skip this mob if it has more loot

                if (lootingMode)
                    this.energyContainer.removeEnergy(GTValues.V[getTier()] * getMaxInputOutputAmperage());
                else this.energyContainer.removeEnergy(GTValues.V[getTier()] * 2);
                m.setDropItemsWhenDead(false);
                m.setDead();
            }
        }
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        // TODO: these nuts are temporary ofc, replace with own overlays
        if (isLootingMode()) {
            Textures.MACERATOR_OVERLAY.renderOrientedState(renderState, translation, pipeline,
                    getFrontFacing(), isActive, isWorkingEnabled());
        } else {
            Textures.CUTTER_OVERLAY.renderOrientedState(renderState, translation, pipeline,
                    getFrontFacing(), isActive, isWorkingEnabled());
        }
    }

    @Override
    public boolean onScrewdriverClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
                                      CuboidRayTraceResult hitResult) {
        if (!getWorld().isRemote) {
            if (isLootingMode()) {
                setLootMode(false);
                playerIn.sendStatusMessage(new TextComponentTranslation("biotec.machine.grinder.mode_normal"), false);
            } else {
                setLootMode(true);
                playerIn.sendStatusMessage(new TextComponentTranslation("biotec.machine.grinder.mode_loot"), false);
            }
        }
        return true;
    }

    public void setLootMode(boolean inverted) {
        lootingMode = inverted;
        if (!getWorld().isRemote) {
            writeCustomData(SYNC_TILE_MODE, b -> b.writeBoolean(lootingMode));
            getHolder().notifyBlockUpdate();
            markDirty();
        }
    }

    public boolean isLootingMode() {
        return lootingMode;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setBoolean("LootingMode", lootingMode);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        lootingMode = data.getBoolean("LootingMode");
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeBoolean(lootingMode);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.lootingMode = buf.readBoolean();
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == SYNC_TILE_MODE) {
            this.lootingMode = buf.readBoolean();
            scheduleRenderUpdate();
        }
    }

    @Override
    public boolean isWorkingEnabled() {
        return true;
    }

    @Override
    public void setWorkingEnabled(boolean b) {

    }

//    @Override
//    public void setWorkingEnabled(boolean b) {
//        isPaused = !b;
//        getHolder().notifyBlockUpdate();
//    }
}
