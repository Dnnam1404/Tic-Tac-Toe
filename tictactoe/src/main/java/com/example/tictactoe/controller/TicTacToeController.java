package com.example.tictactoe.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;


/**
 * Lớp Controller để xử lý các yêu cầu HTTP và render trang trò chơi Tic-Tac-Toe.
 */
@Controller
@RequestMapping("/")
public class TicTacToeController {

    /**
     * Render trang trò chơi Tic-Tac-Toe với bảng trống.
     *
     * @return đối tượng model và view cho trang trò chơi Tic-Tac-Toe
     */
    @GetMapping
    public ModelAndView index() {
        return ticTacToe();
    }

    /**
     * Render trang trò chơi Tic-Tac-Toe với bảng trống.
     *
     * @return đối tượng model và view cho trang trò chơi Tic-Tac-Toe
     */
    @RequestMapping("/index")
    public ModelAndView ticTacToe() {
        ModelAndView modelAndView = new ModelAndView("index");
        String[][] board = new String[3][3];
        Arrays.stream(board).forEach(row -> Arrays.fill(row, " "));
        modelAndView.addObject("board", board);
        return modelAndView;
    }
}
