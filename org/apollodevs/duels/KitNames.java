package org.apollodevs.duels;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class KitNames implements Listener {

    protected static List<KitNames> thisList = new ArrayList<KitNames>();
    private final ConfigInventory inv;
    private final Inventory base;
    private final Player receiving, sending;
    private final int bet;

    public KitNames(Player receiving, Player sending, int bet) {
        inv = ConfigInventory.pull(EmenbeeDuels.plugin.getConfig(), "kits-inventory");
        base = inv.getInv();
        sending.openInventory(base);
        thisList.add(this);
        this.bet = bet;
        this.receiving = receiving;
        this.sending = sending;
    }

    public void close(InventoryCloseEvent event) {
        boolean run = false;
        for (HumanEntity entity : base.getViewers())
            if (entity.getUniqueId().equals(event.getPlayer().getUniqueId()))
                run = true;
        if (!run)
            return;
        EventQueue.invokeLater(new Runnable(){
            @Override
            public void run() {
                thisList.remove(this);
            }
        });
    }

    public void click(InventoryClickEvent event) {
        boolean run = false;
        for (HumanEntity entity : base.getViewers())
            if (entity.getUniqueId().equals(event.getWhoClicked().getUniqueId()))
                run = true;
        if (!run)
            return;
        if (event.getInventory().equals(inv.getInv())) {
            event.setCancelled(true);
            if (event.getRawSlot() < inv.getInv().getSize()) {
                if (inv.getNames().containsKey(event.getRawSlot())) {
                    String kitname = inv.getNames().get(event.getRawSlot());
                    begin(kitname);
                    sending.closeInventory();
                    EventQueue.invokeLater(new Runnable(){
                        @Override
                        public void run() {
                            thisList.remove(this);
                        }
                    });
                }
            }
        }
    }

    private void begin(String kit) {
        if (!Utils.addPendingDuel(receiving, new PendingDuel(receiving, (Player) sending, bet, kit))) {
            sending.sendMessage(ChatColor.RED + receiving.getName() + " already has a pending duel from you!");
            return;
        }
        sending.sendMessage(ChatColor.AQUA + "You've invited " + receiving.getName() + " to a duel!");
        receiving.sendMessage(ChatColor.GREEN + "You've been invited to a " + kit + " duel by " + sending.getName()
                + " with a $" + bet + " bet!");
    }

    public static void openKit(Player receiving, Player sending, int bet) {
        new KitNames(receiving, sending, bet);
    }

}
