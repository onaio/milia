[![Build Status](https://travis-ci.org/onaio/milia.svg?branch=master)](https://travis-ci.org/onaio/milia)

# milia
Ona API client library in Clojure and ClojureScript

## Overview
This library exposes ONA endpoints for retrieving and submitting data through CLJ and CLJS applications.

## Current API Endpoints
* charts
* forms
* media
* organizations
* projects
* profiles
* teams
* user
* xls-reports

## Setting credentials in milia

Milia stores credentials in the `milia.utils.remote/credentials` atom map. This map contains the keys `auth-token`, used with HTTP Digest Authentication, and `refresh-path`, used to fetch updated credentials (TODO clarify). Set the map with:

```clojure
(swap! milia-remote/credentials merge {:temp-token "SECRET TOKEN"})
```

From CLJS you can ONLY set the `temp-token`, setting another type of token would expose a permanent credetial to the client side (TODO: make sure this is enforced in the code).

From CLJ you may also set the `token` and the `username` and `password`. If `temp-token` exists it will be used, if not `token` will be used, and if neither exist the `username` and `password` will be used for authentication.

There are cases where you may want or need to override the default credentials atom. Do this using `with-local-vars`. For example, to force authenticate with the permanent token and retrieve a new temporary token:

```clojure
(with-local-vars [credentials {:token "PERMANENT SECRET TOKEN"}]
  ;; the credentials atom will now only contain the `token` key nd use that
  ;; for authentication.
  (:temp_token (milia.api.user/user)))
```

## Setting up a remote server
You can change the remote server URLs by importing and updating the hosts atom:

```clojure
(ns ona.io.remote
  (:require [milia.utils.remote :as milia-remote])

;; set the hosts atom in milia to custom hosts
(defn set-remote-host [] 
  (swap! milia-remote/hosts merge {:client "my-front-end.com"
                                   :data "my-ona-compatible-api.com"
                                   :request-protocol "https"
                                   :refresh-path "z/path"}))
```

## Debugging

Set the environment variable `DEBUG_API` to true to enable console debugging output on API requests.

## [Todo] Proposed Client Architecture
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
