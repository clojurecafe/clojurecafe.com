(ns ^:figwheel-always clojurecafe.core
  (:require
    [ajax.core :as ajax]
    [cljs.core.async :as async :refer [<! >! chan close! timeout]]
    [om.core :as om :include-macros true]
    [om.dom :as dom :include-macros true]
    [sablono.core :as html :refer-macros [html]]
    [clojure.string :as str]
    [clojurecafe.schema :as schema]
    [cljs-time.format :as format])
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

(defn pretty-date [tzstring]
  (format/unparse (format/formatter "hh:mma on dow, MMM d, yyyy")
   (format/parse (format/formatter "yyyy-MM-dd'T'HH:mm:ssZZ")
                 tzstring)))
(defn calendar-widget [{:keys [events]} ]
  "Microdata formatted html for an event"
  ;;FIXME: this should be more functions bashing on the structure.
  (om/component
   (html [:ul {:id "events"} 
          (for [event events]
            [:div {:id (:type event)
                   :item-type (str (:context event) "/" (str/capitalize (name (:type event))))
                   :item-prop ""}
             [:li
              {:item-prop "startDate"
               :content (:start-date event)}
              (pretty-date (:start-date event))]
             [:li {:item-prop "startDate"
                   :content (:end-date event)}
              (pretty-date (:end-date event))]
             [:li {:item-prop "name"} (:name event)]
             [:span {:item-prop "image"}
              [:img {:src (:image event) :style {:max-height "200px"}}]]
             (let [location (:location event)]
               [:ul {:item-prop "location"
                     :item-scope ""
                     :item-type (str (:context event) "/" (str/capitalize (name (:type location))))}
                [:li {:item-prop "eventStatus"}
                 (str "Status: " (:event-status location))]
                [:li {:itemp-prop "name"} (:name location)]
                [:li [:address {:item-prop "address"} (:address location)]]
                [:li
                 {:item-prop "geo"
                  :item-scope ""
                  :item-type "http://schema.org/GeoCoordinates"}
                 (str (:latitude  (:geo location)) ", " (:longitude (:geo location)))
                 [:meta {:item-prop "latitude"
                         :content (:latitude (:geo location))}]
                 [:meta {:item-prop "longitude"
                         :content (:longitude (:geo location))}]]
                [:li  {:item-prop "description"}
                 (:description location)]])]) ])))
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
