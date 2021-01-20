package me.zeroeightsix.kami.module.modules.godmode;

import me.zeroeightsix.kami.module.Module;
import me.zeroeightsix.kami.setting.Setting;
import me.zeroeightsix.kami.setting.Settings;

@Module.Info(name="GMSpeed", category=Module.Category.GODMODE, description="Godmode Speed")
public class GMSpeed
        extends Module {
    private Setting<Double> gmspeed = this.register(Settings.doubleBuilder("Speed").withRange(0.1, 10.0).withValue(1.0).build());

    @Override
    public void onUpdate() {
        if ((GMSpeed.mc.player.moveForward != 0.0f || GMSpeed.mc.player.moveStrafing != 0.0f) && !GMSpeed.mc.player.isSneaking() && GMSpeed.mc.player.onGround) {
            GMSpeed.mc.player.motionX *= this.gmspeed.getValue().doubleValue();
            GMSpeed.mc.player.motionZ *= this.gmspeed.getValue().doubleValue();
        }
    }
}
