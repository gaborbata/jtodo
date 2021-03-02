/*
 * SameGame
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import static java.lang.String.format;
import java.util.Random;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class SameGame extends JFrame implements MouseListener {

    private final int width = 20;
    private final int height = 10;
    private final int tile = 30;
    private final int status = 24;
    private final int border = 4;

    private final int[] colors = {0x914e3b, 0x7b8376, 0x3d6287, 0xaf8652, 0x303234};

    private final Image[] tiles = new Image[colors.length * 2 - 1];
    private final Color statusBorder = new Color(0x3c3f41);
    private final Color statusBackground = new Color(colors[4]);

    private final Color statusText = new Color(0xbdc3c7);
    private final Color canvasBackground = new Color(colors[4]);
    private final Font font = loadFont();

    private final int[] table;
    private final String[] texts;
    private final Random random;
    private final Image screenBuffer;
    private final JComponent canvas;
    private int score;
    private int marked;
    private boolean bonusAdded;

    private SameGame() {
        for (int i = 0; i < colors.length; i++) {
            tiles[i] = createTile(colors[i], true);
            tiles[colors.length * 2 - 2 - i] = createTile(colors[i], false);
        }
        table = new int[width * height];
        texts = new String[]{"New Game", null, null};
        random = new Random();
        screenBuffer = new BufferedImage(width * tile + border * 2, height * tile + border * 2 + status, BufferedImage.TYPE_INT_RGB);
        canvas = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                paintCanvas(g);
            }
        };

        reset();

        canvas.addMouseListener(this);
        canvas.setPreferredSize(new Dimension(width * tile + border * 2, height * tile + border * 2 + status));
        getContentPane().add(canvas);

        setTitle("SameGame");
        setIconImage(tiles[5]);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        repaint();
    }

    private void paintCanvas(Graphics g) {
        Graphics2D buffer = (Graphics2D) screenBuffer.getGraphics();
        buffer.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        buffer.setColor(canvasBackground);
        buffer.fillRect(0, 0, width * tile + border * 2, height * tile + border * 2);

        buffer.setColor(statusBorder);
        buffer.fillRect(0, height * tile + border * 2, width * tile + border * 2, status);

        buffer.setFont(font);
        texts[1] = marked > 1 ? format("Marked: %d (+%d)", marked, points(marked)) : (canvas.isEnabled() ? "" : "Game Over!");
        texts[2] = format("Score: %d", score);
        for (int i = 0; i < texts.length; i++) {
            buffer.setColor(statusBackground);
            buffer.fillRect((width * tile + border * 2) / 3 * i + 1, height * tile + border * 2 + 1, (width * tile + border * 2) / 3 - 2, status - 2);
            buffer.setColor(statusText);
            buffer.drawString(texts[i],
                    (width * tile + border * 2) / 6 * (2 * i + 1) - buffer.getFontMetrics().stringWidth(texts[i]) / 2,
                    height * tile + border * 2 + status / 2 + buffer.getFontMetrics().getHeight() / 4);
        }
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                buffer.drawImage(tiles[table[width * y + x] + 4], x * tile + border, y * tile + border, null);
            }
        }
        g.drawImage(screenBuffer, 0, 0, null);
    }

    private Image createTile(int c, boolean marked) {
        Color color = new Color(c);
        Image image = new BufferedImage(tile, tile, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) image.getGraphics();

        Color base = marked ? color.brighter() : color;
        Color brighter = base.brighter();
        Color darker = base.darker();

        g.setColor(darker);
        g.fillRect(0, 0, tile, tile);
        g.setColor(base);
        g.fillRect(0, 0, tile - 1, tile - 1);
        g.setColor(brighter);
        g.fillRect(1, 1, tile - 3, tile - 3);
        g.setPaint(new GradientPaint(2, 2, base, tile - 4, tile - 4, brighter));
        g.fillRect(2, 2, tile - 4, tile - 4);
        if (marked) {
            g.setColor(darker);
            g.fillRect(8, 8, tile - 16, tile - 16);
        }
        return image;
    }

    private void reset() {
        marked = 0;
        score = 0;
        bonusAdded = false;
        for (int i = 0; i < table.length; i++) {
            table[i] = random.nextInt(4) + 1;
        }
    }

    private int mark(int x, int y, int c) {
        if (isMarked(x, y) || c != getColor(x, y) || isRemoved(x, y)) {
            return 0;
        }
        int blocks = 1;
        table[width * y + x] = -1 * getColor(x, y);
        blocks += mark(x - 1, y, getColor(x, y));
        blocks += mark(x, y - 1, getColor(x, y));
        blocks += mark(x + 1, y, getColor(x, y));
        blocks += mark(x, y + 1, getColor(x, y));
        return blocks;
    }

    private void swap(int x1, int y1, int x2, int y2) {
        int pos1 = width * y1 + x1;
        int pos2 = width * y2 + x2;
        int temp = table[pos1];
        table[pos1] = table[pos2];
        table[pos2] = temp;
    }

    private boolean isMarked(int x, int y) {
        return getState(x, y) < 0;
    }

    private boolean isRemoved(int x, int y) {
        return getState(x, y) == 0;
    }

    private int getColor(int x, int y) {
        return Math.abs(getState(x, y));
    }

    private int getState(int x, int y) {
        if (x < 0 || y < 0 || x > width - 1 || y > height - 1) {
            return 0;
        }
        return table[width * y + x];
    }

    private int points(int removed) {
        return removed * removed - 4 * removed + 4;
    }

    @Override
    public void mousePressed(MouseEvent event) {
        if (event.getX() > 0
                && event.getX() < (width * tile + border * 2) / 3 - 1
                && event.getY() > height * tile + border * 2
                && event.getY() < height * tile + border * 2 + status - 1) {
            reset();
            canvas.setEnabled(true);
            canvas.repaint();
        }

        int x = event.getX() < border ? -1 : (event.getX() - border) / tile;
        int y = event.getY() < border ? -1 : (event.getY() - border) / tile;

        if (!isMarked(x, y)) {
            for (int i = 0; i < table.length; i++) {
                table[i] = Math.abs(table[i]);
            }
            marked = mark(x, y, getColor(x, y));
        } else {
            int tileCount = 0;
            for (int i = 0; i < table.length; i++) {
                if (table[i] < 0) {
                    tileCount++;
                }
            }
            if (tileCount < 2) {
                return;
            }
            for (int i = 0; i < table.length; i++) {
                if (table[i] < 0) {
                    table[i] = 0;
                }
            }
            for (int i = 0, k = 1; i < width; i++, k = 1) {
                while (k > 0) {
                    k = 0;
                    for (int j = 1; j < height; j++) {
                        if (isRemoved(i, j) && !isRemoved(i, j - 1)) {
                            swap(i, j, i, j - 1);
                            k = 1;
                        }
                    }
                }
            }
            int k = 1;
            while (k > 0) {
                k = 0;
                for (int i = 1; i < width; i++) {
                    if (isRemoved(i - 1, height - 1) && !isRemoved(i, height - 1)) {
                        for (int j = 0; j < height; j++) {
                            swap(i, j, i - 1, j);
                        }
                        k = 1;
                    }
                }
            }

            marked = 0;
            score += points(tileCount);

            k = 0;
            tileCount = 0;
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    if (!isRemoved(i, j)) {
                        tileCount++;
                        int c = getColor(i, j);
                        if (getColor(i - 1, j) == c || getColor(i, j - 1) == c || getColor(i + 1, j) == c || getColor(i, j + 1) == c) {
                            k = 1;
                            break;
                        }
                    }
                }
            }

            if (tileCount == 0 && !bonusAdded) {
                bonusAdded = true;
                score += 1000;
            }

            if (k < 1 && canvas.isEnabled()) {
                canvas.setEnabled(false);
            }
        }
        canvas.repaint();
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
    }

    @Override
    public void mouseEntered(MouseEvent event) {
    }

    @Override
    public void mouseExited(MouseEvent event) {
    }

    @Override
    public void mouseReleased(MouseEvent event) {
    }

    private Font loadFont() {
        Font f;
        try {
            InputStream fontStream = SameGame.class.getClassLoader().getResourceAsStream("font/RobotoMono-Medium.ttf");
            f = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(14.0f);
        } catch (Exception e) {
            f = new Font(Font.SANS_SERIF, Font.BOLD, 14);
        }
        return f;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SameGame::new);
    }
}
