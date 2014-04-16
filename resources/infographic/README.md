AORRA "Tier 0" public website prototype
=======================================

This prototype is a static HTML/JS/CSS website. There is no server-side code to run, and later production versions are not intended to have any server-side code either.

Viewing / Development
---------------------

For the map libraries to operate correctly though, it needs to be run from a HTTP/HTTPS location. i.e. not `file://` on your PC. That means you will need to run a local server for viewing/development.

On a Mac OS X or Linux operating system, you can start a suitable local server at <http://localhost:8000> like so:

    cd /path/to/the/code
    python -m SimpleHTTPServer 8000
    
For a Windows solution, you may wish to consider using [Mongoose](https://github.com/cesanta/mongoose/) or another web server.

Production
----------

In production, obviously you would want to use [Nginx](http://www.nginx.org), [Apache](http://httpd.apache.org) or another production-grade web server.
