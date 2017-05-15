module Main exposing (main)

import Updates.UpdateErrors exposing (updateErrors)
import Updates.UpdateLoginForm exposing (updateLoginForm)
import Updates.UpdateLoginStatus exposing (updateLoginStatus)
import Updates.UpdateBodyView exposing (updateBodyView)
import Updates.Messages exposing (UpdateLoginStatusMsg(..), UpdateErrorsMsg(..))

import Messages exposing (..)

import Views.Header
import Views.Body
import Views.Footer
import Views.Notifications

import Commands.LoginLogout as LoginLogout

import Utils.CmdUtils as CmdUtils

import Routing

import Model exposing (Model)

import Ws

import Websocket

import Time

import Html exposing (..)
import Html.Attributes exposing (..)

import Navigation exposing (Location)

import Set exposing (Set)

import Dict exposing (Dict)

init : Location -> ( Model, Cmd AnyMsg )
init location =
  ( Model.initial location (Routing.parseLocation location)
  , CmdUtils.sendMsg AttemptReconnect
  )

update : AnyMsg -> Model -> ( Model, Cmd AnyMsg )
update msg model =
  case msg of
    SendWsMsg jsonObject wsMsgType ->
      ( model
      , Ws.send model.location jsonObject wsMsgType
      )
    AttemptReconnect ->
      ( model
      , Cmd.map UpdateLoginStatusMsg
          LoginLogout.verifyLogin
      )
    WsConnectError ( url, error ) ->
      let l = Debug.log "ConnectError" ( url, error )
      in
        ( { model | wsConnected = False }
        , CmdUtils.delayMsg (5 * Time.second) AttemptReconnect
        )
    WsConnect url ->
      let l = Debug.log "Connect" url
      in
        ( { model | wsConnected = True }
        , Cmd.none
        )
    WsListenError ( url, error ) ->
      let l = Debug.log "ListenError" ( url, error )
      in
        ( model
        , Cmd.none
        )
    WsConnectionLost url -> -- TODO reconnect?
      let l = Debug.log "ConnectionLost" url
      in
        ( { model
          | wsConnected = False
          , aboutInfo = Nothing
          } -- TODO what do I have to set here? all stuff should be unknown and disabled or so?
        , Ws.connect model.location
        )
    WsMessage ( url, message ) ->
      Ws.update message model
    WsSendError ( url, message, error ) ->
      let l = Debug.log "SendError" ( url, message, error )
      in
        ( model
        , Cmd.none
        )
    WsSent ( url, message ) ->
      let l = Debug.log "Sent" ( url, message )
      in
        ( model
        , Cmd.none
        )
    UpdateLoginStatusMsg subMsg ->
      updateLoginStatus subMsg model
    UpdateErrorsMsg subMsg ->
      let (newErrors, cmd) =
        updateErrors subMsg model.errors
      in
        ({ model | errors = newErrors }
        , cmd
        )
    UpdateBodyViewMsg subMsg ->
      let (newBodyUiModel, cmd) =
        updateBodyView subMsg model.bodyUiModel
      in
        ( { model | bodyUiModel = newBodyUiModel }
        , cmd
        )
    UpdateLoginFormMsg subMsg ->
      let (newLoginForm, cmd) =
        updateLoginForm subMsg model.loginForm
      in
        ({ model | loginForm = newLoginForm }
        , cmd
        )
    OnLocationChange location ->
      let newRoute =
        Routing.parseLocation location
      in
        ( { model
          | route = newRoute
          , location = location
          }
        , Cmd.none
        )
    NoOp -> (model, Cmd.none)

view : Model -> Html AnyMsg
view model =
  div
    []
    [ Views.Header.view model.aboutInfo model.loginForm model.authRequired
    , Views.Notifications.view model.errors
    , Html.map
        UpdateBodyViewMsg
        ( Views.Body.view
            model.templates
            model.instances
            model.bodyUiModel
        )
    , text (toString model)
    , Views.Footer.view model
    ]

-- Sub.map UpdateErrorsMsg ( WebSocket.listen "ws://localhost:9000/ws" AddError ) when AddError is an UpdateErrorsMsg
subscriptions : Model -> Sub AnyMsg
subscriptions model =
  Ws.listen model.location

main =
  Navigation.program OnLocationChange
    { init = init
    , view = view
    , update = update
    , subscriptions = subscriptions
    }
