(ns dopestyle.core
  (:require
    [reagent.core :as r]
    [reagent.dom :as rdom]))

(defonce state (r/atom {}))

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

(defn component-main [_state]
  [:<>
   [:header
    [:h2 "Dopeloop"]
    [:nav
     [:a {:href "/auth/sign-in"} "Sign in"]]]
   [:main
    [:h1.fat {:title "Style guide"} "Style guide"]
    [:section.ui
     [:h2 "UI Styles"]
     [:div.row
      [:button {:data-notification-text "Longer notification. Yes!"
                :on-click #(button-notify (-> % .-target))}
       "Notify"]
      [:button {:data-notification-text "Notify!"
                :on-click #(button-notify (-> % .-target))}
       "Notify 2"]]
     [:div.row
      #_ [:button "One two"]
      [:button "Hello"]]
     [:div.row.right
      [:button "This"]]
     [:table.grid
      [:tbody
       (for [y (range 3)]
         [:tr {:key y}
          [:th "One"]
          (for [x (range 16)]
            [:td {:key x} [:div.tapper "x"]])])]]]
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
  (rdom/render [component-main state]
               (js/document.getElementById "app")))

(defn init []
  (start))
