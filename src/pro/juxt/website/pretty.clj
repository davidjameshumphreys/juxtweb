(ns pro.juxt.website.pretty
  (:require [clojure.java.io :as io]))

(defn ppxml [xml & {:keys [indent indent-amount] :or {indent true indent-amount 2}}]
  (let [in (javax.xml.transform.stream.StreamSource.
            (java.io.StringReader. xml))
        writer (java.io.StringWriter.)
        out (javax.xml.transform.stream.StreamResult. writer)
        transformer (.newTransformer
                     (javax.xml.transform.TransformerFactory/newInstance)
                     (javax.xml.transform.stream.StreamSource.
                      (java.io.StringReader. (slurp (io/resource "pretty.xsl"))))
                     )]
    (.setOutputProperty transformer
                        javax.xml.transform.OutputKeys/OMIT_XML_DECLARATION "yes")
    (.setOutputProperty transformer
                        javax.xml.transform.OutputKeys/INDENT (if indent "yes" "no"))
    (.setOutputProperty transformer
                        "{http://xml.apache.org/xslt}indent-amount" (str indent-amount))
    (.setOutputProperty transformer
                        javax.xml.transform.OutputKeys/METHOD "xml")
    (.transform transformer in out)
    (-> out .getWriter .toString)))
