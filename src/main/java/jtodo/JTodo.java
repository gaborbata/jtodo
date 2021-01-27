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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

/**
 * Experimental Java front-end for https://github.com/gaborbata/todo
 */
public class JTodo extends JFrame {

    private static final Logger LOG = Logger.getLogger(JTodo.class.getName());
    private static final int WINDOW_WIDTH = 720;
    private static final int WINDOW_HEIGHT = 480;
    private static final int FONT_SIZE = 14;
    private static final int BORDER_SIZE = 5;
    private static final int COMMAND_HISTORY_SIZE = 5;
    private static final double COLOR_CHANGE_FACTOR = 0.8;
    private static final String APP_NAME = "todo";

    private static final Map<String, String> COLOR_CODES = Map.of(
            "#2c3e50", "30", // balack
            "#e74c3c", "31", // red
            "#27ae60", "32", // green
            "#f39c12", "33", // yellow
            "#3498db", "34", // blue
            "#9b59b6", "35", // magenta
            "#1aacac", "36", // cyan
            "#bdc3c7", "37"  // white
    );

    static {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %n");
    }

    public JTodo() {
        super(APP_NAME);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        var stringWriter = new StringWriter();
        var scriptingContainer = new ScriptingContainer();
        scriptingContainer.setOutput(stringWriter);
        scriptingContainer.setWriter(stringWriter);
        var receiver = scriptingContainer.runScriptlet(PathType.CLASSPATH, "todo/bin/todo.rb");

        var font = loadFont();

        var outputPane = new JEditorPane();
        outputPane.setEditable(false);
        outputPane.setContentType("text/html");
        outputPane.setFont(font);
        outputPane.setText(convertToHtml(stringWriter.toString(), true));
        outputPane.setBackground(darkerColor(outputPane.getBackground()));

        var scrollPane = new JScrollPane(outputPane);

        var commandLabel = new JLabel("Command");
        commandLabel.setFont(font);

        var commandField = new JComboBox<String>(new String[]{"help", "list :done", "list :active", "list :all"});
        commandField.setFont(font);
        commandField.setEditable(true);
        commandField.setPrototypeDisplayValue(APP_NAME);
        commandField.getEditor().setItem("");

        var executeButton = new JButton("Execute");
        executeButton.setFont(font);

        executeButton.addActionListener(event -> {
            try {
                stringWriter.getBuffer().setLength(0);
                var commandFieldText = String.valueOf(commandField.getEditor().getItem()).trim();
                var command = commandFieldText.isEmpty() ? List.<String>of() : List.of(commandFieldText.split("\\s+"));
                if ("repl".equals(command.stream().findFirst().orElse(""))) {
                    outputPane.setText("repl is not supported in this frontend of " + APP_NAME);
                } else {
                    scriptingContainer.callMethod(receiver, "read", command);
                    var header = commandFieldText.isEmpty() ? "" : "todo> " + commandFieldText + "\n";
                    outputPane.setText(convertToHtml(header, false) + convertToHtml(stringWriter.toString(), true));
                }
                if (!commandFieldText.isEmpty()) {
                    commandField.removeItem(commandFieldText);
                    if (commandField.getModel().getSize() >= COMMAND_HISTORY_SIZE) {
                        commandField.removeItemAt(0);
                    }
                    commandField.addItem(commandFieldText);
                    commandField.getEditor().setItem("");
                }
            } catch (Exception e) {
                outputPane.setText(String.valueOf(e.getMessage()));
            }
        });

        var box = Box.createHorizontalBox();
        box.setBorder(BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));
        box.add(commandLabel);
        box.add(Box.createHorizontalStrut(BORDER_SIZE));
        box.add(commandField);
        box.add(Box.createHorizontalStrut(BORDER_SIZE));
        box.add(executeButton);

        add(scrollPane);
        add(box, BorderLayout.PAGE_END);

        SwingUtilities.getRootPane(executeButton).setDefaultButton(executeButton);

        setMinimumSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        commandField.requestFocus();
        repaint();
    }

    private Font loadFont() {
        Font font;
        try {
            var fontStream = JTodo.class.getClassLoader().getResourceAsStream("font/RobotoMono-Medium.ttf");
            font = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont((float) FONT_SIZE);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
        } catch (Exception e) {
            LOG.log(Level.WARNING, e.getMessage());
            font = new Font(Font.MONOSPACED, Font.BOLD, FONT_SIZE);
        }
        return font;
    }

    private String convertToHtml(String text, boolean convertUnicode) {
        var html = convertUnicode ? new String(text.getBytes(), StandardCharsets.UTF_8) : text;
        html = html.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>")
                .replace("\u001B[0m", "</font>")
                .replace(" ", "&nbsp;");
        for (var entry : COLOR_CODES.entrySet()) {
            html = html.replace("\u001B[" + entry.getValue() + "m", "<font color='" + entry.getKey() + "'>");
        }
        return html;
    }

    private Color darkerColor(Color color) {
        return new Color(Math.max((int) (color.getRed() * COLOR_CHANGE_FACTOR), 0),
                Math.max((int) (color.getGreen() * COLOR_CHANGE_FACTOR), 0),
                Math.max((int) (color.getBlue() * COLOR_CHANGE_FACTOR), 0),
                color.getAlpha());
    }

    public static void main(String[] args) {
        try {
            UIManager.put("Button.arc", 4);
            FlatDarkLaf.install();
            JFrame.setDefaultLookAndFeelDecorated(true);
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (UnsupportedLookAndFeelException e) {
            LOG.log(Level.SEVERE, e.getMessage());
        }
        SwingUtilities.invokeLater(JTodo::new);
    }
}
