package hive.models;

import hive.interfaces.Hive.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Iterator;
import java.util.Stack;

public class BoardTile {

    private Stack<Pair<Tile, PlayerClass>> tileStack = new Stack<>();

    public BoardTile(Tile tile, PlayerClass playerClass) {
        this.tileStack.add(makePair(tile, playerClass));
    }

    public void addToStack(Tile tile, PlayerClass playerClass) {
        tileStack.add(makePair(tile, playerClass));
        System.out.println(tileStack.size());
    }

    public Tile getTopTileType() {
        return tileStack.peek().getKey();
    }

    public PlayerClass getTopTileOwner() {
        return tileStack.peek().getValue();
    }

    public Pair<Tile, PlayerClass> removeTopTile() {
        return tileStack.pop();
    }

    public Integer getStackSize() {
        return tileStack.size();
    }

    private Pair makePair(Tile tileface, PlayerClass playerClass) {
        return Pair.of(tileface, playerClass);
    }

    public String tilesOnStackToString() {
        Iterator iterator = tileStack.iterator();
        StringBuilder stringBuilder = new StringBuilder();
        System.out.println("stacksize " + tileStack.size());
        while (iterator.hasNext()) {
            Pair<Tile, PlayerClass> pair = (Pair<Tile, PlayerClass>) iterator.next();
            stringBuilder.append(" positie:")
                    .append(tileStack.indexOf(pair))
                    .append(' ')
                    .append(pair.getKey())
                    .append(" is van ")
                    .append(pair.getValue().getPlayerEnum());
        }
        return stringBuilder.toString();
    }

}
