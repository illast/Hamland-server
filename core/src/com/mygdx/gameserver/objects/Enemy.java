package com.mygdx.gameserver.objects;

public class Enemy {

    private float x;
    private float y;
    private float width;
    private float height;

    private final double speed;
    private int hp;
    private static int mobId;
    private final int id;

    public Enemy(float x, float y, float width, float height, double speed, int hp) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.speed = speed;
        this.hp = hp;
        this.id = getAndIncrementNextId();
    }

    public static int getAndIncrementNextId() {
        return mobId++;
    }

    public int getId() {
        return id;
    }

    public float getX() {
        return this.x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return this.y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public String getType() {
        return "";
    }

    public double getSpeed() {
        return this.speed;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}
