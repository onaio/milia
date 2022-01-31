[![example workflow](https://github.com/onaio/milia/actions/workflows/ci.yml/badge.svg?branch=feature-1)](https://github.com/onaio/milia/actions/workflows/ci.yml)

# milia
Ona API client library in Clojure and ClojureScript

## Overview
This library exposes ONA endpoints for retrieving and submitting data through CLJ and CLJS applications.

## Installation

Install via clojars with:

[![Clojars Project](http://clojars.org/onaio/milia/latest-version.svg)](http://clojars.org/onaio/milia)

## Deploying to clojars

lein deploy clojars

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

Milia stores credentials in the `milia.utils.remote/*credentials*` dynamic var
map, which defaults to:

```clojure
{:temp-token nil
 :token nil
 :username nil
 :password nil}
```

Set the map by creating a `binding`:

```clojure
(binding [milia.utils.remote/*credentials* {:username "username"
                                            :temp-token "SECRET TOKEN"}]
  ...)
```

And then within that binding change the value of the var using `set!`:
```clojure
(set! milia.utils.remote/*credentials* {:temp-token "NEW SECRET TOKEN"})
```

**WARNING** From CLJS you should ONLY set the `temp-token`, setting another
type of token would expose a permanent credential to client side attackers.

From CLJ you may also set the `token` and the `username` and `password`. If
`temp-token` exists it will be used, if not `token` will be used, and if
neither exist the `username` and `password` will be used for authentication.

### Credential auto-renewal

If a `temp-token` is supplied in the credentials var it will be used for
authentication before any other methods. As the name would suggest temp-tokens
are temporary and do expire. How an expiration is handled differs depending on
the target platform.

In CLJ, if a request fails with a 401 authorization error:

1. A request is made to the `user` endpoint using the `token` credential,
which is a permanent key. If a token was not supplied, this request will fail
and the failure is returned.
2. If this request succeeds, we will receive a new `temp-token`. We then call
`set!` and add the refreshed `temp-token` into the credentials var.
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

If milia raises an exception it will be a map with the key `reason` and,
depending on the type, a key `detail` which is another map with the keys
`response` and `status-code`. Examples are shown below:

No response:
```clojure
{:reason :no-http-response}
```

4xx response:
```clojure
{:reason :http-client-error
 :detail {:response <parsed-json-from-server>
          :status-code <status-code>}
```

5xx response:
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

## Using Milia in JavaScript

To use milia in JavaScript you will have to compile milia and then refer to the compiled output in your JavaScript code.
Below are example steps to follow and an example HTML file using the basic features of milia.

1. Compile the ClojureScript code to JavaScript: `lein cljsbuild once prod`
2. Copy the output JavaScript from `resources/public/js/lib/milia.js`, local to your root milia folder to the location of your choice.
3. Load the compiled JavaScript into your application.
3. Use the JavaScript helpers in `milia.utils.remote` to the set the remote server and the credentials to authenticate against that server.
4. Only call milia functions that are `export`ed to JavaScript.

For example:

```html
<html>
  <script type="text/javascript" src="resources/public/js/lib/milia.js"></script>
  <script type="text/javascript">
    milia.utils.remote.set_hosts("api.ona.io");
    milia.utils.remote.set_credentials("[username]", "[password]");
    milia.api.dataset.data(21438);
  </script>
</html>
```

> NOTE: In the above, and any other HTML example, the domain hosting the HTML page must have cross-origin access rights to the Ona server you are requesting data from.

## [TODO] Proposed Client Architecture

Convert remaining API endpoint files to cljc:

* charts
* images
* j2x
## running CLJS tests
- Install node version `12.x.x` (you can use nvm)
- Install karma cli
    - `npm install -g karma-cli`
- install npm test dependencies
    - `npm install karma --save-dev`
    - `npm install karma-cljs-test --save-dev`
    - `npm install karma-chrome-launcher --save-dev`
- Run the tests
    - `lein doo chrome-headless test once` or `lein doo chrome-headless test auto`
