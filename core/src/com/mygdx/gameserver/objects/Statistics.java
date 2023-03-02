package com.mygdx.gameserver.objects;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Statistics {

    private Map<String, Integer> playerPoints = new HashMap<>();
    private static final int KILL_POINTS = 3;
    private static final int HIT_POINTS = 1;

    public int getPlayerPreviousPoints(String playerNickname) {
        if (!this.playerPoints.containsKey(playerNickname)) return 0;
        return this.playerPoints.get(playerNickname);
    }

    public void addKillPoints(String playerNickname) {
        int prevPlayerPoints = this.getPlayerPreviousPoints(playerNickname);
        this.playerPoints.put(playerNickname, prevPlayerPoints + KILL_POINTS);
    }

    public void addHitPoints(String playerNickname) {
        int prevPlayerPoints = this.getPlayerPreviousPoints(playerNickname);
        this.playerPoints.put(playerNickname, prevPlayerPoints + HIT_POINTS);
    }

    private String join(String separator, List<String> input) {

        if (input == null || input.size() <= 0) return "";

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < input.size(); i++) {

            sb.append(input.get(i));

            // if not the last item
            if (i != input.size() - 1) {
                sb.append(separator);
            }

        }

        return sb.toString();

    }

    public String getAllStatisticsAsString() {
        List<String> statisticsList = new LinkedList<>();

        for (String playerNickname : this.playerPoints.keySet()) {
            int playerScore = this.playerPoints.get(playerNickname);
            statisticsList.add(playerNickname + "         " + playerScore);
        }

        return this.join("\n", statisticsList);
    }
}
