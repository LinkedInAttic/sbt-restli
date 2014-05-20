Rest.li Play Example
====================

This is a simple, minimal play project showing how rest.li services and clients can be used in a play application.

Writing Rest.li Server on Play
------------------------------

The server side is demonstrated by the classes in:

    sample-server/app/com/linkedin/pegasus/play

And also requires the `play.plugins` and `routes` files in:

    sample-server/conf

Using a Rest.li Client in Play
------------------------------

Using a rest.li client in a play application is demonstrated by the classes in:

    sample-server/test

Running the server
------------------

    play run

To test, run:

    curl http://localhost:9000/samples/1

Running the Test Suite
----------------------

The test suite will make a REST request to the rest.li server using a rest.li client.  To run:

    play test