(ns ^{:doc "Schemas and helpers."
      :figwheel-always true}
  clojurecafe.schema
  #?(:cljs
     (:require [schema.core :as s :include-macros true]))
  #?@(:clj [
      (:require [schema.core :as s])
      (:import [java.net URL])]))

(defn iso-date-time? [s]
  ; FIXME: Validate with anything better than this terrible regex
  (let [regex #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\+\d{2}:\d{2}"]
   (some? (re-matches regex s))))

;; url?
#?(:clj
   (defn url? [^String s]
     (try
       (URL. s)
       true
       (catch java.net.MalformedURLException e
         false)))

   :cljs
   (defn- url? [s]
     ;; FIXME: this is a tough one. copy-paste http://regexr.com/37i6s
     ;; is crazy
     (let [regex #"http[s]?:[//].*"]
       (some? (re-matches regex s)))))

(def ISO-Date-Time (s/pred iso-date-time? 'ISO-Date-Time))

(def Url (s/both s/Str (s/pred url? 'Url)))

;; http://schema.org/Event
;; https://developers.google.com/structured-data/rich-snippets/events
;; Attributes map to google and schema.org rich snippets.
(def Event 
  "Schema for a meetup event."
  {:context  Url
   :type (s/enum :event)
   :startDate ISO-Date-Time
   :endDate ISO-Date-Time
   :name s/Str
   :image Url
   :description s/Str
   :location {:type (s/enum :place :postal-address)
              :eventStatus (s/enum :confirmed :pending :suggestion)
              :name s/Str 
              :address s/Str
              (s/optional-key :geo) {:type (s/enum :geo-coordinates :geo-shape) ;;http://schema.org/GeoCoordinates
                      :latitude s/Num
                      :longitude s/Num}  
              :description s/Str}})  

(def Events [Event])

(defn validate-event! [event] (s/validate Event event))
(defn validate-event [event] (s/check Event event))
(defn validate-events! [events] (s/validate Events events))
(defn validate-events [events] (s/check Events events))