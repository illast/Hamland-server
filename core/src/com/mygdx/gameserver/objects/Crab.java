package com.mygdx.gameserver.objects;

public class Crab extends Enemy{
    public Crab(float x, float y, float width, float height, double speed, int hp) {
        super(x, y, width, height, speed, hp);
    }

    @Override
    public String getType() {
        return "crab";
    }
}
