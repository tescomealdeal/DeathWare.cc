package me.zeroeightsix.kami.module.modules.combat;

import me.zeroeightsix.kami.module.Module;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;

@Module.Info( name = "Anti32kTot", category = Module.Category.COMBAT, description = "AutoTotem for ur first inventory slot")
public class Anti32kTotem extends Module {
    public void onUpdate() {
        if (mc.currentScreen == null || !(mc.currentScreen instanceof GuiContainer)) {
            if (mc.player.inventory.getStackInSlot(0).getItem() != Items.TOTEM_OF_UNDYING) {
                for(int i = 9; i < 35; ++i) {
                    if (mc.player.inventory.getStackInSlot(i).getItem() == Items.TOTEM_OF_UNDYING) {
                        mc.playerController.windowClick(mc.player.inventoryContainer.windowId, i, 0, ClickType.SWAP, mc.player);
                        break;
                    }
                }

            }
        }
    }
}