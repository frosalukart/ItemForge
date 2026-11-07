package id.itemforge;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;

public class ItemManager {
    public static NamespacedKey ID_KEY;
    private final ItemForge pl;
    private final Map<String, ItemDef> defs = new LinkedHashMap<>();

    public ItemManager(ItemForge pl) { this.pl = pl; ID_KEY = new NamespacedKey(pl, "item_id"); }

    public Collection<ItemDef> all() { return defs.values(); }
    public ItemDef get(String id) { return defs.get(id.toLowerCase()); }

    public ItemDef fromItem(ItemStack s) {
        if (s == null || !s.hasItemMeta()) return null;
        String id = s.getItemMeta().getPersistentDataContainer().get(ID_KEY, PersistentDataType.STRING);
        return id == null ? null : defs.get(id.toLowerCase());
    }

    public void load() {
        defs.clear();
        File dir = new File(pl.getDataFolder(), "items");
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            for (String id : y.getKeys(false)) {
                ConfigurationSection s = y.getConfigurationSection(id);
                if (s == null) continue;
                try { defs.put(id.toLowerCase(), parse(id.toLowerCase(), s)); }
                catch (Exception ex) { pl.getLogger().warning("Gagal load item '" + id + "' di " + f.getName() + ": " + ex.getMessage()); }
            }
        }
    }

    private ItemDef parse(String id, ConfigurationSection s) {
        ItemDef d = new ItemDef(id);
        Material mat = Material.matchMaterial(s.getString("material", "STONE"));
        if (mat == null) throw new IllegalArgumentException("material tidak valid");
        d.material = mat;
        d.name = s.getString("name", id);
        d.lore = s.getStringList("lore");
        if (s.contains("custom-model-data")) d.customModelData = s.getInt("custom-model-data");
        d.itemModel = s.getString("item-model");
        d.unbreakable = s.getBoolean("unbreakable");
        d.glow = s.getBoolean("glow");
        d.slot = s.getString("slot", "mainhand");
        ConfigurationSection en = s.getConfigurationSection("enchants");
        if (en != null) for (String k : en.getKeys(false)) d.enchants.put(k, en.getInt(k));
        ConfigurationSection st = s.getConfigurationSection("stats");
        if (st != null) for (String k : st.getKeys(false)) d.stats.put(k, st.getDouble(k));
        ConfigurationSection ab = s.getConfigurationSection("abilities");
        if (ab != null) for (String k : ab.getKeys(false)) {
            ConfigurationSection a = ab.getConfigurationSection(k);
            if (a == null) continue;
            Trigger t = Trigger.valueOf(a.getString("trigger", "RIGHT_CLICK").toUpperCase());
            d.abilities.add(new Ability(k, t, a.getDouble("cooldown", 0), a.getString("permission", ""),
                    a.getBoolean("consume"), a.getStringList("actions")));
        }
        return d;
    }
}
