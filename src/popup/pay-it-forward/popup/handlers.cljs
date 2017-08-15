(ns pay-it-forward.popup.handlers
  (:require [re-frame.core :as re-frame]
            [akiroz.re-frame.storage :refer [reg-co-fx!]]
            [pay-it-forward.popup.db :as db]))

(reg-co-fx! :pay-it-forward 
            {:fx :store
             :cofx :store})

(re-frame/reg-event-fx
  :initialize-db
  [(re-frame/inject-cofx :store)]
  (fn [cofx event]
    (let [local  (:store cofx) 
          db (:db cofx)]
      {:db  (if (not (empty? (:affiliates local)))
              (assoc db/default-db :affiliates (:affiliates local))
              db/default-db)})))

(re-frame/reg-event-db
  :set-active-panel
  (fn [db [_ panel]]
    (assoc db
           :active-panel
           panel)))

(re-frame/reg-event-fx
  :add-affiliate
  [(re-frame/inject-cofx :store)]
  (fn [cofx [_ id name]]
    (let [db (:db cofx)
          new-db  (assoc-in db
                          [:affiliates
                           id ]
                          {:id id
                           :name name
                           :status :bronze})]
      {:db new-db 
       :store new-db})))

(re-frame/reg-event-fx
  :delete-affiliate
  [(re-frame/inject-cofx :store)]
  (fn [cofx [_ id]]
    (let [db (:db cofx)
          new-db (update db
                         :affiliates
                         dissoc
                         id)]
      {:db new-db
       :store new-db}))) 

(re-frame/reg-event-fx
  :update-status
  [(re-frame/inject-cofx :store)]
  (fn [cofx [_ id status]]
    (let [db (:db cofx)
          new-db (assoc-in db
                            [:affiliates
                             id
                             :status]
                            status)]
      {:db new-db
       :store new-db }))) 

(re-frame/reg-event-db
  :select-affiliate
  (fn [db [_ id]]
    (assoc db
           :selected-affiliate
           id)))
