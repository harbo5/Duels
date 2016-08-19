package org.apollodevs.duels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;

public class Duel {

    protected static List<Duel> duels = new ArrayList<Duel>();
    protected static LinkedList<UUID> inDuelPlayers = new LinkedList<UUID>();

    protected static boolean isInDuel(Player player) {
        return inDuelPlayers.contains(player.getUniqueId());
    }

    Map<String, ItemStack[]> inv = new HashMap<String, ItemStack[]>();
    Map<String, ItemStack[]> armor = new HashMap<String, ItemStack[]>();

    Player player1;
    Player player2;
    final PendingDuel pending;
    Arena arena;
    int taskId = 0;
    int timerCount = EmenbeeDuels.countDown;
    boolean over = false;

    public Duel(PendingDuel pending, Arena arena) {

        this.player1 = pending.received;
        this.player2 = pending.sent;
        this.pending = pending;
        this.arena = arena;

    }

    @SuppressWarnings("deprecation")
    public void startCountDown() {
        inDuelPlayers.add(player1.getUniqueId());
        inDuelPlayers.add(player2.getUniqueId());
        duels.add(this);
        player1.teleport(arena.spawn1);
        player2.teleport(arena.spawn2);

        inv.put(player1.getName(), player1.getInventory().getContents());// save
        // inventory
        armor.put(player1.getName(), player1.getInventory().getArmorContents());

        player1.getInventory().setArmorContents(null);
        player1.getInventory().clear();

        inv.put(player2.getName(), player2.getInventory().getContents());// save
        // inventory
        armor.put(player2.getName(), player2.getInventory().getArmorContents());

        player2.getInventory().setArmorContents(null);
        player2.getInventory().clear();

        String kitname = pending.kit;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kit " + kitname + " " + player2.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kit " + kitname + " " + player1.getName());

        EmenbeeDuels.economy.withdrawPlayer(player1, pending.bet);
        EmenbeeDuels.economy.withdrawPlayer(player2, pending.bet);

        EmenbeeDuels.frozen.add(player1.getUniqueId().toString());
        EmenbeeDuels.frozen.add(player2.getUniqueId().toString());

