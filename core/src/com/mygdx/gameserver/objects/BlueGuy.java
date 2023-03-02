package com.mygdx.gameserver.objects;

public class BlueGuy extends Enemy {
    public BlueGuy(float x, float y, float width, float height, double speed, int hp) {
        super(x, y, width, height, speed, hp);
    }

    @Override
    public String getType() {
        return "blueguy";
    }
}
