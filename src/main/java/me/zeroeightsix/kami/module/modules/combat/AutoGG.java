// =============================================== //
// Recompile disabled. Please run Recaf with a JDK //
// =============================================== //

// Decompiled with: CFR 0.150
package me.zeroeightsix.kami.module.modules.combat;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import me.zeroeightsix.kami.event.events.PacketEvent;
import me.zeroeightsix.kami.module.Module;
import me.zeroeightsix.kami.setting.Setting;
import me.zeroeightsix.kami.setting.Settings;
import me.zeroeightsix.kami.util.EntityUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

@Module.Info(name="AutoGG", category=Module.Category.COMBAT, description="Announce killed Players")
public class AutoGG
        extends Module {
        private ConcurrentHashMap<String, Integer> targetedPlayers = null;
        private Setting<Boolean> toxicMode = this.register(Settings.b("ToxicMode", false));
        private Setting<Boolean> clientName = this.register(Settings.b("ClientName", true));
        private Setting<Integer> timeoutTicks = this.register(Settings.i("TimeoutTicks", 20));
        @EventHandler
        public Listener<PacketEvent.Send> sendListener = new Listener<PacketEvent.Send>(event -> {
                if (AutoGG.mc.player == null) {
                        return;
                }
                if (this.targetedPlayers == null) {
                        this.targetedPlayers = new ConcurrentHashMap();
                }
                if (!(event.getPacket() instanceof CPacketUseEntity)) {
                        return;
                }
                CPacketUseEntity cPacketUseEntity = (CPacketUseEntity)((Object)event.getPacket());
                if (!cPacketUseEntity.getAction().equals(CPacketUseEntity.Action.ATTACK)) {
                        return;
                }
                Entity targetEntity = cPacketUseEntity.getEntityFromWorld(AutoGG.mc.world);
                if (!EntityUtil.isPlayer(targetEntity)) {
                        return;
                }
                this.addTargetedPlayer(targetEntity.getName());
        }, new Predicate[0]);
        @EventHandler
        public Listener<LivingDeathEvent> livingDeathEventListener = new Listener<LivingDeathEvent>(event -> {
                EntityLivingBase entity;
                if (AutoGG.mc.player == null) {
                        return;
                }
                if (this.targetedPlayers == null) {
                        this.targetedPlayers = new ConcurrentHashMap();
                }
                if ((entity = event.getEntityLiving()) == null) {
                        return;
                }
                if (!EntityUtil.isPlayer(entity)) {
                        return;
                }
                EntityPlayer player = (EntityPlayer)entity;
                if (player.getHealth() > 0.0f) {
                        return;
                }
                String name = player.getName();
                if (this.shouldAnnounce(name)) {
                        this.doAnnounce(name);
                }
        }, new Predicate[0]);

        @Override
        public void onEnable() {
                this.targetedPlayers = new ConcurrentHashMap();
        }

        @Override
        public void onDisable() {
                this.targetedPlayers = null;
        }

        @Override
        public void onUpdate() {
                if (this.isDisabled() || AutoGG.mc.player == null) {
                        return;
                }
                if (this.targetedPlayers == null) {
                        this.targetedPlayers = new ConcurrentHashMap();
                }
                for (Entity entity : AutoGG.mc.world.getLoadedEntityList()) {
                        String name2;
                        EntityPlayer player;
                        if (!EntityUtil.isPlayer(entity) || (player = (EntityPlayer)entity).getHealth() > 0.0f || !this.shouldAnnounce(name2 = player.getName())) continue;
                        this.doAnnounce(name2);
                        break;
                }
                this.targetedPlayers.forEach((name, timeout) -> {
                        if (timeout <= 0) {
                                this.targetedPlayers.remove(name);
                        } else {
                                this.targetedPlayers.put((String)name, timeout - 1);
                        }
                });
        }

        private boolean shouldAnnounce(String name) {
                return this.targetedPlayers.containsKey(name);
        }

        private void doAnnounce(String name) {
                String messageSanitized;
                this.targetedPlayers.remove(name);
                StringBuilder message = new StringBuilder();
                if (this.toxicMode.getValue().booleanValue()) {
                        message.append("EZZZ ");
                } else {
                        message.append("good fight ");
                }
                message.append(name);
                message.append("!");
                if (this.clientName.getValue().booleanValue()) {
                        message.append(" ");
                        message.append("Hephaestus");
                        message.append(" owns me and all");
                }
                if ((messageSanitized = message.toString().replaceAll("§", "")).length() > 255) {
                        messageSanitized = messageSanitized.substring(0, 255);
                }
                AutoGG.mc.player.connection.sendPacket(new CPacketChatMessage(messageSanitized));
        }

        public void addTargetedPlayer(String name) {
                if (Objects.equals(name, AutoGG.mc.player.getName())) {
                        return;
                }
                if (this.targetedPlayers == null) {
                        this.targetedPlayers = new ConcurrentHashMap();
                }
                this.targetedPlayers.put(name, this.timeoutTicks.getValue());
        }
}
 