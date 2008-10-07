/*
 *
 * FreeSand is a pure java implementation of a cellular automata
 * simulation inspired by falling sand like games.
 *
 * Copyright (C) 2007 Robert B. Harris (freesand@trebor.org)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 */

package org.trebor.freesand;

import static java.awt.Color.DARK_GRAY;
import static java.awt.event.InputEvent.CTRL_MASK;
import static java.awt.event.InputEvent.META_MASK;
import static java.awt.event.InputEvent.SHIFT_MASK;
import static java.awt.event.KeyEvent.*;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.KeyStroke.getKeyStroke;
import static org.trebor.freesand.World.Element.*;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.ImageProducer;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;

import org.trebor.freesand.World.Element;

/**
 * Game provides the freesand graphical user interface functionality.
 * World simulation occurs in {@link World}, where element behavior is
 * handled.
 */

@SuppressWarnings("serial")
public class Game extends JFrame
{
    // constants

    /** default world width */

    public static final int    WORLD_WIDTH    = 300;

    /** default world height */

    public static final int    WORLD_HEIGHT   = 300;

    /** frequency to paint graphics */

    public static final long   WORLD_PAINT_MS = 50;

    /** period of time to test each element */

    public static final long   TEST_PERIOD    = 1000;

    /** delay between when user stops resizing screen and when
     * resize computations occur */

    public static final int    RESIZE_DELAY   = 300;

    /** minimum brush size */

    public static final double MIN_BRUSH_SIZE = 0.01;

    /** maximum brush size */

    public static final double MAX_BRUSH_SIZE = 3 + MIN_BRUSH_SIZE;

    /** brush size change step */

    public static final double BRUSH_STEP     = 0.10;

    /** free sand image file extention */

    public static final String FILE_EXTENSION = "png";

    /** default period of time to show screen messages */

    public static final long   MESSAGE_DISPLAY_TIME = 500;

    /** color to paint on background to display element test results */

    public static final Color  TEST_BACKGROUND_COLOR = new Color(128, 128, 128);

    // globals


    /** simulation world image */

    protected World              world;

    /** buffer for manual double buffering */

    protected BufferedImage      frameBuffer;

    /** should the paint brush be antialiased */

    protected boolean            antiAliasBrush = false;

    /** statistics panel */

    protected JPanel             statsPanel;

    /** world simulation panel */

    protected JPanel             worldPanel;

    /** the menu bar */

    protected JMenuBar           menuBar;

    /** file chooser object */

    protected JFileChooser       fileChooser;

    /** toggle display of statistics panel */

    protected JCheckBoxMenuItem  statsToggleCbmi;

    /** toggle full screen mode */

    protected JCheckBoxMenuItem  fullScreenCbmi;

    /** graphics for frameBuffer */

    protected Graphics2D         bufferGr;

    /** graphics for world simulation */

    protected Graphics2D         worldGr;

    /** a handy dandy random number generater */

    protected Random             rnd = new Random();

    /** stack to store paused state on */

    protected Stack<Boolean>     pausedStack = new Stack<Boolean>();

    /** the cut and paste clipboard */

    protected Clipboard          clipboard;

    /** computed frame rate */

    protected double             frameRate;

    /** low pass filter rate for frame rate */

    protected double             rateFilter  = 0.95;

    /** percent of the time spent updateing the world simulation
     * @see #paintPercent
     */

    protected double             updatePercent;

    /** percent of the time spent painting the world image
     * @see #updatePercent
     */

    protected double             paintPercent;

    /** current brush color */

    protected Color              brushColor  = WATER_EL.getColor();

    /** current brush element */

    protected Element            brushElement = WATER_EL;

    /** current brush shape */

    protected Shape              brushShape  = circle;

    /** current brush name */

    protected String             brushName = "Circle";

    /** default scale to increase brush by */

    protected float              paintScale  = 60;

    /** current brush size */

    protected double             brushSize = 1 + MIN_BRUSH_SIZE;

    /** angle to rotate brush shape by */

    protected double             brushAngle = 0;

    /** world width */

    protected int                width;

    /** world height */

    protected int                height;

    /** this frame */

    protected JFrame             frame = null;

    /** background color of frame */

    protected Color              backGround = new Color(64, 64, 64);

    /** force window to be repainted at next oportunity */

    protected boolean            forcePaint = false;

    /** request that the animation thread pause @see paused */

    private   boolean            pauseRequest = false;

    /** paused state of animation thread @see #pauseRequest */

    private   boolean            paused = pauseRequest;

    /** request that simulation take 1 step */

    private   boolean            takeStep = false;

    /** display message @see #messageDisplayTime */

    private   String             message = "";

    /** time remaining to display message @see #message */

    private   long               messageDisplayTime = 0;

    /** triangle shape */

    public static Shape triangle = createRegularPoly(3);

    /** square shape */

    public static Shape square   = normalize(
      new Rectangle2D.Float(0, 0, 1, 1));

    /** rectangle shape */

    public static Shape rectangle = normalize(
      new Rectangle2D.Float(0f, 0f, 1f, .25f));

    /** diamond shape */

    public static Shape diamond = createRegularPoly(4);

    /** pyramid shape */

    public static Shape pyramid = createPyrmidShape();

    /** pentagon shape */

    public static Shape pentagon = createRegularPoly(5);

    /** hexagon shape */

    public static Shape hexagon  = createRegularPoly(6);

    /** cirlce shape */

    public static Shape circle   = normalize(
      new Ellipse2D.Float(0, 0, 1, 1));

    /** heart shape */

    public static Shape heart    = createHeartShape();

    /** star shape */

    public static Shape star     = createStar(5);

    /** cat shape */

    public static Shape cat      = createCatShape();

    /** dog shape */

    public static Shape dog      = createDogShape();

    /** fish shape */

    public static Shape fish     = createFishShape();

    // file fliter

    javax.swing.filechooser.FileFilter fileFilter =
      new javax.swing.filechooser.FileFilter()
      {
          // accept file?

          public boolean accept(File f)
          {
            // allow browse directories

            if (f.isDirectory())
              return true;

            // allow files with correct extention

            String extension = getExtension(f);
            return (extension != null && extension.equals(FILE_EXTENSION));
          }
          // get file extension

          public String getExtension(File f)
          {
            String ext = null;
            String s = f.getName();
            int i = s.lastIndexOf('.');

            if (i > 0 && i < s.length() - 1)
              ext = s.substring(i+1).toLowerCase();

            return ext;
          }
          // return descriton

          public String getDescription()
          {
            return "Image Files (.png)";
          }
      };

    // basic brush selection actions

    BrushSelectionAction[] basicBrushes =
    {
      new BrushSelectionAction("Square",    square,    getKeyStroke(VK_S, 0)),
      new BrushSelectionAction("Circle",    circle,    getKeyStroke(VK_C, 0)),
      new BrushSelectionAction("Pyramid",   pyramid,   getKeyStroke(VK_P, 0)),
      new BrushSelectionAction("Rectangle", rectangle, getKeyStroke(VK_R, 0)),
    };
    // more brush selection actions

    BrushSelectionAction[] moreBrushes =
    {
      new BrushSelectionAction("Triangle", triangle, null),
      new BrushSelectionAction("Diamond",  diamond,  null),
      new BrushSelectionAction("Pentagon", pentagon, null),
      new BrushSelectionAction("Hexagon",  hexagon,  null),
    };
    // fun brush selection actions

