(defproject rest_service "0.1.0"
  :description "REST service examples"
  :url "http://clojure-liberator.github.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :plugins [[lein-ring "0.8.3"]]

  :ring {:handler rest-service.core/app}

  :dependencies [[org.clojure/clojure "1.4.0"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 [compojure "1.1.5"]
                 [cheshire "5.0.2"]
                 [liberator "0.8.0"]])