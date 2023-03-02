package com.mygdx.gameserver.server;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.mygdx.gameserver.objects.*;
import com.mygdx.gameserver.packets.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class KryoServer extends Listener {

    static Server server;  // Server object.
    private static boolean threadFlag = false;

    public static final float SIZES_CONSTANT = 0.065f;

    public static final int PLAYER_WIDTH = (int) (771 * SIZES_CONSTANT);
    public static final int PLAYER_HEIGHT = (int) (1054 * SIZES_CONSTANT);
    public static final int MED_KIT_HP_HEAL_AMOUNT = 3;  //NB! If changing this value also change it on the client!!!

    // Players (clients) data.
    private Map<String, Player> connectedPlayers = new HashMap<>();
    private Map<String, Connection> connections = new HashMap<>();
    private Map<String, Boolean> playersReady = new HashMap<>();

    // Mobs.
    private final LevelController levelController = new LevelController(this);
    private static ServerUpdateThread serverUpdateThread;
    private final Statistics statistics = new Statistics();


    // Ports to listen on.
    static final int udpPort = 8080;
    static final int tcpPort = 8081;
//    static int tcpPort = 27960;
//    static int udpPort = 27960;

    public static void main(String[] args) throws IOException {
        System.out.println("Creating the server...");
        server = new Server();

        // Register packet classes. Server can only handle packets that are registered.
        server.getKryo().register(PacketMessage.class);
        server.getKryo().register(PacketCheckPlayerNicknameUnique.class);
        server.getKryo().register(PacketSendPlayerMovement.class);
        server.getKryo().register(PacketUpdatePlayers.class);
        server.getKryo().register(PacketRequestConnectedPlayers.class);
        server.getKryo().register(java.util.ArrayList.class);
        server.getKryo().register(PacketPlayerConnected.class);
        server.getKryo().register(PacketPlayerDisconnected.class);
        server.getKryo().register(PacketUpdateMobsPos.class);
        server.getKryo().register(java.util.HashMap.class);
        server.getKryo().register(float[].class);
        server.getKryo().register(PacketBulletShot.class);
        server.getKryo().register(PacketMobHit.class);
        server.getKryo().register(PacketPlayerHit.class);
        server.getKryo().register(PacketPlayerReady.class);
        server.getKryo().register(PacketGameBeginTimer.class);
        server.getKryo().register(PacketLootSpawn.class);
        server.getKryo().register(PacketLootCollected.class);
        server.getKryo().register(PacketSendStatistics.class);
        server.getKryo().register(PacketGameIsOngoing.class);

        // Bind to the ports.
        server.bind(tcpPort, udpPort);

        server.start();

        // Add the listener.
        server.addListener(new KryoServer());
        System.out.println("Server is up!");

        threadFlag = true;
    }

    public Map<String, Player> getConnectedPlayers() {
        return connectedPlayers;
    }

    public Map<String, Boolean> getPlayersReady() {
        return this.playersReady;
    }

    public LevelController getLevelController() {
        return this.levelController;
    }

    public void mobsFollowPlayer() {
//        mobController.mobsFollowPlayers();
        this.levelController.beginNextWave();
        this.levelController.getMobController().mobsFollowPlayers();
    }

    public int getDeadPlayersAmount() {
        return this.levelController.getMobController().getAmountOfDeadPlayers();
    }

    // Run this method when a client connects.
    public void connected(Connection c) {
        if (threadFlag) {

            serverUpdateThread = new ServerUpdateThread();
            serverUpdateThread.setKryoServer(this);

            new Thread(serverUpdateThread).start();
            System.out.println("ServerUpdate thread is ON!");
            threadFlag = false;
        }

        System.out.println("Received a connection from " + c.getRemoteAddressTCP().getHostString());
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        PacketMessage packetMessage = new PacketMessage();
        packetMessage.message = "Privet kak dela";
        c.sendTCP(packetMessage);
    }

    // Run this method when server receives any packet from ANY client.
    public void received(Connection c, Object p) {

        // Packet check nickname unique.
        if (p instanceof PacketCheckPlayerNicknameUnique) {
            PacketCheckPlayerNicknameUnique packet = (PacketCheckPlayerNicknameUnique) p;

            // Check if this nickname is not already taken by other player.
            if (serverUpdateThread.isStartTheGame()) respondWithPacketGameIsOngoing(c);
            else if (!connectedPlayers.containsKey(packet.playerNickname)) {
                addPlayer(packet.playerNickname, c);
                System.out.println("Client nickname is " + packet.playerNickname);

                // DEBUG
                System.out.println(connectedPlayers);
                System.out.println(connections);

                // Notify user that his nickname is OK.
                packet.isNicknameUnique = true;
                c.sendTCP(packet);
            }

            // If this nickname already exists.
            else {
                System.out.println("Nickname already taken.");
                packet.isNicknameUnique = false;
                c.sendTCP(packet);
            }
        }

        // If received this packet -> player has moved -> need to broadcast update packet.
        if (p instanceof PacketSendPlayerMovement) {
            PacketSendPlayerMovement packet = (PacketSendPlayerMovement) p;

            // Prepare a new update packet.
            PacketUpdatePlayers updatePacket = new PacketUpdatePlayers();
            updatePacket.playerNickname = packet.playerNickname;
            Player playerToUpdate = connectedPlayers.get(packet.playerNickname);

            updatePacket.playerPositionX = packet.playerCurrentPositionX;
            updatePacket.playerPositionY = packet.playerCurrentPositionY;
            updatePacket.playerRotation = packet.playerCurrentRotation;

            // Broadcast update packet (so everyone knows this player's new position).
            for (String nickname : connections.keySet()) {
                Connection playerConnection = connections.get(nickname);
                playerConnection.sendTCP(updatePacket);
            }

            // Update server players' data.
            playerToUpdate.setX(packet.playerCurrentPositionX);
            playerToUpdate.setY(packet.playerCurrentPositionY);
            playerToUpdate.setRotation(packet.playerCurrentRotation);
        }

        // Packet respond with all currently connected players.
        if (p instanceof PacketRequestConnectedPlayers) {
            PacketRequestConnectedPlayers packet = (PacketRequestConnectedPlayers) p;

            // Get all connected players.
            packet.allPlayers.addAll(connectedPlayers.keySet());
            c.sendTCP(packet);
        }

        // Receive this packet if any players shot a bullet.
        if (p instanceof PacketBulletShot) {
            PacketBulletShot packet = (PacketBulletShot) p;

            // Broadcast received packet to everyone except the sender so other players know who has shot the bullet.
            for (String playerNickname : connections.keySet()) {
                if (!playerNickname.equals(packet.playerWhoShot)) {
                    Connection connection = connections.get(playerNickname);
                    connection.sendTCP(packet);
                }
            }
        }

        // Receive this packet if any mob was hit by any player.
        if (p instanceof PacketMobHit) {
            PacketMobHit packet = (PacketMobHit) p;

            // Update mob data stored on the server.
            Enemy mob = this.levelController.getMobController().getAllMobsSpawned().get(packet.mobId);
            mob.setHp(mob.getHp() - 1);

            // If mob hp is 0 -> remove this mob from the game.
            if (mob.getHp() == 0) {
                this.levelController.getMobController().killMob(mob.getId());
                // Add points for killing a mob to the respective player.
                this.statistics.addKillPoints(packet.playerNickname);
            } else if (mob.getHp() > 0) {
                // If player has damaged the mob but has not killed it.
                // Add points for damaging a mob to the respective player.
                this.statistics.addHitPoints(packet.playerNickname);
            }

//            System.out.println("Mob with ID: " + packet.mobId + " was hit. Now HP is: " + mob.getHp());

            // Broadcast the received packet to other players so other players know which mob was hit.
            for (Connection connection : connections.values()) {
                connection.sendTCP(packet);
            }
        }

        // Receive this packet when any player has pressed the "Ready" button.
        if (p instanceof PacketPlayerReady) {
            PacketPlayerReady packet = (PacketPlayerReady) p;

            // Update players' readiness in server's hashmap.
            this.playersReady.put(packet.playerNickname, packet.isPlayerReady);

            // Resend this packet to other player (except the sender), so other players are notified which
            // teammate has pressed the "Ready" button.
            broadCastPlayerReadyPacket(packet.playerNickname, packet.isPlayerReady);
            System.out.println(this.playersReady);
        }

        // Receive this packet if player has collected any spawned loot.
        if (p instanceof PacketLootCollected) {
            PacketLootCollected packet = (PacketLootCollected) p;

            if (packet.isLootMedKit) {
                // If collected loot was a med kit -> heal the player.
                this.connectedPlayers.get(packet.playerNickname).setHp(MED_KIT_HP_HEAL_AMOUNT);
            }

            // Remove the hashmap.
            this.levelController.deleteLoot(packet.collectedLootIndex);

            // Broadcast received packet to everyone except the sender so other players know which loot was collected.
            for (String playerNickname : connections.keySet()) {
                if (!playerNickname.equals(packet.playerNickname)) {
                    Connection connection = connections.get(playerNickname);
                    connection.sendTCP(packet);
                }
            }
        }
    }

    // Run this method when a client disconnects.
    public void disconnected(Connection c) {
        String playerNicknameToRemove = null;
        for (String nickname : connections.keySet()) {
            // Search which player has disconnected.
            if (connections.get(nickname).equals(c)) {
                // Avoid modifying hashmap while iterating -> remove from player hashmaps after loop.
                playerNicknameToRemove = nickname;  // Save player (his nickname) to be removed.
                break;
            }
        } if (playerNicknameToRemove != null) {
            // Remove the player from hashmaps.
            connections.remove(playerNicknameToRemove);
            connectedPlayers.remove(playerNicknameToRemove);
            playersReady.remove(playerNicknameToRemove);
            broadcastPlayerDisconnected(playerNicknameToRemove);  // Inform other players who left the game.
            System.out.println("Player " + playerNicknameToRemove + " removed.");
        }
    }

    /**
     * Create and add new player object to connected players list.
     *
     * @param playerNickname nickname of the player
     */
    public void addPlayer(String playerNickname, Connection playerConnection) {
        Player newPlayer = new Player(100, 100, PLAYER_WIDTH, PLAYER_HEIGHT);
        connectedPlayers.put(playerNickname, newPlayer);
        connections.put(playerNickname, playerConnection);
        playersReady.put(playerNickname, false);
        broadcastPlayerConnected(playerNickname);
    }

    /**
     * Broadcast packet with new connected players so other are notified of it.
     */
    public void broadcastPlayerConnected(String newConnectedPlayer) {
        for (String connectedPlayer : connections.keySet()) {
            if (!connectedPlayer.equals(newConnectedPlayer)) {
                Connection connection = connections.get(connectedPlayer);
                PacketPlayerConnected packetPlayerConnected = new PacketPlayerConnected();
                packetPlayerConnected.teammateNickname = newConnectedPlayer;
                connection.sendTCP(packetPlayerConnected);
            }
        }
    }

    /**
     * Broadcast this packet so other players know who has disconnected.
     * @param disconnectedNickname nickname of the player who has disconnected.
     */
    public void broadcastPlayerDisconnected(String disconnectedNickname) {
        for (String connectedPlayer : connections.keySet()) {
            if (!connectedPlayer.equals(disconnectedNickname)) {
                Connection connection = connections.get(connectedPlayer);
                PacketPlayerDisconnected packetPlayerDisconnected = new PacketPlayerDisconnected();
                packetPlayerDisconnected.disconnectedPlayerNickname = disconnectedNickname;
                connection.sendTCP(packetPlayerDisconnected);
            }
        }
    }

    public void respondWithPacketGameIsOngoing(Connection connection) {
        connection.sendTCP(new PacketGameIsOngoing());
    }

    /**
     * Broadcast this packet to update mob objects on clients' side.
     *
     * This method is used in a loop in ServerUpdateThread.
     *
     * 0 - zombie, 1 - octopus.
     */
    public void broadcastUpdateMobPacket() {
        // Collect all mobs positions.
        Map<Integer, float[]> mobsPositions = new HashMap<>();
//        for (int mobId : mobController.getAllMobsSpawned().keySet()) {
        for (int mobId : this.levelController.getMobController().getAllMobsSpawned().keySet()) {
//            Enemy currentMob = mobController.getAllMobsSpawned().get(mobId);
            Enemy currentMob = this.levelController.getMobController().getAllMobsSpawned().get(mobId);
            float[] mobData = new float[4];
            mobData[0] = currentMob.getX();
            mobData[1] = currentMob.getY();

            if (currentMob.getType().equals("zombie")) {
                mobData[2] = 0;
            } else if (currentMob.getType().equals("octopus")) {
                mobData[2] = 1;
            } else if (currentMob.getType().equals("crab")) {
                mobData[2] = 2;
            } else if (currentMob.getType().equals("blueguy")) {
                mobData[2] = 3;
            } else if (currentMob.getType().equals("greenguy")) {
                mobData[2] = 4;
            }
            mobData[3] = currentMob.getHp();
            mobsPositions.put(mobId, mobData);
        }

        PacketUpdateMobsPos packetUpdateMobsPos = new PacketUpdateMobsPos();
        packetUpdateMobsPos.allEnemies = mobsPositions;
        for (String connectedPlayer : connections.keySet()) {
            Connection connection = connections.get(connectedPlayer);
            connection.sendUDP(packetUpdateMobsPos);
        }
    }

    // Broadcast this packet to everyone to notify that certain player was hit by a mob.
    public void broadcastPlayerHitPacket(String playerNickname) {

        PacketPlayerHit packetPlayerHit = new PacketPlayerHit();
        packetPlayerHit.playerNickname = playerNickname;

        for (String connectedPlayer : connections.keySet()) {
            // Send this packet to everyone.
            Connection connection = connections.get(connectedPlayer);
            connection.sendTCP(packetPlayerHit);
        }
    }

    public void broadCastPlayerReadyPacket(String playerNickname, boolean isPlayerReady) {

        PacketPlayerReady packetPlayerReady = new PacketPlayerReady();
        packetPlayerReady.playerNickname = playerNickname;
        packetPlayerReady.isPlayerReady = isPlayerReady;

        for (String connectedPlayer : connections.keySet()) {

            if (!connectedPlayer.equals(playerNickname)) {
                // Do not send this packet back to the sender.
                Connection connection = connections.get(connectedPlayer);
                connection.sendTCP(packetPlayerReady);
            }
        }
    }

    public void broadcastPacketGameBeginTimer(int timerValueCurrent, int timerStopValue) {
        PacketGameBeginTimer packetGameBeginTimer = new PacketGameBeginTimer();
        packetGameBeginTimer.timerValueCurrent = timerValueCurrent;
        packetGameBeginTimer.timerStopValue = timerStopValue + 1;

        for (String connectedPlayer : connections.keySet()) {
            Connection connection = connections.get(connectedPlayer);
            connection.sendTCP(packetGameBeginTimer);
        }
    }

    public void broadcastPacketLootSpawn(int lootSpawnIndex, int lootType, float spawnCoordinateX, float spawnCoordinateY) {
        PacketLootSpawn packetLootSpawn = new PacketLootSpawn();
        packetLootSpawn.spawnPosIndex = lootSpawnIndex;
        packetLootSpawn.lootType = lootType;
        packetLootSpawn.spawnCoordinateX = spawnCoordinateX;
        packetLootSpawn.spawnCoordinateY = spawnCoordinateY;

        for (String connectedPlayer : connections.keySet()) {
            Connection connection = connections.get(connectedPlayer);
            connection.sendTCP(packetLootSpawn);
        }
    }

    public void broadcastPacketSendStatistics() {
        PacketSendStatistics packetSendStatistics = new PacketSendStatistics();
        packetSendStatistics.statisticsString = this.statistics.getAllStatisticsAsString();

        for (String connectedPlayer : connections.keySet()) {
            Connection connection = connections.get(connectedPlayer);
            connection.sendTCP(packetSendStatistics);
        }
    }
}