    BrushSelectionAction[] funBrushes =
    {
      new BrushSelectionAction("Heart",   heart, null),
      new BrushSelectionAction("Star",    star,  null),
      new BrushSelectionAction("Cat",     cat,   null),
      new BrushSelectionAction("Dog",     dog,   null),
      new BrushSelectionAction("Fish",    fish,  null),
    };
    // element selection actions

    ElementSelectionAction[] elementActions =
    {
      new ElementSelectionAction(AIR_EL,          getKeyStroke(VK_1, 0)),
      new ElementSelectionAction(WATER_EL,        getKeyStroke(VK_2, 0)),
      new ElementSelectionAction(FIRE1_EL,        getKeyStroke(VK_3, 0)),
      new ElementSelectionAction(EARTH_EL,        getKeyStroke(VK_4, 0)),
      new ElementSelectionAction(SAND_EL,         getKeyStroke(VK_5, 0)),
      new ElementSelectionAction(PLANT_EL,        getKeyStroke(VK_6, 0)),
      new ElementSelectionAction(OIL_EL,          getKeyStroke(VK_7, 0)),
      new ElementSelectionAction(ROCK_EL,         getKeyStroke(VK_8, 0)),
      new ElementSelectionAction(AIR_SOURCE_EL,   getKeyStroke(VK_1, SHIFT_MASK)),
      new ElementSelectionAction(WATER_SOURCE_EL, getKeyStroke(VK_2, SHIFT_MASK)),
      new ElementSelectionAction(FIRE_SOURCE_EL,  getKeyStroke(VK_3, SHIFT_MASK)),
      new ElementSelectionAction(SAND_SOURCE_EL,  getKeyStroke(VK_5, SHIFT_MASK)),
      new ElementSelectionAction(OIL_SOURCE_EL,   getKeyStroke(VK_7, SHIFT_MASK)),
    };
    // load world from disk

    SandAction actionOpen = new SandAction(
      "Open",
      getKeyStroke(VK_O, META_MASK),
      "load from file")
      {
          public void actionPerformed(ActionEvent e)
          {
            readWorld();
          }
      };

    // save world to disk

    SandAction actionSave = new SandAction(
      "Save",
      getKeyStroke(VK_S, META_MASK),
      "save to file")
      {
          public void actionPerformed(ActionEvent e)
          {
            writeWorld();
          }
      };

    // pause sim

    SandAction actionPause = new SandAction(
      "Pause",
      getKeyStroke(VK_SPACE, 0),
      "togglel pause of the simulation")
      {
          public void actionPerformed(ActionEvent e)
          {
            togglePause();
          }
      };

    // step sim

    SandAction actionStep = new SandAction(
      "Step",
      getKeyStroke(VK_SPACE, SHIFT_MASK),
      "cause simulation to take single step")
      {
          public void actionPerformed(ActionEvent e)
          {
            if (!isPaused())
              pause();
            takeStep = true;
          }
      };

    // test performance of various elements

    SandAction actionPerformanceTest = new SandAction(
      "Performance Tests",
      getKeyStroke(VK_T, CTRL_MASK),
      "test performance of elements")
      {
          public void actionPerformed(ActionEvent e)
          {
            new Thread()
            {
                public void run()
                {
                  String OverwriteOption = "Run Tests";
                  String CancelOption = "Cancel";
                  Object[] possibleValues =
                    {OverwriteOption, CancelOption};
                  int n = JOptionPane.showOptionDialog(
                    frame,
                    "Element performance tests will overwrite your current work.",
                    "Run Element Tests?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null,
                    possibleValues, OverwriteOption);

                  // if overwrite authorized, run tests

                  if (n == 0)
                    testElements();
                }
            }
              .start();
          }
      };

    // exit program

    SandAction actionExit = new SandAction(
      "Exit Program",
      getKeyStroke(VK_Q, META_MASK),
      "quit this program")
      {
          public void actionPerformed(ActionEvent e)
          {
            System.exit(0);
          }
      };

    // cut action

    SandAction actionCut = new SandAction(
      "Cut",
      getKeyStroke(VK_X, META_MASK),
      "cut screen contents")
      {
          public void actionPerformed(ActionEvent e)
          {
            cut();
          }
      };

    // copy action

    SandAction actionCopy = new SandAction(
      "Copy",
      getKeyStroke(VK_C, META_MASK),
      "copy screen contents")
      {
          public void actionPerformed(ActionEvent e)
          {
            copy();
          }
      };

    // paste action

    SandAction actionPaste = new SandAction(
      "Paste",
      getKeyStroke(VK_V, META_MASK),
      "copy screen contents")
      {
          public void actionPerformed(ActionEvent e)
          {
            paste();
          }
      };

    // fill screen with current element

    SandAction actionFill = new SandAction(
      "Fill Screen",
      getKeyStroke(VK_BACK_SPACE, SHIFT_MASK),
      "fill entire screen with currently selected element")
      {
          public void actionPerformed(ActionEvent e)
          {
            fillWorld(brushElement);
          }
      };

    // toggle statistics panel

    SandAction actionToggleStatsPanel = new SandAction(
      "Statistics",
      getKeyStroke(VK_A, META_MASK),
      "toggle visibilty of statistics menu")
      {
          public void actionPerformed(ActionEvent e)
          {
            toggleStatsPanel();
          }
      };

    // go to full screen mode

    SandAction actionFullScreen = new SandAction(
      "Full Screen",
      getKeyStroke(VK_F, META_MASK),
      "toggle full screen mode")
      {
          public void actionPerformed(ActionEvent e)
          {
            toggleFullScreen();
          }
      };
    // go to full screen mode

    SandAction actionEscapeFullScreen = new SandAction(
      "Escape Full Screen",
      getKeyStroke(VK_ESCAPE, 0),
      "return to windowed mode")
      {
          public void actionPerformed(ActionEvent e)
          {
            if (fullScreenCbmi.isSelected())
              fullScreenCbmi.doClick();
          }
      };

    // rotate brush left

    SandAction actionRotateLeft = new SandAction(
      "Rotate Left",
      getKeyStroke(VK_PERIOD, SHIFT_MASK),
      "rotate paint cursor left")
      {
          public void actionPerformed(ActionEvent e)
          {
            brushAngle = (brushAngle + 45) % 360;
            setPaintCursor();
          }
      };

    // rotate brush right

    SandAction actionRotateRight = new SandAction(
      "Rotate Right",
      getKeyStroke(VK_COMMA, SHIFT_MASK),
      "rotate paint cursor right")
      {
          public void actionPerformed(ActionEvent e)
          {
            brushAngle = (brushAngle - 45) % 360;
            setPaintCursor();
          }
      };

    // increase brush size

    SandAction actionGrowBrush = new SandAction(
      "Grow Brush",
      getKeyStroke(VK_PERIOD, 0),
      "increase brush size")
      {
          public void actionPerformed(ActionEvent e)
          {
            brushSize = min(brushSize + BRUSH_STEP, MAX_BRUSH_SIZE);
            setPaintCursor();
          }
      };

    // decrease brush size

    SandAction actionShrinkBrush = new SandAction(
      "Shrink Brush",
      getKeyStroke(VK_COMMA, 0),
      "decrease brush size")
      {
          public void actionPerformed(ActionEvent e)
          {
            brushSize = max(brushSize - BRUSH_STEP, MIN_BRUSH_SIZE);
            setPaintCursor();
          }
      };

    // show about box

