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

import static java.awt.Color.BLACK;
import static java.awt.Color.HSBtoRGB;
import static java.lang.Math.abs;
import static java.lang.System.out;
import static org.trebor.freesand.World.ClrConst.*;
import static org.trebor.freesand.World.Element.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.Random;

   /**
    * World provides the freesand simulation functionality and element
    * behavior.  User interface functionality occurs in {@link
    * Game}.
    */

public class World extends BufferedImage implements Transferable
{
         // globals


         /** array providing direct access to image pixles */

      protected int[]   pixels;

         /** arrays of random numbers used to visit pixles in a given
          * row in a random order, pixels may be visited 0 or more times
          * on each update cycle */

      protected int[][] xRndIndex;

         /** a handy dandy random number generater */

      protected Random  rnd = new Random();

         /** width of the world */

      protected int     width;

         /** height of the world */

      protected int     height;

         /** background element */

      protected int     background = AIR;


         /** idicates that a given pixel is not going to change this
          * update cycle */

      public static final int   NO_CHANGE      = -1;

         /** number of randomized row indices we have to choose from */

      public static final int   RND_INDEX_CNT  = 200;

         // chance in X of something happening


         /** chance in X that fire will break out */

      public static final int FIRE_CHANCE_IN  = 3;

         /** chance in X that water will spout out of a water source */

      public static final int WATER_CHANCE_IN = 10;

         /** chance in X that plant will grow */

      public static final int PLANT_CHANCE_IN = 23;

         /** chance sand will spout out of a sand source */

      public static final int SAND_CHANCE_IN  = 10;
    
         // color constants, which must be in enum to allow 
         // initialization of Element enum (go figure)
         // this is a (somewhat failed) attempt to get nice
         // standardized way of represnting colors
      
      static enum ClrConst
      {
            // hues

         RED_HUE          (Color.RED),
            ORANGE_HUE    (Color.ORANGE),
            ORANGEISH_HUE (45f / 360f),
            YELLOW_HUE    (Color.YELLOW),
            GREEN_HUE     (Color.GREEN),
            CYAN_HUE      (Color.CYAN),
            BLUE_HUE      (Color.BLUE),
            MAGENTA_HUE   (Color.MAGENTA),
            
               // brightnesses

            BRIGHT1       (.90f),
            BRIGHT2       (.60f),
            BRIGHT3       (.45f),

               // saturations

            SAT1          (.90f),
            SAT2          (.60f),
            SAT3          (.40f),
            SAT4          (.20f),
            SAT5          (.00f);

         float value;

            // constructor which computes
            // hue based on color

         ClrConst(Color color)
         {
            this(Color.RGBtoHSB(
               color.getRed(), 
               color.getGreen(),
               color.getBlue(), null)[0]);
         }
            // constructor

         ClrConst(float value)
         {
            this.value = value;
         }
      }
         /** Enumeration of the elements in freesand. */

      public static enum Element
      {
            // basic elements

         AIR_EL    ("Air",    BLACK, null),
            WATER_EL  ("Water",  BLUE_HUE,      SAT1,  BRIGHT1, null),
            SAND_EL   ("Sand",   YELLOW_HUE,    SAT3,  BRIGHT1, null),
            EARTH_EL  ("Earth",  ORANGEISH_HUE, SAT2,  BRIGHT2, null),
            OIL_EL    ("Oil",    ORANGE_HUE,    SAT2,  BRIGHT3, null),
            PLANT_EL  ("Plant",  GREEN_HUE,     SAT1,  BRIGHT1, null),
            ROCK_EL   ("Rock",   RED_HUE,       SAT5,  BRIGHT3, null),

               // valuebles

            GOLD_EL   ("Gold",   ORANGEISH_HUE, SAT1,  BRIGHT1, null),
            SILVER_EL ("Silver", RED_HUE,       SAT5,  BRIGHT2, null),
            COPPER_EL ("Copper", RED_HUE,       SAT1,  BRIGHT2, null),

               // fire!

