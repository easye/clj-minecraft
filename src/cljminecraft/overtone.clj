(ns cljminecraft.overtone
  "Music and Minecraft."
  (:import [org.bukkit Location])
  (:use [overtone.core] [mud.core] [mud.timing])
  (:require [cljminecraft.bukkit :as bk]
            [cljminecraft.blocks :as b]
            [cljminecraft.world :as w]
            [cljminecraft.entity :as e]
            [cljminecraft.items :as i]))

;;(connect-external-server)

(do
  (do
    (def highhat (freesound 53532))
    (def dirty-kick (freesound 30669))
    (def ring-hat (freesound 12912))
    (def snare (freesound 193023);;(freesound 26903)
      )
    (def click (freesound 406))
    (def wop (freesound 85291))
    (def subby (freesound 25649))
    (defonce boom-s      (freesound-sample 33637)))

  (def player (first (.getOnlinePlayers (bk/server))))
  (def active-world (last (bk/worlds)))
  (def start-player-loc  (.getLocation player))
  (def ctx (b/setup-context player))

  (defn draw [m actions] (bk/ui-sync @cljminecraft.core/clj-plugin #(apply b/run-actions ctx (b/material m) actions)))

  (defn monster [x y z type]
    (let [start-player-loc (.getLocation player)
          l (Location. active-world
                       (.getX start-player-loc)
                       (.getY start-player-loc)
                       (.getZ start-player-loc))

          _ (.setX l (+ x (.getX l)))
          _ (.setY l (+ y (.getX l)))
          _ (.setZ l (+ z (.getX l)))]
      (bk/ui-sync @cljminecraft.core/clj-plugin #(e/spawn-entity l type))))

(defn bump-y [y-offset] (.setY (:origin ctx) (+ y-offset (.getY (:origin ctx)))))
(defn bump-x [x-offset] (.setX (:origin ctx) (+ x-offset (.getX (:origin ctx)))))
(defn bump-z [z-offset] (.setZ (:origin ctx) (+ z-offset (.getZ (:origin ctx)))))

(defn teleport [x y z]
  (let [l (.getLocation player) ]
    (doto l
      (.setX (+ x (.getX l)))
      (.setY (+ y (.getY l)))
      (.setZ (+ z (.getZ l))))
    (.teleport player l)))

(defn explode [x y z]
  (let [l (.getLocation player)]
    (doto l
      (.setX (+ x (.getX l)))
      (.setY (+ y (.getY l)))
      (.setZ (+ z (.getZ l))))
    (bk/ui-sync
     @cljminecraft.core/clj-plugin
     #(w/explode l 10 true))))

;;(explode 0 -0 0)

(defn blocks "relative to player"
  ([actions material] (blocks (map #(if (= 3 (count %1)) (concat %1 [material])) actions)) )
  ([actions]
     (bk/ui-sync
      @cljminecraft.core/clj-plugin
      (fn []
        (doseq [[x y z m] actions]
          (let [m (i/get-material m)
                l (.getLocation player)]
            (doto l
              (.setX (+ x (.getX l)))
              (.setY (+ y (.getY l)))
              (.setZ (+ z (.getZ l))))
            (doto (.getBlock l)
              (.setData 0)
              (.setType (.getItemType m))
              (.setData (.getData m)))))))))

(defn block "relative to player" [x y z material & [fixed]]
  (let [l (if fixed
            (Location. active-world
                       (.getX start-player-loc)
                       (.getY start-player-loc)
                       (.getZ start-player-loc))
            (.getLocation player))
        m (i/get-material material)]
    (doto l
      (.setX (+ x (.getX l)))
      (.setY (+ y (.getY l)))
      (.setZ (+ z (.getZ l))))
    (bk/ui-sync
     @cljminecraft.core/clj-plugin
     (fn []
       (doto (.getBlock l)
         (.setData 0)
         (.setType (.getItemType m))
         (.setData (.getData m)))))))

(def stairs (atom {:x -1 :y -1 :z 2}))

(defn add-step [thing]
  (block (:x @stairs) (:y @stairs) (:z   @stairs) thing true)
  (swap! stairs assoc :x (dec (:x @stairs)))
  (swap! stairs assoc :y (min 5 (inc (:y @stairs)))))

(defn pattern->cords [pattern y material]
  (mapcat
   (fn [[x line]]
     (map
      (fn [[z cell]]
        (case cell
          1 [x y z material]
          0 [x y z :air]))
      (map vector (range (- 0 (int (/ (count line) 2))) (- (count line) (int (/ (count line) 2)))) line)))
   (map vector (range (- 0 (int (/ (count (first pattern)) 2))) (count (first pattern))) pattern)))

(def letter-patterns
  {:C [[1 1 1 1 1]
       [1 0 0 0 0]
       [1 0 0 0 0]
       [1 0 0 0 0]
       [1 1 1 1 1]]

   :L [[1 0 0 0 0]
       [1 0 0 0 0]
       [1 0 0 0 0]
       [1 0 0 0 0]
       [1 1 1 1 1]]

   :J [[1 1 1 1 1]
       [0 0 1 0 0]
       [0 0 1 0 0]
       [0 0 1 0 0]
       [1 1 1 0 0]]

   :U [[1 0 0 0 1]
       [1 0 0 0 1]
       [1 0 0 0 1]
       [1 0 0 0 1]
       [1 1 1 1 1]]

   :O [[1 1 1 1 1]
       [1 0 0 0 1]
       [1 0 0 0 1]
       [1 0 0 0 1]
       [1 1 1 1 1]]

   :E [[1 1 1 1 1]
       [1 0 0 0 0]
       [1 1 1 1 0]
       [1 0 0 0 0]
       [1 1 1 1 1]]

   :V [[1 0 0 0 1]
       [1 0 0 0 1]
       [1 0 0 0 1]
       [0 1 0 1 0]
       [0 0 1 0 0]]

   :R [[1 1 1 1 1]
       [1 0 0 0 1]
       [1 1 1 1 1]
       [1 0 0 1 0]
       [1 0 0 0 1]]

   :T [[1 1 1 1 1]
       [0 0 1 0 0]
       [0 0 1 0 0]
       [0 0 1 0 0]
       [0 0 1 0 0]]

   :N [[1 0 0 0 1]
       [1 1 0 0 1]
       [1 0 1 0 1]
       [1 0 0 1 1]
       [1 0 0 0 1]]

   :I [[1 1 1 1 1]
       [0 0 1 0 0]
       [0 0 1 0 0]
       [0 0 1 0 0]
       [1 1 1 1 1]]

   :M [[1 0 0 0 1]
       [1 1 0 1 1]
       [1 0 1 0 1]
       [1 0 0 0 1]
       [1 0 0 0 1]]})

(defn letter [char y material]
  (blocks (pattern->cords (get letter-patterns char) y material)))

(defn word [word y material]
  (doseq [l (map keyword (drop 1 (clojure.string/split word #"")))]
    (letter l y material)))

(defn circle
  ([size thing] (circle size -1 thing))
  ([size y thing]
     (let [mid (int (/ size 2))
           neg-mid (- 0 mid)
           top    (map (fn [s] [mid  y  s])    (range neg-mid (- size mid)))
           bottom (map (fn [s] [neg-mid y  s]) (range neg-mid (- size mid)))
           left   (map (fn [s] [s  y  mid])    (range neg-mid (- size mid)))
           right  (map (fn [s] [s  y neg-mid]) (range neg-mid (- size mid)))
           cords (distinct (apply concat top bottom left right []))]
       (blocks cords thing)
       cords)))

(defn diamond
  [material]
  (blocks (pattern->cords [[0 1 0]
                           [1 0 1]
                           [0 1 0]] 3 material)))

(defn corners [material]
  (blocks (pattern->cords [[1 0 1]
                           [0 0 0]
                           [1 0 1]] 3 material)))


(def spiral-state {:x (atom 0)
                    :y (atom 3)
                    :z (atom 0)
                    :size (atom 10)
                    :dir (atom :forward)
                    :material :sand})

(reset! (:material spiral-state) :water)

(defn reset-spiral! []
  (reset! (:x spiral-state) 0)
  (reset! (:z spiral-state) 0)
  (reset! (:y spiral-state) 3)
  (reset! (:size spiral-state) 10)
  (reset! (:dir spiral-state) :forward))

(defn spiral-cords
  ([material] (spiral material #(swap! (:size spiral-state) inc) 1))
  ([material growth-fn] (spiral growth-fn 1))
  ([material growth-fn iterations]
     (loop [cords []]
       (if (= iterations (count cords))
         cords
         (let [{ x :x y :y z :z dir :dir size :size mat :material } spiral-state
               m @size
               offset (int (/ @size 2))]
           (when (and (= @x 0) (= 0 @z) (= @dir :forward)) (reset! dir :forward))
           (when (and (= @x m) (= 0 @z)) (reset! dir :right))
           (when (and (= @x m) (= m @z)) (reset! dir :back))
           (when (and (= @x 0) (= m @z)) (reset! dir :left))

           (when
               (and (= @x 0) (= 0 @z) (not= @dir :forward))
             (swap! y + 4)
             (growth-fn)
             (reset! dir :forward))

           (when (<= @size 1)
             (reset-spiral!)
             (if (= :sand @mat)
               (reset! mat material)
               (reset! mat :sand)))

           (case @dir
             :forward (swap! x inc)
             :back    (swap! x dec)
             :left    (swap! z dec)
             :right   (swap! z inc))
           (recur (concat cords [(- offset @x) @y (- offset @z) @material])))))))

(defn spiral [meterial growth-fn iterations]
  (blocks (spiral-cords material growth-fn iterations)))

(comment
  (reset-spiral!)
  (dotimes [i 100]
    (spiral :dirt)
    ))

(def triangle-state
  {:x (atom 0)
   :y (atom 3)
   :z (atom 0)
   :size (atom 10)
   :dir (atom :forward)
   :material :coal})

(def reset-triangle! []
  (reset! (:x triangle-state) 0)
  (reset! (:z triangle-state) 0)
  (reset! (:y triangle-state) 3)
  (reset! (:size triangle-state) 10)
  (reset! (:dir triangle-state) :forward))

(defn triangle-cords
  ([material]          (triangle material #(swap! (:size triangle-state) inc) 1))
  ([materil growth-fn] (triangle material growth-fn 1))
  ([material growth-fn iterations]
     (loop [cords []]
       (if (= iterations (count cords))
         cords

         (let [{ x :x y :y z :z dir :dir size :size mat :material} triangle-state
               m @size
               offset (int (/ @size 2))]

           (when (and (= @x 0) (= 0 @z) (= @dir :forward)) (reset! dir :forward))
           (when (and (= @x m) (= m @z)) (reset! dir :back))
           (when (and (= @x 0) (= m @z)) (reset! dir :left))

           (when
               (and (= @x 0) (= 0 @z) (not= @dir :forward))
             (swap! y + 4)
             (growth-fn)
             (reset! dir :forward))

           (when (<= @size 1)
             (reset-triangle!)
             (if (= :sand @mat)
               (reset! mat material)
               (reset! mat :sand)))

           (case @dir
             :forward (do (swap! x inc) (swap! z inc))
             :back    (swap! x dec)
             :left    (swap! z dec))
           (recur (concat [(- offset @x) @y (- offset @z) @mat])))))))

(defn triangle [material growth-fn iterations]
  (blocks (triangle-cords material growth-fn iterations)))

;;(dotimes [i 1] (bump-player))

(defn bump-player []
  (teleport 0 3 0 )
  (blocks [[0 -1 0]] :grass))

;;(dotimes [_ 100] (bump-player))

(defn set-time [t]
  (if-not (integer? t)
    (case t
      :sunset (.setTime active-world 12400)
      :night  (.setTime  active-world 21900)
      :day    (.setTime  active-world 0))
    (.setTime (first (bk/worlds)) t)))
)

(set-time :night)

(bk/broadcast "[:overtone :clojure :minecraft]")
(bk/broadcast "(do)")

(bump-player)

(one-time-beat-trigger 32 64 (fn [& _]
                                 (boom-s)
                                 (set-time :day)
                                 ))

(pattern->cords [[0 1 0]
                 [1 0 1]
                 [0 1 0]] 3 :dirt)

(block -1 -1 1 :dirt)
(circle 3 3 :sand)
(circle 5 3 :sand)
(circle 8 3 :sand)

(circle 8 -1 :sand)

(circle 3 :sand)
(diamond :air)
(diamond :sand)
(corners :sand)

(block 0 -1 0 :stone)

(doseq [i (range 10)] (add-step :dirt))

(comment
  (loop [x -1
         y -1]
    (block x y 0 :air)
    (when (> x -10)
      (recur (dec x) (inc y))))
  )

(set-time :day)

(blocks [[-5 -1 0]
         [-5 -1 1]
         [-6 -1 1]
         [-6 -1 0]] :dirt)

;;(.setY (:origin ctx) 70)

(ctl-global-clock 8.0)

(def cell-size (atom 1))
(def growth (atom 0))
(def material-bag (cycle [:sand :stone :grass :brick :wood :dirt :wool :pumpkin :skull :air :stationary_water :water :lava]))
(def instructions [(b/pen-up)
                   (b/up 3)
                   (b/forward 1)
                   (b/right 4)
                   (b/pen-down)
                   (b/back 1)
;;                   (b/up 2)
;;                   (b/forward 4)
;;                   (b/down  3)


;;                   (b/left (rand-int 10))
  ;;                 (b/right (rand-int 10))
    ;;               (b/up (rand-int 10))
      ;;             (b/right (rand-int 10))
        ;;           (b/down (rand-int 10))
                   ])

(reset! cell-size 20)

(def trigger-g77218
  (on-beat-trigger 16 #(do
                        (add-step (nth (cycle [:grass :dirt]) (* -1 (:x @stairs))) )
                        (boom-s)

                       )))

(remove-beat-trigger trigger-g77218)
(remove-all-beat-triggers)

(def sub-trigger
  (sample-trigger
   [1 0 0 0 0 0 0 0
    0 0 0 0 0 0 0 0
    1 0 1 0 0 0 0 0
    0 0 0 0 0 0 0 0]
   (fn []
     (def ctx (b/setup-context player))
     (swap! growth inc)
     (if (= (nth material-bag @growth) :water)
       (do       (sample-player subby :rate 0.5 :amp 1.0)
                 (sample-player wop :rate -0.8 :amp 1.0))
       (sample-player subby :rate 0.4)
       )
     (draw (nth material-bag @growth) instructions))))

(set-time :day)

(remove-beat-trigger sub-trigger)
(remove-all-beat-triggers)

(def instr2 [(b/pen-up)
             (b/up 5)
             (b/left 3)
             (b/forward 1)
             (b/pen-down)
             (b/forward (mod @growth 10))])
(def mat2 [:sand])

(def high-trigger
  (on-beat-trigger 32  #(do
                          (bump-player)
                          (click :rate 1.0)
                          (click :rate -0.5)
                          )))

(remove-beat-trigger high-trigger)
(remove-all-beat-triggers)

(def spir-trigger
  (do (reset-spiral!)
      (sample-trigger
       [0 0 0 0 0 0 0 0
        1 0 0 0 0 0 0 0]  #(do
                             (snare :rate 0.5)
                             (spiral :stone #(swap! @(:size spiral-state) dec) @(:size spiral-state))
                             ))))

(remove-beat-trigger spir-trigger)
(remove-all-beat-triggers)



(bump-player)


(def ring-trigger (on-beat-trigger
        32
        (fn []
          (ring-hat :amp 0.2)
          (circle (inc (mod @growth 20)) :sand)
;;          (draw (choose mat2) instr2)
          )))

(remove-beat-trigger ring-trigger)

(set-time :day)

(remove-all-sample-triggers)

(def sub2-trigger
  (on-beat-trigger
   (* 8)
   (fn []
     (highhat :rate 1.0)

     (monster 0 1 0 :pig)
;;     (bump-player)
     (block 5 7 0 :dirt)
     (blocks [[5 5 0]
              [5 4 0]
              [5 2 0]] :dirt)
     (draw :dirt [(b/pen-up) (b/forward 10) (b/pen-down) (b/up 10) (b/left 1) (b/forward 10) (b/left 1)]))))

(block 5 7 0 :air)

(bump-player)
(reset-spiral)
(reset! spiral-material :stone)
(set-time :day)
(remove-beat-trigger sub2-trigger)
(remove-all-beat-triggers)
(remove-all-sample-triggers)


(dotimes [i 50]
  (dotimes [x 50]
    (blocks [[i -1 x]] :grass)))

(block 1 -1 1 :stone)
(monster 1 1 1 :pig)
(stop)

(spiral :dirt)

(blocks [[3 1 0]] :brick)
