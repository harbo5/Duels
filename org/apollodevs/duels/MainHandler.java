package org.apollodevs.duels;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class MainHandler implements Listener{

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if(Duel.isInDuel(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        for(KitNames kit : KitNames.thisList)
            kit.click(event);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        for(KitNames kit : KitNames.thisList)
            kit.close(event);
    }

}
