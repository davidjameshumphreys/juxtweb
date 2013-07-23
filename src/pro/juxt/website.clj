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

(ns pro.juxt.website
  (:require
   [pro.juxt.website
    [article :refer (get-articles article-handler)]]
   [ring.util.response :refer (redirect)]
   [lamina.core :refer (enqueue)]
   [clojure.java.io :refer (resource)]
   [stencil.core :as stencil]
   [io.pedestal.service.interceptor :refer (defhandler)]
   [io.pedestal.service.impl.interceptor :refer (interceptor)]
   [io.pedestal.service.http.ring-middlewares :as middlewares]
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
  (:import (up.start Lifecycle)))

;; TODO: Only do this for dev, not for prod.
(stencil.loader/set-cache (clojure.core.cache/ttl-cache-factory {} :ttl 0))

(defn index-handler [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (stencil/render-file
          "page.html"
          {:navbar (get-navbar "Home")
           :content (->> {:markdown markdown
                          :events get-events}
                         (stencil/render-file "index.html")
                         constantly)})})

(defn resource-index-handler [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (stencil/render-file
          "page.html"
          {:navbar (get-navbar "Resources")
           :content (->> {:markdown markdown
                          :articles (get-articles)}
                         (stencil/render-file "resources-index.html")
                         constantly)})})

(defn ->index [req] (redirect "/index.html"))

(defonce bus (atom nil))

(defn create-routes []
  [[:juxtweb
    ["/" {:get 'pro.juxt.website/->index}]
    ["/index.html" {:get 'pro.juxt.website/index-handler}]
    ["/resources/index.html" {:get 'pro.juxt.website/resource-index-handler}]
    ["/articles/*path" {:get 'pro.juxt.website.article/article-handler}]
    ["/*static" {:get (middlewares/file "../website-static")}]]])

;; Below functions are for Up only

(defn init []
  (enqueue @bus {:up/topic :up.http/add-webapp
                 ;; :routes must be a function which returns the routes,
                 ;; because the values referenced by the symbols may
                 ;; have changed.
                 :routes create-routes
                 ;; Indicates whether the routes function should be called only once, or on every request.
                 :env :dev}))

(defrecord WebApplication [pctx]
  Lifecycle
  ;; Add the web application by sending a message on the the
  ;; :up.http/add-webapp topic with some routes.
  (start [_]
    (let [{b :bus} pctx]
      (reset! bus b)
      (init)
      )))
