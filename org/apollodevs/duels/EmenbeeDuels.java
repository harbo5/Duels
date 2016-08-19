package org.apollodevs.duels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.milkbowl.vault.economy.Economy;

public class EmenbeeDuels extends JavaPlugin implements Listener {

    public static Map<UUID, List<ItemStack[]>> lostItems = new HashMap<>();
    public static List<UUID> gone = new ArrayList<UUID>();
    public static Economy economy = null;
    List<String> help;
    FileConfiguration config;
    public static List<Arena> arenas = new ArrayList<Arena>();
    public static List<String> frozen = new ArrayList<String>();

    public static HashMap<String, List<PendingDuel>> pendingDuels = new HashMap<String, List<PendingDuel>>();
    public static HashMap<String, Duel> activeDuels = new HashMap<String, Duel>();

    public static List<PendingDuel> queue = new ArrayList<PendingDuel>();
    public static List<String> waiting = new ArrayList<String>();

    public static Plugin plugin;

    public static int minBet = 10;
    public static int maxBet = 10;
    public static int pickUpLoot = 10;
    public static int countDown = 10;

    public void onEnable() {
        Duel.duels.clear();
        lostItems.clear();
        getServer().getPluginManager().registerEvents(new MainHandler(), this);
        getServer().getPluginManager().registerEvents(this, this);

        this.saveDefaultConfig();

        plugin = (Plugin) this;

        setupEconomy();

        config = getConfig();

        help = config.getStringList("help");

        minBet = getConfig().getInt("minBet");
        maxBet = getConfig().getInt("maxBet");
        pickUpLoot = getConfig().getInt("pickUpLoot");
        countDown = getConfig().getInt("countDown");

        for (String str : config.getKeys(false)) {

            if (!str.equals("help") && !str.equals("placeholder") && !str.equals("minBet") && !str.equals("maxBet")
                    && !str.equals("pickUpLoot") && !str.equals("countDown") && !str.equals("kits-inventory")) {
                getLogger().warning("--------><--------");
                getLogger().warning(str);
                getLogger().warning("--------><--------");
                String world = config.getString(str + ".world");

                String[] val1 = config.getString(str + ".spawn1").split("o");
                double x1 = Double.parseDouble(val1[0]);
                double y1 = Double.parseDouble(val1[1]);
                double z1 = Double.parseDouble(val1[2]);
                float yaw1 = Float.parseFloat(val1[3]);
                float pitch1 = Float.parseFloat(val1[4]);

                String[] val2 = config.getString(str + ".spawn2").split("o");
                double x2 = Double.parseDouble(val2[0]);
                double y2 = Double.parseDouble(val2[1]);
                double z2 = Double.parseDouble(val2[2]);
                float yaw2 = Float.parseFloat(val2[3]);
                float pitch2 = Float.parseFloat(val2[4]);

                arenas.add(new Arena(new Location(Bukkit.getWorld(world), x1, y1, z1, yaw1, pitch1),
                        new Location(Bukkit.getWorld(world), x2, y2, z2, yaw2, pitch2)));

            }

        }

    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("queue")) {

            if (waiting.contains(((Player) sender).getUniqueId().toString())) {

                Player player = (Player) sender;
                PendingDuel pending = Utils.getQueue(player);
                player.sendMessage(ChatColor.AQUA + "You are number " + (queue.indexOf(pending) + 1) + " in queue!");

            } else {

                sender.sendMessage(ChatColor.RED + "You are not in queue!");

            }

        }

