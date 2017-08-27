(ns pay-it-forward.background.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [goog.string :as gstring]
            [goog.string.format]
            [cljs.core.async :refer [<! chan]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.chrome-event-channel :refer [make-chrome-event-channel]]
            [chromex.protocols :refer [post-message! get-sender]]
            [chromex.ext.tabs :as tabs]
            [chromex.ext.runtime :as runtime]
            [chromex.ext.web-navigation :as nav]
            [cemerick.url :as url]
            [pay-it-forward.background.storage :refer [test-storage!]]))

(def clients (atom []))

; -- clients manipulation ---------------------------------------------------------------------------------------------------

(defn add-client! [client]
  (log "BACKGROUND: client connected" (get-sender client))
  (swap! clients conj client))

(defn remove-client! [client]
  (log "BACKGROUND: client disconnected" (get-sender client))
  (let [remove-item (fn [coll item] (remove #(identical? item %) coll))]
    (swap! clients remove-item client)))

; -- client event loop ------------------------------------------------------------------------------------------------------

(defn run-client-message-loop! [client]
  (log "BACKGROUND: starting event loop for client:" (get-sender client))
  (go-loop []
    (when-some [message (<! client)]
      (log "BACKGROUND: got client message:" message "from" (get-sender client))
      (recur))
    (log "BACKGROUND: leaving event loop for client:" (get-sender client))
    (remove-client! client)))

; -- event handlers ---------------------------------------------------------------------------------------------------------

(defn handle-client-connection! [client]
  (add-client! client)
  (post-message! client "hello from BACKGROUND PAGE!")
  (run-client-message-loop! client))

(defn tell-clients-about-new-tab! []
  (doseq [client @clients]
    (post-message! client "a new tab was created")))

; -- main event loop --------------------------------------------------------------------------------------------------------

(defn chance [odds consequent alternative]
  (let [numerator (first odds)
        denominator (last odds)]
    (if (< (rand)
           (/ numerator consequent))
      consequent
      alternative)))

(defn choose [affiliates]
  (rand-nth (reduce #(concat %1 (repeat (last %2) (first %2)))
                    []
                    affiliates)))
  
(defn random-affiliate [affiliates]
  (if (nil? (get affiliates "payitforw20")
            (choose affiliates)
            (chance [1 :in 10]
                    "payitforw20"
                   (choose affiliates)))))

(defn final-url [amazon affiliate]
  (str
    (assoc amazon 
           :query {:tag affiliate})))

(defn process-chrome-event [event-num event]
  (let [[event-id event-args] event]
    (case event-id
      ::runtime/on-connect (apply handle-client-connection! event-args)
      ::tabs/on-created (tell-clients-about-new-tab!)
      ::nav/on-before-navigate (let [options (js->clj (first event-args))
                                     tab-id (get options "tabId")
                                     url (url/url (get options "url"))
                                     affiliate "basicinstr20"]
                                 (when (nil? (get (:query url) "tag"))
                                   (log (clj->js event))
                                   (tabs/update
                                     tab-id 
                                     (clj->js {"url" (final-url url affiliate)}))))
      nil)))

(defn run-chrome-event-loop! [chrome-event-channel]
  (log "BACKGROUND: starting main event loop...")
  (go-loop [event-num 1]
    (when-some [event (<! chrome-event-channel)]
      (process-chrome-event event-num event)
      (recur (inc event-num)))
    (log "BACKGROUND: leaving main event loop")))

(defn boot-chrome-event-loop! []
  (let [chrome-event-channel (make-chrome-event-channel (chan))]
    (tabs/tap-all-events chrome-event-channel)
    (runtime/tap-all-events chrome-event-channel)
    (nav/tap-on-before-navigate-events chrome-event-channel (clj->js {"url" [{"hostSuffix" ".amazon.com"}]}))
    (run-chrome-event-loop! chrome-event-channel)))

; -- main entry point -------------------------------------------------------------------------------------------------------

(defn init! []
  (log "BACKGROUND: init")
  (test-storage!)
  (boot-chrome-event-loop!))
