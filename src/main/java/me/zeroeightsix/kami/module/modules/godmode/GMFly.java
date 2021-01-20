package me.zeroeightsix.kami.module.modules.godmode;

import me.zeroeightsix.kami.module.Module;

@Module.Info(name="GMFly", category=Module.Category.GODMODE, description="Godmode Fly")
public class GMFly extends Module {
    @Override
    public void onEnable() {
        this.toggleFly(true);
    }

    @Override
    public void onDisable() {
        this.toggleFly(false);
    }

    @Override
    public void onUpdate() {
        this.toggleFly(true);
    }

    private void toggleFly(boolean b) {
        GMFly.mc.player.capabilities.isFlying = b;
        if (GMFly.mc.player.capabilities.isCreativeMode) {
            return;
        }
        GMFly.mc.player.capabilities.allowFlying = b;
    }
}
