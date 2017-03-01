=====
Tools
=====

This sections is for various tools which rests within the repository of FRESCO. 

.. _Fuel Station:

The Fuel Station
----------------

The Fuel Station is a stand-alone tool which enables a third party to create
preprocessed material used within the SPDZ protocol suite when running the
online phase. It is powered by Spring, and you just need to set a few properties
in order to make it run. These are found within
tools/fuelstation/src/main/resources/prepro.properties.

The possible values to set are:

| mod=[.]
| alpha1=[.]
| alpha2=[.]

mod is the global modulus used throughout the computation. alpha{1,2} are the
shares of the global secret key alpha which can only be known by the owner of
the fuel station. The tool currently only functions for a two party setup using
the fuel station since the security model is identical to the regular two party
setup. In both cases, it is required that two parties work together in order to
break security and reveal the last party's input. If one where to use the Fuel
Station in a setup with more than two parties, the security would still reduce
to just two parties working together (a malicious MPC party and the fuel station
owner).

The initial version of this tool does not contain authentication at the
endpoints, meaning that anyone can call the tool and obtain e.g. the alpha{1,2}
shares, breaking the security. This has to be fixed either within the tool
itself, or by using firewall rules to ensure the security of the system.

Inner workings:

The way it works is by spawning a thread for each type of preprocessed material
that the SPDZ protocol suite uses. Each thread then generates material and puts
it into a blocking queue. Whenever a party asks for preprocessed material, this
is fetched from the queue which blocks until enough material is available and
then streams the material back to the asking party. The advantage of this
approach over the normal approach where the two parties runs a preprocessing
protocol is speed. It is quite fast to generate new material, and this will
normally never be the bottleneck. Instead, the bottleneck is the network
communication speed. 
