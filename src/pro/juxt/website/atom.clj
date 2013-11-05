(ns pro.juxt.website.atom
  (:require
   clj-time.coerce
   [instaparse.core :as insta]
   [clojure.core.match :refer (match)]
   [pro.juxt.website.pretty :refer (ppxml)]
   [pro.juxt.website.article :refer (parse-article compile-article-content)]
   [pro.juxt.website.util :refer (emit-element)]
   [hiccup.core :refer (html h)]
   [clj-time.format :as tf]
   [clojure.xml :as xml]
   [clojure.zip :as zip]
   [clojure.walk :refer (postwalk)]
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

(defn transform-for-syndication
  "Remove certain style conventions to embed style (since we cannot
  control stylesheets on the feed browser)" [xhtml]
  (postwalk
  (fn [el]
    (match el
           {:tag :kbd} (assoc el :tag :span :attrs {:style "font-weight: bold"})

           {:tag :samp}  (-> el
                             (assoc :tag :span :attrs {:style "margin: 0; display: block"})
                             (update-in [:content] #(concat ["local$ "] %)))
           :else el))
  xhtml)
  #_(postwalk
   #(match %
           {:tag :kbd} (assoc % :tag :span :attrs {:style "font-weight: bold"})
           :else %)
   xhtml)
)

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
      [:generator {:uri "https://github.com/juxt/juxtweb"} "JUXT Atom Generator"]

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
                                    (compile-article-content
                                     (transform-for-syndication (parse-article path)) path title 0)
                                    ]]])])
    :indent false)))

#_(insta/transform  {}
                    (parse-article "manual-clojure-deployment.html"))

#_(postwalk #(cond (= :pre (first %)) "he" :otherwise %)
            (parse-article "manual-clojure-deployment.html"))

;; TODO ->
#_(clojure.pprint/pprint
 (postwalk
  (fn [el]
    (match el
           {:tag :pre} {:tag :pre :attr {} :content [(flatten (:content el))]}

           :else el))
  (parse-article "remote-nrepl.html")))


;;(do (println %) {:tag :span :attrs {:style "font-weight: bold"} :content %})


#_(clojure.pprint/pprint (parse-article "remote-nrepl.html"))

#_(compile-article-content (parse-article "remote-nrepl.html") "remote-nrepl.html" "foo" 0)
