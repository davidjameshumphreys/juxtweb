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

(ns pro.juxt.website.profiles
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io :refer (resource)]
            [pro.juxt.website.util :refer (emit-element)]
            [endophile.core :refer (mp to-clj)]
            [stencil.core :as stencil]))

(defn render [profile]
  (stencil/render-file
   "profile.html" (assoc profile :description (->> profile :bio mp to-clj (map emit-element) dorun with-out-str))))

(defn get-profiles [content]
   (str "<h3>Profiles</h3>"
        (apply str (map render (edn/read-string (slurp (resource "profiles.edn")))))))
