Social Islands Viz
==================

This project is the visualization backend for the Social Islands application, which lets
Facebook users visualize their friend network.

This tool uses gephi to generate an svg file of the friends graph. The data is read directly from MongoDB.

This project is also configured for building with Maven, in preparation for deployment on Heroku.
It can be built and run directly from NetBeans.

Running Locally
===============

Make sure you have:

* redis
* mongodb

installed as system components, e.g. via Homebrew on the Mac, or natively on Ubuntu.

The project needs a few Java libraires, but NetBeans should download all the
required dependencies during compilation.

In order to connect to your local MongoDB instance, the connection parameters
need to be passed through an environment variable or a Java system property.

Heroku will use environment variables to set the MongoDB connection parameters,
but that's not easy to do for an IDE like NetBeans. So, if the `MONGOHO_URL`
environment variable is not set, it'll try to look for a Java system
property by the same name.

To set a Java system property in NetBeans, add it here:

Run -> Set Project Configuration -> Customize -> Run

In the "VM Options" field, add:

    -DMONGOHQ_URL="mongodb://:@localhost:27017/trust_exchange_development"

Running Standalone (without Jesque)
===================================

The default operation for the app is to listen on Jesque and take user_id's
from there to process.

If you want to run the app stand-alone, set this in the Run configuration in
NetBeans:

Run -> Set Project Configuration -> Customize -> Run -> Main class

set this to: `com.socialislands.viz.StandAlone` for stand-alone

or to: `com.socialislands.viz.Jesque` for Jesque operation.

