package org.apollodevs.duels;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

public class Utils {

    public static boolean addPendingDuel(Player player, PendingDuel pending) {

        Player player2 = pending.received;

        if(!EmenbeeDuels.pendingDuels.containsKey(player.getUniqueId().toString()))
            EmenbeeDuels.pendingDuels.put(player.getUniqueId().toString(), new ArrayList<PendingDuel>());

        List<PendingDuel> pendings = EmenbeeDuels.pendingDuels.get(player.getUniqueId().toString());

        for(PendingDuel pending2 : pendings)
            if(pending2.received.equals(player2))
                return false;

        pendings.add(pending);

        EmenbeeDuels.pendingDuels.put(player.getUniqueId().toString(), pendings);

        return true;

    }

    public static PendingDuel acceptPendingDuel(Player player, Player player2) {

        if(!EmenbeeDuels.pendingDuels.containsKey(player.getUniqueId().toString()))
            return null;

        List<PendingDuel> pendings = EmenbeeDuels.pendingDuels.get(player.getUniqueId().toString());

        for(PendingDuel pending2 : pendings)
            if(pending2.sent.equals(player2))
                return pending2;

        return null;

    }

    public static void removeAllPendings(Player player) {

        if(!EmenbeeDuels.pendingDuels.containsKey(player.getUniqueId().toString()))
            return;
        else
            EmenbeeDuels.pendingDuels.remove(player.getUniqueId().toString());

    }

    public static PendingDuel getQueue(Player player) {

        for(PendingDuel pending : EmenbeeDuels.queue)
            if(pending.sent.equals(player) || pending.received.equals(player))
                return pending;

        return null;

    }

    public static void startDuel(PendingDuel pending) {

        Arena arena = EmenbeeDuels.arenas.get(0);
        EmenbeeDuels.arenas.remove(arena);

        Duel duel = new Duel(pending, arena);
        EmenbeeDuels.activeDuels.put(pending.sent.getUniqueId().toString(), duel);
        EmenbeeDuels.activeDuels.put(pending.received.getUniqueId().toString(), duel);
        duel.startCountDown();
        EmenbeeDuels.waiting.remove(pending.sent.getUniqueId().toString());
        EmenbeeDuels.waiting.remove(pending.received.getUniqueId().toString());

    }

}

