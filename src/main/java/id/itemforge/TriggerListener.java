package id.itemforge;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class TriggerListener implements Listener {
    private final ItemForge pl;
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Random rng = new Random();

    public TriggerListener(ItemForge pl) { this.pl = pl; }

    @EventHandler public void onQuit(PlayerQuitEvent e) {
        String p = e.getPlayer().getUniqueId().toString();
        cooldowns.keySet().removeIf(k -> k.startsWith(p));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Action a = e.getAction();
        Trigger t;
        if (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK)
            t = e.getPlayer().isSneaking() ? Trigger.SHIFT_RIGHT_CLICK : Trigger.RIGHT_CLICK;
        else if (a == Action.LEFT_CLICK_AIR || a == Action.LEFT_CLICK_BLOCK) t = Trigger.LEFT_CLICK;
        else return;
        ItemStack it = e.getItem();
        ItemDef d = pl.items().fromItem(it);
        if (d == null) return;
        fire(e.getPlayer(), it, d, t, null);
        // SHIFT_RIGHT_CLICK juga memicu RIGHT_CLICK biasa? Tidak - dipisah sengaja.
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        if (damager instanceof Player p) {
            ItemStack hand = p.getInventory().getItemInMainHand();
            ItemDef d = pl.items().fromItem(hand);
            if (d != null) {
                double dmg = e.getDamage();
                double crit = stat(hand, "crit-chance");
                if (crit > 0 && rng.nextDouble() * 100 < crit) {
                    dmg *= 1 + stat(hand, "crit-damage") / 100.0;
                    p.sendActionBar(ItemDef.text("&e&lCRITICAL!"));
                }
                e.setDamage(dmg);
                double ls = stat(hand, "lifesteal");
                if (ls > 0) {
                    var max = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                    double cap = max == null ? 20 : max.getValue();
                    p.setHealth(Math.min(cap, p.getHealth() + dmg * ls / 100.0));
                }
                fire(p, hand, d, Trigger.HIT_ENTITY, e.getEntity());
            }
        }
        if (e.getEntity() instanceof Player victim) {
            for (ItemStack s : equipped(victim)) {
                ItemDef d = pl.items().fromItem(s);
                if (d != null) fire(victim, s, d, Trigger.WHEN_HIT, damager);
            }
        }
    }

    @EventHandler public void onDeath(EntityDeathEvent e) {
        Player k = e.getEntity().getKiller();
        if (k == null) return;
        ItemStack hand = k.getInventory().getItemInMainHand();
        ItemDef d = pl.items().fromItem(hand);
        if (d != null) fire(k, hand, d, Trigger.KILL_ENTITY, e.getEntity());
    }

    @EventHandler public void onConsume(PlayerItemConsumeEvent e) {
        ItemStack it = e.getItem();
        ItemDef d = pl.items().fromItem(it);
        if (d != null) fireNoConsume(e.getPlayer(), it, d, Trigger.CONSUME, null);
    }

    private List<ItemStack> equipped(Player p) {
        List<ItemStack> l = new ArrayList<>(Arrays.asList(p.getInventory().getArmorContents()));
        l.add(p.getInventory().getItemInMainHand());
        l.add(p.getInventory().getItemInOffHand());
        return l;
    }

    private double stat(ItemStack s, String name) {
        Double v = s.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(pl, "stat_" + name), PersistentDataType.DOUBLE);
        return v == null ? 0 : v;
    }

    private void fire(Player p, ItemStack item, ItemDef d, Trigger t, Entity target) {
        if (run(p, d, t, target)) {
            // consume ditangani di run() lewat flag; item dikurangi di sini
        }
    }
    private void fireNoConsume(Player p, ItemStack item, ItemDef d, Trigger t, Entity target) { run(p, d, t, target); }

    /** @return true bila ada ability yang berjalan. */
    private boolean run(Player p, ItemDef d, Trigger t, Entity target) {
        boolean any = false;
        for (Ability a : d.abilities) {
            if (a.trigger() != t) continue;
            if (!a.permission().isEmpty() && !p.hasPermission(a.permission())) {
                p.sendMessage(ItemDef.text(pl.getConfig().getString("messages.no-permission", "")));
                continue;
            }
            String key = p.getUniqueId() + ":" + d.id + ":" + a.key();
            long now = System.currentTimeMillis();
            Long until = cooldowns.get(key);
            if (until != null && until > now) {
                double left = (until - now) / 1000.0;
                p.sendActionBar(ItemDef.text(pl.getConfig().getString("messages.cooldown", "")
                        .replace("{time}", String.format("%.1f", left))));
                continue;
            }
            if (a.cooldown() > 0) cooldowns.put(key, now + (long) (a.cooldown() * 1000));
            for (String act : a.actions()) ActionExecutor.run(pl, p, target, act);
            if (a.consume() && t != Trigger.CONSUME) {
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (pl.items().fromItem(hand) == d) hand.setAmount(hand.getAmount() - 1);
            }
            any = true;
        }
        return any;
    }
}
