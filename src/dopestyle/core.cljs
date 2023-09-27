(ns dopestyle.core
  (:require
    [reagent.core :as r]
    [reagent.dom :as rdom]
    ["nexusui" :as nx]
    [shadow.resource :as rc]))

(defonce state (r/atom {:grid-highlight 2}))

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
               {:on-click #(swap! state update-in [:grid x y] not)
                :class [(when (get-in @state [:grid x y]) "on")
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

(defn component-main [state]
  [:<>
   [:header
    [:h2 "Dopeloop"]
    [:nav
     [:a {:href "/auth/sign-in"} "Sign in"]]]
   [:main
    [:h1.fat {:title "Style guide"} "Style guide"]
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
     [:h2 "Audio widgets"]
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
            ^{:key i} [component-dial]))]]]]
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
