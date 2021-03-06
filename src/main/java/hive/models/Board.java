package hive.models;

import dk.ilios.asciihexgrid.AsciiBoard;
import dk.ilios.asciihexgrid.printers.LargeFlatAsciiHexPrinter;
import hive.game.PlayerClass;
import nl.hanze.hive.Hive;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class Board {

    private Map<Hex, BoardTile> boardMap;
    private List<Hex> possibleDirections = new ArrayList<>();

    public Board() {
        boardMap = new HashMap<>();

        possibleDirections.add(new Hex(-1, 0));
        possibleDirections.add(new Hex(1, 0));
        possibleDirections.add(new Hex(-1, 1));
        possibleDirections.add(new Hex(1, -1));
        possibleDirections.add(new Hex(0, -1));
        possibleDirections.add(new Hex(0, 1));
    }

    public Map getBoardMap() {
        return boardMap;
    }

    public void placeStone(PlayerClass playerClass, Hive.Tile tile, Integer q, Integer r) throws Hive.IllegalMove {
        Hex coordinates = new Hex(q, r);

        if(playerClass.getQueenCount() == 1 && tile != Hive.Tile.QUEEN_BEE && playerClass.getAmountOfMoves() == 3) {
            throw new Hive.IllegalMove("Er moet nu een bij gelegd worden");

        } else if(! areCoordinatesAlreadySet(coordinates)) {
            if (playerClass.getAmountOfMoves() == 0 && playerClass.getPlayerEnum() == Hive.Player.BLACK ) {
                if (getTileNeighbors(new Hex(q, r)).isEmpty()) {
                    throw new Hive.IllegalMove("Je moet de eerste beurt naast de tegestander leggen");
                }
            } else if (playerClass.getAmountOfMoves() > 0) {
                if (hasOpponentNeighbor(coordinates, playerClass)) {
                    throw new Hive.IllegalMove("Er mag niet naast de tegenstander gelegd worden");
                } else if (!hasOwnNeighbor(coordinates, playerClass)) {
                    throw new Hive.IllegalMove("Er moet naast een eigen steen gelegd worden");
                }
            }
            BoardTile boardTile = new BoardTile(tile, playerClass);
            playerClass.deductTile(tile);
            boardMap.put(coordinates, boardTile);
            playerClass.madeMove();
        } else {
            throw new Hive.IllegalMove("Dit mag niet");
        }
    }

    public void moveStone(PlayerClass currentPlayer, Integer fromQ, Integer fromR, Integer toQ, Integer toR) throws Hive.IllegalMove {

        if (currentPlayer.getQueenCount() == 0) {
            Hex oldCoordinate = new Hex(fromQ, fromR);
            Hex newCoordinate = new Hex(toQ, toR);
            BoardTile tileToMove = boardMap.get(oldCoordinate);

            List<Hex> movesForStone = getMovesPerStone(tileToMove, oldCoordinate);

            if (movesForStone.contains(newCoordinate)) {
                if (canTileBeMovedFromOldPlace(tileToMove, currentPlayer) && canTileBePlacedOnNewCoordinates(newCoordinate, tileToMove)) {
                    moveStoneAndDeleteTileIfEmpty(oldCoordinate, newCoordinate);
                } else {
                    throw new Hive.IllegalMove("Tile kan niet verplaasts worden");
                }

            } else {
                throw new Hive.IllegalMove("Move mag niet gemaakt worden");
            }

        } else {
            throw new Hive.IllegalMove("Je moet eerst de queen hebben gelegd voor dat je kan verplaatsen");
        }
    }

    public void printBoard() {
        if (! boardMap.isEmpty()) {
            int minQ = 0;
            int minR = 0;

            for (Hex coordinates : boardMap.keySet()) {
                if (coordinates.getKey() < minQ) {
                    minQ = coordinates.getKey();
                }

                if (coordinates.getValue() < minR) {
                    minR = coordinates.getValue();
                }
            }

            AsciiBoard asciiBoard = new AsciiBoard(0, 30, 0, 30, new LargeFlatAsciiHexPrinter());

            for (Map.Entry<Hex, BoardTile> entry : boardMap.entrySet()) {
                Hex coordinates = entry.getKey();
                BoardTile tile = boardMap.get(entry.getKey());
                String tileType = tile.getTopTileType().toString();
                String owner = tile.getTopTileOwner().getPlayerEnum().toString();
                String bottomString = tileType + " " + owner;
                asciiBoard.printHex(coordinates.toString(), bottomString, ' ', coordinates.getKey() - minQ, coordinates.getValue() - minR);
            }

            System.out.println(asciiBoard.prettPrint(true));
        } else {
            System.out.println("BOARD IS EMPTY MY DUDE");
        }
    }

    public boolean canTileBePlacedForPlayer(PlayerClass player) {
        Hive.Tile tile = player.getTilesLeft().get(0);

        for (Map.Entry<Hex, BoardTile> entry : boardMap.entrySet()) {
            if (entry.getValue().getTopTileOwner() == player) {
                List<Hex> emptyNeighbors = getEmptyNeighbors(entry.getKey());
                for (Hex neighbor : emptyNeighbors) {
                    try {
                        placeStone(player, tile, neighbor.getKey(), neighbor.getValue());
                        return true;
                    } catch (Hive.IllegalMove ignore) { }
                }
            }
        }
        return false;
    }

    public boolean isQueenOfOpponentSurrounded(PlayerClass player) {
        for (Map.Entry<Hex, BoardTile> entry : boardMap.entrySet()) {
            if (boardMap.get(entry.getKey()).isQueenOfOpponentOnStack(player) && getTileNeighbors(entry.getKey()).size() == 6) {
                return true;
            }
        }
        return false;
    }

    Integer amountOfTiles(){
        return boardMap.size();
    }

    List<Hex> getAllNeighbors(Hex coordinatesOfCurrent) {
        List<Hex> neighbors = new ArrayList<>();

        for (Hex possibleNeighbors : possibleDirections) {
            Integer newQ = possibleNeighbors.getKey() + coordinatesOfCurrent.getKey();
            Integer newR = possibleNeighbors.getValue() + coordinatesOfCurrent.getValue();
            neighbors.add(new Hex(newQ, newR));
        }

        return neighbors;
    }


    private List<Hex> getMovesPerStone(BoardTile tile, Hex oldCoordinates) throws Hive.IllegalMove {
        List<Hex> possibleMoveDirections;

        switch (tile.getTopTileType()) {
            case QUEEN_BEE:
                possibleMoveDirections = queenBee(oldCoordinates);
                break;
            case BEETLE:
                possibleMoveDirections = beetle(getAllNeighbors(oldCoordinates), oldCoordinates, tile);
                break;
            case SOLDIER_ANT:
                possibleMoveDirections = soldierAnt(oldCoordinates);
                break;
            case GRASSHOPPER:
                possibleMoveDirections = grassHopper(getTileNeighbors(oldCoordinates), oldCoordinates);
                break;
            case SPIDER:
                possibleMoveDirections = spider(oldCoordinates);
                break;
            default:
                throw new Hive.IllegalMove("Ik heb geen idee wat er gebeurt is");
        }

        return possibleMoveDirections;
    }

    private boolean canTileBeMovedFromOldPlace(BoardTile tileToMove, PlayerClass currentPlayer) {
        if (tileToMove.getTopTileOwner() == currentPlayer && tileToMove.getStackSize() == 2) {
            return true;
        }
        return (tileToMove.getTopTileOwner() == currentPlayer);
    }

    private boolean canTileBePlacedOnNewCoordinates(Hex newCoordinates, BoardTile tile) {
        boolean canBeMoved = false;

        if (boardMap.get(newCoordinates) == null || tile.getTopTileType() == Hive.Tile.BEETLE && boardMap.get(newCoordinates).getStackSize() == 1) {
            canBeMoved = true;
        }

        return canBeMoved;
    }

    private void moveStoneAndDeleteTileIfEmpty(Hex oldCoordinates, Hex newCoordinates) throws Hive.IllegalMove {
        BoardTile boardTileToMoveFrom = boardMap.get(oldCoordinates);
        Pair<Hive.Tile, PlayerClass> tileToBeMoved = boardTileToMoveFrom.removeTopTile();

        Map< Hex, BoardTile> boardCopy = new HashMap<>(boardMap);

        if (boardMap.get(newCoordinates) == null) {
            BoardTile newTile = new BoardTile(tileToBeMoved.getKey(), tileToBeMoved.getValue());
            boardMap.put(newCoordinates, newTile);
        } else {
            BoardTile tile = boardMap.get(newCoordinates);
            tile.addToStack(tileToBeMoved.getKey(), tileToBeMoved.getValue());
        }


        if (boardTileToMoveFrom.getStackSize() == 0) {
            boardMap.remove(oldCoordinates);
        }

        if (! isHiveIntactAfterMove(newCoordinates)) {
            boardMap.putAll(boardCopy);
            throw new Hive.IllegalMove("De hive is niet meer intact");
        }
    }

    boolean isHiveIntactAfterMove(Hex newPlace) {
        List<Hex> neighbors = getTileNeighbors(newPlace);

        if (! neighbors.isEmpty()) {
            Hex tile = neighbors.get(0);
            Set<Hex> marked = dfs(tile, newPlace, new HashSet<>());
            return (marked.size() == (boardMap.size() - 1));

        } else {
            return false;
        }
    }

    private List<Hex> getTileNeighbors(Hex coordinatesOfCurrent) {
        List<Hex> tileNeighbors = new ArrayList<>();
        List<Hex> neighbors = getAllNeighbors(coordinatesOfCurrent);

        for (Hex neighbor : neighbors) {
            if (boardMap.containsKey(neighbor)) {
                tileNeighbors.add(neighbor);
            }
        }
        return tileNeighbors;
    }

    private List<Hex> getEmptyNeighbors(Hex coordinatesOfCurrent) {
        List<Hex> emptyNeighbors = new ArrayList<>();
        List<Hex> neighbors = getAllNeighbors(coordinatesOfCurrent);

        for (Hex neighbor : neighbors) {
            if (! boardMap.containsKey(neighbor)) {
                emptyNeighbors.add(neighbor);
            }
        }
        return emptyNeighbors;
    }

    private boolean areCoordinatesAlreadySet(Hex coordinates) {
        for (Hex key : boardMap.keySet()) {
            if (key.equals(coordinates)) {
                return true;
            }
        }
        return false;
    }

    boolean hasOpponentNeighbor(Hex coordinates, PlayerClass currentPlayer) {
        for (Hex neighgbor : getTileNeighbors(coordinates)) {
            if (boardMap.get(neighgbor).getTopTileOwner() != currentPlayer){
                return true;
            }
        }
        return false;
    }

    private boolean hasOwnNeighbor(Hex coordinates, PlayerClass currentPlayer) {
        for (Hex neighgbor : getTileNeighbors(coordinates)) {
            if (boardMap.get(neighgbor).getTopTileOwner() == currentPlayer){
                return true;
            }
        }
        return false;
    }

    private Set<Hex> dfs(Hex tile, Hex ignore, Set<Hex> visited ) {
        visited.add(tile);

        List<Hex> neighbors = getTileNeighbors(tile);

        for (Hex neighborTile : neighbors){
            if(neighborTile.equals(ignore) || visited.contains(neighborTile)) continue;
            dfs(neighborTile, ignore, visited);
        }

        return visited;
    }

    private List<Hex> queenBee(Hex coordinates) {
        return new ArrayList<>(recursiveForEmptyPlaces(new HashSet<>(), coordinates, 0, coordinates, 1));
    }

    private List<Hex> spider(Hex coordinates) {
        return new ArrayList<>(recursiveForEmptyPlaces(new HashSet<>(), coordinates, 0, coordinates, 3));
    }


    private List<Hex> beetle(List<Hex> allNeighbors, Hex coordinates, BoardTile tile) {

        List<Hex> possibleMoves = new ArrayList<>();

        for (Hex neighbor : allNeighbors) {
            if (boardMap.get(neighbor) == null && canTileBeMovedInGap(coordinates, neighbor) && hiveStaysIntactWhileMoving(coordinates, neighbor)){
                possibleMoves.add(neighbor);
            } else if (boardMap.get(neighbor) != null && boardMap.get(neighbor).getStackSize() == tile.getStackSize()) {
                possibleMoves.add(neighbor);
            }
        }
        return possibleMoves;
    }

    private List<Hex> soldierAnt(Hex coordinates) {
        return new ArrayList<>(recursiveForEmptyPlaces(new HashSet<>(), coordinates, 0, coordinates, Integer.MAX_VALUE));
    }

    private Set<Hex> recursiveForEmptyPlaces(Set<Hex> visited, Hex currentPlace, int recursionDepth, Hex originalLocation, Integer maxRecursion) {
        if (recursionDepth < maxRecursion) {
            recursionDepth = recursionDepth + 1;
            for (Hex neighborOfCurrent : getEmptyNeighbors(currentPlace)) {
                List<Hex> tilesOfNeighborOfCurrent = getTileNeighbors(neighborOfCurrent);
                if (! tilesOfNeighborOfCurrent.isEmpty() && !visited.contains(neighborOfCurrent) && hiveStaysIntactWhileMoving(currentPlace, neighborOfCurrent)) {
                    if (!(tilesOfNeighborOfCurrent.size() == 1 && tilesOfNeighborOfCurrent.contains(originalLocation)) && canTileBeMovedInGap(currentPlace, neighborOfCurrent)) {
                        visited.add(neighborOfCurrent);
                        recursiveForEmptyPlaces(visited, neighborOfCurrent, recursionDepth, originalLocation, maxRecursion);
                    }
                }
            }
        }
        return visited;
    }

    private List<Hex> grassHopper(List<Hex> tileNeighbors, Hex coordinates) {

        List<Hex> possibleMoveDirections = new ArrayList<>();
        List<Hex> endCoordinates = new ArrayList<>();

        for (Hex direction : possibleDirections) {
            Integer newQ = direction.getKey() + coordinates.getKey();
            Integer newR = direction.getValue() + coordinates.getValue();
            if (tileNeighbors.contains(new Hex(newQ, newR))) {
                possibleMoveDirections.add(direction);
            }
        }

        for (Hex possibleDirection : possibleMoveDirections) {
            endCoordinates.add(findEmptyTileInDirection(possibleDirection, coordinates));
        }

        return endCoordinates;
    }

    private Hex findEmptyTileInDirection(Hex direction, Hex startPosition) {
        Integer newQ = direction.getKey() + startPosition.getKey();
        Integer newR = direction.getValue() + startPosition.getValue();
        Hex newCoord = new Hex(newQ, newR);
        if (boardMap.containsKey(newCoord)) {
            newCoord = findEmptyTileInDirection(direction, newCoord);
        }

        return newCoord;
    }

    private boolean canTileBeMovedInGap(Hex currentLocation, Hex emptyNeighbor) {
        List<Hex> usedTileNeighbors = getTileNeighbors(currentLocation);
        List<Hex> usedTileNeighborsEmptyNeighbor = getTileNeighbors(emptyNeighbor);

        int neighborCounter = 0;

        for (Hex usedTile : usedTileNeighbors) {
            if (usedTileNeighborsEmptyNeighbor.contains(usedTile)) {
                neighborCounter++;
            }
        }
        return (neighborCounter < 2);
    }

    private boolean hiveStaysIntactWhileMoving(Hex oldCoordinates, Hex newCoordinates){
        List<Hex> tileNeighborsOld = getTileNeighbors(oldCoordinates);
        List<Hex> tileNeighborsNew = getTileNeighbors(newCoordinates);
        for(Hex neigbour : tileNeighborsNew){
            if (tileNeighborsOld.contains(neigbour)|| tileNeighborsOld.contains(newCoordinates)){
                return true;
            }
        }
        return false;
    }

    public boolean canPlayerMove(PlayerClass player) {
        Map<Hex, BoardTile> boardCopy = new HashMap<>(boardMap);
        for (Map.Entry<Hex, BoardTile> tile : boardCopy.entrySet()) {
            BoardTile boardTile = tile.getValue();
            Hex tileLocation = tile.getKey();

            if (boardTile.getTopTileOwner() == player) {
                try {
                    List<Hex> movesPerStone = getMovesPerStone(boardTile, tileLocation);
                    if (! movesPerStone.isEmpty()) {
                        for (Hex move : movesPerStone) {
                            tryMove(player, tileLocation, move);
                            return true;
                        }
                    }
                } catch (Hive.IllegalMove ignore) { }
            }
        }
        return false;
    }

    private void tryMove(PlayerClass player, Hex tileLocation, Hex move) {
        try {
            moveStone(player, tileLocation.getKey(), tileLocation.getValue(), move.getKey(), move.getValue());
            moveStone(player, move.getKey(), move.getValue(), tileLocation.getKey(), tileLocation.getValue());
        } catch (Hive.IllegalMove ignore) { }
    }

}
