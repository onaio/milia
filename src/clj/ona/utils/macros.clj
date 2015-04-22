(ns ona.utils.macros)

(defprotocol LoadEnvVar
  (env-var-or-default [x]))

(defrecord EnvSetting [var-name default]
  LoadEnvVar
  (env-var-or-default [x]
    (or (System/getenv (name var-name)) default)))

(def env-config
  "Define config keys, environment variables and defaults"
  {:debug? (EnvSetting. :ONA_DEBUG false)
   :debug-api? (EnvSetting. :ONA_DEBUG_API false)
   :ona-api-server-host (EnvSetting. :ONA_API_SERVER_HOST "stage.ona.io")
   :ona-api-server-protocol (EnvSetting. :ONA_API_SERVER_PROTOCOL "https")
   :email-aws-host (EnvSetting. :ONA_EMAIL_AWS_HOST
                                 "email-smtp.us-east-1.amazonaws.com")
   :email-aws-user (EnvSetting. :ONA_EMAIL_AWS_USER "")
   :email-aws-pass (EnvSetting. :ONA_EMAIL_AWS_PASS "")
   ;; Deployments should set ONA_ENV to control the environment
   :env (EnvSetting. :ONA_ENV "local")})

(defmacro env-settings
  "Load environment variables."
  [key] (-> env-config key env-var-or-default))

(defmacro read-file [filename]
  "slurp-at-compile-time takes a filename, and returns contents as string,
   at compile time. Used for ClojureScript tests to read fixtures."
  (slurp filename))