    SandAction actionAbout = new SandAction(
      "About",
      null,
      "information about the FreeSand program")
      {
          public void actionPerformed(ActionEvent e)
          {
          }
      };

    // the animation thread

    Thread animation = new Thread()
      {
          public void run()
          {
            // stats values

            long start = 0;
            long update = 0;
            long end = 0;
            double total = 0;
            long statsSum = 0;
            long worldSum = 0;

            // main loop

            while (true)
            {
              // note it if paused requested

              paused = pauseRequest;

              // record start time

              start = System.currentTimeMillis();

              // if we're not paused update the world

              if (!paused || takeStep)
              {
                world.update();
                if (takeStep)
                {
                  takeStep = false;
                  forcePaint = true;
                }
              }
              // otherwise have litte nap to save the cpu

              else
              {
                try
                {
                  sleep(10);
                }
                catch (Exception e)
                {
                  e.printStackTrace();
                }
              }
              // record time to update

              update = System.currentTimeMillis();

              // update time since last paint

              worldSum += total;

              // draw frame every once in a while or if forced to

              if (forcePaint ||
              ((message != null || !paused)
              && worldSum >= WORLD_PAINT_MS))
              {
                // paint world to buffer

                world.paint(bufferGr);

                // if expected to, paint message to buffer

                if (messageDisplayTime > 0 && message != null)
                  paintMessage(bufferGr, message);
                else
                  message = null;

                // now actually draw buffer to frame

                worldPanel.repaint();

                // update time sum and mark that we did
                // force paint

                worldSum = 0;
                forcePaint = false;
              }
              // compute total time for update and draw

              end = System.currentTimeMillis();
              total = (float)(end - start);

              // if the message has not timed out,
              // update message time

              if (messageDisplayTime > 0)
                messageDisplayTime -= total;

              // if not paused update stats

              if (total > 0 && !paused)
              {
                // compute filtered frame rate

                frameRate = rateFilter * frameRate +
                  (1 - rateFilter) * 1000 / total;

                // compute filtered percents

                updatePercent = rateFilter * updatePercent +
                  (1 - rateFilter) * ((update - start) / total);
                paintPercent = rateFilter * paintPercent +
                  (1 - rateFilter) * ((end - update) / total);

                // if we've been going for a second, print stats

                if ((statsSum += total) >= 1000)
                {
                  statsSum = 0;
                  statsPanel.repaint();
                }
                // total = tmp;
              }
            }
          }
      };

    /**
     * Main entry point into program.  It creates and starts a
     * freesand game. It also sets the mac style menus.
     *
     * @param  args currently ignored
     */

    static public void main(String[] args)
    {
      // if we're on a mac, use mac style menus

      System.setProperty("apple.laf.useScreenMenuBar", "true");

      // create a new free sand game

      new Game(true);
    }
    /**
     * Construct a freesand game with option to build gui
     * components at construction time.
     *
     * @param build build and start game at construction time
     * @see #buildGame()
     */

    public Game(boolean build)
    {
      if (build)
        buildGame();
    }
    /**
     * Construct gui elements, display and start game.
     */

    protected void buildGame()
    {
      // create the frame

      constructFrame();

      // display the frame

      displayFrame();

      // create the world

      constructWorld();

      // start the animation thread

      animation.start();

      // set the cursor

      setPaintCursor();
    }
    /**
     * Put together elements of the gui frame.
     */

    protected void constructFrame()
    {
      // indentify the frame

      frame = this;

      // get system clipboard

      try
      {
        clipboard =  Toolkit.getDefaultToolkit().getSystemClipboard();
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
      // app exits on frame close

      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

      fileChooser = new JFileChooser();
      fileChooser.addChoosableFileFilter(fileFilter);

      // create the menu bar

      menuBar = new JMenuBar();

      // add the file menu

      JMenu menu = new JMenu("File");
      menu.add(actionOpen);
      menu.add(actionSave);
      menu.addSeparator();
      menu.add(actionPause);
      menu.add(actionStep);
      menu.add(actionPerformanceTest);
      menu.addSeparator();
      menu.add(actionExit);
      menuBar.add(menu);

      // add edit menu

      menu = new JMenu("Edit");
      menu.add(actionCut);
      menu.add(actionCopy);
      menu.add(actionPaste);
      menu.addSeparator();
      menu.add(actionFill);
      menuBar.add(menu);

      // add view menu

      menu = new JMenu("View");
      menu.add(statsToggleCbmi =
      new JCheckBoxMenuItem(actionToggleStatsPanel));
      menu.add(fullScreenCbmi =
      new JCheckBoxMenuItem(actionFullScreen));
      menu.add(actionEscapeFullScreen);
      menuBar.add(menu);

      // full screen only enabled if it's supported

      GraphicsDevice gv = GraphicsEnvironment.
        getLocalGraphicsEnvironment().getScreenDevices()[0];
      fullScreenCbmi.setEnabled(gv.isFullScreenSupported());

      // add brush menu

      menu = new JMenu("Brush");
      for (BrushSelectionAction bb: basicBrushes)
        menu.add(bb);
      menu.addSeparator();

      // add more brushes sub menu

      JMenu mBrushes = new JMenu("More Brushes");
      for (BrushSelectionAction ab: moreBrushes)
        mBrushes.add(ab);
      menu.add(mBrushes);

      // add fun brushes sub menu

      mBrushes = new JMenu("Fun Brushes");
      for (BrushSelectionAction fb: funBrushes)
        mBrushes.add(fb);
      menu.add(mBrushes);

      // add brush manipulation options

      menu.addSeparator();
      menu.add(actionRotateLeft);
      menu.add(actionRotateRight);
      menu.addSeparator();
      menu.add(actionGrowBrush);
      menu.add(actionShrinkBrush);
      menuBar.add(menu);
      setJMenuBar(menuBar);

      // add element menu

      menu = new JMenu("Elements");
      for (ElementSelectionAction ea: elementActions)
        menu.add(ea);
      menuBar.add(menu);

      // add toolbar

      JToolBar toolBar = new JToolBar("Elements");
      for (ElementSelectionAction ea: elementActions)
        toolBar.add(ea);
      //add(toolBar);

      // set background

      setBackground(backGround);

      // create graphics panel

      worldPanel = new JPanel()
        {
            public void paint(Graphics graphics)
            {
              Graphics2D g = (Graphics2D)graphics;
              g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
              g.drawImage(frameBuffer, 0, 0, null);
            }
        };
      worldPanel.setPreferredSize(
        new Dimension(WORLD_WIDTH, WORLD_HEIGHT));
      worldPanel.setLayout(null);
      add(worldPanel);

      // add component listener to handle frame resize

      addComponentListener(new ComponentAdapter()
        {
            // timing thread that waits until
            // the user has stopped dragging to resize
            // the window

            Thread resizeThread = null;

            // handle resize event

            public void componentResized(ComponentEvent e)
            {
              // assign the timing thread

              (resizeThread = new Thread()
                {
                    public void run()
                    {
                      try
                      {
                        // sleep for a bit

                        sleep(RESIZE_DELAY);

                        // if, when woken up, some other
                        // delay thread has not been
                        // created, then we'll it handle
                        // the resize event

                        if (resizeThread == this)
                          resize();
                      }
                      catch (Exception ex)
                      {
                        ex.printStackTrace();
                      }
                    }
                }
                ).start();
            }
        }
        );

      // add mouse listener

      MouseInputAdapter mia = new MouseInputAdapter()
        {
            // mouse clicked event

            public void mouseClicked(MouseEvent e)
            {
              paint(e);
            }
            // mouse clicked event

            public void mouseDragged(MouseEvent e)
            {
              paint(e);
            }
            // paint

            public void paint(MouseEvent e)
            {
              Element source = null;
              if (e.isShiftDown() && (source = Element.lookup(brushColor)
              .lookupSourceOrOutput()) != null)
                worldGr.setColor(source.getColor());
              else
                worldGr.setColor(brushColor);

              paintBrushShape(brushShape, worldGr, e.getX(), e.getY());
              forcePaint = true;
            }
        };
      worldPanel.addMouseListener(mia);
      worldPanel.addMouseMotionListener(mia);

      // create the stats panel

      statsPanel = new JPanel()
        {
            public void paint(Graphics graphics)
            {
              Graphics2D gr = (Graphics2D)graphics;
              gr.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
              gr.setColor(new Color(205, 205, 205));
              gr.fill(gr.getClipBounds());
              gr.setColor(Color.GRAY);
              gr.setFont(Font.decode("Courier"));
              gr.drawString("fps:   " +
              round(frameRate * 100) / 100.0, 5, 15);
              gr.drawString("sim:   " +
              round(updatePercent * 100) + "%", 5, 30);
              gr.drawString("paint: " +
              round(paintPercent  * 100) + "%", 5, 45);
              gr.drawString("width:  " + width,  100, 15);
              gr.drawString("height: " + height, 100, 30);
              gr.drawString("pixels: " + (width * height), 100, 45);
              gr.drawString("brush: " + brushName,    195, 15);
              gr.drawString("elmnt: " + brushElement, 195, 30);
            }
        };

      // add the statistcs panel

      statsPanel.setPreferredSize(new Dimension(300, 55));
      statsPanel.setMinimumSize(new Dimension(150, 55));
      statsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
    }
    /**
     * Toggle display of statistics panel.
     */

    public void toggleStatsPanel()
    {
      if (statsToggleCbmi.isSelected())
        add(statsPanel);
      else
        remove(statsPanel);

      worldPanel.setPreferredSize(
        new Dimension(worldPanel.getWidth(), worldPanel.getHeight()));

      invalidate();
      statsPanel.invalidate();
      pack();
    }

    /**
     * Toggle full screen mode.
     */

    public void toggleFullScreen()
    {
      // get the graphics device from the local graphic environment

      GraphicsDevice gv = GraphicsEnvironment.
        getLocalGraphicsEnvironment().getScreenDevices()[0];

      // if full screen selected

      if (fullScreenCbmi.isSelected())
      {
        // if full screen is supported setup frame accordingly

        if (gv.isFullScreenSupported())
        {
          setVisible(false);
          dispose();
          setUndecorated(true);
          pack();
          gv.setFullScreenWindow(this);
          setVisible(true);
        }
        else
        {
          showMessage("Not Supported");
          fullScreenCbmi.setSelected(false);
        }
      }
      // otherwise just pack and show the thing

      else
      {
        setVisible(false);
        dispose();
        setUndecorated(false);
        pack();
        gv.setFullScreenWindow(null);
        setVisible(true);
      }
    }
    /**
     * Pack and display frame. Also record size of world.
     */

    protected void displayFrame()
    {
      pack();
      setVisible(true);

      // anti alias graphics

      ((Graphics2D)worldPanel.getGraphics()).setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);

      // record the size of the world

      width = worldPanel.getWidth();
      height = worldPanel.getHeight();
    }
    /**
     * Construct simulaiton world.
     */

