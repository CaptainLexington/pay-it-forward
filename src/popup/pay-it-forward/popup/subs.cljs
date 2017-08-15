(ns pay-it-forward.popup.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
  :selected-affiliate
  (fn [db]
    (:selected-affiliate db)))

(re-frame/reg-sub
  :affiliates
  (fn [db]
    (vals (:affiliates db))))

(re-frame/reg-sub
  :active-panel
  (fn [db]
    (:active-panel db)))
