(ns pro.juxt.website.atom
  (:require
   clj-time.coerce
   [pro.juxt.website.pretty :refer (ppxml)]
   [hiccup.core :refer (html h)]
   [clj-time.format :as tf]
   [hiccup.page :refer (xml-declaration)])
)

(defn generate-feed [url-for articles]
  (str
   (xml-declaration "utf-8")
   "\r\n"
   (ppxml
    (html
     [:feed {:xmlns "http://www.w3.org/2005/Atom"}
      [:title {:type "text"} "JUXT Articles"]
      ;;    [:subtitle "A subtitle."]
      ;; TODO Make these absolute (there's a way of doing that in a recent version of Pedestal)
      [:link {:href (url-for :pro.juxt.website.article/articles-atom-feed) :ref "self"}]
      [:link {:href (url-for :pro.juxt.website.core/resource-index-page)}]
      ;;    [:id "urn:uuid:60a76c80-d399-11d9-b91C-0003939e0af6"]
      [:updated "2003-12-13T18:30:02Z"]

      (for [{:keys [title abstract publication-date] {:keys [name email]} :author} articles]
        [:entry
         [:title {:type "text"} title]
         ;; Use clj-time
         ;;         [:updated (.format (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ssXXX") publication-date)]
         [:updated (tf/unparse (tf/formatters :date-time-no-ms) (clj-time.coerce/from-date publication-date))]

         ;; "If the value of "type" is "xhtml", the content of the Text
         ;; construct MUST be a single XHTML div element." RFC 4287 (p8)

         [:summary {:type "xhtml"} [:div {:xmlns "http://www.w3.org/1999/xhtml"} abstract]]
         [:author
          [:name name]
          (when email [:email email])]
         ])])))

  )




;; <?xml version="1.0" encoding="utf-8"?>

;; <feed xmlns="http://www.w3.org/2005/Atom">

;;         <title>Example Feed</title>
;;         <subtitle>A subtitle.</subtitle>
;;         <link href="http://example.org/feed/" rel="self" />
;;         <link href="http://example.org/" />
;;         <id>urn:uuid:60a76c80-d399-11d9-b91C-0003939e0af6</id>
;;         <updated>2003-12-13T18:30:02Z</updated>


;;         <entry>
;;                 <title>Atom-Powered Robots Run Amok</title>
;;                 <link href="http://example.org/2003/12/13/atom03" />
;;                 <link rel="alternate" type="text/html" href="http://example.org/2003/12/13/atom03.html"/>
;;                 <link rel="edit" href="http://example.org/2003/12/13/atom03/edit"/>
;;                 <id>urn:uuid:1225c695-cfb8-4ebb-aaaa-80da344efa6a</id>
;;                 <updated>2003-12-13T18:30:02Z</updated>
;;                 <summary>Some text.</summary>
;;                 <author>
;;                       <name>John Doe</name>
;;                       <email>johndoe@example.com</email>
;;                 </author>
;;         </entry>

;; </feed>
