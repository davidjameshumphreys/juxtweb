(ns pro.juxt.website.feeds
  (:require
   [hiccup.core :refer (html)]
   [pro.juxt.website
    [pretty :as pr]]
   [io.pedestal.service.interceptor :refer (defbefore defhandler)]))

(defn rss [url-for]
  (pr/ppxml
   (html {:mode :xml}
         [:rss {:version "2.0"
                :xmlns:content "http://purl.org/rss/1.0/modules/content/"
                :xmlns:wfw "http://wellformedweb.org/CommentAPI/"
                :xmlns:dc "http://purl.org/dc/elements/1.1/"
                :xmlns:atom "http://www.w3.org/2005/Atom"
                :xmlns:sy "http://purl.org/rss/1.0/modules/syndication/"
                :xmlns:slash "http://purl.org/rss/1.0/modules/slash/"}
          [:channel
           [:title "JUXT"]
           [:atom:link (url-for :pro.juxt.website.core/index-page)]
           [:link (url-for :pro.juxt.website.core/index-page)]
           ]])))

(defbefore feed-handler [context]
  (assoc context :response
         (let [feed (get-in context [:request :path-params :feed])]
           {:status 200
            :headers {"Content-Type" "text/plain"}
            :body (rss (:url-for context))})))