    protected void constructWorld()
    {
      // create the world

      world = (world == null)
        ? new World(width, height)
        : new World(width, height, world);

      // initialize the world

      world.initialize();

      // frame buffer

      frameBuffer = new BufferedImage(
        world.getWidth(),
        world.getHeight(),
        BufferedImage.TYPE_INT_ARGB);
      bufferGr = (Graphics2D)frameBuffer.getGraphics();

      // get graphics for world image

      worldGr = (Graphics2D)world.getGraphics();
    }
    /**
     * Resize the world to match the current world panel dimentions.
     */

    public void resize()
    {
      pushPaused(true);

      // get the world panel dimensions

      width = worldPanel.getWidth();
      height = worldPanel.getHeight();

      // construct the world

      constructWorld();
      popPaused();
      forcePaint = true;
    }
    /**
     * Copy world frame image to copy/paste buffer.
     */

    public void copy()
    {
      showMessage("Copying");
      World copy = new World(world);
      clipboard.setContents(copy, null);
      forcePaint = true;
    }
    /**
     * Cut world frame image to copy/paste buffer.
     */

    public void cut()
    {
      showMessage("Cutting");
      World copy = new World(world);
      clipboard.setContents(copy, null);
      fillWorld(AIR_EL);
      forcePaint = true;
    }
    /**
     * Paste image in copy/paste buffer to world frame.
     */