            FIRE1_EL  ("Fire",  new Color(204,   0,   0), null),
            FIRE2_EL  ("Fire2", new Color(255, 204,  51), null),
            FIRE3_EL  ("Fire3", new Color(255, 204,   0), null),
            FIRE4_EL  ("Fire4", new Color(255, 153,   0), null),
            FIRE5_EL  ("Fire5", new Color(255, 102,   0), null),
            FIRE6_EL  ("Fire6", new Color(255,  51,   0), null),

               // sources
            
            AIR_SOURCE_EL   ("Black Hole",   new Color(128,   0,  64), AIR_EL),
            WATER_SOURCE_EL ("Water Source", WATER_EL.darker(),        WATER_EL),
            SAND_SOURCE_EL  ("Sand Source",  SAND_EL.darker(),         SAND_EL),
            FIRE_SOURCE_EL  ("Fire Source",  FIRE1_EL.darker(),        FIRE1_EL),
            OIL_SOURCE_EL   ("Oil Source",   OIL_EL.darker(),          OIL_EL);
         
            // fields
         
         final String  elementName;
         final Color   color;
         final int     value;
         final Element sourceOf;

            /** 
             * Construct an element given a name and HSB values for color.
             *
             * @param elementName name of element
             * @param h hue value of element color
             * @param s saturation value of element color
             * @param b brightness value of element color
             * @param sourceOf element this is a source of if there is one
             */

         Element(final String elementName, ClrConst h, ClrConst s, ClrConst b, Element sourceOf)
         {
            this(elementName, new Color(HSBtoRGB(h.value, s.value, b.value)), sourceOf);
         }
            /** 
             * Construct an element given a name and HSB values for color.
             *
             * @param elementName name of element
             * @param h hue value of element color
             * @param s saturation value of element color
             * @param b brightness value of element color
             * @param sourceOf element this is a source of if there is one
             */

         @SuppressWarnings("unused")
         Element(final String elementName, float h, float s, float b, Element sourceOf)
         {
            this(elementName, new Color(HSBtoRGB(h, s, b)), sourceOf);
         }
            /** 
             * Construct an element given a name and RGB values for color.
             *
             * @param elementName name of element
             * @param r red value of element color
             * @param g green value of element color
             * @param b blue value of element color
             * @param sourceOf element this is a source of if there is one
             */

         @SuppressWarnings("unused")
         Element(final String elementName, int r, int g, int b, Element sourceOf)
         {
            this(elementName, new Color(r, g, b), sourceOf);
         }
            /** 
             * Construct an element given a name and a color.
             *
             * @param elementName name of element
             * @param color color of element
             * @param sourceOf element this is a source of if there is one
             */

         Element(final String elementName, Color color, Element sourceOf)
         {
            this.elementName = elementName;
            this.color = color;
            this.value = color.getRGB();
            this.sourceOf = sourceOf;
         }
            /** 
             * Return a darker version of this elements color.
             *
             * @return A darker verions of this elements color.
             */

         public Color darker()
         {
            return color.darker();
         }
            /** 
             * Return a brighter version of this elements color.
             *
             * @return A brighter verions of this elements color.
             */

         public Color brighter()
         {
            return color.brighter();
         }
            /** 
             * Return numeric value of this elements color.
             *
             * @return A numeric value of this elements color.
             */

         public int getValue()
         {
            return value;
         }
            /** 
             * Convert element to string value.  This function returns
             * the name of the element.
             *
             * @return A string represting this element.
             */

         public String toString()
         {
            return elementName;
         }
            /** 
             * Return the color of this element.
             *
             * @return The color of this element.
             */

         public Color getColor()
         {
            return color;
         }
            /** 
             * Find the element that exactly matches the provided color.
             *
             * @param color color to test against candidate elements
             * @return The matching element or null if not found.
             */
 
         static public Element lookup(Color color)
         {
            return lookup(color.getRGB());
         }
            /** 
             * Find the element that exactly matches the provided rgbvalue.
             *
             * @param rgbValue rgb value to test against candidate elements
             * @return The matching element or null if not found.
             */
 
         static public Element lookup(int rgbValue)
         {
            for (Element e: Element.values())
               if (e.getValue() == rgbValue)
                  return e;
            return null;
         }
            /** 
             * Find the element that is the nearsted match to the
             * provided color.
             *
             * @param c color to match
             * @return The nearest matching element.
             */
 
