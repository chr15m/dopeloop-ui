(ns dopestyle.core
  (:require
    [reagent.core :as r]
    [reagent.dom :as rdom]))

(defonce state (r/atom {}))

(defn year [] (-> (js/Date.) .getFullYear .toString))

(defn component-main [_state]
  [:<>
   [:header
    [:h2 "Dopeloop"]
    [:nav
     [:a {:href "/auth/sign-in"} "Sign in"]]]
   [:main
    [:h2 "Style guide"]
    [:div.row
     [:button "Hello"]]
    [:details
     [:summary "Unfold for more info."]
     (for [_ (range 500)]
       [:span "Ipsum lorem something. "])]]
   [:footer "Copyright "
    (year)
    " McCormick IT Pty Ltd."]])

(defn start {:dev/after-load true} []
  (rdom/render [component-main state]
               (js/document.getElementById "app")))

(defn init []
  (start))
