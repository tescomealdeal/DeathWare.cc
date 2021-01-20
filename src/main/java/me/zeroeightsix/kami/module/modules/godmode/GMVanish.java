package me.zeroeightsix.kami.module.modules.godmode;

import java.util.Objects;
import me.zeroeightsix.kami.module.Module;
import me.zeroeightsix.kami.module.ModuleManager;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.CPacketVehicleMove;

@Module.Info(name="GMVanish", category=Module.Category.GODMODE, description="Godmode Vanish")
public class GMVanish extends Module {
    private Entity entity;

    @Override
    public void onEnable() {
        if (GMVanish.mc.player == null || GMVanish.mc.player.getRidingEntity() == null) {
            this.disable();
            return;
        }
        this.entity = GMVanish.mc.player.getRidingEntity();
        GMVanish.mc.player.dismountRidingEntity();
        GMVanish.mc.world.removeEntity(this.entity);
    }

    @Override
    public void onUpdate() {
        if (this.isDisabled() || GMVanish.mc.player == null || ModuleManager.isModuleEnabled("Freecam")) {
            return;
        }
        if (GMVanish.mc.player.getRidingEntity() == null) {
            this.disable();
            return;
        }
        if (this.entity != null) {
            this.entity.posX = GMVanish.mc.player.posX;
            this.entity.posY = GMVanish.mc.player.posY;
            this.entity.posZ = GMVanish.mc.player.posZ;
            try {
                Objects.requireNonNull(mc.getConnection()).sendPacket(new CPacketVehicleMove(this.entity));
            }
            catch (Exception e) {
                System.out.println("ERROR: Dude we kinda have a problem here:");
                e.printStackTrace();
            }
        }
    }
}
