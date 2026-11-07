package id.itemforge;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class ForgeCommand implements CommandExecutor, TabCompleter {
    private final ItemForge pl;
    public ForgeCommand(ItemForge pl) { this.pl = pl; }

    private void msg(CommandSender s, String m) {
        s.sendMessage(ItemDef.text(pl.getConfig().getString("messages.prefix", "") + m));
    }

    @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 0) { msg(s, "&e/ifg give <player> <id> [jumlah] | list | info <id> | reload"); return true; }
        switch (a[0].toLowerCase()) {
            case "reload" -> { pl.reload(); msg(s, "&aReload selesai: " + pl.items().all().size() + " item."); }
            case "list" -> msg(s, "&7Item: &f" + String.join(", ", pl.items().all().stream().map(d -> d.id).toList()));
            case "info" -> {
                ItemDef d = a.length > 1 ? pl.items().get(a[1]) : null;
                if (d == null) { msg(s, "&cItem tidak ada."); break; }
                msg(s, "&7" + d.id + " &8| &f" + d.material + " &8| CMD=" + d.customModelData + " &8| model=" + d.itemModel
                        + " &8| ability=" + d.abilities.size());
            }
            case "give" -> {
                if (a.length < 3) { msg(s, "&cPakai: /ifg give <player> <id> [jumlah]"); break; }
                Player t = Bukkit.getPlayerExact(a[1]);
                ItemDef d = pl.items().get(a[2]);
                if (t == null || d == null) { msg(s, "&cPlayer atau item tidak ditemukan."); break; }
                int n = 1;
                try { if (a.length > 3) n = Integer.parseInt(a[3]); } catch (NumberFormatException ignored) {}
                t.getInventory().addItem(d.build(pl, n)).values().forEach(r -> t.getWorld().dropItem(t.getLocation(), r));
                msg(s, "&aMemberi " + n + "x " + d.id + " ke " + t.getName());
            }
            default -> msg(s, "&cSubcommand tidak dikenal.");
        }
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        List<String> r = new ArrayList<>();
        if (a.length == 1) r.addAll(List.of("give", "list", "info", "reload"));
        else if (a.length == 2 && a[0].equalsIgnoreCase("give")) Bukkit.getOnlinePlayers().forEach(p -> r.add(p.getName()));
        else if ((a.length == 3 && a[0].equalsIgnoreCase("give")) || (a.length == 2 && a[0].equalsIgnoreCase("info")))
            pl.items().all().forEach(d -> r.add(d.id));
        String last = a[a.length - 1].toLowerCase();
        r.removeIf(x -> !x.toLowerCase().startsWith(last));
        return r;
    }
}
