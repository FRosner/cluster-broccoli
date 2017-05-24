module Updates.UpdateLoginStatus exposing (updateLoginStatus)

import Updates.Messages exposing (UpdateLoginStatusMsg(..), UpdateLoginFormMsg(..), UpdateErrorsMsg(..))

import Messages exposing (AnyMsg(..))

import Model exposing (Model)

import Models.Ui.LoginForm as LoginForm exposing (LoginForm)

import Utils.CmdUtils as CmdUtils

import Ws

import Time

import Debug

import Http exposing (Error(..))

updateLoginStatus : UpdateLoginStatusMsg -> Model -> (Model, Cmd AnyMsg)
updateLoginStatus message model =
  case message of
    FetchLogin (Ok loggedInUser) ->
      let s = Debug.log "FetchLogout Ok" loggedInUser
      in
      ( { model
        | authRequired = Just False
        , loginForm = LoginForm.empty
        }
      , Ws.connect model.location
      )
    FetchLogin (Err error) ->
      let oldLoginForm = model.loginForm
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
      ( { model
        | authRequired = Just True
        }
      , Ws.disconnect model.location
      )
    FetchLogout (Err error) ->
      ( model
      , Ws.disconnect model.location
      )
    FetchVerify (Ok string) ->
      let s = Debug.log "FetchVerify Ok" string
      in
      if (not (model.wsConnected)) then
        ( { model | authRequired = Just False }
        , Ws.connect model.location
        )
      else
        ( model
        , Cmd.none
        )
    FetchVerify (Err error) ->
      let s = Debug.log "FetchVerify Err" error
      in
      case error of -- TODO always attempt reconnect or only on non-403?
        BadStatus response ->
          case response.status.code of
            403 ->
              ( { model | authRequired = Just True }
              , CmdUtils.delayMsg (5 * Time.second) AttemptReconnect
              )
            _ ->
              ( model
              , CmdUtils.delayMsg (5 * Time.second) AttemptReconnect
              )
        _ ->
          ( model
          , CmdUtils.delayMsg (5 * Time.second) AttemptReconnect
          )
