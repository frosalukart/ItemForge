package id.itemforge;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Material;

import java.util.*;

public class ItemDef {
    public final String id;
    public Material material = Material.STONE;
    public String name;
    public List<String> lore = new ArrayList<>();
    public Integer customModelData;
    public String itemModel;
    public boolean unbreakable, glow;
    public Map<String, Integer> enchants = new LinkedHashMap<>();
    public Map<String, Double> stats = new LinkedHashMap<>();
    public String slot = "mainhand";
    public List<Ability> abilities = new ArrayList<>();

    public ItemDef(String id) { this.id = id; this.name = id; }

    static Component text(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s).decoration(TextDecoration.ITALIC, false);
    }

    private static Attribute attr(String stat) {
        return switch (stat) {
            case "damage" -> Attribute.ATTACK_DAMAGE;
            case "attack-speed" -> Attribute.ATTACK_SPEED;
            case "armor" -> Attribute.ARMOR;
            case "health" -> Attribute.MAX_HEALTH;
            case "speed" -> Attribute.MOVEMENT_SPEED;
            case "knockback-resist" -> Attribute.KNOCKBACK_RESISTANCE;
            default -> null;
        };
    }

    public ItemStack build(ItemForge pl, int amount) {
        ItemStack stack = new ItemStack(material, Math.max(1, amount));
        ItemMeta m = stack.getItemMeta();
        m.displayName(text(name));

        List<Component> out = new ArrayList<>();
        var cfg = pl.getConfig();
        if (cfg.getBoolean("show-stats-in-lore", true)) {
            String fmt = cfg.getString("stat-format", "&7{name}: &a+{value}");
            for (var e : stats.entrySet()) {
                String nm = cfg.getString("stat-names." + e.getKey(), e.getKey());
                String val = e.getValue() % 1 == 0 ? String.valueOf(e.getValue().intValue()) : String.valueOf(e.getValue());
                out.add(text(fmt.replace("{name}", nm).replace("{value}", val)));
            }
        }
        for (String l : lore) out.add(text(l));
        if (!out.isEmpty()) m.lore(out);

        // Resource pack hooks
        if (customModelData != null) m.setCustomModelData(customModelData);
        if (itemModel != null) {
            NamespacedKey k = NamespacedKey.fromString(itemModel);
            if (k != null) m.setItemModel(k);
        }
        m.setUnbreakable(unbreakable);
        if (glow) m.setEnchantmentGlintOverride(true);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);

        for (var e : enchants.entrySet()) {
            Enchantment en = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(e.getKey().toLowerCase()));
            if (en != null) m.addEnchant(en, e.getValue(), true);
        }

        EquipmentSlotGroup group = EquipmentSlotGroup.getByName(slot.toLowerCase());
        if (group == null) group = EquipmentSlotGroup.MAINHAND;
        var pdc = m.getPersistentDataContainer();
        pdc.set(ItemManager.ID_KEY, PersistentDataType.STRING, id);
        for (var e : stats.entrySet()) {
            pdc.set(new NamespacedKey(pl, "stat_" + e.getKey()), PersistentDataType.DOUBLE, e.getValue());
            Attribute a = attr(e.getKey());
            if (a != null) {
                m.addAttributeModifier(a, new AttributeModifier(
                        new NamespacedKey(pl, id + "_" + e.getKey()), e.getValue(),
                        AttributeModifier.Operation.ADD_NUMBER, group));
            }
        }
        stack.setItemMeta(m);
        return stack;
    }
}