        if (cmd.getName().equalsIgnoreCase("duel")) {

            if (sender.hasPermission("EmenbeeDuels.admin")) {

                if (args.length == 2) {

                    if (args[0].equals("createarena")) {

                        config.set(args[1] + ".spawn1", "placeholder");
                        config.set(args[1] + ".spawn2", "placeholder");
                        config.set(args[1] + ".world", ((Player) sender).getWorld().getName());
                        saveConfig();
                        sender.sendMessage(ChatColor.AQUA + "Arena created!");
                        return true;

                    } else if (args[0].equals("delarena")) {

                        config.set(args[1], null);
                        saveConfig();
                        sender.sendMessage(ChatColor.AQUA + "Arena deleted!");
                        return true;

                    }

                } else if (args.length == 3) {

                    if (args[0].equals("setposition")) {

                        Location loc = ((Player) sender).getLocation();
                        config.set(args[1] + ".spawn" + args[2], loc.getX() + "o" + loc.getY() + "o" + loc.getZ() + "o"
                                + loc.getYaw() + "o" + loc.getPitch());
                        saveConfig();
                        sender.sendMessage(ChatColor.AQUA + "Position " + args[2] + " set!");
                        return true;

                    }

                }

            }

            if (args.length == 0) {
                for (String str : help)
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', str));
                return true;
            }

            if (args.length == 1 && args[0].equalsIgnoreCase("cancel")) {

                Player playerS = (Player) sender;

                if (waiting.contains(playerS.getUniqueId().toString())) {

                    for (PendingDuel pending : queue) {

                        if (pending.sent.equals(playerS) || pending.received.equals(playerS)) {

                            for (int i = queue.indexOf(pending) + 1; i < queue.size(); i++) {
                                PendingDuel pending2 = queue.get(i);
                                pending2.sent.sendMessage(
                                        ChatColor.AQUA + "A queue has been cancelled, you are now " + i + " in queue!");
                                pending2.received.sendMessage(
                                        ChatColor.AQUA + "A queue has been cancelled, you are now " + i + " in queue!");
                            }

                            queue.remove(pending);
                            pending.sent.sendMessage(ChatColor.RED + "The queue has been cancelled!");
                            pending.received.sendMessage(ChatColor.RED + "The queue has been cancelled!");
                            if (waiting.contains(pending.sent.getUniqueId().toString()))
                                waiting.remove(pending.sent.getUniqueId().toString());
                            if (waiting.contains(pending.received.getUniqueId().toString()))
                                waiting.remove(pending.received.getUniqueId().toString());

                            break;

                        }

                    }

                } else {

                    playerS.sendMessage(ChatColor.RED + "You are not in queue!");

                }

                return true;

            }

            if (args.length == 2) {

                if (args[0].equalsIgnoreCase("accept")) {

                    if (!isPlayer(args[1])) {

                        sender.sendMessage(ChatColor.RED + args[1] + " is not a valid player!");

                    } else {

                        Player player = getPlayer(args[1]);

                        PendingDuel pending = Utils.acceptPendingDuel((Player) sender, player);

                        if (pending == null) {

                            sender.sendMessage(ChatColor.RED + "You don't have a pending duel from that player!");
                            return false;

                        } else {

                            if (waiting.contains(player.getUniqueId().toString())) {

                                sender.sendMessage(ChatColor.RED + "That player is already in a queue!");
                                return false;

                            }

                            if (activeDuels.containsKey(player.getUniqueId().toString())) {

                                sender.sendMessage(ChatColor.RED + "That player is already in a duel!");
                                return false;

                            }

                            if (economy.getBalance((Player) sender) < pending.bet) {

                                ((Player) sender)
                                        .sendMessage(ChatColor.RED + "You don't have enough money to start the duel!");
                                player.sendMessage(ChatColor.RED + sender.getName()
                                        + " doesn't have enough money to start the duel!");
                                return false;

                            }

                            if (economy.getBalance(player) < pending.bet) {

                                player.sendMessage(ChatColor.RED + "You don't have enough money to start the duel!");
                                ((Player) sender).sendMessage(ChatColor.RED + player.getName()
                                        + " doesn't have enough money to start the duel!");
                                return false;

                            }

                            Utils.removeAllPendings(player);
                            Utils.removeAllPendings((Player) sender);

                            if (!arenas.isEmpty()) {

                                Utils.startDuel(pending);

                            } else {

                                queue.add(pending);
                                pending.sent.sendMessage(
                                        ChatColor.AQUA + "You are number " + queue.size() + " in queue for your duel!");
                                pending.received.sendMessage(
                                        ChatColor.AQUA + "You are number " + queue.size() + " in queue for your duel!");
                                waiting.add(pending.sent.getUniqueId().toString());
                                waiting.add(pending.received.getUniqueId().toString());

                            }

                        }

                    }

                    return true;

                } else if (args[0].equalsIgnoreCase("decline")) {

                    if (!isPlayer(args[1])) {

                        sender.sendMessage(ChatColor.RED + args[1] + " is not a valid player!");

                    } else {

                        Player player = (Player) sender;
                        Player player2 = getPlayer(args[1]);

                        PendingDuel pending = null;

                        if (!pendingDuels.containsKey(player.getUniqueId().toString())) {

                            player.sendMessage(ChatColor.RED + "You have no pending requests!");
                            return false;

                        }

                        for (PendingDuel pending2 : pendingDuels.get(player.getUniqueId().toString())) {
                            if (pending2.sent.equals(player2)) {
                                pending = pending2;
                                break;
                            }
                        }

                        if (pending == null) {

                            sender.sendMessage(ChatColor.RED + "You don't have a pending duel from that player!");
                            return false;

                        } else {

                            pendingDuels.get(player.getUniqueId().toString()).remove(pending);

                            player.sendMessage(ChatColor.RED + args[1] + "'s duel request has been cancelled!");
                            player2.sendMessage(ChatColor.RED + player.getName() + " cancelled your duel request!");

                        }

                    }

                    return true;

                }

            }

            if (args.length == 1 || args.length == 2 || args.length == 3) {

                if (isPlayer(args[0]) && !args[0].equals(sender.getName())) {

                    Player player = getPlayer(args[0]);
                    Player player2 = (Player) sender;

                    if (waiting.contains(player.getUniqueId().toString())) {

                        sender.sendMessage(ChatColor.RED + "That player is already in a queue!");
                        return false;

                    }

                    if (activeDuels.containsKey(player.getUniqueId().toString())) {

                        sender.sendMessage(ChatColor.RED + "That player is already in a duel!");
                        return false;

                    }

                    if (waiting.contains(player2.getUniqueId().toString())) {

                        sender.sendMessage(ChatColor.RED + "You are already in a queue!");
                        return false;

                    }

                    if (args.length == 2 && isInteger(args[1])) {

                        int money = Integer.parseInt(args[1]);

                        if (money < minBet) {
                            sender.sendMessage(ChatColor.RED + "That bet is to small!");
                            return false;
                        }

                        if (money > maxBet) {
                            sender.sendMessage(ChatColor.RED + "That bet is to big!");
                            return false;
                        }

                        if (economy.getBalance((Player) sender) >= money) {

                            if (economy.getBalance(player) >= money) {

                                KitNames.openKit(player, (Player) sender, money);

                            } else {

                                sender.sendMessage(ChatColor.RED + args[0] + " does not have enough money!");

                            }

                        } else {

                            sender.sendMessage(ChatColor.RED + "You don't have enough money!");

                        }

                    } else {

                        KitNames.openKit(player, (Player) sender, 0);

                    }

                } else {

                    sender.sendMessage(ChatColor.RED + args[0] + " is not a valid player!");

                }

            }

        }

