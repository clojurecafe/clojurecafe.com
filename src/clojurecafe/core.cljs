(ns ^:figwheel-always clojurecafe.core
  (:require
    [ajax.core :as ajax]
    [cljs.core.async :as async :refer [<! >! chan close! timeout]]
    [om.core :as om :include-macros true]
    [om.dom :as dom :include-macros true]
    [sablono.core :as html :refer-macros [html]]
    [clojurecafe.schema :as schema])
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
  (swap! app-state assoc :events res)
  (swap! app-state assoc :events-validation (schema/validate-events res))
  (let [schema-errors (:events-validation @app-state)]
    (if schema-errors (log "Event data validation errors: " schema-errors))))

(defn error-handler [{:keys [status status-text]}]
  (log "Error: " status " " status-text))

(defn get-events [result-chan]
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

(get-events result-chan)

(defn calendar-widget [{:keys [events]} ]
  (om/component
   (html [:ul {:id "events"}
          (for [event events]
            [:ul {:id (:type event)}
             [:li (:start-date event)]
             [:li (:end-date event)]
             [:li (:name event)]
             [:img {:src (:image event)}]
             [:ul {:id "location"}
              (let [location (:location event)]
                [:li (:type location)]
                [:li (:event-status location)]
                [:li (:name location)]
                [:li (:address location)]
                [:li (:latitude  (:geo location))]
                [:li (:longitude (:geo location))]
                [:li (:description location)])]])] )))

(om/root calendar-widget
         ;;         (:events @app-state)
         app-state
         {:target js/document.body}
         )


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  (swap! app-state update-in [:__figwheel_counter] inc)
)
