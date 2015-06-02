(ns ^{:doc "Schemas and helpers."
      :figwheel-always true}
  clojure-cafe.schema
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
     ; FIXME: Didn't even bother to copy/paste some crappy regex.
     (let [regex #".*"]
       (some? (re-matches regex s)))))

(def ISO-Date-Time (s/pred iso-date-time? 'ISO-Date-Time))

(def Url (s/both s/Str (s/pred url? 'Url)))

(def Event
  "Schema for a meetup event."
  {:start ISO-Date-Time
   :end ISO-Date-Time
   :name s/Str
   :image Url
   :description s/Str
   :location {:confirmed? s/Bool
              :name s/Str
              :address s/Str
              :coordinates [(s/one s/Num "Latititude")
                            (s/one s/Num "Longitude")]
              :description s/Str}})

(def Events [Event])

(defn validate-event [event] (s/validate Event event))

(defn validate-events [events] (s/validate Events events))
