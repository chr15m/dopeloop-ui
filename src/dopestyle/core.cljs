(ns dopestyle.core
  (:require
    [shadow.resource :as rc]
    [reagent.core :as r]
    [reagent.dom :as rdom]
    ["nexusui" :as nx]
    ["wavesurfer.js" :as ws]))

(defonce state
  (r/atom
    {:grid  [{0 true 8 true 3 true}
             {4 true 12 true}
             {0 true 2 true 4 true 6 true 7 true
              8 false 10 true 12 true 13 true 14 true}]
     :grid-highlight 2}))

(defn year [] (-> (js/Date.) .getFullYear .toString))

(defn button-notify [el]
  (let [cl (aget el "classList")
        rmfn (fn [] (js/console.log "notify remomve") (.remove cl "notify"))]
    (if (.contains cl "notify")
      (rmfn)
      (.addEventListener el "transitionend" rmfn #js {:once true}))
    ; trigger CSS reflow
    ((fn [] (aget el "offsetHeight")))
    (.add cl "notify")))

(def re-html-comment (js/RegExp. "<\\!--.*?-->" "g"))

(defn icon [svg]
  [:dope-icon {:dangerouslySetInnerHTML
               {:__html (.replace svg re-html-comment "")}}])

(defn inline-img [svg attrs]
  [:dope-inline-svg (merge {:dangerouslySetInnerHTML
                            {:__html svg}}
                           attrs)])

(defn component-demo-grid [state]
  [:table.grid
   [:tbody
    (doall
      (for [y (range 3)]
        [:tr {:key y}
         [:th (get {0 "Bass" 1 "Snare" 2 "Hat"} y)]
         (doall
           (for [x (range 16)]
             [:td {:key x}
              [:button.multistate
               {:on-click #(swap! state update-in [:grid y x] not)
                :class [(when (get-in @state [:grid y x]) "on")
                        (when (= (get-in @state [:grid-highlight]) x)
                          "highlight")]}]]))]))]])

(defn component-envelope []
  [:span.envelope.nxui
   {:ref
    (fn [el]
      (when el
        (let [env (nx/Envelope.
                    el (clj->js {:size [300 100]
                                 :points [{:x 0.1 :y 0.4}
                                          {:x 0.5 :y 0.1}]}))]
          (.on env "change" (fn [v] (js/console.log "envelope" v))))))}])

(defn component-dial []
  [:span.dial.nxui
   {:ref
    (fn [el]
      (when el
        (js/setTimeout
          #(let [dial (nx/Dial.
                        el
                        #js {:value 0.5
                             :interaction "horizontal"})]
             (.on dial "change" (fn [v] (js/console.log "dial" v))))
          0)))}])

(defn mount-wavesurfer [el reference]
  (js/console.log "render wavesurfer" el)
  (if el
    (let [style (js/getComputedStyle el)
          ws (.create ws
                      #js {:container el
                           :waveColor
                           (.getPropertyValue style "--color-1")
                           :progressColor
                           (.getPropertyValue style "--color-2")
                           :barWidth 3
                           :barGap 1
                           :barRadius 3
                           :cursorWidth 3
                           :dragToSeek true
                           :url "snd/ae.mp3"})]
      (.on ws "finish" #(swap! reference assoc :playing false))
      (swap! reference assoc :ws ws))
    (when (:ws @reference)
      (js/console.log "destroy ws")
      (.destroy (:ws @reference)))))

(defn component-waveform-controls [state coords]
  (let [play-coords (conj coords :playing)
        playing (get-in @state play-coords)]
    [:dope-row.right
     [:button.round
      {:on-click (fn []
                   (swap! state update-in play-coords
                          (fn [playing]
                            (let [player (get-in @state
                                                 (conj coords :ws))
                                  playing-updated (not playing)]
                              (when player
                                (if playing
                                  (.pause player)
                                  (.play player)))
                              playing-updated))))}
      [icon (if playing
              (rc/inline "icons/tabler/player-pause-filled.svg")
              (rc/inline "icons/tabler/player-play-filled.svg"))]]]))

(defn component-waveform [state coords]
  [:div.wave
   [:div.waveform
    {:ref #(mount-wavesurfer % (r/cursor state coords))}]
   [component-waveform-controls state coords]])

(defn component-main [state]
  [:<>
   [:header
    [:a {:href "https://dopeloop.ai"}
     [inline-img (rc/inline "img/favicon.svg") {:class "logo"}]
     [:h2 "dopeloop.ai"]]
    [:nav
     [:a {:href "https://dopeloop.ai"} "Somelink"]
     [:a {:href "https://dopeloop.ai/auth/sign-in"} "Sign in"]]]
   [:main
    [:h1.fat {:title "Design reference"} "Design reference"]
    [:section.ui
     [:h2 "Buttons"]
     [:dope-card
      [:dope-row
       [:button "First"]
       [:button "One two"]
       [:button "Hello"]]]
     [:dope-card.alt
      [:dope-row.right
       [:button "This"]]]
     [:dope-card
      [:dope-row
       [:button {:data-notification-text "Longer notification. Yes!"
                 :on-click #(button-notify (-> % .-target))}
        "Notify"]
       [:button {:data-notification-text "Notify!"
                 :on-click #(button-notify (-> % .-target))}
        "Notify 2"]]]
     [:dope-card.alt
      [:dope-row
       [:dope-row.left
        [:button.square
         [icon (rc/inline "icons/tabler/headphones-filled.svg")]]
        [:button.round
         [icon (rc/inline "icons/tabler/check.svg")]]]
       [:button.round.large
        [icon (rc/inline "icons/tabler/player-play-filled.svg")]]]]
     [:h2 "Grid"]
     [:dope-card.alt
      [:dope-row.title "Drums"]
      [:dope-row [component-demo-grid state]]]
     [:h2 "Audio parameter widgets"]
     [:dope-card
      [:dope-row
       [component-envelope]
       [:span
        (doall
          (for [i (range 4)]
            ^{:key i} [component-dial]))]]]
     [:dope-card.alt
      [:dope-row
       [component-envelope]
       [:span
        (doall
          (for [i (range 4)]
            ^{:key i} [component-dial]))]]]
     [:h2 "Wave"]
     [:dope-card
      [component-waveform state [:waveform]]]]
    ; TODO: sliders
    ; TODO: BPM component
    [:section.typography
     [:h2 "Typography"]
     [:details
      [:summary "Unfold for more info."]
      (for [p (range 100)]
        [:p {:key p}
         (for [s (range (int (inc (* (js/Math.random) 10))))]
           [:span {:key s} "Ipsum lorem something. "])])]]]
   [:footer "Copyright "
    (year)
    " McCormick IT Pty Ltd."]])

(defn start {:dev/after-load true} []
  (let [app (js/document.getElementById "app")
        style (js/getComputedStyle app)]
    ; set nexus ui colors
    (aset nx "colors" "accent" (.getPropertyValue style "--color-1"))
    (aset nx "colors" "fill" (.getPropertyValue style "--color-1-trans"))
    (rdom/render [component-main state] app)))

(defn init []
  (let [updater
        (fn updater []
          (swap! state update-in [:grid-highlight] #(-> % inc (mod 16)))
          (js/setTimeout updater 250))]
    (updater))
  (start))