Social Islands Viz
==================

This project is the visualization backend for the Social Islands application, which lets
Facebook users visualize their friend network.

This tool uses gephi to generate an svg file of the friends graph. The data is read directly from MongoDB.

This project is also configured for building with Maven, in preparation for deployment on Heroku.
It can be built and run directly from NetBeans.

Environments & Configuration
============================

I've adopted the Rails conventions about different environments for the Java
app as well. The app looks for a System Property `APP_ENV` or if not
set, an Environment Variable by the same name. If neither is found, then the environment
defaults to `development`. They can be set to `test` for testing (not currently used)
or `production`. On Heroku, we've set `APP_ENV` to `production`.

This matters for setting MongoDB and Redis configurations. Configuration files
for these live in `config/mongo.yml` and `config/redis.yml`, etc.

The YAML files follow the Rails conventions of defining a stanza for each
environment, e.g.

    development:
      host: http://localhost/...

    production:
      host: {{ MONGOHQ_URL }}
      user: ...
      password: ...

Note that in the 2nd example, `{{ ... }}` can be used to expand to a System
Property or Environment variable.


Running Locally
===============

Make sure you have:

* redis
* mongodb

installed as system components, e.g. via Homebrew on the Mac, or natively on Ubuntu.

The project needs a few Java libraires, but NetBeans should download all the
required dependencies during compilation.

In order to connect to your local MongoDB instance, the connection parameters
are specified in `config/mongo.yml`. For local operation `APP_ENV` should
be blank or set to `development`. (On Heorku `APP_ENV` is set to `production`.)


Running inside NetBeans
-----------------------

Specify the main class in:

Run -> Set Project Configuration -> Customize -> Run

You can choose Jesque operation or Standalone mode. Standalone mode is for
command line testing and it won't take jobs from Jesque. Note: You can run
the stand-alone app also from the command line and pass the job type and user_id
on the command line, see below.

To set Java properties, e.g. `APP_ENV` when running in NetBeans, add them to the
"VM Options" field of the Run dialog, e.g.:

    -DAPP_ENV=production -DMONGOHQ_URL=mongo://...


Running Jesque from the command line
------------------------------------

Build as normal with Netbeans. This will create a script and the entire code
archive in a sub-folder `target` -- the contents of which are entirely
generated and it's not in the git repo.

A launch script is written to `target/bin/social_islands_viz`. You need to give
it execute permissions:

    chmod +x target/bin/social_islands_viz

Then run it with the `MONGOHQ_URL` set:

    target/bin/social_islands_viz

Running StandAlone from the command line
----------------------------------------

For easier diagnostics, you can run the stand-alone mode also from the command line:

    sh target/bin/stand_alone <job_type> <user_id>

where `job_type` is either "score" or "viz", and `user_id` is the MongoDB
user_id.