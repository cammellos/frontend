(ns frontend.instrumentation
  (:require [cljs-time.core :as time]
            [cljs-time.format :as time-format]
            [frontend.datetime :as datetime]
            [frontend.state :as state]
            [frontend.utils :as utils :include-macros true]
            [goog.dom]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn wrap-api-instrumentation [handler api-data]
  (fn [state]
    (let [state (handler state)]
      (try
        (if-not (and (:response-headers api-data) (= \/ (first (:url api-data))))
          state
          (let [{:keys [url method request-time response-headers]} api-data]
            (update-in state state/instrumentation-path conj {:url url
                                                              :route (get response-headers "X-Route")
                                                              :method method
                                                              :request-time request-time
                                                              :circle-latency (js/parseInt (get response-headers "X-Circleci-Latency"))
                                                              :query-count (js/parseInt (get response-headers "X-Circleci-Query-Count"))
                                                              :query-latency (js/parseInt (get response-headers "X-Circleci-Query-Latency"))})))
        (catch :default e
          (utils/merror e)
          state)))))
