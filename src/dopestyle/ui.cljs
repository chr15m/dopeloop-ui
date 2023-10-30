(ns dopestyle.ui
  (:require
    [reagent.core :as r]
    [shadow.resource :as rc]
    [reagent.dom :as rdom]
    [dopestyle.core :refer [component-header component-slider
                            component-envelope component-dial
                            component-waveform component-footer
                            set-nx-colors! ev-val button-notify icon]]))

(defonce state
  (r/atom
    {:grid  [{0 true 8 true 3 true}
             {4 true 12 true}
             {0 true 2 true 4 true 6 true 7 true
              8 false 10 true 12 true 13 true 14 true}]
     :grid-highlight 2}))

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

(defn component-main [state]
  [:<>
   [component-header]
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
       [:dope-group
        [:button.square
         [icon (rc/inline "icons/tabler/headphones-filled.svg")]]
        [:button.round
         [icon (rc/inline "icons/tabler/check.svg")]]]
       [:dope-group
        [:button.round.large
         [icon (rc/inline "icons/tabler/player-play-filled.svg")]]]]]
     [:h2 "Sliders"]
     [:dope-card
      [:dope-row
       (let [v (r/cursor state [:sliders :thing])]
         [component-slider "thing" @v 0 127
          {:on-change #(reset! v (ev-val %))}])
       (let [v (r/cursor state [:sliders :whatsit])]
         [component-slider "whatsit" @v 0 127
          {:on-change #(reset! v (ev-val %))}])]]
     [:h2 "Grid"]
     [:dope-card.alt
      [:dope-row.title "Drums"]
      [:dope-row [component-demo-grid state]]]
     [:h2 "Audio parameter widgets"]
     [:dope-card
      [:dope-row.center-bias
       [component-envelope]
       [:span
        (doall
          (for [i (range 4)]
            ^{:key i} [component-dial]))]]]
     [:dope-card.alt
      [:dope-row.center-bias
       [component-envelope]
       [:span
        (doall
          (for [i (range 4)]
            ^{:key i} [component-dial]))]]]
     [:h2 "Wave"]
     [:dope-card
      [component-waveform state [:waveform] (:wave-file @state)]]]
    ; TODO: BPM component
    [:section.typography
     [:h2 "Typography"]
     [:details
      [:summary "Unfold for more info."]
      (for [p (range 100)]
        [:p {:key p}
         (for [s (range (int (inc (* (js/Math.random) 10))))]
           [:span {:key s} "Ipsum lorem something. "])])]]]
   [component-footer]])

(defn start {:dev/after-load true} []
  (let [app (js/document.getElementById "app")]
    (set-nx-colors! app)
    (rdom/render [component-main state] app)))

(defn init []
  (let [updater
        (fn updater []
          (swap! state update-in [:grid-highlight] #(-> % inc (mod 16)))
          (js/setTimeout updater 250))]
    (updater))
  (-> (js/fetch "snd/ae.mp3")
      (.then #(.arrayBuffer %))
      (.then (fn [mp3-buffer]
               (swap! state assoc :wave-file
                      (js/File. #js [mp3-buffer]
                                #js {:content-type "audio/mp3"}))))
      (.then #(start))))
