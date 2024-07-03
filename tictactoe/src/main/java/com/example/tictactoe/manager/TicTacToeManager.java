package com.example.tictactoe.manager;

import com.example.tictactoe.model.TicTacToe;
import com.example.tictactoe.enumeration.GameState;
import com.example.tictactoe.model.TicTacToe;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lớp quản lý cho các trò chơi Tic-Tac-Toe.
 * Xử lý việc thêm và loại bỏ người chơi khỏi trò chơi, lưu trữ và truy xuất các trò chơi hiện tại.
 */
public class TicTacToeManager {

    /**
     * Bản đồ các trò chơi Tic-Tac-Toe đang hoạt động, với ID của trò chơi làm khóa.
     */
    private final Map<String, TicTacToe> games;

    /**
     * Bản đồ của các người chơi đang chờ tham gia vào trò chơi Tic-Tac-Toe, với tên của người chơi làm khóa.
     */
    protected final Map<String, String> waitingPlayers;

    /**
     * Tạo mới một TicTacToeManager.
     */
    public TicTacToeManager() {
        games = new ConcurrentHashMap<>();
        waitingPlayers = new ConcurrentHashMap<>();
    }

    /**
     * Cố gắng thêm một người chơi vào một trò chơi Tic-Tac-Toe hiện có, hoặc tạo một trò chơi mới nếu không có trò chơi nào mở sẵn.
     *
     * @param player tên của người chơi
     * @return trò chơi Tic-Tac-Toe mà người chơi đã được thêm vào
     */
    public synchronized TicTacToe joinGame(String player) {
        if (games.values().stream().anyMatch(game -> game.getPlayer1().equals(player) || (game.getPlayer2() != null && game.getPlayer2().equals(player)))) {
            return games.values().stream().filter(game -> game.getPlayer1().equals(player) || game.getPlayer2().equals(player)).findFirst().get();
        }

        for (TicTacToe game : games.values()) {
            if (game.getPlayer1() != null && game.getPlayer2() == null) {
                game.setPlayer2(player);
                game.setGameState(GameState.PLAYER1_TURN);
                return game;
            }
        }

        TicTacToe game = new TicTacToe(player, null);
        games.put(game.getGameId(), game);
        waitingPlayers.put(player, game.getGameId());
        return game;
    }

    /**
     * Loại bỏ một người chơi khỏi trò chơi Tic-Tac-Toe của họ. Nếu người chơi là người chơi duy nhất trong trò chơi,
     * trò chơi sẽ bị loại bỏ.
     *
     * @param player tên của người chơi
     */
    public synchronized TicTacToe leaveGame(String player) {
        String gameId = getGameByPlayer(player) != null ? getGameByPlayer(player).getGameId() : null;
        if (gameId != null) {
            waitingPlayers.remove(player);
            TicTacToe game = games.get(gameId);
            if (player.equals(game.getPlayer1())) {
                if (game.getPlayer2() != null) {
                    game.setPlayer1(game.getPlayer2());
                    game.setPlayer2(null);
                    game.setGameState(GameState.WAITING_FOR_PLAYER);
                    game.setBoard(new String[3][3]);
                    waitingPlayers.put(game.getPlayer1(), game.getGameId());
                } else {
                    games.remove(gameId);
                    return null;
                }
            } else if (player.equals(game.getPlayer2())) {
                game.setPlayer2(null);
                game.setGameState(GameState.WAITING_FOR_PLAYER);
                game.setBoard(new String[3][3]);
                waitingPlayers.put(game.getPlayer1(), game.getGameId());
            }
            return game;
        }
        return null;
    }

    /**
     * Trả về trò chơi Tic-Tac-Toe với ID trò chơi đã cho.
     *
     * @param gameId ID của trò chơi
     * @return trò chơi Tic-Tac-Toe với ID trò chơi đã cho, hoặc null nếu không có trò chơi nào như vậy tồn tại
     */
    public TicTacToe getGame(String gameId) {
        return games.get(gameId);
    }


    /**
     * Trả về trò chơi Tic-Tac-Toe mà người chơi đã cho đang tham gia.
     *
     * @param player tên của người chơi
     * @return trò chơi Tic-Tac-Toe mà người chơi đã cho đang tham gia, hoặc null nếu người chơi không tham gia trò chơi nào
     */
    public TicTacToe getGameByPlayer(String player) {
        return games.values().stream().filter(game -> game.getPlayer1().equals(player) || (game.getPlayer2() != null &&
                game.getPlayer2().equals(player))).findFirst().orElse(null);
    }

    /**
     * Loại bỏ trò chơi Tic-Tac-Toe với ID trò chơi đã cho.
     *
     * @param gameId ID của trò chơi cần loại bỏ
     */
    public void removeGame(String gameId) {
        games.remove(gameId);
    }
}
