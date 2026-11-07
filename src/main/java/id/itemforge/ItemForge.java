package id.itemforge;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class ItemForge extends JavaPlugin {
    private static ItemForge instance;
    private ItemManager items;
    private ModelEngineHook models;

    public static ItemForge get() { return instance; }
    public ItemManager items() { return items; }
    public ModelEngineHook models() { return models; }

    @Override public void onEnable() {
        instance = this;
        saveDefaultConfig();
        if (!new File(getDataFolder(), "items").exists()) saveResource("items/example.yml", false);
        items = new ItemManager(this);
        models = new ModelEngineHook(this);
        items.load();
        getServer().getPluginManager().registerEvents(new TriggerListener(this), this);
        ForgeCommand cmd = new ForgeCommand(this);
        getCommand("itemforge").setExecutor(cmd);
        getCommand("itemforge").setTabCompleter(cmd);
        getLogger().info("Loaded " + items.all().size() + " item(s). ModelEngine: " + models.available());
    }

    public void reload() { reloadConfig(); items.load(); }
}
