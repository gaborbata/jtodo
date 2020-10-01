/*
 * JTodo
 *
 * Copyright (c) 2020 Gabor Bata
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
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
    private static final int WINDOW_WIDTH = 700;
    private static final int WINDOW_HEIGHT = 500;
    private static final int FONT_SIZE = 15;
    private static final int BORDER_SIZE = 5;
    private static final String PREFERRED_FONT = "Consolas";
    private static final String APP_NAME = "todo";

    private static final Map<String, String> COLOR_CODES = Map.of(
            "#2c3e50", "30", // balack
            "#e74c3c", "31", // red
            "#27ae60", "32", // green
            "#f39c12", "33", // yellow
            "#3498db", "34", // blue
            "#9b59b6", "35", // magenta
            "#1aacac", "36", // cyan
            "#ecf0f1", "37" // white
    );

    public JTodo() {
        super(APP_NAME);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        var stringWriter = new StringWriter();
        var scriptingContainer = new ScriptingContainer();
        scriptingContainer.setOutput(stringWriter);
        scriptingContainer.setWriter(stringWriter);
        var receiver = scriptingContainer.runScriptlet(PathType.CLASSPATH, "todo/bin/todo.rb");

        var fontName = Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
                .filter(font -> PREFERRED_FONT.equalsIgnoreCase(font))
                .findFirst()
                .orElse(Font.MONOSPACED);

        var editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");
        editorPane.setFont(new Font(fontName, Font.BOLD, FONT_SIZE));
        editorPane.setText(convertToHtml(stringWriter.toString()));

        var scrollPane = new JScrollPane(editorPane);

        var textField = new JTextField();

        var enterButton = new JButton("Search");
        enterButton.addActionListener(event -> {
            try {
                stringWriter.getBuffer().setLength(0);
                List<String> query = textField.getText().isEmpty() ? List.of() : List.of(textField.getText().split("\\s+"));
                if ("repl".equals(query.stream().findFirst().orElse(""))) {
                    editorPane.setText("repl is not supported in " + APP_NAME);
                } else {
                    scriptingContainer.callMethod(receiver, "read", query);
                    editorPane.setText(convertToHtml(stringWriter.toString()));
                }
                textField.setText("");
            } catch (Exception e) {
                editorPane.setText(String.valueOf(e.getMessage()));
            }
        });

        var box = Box.createHorizontalBox();
        box.setBorder(BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));
        box.add(Box.createHorizontalGlue());
        box.add(textField);
        box.add(Box.createHorizontalStrut(BORDER_SIZE));
        box.add(enterButton);

        add(scrollPane);
        add(box, BorderLayout.PAGE_END);

        SwingUtilities.getRootPane(enterButton).setDefaultButton(enterButton);

        setMinimumSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        textField.requestFocus();
        repaint();
    }

    private String convertToHtml(String text) {
        String html = text
            .replace("&", "&amp;")
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

    public static void main(String[] args) {
        try {
            UIManager.put("Button.arc", 0);
            FlatDarkLaf.install();
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (UnsupportedLookAndFeelException e) {
            LOG.log(Level.SEVERE, e.getMessage());
        }
        SwingUtilities.invokeLater(JTodo::new);
    }
}
