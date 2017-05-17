module Updates.UpdateLoginStatus exposing (updateLoginStatus)

import Updates.Messages exposing (UpdateLoginStatusMsg(..), UpdateLoginFormMsg(..), UpdateErrorsMsg(..))

import Messages exposing (AnyMsg(..))

import Model exposing (Model)

import Utils.CmdUtils as CmdUtils

import Ws

import Time

import Debug

import Http exposing (Error(..))

updateLoginStatus : UpdateLoginStatusMsg -> Model -> (Model, Cmd AnyMsg)
updateLoginStatus message model =
  case message of
    FetchLogin (Ok loggedInUser) ->
      ( model
      , Cmd.none -- TODO request about again on successful login?
      )
    FetchLogin (Err error) ->
      ( model
      , (CmdUtils.sendMsg (UpdateLoginFormMsg FailedLoginAttempt))
      )
    FetchLogout (Ok string) ->
      ( { model
        | authRequired = Just True
        }
      , Cmd.none
      )
    FetchLogout (Err error) ->
      ( model
      , ( CmdUtils.sendMsg ( UpdateErrorsMsg ( AddError "Logout failed." ) ) )
      )
    FetchVerify (Ok string) ->
      let s = Debug.log "FetchVerify Ok" string
      in
      ( { model | authRequired = Just False }
      , Ws.connect model.location
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
