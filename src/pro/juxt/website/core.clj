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

(ns pro.juxt.website.core
  (:require
   [pro.juxt.website
    [article :refer (get-articles article-handler)]
    [blog :refer (get-blog-articles)]]
   [ring.util.response :refer (redirect) :as ring-resp]
   [clojure.java.io :refer (resource)]
   [stencil.core :as stencil]
   [io.pedestal.service.interceptor :refer (defbefore defhandler)]
   [io.pedestal.service.impl.interceptor :refer (interceptor)]
   [io.pedestal.service.http :as bootstrap]
   [io.pedestal.service.http.ring-middlewares :as middlewares]
   [io.pedestal.service.http.body-params :as body-params]
   [endophile.core :refer (mp to-clj)]
   [clojure.xml :as xml]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.zip :as zip]
   [clojure.data.zip :as dz]
   [clojure.data.zip.xml :as zxml :refer (xml-> xml1-> attr= tag= text)]
   [pro.juxt.website
    [util :refer (emit-element get-navbar markdown)]
    [events :refer (get-events)]])
  (:import (jig Lifecycle)))

;; TODO: Only do this for dev, not for prod.

;; TODO Don't do ANY caching at all
(stencil.loader/set-cache (clojure.core.cache/ttl-cache-factory {} :ttl 0))

(defhandler index-page [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (stencil/render-file
          "page.html"
          {:navbar (get-navbar "Home")
           :content (->> {:markdown markdown
                          :events get-events}
                         (stencil/render-file "index.html")
                         constantly)})})

(defhandler blog-page [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (stencil/render-file
          "page.html"
          {:navbar (get-navbar "Blog")
           :content (->> {:markdown markdown
                          :articles (get-blog-articles)}
                         (stencil/render-file "blog.html")
                         constantly)})})

(defhandler resource-index-page [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (stencil/render-file
          "page.html"
          {:navbar (get-navbar "Resources")
           :content (->> {:markdown markdown
                          :articles (get-articles)}
                         (stencil/render-file "resources-index.html")
                         constantly)})})

(defbefore root-page
  [{:keys [request system url-for] :as context}]
  (assoc context :response
         (ring-resp/redirect (url-for ::index-page))))

;; Jig component
(deftype Component [config]
  Lifecycle
  (init [_ system]
    system
    (update-in system [(:jig.web/app-name config) :jig.web/routes]
               conj [
                     ["/" {:get root-page} ^:interceptors
                      [(body-params/body-params)
                       bootstrap/html-body]]
                     ["/index.html" {:get index-page} ^:interceptors
                      [(body-params/body-params)
                       bootstrap/html-body]]
                     ["/blog.html" {:get blog-page}]
                     ["/resources/index.html" {:get resource-index-page}]
                     ["/articles/*path" {:get article-handler}]
                     ["/*static" {:get (middlewares/file (:static-path config))}]
                     ]))
  (start [_ system] system)
  (stop [_ system] system))
