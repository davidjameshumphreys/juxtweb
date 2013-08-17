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

(ns pro.juxt.website.blog
  (:require
   [pro.juxt.website
    [util :refer (markdown)]]
   [clojure.java.io :as io :refer (resource)]
   [clojure.edn :as edn]))

(defn get-blog-articles []
  (for [entry (edn/read-string (slurp (resource "blog-articles.edn")))]
    {:content (markdown (:path entry))}
    ))
