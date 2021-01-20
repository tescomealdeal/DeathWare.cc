package me.zeroeightsix.kami.module.modules.combat;

import com.mojang.realmsclient.gui.ChatFormatting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import me.zeroeightsix.kami.command.Command;
import me.zeroeightsix.kami.event.events.PacketEvent;
import me.zeroeightsix.kami.event.events.RenderEvent;
import me.zeroeightsix.kami.module.Module;
import me.zeroeightsix.kami.module.ModuleManager;
import me.zeroeightsix.kami.module.modules.combat.AutoGG;
import me.zeroeightsix.kami.setting.Setting;
import me.zeroeightsix.kami.setting.Settings;
import me.zeroeightsix.kami.util.BlockInteractionHelper;
import me.zeroeightsix.kami.util.EntityUtil;
import me.zeroeightsix.kami.util.Friends;
import me.zeroeightsix.kami.util.KamiTessellator;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.util.CombatRules;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;

@Module.Info(name="CrystalAura", category=Module.Category.COMBAT)
public class CrystalAura
        extends Module {
    private static boolean togglePitch = false;
    private static boolean isSpoofingAngles;
    private static double yaw;
    private static double pitch;
    private Setting<Boolean> place = this.register(Settings.b("Place", true));
    private Setting<Boolean> explode = this.register(Settings.b("Explode", true));
    private Setting<Boolean> autoSwitch = this.register(Settings.b("Auto Switch", true));
    private Setting<Boolean> antiWeakness = this.register(Settings.b("Anti Weakness", true));
    private Setting<Integer> hitTickDelay = this.register(Settings.integerBuilder("Hit Delay").withMinimum(0).withValue(4).withMaximum(20).build());
    private Setting<Double> hitRange = this.register(Settings.doubleBuilder("Hit Range").withMinimum(0.0).withValue(5.5).build());
    private Setting<Double> placeRange = this.register(Settings.doubleBuilder("Place Range").withMinimum(0.0).withValue(3.5).build());
    private Setting<Double> minDamage = this.register(Settings.doubleBuilder("Min Damage").withMinimum(0.0).withValue(2.0).withMaximum(20.0).build());
    private Setting<Boolean> spoofRotations = this.register(Settings.b("Spoof Rotations", false));
    private Setting<Boolean> rayTraceHit = this.register(Settings.b("RayTraceHit", false));
    private Setting<RenderMode> renderMode = this.register(Settings.e("Render Mode", RenderMode.UP));
    private Setting<Integer> red = this.register(Settings.integerBuilder("Red").withMinimum(0).withValue(104).withMaximum(255).build());
    private Setting<Integer> green = this.register(Settings.integerBuilder("Green").withMinimum(0).withValue(12).withMaximum(255).build());
    private Setting<Integer> blue = this.register(Settings.integerBuilder("Blue").withMinimum(0).withValue(35).withMaximum(255).build());
    private Setting<Integer> alpha = this.register(Settings.integerBuilder("Alpha").withMinimum(0).withValue(169).withMaximum(255).build());
    private Setting<Boolean> announceUsage = this.register(Settings.b("Announce Usage", true));
    private BlockPos renderBlock;
    private EntityPlayer target;
    private boolean switchCooldown = false;
    private boolean isAttacking = false;
    private int oldSlot = -1;
    private int newSlot;
    private int hitDelayCounter;
    @EventHandler
    private Listener<PacketEvent.Send> packetListener = new Listener<PacketEvent.Send>(event -> {
        if (!this.spoofRotations.getValue().booleanValue()) {
            return;
        }
        Packet packet = event.getPacket();
        if (packet instanceof CPacketPlayer && isSpoofingAngles) {
            ((CPacketPlayer)packet).yaw = (float)yaw;
            ((CPacketPlayer)packet).pitch = (float)pitch;
        }
    }, new Predicate[0]);

    public static BlockPos getPlayerPos() {
        return new BlockPos(Math.floor(CrystalAura.mc.player.posX), Math.floor(CrystalAura.mc.player.posY), Math.floor(CrystalAura.mc.player.posZ));
    }

    static float calculateDamage(double posX, double posY, double posZ, Entity entity) {
        float doubleExplosionSize = 12.0f;
        double distancedsize = entity.getDistance(posX, posY, posZ) / (double)doubleExplosionSize;
        Vec3d vec3d = new Vec3d(posX, posY, posZ);
        double blockDensity = entity.world.getBlockDensity(vec3d, entity.getEntityBoundingBox());
        double v = (1.0 - distancedsize) * blockDensity;
        float damage = (int)((v * v + v) / 2.0 * 7.0 * (double)doubleExplosionSize + 1.0);
        double finald = 1.0;
        if (entity instanceof EntityLivingBase) {
            finald = CrystalAura.getBlastReduction((EntityLivingBase)entity, CrystalAura.getDamageMultiplied(damage), new Explosion(CrystalAura.mc.world, null, posX, posY, posZ, 6.0f, false, true));
        }
        return (float)finald;
    }

    public static float calculateDamage(EntityEnderCrystal crystal, Entity entity) {
        return calculateDamage(crystal.posX, crystal.posY, crystal.posZ, entity);
    }

    private static float getBlastReduction(EntityLivingBase entity, float damage, Explosion explosion) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer ep = (EntityPlayer)entity;
            DamageSource ds = DamageSource.causeExplosionDamage(explosion);
            damage = CombatRules.getDamageAfterAbsorb(damage, ep.getTotalArmorValue(), (float)ep.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue());
            int k = EnchantmentHelper.getEnchantmentModifierDamage(ep.getArmorInventoryList(), ds);
            float f = MathHelper.clamp(k, 0.0f, 20.0f);
            damage *= 1.0f - f / 25.0f;
            if (entity.isPotionActive(MobEffects.RESISTANCE)) {
                damage -= damage / 4.0f;
            }
            return damage;
        }
        damage = CombatRules.getDamageAfterAbsorb(damage, entity.getTotalArmorValue(), (float)entity.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue());
        return damage;
    }

    private static float getDamageMultiplied(float damage) {
        int diff = CrystalAura.mc.world.getDifficulty().getId();
        return damage * (diff == 0 ? 0.0f : (diff == 2 ? 1.0f : (diff == 1 ? 0.5f : 1.5f)));
    }

    private static void setYawAndPitch(float yaw1, float pitch1) {
        yaw = yaw1;
        pitch = pitch1;
        isSpoofingAngles = true;
    }

    private static void resetRotation() {
        if (isSpoofingAngles) {
            yaw = CrystalAura.mc.player.rotationYaw;
            pitch = CrystalAura.mc.player.rotationPitch;
            isSpoofingAngles = false;
        }
    }

    @Override
    public void onWorldRender(RenderEvent event) {
        if (this.renderBlock != null && !this.renderMode.getValue().equals((Object)RenderMode.NONE)) {
            this.drawBlock(this.renderBlock, this.red.getValue(), this.green.getValue(), this.blue.getValue());
        }
    }

    private void drawBlock(BlockPos blockPos, int r, int g, int b) {
        Color color = new Color(r, g, b, this.alpha.getValue());
        KamiTessellator.prepare(7);
        if (this.renderMode.getValue().equals((Object)RenderMode.UP)) {
            KamiTessellator.drawBox(blockPos, color.getRGB(), 2);
        } else if (this.renderMode.getValue().equals((Object)RenderMode.BLOCK)) {
            KamiTessellator.drawBox(blockPos, color.getRGB(), 63);
        }
        KamiTessellator.release();
    }

    @Override
    public void onUpdate() {
        int crystalSlot;
        if (CrystalAura.mc.player == null) {
            return;
        }
        EntityEnderCrystal crystal = CrystalAura.mc.world.loadedEntityList.stream().filter(entity -> entity instanceof EntityEnderCrystal).map(entity -> (EntityEnderCrystal)entity).min(Comparator.comparing(c -> Float.valueOf(CrystalAura.mc.player.getDistance((Entity)c)))).orElse(null);
        if (this.explode.getValue().booleanValue() && crystal != null && (double)CrystalAura.mc.player.getDistance(crystal) <= this.hitRange.getValue() && this.rayTraceHitCheck(crystal)) {
            if (this.hitDelayCounter < this.hitTickDelay.getValue()) {
                ++this.hitDelayCounter;
                return;
            }
            this.hitDelayCounter = 0;
            if (this.antiWeakness.getValue().booleanValue() && CrystalAura.mc.player.isPotionActive(MobEffects.WEAKNESS)) {
                if (!this.isAttacking) {
                    this.oldSlot = CrystalAura.mc.player.inventory.currentItem;
                    this.isAttacking = true;
                }
                this.newSlot = -1;
                for (int i = 0; i < 9; ++i) {
                    ItemStack stack = CrystalAura.mc.player.inventory.getStackInSlot(i);
                    if (stack == ItemStack.EMPTY) continue;
                    if (stack.getItem() instanceof ItemSword) {
                        this.newSlot = i;
                        break;
                    }
                    if (!(stack.getItem() instanceof ItemTool)) continue;
                    this.newSlot = i;
                    break;
                }
                if (this.newSlot != -1) {
                    CrystalAura.mc.player.inventory.currentItem = this.newSlot;
                    this.switchCooldown = true;
                }
            }
            this.lookAtPacket(crystal.posX, crystal.posY, crystal.posZ, CrystalAura.mc.player);
            CrystalAura.mc.playerController.attackEntity(CrystalAura.mc.player, crystal);
            CrystalAura.mc.player.swingArm(EnumHand.MAIN_HAND);
            return;
        }
        CrystalAura.resetRotation();
        if (this.oldSlot != -1) {
            CrystalAura.mc.player.inventory.currentItem = this.oldSlot;
            this.oldSlot = -1;
        }
        this.isAttacking = false;
        int n = crystalSlot = CrystalAura.mc.player.getHeldItemMainhand().getItem() == Items.END_CRYSTAL ? CrystalAura.mc.player.inventory.currentItem : -1;
        if (crystalSlot == -1) {
            for (int l = 0; l < 9; ++l) {
                if (CrystalAura.mc.player.inventory.getStackInSlot(l).getItem() != Items.END_CRYSTAL) continue;
                crystalSlot = l;
                break;
            }
        }
        boolean offhand = false;
        if (CrystalAura.mc.player.getHeldItemOffhand().getItem() == Items.END_CRYSTAL) {
            offhand = true;
        } else if (crystalSlot == -1) {
            return;
        }
        List<EntityPlayer> entities = CrystalAura.mc.world.playerEntities.stream().filter(entityPlayer -> !Friends.isFriend(entityPlayer.getName())).sorted((entity1, entity2) -> Float.compare(CrystalAura.mc.player.getDistance((Entity)entity1), CrystalAura.mc.player.getDistance((Entity)entity2))).collect(Collectors.toList());
        List<BlockPos> blocks = this.findCrystalBlocks();
        BlockPos targetBlock = null;
        double targetBlockDamage = 0.0;
        this.target = null;
        for (Entity entity3 : entities) {
            if (entity3 == CrystalAura.mc.player || !(entity3 instanceof EntityPlayer)) continue;
            EntityPlayer testTarget = (EntityPlayer)entity3;
            if (testTarget.isDead || testTarget.getHealth() <= 0.0f) continue;
            for (BlockPos blockPos : blocks) {
                if (testTarget.getDistanceSq(blockPos) >= 169.0) continue;
                double targetDamage = CrystalAura.calculateDamage((double)blockPos.x + 0.5, blockPos.y + 1, (double)blockPos.z + 0.5, testTarget);
                double selfDamage = CrystalAura.calculateDamage((double)blockPos.x + 0.5, blockPos.y + 1, (double)blockPos.z + 0.5, CrystalAura.mc.player);
                float healthTarget = testTarget.getHealth() + testTarget.getAbsorptionAmount();
                float healthSelf = CrystalAura.mc.player.getHealth() + CrystalAura.mc.player.getAbsorptionAmount();
                if (targetDamage < this.minDamage.getValue() || selfDamage >= (double)healthSelf - 0.5 || selfDamage > targetDamage && targetDamage < (double)healthTarget || !(targetDamage > targetBlockDamage)) continue;
                targetBlock = blockPos;
                targetBlockDamage = targetDamage;
                this.target = testTarget;
            }
            if (this.target == null) continue;
            break;
        }
        if (this.target == null) {
            this.renderBlock = null;
            CrystalAura.resetRotation();
            return;
        }
        this.renderBlock = targetBlock;
        if (ModuleManager.getModuleByName("AutoGG").isEnabled()) {
            AutoGG autoGG = (AutoGG)ModuleManager.getModuleByName("AutoGG");
            autoGG.addTargetedPlayer(this.target.getName());
        }
        if (this.place.getValue().booleanValue()) {
            if (!offhand && CrystalAura.mc.player.inventory.currentItem != crystalSlot) {
                if (this.autoSwitch.getValue().booleanValue()) {
                    CrystalAura.mc.player.inventory.currentItem = crystalSlot;
                    CrystalAura.resetRotation();
                    this.switchCooldown = true;
                }
                return;
            }
            this.lookAtPacket((double)targetBlock.x + 0.5, (double)targetBlock.y - 0.5, (double)targetBlock.z + 0.5, CrystalAura.mc.player);
            RayTraceResult result = CrystalAura.mc.world.rayTraceBlocks(new Vec3d(CrystalAura.mc.player.posX, CrystalAura.mc.player.posY + (double)CrystalAura.mc.player.getEyeHeight(), CrystalAura.mc.player.posZ), new Vec3d((double)targetBlock.x + 0.5, (double)targetBlock.y - 0.5, (double)targetBlock.z + 0.5));
            EnumFacing f = result == null || result.sideHit == null ? EnumFacing.UP : result.sideHit;
            if (this.switchCooldown) {
                this.switchCooldown = false;
                return;
            }
            CrystalAura.mc.player.connection.sendPacket(new CPacketPlayerTryUseItemOnBlock(targetBlock, f, offhand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND, 0.0f, 0.0f, 0.0f));
        }
        if (this.spoofRotations.getValue().booleanValue() && isSpoofingAngles) {
            if (togglePitch) {
                CrystalAura.mc.player.rotationPitch = (float)((double)CrystalAura.mc.player.rotationPitch + 4.0E-4);
                togglePitch = false;
            } else {
                CrystalAura.mc.player.rotationPitch = (float)((double)CrystalAura.mc.player.rotationPitch - 4.0E-4);
                togglePitch = true;
            }
        }
    }

    private boolean rayTraceHitCheck(EntityEnderCrystal crystal) {
        if (!this.rayTraceHit.getValue().booleanValue()) {
            return true;
        }
        return CrystalAura.mc.player.canEntityBeSeen(crystal);
    }

    private void lookAtPacket(double px, double py, double pz, EntityPlayer me) {
        double[] v = EntityUtil.calculateLookAt(px, py, pz, me);
        CrystalAura.setYawAndPitch((float)v[0], (float)v[1]);
    }

    private boolean canPlaceCrystal(BlockPos blockPos) {
        BlockPos boost = blockPos.add(0, 1, 0);
        BlockPos boost2 = blockPos.add(0, 2, 0);
        return (CrystalAura.mc.world.getBlockState(blockPos).getBlock() == Blocks.BEDROCK || CrystalAura.mc.world.getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN) && CrystalAura.mc.world.getBlockState(boost).getBlock() == Blocks.AIR && CrystalAura.mc.world.getBlockState(boost2).getBlock() == Blocks.AIR && CrystalAura.mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(boost)).isEmpty() && CrystalAura.mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(boost2)).isEmpty();
    }

    private List<BlockPos> findCrystalBlocks() {
        NonNullList positions = NonNullList.create();
        positions.addAll((Collection)BlockInteractionHelper.getSphere(CrystalAura.getPlayerPos(), this.placeRange.getValue().floatValue(), this.placeRange.getValue().intValue(), false, true, 0).stream().filter(this::canPlaceCrystal).collect(Collectors.toList()));
        return positions;
    }

    @Override
    public void onEnable() {
        if (this.announceUsage.getValue().booleanValue()) {
            Command.sendChatMessage("[CrystalAura] " + ChatFormatting.GREEN.toString() + "Enabled!");
        }
        this.hitDelayCounter = 0;
    }

    @Override
    public void onDisable() {
        this.renderBlock = null;
        this.target = null;
        CrystalAura.resetRotation();
        if (this.announceUsage.getValue().booleanValue()) {
            Command.sendChatMessage("[CrystalAura] " + ChatFormatting.RED.toString() + "Disabled!");
        }
    }

    @Override
    public String getHudInfo() {
        if (this.target == null) {
            return "";
        }
        return this.target.getName().toUpperCase();
    }

    private static enum RenderMode {
        UP,
        BLOCK,
        NONE;
    }
}
