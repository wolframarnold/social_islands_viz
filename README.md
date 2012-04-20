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

Running inside NetBeans
-----------------------

To set a Java system property in NetBeans, add it here:

Run -> Set Project Configuration -> Customize -> Run

In the "VM Options" field, add:

    -DMONGOHQ_URL="mongodb://:@127.0.0.1:27017/trust_exchange_development"

Note: Instead of `127.0.0.1`, you can also try `localhost`, if it doesn't work.

Running Standalone (without Jesque) with NetBeans
-------------------------------------------------

The default operation for the app is to listen on Jesque and take user_id's
from there to process.

If you want to run the app stand-alone, set this in the Run configuration in
NetBeans:

Run -> Set Project Configuration -> Customize -> Run -> Main class

set this to: `com.socialislands.viz.StandAlone` for stand-alone

or to: `com.socialislands.viz.Jesque` for Jesque operation.

Running Jesque from the command line
------------------------------------

Build as normal with Netbeans. This will create a script and the entire code
archive in a sub-folder `target` -- the contents of which are entirely
generated and it's not in the git repo.

A launch script is written to `target/bin/social_islands_viz`. You need to give
it execute permissions:

    chmod +x target/bin/social_islands_viz

Then run it with the `MONGOHQ_URL` set:

    MONGOHQ_URL="mongodb://:@127.0.0.1:27017/trust_exchange_development" target/bin/social_islands_viz

