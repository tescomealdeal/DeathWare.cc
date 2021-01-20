package me.zeroeightsix.kami.module.modules.combat;

import me.zeroeightsix.kami.module.Module;
import me.zeroeightsix.kami.setting.Setting;
import me.zeroeightsix.kami.setting.Settings;
import me.zeroeightsix.kami.util.BlockInteractionHelper;
import me.zeroeightsix.kami.util.Wrapper;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@Module.Info(name="HopperNuker", category=Module.Category.COMBAT)
public class HopperNuker
        extends Module {
    private Setting<Double> range = this.register(Settings.d("Range", 5.5));
    private Setting<Boolean> pickswitch = this.register(Settings.b("Auto Switch", false));
    private int oldSlot = -1;
    private boolean isMining = false;

    @Override
    public void onUpdate() {
        BlockPos pos = this.getNearestHopper();
        if (pos != null) {
            if (!this.isMining) {
                this.oldSlot = Wrapper.getPlayer().inventory.currentItem;
                this.isMining = true;
            }
            float[] angle = BlockInteractionHelper.calcAngle(Wrapper.getPlayer().getPositionEyes(Wrapper.getMinecraft().getRenderPartialTicks()), new Vec3d((float)pos.getX() + 0.5f, (float)pos.getY() + 0.5f, (float)pos.getZ() + 0.5f));
            Wrapper.getPlayer().rotationYaw = angle[0];
            Wrapper.getPlayer().rotationYawHead = angle[0];
            Wrapper.getPlayer().rotationPitch = angle[1];
            if (this.canBreak(pos)) {
                if (this.pickswitch.getValue().booleanValue()) {
                    int newSlot = -1;
                    for (int i = 0; i < 9; ++i) {
                        ItemStack stack = Wrapper.getPlayer().inventory.getStackInSlot(i);
                        if (stack == ItemStack.EMPTY || !(stack.getItem() instanceof ItemPickaxe)) continue;
                        newSlot = i;
                        break;
                    }
                    if (newSlot != -1) {
                        Wrapper.getPlayer().inventory.currentItem = newSlot;
                    }
                }
                Wrapper.getMinecraft().playerController.onPlayerDamageBlock(pos, Wrapper.getPlayer().getHorizontalFacing());
                Wrapper.getPlayer().swingArm(EnumHand.MAIN_HAND);
            }
        } else if (this.pickswitch.getValue().booleanValue() && this.oldSlot != -1) {
            Wrapper.getPlayer().inventory.currentItem = this.oldSlot;
            this.oldSlot = -1;
            this.isMining = false;
        }
    }

    private boolean canBreak(BlockPos pos) {
        IBlockState blockState = Wrapper.getWorld().getBlockState(pos);
        Block block = blockState.getBlock();
        return block.getBlockHardness(blockState, Wrapper.getWorld(), pos) != -1.0f;
    }

    private BlockPos getNearestHopper() {
        Double maxDist = this.range.getValue();
        BlockPos ret = null;
        Double x = maxDist;
        while (x >= -maxDist.doubleValue()) {
            Double y = maxDist;
            while (y >= -maxDist.doubleValue()) {
                Double z = maxDist;
                while (z >= -maxDist.doubleValue()) {
                    BlockPos pos = new BlockPos(Wrapper.getPlayer().posX + x, Wrapper.getPlayer().posY + y, Wrapper.getPlayer().posZ + z);
                    double dist = Wrapper.getPlayer().getDistance(pos.getX(), pos.getY(), pos.getZ());
                    if (dist <= maxDist && Wrapper.getWorld().getBlockState(pos).getBlock() == Blocks.HOPPER && this.canBreak(pos) && (double)pos.getY() >= Wrapper.getPlayer().posY) {
                        maxDist = dist;
                        ret = pos;
                    }
                    Double d = z;
                    z = z - 1.0;
                }
                Double d = y;
                y = y - 1.0;
            }
            Double d = x;
            x = x - 1.0;
        }
        return ret;
    }
}