package traffic.ui;

import traffic.db.AppConfig;
import traffic.db.MySqlDataStore;
import traffic.db.StorageManager;

import javax.swing.*;
import java.awt.*;

/** Lets the user choose between MySQL and CSV storage and test the connection. */
public class DbSettingsDialog extends JDialog {
    private final JComboBox<AppConfig.StorageMode> mode = new JComboBox<>(AppConfig.StorageMode.values());
    private final JTextField url = new JTextField(38);
    private final JTextField user = new JTextField(14);
    private final JPasswordField password = new JPasswordField(14);
    private final JLabel result = new JLabel(" ");

    public DbSettingsDialog(Window owner, StorageManager storage) {
        super(owner, "Database settings", ModalityType.APPLICATION_MODAL);
        AppConfig cfg = storage.config();
        mode.setSelectedItem(cfg.mode);
        url.setText(cfg.url);
        user.setText(cfg.user);
        password.setText(cfg.password);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.PANEL);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        row(p, g, 0, "Storage mode", mode);
        row(p, g, 1, "JDBC URL", url);
        row(p, g, 2, "User", user);
        row(p, g, 3, "Password", password);

        JLabel hint = Theme.muted("<html>AUTO tries MySQL first and falls back to CSV files in data/. "
                + "Place mysql-connector-j.jar in the lib/ folder.</html>");
        g.gridx = 0;
        g.gridy = 4;
        g.gridwidth = 2;
        p.add(hint, g);
        g.gridy = 5;
        p.add(result, g);

        JButton test = new JButton("Test connection");
        JButton save = new JButton("Save & apply");
        JButton cancel = new JButton("Cancel");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(test);
        buttons.add(save);
        buttons.add(cancel);
        g.gridy = 6;
        p.add(buttons, g);

        test.addActionListener(e -> {
            result.setForeground(Theme.MUTED);
            result.setText("Connecting...");
            String u = url.getText().trim();
            String us = user.getText().trim();
            String pw = new String(password.getPassword());
            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() {
                    try (MySqlDataStore s = new MySqlDataStore(u, us, pw)) {
                        return "OK";
                    } catch (Exception ex) {
                        return ex.getMessage();
                    }
                }

                @Override
                protected void done() {
                    try {
                        String r = get();
                        boolean ok = "OK".equals(r);
                        result.setForeground(ok ? Theme.OK : Theme.BAD);
                        result.setText(ok ? "Connected - tables are ready." : "<html>Failed: " + r + "</html>");
                        pack();
                    } catch (Exception ignored) {
                        // ignore
                    }
                }
            }.execute();
        });
        save.addActionListener(e -> {
            AppConfig c = new AppConfig();
            c.mode = (AppConfig.StorageMode) mode.getSelectedItem();
            c.url = url.getText().trim();
            c.user = user.getText().trim();
            c.password = new String(password.getPassword());
            c.save();
            storage.apply(c);
            dispose();
        });
        cancel.addActionListener(e -> dispose());

        setContentPane(p);
        pack();
        setLocationRelativeTo(owner);
    }

    private static void row(JPanel p, GridBagConstraints g, int y, String label, JComponent c) {
        g.gridy = y;
        g.gridx = 0;
        g.gridwidth = 1;
        g.weightx = 0;
        JLabel l = new JLabel(label);
        l.setForeground(Theme.MUTED);
        p.add(l, g);
        g.gridx = 1;
        g.weightx = 1;
        p.add(c, g);
    }
}
