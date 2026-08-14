package src.pinksheep;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class Pinksheep extends JavaPlugin implements Listener, CommandExecutor {

    private final Set<UUID> invincibleSheep = new HashSet<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("spawnpink") != null) {
            getCommand("spawnpink").setExecutor(this);
        }
        getLogger().info("PinkSheep Plugin enabled!");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Game-only command.");
            return true;
        }

        // スポーンイベント(EntitySpawnEvent)が発火する前に色をピンクに設定
        player.getWorld().spawn(player.getLocation(), Sheep.class, sheep -> {
            sheep.setColor(DyeColor.PINK);
        });

        player.sendMessage(Component.text("テスト用のピンク羊を召喚しました。", NamedTextColor.LIGHT_PURPLE));
        return true;
    }

    @EventHandler
    public void onPinkSheepSpawn(EntitySpawnEvent event) {
        if (event.getEntityType() == EntityType.SHEEP) {
            Sheep sheep = (Sheep) event.getEntity();
            if (sheep.getColor() == DyeColor.PINK) {
                startAwakeningTask(sheep);
            }
        }
    }

    private void startAwakeningTask(Sheep sheep) {
        UUID sheepId = sheep.getUniqueId();
        if (invincibleSheep.contains(sheepId)) return;
        invincibleSheep.add(sheepId);

        // 1.20.5 / 1.21+ 対応の属性取得
        AttributeInstance scale = sheep.getAttribute(Attribute.SCALE);
        if (scale != null) {
            scale.setBaseValue(3.5);
        }

        sheep.setGlowing(true);

        Component mainTitle = Component.text("ピンク羊当選", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD);
        Component subTitle = Component.text("当選おめでとう！", NamedTextColor.RED);
        Title title = Title.title(mainTitle, subTitle);

        for (Player p : getServer().getOnlinePlayers()) {
            p.showTitle(title);
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        }

        new BukkitRunnable() {
            int timer = 60;

            @Override
            public void run() {
                if (sheep.isDead() || timer <= 0) {
                    invincibleSheep.remove(sheepId);
                    sheep.setGlowing(false);

                    AttributeInstance currentScale = sheep.getAttribute(Attribute.SCALE);
                    if (currentScale != null) {
                        currentScale.setBaseValue(1.0);
                    }

                    this.cancel();
                    return;
                }

                Location loc = sheep.getLocation();

                for (int i = 0; i < 3; i++) {
                    TNTPrimed tnt = (TNTPrimed) loc.getWorld().spawnEntity(loc.clone().add(0, 5, 0), EntityType.TNT);
                    tnt.setFuseTicks(40);
                    tnt.setVelocity(new Vector((Math.random() - 0.5) * 1.5, 0.5, (Math.random() - 0.5) * 1.5));
                }

                Firework fw = (Firework) loc.getWorld().spawnEntity(loc.clone().add(0, 2, 0), EntityType.FIREWORK_ROCKET);
                FireworkMeta meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .withColor(Color.FUCHSIA, Color.WHITE)
                        .withFade(Color.PURPLE)
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .trail(true)
                        .flicker(true)
                        .build());
                fw.setFireworkMeta(meta);
                fw.detonate();

                sheep.setCustomNameVisible(true);

                timer--;
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    @EventHandler
    public void onSheepDamage(EntityDamageEvent event) {
        if (invincibleSheep.contains(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
            event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.3f, 2.0f);
        }
    }
}