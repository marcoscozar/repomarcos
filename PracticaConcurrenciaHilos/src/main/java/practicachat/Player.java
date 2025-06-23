package practicachat;

import java.util.concurrent.atomic.AtomicInteger;

public class Player {

    private String name;
    private AtomicInteger hp;
    private AtomicInteger money;
    private String color;

    public Player(String name, String color) {
        this.name = name;
        this.hp = new AtomicInteger(50);
        this.money = new AtomicInteger(25);
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public int getHp() {
        return hp.get();
    }

    public int getMoney() {
        return money.get();
    }

    public String getColor() {
        return color;
    }

    public void reduceHp(int amount) {
        hp.updateAndGet(value -> Math.max(0, value - amount));
    }

    public void reduceMoney(int amount) {
        money.updateAndGet(value -> Math.max(0, value - amount));
    }

    public void addMoney(int amount) {
        money.addAndGet(amount);
    }

    @Override
    public String toString() {
        return color + name + "\u001B[0m - PV: " + hp + ", Dinero: " + money;
    }
}

