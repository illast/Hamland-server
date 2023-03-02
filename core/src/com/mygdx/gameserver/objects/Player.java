package com.mygdx.gameserver.objects;

public class Player extends GameObject {

    private final static int PLAYER_HP = 3;

    private Character character;
    private float x;
    private float y;
    private float width;
    private float height;
    private int hp;
    private float rotation;
    private boolean isImmune;

    /**
     * Constructor for all objects on the screen.
     *
     * @param x         X-coordinate.
     * @param y         Y-coordinate.
     * @param width     object width.
     * @param height    object height.
     */
    public Player(float x, float y, float width, float height) {
        super(x, y, width, height);

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.hp = PLAYER_HP;
//        this.character = character;
        this.rotation = 0;
        this.isImmune = false;
    }

    public float getX() {
        return x;
    }

    public void setX(float newX) {
        x = newX;
    }

    public float getY() {
        return y;
    }

    public void setY(float newY) {
        y = newY;
    }

    public float getRotation() {
        return rotation;
    }

    public void setRotation(float newRotation) {
        rotation = newRotation;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public Character getCharacter() {
        return character;
    }

    public void setCharacter(Character newCharacter) {
        character = newCharacter;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getHp() {
        return this.hp;
    }

    public boolean isImmune() {
        return isImmune;
    }

    public void setIsImmune(boolean state) {
        isImmune = state;
    }
}