        final BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
        taskId = scheduler.scheduleAsyncRepeatingTask(EmenbeeDuels.plugin, new Runnable() {
            @Override
            public void run() {
                if (over) {
                    scheduler.cancelTask(taskId);
                    taskId = 0;

                    return;
                }
                if (timerCount == 60) {
                    player1.sendMessage(ChatColor.DARK_RED + "The duel will start in " + timerCount + " seconds!");
                    player2.sendMessage(ChatColor.DARK_RED + "The duel will start in " + timerCount + " seconds!");
                } else if (timerCount == 30) {
                    player1.sendMessage(ChatColor.RED + "The duel will start in " + timerCount + " seconds!");
                    player2.sendMessage(ChatColor.RED + "The duel will start in " + timerCount + " seconds!");
                } else if (timerCount == 20) {
                    player1.sendMessage(ChatColor.RED + "The duel will start in " + timerCount + " seconds!");
                    player2.sendMessage(ChatColor.RED + "The duel will start in " + timerCount + " seconds!");
                } else if (timerCount == 10) {
                    player1.sendMessage(ChatColor.RED + "The duel will start in " + timerCount + " seconds!");
                    player2.sendMessage(ChatColor.RED + "The duel will start in " + timerCount + " seconds!");
                } else if (timerCount <= 5) {
                    if (timerCount == 0) {
                        scheduler.cancelTask(taskId);
                        taskId = 0;
                        start();
                        return;
                    }
                    player1.sendMessage(ChatColor.GREEN + "The duel will start in " + timerCount + " seconds!");
                    player2.sendMessage(ChatColor.GREEN + "The duel will start in " + timerCount + " seconds!");
                }
                timerCount--;

            }
        }, 0L, 20L);

    }

    public void start() {

        player1.sendMessage(ChatColor.DARK_RED + "The duel starts now!");
        player2.sendMessage(ChatColor.DARK_RED + "The duel starts now!");
        EmenbeeDuels.frozen.remove(player1.getUniqueId().toString());
        EmenbeeDuels.frozen.remove(player2.getUniqueId().toString());

    }

    public void end(final OfflinePlayer loser, final boolean safeClear) {
        over = true;
        final Player winner;
        OfflinePlayer check = Bukkit.getOfflinePlayer(player1.getUniqueId());
        if (check.getUniqueId().equals(loser.getUniqueId()))
            winner = player2;
        else
            winner = player1;

        winner.setHealth(20.0f);

        if (EmenbeeDuels.frozen.contains(player1.getUniqueId().toString()))
            EmenbeeDuels.frozen.remove(player1.getUniqueId().toString());
        if (EmenbeeDuels.frozen.contains(player2.getUniqueId().toString()))
            EmenbeeDuels.frozen.remove(player2.getUniqueId().toString());

        if (taskId != 0) {
            BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
            scheduler.cancelTask(taskId);
        }
        if (safeClear) {
            if (!inv.isEmpty() && !armor.isEmpty()) {
                if (check.isOnline()) {
                    winner.setItemOnCursor(null);
                    winner.getOpenInventory().getTopInventory().clear();
                    winner.getInventory().clear();
                    winner.getInventory().setArmorContents(null);

                    winner.getInventory().setContents(inv.get(winner.getName()));// restore
                    // inventory
                    winner.getInventory().setArmorContents(armor.get(winner.getName()));

                    inv.remove(winner.getName());// remove entries from
                    // hashmaps
                    armor.remove(winner.getName());
                } else {
                    removeSafe(check);
                }
            }
        } else if (check.isOnline() && loser.isOnline()) {
            player1.setItemOnCursor(null);
            winner.getOpenInventory().getTopInventory().clear();
            player1.getInventory().clear();
            player1.getInventory().setArmorContents(null);

            player1.getInventory().setContents(inv.get(player1.getName()));

            player1.getInventory().setArmorContents(armor.get(player1.getName()));

            inv.remove(player1.getName());// remove entries from
            // hashmaps
            armor.remove(player1.getName());
            player2.setItemOnCursor(null);
            winner.getOpenInventory().getTopInventory().clear();
            player2.getInventory().clear();
            player2.getInventory().setArmorContents(null);

            player2.getInventory().setContents(inv.get(player2.getName()));// restore
            // inventory
            player2.getInventory().setArmorContents(armor.get(player2.getName()));

            inv.remove(player2.getName());// remove entries from
            // hashmaps
            armor.remove(player2.getName());
        } else if (check.isOnline()) {
            winner.setItemOnCursor(null);
            winner.getOpenInventory().getTopInventory().clear();
            winner.getInventory().clear();
            winner.getInventory().setArmorContents(null);

            winner.getInventory().setContents(inv.get(winner.getName()));// restore
            // inventory
            winner.getInventory().setArmorContents(armor.get(winner.getName()));

            inv.remove(winner.getName());// remove entries from
            // hashmaps
            armor.remove(winner.getName());
            removeSafe(loser);
        } else if (loser.isOnline()) {
            Player player = Bukkit.getPlayer(loser.getUniqueId());
            player.setItemOnCursor(null);
            winner.getOpenInventory().getTopInventory().clear();
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);

            player.getInventory().setContents(inv.get(player.getName()));// restore
            // inventory
            player.getInventory().setArmorContents(armor.get(player.getName()));

            inv.remove(player.getName());// remove entries from
            // hashmaps
            armor.remove(player.getName());
            removeSafe(check);
        } else {
            removeSafe(loser);
            removeSafe(check);
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(EmenbeeDuels.plugin, new Runnable() {
            public void run() {
                inDuelPlayers.remove(player1.getUniqueId());
                inDuelPlayers.remove(player2.getUniqueId());
                duels.remove(this);
                Location loc = arena.spawn1;
                int[] offset = { -1, 0, 1 };
                World world = loc.getWorld();
                int baseX = loc.getChunk().getX();
                int baseZ = loc.getChunk().getZ();
                Collection<Chunk> chunksAroundPlayer = new HashSet<>();
                for (int x : offset) {
                    for (int z : offset) {
                        Chunk chunk = world.getChunkAt(baseX + x, baseZ + z);
                        chunksAroundPlayer.add(chunk);
                    }
                }
                if (!chunksAroundPlayer.contains(loc.getChunk()))
                    chunksAroundPlayer.add(loc.getChunk());
                for (Chunk chunk : chunksAroundPlayer) {
                    for (Entity e : chunk.getEntities()) {
                        if (!(e instanceof Player))
                            e.remove();
                    }
                }
                Location loc2 = arena.spawn2;
                baseX = loc2.getChunk().getX();
                baseZ = loc2.getChunk().getZ();
                chunksAroundPlayer = new HashSet<>();
                for (int x : offset) {
                    for (int z : offset) {
                        Chunk chunk = world.getChunkAt(baseX + x, baseZ + z);
                        chunksAroundPlayer.add(chunk);
                    }
                }
                if (!chunksAroundPlayer.contains(loc2.getChunk()))
                    chunksAroundPlayer.add(loc2.getChunk());
                for (Chunk chunk : chunksAroundPlayer) {
                    for (Entity e : chunk.getEntities()) {
                        if (!(e instanceof Player))
                            e.remove();
                    }
                }
                winner.teleport(winner.getWorld().getSpawnLocation());
                EmenbeeDuels.economy.depositPlayer(winner, pending.bet * 2);
                EmenbeeDuels.economy.withdrawPlayer(loser, pending.bet);
                winner.sendMessage(ChatColor.GRAY + "You have won " + ChatColor.GOLD + "$" + pending.bet);
                EmenbeeDuels.arenas.add(arena);
                if (!EmenbeeDuels.queue.isEmpty()) {
                    Utils.startDuel(EmenbeeDuels.queue.get(0));
                    EmenbeeDuels.queue.remove(0);
                }
                if (!EmenbeeDuels.queue.isEmpty()) {
                    int i = 1;
                    for (PendingDuel queue : EmenbeeDuels.queue) {
                        queue.sent.sendMessage(ChatColor.AQUA + "You are number " + i + " in queue for your duel!");
                        queue.received.sendMessage(ChatColor.AQUA + "You are number " + i + " in queue for your duel!");
                        i++;
                    }
                }
                EmenbeeDuels.activeDuels.remove(winner.getUniqueId().toString());
                EmenbeeDuels.activeDuels.remove(loser.getUniqueId().toString());
            }
        }, 100L);

        Bukkit.broadcastMessage(
                ChatColor.AQUA + winner.getName() + " triumphed against " + loser.getName() + " in a duel!");

    }

    public void removeSafe(OfflinePlayer player) {
        List<ItemStack[]> lost = Arrays.asList(inv.get(player.getName()), armor.get(player.getName()));

        EmenbeeDuels.lostItems.put(player.getUniqueId(), lost);

        inv.remove(player.getName());
        armor.remove(player.getName());
    }

}
