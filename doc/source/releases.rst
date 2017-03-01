
.. _releases:

Releases
========

FRESCO uses `semantic <http://semver.org/>`_ versioning. FRESCO has
still not arrived at a stable API and the latest release is 0.1.0. You
can download this and older releases from `GitHub
<http://github.com/aicis/fresco/releases>`_ Here is a description of
the FRESCO releases:


0.1
---

The initial version, released 2015-11-30.
 
* The overall FRESCO framework.

* Implementation of the BGW and SPDZ protocol suites (only the SPDZ online phase). 

* A standard library, including
 
  * Generic protocols for comparison of integers.

  * Protocols for AES, DES, MD5, SHA1, and SHA256 provided by the
    `Bristol Cryptography Group
    <https://www.cs.bris.ac.uk/Research/CryptographySecurity/MPC>`_ .


0.2
---

This is a planned future release. It should be released ultimo march. We plan to
include:

* Implementation of the boolean TinyTables protocol suite. 

* A :ref:`fuelstation <Fuel Station>` for SPDZ preprocessed material which an
  independent third party can run. For the two-party setup, this will provide
  the same security guarantees.

* Improvements to the standard library:

  * The symmetric encryption cipher MIMC

  * Statistical functionalities such as covariance.

  * Integer division
