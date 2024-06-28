(ns dopestyle.util)

(def re-html-comment (js/RegExp. "<\\!--.*?-->" "g"))

(defn icon [svg attrs]
  [:dope-icon (merge {:dangerouslySetInnerHTML
                      {:__html (.replace svg re-html-comment "")}}
                     attrs)])

(defn inline-img [svg attrs]
  [:dope-inline-svg (merge {:dangerouslySetInnerHTML
                            {:__html svg}}
                           attrs)])
