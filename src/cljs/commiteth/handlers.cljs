(ns commiteth.handlers
  (:require [commiteth.db :as db]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-fx]]
            [ajax.core :refer [GET POST]]))

(reg-fx
  :http
  (fn [{:keys [method url on-success params]}]
    (method url
      {:headers {"Accept" "application/transit+json"}
       :handler on-success
       :params  params})))

(reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

(reg-event-db
  :assoc-in
  (fn [db [_ path value]]
    (assoc-in db path value)))

(reg-event-db
  :set-active-page
  (fn [db [_ page]]
    (assoc db :page page)))

(reg-event-fx
  :set-active-user
  (fn [{:keys [db]} [_ user]]
    {:db         (assoc db :user user)
     :dispatch-n [[:load-user-profile]
                  [:load-user-repos]
                  [:load-enabled-repos]
                  [:load-bounties]
                  [:load-issues]]}))

(reg-event-fx
  :load-bounties
  (fn [{:keys [db]} [_]]
    {:db   db
     :http {:method     GET
            :url        "/api/bounties"
            :on-success #(dispatch [:set-bounties %])}}))

(reg-event-db
  :set-bounties
  (fn [db [_ bounties]]
    (assoc db :bounties bounties)))

(reg-event-fx
  :load-issues
  (fn [{:keys [db]} [_]]
    {:db   db
     :http {:method     GET
            :url        "/api/issues"
            :on-success #(dispatch [:set-issues %])}}))

(reg-event-db
  :set-issues
  (fn [db [_ issues]]
    (assoc db :issues issues)))

(reg-event-fx
  :load-user-profile
  (fn [{:keys [db]} [_]]
    {:db   db
     :http {:method     GET
            :url        "/api/user"
            :on-success #(dispatch [:set-user-profile %])}}))

(reg-event-db
  :set-user-profile
  (fn [db [_ user-profile]]
    (assoc db :user-profile user-profile)))

(reg-event-db
  :set-user-repos
  (fn [db [_ repos]]
    (assoc db :repos repos)))

(reg-event-fx
  :load-user-repos
  (fn [{:keys [db]} [_]]
    {:db   db
     :http {:method     GET
            :url        "/api/user/repositories"
            :on-success #(dispatch [:set-user-repos (:repositories %)])}}))

(reg-event-db
  :set-enabled-repos
  (fn [db [_ repos]]
    (assoc db :enabled-repos (zipmap repos (repeat true)))))

(reg-event-fx
  :load-enabled-repos
  (fn [{:keys [db]} [_]]
    {:db   db
     :http {:method     GET
            :url        "/api/repositories"
            :on-success #(dispatch [:set-enabled-repos %])}}))

(reg-event-fx
  :toggle-repo
  (fn [{:keys [db]} [_ repo]]
    {:db   db
     :http {:method     POST
            :url        "/api/repository/toggle"
            :on-success #(println %)
            :params     (select-keys repo [:id :login :name])}}))

(reg-event-fx
  :save-user-address
  (fn [{:keys [db]} [_ user-id address]]
    {:db   db
     :http {:method     POST
            :url        "/api/user/address"
            :on-success #(println %)
            :params     {:user-id user-id :address address}}}))