        return false;

    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        if (Duel.isInDuel(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (Duel.isInDuel((Player) event.getWhoClicked())
                && event.getClickedInventory().equals(event.getWhoClicked().getOpenInventory().getTopInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (Duel.isInDuel(event.getPlayer())) {
            final Item item = event.getItemDrop();
            new BukkitRunnable() {
                @Override
                public void run() {
                    item.remove();
                }
            }.runTaskLaterAsynchronously(this, 5L);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (gone.contains(event.getEntity().getUniqueId())) {
            event.getEntity().setItemOnCursor(new ItemStack(Material.AIR));
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            gone.remove(event.getEntity().getUniqueId());
            return;
        }
        if (activeDuels.containsKey(event.getEntity().getUniqueId().toString())) {
            Duel duel = activeDuels.get(event.getEntity().getUniqueId().toString());
            if (duel.over)
                return;

            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            duel.end(event.getEntity(), false);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {

        Player player = null;

        if (event.getEntity() instanceof Player)
            player = (Player) event.getEntity();
        else if (event.getDamager() instanceof Player)
            player = (Player) event.getDamager();

        if (player == null)
            return;

        if (waiting.contains(player.getUniqueId().toString())) {

            PendingDuel pending = Utils.getQueue(player);

            queue.remove(pending);

            pending.sent.sendMessage(ChatColor.RED + "The queue has been cancelled!");
            pending.received.sendMessage(ChatColor.RED + "The queue has been cancelled!");

            waiting.remove(pending.sent.getUniqueId().toString());
            waiting.remove(pending.received.getUniqueId().toString());

            Utils.removeAllPendings(pending.sent);
            Utils.removeAllPendings(pending.received);

        } else {

            Utils.removeAllPendings(player);

        }

    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (lostItems.containsKey(event.getPlayer().getUniqueId())) {
            Player player = event.getPlayer();
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            ItemStack[] contents = lostItems.get(player.getUniqueId()).get(0);
            ItemStack[] armor = lostItems.get(player.getUniqueId()).get(1);
            player.getInventory().setContents(contents);
            player.getInventory().setArmorContents(armor);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        if (activeDuels.containsKey(event.getPlayer().getUniqueId().toString())) {

            gone.add(event.getPlayer().getUniqueId());
            event.getPlayer().setHealth(0.0F);
            Duel duel = activeDuels.get(event.getPlayer().getUniqueId().toString());
            OfflinePlayer player = Bukkit.getOfflinePlayer(event.getPlayer().getUniqueId());

            List<ItemStack[]> lost = Arrays.asList(duel.inv.get(player.getName()), duel.armor.get(player.getName()));

            lostItems.put(player.getUniqueId(), lost);

            duel.inv.remove(player.getName());
            duel.armor.remove(player.getName());

            duel.end(Bukkit.getOfflinePlayer(event.getPlayer().getUniqueId()), true);

        } else if (waiting.contains(event.getPlayer().getUniqueId().toString())) {

            PendingDuel pending = Utils.getQueue(event.getPlayer());

            for (int i = queue.indexOf(pending) + 1; i < queue.size(); i++) {
                PendingDuel pending2 = queue.get(i);
                pending2.sent
                        .sendMessage(ChatColor.AQUA + "A queue has been cancelled, you are now " + i + " in queue!");
                pending2.received
                        .sendMessage(ChatColor.AQUA + "A queue has been cancelled, you are now " + i + " in queue!");
            }

            queue.remove(pending);
            waiting.remove(pending.sent.getUniqueId().toString());
            waiting.remove(pending.received.getUniqueId().toString());

            pending.sent.sendMessage(ChatColor.RED + "The queue has been cancelled because "
                    + event.getPlayer().getName() + " logged out!");
            pending.received.sendMessage(ChatColor.RED + "The queue has been cancelled because "
                    + event.getPlayer().getName() + " logged out!");

        }
        if(pendingDuels.containsKey(event.getPlayer().getUniqueId().toString())){
            pendingDuels.remove(event.getPlayer().getUniqueId().toString());
        }

    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {

        if (event.getEntity() instanceof Player)
            if (activeDuels.containsKey(((Player) event.getEntity()).getUniqueId().toString()))
                if (activeDuels.get(((Player) event.getEntity()).getUniqueId().toString()).timerCount != 0)
                    event.setCancelled(true);

    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (frozen.contains(event.getPlayer().getUniqueId().toString())) {
            Location from = event.getFrom();
            Location to = event.getTo();
            double x = Math.floor(from.getX());
            double z = Math.floor(from.getZ());
            if (Math.floor(to.getX()) != x || Math.floor(to.getZ()) != z) {
                x += .5;
                z += .5;
                event.getPlayer()
                        .teleport(new Location(from.getWorld(), x, from.getY(), z, from.getYaw(), from.getPitch()));
            }
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {

        String msg = event.getMessage().toLowerCase();

        if (activeDuels.containsKey(event.getPlayer().getUniqueId().toString())) {

            if (!msg.contains("/msg") && !msg.contains("/tell") && (!msg.contains("/r") && !msg.contains("/repair"))) {
                event.getPlayer().sendMessage(ChatColor.RED + "You can't do that while in a duel!");
                event.setCancelled(true);
            }

        }

        if (waiting.contains(event.getPlayer().getUniqueId().toString())) {

            if (msg.contains("/pay") || msg.contains("/epay") || msg.contains("/shop")) {
                event.getPlayer().sendMessage(ChatColor.RED + "You can't do that while in queue!");
                event.setCancelled(true);
            }

        }

    }

    public boolean isPlayer(String name) {
        for (Player player : Bukkit.getOnlinePlayers())
            if (player.getName().equals(name))
                return true;
        return false;
    }

    public Player getPlayer(String name) {
        for (Player player : Bukkit.getOnlinePlayers())
            if (player.getName().equals(name))
                return player;
        return null;
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager()
                .getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }
        return (economy != null);
    }

    public static boolean isInteger(String s) {
        return isInteger(s, 10);
    }

    public static boolean isInteger(String s, int radix) {
        if (s.isEmpty())
            return false;
        for (int i = 0; i < s.length(); i++) {
            if (i == 0 && s.charAt(i) == '-') {
                if (s.length() == 1)
                    return false;
                else
                    continue;
            }
            if (Character.digit(s.charAt(i), radix) < 0)
                return false;
        }
        return true;
    }

}
