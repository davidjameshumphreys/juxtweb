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
   jig
   [jig.web.app :refer (add-routes)]
   [pro.juxt.website
    [article :refer (get-articles article-handler)]
    [blog :refer (get-blog-articles)]]
   [ring.util.response :refer (redirect) :as ring-resp]
   [ring.middleware.file :as file]
   [clojure.java.io :refer (resource)]
   [stencil.core :as stencil]
   [io.pedestal.service.interceptor :as interceptor :refer (defbefore defhandler definterceptorfn interceptor)]
   [io.pedestal.service.http :as bootstrap]
   [io.pedestal.service.http.ring-middlewares :as middlewares]
   [io.pedestal.service.http.body-params :as body-params]
   [endophile.core :refer (mp to-clj)]
   [clojure.xml :as xml]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.zip :as zip]
   [clojure.data.zip :as dz]
   [clojure.tools.logging :refer :all]
   [ring.util.codec :as codec]
   [clojure.data.zip.xml :as zxml :refer (xml-> xml1-> attr= tag= text)]
   [pro.juxt.website
    [util :refer (emit-element get-navbar markdown)]
    [events :refer (get-events)]])
  (:import (jig Lifecycle)))

(defn page-response [context active-nav content]
  (assoc context :response
         (ring-resp/response
          (stencil/render-file
           "page.html"
           {:ctx (let [ctx (get-in context [:app :jig.web/context])]
                   (if (= ctx "/") "" ctx))
            :navbar (get-navbar (:url-for context) active-nav)
            :content (constantly content)}))))

(defbefore index-page [context]
  (page-response
   context "Home"
   (stencil/render-file
    "index.html" {:markdown markdown :events get-events})))

(defbefore blog-page [context]
  (page-response
   context "Blog"
   (stencil/render-file
    "blog.html" {:markdown markdown :articles (get-blog-articles)})))


(defbefore resource-index-page [context]
  (page-response
   context "Resources"
   (stencil/render-file
    "resources-index.html" {:markdown markdown :articles (get-articles)})))

(defbefore root-page
  [{:keys [url-for] :as context}]
  (assoc context :response
         (ring-resp/redirect (url-for ::index-page))))

(definterceptorfn static
  [root-path & [opts]]
  (interceptor/handler
   ::static
   (fn [req]
     (infof "Request for static is %s" req)
     (ring-resp/file-response
      (codec/url-decode (get-in req [:path-params :static]))
      {:root root-path, :index-files? true, :allow-symlinks? false}))))

;; Jig component
(deftype Component [config]
  Lifecycle
  (init [_ system]
    (add-routes
     system config
     [
      ["/" {:get root-page}]
      ["/index.html" {:get index-page}]
      ["/blog.html" {:get blog-page}]
      ["/resources/index.html" {:get resource-index-page}]
      ["/articles/*path" {:get article-handler}]
      ["/*static" {:get (static (:static-path config))}]
      ]))

  (start [_ system]
    (let [cache (clojure.core.cache/lru-cache-factory {})]
      (stencil.loader/set-cache cache)
      (assoc system :stencil-cache cache)))

  (stop [_ system] system))
