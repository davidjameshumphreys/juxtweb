(ns pro.juxt.website.atom
  (:require
   clj-time.coerce
   [pro.juxt.website.pretty :refer (ppxml)]
   [pro.juxt.website.article :refer (parse-article compile-article-content)]
   [pro.juxt.website.util :refer (emit-element)]
   [hiccup.core :refer (html h)]
   [clj-time.format :as tf]
   [clojure.xml :as xml]
   [clojure.zip :as zip]
   [clojure.data.zip :as dz]
   [clojure.data.zip.xml :as zxml :refer (xml-> xml1-> attr= tag= text)]
   [hiccup.page :refer (xml-declaration)]))

(defn extract-article-sections [article]
  {:post [%]}
  (interpose
   "\n"
   (for [sect (map zip/node (xml-> (zip/xml-zip article)
                                     dz/descendants :article :section))]
     (with-out-str (emit-element sect))
     )))

(defn generate-feed [url-for articles]
  (str
   (xml-declaration "utf-8")
   "\r\n"
   (ppxml
    (html ;; mode?
     [:feed {:xmlns "http://www.w3.org/2005/Atom"}
      [:title {:type "text"} "JUXT Articles"]
      ;; TODO Make these absolute (there's a way of doing that in a recent version of Pedestal)
      [:link {:href (url-for :pro.juxt.website.core/articles-atom-feed) :rel "self" :type "application/atom+xml"}]
      [:link {:href (url-for :pro.juxt.website.core/index-page) :rel "hub"}]
      [:generator {:uri "https://github.com/juxt/juxtweb"} "JUXT's Atom Generator"]

      (for [{:keys [title abstract publication-date path article] {:keys [name email]} :author} (filter :syndicate articles)]
        [:entry
         [:title {:type "text"} title]
         [:updated (tf/unparse (tf/formatters :date-time-no-ms) (clj-time.coerce/from-date publication-date))]

         ;; "If the value of "type" is "xhtml", the content of the Text
         ;; construct MUST be a single XHTML div element." RFC 4287 (p8)

         [:link {:href (url-for :pro.juxt.website.article/article-handler :params {:path path})
                 :rel "alternate" :type "html"}]
         [:summary {:type "xhtml"} [:div {:xmlns "http://www.w3.org/1999/xhtml"} abstract]]
         [:author
          [:name name]
          (when email [:email email])]
         [:content {:type "xhtml"} [:div {:xmlns "http://www.w3.org/1999/xhtml"}
                                    (compile-article-content (parse-article path) path title 0)
]]])]))))
