[![Build Status](https://travis-ci.org/elasticmodules/demo.svg?branch=master)](https://travis-ci.org/elasticmodules/demo)
# Welcome to Elastic Modules Demo

This is a small scratchpad demo project for part of what is coming together in Elastic Modules.  It brings together
several key libraries in a working whole:

* Spray & Akka -- Akka is a Scala toolkit and runtime for building highly concurrent, distributed, and fault tolerant
event-driven applications on the JVM. Spray (soon to be known as "Akka HTTP") is an open-source toolkit for building
REST/HTTP-based integration layers on top of Scala and Akka.  Effectively, Spray provides a fast, lightweight mapping
between HTTP requests and Akka actors.
* spray-websockets -- This project provides core changes to internal Spray pipelines such that websocket upgrade requests
are fulfilled by elegantly managing Akka actor state machine changes.
* ExtJS -- While ExtJS is no longer open source, for enterprise projects, it's an extremely capable UI framework.
This is especially true when combined with Sencha Architect.
* Ext.ux.data.proxy.WebSocket -- This project connects ExtJS "Stores" (the model in ExtJS MVC) to the server for push
remoting to the client desktop.  Stores back view components and update them with an internal event notifications scheme.

Pulled together, this demonstration allows multiple browser clients to maintain shared state between browsers by propagating
event messages between browsers.  When the store is updated on each browser, the view components are notified, resulting
in updates across all browsers.

This is a simple demo, and there are plenty of improvements that could be made.  Please feel free to contribute
pull requests as you wish and we will include them as indicated.

## Running the demo:

* Clone project locally
* ```mvn package exec:java```
