package se233.asteroidgame.model;

abstract class Character {
    protected int health;
    protected int x, y;
    protected int width, height;

    public Character(int health, int x, int y) {
        this.health = health;
        this.x = x;
        this.y = y;
    }

    public abstract void move();
    public abstract void collide(Character other);
    public abstract void takeDamage(int damage);
}
