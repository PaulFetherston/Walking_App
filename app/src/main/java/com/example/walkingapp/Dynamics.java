package com.example.walkingapp;

public class Dynamics {


    //Used to compare floats, if the difference is smaller than this, they are
    //considered equal
    private static final float TOLERANCE = 0.01f;

    //The current position of the dynamics
    private float position;

    // The current velocity of the dynamics
    private float velocity;

    // The time the last update happened
    private long lastTime;

    /**
     *
     * @param springiness value for springiness
     * @param dampingRatio value for dampingRatio
     */
    Dynamics(float springiness, float dampingRatio) {
        float damping = dampingRatio * 2 * (float) Math.sqrt(springiness);
    }

    public void setPosition(float position, long now) {
        this.position = position;
        lastTime = now;
    }

    public void setTargetPosition(long now) {
        lastTime = now;
    }

    public float getPosition() {
        return position;
    }


}
