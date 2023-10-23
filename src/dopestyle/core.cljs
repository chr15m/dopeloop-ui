(ns dopestyle.core
  (:require
    [shadow.resource :as rc]
    [reagent.core :as r]
    ["nexusui" :as nx]
    ["wavesurfer.js" :as ws]))

(defn year [] (-> (js/Date.) .getFullYear .toString))

(defn trigger-file-download [f]
  (let [link (.createElement js/document "a")
        body (aget js/document "body")]
    (aset link "download" (aget f "name"))
    (aset link "href" (.createObjectURL js/URL f))
    (.appendChild body link)
    (.click link)
    (.removeChild body link)
    (js/console.log link)
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

(def tempo-range [30 210])

(defn clamp-tempo [t]
  (-> t int (min (second tempo-range)) (max (first tempo-range))))

(defn set-tempo [state lookup new-tempo]
  (let [_old-tempo (:tempo @state)]
    (swap! state assoc-in lookup new-tempo)
    #_ (when (not= old-tempo new-tempo)
         (set-tempo-throttled state))
    new-tempo))

; *** components *** ;

(defn component-tempo [state lookup]
  (let [min-tempo (first tempo-range)
        max-tempo (second tempo-range)
        tmp-tempo (r/atom (or (get-in @state lookup) 120))
        update-fn #(let [v (-> @tmp-tempo clamp-tempo)]
                     (reset! tmp-tempo (set-tempo state lookup v)))
        update-up-fn
        #(reset! tmp-tempo (set-tempo state lookup (+ @tmp-tempo 10)))
        update-dn-fn
        #(reset! tmp-tempo (set-tempo state lookup (- @tmp-tempo 10)))]
    (fn []
      [:dope-tempo {:data-tempo (get-in @state lookup)}
        [:button.round
         {:on-click update-dn-fn
          :class (when (< @tmp-tempo (+ min-tempo 10)) "disabled")
          :title "Reduce BPM by 10"}
         [icon (rc/inline "icons/tabler/minus.svg")]]
        [:input {:value @tmp-tempo
                 :on-change #(reset! tmp-tempo (-> % .-target .-value))
                 :on-key-down #(when (= (aget % "key") "Enter")
                                 (-> % .-target .blur))
                 :type "number"
                 :name "tempo"
                 :alt "BPM"
                 :on-blur update-fn
                 :on-mouse-up update-fn
                 :title "Tempo (BPM)"}]
        [:label {:for "tempo" :style {:display "none"}} "Tempo (BPM)"]
        [:button.round
         {:on-click update-up-fn
          :class (when (> @tmp-tempo (- max-tempo 10)) "disabled")
          :title "Increase BPM by 10"}
         [icon (rc/inline "icons/tabler/plus.svg")]]])))

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

(defn mount-wavesurfer [el reference wav-file]
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
                           :dragToSeek true})]
      (.load ws (js/URL.createObjectURL wav-file))
      (.on ws "finish" #(swap! reference assoc :playing false))
      (swap! reference assoc :ws ws))
    (when (:ws @reference)
      (.destroy (:ws @reference)))))

(defn component-waveform-play [state coords]
  (let [play-coords (conj coords :playing)
        playing (get-in @state play-coords)]
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
              (rc/inline "icons/tabler/player-play-filled.svg"))]]))

(defn component-waveform [state coords wav-file uid & [component-tools]]
  [:div.wave {:key uid}
   [:div.waveform
    {:ref #(mount-wavesurfer % (r/cursor state coords) wav-file)}]
   [:dope-row.right.controls
    (if component-tools
      component-tools
      [component-waveform-play state coords])]])

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
