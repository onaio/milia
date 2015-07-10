[![Build Status](https://travis-ci.org/onaio/milia.svg?branch=master)](https://travis-ci.org/onaio/milia)

# milia
Ona API client library in Clojure and ClojureScript

# Overview
This library exposes ONA endpoints for retrieving and submitting data through CLJ and CLJS applications.

# Current API Endpoints
* charts
* forms
* media
* organizations
* projects
* profiles
* teams
* user
* xls-reports

# Setting up a remote server
You can add a new ONA api server url by importing and modifying the hosts atom:

```clojure
    (ns ona.io.remote
      (:require [environ.core :refer [env]]
                [milia.utils.remote :as milia-remote])

      ;; set the hosts atom in milia to point to the remote host
      (defn set-remote-host []
        (let [{:keys [ui-host api-host protocol]} (:host env)]
          (swap! milia-remote/hosts merge {:ui ui-host
                                           :data api-host
                                           :ona-api-server-protocol protocol})))
```

# Debugging

Set the environment variable `DEBUG_API` to true to enable console debugging output on API requests.

# [Todo] Proposed Client Architecture
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
