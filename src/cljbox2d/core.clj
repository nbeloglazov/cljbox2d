(ns cljbox2d.core
  (:import (org.jbox2d.common Vec2)
           (org.jbox2d.dynamics Body BodyDef BodyType Fixture FixtureDef World)
           (org.jbox2d.dynamics.joints DistanceJoint RevoluteJoint)
           (org.jbox2d.collision.shapes PolygonShape CircleShape ShapeType)))

;(set! *warn-on-reflection* true)

;; ENUMS

(def body-types
  {:dynamic BodyType/DYNAMIC
   :static BodyType/STATIC
   :kinematic BodyType/KINEMATIC})

(def body-keywords
  (zipmap (vals body-types) (keys body-types)))

(def shape-types
  {:circle ShapeType/CIRCLE
   :polygon ShapeType/POLYGON})

(def shape-keywords
  (zipmap (vals shape-types) (keys shape-types)))

;; BASIC DATA

(defn vec2
  "Make a org.jbox2d.common.Vec2 from a clojure vector or two numbers.
  If v is already a Vec2, return it."
  ([v] (if (isa? (class v) Vec2)
         v
         (Vec2. (v 0) (v 1))))
  ([x y] (Vec2. x y)))

(defn xy
  "Gets a vector [x y] from a Vec2"
  [vec2]
  [(.x vec2) (.y vec2)])

;; WORLD

(def ^:dynamic *world*)

(defn create-world!
  "Create a new Box2D world. Gravity defaults to -10 m/s^2."
  ([]
     (create-world! [0 -10]))
  ([gravity]
     (alter-var-root (var *world*) (fn [_] (World. (vec2 gravity) true)))))

(defn step!
  "Simulate the world for a time step given in seconds"
  ([dt]
     (step! dt 5 5))
  ([dt velocity-iterations position-iterations]
     (.step *world* dt velocity-iterations position-iterations)))

;; CREATION OF OBJECTS

;; SHAPES

(defn circle
  "Create a circle shape"
  [radius]
  (let [shape (CircleShape.)]
    (set! (. shape m_radius) radius)
    shape))

(defn edge
  "Create an edge shape"
  [pt1 pt2]
  (let [shape (PolygonShape.)]
    (.setAsEdge shape (vec2 pt1) (vec2 pt2))
    shape))

(defn box
  "Create a box shape"
  ([hx hy]
     (let [shape (PolygonShape.)]
       (.setAsBox shape hx hy)
       shape))
  ([hx hy center]
     (box hx hy center 0))
  ([hx hy center angle]
     (let [shape (PolygonShape.)]
       (.setAsBox shape hx hy (vec2 center) angle)
       shape)))

(defn polygon
  "Create a polygon shape. Must be convex!
   It is assumed that the exterior is the right of each edge.
   i.e. vertices go counter-clockwise."
  [vertices]
  (let [shape (PolygonShape.)
        vv (to-array (map vec2 vertices))]
    (.set shape vv (count vertices))
    shape))

;; FIXTURES

(defn fixture-def
  "Create a Fixture definition: a shape with some physical properties"
  ;; TODO: filter (contact filtering)
  [shape & {:keys [density friction restitution user-data]
            :or {density 1, friction 0.3, restitution 0.3}}]
  (let [fd (FixtureDef.)]
    (set! (.shape fd) shape)
    (set! (.density fd) density)
    (set! (.friction fd) friction)
    (set! (.restitution fd) restitution)
    (set! (.userData fd) user-data)
    fd))

(defn fixture-from-def
  "Creates a Fixture on an existing Body from a FixtureDef."
  [body fd]
  (.createFixture body fd))

(defn fixture!
  "Creates a Fixture on an existing Body.
   A convenience wrapper for (fixture-from-def body (fixture-def ...))"
  [body shape & opts]
  (fixture-from-def body (apply fixture-def shape opts)))

;; BODIES

(defn body-def
  "Creates a Body definition, which holds properties but not shapes."
  [& {:keys [type position angle bullet fixed-rotation
             angular-damping linear-damping user-data]
      :or {type :dynamic, position [0 0], angle 0,
           bullet false, fixed-rotation false,
           angular-damping 0, linear-damping 0}}]
  (let [bd (BodyDef.)]
    (set! (.type bd) (body-types type))
    (set! (.position bd) (vec2 position))
    (set! (.angle bd) angle)
    (set! (.bullet bd) bullet)
    (set! (.fixedRotation bd) fixed-rotation)
    (set! (.angularDamping bd) angular-damping)
    (set! (.linearDamping bd) linear-damping)
    (set! (.userData bd) user-data)
    bd))

(defn body!
  "Creates a Body from a BodyDef and optional FixtureDefs."
  [bd & fixture-defs]
  (let [bod (.createBody *world* bd)]
    (doseq [fd fixture-defs]
      (fixture-from-def bod fd))
    bod))

;; QUERY OF OBJECTS

(defn bodyseq
  "Seq of all bodies in the world, or a body list"
  ([]
     (bodyseq (.getBodyList *world*)))
  ([body]
     (lazy-seq (if body (cons body (bodyseq (.getNext body)))))))

(defn fixtureseq*
  "Seq of fixtures from a Fixture list."
  [fixt]
  (lazy-seq (if fixt (cons fixt (fixtureseq* (.getNext fixt))))))

(defn fixtureseq
  "Seq of fixtures on a body or (concatenated) all in the world"
  ([body]
     (fixtureseq* (.getFixtureList body)))
  ([]
     (mapcat fixtureseq (bodyseq))))

(defn shape-type
  [fixt]
  (shape-keywords (.getType fixt)))

(defn body-type
  [body]
  (body-keywords (.getType body)))

;; COORDINATES

(defn local-point
  "Return body-local coordinates for a given world point"
  [body pt]
  (xy (.getLocalPoint body (vec2 pt))))

(defn world-point
  "Return world coordinates for a point in body-local coordinates,
   or for a body origin point"
  ([body]
     (xy (.getPosition body)))
  ([body pt]
     (xy (.getWorldPoint body (vec2 pt)))))

(defn local-center
  "Center of mass of a body in local coordinates"
  [body]
  (xy (.getLocalCenter body)))

(defn world-center
  "Center of mass of a body in world coordinates"
  [body]
  (xy (.getWorldCenter body)))

(defn local-coords
  "Local coordinates for a polygon (vertices) or circle (center)."
  [fixt]
  (let [shp (.getShape fixt)]
    (case (shape-type fixt)
      :circle (map xy [(.getVertex shp 0)]) ;[(.m_p shp)])
      :polygon (map xy (.getVertices shp)))))

(defn world-coords
  "World coordinates for a polygon (vertices) or circle (center)."
  [fixt]
  (let [body (.getBody fixt)]
    (map (partial world-point body) (local-coords fixt))))

(defn radius
  "Radius of a Fixture's shape."
  [fixt]
  (.m_radius (.getShape fixt)))

(defn angle
  "Angle of a body in radians"
  [body]
  (.getAngle body))

;; TODO get-mass get-inertia get-user-data


(defn -main
  "I don't do a whole lot."
  [& args]
  (println "Hello, World!"))