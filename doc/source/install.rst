.. _install:

Installation
============

FRESCO is designed to run on Linux, Mac OS X, and Windows.


Downloading a Pre-Built FRESCO Release
--------------------------------------

You can download the latest release of FRESCO from :ref:`here
<releases>`.


Installing FRESCO from Source
-----------------------------

To install from source, make sure you have installed `git
<http://git-scm.org>`_, `Java 1.8 <http://java.com>`_, and `Maven
<https://maven.apache.org/>`_. Then run: ::

  $ git clone https://github.com/aicis/fresco/fresco.git
  $ cd fresco
  $ mvn install -DskipITs

This will download some dependencies, compile FRESCO, and run a number
of tests. If everything works fine Maven installs FRESCO on your
system and a FRESCO JAR file can now be found in the ``./target``
folder. Note that running the tests can take a while.

.. _scapi: https://github.com/cryptobiu/scapi