         static public Element nearest(Color c)
         {
            int r = c.getRed();
            int g = c.getGreen();
            int b = c.getBlue();
            int minDelta = Integer.MAX_VALUE;
            Element nearest = null;

            for (Element e: Element.values())
            {
               Color ec = e.getColor();
               int delta = 
                  abs(r - ec.getRed()) +
                  abs(g - ec.getGreen()) +
                  abs(b - ec.getBlue());

               if (delta < minDelta)
               {
                  minDelta = delta;
                  nearest = e;
               }
            }
            return nearest;
         }
            /** 
             * Find an elemnts source or output.
             *
             * @return The source our output of this element, or null if
             * there is none.
             */
 
         public Element lookupSourceOrOutput()
         {
            for (Element e: Element.values())
               if (e.sourceOf == this || this.sourceOf == e)
                  return e;

            return null;
         }
      }
         /** air element integer value */

      public static final int AIR    = AIR_EL  .getValue();

         /** water element integer value */

      public static final int WATER  = WATER_EL.getValue();

         /** sand element integer value */

      public static final int SAND   = SAND_EL .getValue();

         /** earth element integer value */

      public static final int EARTH  = EARTH_EL.getValue();

         /** oil element integer value */

      public static final int OIL    = OIL_EL  .getValue();

         /** plant element integer value */

      public static final int PLANT  = PLANT_EL.getValue();

         /** rock element integer value */

      public static final int ROCK   = ROCK_EL .getValue();

         /** gold element integer value */

      public static final int GOLD    = GOLD_EL.getValue();

         /** silver element integer value */

      public static final int SILVER  = SILVER_EL.getValue();

         /** copper element integer value */

      public static final int COPPER  = COPPER_EL.getValue();

         /** fire1 element integer value */

      public static final int FIRE1  = FIRE1_EL.getValue();

         /** fire2 element integer value */

      public static final int FIRE2  = FIRE2_EL.getValue();

         /** fire3 element integer value */

      public static final int FIRE3  = FIRE3_EL.getValue();

         /** fire4 element integer value */

      public static final int FIRE4  = FIRE4_EL.getValue();

         /** fire5 element integer value */

      public static final int FIRE5  = FIRE5_EL.getValue();

         /** fire6 element integer value */

      public static final int FIRE6  = FIRE6_EL.getValue();

         /** air source element integer value */

      public static final int AIR_SOURCE   = AIR_SOURCE_EL.getValue();

         /** water source element integer value */

      public static final int WATER_SOURCE = WATER_SOURCE_EL.getValue();

         /** sand source element integer value */

      public static final int SAND_SOURCE  = SAND_SOURCE_EL.getValue();

         /** fire source element integer value */

      public static final int FIRE_SOURCE  = FIRE_SOURCE_EL.getValue();

         /** oil source element integer value */

      public static final int OIL_SOURCE   = OIL_SOURCE_EL.getValue();

         /**
          * Construct a world, copying content from another world. Set
          * the background to air and the size to that of the other
          * world.
          *
          * @param other other world to copy content from
          */

      public World(Image other)
      {
         this(other.getWidth(null), other.getHeight(null), AIR_EL);
         getGraphics().drawImage(other, 0, 0, null);
      }
         /**
          * Construct a world, copying content from another world.  Set
          * the background to air.
          *
          * @param width width of world to create
          * @param height height of world to create
          * @param other other world to copy content from
          */

      public World(int width, int height, World other)
      {
         this(width, height, other, AIR_EL);
      }
         /**
          * Construct a world, copying content from another world.
          *
          * @param width width of world to create
          * @param height height of world to create
          * @param other other world to copy content from
          * @param bgElement element to fill world with
          */

