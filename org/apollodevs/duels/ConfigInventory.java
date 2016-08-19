package org.apollodevs.duels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ConfigInventory {

    private Inventory inv;
    private List<String> sections;
    private Map<Integer, String> names = new HashMap<Integer, String>();
    private FileConfiguration conf;

    public ConfigInventory(FileConfiguration conf, String key) {
        this.conf = conf;
        this.sections = new ArrayList<String>();
        this.inv = Bukkit.createInventory(null, conf.getInt(key + ".rows") * 9,
                ChatColor.translateAlternateColorCodes('&', conf.getString(key + ".title")));
        for (String touch : conf.getConfigurationSection(key + ".items").getKeys(false))
            sections.add(key + ".items." + touch);
        Map<Integer, ItemStack> items = createItems();
        for (int slot : items.keySet()) {
            if (slot != 0 && slot < inv.getSize()) {
                inv.setItem(slot - 1, items.get(slot));
            }
        }
    }

    @SuppressWarnings("deprecation")
    public Map<Integer, ItemStack> createItems() {
        Map<Integer, ItemStack> map = new HashMap<Integer, ItemStack>();
        for (String key : sections) {
            String check = key.replace('.', ',');
            String kitname = check.split(",")[check.split(",").length - 1];
            int slot = conf.getInt(key + ".slot");
            if(slot < 0)
                slot = -slot;
            int amount = conf.getInt(key + ".amount", 1);
            String[] vals = conf.getString(key + ".item").split(":");
            String material = vals[0];
            Material mat;
            try {
                int id = Integer.parseInt(material);
                mat = Material.getMaterial(id);
            } catch (NumberFormatException ex) {
                mat = Material.matchMaterial(material);
            }
            if (mat == null) {
                System.out.println("Could not load item: " + kitname);
                System.out.println("-> Invalid material");
                continue;
            }
            short data = 0;
            if (vals.length == 2) {
                try {
                    data = Short.parseShort(vals[1]);
                } catch (NumberFormatException ex) {
                    data = 0;
                }
            }
            ItemStack item = new ItemStack(mat, amount, data);
            String name = conf.getString(key + ".name");
            List<String> lore = conf.getStringList(key + ".lore");
            ItemMeta meta = item.getItemMeta();
            if (name != null)
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore != null && !lore.isEmpty()) {
                List<String> base = new ArrayList<String>();
                for (int i = 0; i < lore.size(); i++)
                    base.add(ChatColor.translateAlternateColorCodes('&', lore.get(i)));
                meta.setLore(base);
            }
            item.setItemMeta(meta);
            if (map.containsKey(slot)) {
                System.out.println("Skipped item: " + kitname);
                System.out.println("-> Duplicate slots");
                continue;
            }
            names.put(slot - 1, kitname);
            map.put(slot, item);
        }
        return map;
    }

    @Override
    protected ConfigInventory clone() {
        return this;
    }

    public Map<Integer, String> getNames() {
        return names;
    }

    public Inventory getInv() {
        return inv;
    }

    public static ConfigInventory pull(FileConfiguration conf, String section) {
        return new ConfigInventory(conf, section).clone();
    }

}