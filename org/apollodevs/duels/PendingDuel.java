package org.apollodevs.duels;

import org.bukkit.entity.Player;

public class PendingDuel {

    Player received;
    Player sent;
    int bet;
    String kit;

    public PendingDuel(Player received, Player sent, int bet, String kit) {

        this.received = received;
        this.sent = sent;
        this.bet = bet;
        this.kit = kit;

    }

}


