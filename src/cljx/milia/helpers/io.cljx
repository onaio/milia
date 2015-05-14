(ns milia.helpers.io)

(defn error-status?
  "True if the status code is >= 400, false otherwise."
  [status]
  (>= status 400))
