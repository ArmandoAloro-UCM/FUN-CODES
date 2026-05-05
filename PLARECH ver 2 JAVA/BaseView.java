// BaseView.java — thin base class for every screen panel
// Provides access to the AppController and sets the standard background color.

import javax.swing.*;
import java.awt.*;

public abstract class BaseView extends JPanel {

    protected final AppController ctrl;

    public BaseView(AppController ctrl) {
        super(new BorderLayout());
        this.ctrl = ctrl;
        setBackground(Theme.BG);
    }
}
