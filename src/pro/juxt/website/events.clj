;; Copyright © 2013, JUXT. All Rights Reserved.
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;;
;; By using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.
;;
;; You must not remove this notice, or any other, from this software.

(ns pro.juxt.website.events
  (:require
   [clj-time.format :as tf]
   [clj-time.core :as time]
   [clojure.java.io :as io :refer (resource)]
   [clojure.edn :as edn]
   [endophile.core :refer (mp to-clj)]
   [pro.juxt.website.util :refer (emit-element)]
   )
  (:import (org.joda.time DateTime LocalDateTime))
  )

(def date-formatter (tf/formatter "EEEE, d MMMM. HH:mm"))
(def datetime-formatter (tf/formatter "EEEE, d MMMM."))

(defn get-time [d]
  (.getTime d))

(defn as-date [d tz]
  (.print
   ;; This is just a hack that works for our purposes.
   (if (#{0 1 2} (.getHours d)) datetime-formatter date-formatter)
   (LocalDateTime. (DateTime. d) (time/time-zone-for-id tz))))

(defn render [ev]
  (str
   "<em>"
   (as-date (:date ev) (or (:timezone ev) "Europe/London"))
   "</em>"
   (->> ev :description mp to-clj (map emit-element) dorun with-out-str)))

(defn get-events [content]
  (let [now (.getTime (java.util.Date.))
        [prev-events upcoming-events] (split-with #(< ((comp get-time :date) %) now) (sort-by (comp get-time :date) (comparator <) (edn/read-string (slurp (resource "events.edn")))))]
    (str "<h3>Upcoming events</h3>"
         (apply str (map render upcoming-events))
         "<h3>Past events</h3>"
         (apply str (map render prev-events)))
    )
  )
