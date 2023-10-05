(ns dopestyle.core
  (:require
    [shadow.resource :as rc]
    [reagent.core :as r]
    ["nexusui" :as nx]
    ["wavesurfer.js" :as ws]))

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

(defn ev-val [ev] (-> ev .-target .-value))

(def re-html-comment (js/RegExp. "<\\!--.*?-->" "g"))

(defn set-nx-colors! [app]
  (let [style (js/getComputedStyle app)] 
    ; set nexus ui colors
    (aset nx "colors" "accent" (.getPropertyValue style "--color-1"))
    (aset nx "colors" "fill" (.getPropertyValue style "--color-1-trans"))))

(defn icon [svg attrs]
  [:dope-icon (merge {:dangerouslySetInnerHTML
                      {:__html (.replace svg re-html-comment "")}}
                     attrs)])

(defn inline-img [svg attrs]
  [:dope-inline-svg (merge {:dangerouslySetInnerHTML
                            {:__html svg}}
                           attrs)])

(defn update-val! [state coords ev]
  (swap! state update-in coords (-> ev .-target .-value)))

(defn component-envelope []
  [:span.envelope.nxui
   {:ref
    (fn [el]
      (when el
        (let [env (nx/Envelope.
                    el (clj->js {:size [288 96]
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
                        #js {:size #js [72 72]
                             :value 0.5
                             :interaction "horizontal"})]
             (.on dial "change" (fn [v] (js/console.log "dial" v))))
          0)))}])

(defn mount-wavesurfer [el reference url]
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
                           :height 144
                           :dragToSeek true
                           :url url})]
      (.on ws "finish" #(swap! reference assoc :playing false))
      (swap! reference assoc :ws ws))
    (when (:ws @reference)
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

(defn component-waveform [state coords url]
  [:div.wave
   [:div.waveform
    {:ref #(mount-wavesurfer % (r/cursor state coords) url)}]
   [component-waveform-controls state coords]])

(defn component-slider [slider-name value min-val max-val & [props]]
  (let [midpoint (/ (+ min-val max-val) 2)]
    [:dope-slider 
     [:label (when (< value midpoint) {:class "right"})
      [:span slider-name]]
     [:input
      (merge
        {:type "range"
         :min min-val
         :max max-val
         ;:on-change #(update-val! state [k] %)
         ;:on-mouse-up #(update-loop! state)
         ;:on-touch-end #(update-loop! state)
         :value value}
        props)]]))

(defn component-header []
  [:header
   [:a {:href "https://dopeloop.ai"}
    [inline-img (rc/inline "style/img/logo.svg") {:class "logo"}]
    [:h2 "dopeloop.ai"]]
   ; TODO: make this a hamburger menu
   [:nav
    [:a {:href "https://dopeloop.ai/auth/sign-in"} "Sign in"]
    [:dope-menu
     [:input {:type "checkbox" :id "nav-menu-dropdown"}]
     [:label {:for "nav-menu-dropdown"}
      [icon (rc/inline "icons/tabler/menu.svg")]
      [icon (rc/inline "icons/tabler/x.svg")]]
     [:ul
      [:li [:a {:href "https://dopeloop.ai"} "Somelink"]]
      [:li [:a {:href "https://dopeloop.ai"} "Another link"]]]]]])

(defn component-footer []
  [:footer "Copyright "
    (year)
    " McCormick IT Pty Ltd."])
