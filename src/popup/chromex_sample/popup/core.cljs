(ns chromex-sample.popup.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [cljs.core.async :refer [<!]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols :refer [post-message!]]
            [chromex.ext.runtime :as runtime :refer-macros [connect]]))

; -- a message loop ---------------------------------------------------------------------------------------------------------

(defn process-message! [message]
  (log "POPUP: got message:" message))

(defn run-message-loop! [message-channel]
  (log "POPUP: starting message loop...")
  (go-loop []
    (when-some [message (<! message-channel)]
      (process-message! message)
      (recur))
    (log "POPUP: leaving message loop")))

(defn connect-to-background-page! []
  (let [background-port (runtime/connect)]
    (post-message! background-port "hello from POPUP!")
    (run-message-loop! background-port)))

; -- main entry point -------------------------------------------------------------------------------------------------------
(defn mount-root []
  (reagent/render [:p "Hello from RE-FRAME in the Main Panel!!"]
                  (.getElementById js/document "app")))

(defn init! []
  (log "POPUP: init")
  (re-frame/dispatch-sync [:initialize-db])
  (connect-to-background-page!)
  (mount-root))
