Last updated 16 February 2007 by Robert Harris, freesand@trebor.org.

FreeSand is a pure java implementation of a cellular automata simulation
inspired by falling sand like games.  The most recent version of this
program can be found at:

    http://www.trebor.org/freesand/

FreeSand is released under the terms of the GNU General Public License.
The source code is contained inside the jar file.  It is hoped that
others will extend the functionality of this program.  The following are
some possible areas of improvement:

- addition of new elements
- tweaking behavior of existing elements
- addition of arrays with a one-to-one mapping to pixels for properties such
  as velocity, temperature or age, to increase behavioral complexity
- more "chaining" of elements to create fractal plant or flower like structures
- some way to composite images together
- any damn thing you can think of that would be cool and/or fun


Requirements:

- Java 1.5 or later

Features:

- load & save images
- pause and step simulation
- cut, copy & paste images
- conversion of imported image pixels to nearest element color
- window resize without total loss of image content
- full screen mode
- frame rate and other statistics viewable 
- a variety of brushes and brush sizes available
- on screen cueing of actions
- holding shift while painting momentarily toggles between element and element
  source
- keyboard shortcuts for nearly all actions
- "tool tips" when menu items moused over


Known Bugs:

- some brushes, specifically the "pyramid" brush, do not paint in the proper
  screen location when rotated
- brush shapes are wrong size on windows machines, this is likely an os
  limitation
- on fast machines, the printed frame rate caculation is broken


Change History:

16 February 2007 version 1 beta 3

   - improved element performance tests
   - added escape key to exits full screen mode
   - added (untested) windows/*nix build environment distribution

12 February 2007 version 1 beta 2

   - fixed flickering menus
   - fixed broken statistics view
   - added mac os build environment distribution

8 February 2007 version 1 beta

   - initial beta release

-------------------------------------------------------------------------------

Copyright (C) 2007 Robert B. Harris (freesand@trebor.org)
   
This program is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the
Free Software Foundation; either version 2 of the License, or (at your
option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along
with this program; if not, write to the Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
