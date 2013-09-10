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

(ns pro.juxt.website.article
  (:require
   [clojure.java.io :as io :refer (resource)]
   [clojure.tools.logging :refer :all]
   [clojure.xml :as xml]
   [clojure.zip :as zip]
   [clojure.edn :as edn]
   [clojure.walk :as walk]
   [clojure.data.zip :as dz]
   [hiccup.core :as hiccup]
   [io.pedestal.service.interceptor :refer (defbefore defhandler)]
   [stencil.core :as stencil]
   [pro.juxt.website.util :refer (get-navbar markdown emit-element)]
   [clojure.data.zip.xml :as zxml :refer (xml-> xml1-> attr= tag= text)]))

(defn parse-article [path]
  (some->> path resource io/input-stream xml/parse))

(defn grab-abstract [article]
  (xml1-> (zip/xml-zip article)
       dz/descendants :aside (attr= :class "abstract") zip/node))

(defn wrap-in-div [el class]
  {:tag :div :attrs {:class class} :content [el]})

(defn toc [article]
  (map-indexed (fn [ix node] {:section (inc ix)
                              :title (zxml/text node)})
               (->
                article
                zip/xml-zip
                (xml-> dz/descendants :article :section :h1)
                )))

(defn extract-section-title [section]
  (xml1-> (zip/xml-zip section) :h1 zxml/text))

(defn create-link [to-sect-no to-section]
  (if to-section
    {:tag :p :attrs {:class "next-section"}
     :content [{:tag :a :attrs {:href (format "?sect=%d" to-sect-no)} :content ["Next section: " (extract-section-title to-section)]}]}
    ""))

(defn extract-section [article sectno]
  (update-in article [:content]
             (fn [content]
               (case sectno
                 0 content ;; The whole article

                 1 (let [[a b] (split-with (comp not (partial = :section) :tag) content)]
                     (concat a (take 1 b) [(create-link (inc sectno) (second b))]))

                 (let [[a b] (split-at sectno (filter (comp (partial = :section) :tag) content))]
                   [(last a)
                    (create-link (inc sectno) (first b))
                    ])))))

(defn compute-label-map [content tag]
  (->> content
       xml-seq
       (filter #(= (:tag %) tag))
       (map-indexed #(vector (get-in %2 [:attrs :id]) (inc %1)))
       (into {})))

(defn create-article-html-body [{:keys [url-for]} path sect {:keys [title id] :as metadata}]
  (let [article (parse-article (str "articles/" path))
        refs (merge (compute-label-map article :figure))]
    (stencil/render-file
     "page.html"
     {:navbar (get-navbar url-for nil)
      :markdown markdown
      :content
      (constantly
       (stencil/render-file
        "article.html"
        {:article (fn [] (stencil/render-string
                          (-> (some->> article xml-seq
                                       (filter #(= (:tag %) :article)) first)
                              (extract-section sect)
                              (wrap-in-div "container-narrow")
                              emit-element with-out-str)
                          {:title title
                           :path (str "/articles/" path)
                           :snippet #(if-let [res (io/resource (format "articles/%s/%s" (second (first (re-seq #"(.*).html" path))) %))]
                                       (hiccup/html [:pre [:samp (hiccup/h (slurp res))]])
                                       (format "(Resource not found: %s)" %))
                           :ref #(format "Figure %s" (refs %))}))
         :comments (fn [] (stencil/render-file
                           "disqus.html"
                           {:disqus
                            [{:varname "shortname" :value "juxtpro"}
                             {:varname "identifier" :value id}
                             ;; We use the prod link (juxt.pro) so that
                             ;; our test versions also show prod
                             ;; comments (to get an idea of how the page
                             ;; is looking)
                             {:varname "url"
                              :value (str "https://juxt.pro"
                                          (url-for ::article-handler :params {:path path}))}
                             {:varname "title" :value title}]}))}))})))

(defbefore article-handler [context]
  (assoc context :response
         (let [path (get-in context [:request :path-params :path])
               sect (get-in context [:request :query-params :sect])
               metadata (->> "articles.edn" resource slurp edn/read-string (filter #(= (:path %) (str "articles/" path))) first)]
           {:status 200
            :headers {"Content-Type" "text/html"}
            :body (create-article-html-body context path (if sect (Integer/parseInt sect) 0) metadata)})))

(defn get-articles []
  (for [article (edn/read-string (slurp (resource "articles.edn")))]
    (let [a (parse-article (:path article))]
      (assoc article
        :toc (when (:chunked article) (toc a))
        :abstract (with-out-str (emit-element (grab-abstract a)))))))
