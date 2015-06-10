# overview #

freesand is a pure java implementation of a cellular automata simulation inspired by falling sand like games.  it is released under the terms of the gnu general public license.  the freesand source code is contained inside the distribution packages.

> version:           1.0 beta 3
> date:              16 february 2007
> requirements:      java 1.5+
> mac os x:          freesand.dmg
> windows/**nix:      freesand.zip**

build environment packages are also provided.  the windows/**nix package has NOT been tested.**

> requirements:      ant (built with v1.6.5)

mac os x installation:

  1. download and mount freesand.dmg
  1. drag FreeSand.app to applications directory
  1. double click FreeSand.app to start

if the application does not start properly, it is possible that you have an older version of java.  if you have mac os 10.4, running the software update from the apple menu should update the java version.


windows/**nix installation:**

  1. download freesand.jar
  1. double click that jar

if the program does not start properly, it is possible that you have an older version of java.  an up-to-date version of java can be found at http://java.sun.com/.

## out of date text ##

mac os x build environment installation:

  1. download and mount freesand-build.dmg
  1. drag freesand folder to your local drive

the build environment requires apache ant, which is included in the apple developer tools.  it was developed with ant version 1.6.5.  for a list of provided build targets type:

> ant -p

on the command line in freesand directory.  to build and run the program type:

> ant sand


windows/**nix build environment installation:**

  1. download and unzip freesand-build.zip

the build environment requires apache ant, and was developed with ant version 1.6.5.  this package has NOT been tested.  for a list of provided build targets type:

> ant -p

on the command line in freesand directory.  to build and run the program type:

> ant sand


miscellaneous command line operations:

to identify the version of java on your computer:

> java -version

to run the program contained in freesand.jar:

> java -jar freesand.jar

to identify the version of ant on your computer:

> ant -version


change history:

16 february 2007

> - improved element performance tests
> - added escape key to exits full screen mode
> - added (untested) windows/**nix build environment distribution**

12 february 2007

> - fixed flickering menus
> - fixed broken statistics view
> - added mac os build environment distribution

8 february 2007

> - initial beta release