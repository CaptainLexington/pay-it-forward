(ns pay-it-forward.popup.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-com.core :as re-com]))

(defn icon-by-status [status]
  (case status 
    :bronze "zmdi-dot-circle-alt"
    :silver "zmdi-dot-circle"
    :gold "zmdi-circle"
    "zmdi-alert-circle"))

(defn tooltip-by-status [status]
  (case status 
    :bronze "Bronze"
    :silver "Silver"
    :gold "Gold"
    "Error"))

(defn cycle-status [status]
  (case status 
    :bronze :silver
    :silver :gold
    :gold :bronze
    :bronze))

(defn affiliate-row [affiliate selected-affiliate]
  [re-com/h-box
   :children [[re-com/radio-button
               :model @selected-affiliate 
               :value (:id affiliate)
               :label [re-com/label 
                       :label (:name affiliate)
                       :width "150px"]
               :on-change #(re-frame/dispatch [:select-affiliate (:id affiliate)])]
              [re-com/md-icon-button
               :size :smaller
               :md-icon-name (icon-by-status (:status affiliate))
               :tooltip (tooltip-by-status (:status affiliate))
               :on-click #(re-frame/dispatch [:update-status (:id affiliate) (cycle-status (:status affiliate))]) ]
              [re-com/md-icon-button
               :size :smaller
               :md-icon-name "zmdi-edit"]
              [re-com/md-icon-button
               :size :smaller
               :md-icon-name "zmdi-delete"
               :on-click #(re-frame/dispatch [:delete-affiliate (:id affiliate)])]]])


(defn affiliate-list [affiliates]
  (let [selected-affiliate (re-frame/subscribe [:selected-affiliate])
        affiliates (re-frame/subscribe [:affiliates])]
    [re-com/v-box
     :children [[re-com/button
                 :label "Add an Affiliate"
                 :on-click #(re-frame/dispatch [:set-active-panel :add-affiliate])]
                (for [affiliate @affiliates]
                  [affiliate-row affiliate selected-affiliate])]])) 



(defn add-affiliate []
  (let [new-affiliate-id (reagent/atom "")
        new-affiliate-name (reagent/atom "")]
    [re-com/v-box
     :children [[re-com/label
                 :label "Affiliate ID or Link"]
                [re-com/input-text
                 :width "100px"
                 :model new-affiliate-id
                 :on-change #(reset! new-affiliate-id %)]
                [re-com/label
                 :label "Name"]
                [re-com/input-text
                 :model new-affiliate-name
                 :width "100px"
                 :on-change #(reset! new-affiliate-name %)
                 :placeholder "Pay It Forward"]
                [re-com/button
                 :label "Add Affiliate"
                 :on-click #(do
                              (re-frame/dispatch [:set-active-panel :index])      
                              (re-frame/dispatch [:add-affiliate @new-affiliate-id @new-affiliate-name])
                              (reset! new-affiliate-id "")
                              (reset! new-affiliate-name ""))]]]))


(defmulti panels identity)
(defmethod panels :index [] [affiliate-list])
(defmethod panels :add-affiliate [] [add-affiliate])
(defmethod panels :default [] [:div "There has been an error...an explosion!"])

(defn popup []
  (fn []
    (let [active-panel (re-frame/subscribe [:active-panel])]
      [re-com/v-box
       :height "350px"
       :width "300px"
       :class "container"
       :children [[:h3 "Pay It Forward"]
                  [panels @active-panel]]])))
