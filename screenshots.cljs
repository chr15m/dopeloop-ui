#!/usr/bin/env npx nbb
(ns screenshots
  (:require ["playwright" :as pw]
            [promesa.core :as p]))

(defn wait-for-network-idle [page]
  (p/do
    (.waitForLoadState page "networkidle")
    (p/delay 100)))  ;; Add 100ms delay

(defn take-screenshot [browser-type url file-path]
  (p/let [browser (.launch browser-type)
          context (.newContext browser)
          page (.newPage context)]
    (.goto page url)
    (wait-for-network-idle page)
    ;(-> page (.locator "dope-split-button>label") .click)
    (.screenshot page (clj->js {:path file-path :fullPage true}))
    (print (str "wrote " file-path))
    (.close browser)))

(defn run [url]
  (print "Taking screenshots")
  (p/do (take-screenshot (.-chromium pw) url "chromium.png")
        (take-screenshot (.-firefox pw) url "firefox.png")
        (take-screenshot (.-webkit pw) url "webkit.png")))

(run "http://localhost:8000")
