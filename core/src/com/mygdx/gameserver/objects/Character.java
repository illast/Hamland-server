package com.mygdx.gameserver.objects;

public class Character {

    private final String name;
    private int damage;
    private int maxHealth;
    private int currentHealth;
    private int speed;

    /**
     * Constructor.
     *
     * @param name      name of the character
     * @param damage    amount of damage character can deal to mobs (NPCs)
     * @param maxHealth maximum amount of HP character has.
     * @param speed     speed character moves with
     */
    public Character(String name, int damage, int maxHealth, int speed) {
        this.name = name;
        this.damage = damage;
        this.maxHealth = maxHealth;
        this.speed = speed;
    }

    public String getName() {
        return name;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int newDamage) {
        damage = newDamage;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public void setCurrentHealth(int currentHealth) {
        this.currentHealth = currentHealth;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(int newMaxHealth) {
        maxHealth = newMaxHealth;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int newSpeed) {
        speed = newSpeed;
    }
}