      public World(int width, int height, World other, Element bgElement)
      {
            // construct self

         this(width, height, bgElement);

            // if the other width and height the same as this don't adjust

         if (other.width == width && other.height == height)
         {
            getGraphics().drawImage(other, 0, 0, null);
            return;
         }
            // locate content on other world

         int leftMost  = -1;
         int rightMost = -1;
         int topMost   = -1;
         int botMost   = -1;

            // find left most particle

         for (int x = 0; leftMost == -1 && x < other.width; ++x)
            for (int y = 0; leftMost == -1 && y < other.height; ++y)
               if (other.getRGB(x, y) != background)
                  leftMost = x;

            // if no content on other world, stop here

         if (leftMost == -1)
            return;

            // find right most particle

         for (int x = other.width - 1; rightMost == -1 && x >= 0; --x)
            for (int y = 0; rightMost == -1 && y < other.height; ++y)
               if (other.getRGB(x, y) != background)
                  rightMost = x;

            // find top most particle

         for (int y = 0; topMost == -1 && y < other.height; ++y)
            for (int x = 0; topMost == -1 && x < other.width; ++x)
               if (other.getRGB(x, y) != background)
                  topMost = y;

            // find top most particle

         for (int y = other.height - 1; botMost == -1 && y >= 0; --y)
            for (int x = 0; botMost == -1 && x < other.width; ++x)
               if (other.getRGB(x, y) != background)
                  botMost = y;

            // copy world centered into new world

         int otherWidth = rightMost - leftMost + 1;
         int otherHeight = botMost - topMost + 1;

        BufferedImage content = other.getSubimage(
           leftMost, topMost, otherWidth, otherHeight);
        getGraphics().drawImage(content, 
                                (width - otherWidth) / 2, 
                                (height - otherHeight),
                                null);
      }
         /**
          * Construct a world of a given size and set the background to air.
          *
          * @param width width of world to create
          * @param height height of world to create
          */

      public World(int width, int height)
      {
         this(width, height, AIR_EL);
      }
         /**
          * Construct a world of a given size and background element.
          *
          * @param width width of world to create
          * @param height height of world to create
          * @param bgElement element to fill world with
          */

      public World(int width, int height, Element bgElement)
      {                                        
            // construct parent

         super(width, height, TYPE_INT_ARGB);

            // get worl parameters

         this.background = bgElement.getValue();
         this.width = width;
         this.height = height;

            // fill background 

         fill(background);

            // get the pixel array for the world

         pixels = ((DataBufferInt)getRaster().getDataBuffer()).getData();
         
            // fill random index array with lots of random indicies

         xRndIndex = new int[RND_INDEX_CNT][getWidth()];
         for (int[] row: xRndIndex)
            for (int i = 0; i < row.length; ++i)
               row[i] = rnd.nextInt(row.length);
      }
         /** 
          * Initialze world to some value.  This is provided as a
          * virtuale hook to programs which may which to extend this
          * class.  This implementation does nothing.
          */

      public void initialize()
      {
      }
         /**
          * Fill entier world with provided element
          *
          * @param el element to paint world
          */

      public void fill(Element el)
      {
         fill(el.getColor());
      }
         /**
          * Fill entier world with provided color value
          *
          * @param color color value to paint world
          */

      public void fill(int color)
      {
         fill(new Color(color));
      }
         /**
          * Fill entier world with provided color.
          *
          * @param color color to paint world
          */

      public void fill(Color color)
      {
         Graphics2D g = (Graphics2D)getGraphics();
         g.setColor(color);
         g.fillRect(0, 0, width, height);
      }
         /**
          * Paint world onto provided graphics.
          *
          * @param g graphics to paint world onto
          */

      public void paint(Graphics2D g)
      {
         g.drawImage(this, 0, 0, null);
      }
         /** 
          * Uppdate the world state.  Note that the follow code is a
          * morass of if conditionals.  This is done so that the code
          * runs fast.  It would be easy to come up with an abstraction
          * layer which would make this code more general and robust,
          * but at the cost of performance.  I look forward to seeing a
          * clever, low cost, abstraction, if someone can figure one
          * out.
          */
      