    public void paste()
    {
      pushPaused(true);
      showMessage("Pasting");
      try
      {
        Transferable content = clipboard.getContents(null);
        DataFlavor flavor = content.getTransferDataFlavors()[0];

        if (content.isDataFlavorSupported(DataFlavor.imageFlavor))
        {
          setWorldImage((BufferedImage)content
          .getTransferData(DataFlavor.imageFlavor));
        }
        else if (flavor.isMimeTypeEqual("image/x-pict"))
        {
          InputStream is = (InputStream)content.getTransferData(flavor);
          Image image = (Image)getImageFromPictStream(is);

          if (image != null)
            setWorldImage(image);
        }
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
      popPaused();
      forcePaint = true;
    }
    /**
     * Convert pixels in provided image to nearest {@link Element} color.
     *
     * @param  image image to convert
     * @return The modified image passed to this function. No new image is created.
     */

    public static BufferedImage convertToElements(BufferedImage image)
    {
      int[] pixels = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
      for (int i = 0; i < pixels.length; ++i)
        pixels[i] = nearest(new Color(pixels[i])).getValue();
      return image;
    }
    /**
     * Read pict type image from input stream.  This is used to
     * read images out of the copy/paste buffer.
     *
     * @param  is inputstream from wich the image will be read.
     * @return The image read from the input stream.
     */


    @SuppressWarnings("unchecked") 

    protected Image getImageFromPictStream(InputStream is)
    {
      try
      {
        // cast nulls to eliminate compiler warnings

        java.lang.Object[] nullObjects = null;
        java.lang.Class<?>[] nullClasses = null;

        // a place to put the data

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] header = new byte[512];
        byte[] buf = new byte[4096];
        int retval = 0;
        int size = 0;
        Method m;

        // write data into byte array

        baos.write(header, 0, 512);
        while ((retval = is.read(buf, 0, 4096)) > 0)
          baos.write(buf, 0, retval);
        baos.close();

        // if we got no data, return null

        size = baos.size();
        if (size<=0)
          return null;

        // now get the data into a nice big array

        byte[] imgBytes = baos.toByteArray();

        // create a quick time session

        Class c = Class.forName("quicktime.QTSession");
          m = c.getMethod("isInitialized", nullClasses);
        
        // verify session initialized

        Boolean b = (Boolean)m.invoke(nullObjects, nullObjects);
        if (b.booleanValue() == false)
        {
          m = (Method)c.getMethod("open", nullClasses)
            .invoke(null, nullObjects);
        }

        // get the handle

        c = Class.forName("quicktime.util.QTHandle");
        Constructor con = c.getConstructor(
          new Class[] {imgBytes.getClass()});
        Object handle = con.newInstance(
          new Object[] {imgBytes});

        String s = new String("PICT");
        c = Class.forName("quicktime.util.QTUtils");
        m = c.getMethod("toOSType", new Class[] {s.getClass()});


        Integer type = (Integer)m.invoke(nullObjects, new Object[] { s });
        c = Class.forName("quicktime.std.image.GraphicsImporter");
        con = c.getConstructor(new Class[] { Integer.TYPE });
        Object importer= con.newInstance(new Object[] { type });
        m = c.getMethod("setDataHandle", new Class[]
          {Class.forName("quicktime.util." + "QTHandleRef") });
        m.invoke(importer, new Object[] { handle });
        m = c.getMethod("getNaturalBounds", nullClasses);
        Object rect= m.invoke(importer, nullObjects);
        c = Class.forName("quicktime.app.view.GraphicsImporterDrawer");
        con = c.getConstructor(new Class[] { importer.getClass() });
        Object iDrawer = con.newInstance(new Object[] { importer });
        m = rect.getClass().getMethod("getWidth", nullClasses);
        Integer width= (Integer)m.invoke(rect, nullObjects);
        m = rect.getClass().getMethod("getHeight", nullClasses);
        Integer height= (Integer)m.invoke(rect, nullObjects);
        Dimension d= new Dimension(width.intValue(), height.intValue());
        c = Class.forName("quicktime.app.view.QTImageProducer");
        con = c.getConstructor(new Class[]
          {iDrawer.getClass(), d.getClass()});
        Object producer= con.newInstance(new Object[] {iDrawer, d});
        if (producer instanceof ImageProducer)
          return(Toolkit.getDefaultToolkit()
          .createImage((ImageProducer)producer));
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
      return null;
    }
    /**
     * Write current world image to disk.
     */

    public void writeWorld()
    {
      try
      {
        pushPaused(true);
        showMessage("Saving");
        if (fileChooser.showSaveDialog(this) == APPROVE_OPTION)
        {
          // select file

          File file = fileChooser.getSelectedFile();

          // if file name does not end if correct file extension
          // add file extension

          if (!file.toString().toLowerCase()
          .endsWith("." + FILE_EXTENSION))
          {
            file = new File(file + "." + FILE_EXTENSION);
          }
          // if file exists be sure we should overwrite it

          if (file.exists())
          {
            String OverwriteOption = "Overwrite";
            String CancelOption = "Cancel";
            Object[] possibleValues = {OverwriteOption, CancelOption};
            int n = JOptionPane.showOptionDialog(
              this,
              file.getName() +
              " already exists in this directory.  Should it be overwritten?",
              "Overwrite?",
              JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE, null,
              possibleValues, OverwriteOption);

            // if overwrite authorized

            if (n == 0)
              ImageIO.write(world, "png", file);
          }
          else
            ImageIO.write(world, "png", file);
        }
        popPaused();
        forcePaint = true;
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
    /**
     * Read an image from the disk.
     */

    public void readWorld()
    {
      try
      {
        pushPaused(true);
        showMessage("Loading");
        if (fileChooser.showOpenDialog(this) == APPROVE_OPTION)
          setWorldImage(ImageIO.read(fileChooser.getSelectedFile()));

        popPaused();
        forcePaint = true;
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
    /**
     * Set the world image to the provided image.
     *
     * @param  rawImage image to set as the new world image
     */

    protected void setWorldImage(Image rawImage)
    {
      pushPaused(true);

      // convert image to buffered image with corrected pixel colors

      BufferedImage image = new BufferedImage(
        rawImage.getWidth(null),
        rawImage.getHeight(null),
        BufferedImage.TYPE_INT_ARGB);
      image.getGraphics().drawImage(rawImage, 0, 0, null);
      image = convertToElements(image);

      // create world from image

      World newWorld = new World(image);

      // if new world differntly sized than current, maybe
      // the window should be resized

      if (newWorld.width != worldPanel.getWidth() || newWorld.height != worldPanel.getHeight())
      {
        String ResizeOption = "Resize";
        String CancelOption = "Cancel";
        Object[] possibleValues = {ResizeOption, CancelOption};
        int n = JOptionPane.showOptionDialog(
          this, "Resize to " + newWorld.width + "x" + newWorld.height +
          " to fit new image?", "Resize?",
          JOptionPane.YES_NO_OPTION,
          JOptionPane.QUESTION_MESSAGE, null,
          possibleValues, ResizeOption);

        // if resize requested

        if (n == 0)
        {
          worldPanel.setPreferredSize(
            new Dimension(newWorld.width, newWorld.height));
          invalidate();
          worldPanel.invalidate();
          pack();

          // get the world panel dimensions

          // construct the world

          width = newWorld.width;
          height = newWorld.height;
        }
      }

      world = newWorld;
      worldGr = (Graphics2D)world.getGraphics();
      constructWorld();

      popPaused();
    }
    /**
     * Execute performance tests on all {@link Element}s.  Destroys
     * current world image.
     */

    public void testElements()
    {
      try
      {
        showMessage("Testing");

        // set rate filter to zero for accurate readings

        double tmpRateFilter = rateFilter;
        rateFilter = 0;

        // a class to store results so, so they sort

        class Result implements Comparable<Object>
        {
          Element element;
          double fps;

          public Result(Element element, double fps)
          {
            this.element = element;
            this.fps = fps;
          }
          public int compareTo(Object o)
          {
            Result other = (Result)o;
            if (other.fps < fps)
              return -1;
            return other.fps > fps ? 1 : 0;
          }
        };
        // a place to put the results

        Vector<Result> results = new Vector<Result>();

        // ensure that the system is running

        unpause();

        // test elements in element action window

        for (int i = 0; i < elementActions.length; ++i)
        {
          // paint the world with the subject element

          fillWorld(elementActions[i].element);

          // record data for the test period


          double maxFps = 0;
          long start = System.currentTimeMillis();
          while (System.currentTimeMillis() - start
          < TEST_PERIOD)
          {
            Thread.sleep(7);
            if (frameRate > maxFps)
              maxFps = frameRate;
          }
          // now add results to results data structure

          results.add(new Result(elementActions[i].element, maxFps));
        }
        // pause the system

        pause();
        Thread.sleep(2 * WORLD_PAINT_MS);

        // set a reasonable background color

        fillWorld(TEST_BACKGROUND_COLOR);

        // sort the results

        //Vector<Result> checkedResult = (Vector<Result>)Collections.checkedCollection(results, Result.class);
        Collections.sort(results);

        // establish the fastest element

        double fastest = 0;
        for (Result result: results)
          if (result.fps > fastest)
            fastest = result.fps;

        // compute display scale based on fastest material

        double scale = (width - 40) / fastest;

        // set results font

        worldGr.setFont(worldGr.getFont().deriveFont(10f));

        // display the results

        int i = 0;
        showMessage("Results");
        for (Result result: results)
        {
          Element el = result.element;
          double fps = result.fps;
          worldGr.setColor(el.getColor());
          worldGr.fillRect(20, 20 + 20 * i, (int)(scale * fps), 10);
          worldGr.setColor(computeMatchingColor(el.getColor()));
          worldGr.drawString(el + ": " + round(100 * fps) / 100d + " fps",
          25, 29 + 20 * i++);
          forcePaint = true;
        }
        // restore rate filter

        rateFilter = tmpRateFilter;

        // force results to paint

        forcePaint = true;
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }
    /**
     * Push current paused state onto a stack and request new
     * paused state.  This function does not return until pause
     * state is synchronizePause.
     *
     * @param  pauseRequest new requested pause state
     * @see    #popPaused()
     * @see    #synchronizePause()
     */

    public void pushPaused(boolean pauseRequest)
    {
      pausedStack.push(this.pauseRequest);
      this.pauseRequest = pauseRequest;
      synchronizePause();
    }
    /**
     * Pop paused state from stack.  This function does not return
     * until pause state is synchronized.
     *
     * @return Returns the resulting paused state.
     * @see    #pushPaused(boolean pauseRequest)
     * @see    #synchronizePause()
     */

    public boolean popPaused()
    {
      this.pauseRequest = pausedStack.pop();
      synchronizePause();
      return isPaused();
    }
    /**
     * Pause simulation.  This function does not return until pause
     * state is synchronized.
     *
     * @see    #unpause()
     * @see    #synchronizePause()
     */

    public void pause()
    {
      pauseRequest = true;
      synchronizePause();
      showMessage("Paused");
    }
    /**
     * Resume paused simulation.  This function does not return
     * until pause state is synchronized.
     *
     * @see    #pause()
     * @see    #synchronizePause()
     */

    public void unpause()
    {
      pauseRequest = false;
      synchronizePause();
    }
    /**
     * Test paused state of simulation.  This function does not
     * return until pause state is synchronized.
     *
     * @return The paused state of the simulation.
     * @see    #pause()
     * @see    #unpause()
     * @see    #synchronizePause()
     */

    public boolean isPaused()
    {
      synchronizePause();
      return paused;
    }
    /**
     * Toggle paused state of simulation.  This function does not
     * return until pause state is synchronized.
     *
     * @return The paused state of the simulation.
     * @see    #synchronizePause()
     */

    public boolean togglePause()
    {
      pauseRequest = !pauseRequest;
      synchronizePause();
      if (paused)
        showMessage("Paused");
      return paused;
    }
    /**
     * Wait until the animation thread has recognized the requested
     * paused state.  This is done so that the the world is not
     * treated as though it is paused even if it's in the middle
     * updating the world.
     *
     * @see    #pause()
     * @see    #unpause()
     * @see    #isPaused()
     * @see    #pushPaused(boolean pauseRequest)
     * @see    #popPaused()
     */

    protected void synchronizePause()
    {
      try
      {
        while (paused != pauseRequest)
          Thread.sleep(10);
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
    /**
     * Fill entire world image with a provided element.  This
     * function must pause or the animation thread may cause one
     * pixel to be missed.
     *
     * @param  element element to fill image with
     */

    protected void fillWorld(Element element)
    {
      fillWorld(element.getColor());
    }
    /**
     * Fill entire world image with a provided color.  This
     * function must pause or the animation thread may cause one
     * pixel to be missed.
     *
     * @param  color color to fill image with
     */

    protected void fillWorld(Color color)
    {
      // must pause or there might be a left over pixel

      pushPaused(true);
      world.fill(color);
      popPaused();
    }
    /**
     * Set current paint cursor from currently selected brush.
     */

    protected void setPaintCursor()
    {
      // create cursor image

      BufferedImage cursorImage =
        createShapeImage(transformBrush(brushShape, 0, 0),
        computeMatchingColor(brushColor),
        antiAliasBrush);

      // create the custom cursor

      Toolkit tk = Toolkit.getDefaultToolkit();
      Cursor cursor = tk.createCustomCursor(
        cursorImage,
        new Point(cursorImage.getWidth() / 2, cursorImage.getHeight() / 2),
        "");

      // set the cursor for the world panel

      worldPanel.setCursor(cursor);
    }
    // construct an image of a given shape

    /**
     * Create a BufferedImage from a given Shape.  The returned
     * image has the shape painted into the image filled with the
     * provided color, and optionally is antialiased.
     *
     * @param  shape shape to create image with
     * @param  color color to fill shape when it's drawn into image
     * @param  antialias whether or not to antialias the painted shape
     * @return A BufferedImage the size of the provided shape.
     */

    public static BufferedImage createShapeImage(Shape shape, Color color,
    boolean antialias)
    {
      // get the bounds and create the image

      Rectangle2D bounds = shape.getBounds();
      int size = max((int)(bounds.getWidth()), (int)(bounds.getHeight()));

      BufferedImage image = new BufferedImage(
        size, size,
        BufferedImage.TYPE_4BYTE_ABGR);

      // paint the shape onto the image

      Graphics2D g = (Graphics2D)image.getGraphics();
      if (antialias)
        g.setRenderingHint(
          RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON);
      g.translate((size - bounds.getWidth ()) / 2 - bounds.getX(),
      (size - bounds.getHeight()) / 2 - bounds.getY());
      g.setColor(color);
      g.fill(shape);

      // return the image

      return image;
    }
    // set cursor of a container and all its descendents

    /**
     * Set the cursor for a given container and all its descendents.
     * @deprecated This function should no loger be needed as
     * cursors are being handled in a better way.
     *
     * @param  container container to change cursor of
     * @param  cursor new cursor for the container
     */

    public static void setCursor(Container container, Cursor cursor)
    {
      for (Component c: container.getComponents())
      {
        c.setCursor(cursor);
        if (c instanceof Container)
          setCursor((Container)c, cursor);
      }
    }

    /**
     * SandAction is derived from AbstractAction and provides a
     * standard class from which to subclass Game actions.
     */

    public abstract class SandAction extends AbstractAction
    {
        /**
         * Create a SandAction with a given name, shortcut key
         * and description.
         *
         * @param  name name of action
         * @param  key shortcut key to trigger action
         */

        public SandAction(String name, KeyStroke key,
        String description)
        {
          putValue(NAME, name);
          putValue(SHORT_DESCRIPTION, description);
          putValue(ACCELERATOR_KEY, key);
        }
        /**
         * Called when the given action is to be executed.  This
         * function must be implemented by the subclass.
         *
         * @param  e action event
         */

        abstract public void actionPerformed(ActionEvent e);

        /**
         * Update the enabled state of the given action.
         * Currently this functionality is not used.
         *
         * @return The enabled state of this action.
         */

        // update enabled state of action

        public boolean updateEnabledState()
        {
          return isEnabled();
        }
    }
    /**
     * BrushAction is derived from SandActionAction and is used
     * to select different brushes.
     */

    protected class BrushSelectionAction extends SandAction
    {
        Shape brush;

        // create a brush selection action

        public BrushSelectionAction(String name, Shape brush, KeyStroke key)
        {
          super(name, key, "Select " + name.toLowerCase() + " brush");
          this.brush = brush;

          // construct image of brush

          putValue(
            SMALL_ICON,
            new ImageIcon(createShapeImage(
              transformBrush(brush, 0, 0),
              DARK_GRAY, true)));
        }
        // execute action

        public void actionPerformed(ActionEvent e)
        {
          showMessage(getValue(NAME) + " Brush");
          brushShape = brush;
          brushName = getValue(NAME).toString();
          setPaintCursor();
          forcePaint = true;
        }
    }
    // element selection action

    class ElementSelectionAction extends SandAction
    {
        Element element;

        // create element selection action

        public ElementSelectionAction(Element element, KeyStroke key)
        {
          super(null, key, "Select " + element + " element");
          this.element = element;

          // construct image of element

          BufferedImage elementImage = new BufferedImage(
            70, 25,
            BufferedImage.TYPE_4BYTE_ABGR);
          Graphics g = elementImage.getGraphics();
          g.setClip(0, 0, elementImage.getWidth(), elementImage.getHeight());
          paint(g);
          putValue(SMALL_ICON, new ImageIcon(elementImage));
        }
        // paint element icon onto provided graphics

        public void paint(Graphics graphics)
        {
          Graphics2D g = (Graphics2D)graphics;
          g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
          g.setColor(new Color(0,0,0,0));
          Rectangle2D bounds = g.getClipBounds();
          g.fill(bounds);
          g.setColor(element.getColor());
          g.fillRoundRect(0, 0,
          (int)bounds.getWidth() - 1,
          (int)bounds.getHeight() - 1,
          20, 20);
          g.setFont(g.getFont().deriveFont(10f));
          g.setColor(computeMatchingColor(element.getColor()));
          FontMetrics fm = g.getFontMetrics();
          Rectangle2D sBounds = fm.getStringBounds(element.toString(), g);
          g.drawString(
            element.toString(),
            (int)((bounds.getWidth()  - sBounds.getWidth()     ) / 2),
            (int)((bounds.getHeight() + sBounds.getHeight() / 2) / 2));
        }
        // execute action

        public void actionPerformed(ActionEvent e)
        {
          showMessage(element.toString());
          brushColor = element.getColor();
          brushElement = element;
          setPaintCursor();
          forcePaint = true;
        }
    }
    /**
     * Show a message on the screen for the default amount of time.
     *
     * @param  message message to show on the screen
     * @see    #MESSAGE_DISPLAY_TIME
     */

    public void showMessage(String message)
    {
      showMessage(message, MESSAGE_DISPLAY_TIME);
    }
    /**
     * Show a message on the screen for the specifed number of miliseconds.
     *
     * @param  message message to show on the screen
     * @param  messageDisplayTime time in miliseconds to display message
     */

    public void showMessage(String message, long messageDisplayTime)
    {
      this.message = message;
      this.messageDisplayTime = messageDisplayTime;
      forcePaint = true;
    }
    /**
     * Paint provided message onto the provided graphics object as
     * big as possible.  The function does not know how to handle
     * carriage returns.
     *
     * @param  g graphic on which to draw message
     * @param  message message to draw
     */

    public void paintMessage(Graphics2D g, String message)
    {
      g.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);

      // figure out how big the string will be

      Rectangle2D sBounds = g.getFontMetrics().getStringBounds(message, g);

      // compute how much to increase font by to make it fit
      // nicely on the screen

      double increase = (worldPanel.getWidth() / sBounds.getWidth()) * 0.9d;

      // set font size

      Font font = g.getFont();
      g.setFont(font.deriveFont(font.getSize() * (float)increase));

      // draw message into frame

      g.setColor(new Color(255, 255, 255, 128));
      sBounds = g.getFontMetrics().getStringBounds(message, g);
      g.drawString(
        message,
        (int)((worldPanel.getWidth()  - sBounds.getWidth()     ) / 2),
        (int)((worldPanel.getHeight() + sBounds.getHeight() / 2) / 2));

      // restore font

      g.setFont(font);
    }
    /**
     * Given a color, compute a close color which will be visible
     * if overlayed on the original color.
     *
     * @param  color color to find match to
     * @return The new matched color.
     */

    Color computeMatchingColor(Color color)
    {
      float[] hsb = Color.RGBtoHSB(
        color.getRed(),
        color.getGreen(),
        color.getBlue(),
        new float[3]);

      return hsb[2] < 0.2
        ? Color.getHSBColor(hsb[0], hsb[1], hsb[2] < .1
        ? 0.5f
        : hsb[2] * 2f)
        : Color.getHSBColor(hsb[0], hsb[1], hsb[2] / 2);
    }
    /**
     * Create normalized pyramid shape.
     *
     * @return The normalized shape.
     * @see #normalize(Shape shape)
     */

    public static Shape createPyrmidShape()
    {
      Area gp = new Area();
      gp.add(new Area(createRegularPoly(4)));
      gp.subtract(new Area(translate(square, 0, 0.5)));
      return normalize(gp);
    }
    /**
     * Create normalized heart shape.
     *
     * @return The normalized shape.
     * @see #normalize(Shape shape)
     */

    public static Shape createHeartShape()
    {
      GeneralPath gp = new GeneralPath();
      gp.append(translate(circle, 0.5, 0), false);
      gp.append(translate(circle, 0, 0.5), false);
      gp.append(square, false);
      return normalize(rotate(gp, 225));
    }
    /**
     * Create normalized cat shape.
     *
     * @return The normalized shape.
     * @see #normalize(Shape shape)
     */

    public static Shape createCatShape()
    {
      Area cat = new Area(circle);
      Area wisker = new Area(new Rectangle2D.Double(0, -.01, .3, .02));

      // create left wiskers

      Area leftWiskers = new Area();
      leftWiskers.add(rotate(wisker, -20));
      leftWiskers.add(rotate(wisker,  20));
      leftWiskers.add(rotate(wisker,  20));

      // create right wiskers

      Area rightWiskers = new Area();
      rightWiskers.add(rotate(wisker, 180));
      rightWiskers.add(rotate(wisker, -20));
      rightWiskers.add(rotate(wisker, -20));

      // add the ears

      Area ear = new Area(translate(scale(triangle, .5, .5), 0.0, -0.6));
      translate(ear, .07, 0);
      cat.add(ear);
      rotate(cat, 60);
      translate(ear, -.14, 0);
      cat.add(ear);
      rotate(cat, -30);

      // add the eyes

      Area eye = new Area(scale(circle, 0.18, 0.18));
      eye.subtract(new Area(scale(circle, .06, .12)));
      translate(eye, -.15, -.1);
      cat.subtract(eye);
      translate(eye, .3, 0);
      cat.subtract(eye);

      // add the wiskers

      cat.subtract(translate(leftWiskers,   .08, .14));
      cat.subtract(translate(rightWiskers, -.08, .14));

      // add nose

      Area nose = new Area(createRegularPoly(3));
      rotate(nose, 180);
      scale(nose, .15, .15);
      translate(nose, 0, .1);
      cat.subtract(nose);

      // flatten the cat

      scale(cat, 1.0, 0.85);

      // return normalized shape

      return normalize(cat);
    }
    /**
     * Create normalized dog shape.
     *
     * @return The normalized shape.
     * @see #normalize(Shape shape)
     */

    public static Shape createDogShape()
    {
      Area dog = new Area(circle);

      // add the ears

      Area ear = new Area(scale(circle, .4, .7));
      rotate(ear, 20);
      translate(ear, -.5, -.2);
      dog.subtract(ear);
      scale(ear, -1, 1);
      dog.subtract(ear);
      scale(ear, -1, 1);
      translate(ear, -.05, 0);
      dog.add(ear);
      scale(ear, -1, 1);
      dog.add(ear);
      scale(ear, -1, 1);

      // add the eyes

      Area eye = new Area(scale(circle, 0.18, 0.18));
      eye.subtract(new Area(scale(circle, .12, .12)));
      translate(eye, -.15, -.1);
      dog.subtract(eye);
      translate(eye, .3, 0);
      dog.subtract(eye);

      // add snout

      Area snout = new Area(circle);
      scale(snout, .30, .30);
      translate(snout, 0, .2);
      dog.subtract(snout);

      // add nose

      Area nose = new Area(createRegularPoly(3));
      rotate(nose, 180);
      scale(nose, .20, .20);
      translate(nose, 0, .2);
      dog.add(nose);

      // stretch the dog

      scale(dog, 0.90, 1.0);

      // return normalized shape

      return normalize(dog);
    }
    /**
     * Create normalized fish shape.
     *
     * @return The normalized shape.
     * @see #normalize(Shape shape)
     */

    public static Shape createFishShape()
    {
      Area fish = new Area();
      Area body = new Area(new Arc2D.Double(0.0, 0, 1.0, 1.0, 30, 120, Arc2D.CHORD));
      Rectangle2D bounds = body.getBounds2D();
      translate(body,
      -(bounds.getX() + bounds.getWidth()  / 2),
      -bounds.getHeight());
      fish.add(body);
      scale(body, 1, -1);
      fish.add(body);

      // add the eye

      Area eye = new Area(scale(circle, .13, .13));
      eye.subtract(new Area(scale(circle, .08, .08)));
      translate(eye, -.15, -.08);
      fish.subtract(eye);

      // add tail

      Area tail = new Area(normalize(rotate(triangle, 30)));
      scale(tail, .50, .50);
      translate(tail, .4, 0);
      fish.add(tail);

      // return normalized shape

      return normalize(fish);
    }
    /**
     * Create normalized regular polygon shape.
     *
     * @return The normalized shape.
     * @see #normalize(Shape shape)
     */

    public static Shape createRegularPoly(int edges)
    {
      double radius = 1000;
      double theta = 0.75 * (2 * Math.PI);
      double dTheta = (2 * Math.PI) / edges;
      Polygon p = new Polygon();

      // add a point for each edge

      for (int edge = 0; edge < edges; ++edge)
      {
        p.addPoint(
          (int)(Math.cos(theta) * radius),
          (int)(Math.sin(theta) * radius));
        theta += dTheta;
      }
      // return the normalized poly

      return normalize(p);
    }
    /**
     * Create normalized star shape.
     *
     * @return The normalized shape.
     * @see #normalize(Shape shape)
     */

    public static Shape createStar(int points)
    {
      double radius = 1000;
      double theta = 0.75 * (2 * Math.PI);
      double dTheta = (4 * Math.PI) / points;
      Polygon p = new Polygon();

      // add a point for each edge

      for (int point = 0; point < points; ++point)
      {
        p.addPoint(
          (int)(Math.cos(theta) * radius),
          (int)(Math.sin(theta) * radius));
        theta += dTheta;
      }
      // convert to a general path to fill the shape

      GeneralPath gp = new GeneralPath(GeneralPath.WIND_NON_ZERO);
      gp.append(p, true);

      // return the normalized star

      return normalize(gp);
    }
    /**
     * Create a transformed version of the provided shape with a
     * new width & length <= 1 and centered at the origin (0, 0).
     *
     * @param  shape source shape to tranform, it is not changed
     * @return The newly normalized shape.
     */

    public static Shape normalize(Shape shape)
    {
      // center the shape on the origin

      Rectangle2D bounds = shape.getBounds2D();
      shape = translate(shape,
      -(bounds.getX() + bounds.getWidth() / 2),
      -(bounds.getY() + bounds.getHeight() / 2));

      // normalize size

      bounds = shape.getBounds2D();
      double scale = bounds.getWidth() > bounds.getHeight()
        ? 1.0 / bounds.getWidth()
        : 1.0 / bounds.getHeight();
      return scale(shape, scale, scale);
    }
    /**
     * Rotate a provided shape the number of degrees specified.
     *
     * @param  shape source shape to rotate, remains unchanged
     * @param  degrees to rotate shape
     * @return The new instance of the rotated shape.
     */

    public static Shape rotate(Shape shape, double degrees)
    {
      return AffineTransform.getRotateInstance(degrees / 180 * Math.PI)
        .createTransformedShape(shape);
    }
    /**
     * Rotate a provided shape around the point specifed, the
     * number of degrees specified.
     *
     * @param  shape source shape to rotate, remains unchanged
     * @param  degrees to rotate shape
     * @param  x x location of point of rotation
     * @param  y y location of point of rotation
     * @return The new instance of the rotated shape.
     */

    public static Shape rotate(Shape shape, double degrees,
    double x, double y)
    {
      return AffineTransform.
        getRotateInstance(degrees / 180 * Math.PI, x, y)
        .createTransformedShape(shape);
    }
    /**
     * Translate a provided shape by the amounts specifed.
     *
     * @param  shape source shape to translate, remains unchanged
     * @param  x x location to translate shape to
     * @param  y y location to translate shape to
     * @return The new instance of the translated shape.
     */

    public static Shape translate(Shape shape, double x, double y)
    {
      return AffineTransform.getTranslateInstance(x, y).createTransformedShape(shape);
    }
    /**
     * Scale a provided shape by the amounts specifed.
     *
     * @param  shape source shape to translate, remains unchanged
     * @param  x scale shape in x dimention by this amount
     * @param  y scale shape in y dimention by this amount
     * @return The new instance of the scaled shape.
     */

    public static Shape scale(Shape shape, double x, double y)
    {
      return AffineTransform.getScaleInstance(x, y).createTransformedShape(shape);
    }
    /**
     * Rotate a provided area the number of degrees specified.
     *
     * @param  area source area to rotate, remains unchanged
     * @param  degrees to rotate area
     * @return The new instance of the rotated area.
     */

    public static Area rotate(Area area, double degrees)
    {
      area.transform(AffineTransform.getRotateInstance(degrees / 180 * Math.PI));
      return area;
    }
    /**
     * Translate a provided area by the amounts specifed.
     *
     * @param  area source area to translate, remains unchanged
     * @param  x x location to translate area to
     * @param  y y location to translate area to
     * @return The new instance of the translated area.
     */

    public static Area translate(Area area, double x, double y)
    {
      area.transform(AffineTransform.getTranslateInstance(x, y));
      return area;
    }
    /**
     * Scale a provided area by the amounts specifed.
     *
     * @param  area source area to translate, remains unchanged
     * @param  x scale area in x dimention by this amount
     * @param  y scale area in y dimention by this amount
     * @return The new instance of the scaled area.
     */

    public static Area scale(Area area, double x, double y)
    {
      area.transform(AffineTransform.getScaleInstance(x, y));
      return area;
    }
    /**
     * Perform all standard transforms to brush before it is
     * painted to the screen.
     *
     * @param  brush original brush shape
     * @param  x x location translate brush to
     * @param  y y location translate brush to
     * @return The new instance of the tranfromed brush shape.
     */

    public Shape transformBrush(Shape brush, double x, double y)
    {
      brush = rotate(brush, brushAngle);
      brush = scale(brush,
      paintScale * brushSize,
      paintScale * brushSize);
      brush = translate(brush, x, y);
      return brush;
    }
    /**
     * Paint brush onto provided graphics.
     *
     * @param  brush brush shape
     * @param  x x location translate brush to
     * @param  y y location translate brush to
     * @return The new instance of the tranfromed brush shape.
     */

    public Shape paintBrushShape(Shape brush, Graphics2D g, double x, double y)
    {
      brush = transformBrush(brush, x, y);
      g.fill(brush);
      return brush;
    }
}
