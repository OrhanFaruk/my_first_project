import javax.swing.JFrame;

public class GameFrame extends JFrame {

    GamePanel gamePanel;

    public GameFrame(String title) {
        gamePanel = new GamePanel();
        this.add(gamePanel);
        this.setTitle(title);
        this.setResizable(false);
        this.pack();
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        gamePanel.startGame();
    }
}
