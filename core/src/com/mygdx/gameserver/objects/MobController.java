package com.mygdx.gameserver.objects;

import com.mygdx.gameserver.server.KryoServer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class MobController {

    public static final float SIZES_CONSTANT = 0.065f;

    private static final int ZOMBIE_WIDTH = (int) (694 * SIZES_CONSTANT);
    private static final int ZOMBIE_HEIGHT = (int) (1167 * SIZES_CONSTANT);
    private static final int OCTOPUS_WIDTH = (int) (923 * SIZES_CONSTANT);
    private static final int OCTOPUS_HEIGHT = (int) (986 * SIZES_CONSTANT);
    private static final int CRAB_WIDTH = (int) (824 * SIZES_CONSTANT);
    private static final int CRAB_HEIGHT = (int) (550 * SIZES_CONSTANT);
    private static final int BLUEGUY_WIDTH = (int) (562 * SIZES_CONSTANT);
    private static final int BLUEGUY_HEIGHT = (int) (776 * SIZES_CONSTANT);
    private static final int GREENGUY_WIDTH = (int) (887 * SIZES_CONSTANT);
    private static final int GREENGUY_HEIGHT = (int) (726 * SIZES_CONSTANT);

    private static final int TIME_PLAYER_IS_IMMUNE = 3;

    private static final int MOB_SPAWN_RADIUS = 100;

    private static final double ZOMBIE_SPEED = 0.3;
    private static final int ZOMBIE_HP = 3;
    private static final double OCTOPUS_SPEED = 0.2;
    private static final int OCTOPUS_HP = 5;
    private static final double CRAB_SPEED = 0.1;
    private static final int CRAB_HP = 6;
    private static final double BLUEGUY_SPEED = 0.8;
    private static final int BLUEGUY_HP = 1;
    private static final double GREENGUY_SPEED = 0.05;
    private static final int GREENGUY_HP = 35;

    private Map<Integer, Enemy> allMobsSpawned = new HashMap<>();
    private Map<String, Player> players;
    private Map<String, Long> playerTimers = new HashMap<>();
    private Set<Player> deadPlayers = new HashSet<>();
    private KryoServer server;
    private long serverStartTime = System.currentTimeMillis();

    public MobController(KryoServer server) {
        this.server = server;
        this.players = this.server.getConnectedPlayers();

        for (String playerNickname : players.keySet()) {
            playerTimers.put(playerNickname, 0L);
        }
    }

    /**
     * Spawn specified amount of mobs at specified location.
     *
     * @param mobType      mob type to spawn (e.g. "zombie", "octopus")
     * @param amount       amount of mobs to spawn
     * @param spawnPointX  initial spawn point coordinate x
     * @param spawnPointY  initial spawn point coordinate y
     */
    public void spawnMob(String mobType, int amount, float spawnPointX, float spawnPointY) {

        for (int i = 0; i < amount; i++) {

            Enemy newMob = null;

            float randomNum = ThreadLocalRandom.current().nextInt(-MOB_SPAWN_RADIUS, MOB_SPAWN_RADIUS + 1);

            float randomX = spawnPointX + randomNum;
            float randomY = spawnPointY + randomNum;

            switch (mobType) {

                case "zombie": newMob = new Zombie(randomX, randomY,
                        ZOMBIE_WIDTH, ZOMBIE_HEIGHT, ZOMBIE_SPEED, ZOMBIE_HP);
                    break;

                case "octopus": newMob = new Octopus(randomX, randomY,
                        OCTOPUS_WIDTH, OCTOPUS_HEIGHT, OCTOPUS_SPEED, OCTOPUS_HP);
                    break;

                case "crab": newMob = new Crab(randomX, randomY,
                        CRAB_WIDTH, CRAB_HEIGHT, CRAB_SPEED, CRAB_HP);
                    break;

                case "blueguy": newMob = new BlueGuy(randomX, randomY,
                        BLUEGUY_WIDTH, BLUEGUY_HEIGHT, BLUEGUY_SPEED, BLUEGUY_HP);
                    break;

                case "greenguy": newMob = new GreenGuy(randomX, randomY,
                        GREENGUY_WIDTH, GREENGUY_HEIGHT, GREENGUY_SPEED, GREENGUY_HP);
                    break;
            }

            allMobsSpawned.put(newMob.getId(), newMob);
        }
    }

    public void killMob(int mobId) {
        this.allMobsSpawned.remove(mobId);
    }

    public int getAmountOfDeadPlayers() {
        return this.deadPlayers.size();
    }

    public void mobsFollowPlayers() {

        double gradientX = 0;
        double gradientY = 0;

        for (Enemy mob : allMobsSpawned.values()) {
            Player nearestPlayer = getNearestPlayer(mob);  // Player to follow.

            // The mob will follow this player who is the nearest one.
            if (nearestPlayer != null) {
                double distance = Math.sqrt(Math.pow(mob.getX() - nearestPlayer.getX(), 2)
                                          + Math.pow(mob.getX() - nearestPlayer.getX(), 2)) + 0.00001;

                gradientX = (mob.getX() - nearestPlayer.getX()) / distance;
                gradientY = (mob.getY() - nearestPlayer.getY()) / distance;

                double vectorLength = Math.sqrt(Math.pow(gradientX, 2) + Math.pow(gradientY, 2));
                double unitVectorX = gradientX / vectorLength;
                double unitVectorY = gradientY / vectorLength;

                mob.setX((float) (mob.getX() - (unitVectorX * mob.getSpeed())));
                mob.setY((float) (mob.getY() - (unitVectorY * mob.getSpeed())));
            }

            // Check if any mob's sprite intercepts any player's sprite.
            this.checkSpritesCollision(mob);
        }
    }

    public Player getNearestPlayer(Enemy mob) {
        Player nearestPlayer = null;
        float distanceToPlayer;
        float distanceToPlayerPrev = 99999;

        for (Player player : server.getConnectedPlayers().values()) {

            if (!this.deadPlayers.contains(player)) {
                distanceToPlayer = (float) Math.sqrt(Math.pow(player.getX() - mob.getX(), 2) +
                        Math.pow(player.getY() - mob.getY(), 2));

                if (distanceToPlayer < distanceToPlayerPrev) {
                    distanceToPlayerPrev = distanceToPlayer;
                    nearestPlayer = player;
                }
            }
        }
        return nearestPlayer;
    }

    /**
     * Check if mob's sprite intercepts any player's sprite.
     */
    public void checkSpritesCollision(Enemy mob) {

        long delta = -(serverStartTime - System.currentTimeMillis()) / 1000;

        for (String playerNickname : players.keySet()) {
            // Check collision with each player on the server.
            Player player = players.get(playerNickname);

            if (!this.deadPlayers.contains(player) && !player.isImmune()) {
                // If player is not immune.
                if (((mob.getX() <= player.getX() && player.getX() <= mob.getX() + mob.getWidth())
                        && (mob.getY() <= player.getY() && player.getY() <= mob.getY() + mob.getHeight()))
                        || ((mob.getX() <= player.getX() + player.getWidth() && player.getX() + player.getWidth() <= mob.getX() + mob.getWidth())
                        && (mob.getY() <= player.getY() && player.getY() <= mob.getY() + mob.getHeight()))
                        || ((mob.getX() <= player.getX() + player.getWidth() && player.getX() + player.getWidth() <= mob.getX() + mob.getWidth())
                        && (mob.getY() <= player.getY() + player.getHeight() && player.getY() + player.getHeight() <= mob.getY() + mob.getHeight()))
                        || ((mob.getX() <= player.getX() && player.getX() <= mob.getX() + mob.getWidth())
                        && (mob.getY() <= player.getY() + player.getHeight() && player.getY() + player.getHeight() <= mob.getY() + mob.getHeight()))) {
                    // If player is hit by a mob -> decrease player's hp.

                    player.setHp(player.getHp() - 1);
                    // Player become immune for specified amount of time.
                    player.setIsImmune(true);
                    // Reset the previous timer.
                    this.playerTimers.put(playerNickname, delta);

                    System.out.println("Player is hit, hp is now " + player.getHp());
                    this.server.broadcastPlayerHitPacket(playerNickname);
                }
            }

            else {
                // If player was hit by an enemy -> the player remains immune for specified amount of time.
                if (delta - playerTimers.get(playerNickname) >= TIME_PLAYER_IS_IMMUNE) {
                    // Player stops being immune.
                    player.setIsImmune(false);
                }
            }

            if (player.getHp() == 0) {
                this.deadPlayers.add(player);
            }
        }
    }

    public Map<Integer, Enemy> getAllMobsSpawned() {
        return this.allMobsSpawned;
    }

    public void reset() {
        this.allMobsSpawned.clear();
        this.deadPlayers.clear();
    }
}
