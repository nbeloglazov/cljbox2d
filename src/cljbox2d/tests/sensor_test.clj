(ns cljbox2d.tests.sensor-test
  "A translation of Daniel Murphy's
   org.jbox2d.testbed.tests.SensorTest"
  (:use (cljbox2d core testbed)
        [cljbox2d.vec2d :only [v-scale]])
  (:import (org.jbox2d.callbacks ContactListener))
  (:require [quil.core :as quil]))

(def sensor (atom nil))

(def balls (atom []))

(defn setup-world! []
  (create-world!)
  (let [ground (body! {:type :static}
                      {:shape (edge [-40 0] [40 0])})
        sens (fixture! ground {:shape (circle 5 [0 10])
                               :is-sensor true})
        ballseq (for [i (range 7)
                      :let [x (+ -10 (* i 3))]]
                  (body! {:position [x 20]
                          :user-data (atom {:touching false})}
                         {:shape (circle 1)}))]
    (reset! sensor sens)
    (reset! balls (doall ballseq))
    (reset! ground-body ground)))

(defn sensor-touching-listener
  []
  (reify ContactListener
    (beginContact [_ contact]
      (let [fixt-a (.getFixtureA contact)
            fixt-b (.getFixtureB contact)
            bod (cond
                 (= fixt-a @sensor) (body fixt-b)
                 (= fixt-b @sensor) (body fixt-a))]
        (when bod
          (swap! (user-data bod) assoc-in [:touching] true))))
    (endContact [_ contact]
      (let [fixt-a (.getFixtureA contact)
            fixt-b (.getFixtureB contact)
            bod (cond
                 (= fixt-a @sensor) (body fixt-b)
                 (= fixt-b @sensor) (body fixt-a))]
        (when bod
          (swap! (user-data bod) assoc-in [:touching] false))))
    (postSolve [_ contact impulse])
    (preSolve [_ contact omanifold])))

(defn my-step []
  ;; process the buffer of contact points
  (let [cent (center @sensor)]
    (doseq [b @balls
            :when (:touching @(user-data b))
            :let [pt (position b)
                  d (map - cent pt)
                  d-unit (v-scale d)
                  forc (v-scale d-unit 100)]]
      (apply-force! b forc pt))))

(defn setup []
  (quil/frame-rate (/ 1 *timestep*))
  (setup-world!)
  (.setContactListener *world* (sensor-touching-listener))
  (reset! step-fn my-step))

(defn -main
  "Run the test sketch."
  [& args]
  (quil/defsketch test-sketch
    :title "Sensor Test"
    :setup setup
    :draw draw
    :key-typed key-press
    :mouse-pressed mouse-pressed
    :mouse-released mouse-released
    :mouse-dragged mouse-dragged
    :size [600 500]))
