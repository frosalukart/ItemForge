package id.itemforge;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Format action:  TIPE:arg1:arg2...
 * COMMAND:<cmd> | PLAYER_COMMAND:<cmd> | MESSAGE:<teks> | SOUND:<key>:<vol>:<pitch>
 * PARTICLE:<NAMA>:<jumlah>:<spread> | POTION:<efek>:<detik>:<level> | HEAL:<jumlah>
 * DAMAGE_NEARBY:<radius>:<damage> | IGNITE_NEARBY:<radius>:<tick> | LAUNCH:<power>
 * LIGHTNING | TELEPORT_FORWARD:<jarak> | MODEL:<modelId>:<tick>
 * Placeholder: %player%
 */
public final class ActionExecutor {
    private ActionExecutor() {}

    public static void run(ItemForge pl, Player p, Entity target, String raw) {
        try {
            int i = raw.indexOf(':');
            String type = (i < 0 ? raw : raw.substring(0, i)).toUpperCase();
            String rest = i < 0 ? "" : raw.substring(i + 1);
            String[] a = rest.isEmpty() ? new String[0] : rest.split(":");
            Location loc = p.getLocation();
            switch (type) {
                case "COMMAND" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rest.replace("%player%", p.getName()));
                case "PLAYER_COMMAND" -> p.performCommand(rest.replace("%player%", p.getName()));
                case "MESSAGE" -> p.sendMessage(ItemDef.text(rest.replace("%player%", p.getName())));
                case "SOUND" -> p.getWorld().playSound(loc, a[0], a.length > 1 ? f(a[1]) : 1f, a.length > 2 ? f(a[2]) : 1f);
                case "PARTICLE" -> {
                    double s = a.length > 2 ? Double.parseDouble(a[2]) : 0.3;
                    p.getWorld().spawnParticle(Particle.valueOf(a[0].toUpperCase()), loc.clone().add(0, 1, 0),
                            a.length > 1 ? Integer.parseInt(a[1]) : 10, s, s, s, 0);
                }
                case "POTION" -> {
                    PotionEffectType t = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(a[0].toLowerCase()));
                    if (t != null) p.addPotionEffect(new PotionEffect(t, (int) (Double.parseDouble(a[1]) * 20),
                            a.length > 2 ? Integer.parseInt(a[2]) - 1 : 0));
                }
                case "HEAL" -> {
                    var max = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                    p.setHealth(Math.min(max == null ? 20 : max.getValue(), p.getHealth() + Double.parseDouble(a[0])));
                }
                case "DAMAGE_NEARBY" -> nearby(p, Double.parseDouble(a[0])).forEach(le -> le.damage(Double.parseDouble(a[1]), p));
                case "IGNITE_NEARBY" -> nearby(p, Double.parseDouble(a[0])).forEach(le -> le.setFireTicks(Integer.parseInt(a[1])));
                case "LAUNCH" -> p.setVelocity(loc.getDirection().multiply(Double.parseDouble(a[0])).setY(0.5));
                case "LIGHTNING" -> (target != null ? target : p).getWorld().strikeLightning((target != null ? target : p).getLocation());
                case "TELEPORT_FORWARD" -> {
                    Vector dir = loc.getDirection().setY(0).normalize().multiply(Double.parseDouble(a[0]));
                    Location to = loc.clone().add(dir);
                    if (to.getBlock().isPassable() && to.clone().add(0, 1, 0).getBlock().isPassable()) p.teleport(to);
                }
                case "MODEL" -> pl.models().apply(target != null ? target : p, a[0], a.length > 1 ? Long.parseLong(a[1]) : 0);
                default -> pl.getLogger().warning("Action tidak dikenal: " + raw);
            }
        } catch (Exception ex) {
            pl.getLogger().warning("Action gagal '" + raw + "': " + ex);
        }
    }

    private static float f(String s) { return Float.parseFloat(s); }

    private static java.util.List<LivingEntity> nearby(Player p, double r) {
        return p.getNearbyEntities(r, r, r).stream()
                .filter(e -> e instanceof LivingEntity && e != p && !(e instanceof org.bukkit.entity.ArmorStand))
                .map(e -> (LivingEntity) e).toList();
    }
}
