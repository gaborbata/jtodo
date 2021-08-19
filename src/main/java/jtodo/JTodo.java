/*
 * JTodo
 *
 * Copyright (c) 2020-2021 Gabor Bata
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jtodo;

import com.formdev.flatlaf.FlatDarkLaf;
import com.udojava.evalex.Expression;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.io.StringWriter;
import static java.lang.String.format;
import java.nio.charset.StandardCharsets;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

/**
 * Experimental Java front-end for https://github.com/gaborbata/todo
 */
public class JTodo extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(JTodo.class.getName());
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 500;
    private static final int FONT_SIZE = 14;
    private static final int BORDER_SIZE = 5;
    private static final int COMMAND_HISTORY_SIZE = 5;
    private static final String APP_NAME = "todo";
    private static final Map<String, String> COLOR_CODES = new HashMap<>();

    static {
        COLOR_CODES.put("#303234", "30"); // black
        COLOR_CODES.put("#e74c3c", "31"); // red
        COLOR_CODES.put("#27ae60", "32"); // green
        COLOR_CODES.put("#f39c12", "33"); // yellow
        COLOR_CODES.put("#3498db", "34"); // blue
        COLOR_CODES.put("#9b59b6", "35"); // magenta
        COLOR_CODES.put("#1aacac", "36"); // cyan
        COLOR_CODES.put("#bdc3c7", "37"); // white

        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %n");
    }

    private final JComboBox<String> commandField;
    private final JTabbedPane tabbedPane;
    private final JButton executeButton;

    private StringWriter stringWriter;
    private ScriptingContainer scriptingContainer;
    private Object receiver;

    public JTodo() {
        super(APP_NAME);

        Font font = loadFont();

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(font);
        addNewTab();
        tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), ":active");
        tabbedPane.setEnabled(false);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this::dispatchKeyEvent);

        commandField = new JComboBox<>(new String[]{"help", "list :done", "list :active", "list :all", "eval 1+2"});
        commandField.setFont(font);
        commandField.setEditable(true);
        commandField.setPrototypeDisplayValue(APP_NAME);
        commandField.getEditor().setItem("");
        commandField.setEnabled(false);

        executeButton = new JButton("Execute");
        executeButton.setFont(font);
        executeButton.addActionListener(this::handleEvent);
        executeButton.setEnabled(false);

        JLabel commandLabel = new JLabel("todo>");
        commandLabel.setFont(font);

        Box box = Box.createHorizontalBox();
        box.setBorder(BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));
        box.add(commandLabel);
        box.add(Box.createHorizontalStrut(BORDER_SIZE));
        box.add(commandField);
        box.add(Box.createHorizontalStrut(BORDER_SIZE));
        box.add(executeButton);

        setLayout(new BorderLayout());
        add(tabbedPane);
        add(box, BorderLayout.PAGE_END);

        SwingUtilities.getRootPane(executeButton).setDefaultButton(executeButton);

        setApplicationIcon();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        repaint();
        new ScriptInitializationWorker().execute();
    }

    private void handleEvent(ActionEvent event) {
        try {
            stringWriter.getBuffer().setLength(0);
            String commandFieldText = String.valueOf(commandField.getEditor().getItem()).trim();
            tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), commandFieldText.isEmpty() ? ":active" : commandFieldText);
            final List<String> command = commandFieldText.isEmpty() ? emptyList() : asList(commandFieldText.split("[\\s\\xa0]+"));
            String action = command.stream().findFirst().orElse("");
            if ("repl".equals(action)) {
                getOutputPane().ifPresent(pane -> pane.setText("repl is not supported in this frontend of todo-jsonl"));
            } else if ("samegame".equals(action)) {
                SameGame.main(null);
                tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), "empty");
                getOutputPane().ifPresent(pane -> pane.setText(""));
            } else if ("eval".equals(action)) {
                String expression = command.stream().skip(1).collect(Collectors.joining(" "));
                String result = new Expression(expression).setPrecision(16).eval().toPlainString();
                getOutputPane().ifPresent(pane -> pane.setText(convertToHtml(result, false)));
            } else if ("version".equals(action)) {
                Object gemSpec = scriptingContainer.runScriptlet(PathType.CLASSPATH, "todo/todo.gemspec");
                final String gemVersion = "\u001B[33mGem Specification\u001B[0m\n" + Stream.of("name", "version", "date", "authors", "homepage", "license")
                        .map(attribute -> format("\u001B[36m%14s:\u001B[0m %s", attribute, scriptingContainer.callMethod(gemSpec, attribute)))
                        .collect(Collectors.joining("\n"));
                final String envVersion = "\u001B[33mEnvironment\u001B[0m\n" + Stream.of("RUBY_VERSION", "JRUBY_VERSION")
                        .map(attribute -> format("\u001B[36m%14s:\u001B[0m %s", attribute.toLowerCase().replace("_", " "), scriptingContainer.get(attribute)))
                        .collect(Collectors.joining("\n"))
                        .concat("\n")
                        .concat(Stream.of("java.version", "java.vendor", "os.name")
                                .map(attribute -> format("\u001B[36m%14s:\u001B[0m %s", attribute.replace(".", " "), System.getProperties().getProperty(attribute)))
                                .collect(Collectors.joining("\n")));
                getOutputPane().ifPresent(pane -> pane.setText(convertToHtml(gemVersion + "\n\n" + envVersion, false)));
            } else {
                Object commandArray = scriptingContainer.callMethod(scriptingContainer.get("Array"), "new", command);
                scriptingContainer.callMethod(receiver, "execute", commandArray);
                getOutputPane().ifPresent(pane -> {
                    pane.setText(convertToHtml(stringWriter.toString(), true));
                    pane.setSelectionStart(0);
                    pane.setSelectionEnd(0);
                });
            }
            if (!commandFieldText.isEmpty()) {
                commandField.setSelectedItem(null);
                List<String> items = IntStream.range(0, commandField.getItemCount())
                        .mapToObj(commandField::getItemAt)
                        .filter(item -> !item.equals(commandFieldText))
                        .limit(COMMAND_HISTORY_SIZE - 1)
                        .collect(Collectors.toList());
                commandField.removeAllItems();
                commandField.addItem(commandFieldText);
                items.forEach(commandField::addItem);
                commandField.getEditor().setItem("");
            }
        } catch (Exception e) {
            getOutputPane().ifPresent(pane -> pane.setText("ERROR: " + convertToHtml(String.valueOf(e.getMessage()), false)));
        }
    }

    private boolean dispatchKeyEvent(KeyEvent e) {
        boolean ctrlDown = (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0;
        if (ctrlDown && e.getID() == KeyEvent.KEY_PRESSED && tabbedPane.isEnabled()) {
            if (e.getKeyCode() == KeyEvent.VK_T && tabbedPane.getTabCount() < 10) {
                addNewTab();
                return true;
            } else if (e.getKeyCode() == KeyEvent.VK_W && tabbedPane.getTabCount() > 1) {
                tabbedPane.removeTabAt(tabbedPane.getSelectedIndex());
                return true;
            } else if (e.getKeyCode() == KeyEvent.VK_TAB && tabbedPane.getTabCount() > 1) {
                tabbedPane.setSelectedIndex((tabbedPane.getSelectedIndex() + 1) % tabbedPane.getTabCount());
                return true;
            }
        }
        return false;
    }

    private void addNewTab() {
        JEditorPane outputPane = new JEditorPane();
        outputPane.setEditable(false);
        outputPane.setContentType("text/html");
        outputPane.setFont(tabbedPane.getFont());
        outputPane.setBackground(new Color(0x303234));
        outputPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane scrollPane = new JScrollPane(outputPane);
        tabbedPane.addTab("empty", scrollPane);
        tabbedPane.setSelectedComponent(scrollPane);
    }

    private void setApplicationIcon() {
        try {
            Image image = new ImageIcon(JTodo.class.getClassLoader().getResource("image/jtodo.png")).getImage();
            setIconImages(Stream.of(16, 20, 32, 40, 64, 80, 128, 160)
                    .map(size -> image.getScaledInstance(size, size, Image.SCALE_SMOOTH))
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load image icon: {0}", e.getMessage());
        }
    }

    private Font loadFont() {
        Font font;
        try {
            InputStream fontStream = JTodo.class.getClassLoader().getResourceAsStream("font/RobotoMono-Medium.ttf");
            font = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont((float) FONT_SIZE);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load font: {0}", e.getMessage());
            font = new Font(Font.MONOSPACED, Font.BOLD, FONT_SIZE);
        }
        return font;
    }

    private String convertToHtml(String text, boolean convertUnicode) {
        String html = convertUnicode ? new String(text.getBytes(), StandardCharsets.UTF_8) : text;
        html = html.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>")
                .replace("\u001B[0m", "</font>")
                .replace(" ", "&nbsp;");
        for (Map.Entry<String, String> entry : COLOR_CODES.entrySet()) {
            html = html.replace("\u001B[" + entry.getValue() + "m", "<font color='" + entry.getKey() + "'>");
        }
        return html;
    }

    private Optional<JEditorPane> getOutputPane() {
        if (tabbedPane != null) {
            return Optional.ofNullable((JScrollPane) tabbedPane.getSelectedComponent())
                    .flatMap(pane -> Optional.ofNullable((JEditorPane) pane.getViewport().getView()));
        }
        return Optional.empty();
    }

    public static void main(String[] args) {
        try {
            UIManager.put("Button.arc", 4);
            JFrame.setDefaultLookAndFeelDecorated(true);
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (UnsupportedLookAndFeelException e) {
            LOGGER.log(Level.WARNING, "Could not set look and feel: {0}", e.getMessage());
        }
        SwingUtilities.invokeLater(JTodo::new);
    }

    private final class ScriptInitializationWorker extends SwingWorker<Void, String> {
        @Override
        protected Void doInBackground() throws Exception {
            publish("Initializing...");
            stringWriter = new StringWriter();
            scriptingContainer = new ScriptingContainer();
            scriptingContainer.setOutput(stringWriter);
            scriptingContainer.setWriter(stringWriter);
            receiver = scriptingContainer.runScriptlet(PathType.CLASSPATH, "todo/bin/todo.rb");
            return null;
        }

        @Override
        protected void process(List<String> chunks) {
            getOutputPane().ifPresent(pane -> pane.setText(String.join("", chunks)));
        }

        @Override
        protected void done() {
            tabbedPane.setEnabled(true);
            getOutputPane().ifPresent(pane -> pane.setText(convertToHtml(stringWriter.toString(), true)));
            executeButton.setEnabled(true);
            commandField.setEnabled(true);
            commandField.requestFocus();
        }
    };
}
