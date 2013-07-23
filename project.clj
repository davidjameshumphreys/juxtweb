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

(require '(clojure [string :refer (join)]
                   [edn :as edn])
         '(clojure.java [shell :refer (sh)]))

(def version
  (let [{:keys [exit out err]} (sh "git" "describe" "--tags" "--long")]
    (if (= 128 exit) "0.0.1"
        (let [[[_ tag commits hash]] (re-seq #"(.*)-(.*)-(.*)" out)]
          (if (zero? (edn/read-string commits))
            tag
            (let [[[_ stem lst]] (re-seq #"(.*\.)(.*)" tag)]
              (join [stem (inc (read-string lst)) "-" "SNAPSHOT"])))))))

(def versions {:up "0.0.3"})

(defproject pro.juxt/juxtweb version
  :plugins [[lein-up ~(versions :up)]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.zip "0.1.1"]
                 [endophile "0.1.0"]
                 [clj-time "0.5.1"]]

  :source-paths ["src"]

  :up {:component pro.juxt.website/WebApplication
       :components {[up/up-firefox-reload ~(versions :up)]
                    {:host "localhost"
                     :topics [:juxtweb/resource-change]}
                    [up/up-logging ~(versions :up)] nil
                    [up/up-http ~(versions :up)] {:port 8081}
                    [up/up-nrepl ~(versions :up)] {:port 6011}
                    [up/up-pedestal-webapp ~(versions :up)] {:handler pro.juxt.website/handler}
                    [up/up-watch ~(versions :up)]
                    {:watches [{:topic :juxtweb/resource-change
                                :dir "resources"}
                               {:topic :juxtweb/resource-change
                                :dir "../website-static"}]}
                    [up/up-stencil ~(versions :up)] nil
                    }})
