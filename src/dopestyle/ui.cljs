(ns dopestyle.ui
  (:require
    [reagent.core :as r]
    [shadow.resource :as rc]
    [reagent.dom :as rdom]
    [dopestyle.core :refer [component-header component-modal
                            component-slider component-envelope
                            component-dial component-waveform
                            component-footer
                            set-nx-colors! ev-val button-notify icon
                            handle-root-click!]]))

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

(defn component-demo-action-bar [state]
  [:section.action-bar
   [:dope-row.right
    [:div "Something here"]
    [:dope-group
     [:button
      {:on-click #(js/alert "Download example")}
      [icon (rc/inline "icons/tabler/download.svg")]
      "Download"]
     [:button.round.large
      {:on-click #(js/alert "Play example")}
      [icon
       (if (-> @state :audio :playing)
         (rc/inline "icons/tabler/player-pause-filled.svg")
         (rc/inline "icons/tabler/player-play-filled.svg"))]]]]])

(defn component-buttons-demo []
  [:<>
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
       [icon (rc/inline "icons/tabler/player-play-filled.svg")]]]]]])

(defn component-inputs-demo []
  [:<>
   [:h2 "Inputs"]
   [:dope-card
    [:dope-row
     [:input {:placeholder "Hi"}]
     [:dope-group
      [:select
       [:option "First"]
       [:option "Second"]
       [:option "Third"]]
      [:button
       [icon (rc/inline "icons/tabler/trash.svg")]
       "Trash"]]]]
   [:h2 "Sliders"]
   [:dope-card
    [:dope-row
     (let [v (r/cursor state [:sliders :thing])]
       [component-slider "thing" @v 0 127
        {:on-change #(reset! v (ev-val %))}])
     (let [v (r/cursor state [:sliders :whatsit])]
       [component-slider "whatsit" @v 0 127
        {:on-change #(reset! v (ev-val %))}])]]])

(defn component-audio-widgets-demo []
  [:<>
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
    [component-waveform state [:waveform] (:wave-file @state)]]])

(defn component-main [state]
  (if (:modal @state)
    [component-modal state]
    [:<>
     [component-header state]
     [:main
      [:h1.fat {:title "Design reference"} "Design reference"]
      [:section.ui
       [component-buttons-demo]
       [component-inputs-demo]
       [component-audio-widgets-demo]]
      ; TODO: BPM component
      [:section.typography
       [:h2 "Typography"]
       [:details
        [:summary "Unfold for more info."]
        (for [p (range 100)]
          [:p {:key p}
           (for [s (range (int (inc (mod (+ (* p 33) 5381) 10))))]
             [:span {:key s} "Ipsum lorem something. "])])]]
      [component-demo-action-bar state]]
     [component-footer]]))

(defn start {:dev/after-load true} []
  (let [app (js/document.getElementById "app")]
    (set-nx-colors! app)
    (rdom/render [component-main state] app)))

(defn init []
  ; listen for clicks on document root
  (.addEventListener js/document "click" handle-root-click!)
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
