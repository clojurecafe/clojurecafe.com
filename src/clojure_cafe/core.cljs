(ns ^:figwheel-always clojure-cafe.core
  (:require
    [om.core :as om :include-macros true]
    [om.dom :as dom :include-macros true]
    [clojure-cafe.schema :as schema]))

(enable-console-print!)

(defonce app-state (atom {:text "Hello world!"}))

(om/root
  (fn [data owner]
    (reify om/IRender
      (render [_]
        (dom/h1 nil (:text data)))))
  app-state
  {:target (. js/document (getElementById "app"))})


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
