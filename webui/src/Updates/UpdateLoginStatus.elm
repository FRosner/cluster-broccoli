module Updates.UpdateLoginStatus exposing (updateLoginStatus)

import Debug
import Http exposing (Error(..))
import Messages exposing (AnyMsg(..))
import Model exposing (Model)
import Models.Ui.LoginForm as LoginForm exposing (LoginForm)
import Routing
import Updates.Messages exposing (UpdateErrorsMsg(..), UpdateLoginFormMsg(..), UpdateLoginStatusMsg(..))
import Utils.CmdUtils as CmdUtils
import Ws


updateLoginStatus : UpdateLoginStatusMsg -> Model -> ( Model, Cmd AnyMsg )
updateLoginStatus message model =
    case message of
        FetchLogin (Ok loggedInUser) ->
            ( { model
                | authRequired = Just False
                , loginForm = LoginForm.empty
              }
            , Ws.connect model.location
            )

        FetchLogin (Err error) ->
            let
                oldLoginForm =
                    model.loginForm
            in
            ( { model
                | loginForm =
                    { oldLoginForm
                        | password = ""
                        , loginIncorrect = True
                    }
              }
            , Cmd.none
            )

        FetchLogout (Ok string) ->
            let
                initialModel =
                    Model.initial model.location (Routing.parseLocation model.location)
            in
            ( { initialModel
                | authRequired = Just True
              }
            , Ws.disconnect model.location
            )

        FetchLogout (Err error) ->
            ( Model.initial model.location (Routing.parseLocation model.location)
            , Ws.disconnect model.location
            )

        FetchVerify (Ok string) ->
            if not model.wsConnected then
                ( { model | authRequired = Just False }
                , Ws.connect model.location
                )

            else
                ( model
                , Cmd.none
                )

        FetchVerify (Err error) ->
            if not model.wsConnected then
                case error of
                    BadStatus response ->
                        case response.status.code of
                            403 ->
                                ( { model | authRequired = Just True }
                                , Ws.attemptReconnect
                                )

                            _ ->
                                ( model
                                , Ws.attemptReconnect
                                )

                    _ ->
                        ( model
                        , Ws.attemptReconnect
                        )

            else
                ( model
                , Cmd.none
                )
