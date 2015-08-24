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

Milia stores credentials in the `milia.utils.remote/*credentials*` atom map, which defaults to:

```clojure
{:temp-token nil
 :token nil
 :username nil
 :password nil}
```

Set the map with:

```clojure
(swap! milia.remote/*credentials* merge {:temp-token "SECRET TOKEN"})
```

**WARNING** From CLJS you should ONLY set the `temp-token`, setting another
type of token would expose a permanent credential to client side attackers.

From CLJ you may also set the `token` and the `username` and `password`. If
`temp-token` exists it will be used, if not `token` will be used, and if
neither exist the `username` and `password` will be used for authentication.

There are cases where you may want or need to override the default credentials
atom. Do this using `binding`. For example, to force authenticate with the
permanent token and retrieve a new temporary token:

```clojure
(binding [*credentials* (atom {:token "PERMANENT SECRET TOKEN"}]
  ;; the credentials atom will now only contain the `token` key and use that
  ;; for authentication.
  (:temp_token (milia.api.user/user)))
```

### Credential auto-renewal

As explained above, if a `temp-token` is supplied in the credentials atom it
will be used for authentication before any other methods. As the name would
suggest temp-tokens are temporary and do expire. How an expiration is handled
differs depending on the target platform.

In CLJ, if a request fails with a 401 authorization error:

1. A request is made to the `user` endpoint using the `token` credential,
which is a permanent key. If a token was not supplied, this request will fail
and the failure is returned.
2. If this request succeeds, we will receive a new `temp-token`. We then call
`swap!` and insert the refreshed `temp-token` into the credentials atom.
3. We retry our initial request. If it fails again, an exception is raised
or returned depending on the value of the `supress-4xx-exceptions?` option
in the originating call.

In CLJS, if a request fails with a 401 authorization error:

We reload the page. *TODO* attempt a session based refresh mediated by
a client backend.

## Handling Errors

If the server that milia attempts to connect to returns an exceptional status,
>=400, or if there is a connection problem, milia may raise an exception.

Milia will raise an exception if:

1. there is a connection problem in which the server does no return a response
   or returns a response that does not include a status code,
2. the server returns a 4xx status and the `suppress-4xx-exceptions?` flag is
    false,
3. the server returns a 5xxx status code.

If milia raises an exception its format will be as below depending on the
type of exception:

1. No response:
  ```clojure
  {:reason :no-http-response}
  ```
2. 4xx response:
  ```clojure
  {:reason :http-client-error
   :detail {:status-code <status-code>
            :response <parsed-json-from-server>}
   ```
3. 5xx response:
   ```clojure
   {:reason :http-server-error
    :detail {:response <raw-response>
             :status-code <status-code>}
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
Convert remaining API endpoint files to cljc:

* charts
* images
* j2x
