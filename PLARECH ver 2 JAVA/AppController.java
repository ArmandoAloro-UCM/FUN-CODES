// AppController.java — entry point and screen controller
// Run:  javac *.java && java AppController   (from inside SJFProject/)
//
// Files:
//   AppController.java   entry point + controller
//   SJFScheduler.java    SRTF algorithm + data classes
//   Theme.java           colors, fonts, constants
//   Widgets.java         reusable Swing helpers
//   views/               all screen panel classes

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AppController extends JFrame {

    // Shared application state passed between screens
    public int           numProcesses = 3;
    public List<Process> processes    = new ArrayList<>();
    public SJFScheduler  scheduler    = null;

    private JPanel currentView = null;

    public AppController() {
        super("Preemptive SJF Scheduler — CPU Replica Project");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(Theme.BG);
        setLayout(new BorderLayout());
        setSize(Theme.WIN_W, Theme.WIN_H);
        setResizable(true);
        centerOnScreen();

        showIntro();
        setVisible(true);
    }

    // ── Screen navigation ─────────────────────────────────────────────────────

    public void showIntro()   { switchTo(new IntroView(this));   }
    public void showCount()   { switchTo(new CountView(this));   }
    public void showInput()   { switchTo(new InputView(this));   }
    public void showTable()   { switchTo(new TableView(this));   }
    public void showAnim()    { switchTo(new AnimView(this));    }
    public void showResults() { switchTo(new ResultsView(this)); }

    /** Removes the current screen and replaces it with the given panel. */
    private void switchTo(JPanel view) {
        if (currentView != null) {
            getContentPane().remove(currentView);
        }
        currentView = view;
        getContentPane().add(currentView, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /** Centers the window on the primary screen. */
    private void centerOnScreen() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screen.width  - Theme.WIN_W) / 2;
        int y = (screen.height - Theme.WIN_H) / 2;
        setLocation(x, y);
    }

    // ── Main entry point ──────────────────────────────────────────────────────

    public static void main(String[] args) {
        // Run the GUI on the Swing event dispatch thread
        SwingUtilities.invokeLater(AppController::new);
    }
}
