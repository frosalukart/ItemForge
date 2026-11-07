package id.itemforge;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

/**
 * Hook ModelEngine (R4) lewat reflection: plugin tetap jalan tanpa ModelEngine.
 * Jika versi ModelEngine kamu beda nama method, cukup ubah di sini.
 */
public class ModelEngineHook {
    private final ItemForge pl;
    public ModelEngineHook(ItemForge pl) { this.pl = pl; }

    public boolean available() { return Bukkit.getPluginManager().isPluginEnabled("ModelEngine"); }

    public void apply(Entity entity, String modelId, long ticks) {
        if (!available()) { pl.getLogger().warning("ModelEngine tidak aktif, action MODEL dilewati."); return; }
        try {
            Class<?> api = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
            Class<?> modeledCls = Class.forName("com.ticxo.modelengine.api.model.ModeledEntity");
            Class<?> activeCls = Class.forName("com.ticxo.modelengine.api.model.ActiveModel");
            Object active = api.getMethod("createActiveModel", String.class).invoke(null, modelId);
            if (active == null) { pl.getLogger().warning("Model '" + modelId + "' tidak ditemukan di ModelEngine."); return; }
            Object modeled = api.getMethod("getOrCreateModeledEntity", Entity.class).invoke(null, entity);
            modeledCls.getMethod("addModel", activeCls, boolean.class).invoke(modeled, active, true);
            if (ticks > 0) {
                Bukkit.getScheduler().runTaskLater(pl, () -> {
                    try { modeledCls.getMethod("removeModel", String.class).invoke(modeled, modelId); }
                    catch (Exception ignored) {}
                }, ticks);
            }
        } catch (Exception ex) {
            pl.getLogger().warning("Gagal apply model '" + modelId + "': " + ex);
        }
    }
}
