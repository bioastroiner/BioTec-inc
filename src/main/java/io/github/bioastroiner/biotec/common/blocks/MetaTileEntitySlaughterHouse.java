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
import gregtech.api.util.GTFluidUtils;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.client.renderer.texture.Textures;
import gregtech.common.MetaFluids;
import net.minecraft.client.Minecraft;
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
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

import static gregtech.api.capability.GregtechDataCodes.IS_WORKING;
import static gregtech.api.capability.GregtechDataCodes.SYNC_TILE_MODE;

public class MetaTileEntitySlaughterHouse extends SimpleMachineMetaTileEntity implements IControllable {

    private final int outputAmount;

    private final long energyPerTick;
    private final int speed;

    private boolean lootingMode = false;
    private boolean isActive = false;
    private boolean isPaused = false;
    private int lastTick;
    private Supplier<Iterable<BlockPos.MutableBlockPos>> range;

    public MetaTileEntitySlaughterHouse(ResourceLocation metaTileEntityId, RecipeMap<?> recipeMap, ICubeRenderer renderer, int tier) {
        super(metaTileEntityId, recipeMap, renderer, tier, true);
        this.outputAmount = 20 + getTier()*10;
        this.energyPerTick = GTValues.V[tier] / 4; // uses a quarter of an amp per tick, and even less while idle
        this.lastTick = 0;
        this.speed = (int) Math.pow(2, tier);
        initializeInventory();
    }

    @Override
    protected IItemHandlerModifiable createExportItemHandler() {
        return new NotifiableItemStackHandler(outputAmount, this, true);
    }

    @Override
    protected IItemHandlerModifiable createImportItemHandler() {
        return null;
    }

    @Override
    protected FluidTankList createExportFluidHandler() {
        return new FluidTankList(true,new FluidTank(Materials.Methane.getFluid(0),32000));
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntitySlaughterHouse(metaTileEntityId, workable.getRecipeMap(), renderer, getTier());
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, @Nonnull List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gregtech.universal.tooltip.voltage_in", energyContainer.getInputVoltage(), GTValues.VNF[getTier()]));
        tooltip.add(I18n.format("gregtech.universal.tooltip.energy_storage_capacity", energyContainer.getEnergyCapacity()));
        if (getTier() > GTValues.MV) {
            tooltip.add(I18n.format("gregtech.machine.slaughter_house.tooltip2"));
        } else {
            tooltip.add(I18n.format("gregtech.machine.slaughter_house.tooltip1"));
        }
    }

    @Override
    public void update() {
        super.update();
        if(!getWorld().isRemote){
            if (isPaused) {
                if (isActive) {
                    setActive(false);
                }
                return;
            }
            if (energyContainer.getEnergyStored() < energyPerTick) {
                if (isActive) {
                    setActive(false);
                }
                return;
            }
            if (!isActive) {
                setActive(true);
            }
            int currentTick = FMLCommonHandler.instance().getMinecraftServerInstance().getTickCounter();
            if(currentTick != lastTick){
                World world = getWorld();
                BlockPos currentPos = getPos();
                lastTick = currentTick;
                energyContainer.removeEnergy(energyPerTick/2);
                if (range == null) {
                    int area = getTier() * 2;
                    //range = () -> BlockPos.getAllInBoxMutable(currentPos.add(-area, -area, -area), currentPos.add(area, area, area));
                    List<EntityMob> mobs =
                    world.getEntitiesWithinAABB(EntityMob.class, new AxisAlignedBB(currentPos.add(-area, -area, -area), currentPos.add(area, area, area)));
                    for (EntityMob m:mobs) {
                        if(m.getMaxHealth() > GTValues.V[getTier()] - 5*getTier()) continue;
                        // TODO: probably use AT in future cause the loot table field is private, but for now we can access it via NBT data
                        if(m.serializeNBT().hasKey("DeathLootTable") && m.serializeNBT().hasKey("DeathLootTableSeed")){
                            ResourceLocation resourcelocation;
                            Long deathLootTableSeed = m.serializeNBT().getLong("DeathLootTableSeed");

                            resourcelocation = new ResourceLocation(m.serializeNBT().getString("DeathLootTable"));
                            LootTable loottable = m.world.getLootTableManager().getLootTableFromLocation(resourcelocation);

                            LootContext.Builder lootcontext$builder = (new LootContext.Builder((WorldServer)m.world)).withLootedEntity(m).withDamageSource(DamageSource.causePlayerDamage(FakePlayerFactory.get((WorldServer) m.world, new GameProfile(UUID.randomUUID(),UUID.randomUUID().toString()))));
                            if(lootingMode){
                                lootcontext$builder.withLuck(getTier()*2f);
                            }
                            else {
                                lootcontext$builder.withLuck(getTier());
                            }
                            List<ItemStack> droppedLoot =
                            loottable.generateLootForPools(deathLootTableSeed == 0L ? world.rand : new Random(deathLootTableSeed), lootcontext$builder.build());
                            if(!addItemsToItemHandler(exportItems,true,droppedLoot)) continue; // skip this mob if it has more loot
                            
                            if(lootingMode) this.energyContainer.removeEnergy(GTValues.V[getTier()]*getMaxInputOutputAmperage());
                            m.setDropItemsWhenDead(false);
                            m.onKillCommand();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        if (isTEMode()) {
            Textures.WORLD_ACCELERATOR_TE_OVERLAY.renderOrientedState(renderState, translation, pipeline, getFrontFacing(), isActive, isWorkingEnabled());
        } else {
            Textures.WORLD_ACCELERATOR_OVERLAY.renderOrientedState(renderState, translation, pipeline, getFrontFacing(), isActive, isWorkingEnabled());
        }
    }

    @Override
    public boolean onScrewdriverClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
        if (!getWorld().isRemote) {
            if (isTEMode()) {
                setTEMode(false);
                playerIn.sendStatusMessage(new TextComponentTranslation("gregtech.machine.world_accelerator.mode_entity"), false);
            } else {
                setTEMode(true);
                playerIn.sendStatusMessage(new TextComponentTranslation("gregtech.machine.world_accelerator.mode_tile"), false);
            }
        }
        return true;
    }

    public void setTEMode(boolean inverted) {
        lootingMode = inverted;
        if (!getWorld().isRemote) {
            writeCustomData(SYNC_TILE_MODE, b -> b.writeBoolean(lootingMode));
            getHolder().notifyBlockUpdate();
            markDirty();
        }
    }

    public boolean isTEMode() {
        return lootingMode;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setBoolean("LootingMode", lootingMode);
        data.setBoolean("isPaused", isPaused);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        lootingMode = data.getBoolean("LootingMode");
        isPaused = data.getBoolean("isPaused");
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeBoolean(lootingMode);
        buf.writeBoolean(isPaused);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.lootingMode = buf.readBoolean();
        this.isPaused = buf.readBoolean();
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == IS_WORKING) {
            this.isActive = buf.readBoolean();
            scheduleRenderUpdate();
        }
        if (dataId == SYNC_TILE_MODE) {
            this.lootingMode = buf.readBoolean();
            scheduleRenderUpdate();
        }
    }

    protected void setActive(boolean active) {
        this.isActive = active;
        markDirty();
        if (!getWorld().isRemote) {
            writeCustomData(IS_WORKING, buf -> buf.writeBoolean(active));
        }
    }

    @Override
    public boolean isWorkingEnabled() {
        return !isPaused;
    }

    @Override
    public void setWorkingEnabled(boolean b) {
        isPaused = !b;
        getHolder().notifyBlockUpdate();
    }
}
