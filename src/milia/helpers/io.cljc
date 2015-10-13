(ns milia.helpers.io)

(defn error-status?
  "True if the status code is nil or >= 400, false otherwise."
  [status]
  (or (nil? status) (>= status 400)))
