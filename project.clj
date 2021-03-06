(defproject cljbox2d "0.2.0-SNAPSHOT"
  :description "A clojure wrapper for JBox2D, for 2D physics simulation."
  :url "https://github.com/floybix/cljbox2d/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.jbox2d/jbox2d-library "2.1.2.2"]
                 [org.slf4j/slf4j-simple "1.6.6"]
                 [quil "1.6.0"]
                 [org.clojure/clojure "1.4.0"]]
  :autodoc {:copyright "Copyright (C) 2012 Felix Andrews."
            :web-src-dir "http://github.com/floybix/cljbox2d/blob/"
            :web-home "http://floybix.github.com/cljbox2d/"})
