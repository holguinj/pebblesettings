(ns pebblesettings.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType])
    (:import goog.History))

(defn clj->json [x]
  (.stringify js/JSON (clj->js x)))

;; -------------------------
;; Views

(defn current-page []
  [:div [(session/get :current-page)]])

(defn submit-form [{:keys [stop-id bus]}]
  (let [payload {:stopID stop-id
                 :bus bus}
        query-str (-> payload clj->json js/encodeURIComponent)
        return-to (session/get :return-to "pebblejs://close#")
        url (str return-to query-str)]
    (println "redirecting to:" url)
    (set! (.-href js/location) url)
    false))

(defn form []
  (let [value (atom {:stop-id 4016
                     :bus 15})]
    (fn []
      [:div
       [:form
        "Stop ID: "
        [:input {:type "number"
                 :pattern "[0-9]"
                 :value (:stop-id @value)
                 :on-change #(swap! value update-in [:stop-id]
                                    (-> % .-target .-value))}]
        [:p
         "Bus: "
         [:input {:type "number"
                  :pattern "[0-9]"
                  :value (:bus @value)
                  :on-change #(swap! value update-in [:bus]
                                     (-> % .-target .-value))}]]
        [:p
         [:input {:type "submit"
                  :on-click #(submit-form @value)}]]]])))

(defn main-page []
  [:div
   [:p "Select your bus stop!"]
   [form]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (let [location (.-location js/document)
        return-to (some->> location
                    str
                    (re-find #"return_to=(.*)")
                    second
                    js/decodeURIComponent)]
    (if (some? return-to)
      (session/put! :return-to return-to))))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render [main-page] (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-root))
