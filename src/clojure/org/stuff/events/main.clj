(ns org.stuff.events.main
  (:require [neko.activity :refer [defactivity set-content-view!]]
            [neko.debug :refer [*a]]
            [neko.ui :refer [config]]
            [neko.ui.adapters :refer [ref-adapter]]
            [clojure.string :refer [join]]
            [neko.find-view :refer [find-view]]
            [neko.threading :refer [on-ui]]
            [neko.notify :refer [toast]]
            [neko.intent :as intent])
  (:import android.widget.TextView
           (java.util Calendar)
           (android.app Activity)
           (android.app DatePickerDialog DatePickerDialog$OnDateSetListener)
           (android.app DialogFragment)))


(declare add-event)
(declare date-picker)

(defn show-picker [activity dp]
  (. dp show (. activity getFragmentManager) "datePicker"))

(def listing (atom (sorted-map)))

(defn format-events [events]
  (->> (map (fn [[location event]]
              (format "%s - %s\n" location event))
            events)
       (join "                      ")))

(defn format-listing [lst]
  (->> (map (fn [[date events]]
              (format "%s - %s" date (format-events events)))
            lst)
       join))

(defn launch-other-activity
  [activity]
  (.startActivity activity (intent/intent activity '.OtherActivity
                                          {})))

(defn main-layout [activity]
  [:linear-layout {:orientation :vertical}
   [:edit-text {:hint "Event Name",
                :id ::name}]
   [:edit-text {:hint "Event Location",
                :id ::location}]
   [:linear-layout {:orientation :horizontal}
    [:text-view {:hint "Event date",
                 :id ::date}]
    [:button {:text "...",
              :on-click (fn [_] (show-picker activity
                                            (date-picker activity)))}]]
   [:button {:text "+ Event",
             :on-click (fn [_] (add-event activity))}]
   [:button {:text "go to other view"
             :on-click (fn [_] (launch-other-activity activity))
             }]
   [:text-view {:text (format-listing @listing)
                :id ::listing}]])
;;TO-DO FINISH THIS ADAPTER
#_(defn make-adapter []
  (ref-adapter (fn [_] [:linear-layout {:id-holder true}
                       ])))
(defn another-layout [activity]
  [:linear-layout {:orientation :vertical}
   [:text-view {:text "Oh Hai"}]
   [:text-view {:text (format-listing @listing) :id ::other-listing}]])

(defn get-elmt [activity elmt]
  (str (.getText ^TextView (find-view activity elmt))))

(defn set-elmt [activity elmt s]
  (on-ui (config (find-view activity elmt) :text s)))

(defn update-ui [activity]
  (toast "clearing inputs...")
    (set-elmt activity ::listing (format-listing @listing))
  (set-elmt activity ::location "")
  (set-elmt activity ::name ""))

(defn add-event [activity]
  (let [date-key (try
                   (read-string (get-elmt activity ::date))
                   (catch RuntimeException e "Date string is empty!"))]
    (when (number? date-key)
      (swap! listing update-in [date-key] (fnil conj [])
             [(get-elmt activity ::location) (get-elmt activity ::name)])
      (update-ui activity))))

(defn date-picker [activity]
  (proxy [DialogFragment DatePickerDialog$OnDateSetListener] []
    (onCreateDialog [savedInstanceState]
      (let [c (Calendar/getInstance)
            year (.get c Calendar/YEAR)
            month (.get c Calendar/MONTH)
            day (.get c Calendar/DAY_OF_MONTH)]
        (DatePickerDialog. activity this year month day)))
    (onDateSet [view year month day]
      (set-elmt activity ::date
                (format "%d%02d%02d" year (inc month) day)))))

(defactivity org.stuff.events.MainActivity
  :key :main

  (onCreate [this bundle]
    (.superOnCreate this bundle)
    (on-ui
      (set-content-view! (*a) (main-layout (*a))))
    ))

(defactivity org.stuff.events.OtherActivity
  :key :other
  (onCreate [this bundle]
            (.superOnCreate this bundle)
            (on-ui
              (set-content-view! (*a) (another-layout (*a))))))
