package com.example.tictactoe.controller;

import com.example.tictactoe.enumeration.GameState;
import com.example.tictactoe.manager.TicTacToeManager;
import com.example.tictactoe.model.TicTacToe;
import com.example.tictactoe.model.dto.JoinMessage;
import com.example.tictactoe.model.dto.PlayerMessage;
import com.example.tictactoe.model.dto.TicTacToeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Lớp Controller để xử lý các tin nhắn WebSocket và quản lý trò chơi Tic-Tac-Toe.
 */
@Controller
public class MessageController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Quản lý các trò chơi Tic-Tac-Toe.
     */
    private final TicTacToeManager ticTacToeManager = new TicTacToeManager();

    /**
     * Xử lý yêu cầu từ một người chơi để tham gia vào một trò chơi Tic-Tac-Toe.
     * Nếu có trò chơi sẵn có và người chơi được thêm thành công vào trò chơi,
     * trạng thái hiện tại của trò chơi sẽ được gửi đến tất cả người đăng ký của chủ đề trò chơi.
     * @param message tin nhắn từ khách hàng chứa tên người chơi
     * @return trạng thái hiện tại của trò chơi hoặc một tin nhắn lỗi nếu không thể tham gia trò chơi
     */
    @MessageMapping("/game.join")
    @SendTo("/topic/game.state")
    public Object joinGame(@Payload JoinMessage message, SimpMessageHeaderAccessor headerAccessor) {
        TicTacToe game = ticTacToeManager.joinGame(message.getPlayer());
        if (game == null) {
            TicTacToeMessage errorMessage = new TicTacToeMessage();
            errorMessage.setType("error");
            errorMessage.setContent("Não foi possível entrar no jogo. Talvez o jogo já esteja cheio ou ocorreu um erro interno.");
            return errorMessage;
        }
        headerAccessor.getSessionAttributes().put("gameId", game.getGameId());
        headerAccessor.getSessionAttributes().put("player", message.getPlayer());

        TicTacToeMessage gameMessage = gameToMessage(game);
        gameMessage.setType("game.joined");
        return gameMessage;
    }

    /**
     * Xử lý yêu cầu từ một khách hàng để rời khỏi một trò chơi Tic-Tac-Toe.
     * Nếu người chơi được rời khỏi trò chơi thành công, một tin nhắn sẽ được gửi đến những người đăng ký
     * của chủ đề trò chơi chỉ ra rằng người chơi đã rời đi.
     *
     * @param message tin nhắn từ khách hàng chứa tên người chơi
     */
    @MessageMapping("/game.leave")
    public void leaveGame(@Payload PlayerMessage message) {
        TicTacToe game = ticTacToeManager.leaveGame(message.getPlayer());
        if (game != null) {
            TicTacToeMessage gameMessage = gameToMessage(game);
            gameMessage.setType("game.left");
            messagingTemplate.convertAndSend("/topic/game." + game.getGameId(), gameMessage);
        }
    }

    /**
     * Xử lý yêu cầu từ một khách hàng để thực hiện một bước đi trong trò chơi Tic-Tac-Toe.
     * Nếu bước đi hợp lệ, trạng thái của trò chơi được cập nhật và gửi đến tất cả người đăng ký của chủ đề trò chơi.
     * Nếu trò chơi kết thúc, một tin nhắn sẽ được gửi chỉ ra kết quả của trò chơi.
     *
     * @param message tin nhắn từ khách hàng chứa tên người chơi, ID trò chơi, và bước đi
     */
    @MessageMapping("/game.move")
    public void makeMove(@Payload TicTacToeMessage message) {
        String player = message.getSender();
        String gameId = message.getGameId();
        int move = message.getMove();
        TicTacToe game = ticTacToeManager.getGame(gameId);

        if (game == null || game.isGameOver()) {
            TicTacToeMessage errorMessage = new TicTacToeMessage();
            errorMessage.setType("error");
            errorMessage.setContent("Game not found or is already over.");
            this.messagingTemplate.convertAndSend("/topic/game." + gameId, errorMessage);
            return;
        }

        if (game.getGameState().equals(GameState.WAITING_FOR_PLAYER)) {
            TicTacToeMessage errorMessage = new TicTacToeMessage();
            errorMessage.setType("error");
            errorMessage.setContent("Game is waiting for another player to join.");
            this.messagingTemplate.convertAndSend("/topic/game." + gameId, errorMessage);
            return;
        }

        if (game.getTurn().equals(player)) {
            game.makeMove(player, move);

            TicTacToeMessage gameStateMessage = new TicTacToeMessage(game);
            gameStateMessage.setType("game.move");
            this.messagingTemplate.convertAndSend("/topic/game." + gameId, gameStateMessage);

            if (game.isGameOver()) {
                TicTacToeMessage gameOverMessage = gameToMessage(game);
                gameOverMessage.setType("game.gameOver");
                this.messagingTemplate.convertAndSend("/topic/game." + gameId, gameOverMessage);
                ticTacToeManager.removeGame(gameId);
            }
        }
    }

    @EventListener
    public void SessionDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String gameId = headerAccessor.getSessionAttributes().get("gameId").toString();
        String player = headerAccessor.getSessionAttributes().get("player").toString();
        TicTacToe game = ticTacToeManager.getGame(gameId);
        if (game != null) {
            if (game.getPlayer1().equals(player)) {
                game.setPlayer1(null);
                if (game.getPlayer2() != null) {
                    game.setGameState(GameState.PLAYER2_WON);
                    game.setWinner(game.getPlayer2());
                } else {
                    ticTacToeManager.removeGame(gameId);
                }
            } else if (game.getPlayer2() != null && game.getPlayer2().equals(player)) {
                game.setPlayer2(null);
                if (game.getPlayer1() != null) {
                    game.setGameState(GameState.PLAYER1_WON);
                    game.setWinner(game.getPlayer1());
                } else {
                    ticTacToeManager.removeGame(gameId);
                }
            }
            TicTacToeMessage gameMessage = gameToMessage(game);
            gameMessage.setType("game.gameOver");
            messagingTemplate.convertAndSend("/topic/game." + gameId, gameMessage);
            ticTacToeManager.removeGame(gameId);
        }
    }

    private TicTacToeMessage gameToMessage(TicTacToe game) {
        TicTacToeMessage message = new TicTacToeMessage();
        message.setGameId(game.getGameId());
        message.setPlayer1(game.getPlayer1());
        message.setPlayer2(game.getPlayer2());
        message.setBoard(game.getBoard());
        message.setTurn(game.getTurn());
        message.setGameState(game.getGameState());
        message.setWinner(game.getWinner());
        return message;
    }
}