      public void update()
      {
            // start from the bottome of the world

         for (int y = height - 1; y >= 0; --y)
         {
               // compute offset to this and next line

            int thisOffset = y * width;
            
               // are we at top or bottom

            boolean atTop = y == 0;
            boolean atBot = y == height - 1;

               // process line in random order
            
            for (int x: xRndIndex[rnd.nextInt(RND_INDEX_CNT)])
            {
                  // index of this pixel
               
               int ip = thisOffset + x;
               
                  // value this pixel

               int p = pixels[ip];

                  // don't do process certain things

               if (p == AIR || p == ROCK || p == EARTH)
                  continue;

                  // are we on a left or right edge?

               boolean atLeft = x == 0;
               boolean atRight = x == width - 1;

                  // indices of pixels around this particle

               int iuc = ip - width;
               int idc = ip + width;
               int idl = idc - 1;
               int idr = idc + 1;
               int il = ip - 1;
               int ir = ip + 1;

                  // get pixels for each index

               int uc = atTop            ? ROCK : pixels[iuc];
               int dc = atBot            ? ROCK : pixels[idc];
               int dl = atBot || atLeft  ? ROCK : pixels[idl];
               int dr = atBot || atRight ? ROCK : pixels[idr];
               int l =  atLeft           ? ROCK : pixels[il];
               int r =  atRight          ? ROCK : pixels[ir];
               
                  // the follwing actions propogate elements around the
                  // world, they do not conserve matter

                  // if fire, propogate fire

               if (p == FIRE1 || p == FIRE2 || p == FIRE3 || 
                   p == FIRE4 || p == FIRE5 || p == FIRE6)
               {
                  int[] burn = {atLeft  ? ip : il,
                                atRight ? ip : ir,
                                atTop   ? ip : iuc,
                                atBot   ? ip : idc};


                  for (int ib: burn)
                  {
                     int b = pixels[ib];

                     if ((b == PLANT || b == OIL) &&
                         rnd.nextInt(FIRE_CHANCE_IN) == 0)
                        pixels[ib] = FIRE1;
                     else
                        if (b == WATER)
                        {
                           pixels[ib] = AIR;
                           pixels[ip] = AIR;
                           p = AIR;
                           break;
                        }
                  }
                     // move fire along

                  if (p == FIRE1)
                  {
                     pixels[ip] = FIRE3;
                     continue;
                  }
                  if (p == FIRE2)
                  {
                     pixels[ip] = FIRE3;
                     continue;
                  }
                  if (p == FIRE3)
                  {
                     pixels[ip] = FIRE4;
                     continue;
                  }
                  if (p == FIRE4)
                  {
                     pixels[ip] = FIRE5;
                     continue;
                  }
                  if (p == FIRE5)
                  {
                     pixels[ip] = FIRE6;
                     continue;
                  }
                  if (p == FIRE6)
                  {
                     pixels[ip] = AIR;
                     continue;
                  }
               }
                  // if this is an everything sucker

               if (p == AIR_SOURCE)
               {
                  int[] targets = {atLeft  ? ip : il,
                                   atRight ? ip : ir,
                                   atTop   ? ip : iuc,
                                   atBot   ? ip : idc};
                  for (int it: targets)
                        pixels[it] = AIR;
                  continue;
               }
                  // if this is a water source

               if (p == WATER_SOURCE)
               {
                  int[] targets = {atLeft  ? ip : il,
                                   atRight ? ip : ir,
                                   atTop   ? ip : iuc,
                                   atBot   ? ip : idc};
                  for (int it: targets)
                     if (pixels[it] == AIR &&
                         rnd.nextInt(WATER_CHANCE_IN) == 0)
                        pixels[it] = WATER;
                  continue;
               }
                  // if this is a fire source

               if (p == OIL_SOURCE)
               {
                  int[] targets = {atRight ? ip : ir,
                                   atLeft  ? ip : il,
                                   atTop   ? ip : iuc,
                                   atBot   ? ip : idc};
                  for (int it: targets)
                     if (pixels[it] == AIR)
                        pixels[it] = OIL;
                  continue;
               }
                  // if this is a sand source

               if (p == SAND_SOURCE)
               {
                  int[] targets = {atLeft  ? ip : il,
                                   atRight ? ip : ir,
                                   atTop   ? ip : iuc,
                                   atBot   ? ip : idc};
                  for (int it: targets)
                     if (pixels[it] == AIR &&
                         rnd.nextInt(SAND_CHANCE_IN) == 0)
                        pixels[it] = SAND;
                  continue;
               }
                  // if this is a plant, propogate growth

               if (p == PLANT)
               {

                  int iul = iuc - 1;
                  int iur = iuc + 1;

                  int[] targets = {atLeft             ? ip : il,
                                   atLeft  || atTop   ? ip : iul,
                                   atRight            ? ip : ir,
                                   atRight || atTop   ? ip : iur,
                                   atTop              ? ip : iuc,
                                   atBot              ? ip : idc,
                                   atBot   || atLeft  ? ip : idl,
                                   atBot   || atRight ? ip : idr,
                  };
                  for (int ix: targets)
                     if (pixels[ix] == AIR)
                        for (int it: targets)
                           if (pixels[it] == WATER && rnd.nextInt(PLANT_CHANCE_IN) == 0)
                              pixels[it] = PLANT;
                  continue;
               }
                  // if this is a fire source

               if (p == FIRE_SOURCE)
               {
                  int[] targets = {atLeft  ? ip : il,
                                   atRight ? ip : ir,
                                   atTop   ? ip : iuc,
                                   atBot   ? ip : idc};
                  for (int it: targets)
                     if (pixels[it] == PLANT || pixels[it] == OIL)
                        pixels[it] = FIRE1;
                  continue;
               }
                  // all actions from this point on conserve matter
                  // we only calculate the place to which this particle
                  // will move, the the default is to do nothing

               int dest = NO_CHANGE;
                           
                  // if it's a oil

               if (p == OIL)
               {
                     // comput indices for up left and up right

                  int iul = iuc - 1;
                  int iur = iuc + 1;

                     // get pixels for each index

                  int ul = atTop || atLeft  ? ROCK : pixels[iul];
                  int ur = atTop || atRight ? ROCK : pixels[iur];

                     // if there is sand/earth above, erode that

                  if (uc == EARTH || uc == SAND)
                     dest = iuc;                     

                     // if pixles up left and up right sand/water

                  else if ((ul == EARTH || ul == SAND) && 
                           (ur == EARTH || ur == SAND))
                     dest = rnd.nextBoolean() ? iul : iur;

                     // if air underneath, go down
                  
                  else if (dc == AIR)
                     dest = idc;
                  
                     // if air on both sides below, pick one

                  else if (dl == AIR && dr == AIR)
                     dest = rnd.nextBoolean() ? idl : idr;

                     // if air only down left, go left

                  else if (dl == AIR)
                     dest = idl;

                     // if air only down right, go right

                  else if (dr == AIR)
                     dest = idr;

                     // if air on both sides below, pick one

                  else if ((l == AIR || l == EARTH || l == SAND) &&
                           (r == AIR || r == EARTH || r == SAND))
                     dest = rnd.nextBoolean() ? il : ir;

                     // if air only down left, go left

                  else if (l == AIR || l == EARTH || l == SAND)
                     dest = il;

                     // if air only down right, go right

                  else if (r == AIR || r == EARTH || r == SAND)
                     dest = ir;

                     // the case where water flows out two pixels
                  
                  else
                  {
                        // get items 2 pixels on either side of this one

                     int ill = ip - 2;
                     int irr = ip + 2;
                     int ll = x < 2         ? ROCK : pixels[ill];
                     int rr = x > width - 3 ? ROCK : pixels[irr];

                        // if air on both sides, pick one

                     if (ll == AIR && rr == AIR)
                        dest = rnd.nextBoolean() ? irr : ill;
                     
                        // if air only right right, go right right
                     
                     else if (rr == AIR)
                        dest = irr;
                     
                        // if air only left left, go left left

                     else if (ll == AIR)
                        dest = ill;
                  }
               }
                  // if it's water

               else if (p == WATER)
               {
                     // comput indices for up left and up right

                  int iul = iuc - 1;
                  int iur = iuc + 1;

                     // get pixels for each index

                  int ul = atTop || atLeft  ? ROCK : pixels[iul];
                  int ur = atTop || atRight ? ROCK : pixels[iur];

                     // if there is sand/earth above, erode that

                  if (uc == EARTH || uc == SAND)
                     dest = iuc;                     

                     // if pixles up left and up right sand/water

                  else if ((ul == EARTH || ul == SAND) && 
                           (ur == EARTH || ur == SAND))
                     dest = rnd.nextBoolean() ? iul : iur;

                     // if air underneath, go down
                  
                  else if (dc == AIR || dc == OIL)
                     dest = idc;

                     // if air on both sides below, pick one

                  else if ((dl == AIR || dl == OIL) && (dr == AIR || dr == OIL))
                     dest = rnd.nextBoolean() ? idl : idr;

                     // if air only down left, go left

                  else if (dl == AIR || dl == OIL)
                     dest = idl;

                     // if air only down right, go right

                  else if (dr == AIR || dr == OIL)
                     dest = idr;

                     // if air on both sides below, pick one

                  else if ((l == AIR || l == EARTH || l == SAND) &&
                           (r == AIR || r == EARTH || r == SAND))
                     dest = rnd.nextBoolean() ? il : ir;

                     // if air only down left, go left

                  else if (l == AIR || l == EARTH || l == SAND)
                     dest = il;

                     // if air only down right, go right

                  else if (r == AIR || r == EARTH || r == SAND)
                     dest = ir;

                     // the case where water flows out two pixels
                  
                  else
                  {
                        // get items 2 pixels on either side of this one

                     int ill = ip - 2;
                     int irr = ip + 2;
                     int ll = x < 2         ? ROCK : pixels[ill];
                     int rr = x > width - 3 ? ROCK : pixels[irr];

                        // if air on both sides, pick one

                     if (ll == AIR && rr == AIR)
                        dest = rnd.nextBoolean() ? irr : ill;
                     
                        // if air only right right, go right right
                     
                     else if (rr == AIR)
                        dest = irr;
                     
                        // if air only left left, go left left

                     else if (ll == AIR)
                        dest = ill;
                  }
               }
                  // all other elements behave like sand

               else
               {
                     // if air underneath, go down
               
                  if (dc == AIR || dc == WATER)
                     dest = idc;

                     // if air on both sides below, pick one

                  else if ((dl == AIR || dl == WATER) && 
                           (dr == AIR || dr == WATER))
                     dest = rnd.nextBoolean() ? idl : idr;

                     // if air only down left, go left

                  else if (dl == AIR || dl == WATER)
                     dest = idl;

                     // if air only down right, go right

                  else if (dr == AIR || dr == WATER)
                     dest = idr;
               }
                  // if a change is requried, swap pixles

               try
               {
                  if (dest != NO_CHANGE)
                  {
                     if (pixels[ip] == WATER_SOURCE)
                        out.println("swap1 WS & " + Element.lookup(pixels[dest]));
                     if (pixels[dest] == WATER_SOURCE)
                        out.println("swap2 WS & " + Element.lookup(pixels[ip]));

                     pixels[ip] = pixels[dest];
                     pixels[dest] = p;
                  }
               }
               catch (Exception ex)
               {
                  ex.printStackTrace();
                  out.println("     X: " + x);
                  out.println("     Y: " + y);
                  out.println("Source: " + Element.lookup(p));
                  out.println("Source: " + Element.lookup(pixels[ip]));
                  out.println("  Dest: " + Element.lookup(pixels[dest]));
                  
               }
            }
         }
      }
         /**
          * Return self as transferable data.  If the provided data
          * flavor is an image return self
          *
          * @param  flavor target flavor
          * @return Self if flavor is image flavor, otherwise null.
          */

      public Object getTransferData(DataFlavor flavor)
         throws UnsupportedFlavorException, IOException
      {
         if (flavor == DataFlavor.imageFlavor)
            return this;

         return null;
      }
         /**
          * Return transferable data flavors supported by this object,
          * in this case one the image flavor.
          *
          * @return An array of supported data flavors.
          */

      public DataFlavor[] getTransferDataFlavors()
      {
         DataFlavor[] flavors = {DataFlavor.imageFlavor};
         return flavors;
      }
         /**
          * Test if a provided data flavor is supported.
          *
          * @return true if data flavor supported, otherwise return false.
          */

      public boolean isDataFlavorSupported(DataFlavor flavor)
      {
         return (flavor == DataFlavor.imageFlavor);
      }
}

