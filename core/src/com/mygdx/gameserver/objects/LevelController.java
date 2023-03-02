package com.mygdx.gameserver.objects;

import com.mygdx.gameserver.server.KryoServer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class LevelController {

    private static final int TIME_BETWEEN_NEXT_LOOT_SPAWN = 3; // In seconds.

    private KryoServer server;
    private MobController mobController;
    private int currentWave;
    private boolean isActive;

    // Timer
    private long timerStartTime;
    long delta;

    private final Map<Integer, float[]> lootSpawnPositions = new HashMap<>();
    private final Map<Integer, Boolean> lootSpawnExistenceOnPosition = new HashMap<>();
    private int lastLootSpawnPositionIndex;

    public LevelController(KryoServer server) {
        this.server = server;
        this.timerStartTime = System.currentTimeMillis();

        this.mobController = new MobController(this.server);
        this.currentWave = 1;
        this.isActive = true;  // Use this to manually disable mobs spawn (for testing).

        // Fill map of loot positions.
        this.lootSpawnPositions.put(0, new float[]{1000f, 1050f});
        this.lootSpawnPositions.put(1, new float[]{950f, 1050f});
        this.lootSpawnPositions.put(2, new float[]{900f, 1050f});
        this.lootSpawnPositions.put(3, new float[]{1200f, 1050f});
        this.lootSpawnPositions.put(4, new float[]{1250f, 1050f});
        this.lootSpawnPositions.put(5, new float[]{1300f, 1050f});
        this.lootSpawnPositions.put(6, new float[]{600f, 1750f});
        this.lootSpawnPositions.put(7, new float[]{250f, 1450f});
        this.lootSpawnPositions.put(8, new float[]{125f, 1050f});
        this.lootSpawnPositions.put(9, new float[]{400f, 900f});
        this.lootSpawnPositions.put(10, new float[]{125f, 650f});
        this.lootSpawnPositions.put(11, new float[]{250f, 500f});
        this.lootSpawnPositions.put(12, new float[]{650f, 450f});
        this.lootSpawnPositions.put(13, new float[]{700f, 150f});
        this.lootSpawnPositions.put(14, new float[]{950f, 250f});
        this.lootSpawnPositions.put(15, new float[]{1800f, 125f});
        this.lootSpawnPositions.put(16, new float[]{1300f, 500f});
        this.lootSpawnPositions.put(17, new float[]{1200f, 750f});
        this.lootSpawnPositions.put(18, new float[]{1400f, 850f});

        // Fill map of loot existence on certain position.
        this.lootSpawnExistenceOnPosition.put(0, false);
        this.lootSpawnExistenceOnPosition.put(1, false);
        this.lootSpawnExistenceOnPosition.put(2, false);
        this.lootSpawnExistenceOnPosition.put(3, false);
        this.lootSpawnExistenceOnPosition.put(4, false);
        this.lootSpawnExistenceOnPosition.put(5, false);
        this.lootSpawnExistenceOnPosition.put(6, false);
        this.lootSpawnExistenceOnPosition.put(7, false);
        this.lootSpawnExistenceOnPosition.put(8, false);
        this.lootSpawnExistenceOnPosition.put(9, false);
        this.lootSpawnExistenceOnPosition.put(10, false);
        this.lootSpawnExistenceOnPosition.put(11, false);
        this.lootSpawnExistenceOnPosition.put(12, false);
        this.lootSpawnExistenceOnPosition.put(13, false);
        this.lootSpawnExistenceOnPosition.put(14, false);
        this.lootSpawnExistenceOnPosition.put(15, false);
        this.lootSpawnExistenceOnPosition.put(16, false);
        this.lootSpawnExistenceOnPosition.put(17, false);
        this.lootSpawnExistenceOnPosition.put(18, false);

        this.lastLootSpawnPositionIndex = -1;
    }

    public void beginNextWave() {

        if (this.isActive) {
            this.spawnLootRandomly();
            if (!this.getIsWaveOngoing()) {
                // If there is no currently ongoing wave.
                switch (this.currentWave) {
                    // The higher the wave the more difficult it is for players to survive.
                    case 1:
                        this.mobController.spawnMob("zombie", 2, 500, 500);
                        break;

                    case 2:
                        this.mobController.spawnMob("blueguy", 4, 300, 800);
                        break;

                    case 3:
                        this.mobController.spawnMob("zombie", 4, 500, 1200);
                        this.mobController.spawnMob("crab", 2, 300, 800);
                        break;

                    case 4:
                        this.mobController.spawnMob("zombie", 3, 1500, 700);
                        this.mobController.spawnMob("octopus", 3, 300, 400);
                        break;

                    case 5:
                        this.mobController.spawnMob("crab", 6, 300, 800);
                        break;

                    case 6:
                        this.mobController.spawnMob("zombie", 8, 0, 300);
                        this.mobController.spawnMob("zombie", 4, 500, 1700);
                        break;

                    case 7:
                        this.mobController.spawnMob("blueguy", 9, 300, 800);
                        this.mobController.spawnMob("blueguy", 6, 1300, 0);
                        break;

                    case 8:
                        this.mobController.spawnMob("octopus", 5, 300, 400);
                        this.mobController.spawnMob("zombie", 8, 1500, 700);
                        break;

                    case 9:
                        this.mobController.spawnMob("blueguy", 6, 1300, 0);
                        this.mobController.spawnMob("zombie", 8, 1500, 700);
                        break;

                    case 10:
                        this.mobController.spawnMob("crab", 6, 300, 800);
                        this.mobController.spawnMob("blueguy", 12, 1300, 0);
                        this.mobController.spawnMob("blueguy", 12, 0, 0);
                        this.currentWave = 1; // FOR WAVE LOOP.
                        break;
                }

                // Increment next wave number.
                this.currentWave++;
            }
        }
    }

    public boolean getIsWaveOngoing() {
        // If there are no mobs alive -> there is no ongoing wave and new wave can begin.
        return this.mobController.getAllMobsSpawned().size() != 0;
    }

    /**
     * When any player collects any loot -> delete it from the hashmap, so it can spawn again on this position later.
     *
     * @param lootSpawnPositionIndex index to delete loot by
     */
    public void deleteLoot(int lootSpawnPositionIndex) {
        this.lootSpawnExistenceOnPosition.put(lootSpawnPositionIndex, false);
    }

    public void spawnLootRandomly() {
        delta = (System.currentTimeMillis() - this.timerStartTime) / 1000;
        if (delta > TIME_BETWEEN_NEXT_LOOT_SPAWN) {
            // Spawn loot every TIME_BETWEEN_NEXT_LOOT_SPAWN seconds.
            this.timerStartTime = System.currentTimeMillis();
            // Generate random loot position index withing known range (0 - 18).
            byte min = 0;
            byte max = 18;
            int randomLootSpawnIndex = ThreadLocalRandom.current().nextInt(min, max + 1);

            if (this.lastLootSpawnPositionIndex != randomLootSpawnIndex
                    && !this.lootSpawnExistenceOnPosition.get(randomLootSpawnIndex)) {
                // If loot on this position was not spawned yet -> spawn it.
                this.lastLootSpawnPositionIndex = randomLootSpawnIndex;

                // Tick that loot was spawned.
                this.lootSpawnExistenceOnPosition.put(randomLootSpawnIndex, true);

                // Randomly determine what kind of loot it would be (ammo pack or med kit).
                int randomLootType = ThreadLocalRandom.current().nextInt(0, 2);

                // Get the predetermined loot spawn coordinates by spawn index.
                float lootSpawnCoordinateX = this.lootSpawnPositions.get(randomLootSpawnIndex)[0];
                float lootSpawnCoordinateY = this.lootSpawnPositions.get(randomLootSpawnIndex)[1];

                // Send loot spawn packet.
                this.server.broadcastPacketLootSpawn(randomLootSpawnIndex, randomLootType, lootSpawnCoordinateX, lootSpawnCoordinateY);
            }
        }
    }

    public MobController getMobController() {
        return this.mobController;
    }

    public void reset() {
        // Reset the loot spawner.
        this.lastLootSpawnPositionIndex = -1;
        for (int lootIndex : this.lootSpawnExistenceOnPosition.keySet()) {
            this.lootSpawnExistenceOnPosition.put(lootIndex, false);
        }

        // Reset the mob controller.
        this.mobController.reset();
    }
}
