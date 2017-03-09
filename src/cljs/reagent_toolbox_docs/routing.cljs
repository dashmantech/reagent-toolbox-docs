;;;; Copyright © 2017 Flexpoint Tech Ltd

(ns reagent-toolbox-docs.routing
  (:require [reagent.ratom :as ratom :include-macros true]
            [re-frame.core :as re-frame]
            [clojure.set :refer [rename-keys]]
            [domkm.silk :as silk]
            [pushy.core :as pushy]))

(def routes (silk/routes [[:home [[]]]
                          [:app-bar [["app-bar"]]]
                          [:autocomplete [["autocomplete"]]]
                          [:avatar [["avatar"]]]
                          [:button [["button"]]]
                          [:checkbox [["checkbox"]]]
                          [:drawer [["drawer"]]]
                          [:dropdown [["dropdown"]]]
                          [:font-icon [["font-icon"]]]
                          [:input [["input"]]]
                          [:link [["link"]]]
                          [:navigation [["navigation"]]]
                          [:progress-bar [["progress-bar"]]]
                          [:reagent-switch [["reagent-switch"]]]
                          [:radio-buttons [["radio-buttons"]]]
                          [:slider [["slider"]]]
                          [:snackbar [["snackbar"]]]
                          [:tabs [["tabs"]]]
                          [:time-picker [["time-picker"]]]]))

(defn- sanitize-silk-keywords [matched-route]
  (rename-keys matched-route {:domkm.silk/name    :name
                              :domkm.silk/pattern :pattern
                              :domkm.silk/routes  :routes
                              :domkm.silk/url     :url}))

(defn- parse-path [path]
  (sanitize-silk-keywords (silk/arrive routes path)))

(defn- routing-event [matched-route]
  [:set-current-route matched-route])

(defn- dispatch-route [parsed-path]
  (re-frame/dispatch (routing-event parsed-path)))

(def history (atom nil))

(defn- start! []
  (when (nil? @history)
    (reset! history (pushy/pushy dispatch-route parse-path)))
  (pushy/start! @history))

(def url-for (partial silk/depart routes))

(defn- redirect-to [& args]
  (when @history
    (let [path (apply url-for args)
          self-redirect (= path (pushy/get-token @history))]
      (pushy/set-token! @history path)
      (when self-redirect                                   ; If we are re-directing to itself, we need to re-trigger routing manually.
        (when-let [parsed-path (parse-path path)]
          (dispatch-route parsed-path))))))

(re-frame/reg-event-db :redirect-to
  (fn [db [& args]]
    (apply redirect-to (drop 1 args))
    db))

(defmulti display-page :name)
(defmethod display-page :default [_current-route db]
  db)

(re-frame/reg-event-db :set-current-route
  (fn [db [_name current-route]]
    (display-page current-route (assoc db :current-route current-route))))

(re-frame/reg-sub-raw :current-route
  (fn [db _]
    (ratom/reaction (:current-route @db))))
