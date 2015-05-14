# milia
Ona API client library in Clojure and ClojureScript

# Overview
This library exposes ONA endpoints for retrieving and submitting data through CLJ and CLJS applications.

# API Client Structure [Current]
* src/clj/milia/api
    * charts
    * organizations
    * projects
    * user
* src/clj/milia/io
    * requests
* src/cljs/milia/api
    * async_export
    * projects
* src/cljs/milia/io
    * requests
* src/cljx/milia/api
    * http
    * dataset

# [Todo] Proposed Client Structure
Since the requests to the ONA api from `cljs` or `clj` all return the same data, all the endpoints should be converted into cljx files that can be reused in other projects/dashboards.

* src/cljx/milia/api
    * charts
    * organizations
    * projects
    * user
    * profiles(Currently, this functionality is in the user namespace)
    * forms(Both forms and data are currently encapsulated in dataset namespace)
    * data
    * osm
    * restservices
    * notes
    * media
    * teams
* src/cljs/milia/api
    * async_export
* src/cljx/milia/io
    * requests
