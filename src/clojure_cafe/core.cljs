(ns ^:figwheel-always clojure-cafe.core
  (:require
    [ajax.core :as ajax]
    [cljs.core.async :as async :refer [<! >! chan close! timeout]]
    [om.core :as om :include-macros true]
    [om.dom :as dom :include-macros true]
    [clojure-cafe.schema :as schema])
  (:require-macros
    [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

(defonce app-state (atom {:text "Hello world!"}))

(defn log [& s]
  (.log js/console (apply str s)))

(def result-chan
  "Channel for fetching events."
  (chan))

(defn success-handler [res]
  (log "Got event data: " res)
  (swap! app-state assoc :events res))

(defn error-handler [{:keys [status status-text]}]
  (log "Error: " status " " status-text))

(defn load-events [result-chan]
  (ajax/GET
    "/data/events.edn"
    {:handler (fn [res] (go (>! result-chan {:success? true :data res})))
     :error-handler (fn [res] (go (>! result-chan {:success? false :data res})))
     :response-format :edn}))

;; Init!
(go
  (while true
    (let [result (<! result-chan)
          {:keys [success? data]} result]
      (if success? (success-handler data) (error-handler data)))))

(load-events result-chan)

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
