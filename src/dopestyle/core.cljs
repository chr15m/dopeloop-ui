(ns dopestyle.core
  (:require
    ["nexusui" :as nx]))

(defn year [] (-> (js/Date.) .getFullYear .toString))

(defn trigger-file-download [f]
  (let [link (.createElement js/document "a")
        body (aget js/document "body")]
    (aset link "download" (aget f "name"))
    (aset link "href" (.createObjectURL js/URL f))
    (.appendChild body link)
    (.click link)
    (.removeChild body link)
    f))

(defn button-notify [el]
  (let [cl (aget el "classList")
        rmfn (fn [] (.remove cl "notify"))]
    (if (.contains cl "notify")
      (rmfn)
      (.addEventListener el "transitionend" rmfn #js {:once true}))
    ; trigger CSS reflow
    ((fn [] (aget el "offsetHeight")))
    (.add cl "notify")))

(defn ev-val [ev] (-> ev .-target .-value))

(defn set-nx-colors! [app]
  (let [style (js/getComputedStyle app)] 
    ; set nexus ui colors
    (aset nx "colors" "accent" (.getPropertyValue style "--color-1"))
    (aset nx "colors" "fill" (.getPropertyValue style "--color-1-trans"))))

(defn download-file-link [file & callback]
  [:a {:href (.createObjectURL js/URL file)
       :download (aget file "name")
       :on-click #(when callback (callback file %))
       :on-contextmenu #(when callback (callback file %))}])

(defn update-val! [state coords ev]
  (swap! state update-in coords (-> ev .-target .-value)))

(defn hide-menu! []
  (->
    (.querySelector js/document "#nav-menu-dropdown")
    (aset "checked" false)))

(defn show-modal! [ev state component & [modal-data]]
  (when ev (.preventDefault ev))
  (swap! state assoc
         :modal {:component component
                 :data modal-data})
  (hide-menu!))

(defn handle-root-click!
  "Closes the nav menu on root click. Use:
  `(.addEventListener js/document \"click\" handle-root-click!)`"
  [ev]
  (when-let [menu-button (.querySelector js/document "#nav-menu-dropdown")]
    (let [menu-modal (.querySelector js/document "nav dope-menu")]
      (when (and (aget menu-button "checked")
                 (not (.contains menu-modal (aget ev "target"))))
        (hide-menu!)))))
