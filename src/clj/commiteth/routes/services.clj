(ns commiteth.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [compojure.api.meta :refer [restructure-param]]
            [buddy.auth.accessrules :refer [restrict]]
            [buddy.auth :refer [authenticated?]]
            [commiteth.db.users :as users]
            [commiteth.db.repositories :as repositories]
            [commiteth.db.bounties :as bounties]
            [commiteth.github.core :as github]))

(defn access-error [_ _]
  (unauthorized {:error "unauthorized"}))

(defn wrap-restricted [handler rule]
  (restrict handler {:handler  rule
                     :on-error access-error}))

(defmethod restructure-param :auth-rules
  [_ rule acc]
  (update-in acc [:middleware] conj [wrap-restricted rule]))

(defmethod restructure-param :current-user
  [_ binding acc]
  (update-in acc [:letks] into [binding `(:identity ~'+compojure-api-request+)]))

(defapi service-routes
  {:swagger {:ui   "/swagger-ui"
             :spec "/swagger.json"
             :data {:info {:version     "0.1"
                           :title       "commitETH API"
                           :description "commitETH API"}}}}

  (context "/api" []
    (POST "/user/address" []
      :auth-rules authenticated?
      :body-params [user-id :- String, address :- String]
      :summary "Update user address"
      (let [result (users/update-user-address (Integer/parseInt user-id) address)]
        (if (= 1 result)
          (ok)
          (internal-server-error))))
    (GET "/user" []
      :auth-rules authenticated?
      :current-user user
      (ok {:user (users/get-user (:id user))}))
    (GET "/user/repositories" []
      :auth-rules authenticated?
      :current-user user
      (ok {:repositories (github/list-repos (:token user))}))
    (GET "/repositories" []
      :auth-rules authenticated?
      :current-user user
      (ok (repositories/get-enabled (:id user))))
    (GET "/bounties" []
      :auth-rules authenticated?
      :current-user user
      (ok (bounties/list-fixed-issues (:id user))))
    (GET "/issues" []
      :auth-rules authenticated?
      :current-user user
      (ok (bounties/list-not-fixed-issues (:id user))))
    (POST "/repository/toggle" {:keys [params]}
      :auth-rules authenticated?
      :current-user user
      (ok (let [{repo-id :id
                 repo    :name} params
                {token   :token
                 login   :login
                 user-id :id} user
                result (or
                         (repositories/create (merge params {:user_id user-id}))
                         (repositories/toggle repo-id))
                ]
            (if (:enabled result)
              ;; @todo: do we really want to make this call at this moment?
              (let [created-hook (github/add-webhook token login repo)]
                (println created-hook)
                (repositories/update-hook-id repo-id (:id created-hook)))
              (github/remove-webhook token login repo (:hook_id result)))
            result)))))