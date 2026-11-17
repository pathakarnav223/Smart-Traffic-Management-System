package traffic.ui;

import traffic.db.StorageManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class MainFrame extends JFrame {
    private final AppPanel app;
    private final StorageManager storage;

    public MainFrame(StorageManager storage) {
        super("Smart Traffic Management System");
        this.storage = storage;
        app = new AppPanel(storage);
        setContentPane(app);
        setJMenuBar(buildMenu());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });
        setSize(1380, 860);
        setMinimumSize(new Dimension(1100, 720));
        setLocationRelativeTo(null);
    }

    private JMenuBar buildMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem shot = new JMenuItem("Save dashboard screenshot...");
        shot.addActionListener(e -> saveScreenshot());
        JMenuItem db = new JMenuItem("Database settings...");
        db.addActionListener(e -> new DbSettingsDialog(this, storage).setVisible(true));
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> quit());
        file.add(shot);
        file.add(db);
        file.addSeparator();
        file.add(exit);

        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About this project");
        about.addActionListener(e -> app.tabs().setSelectedIndex(3));
        help.add(about);
        bar.add(file);
        bar.add(help);
        return bar;
    }

    private void saveScreenshot() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("traffic_dashboard.png"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            Component c = app.dashboard();
            BufferedImage img = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            c.paint(g);
            g.dispose();
            ImageIO.write(img, "png", fc.getSelectedFile());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save screenshot: " + ex.getMessage());
        }
    }

    private void quit() {
        app.shutdown();
        storage.flushAndClose();
        dispose();
        System.exit(0);
    }
}
