import javax.swing.*;

public class Game {
    private static final String GAME_TITLE = "Zombie Shooter";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new GameFrame(GAME_TITLE);
        });
    }
}