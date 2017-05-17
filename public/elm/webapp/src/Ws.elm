module Ws exposing (update, listen, send, connect)

import Json.Decode as Decode exposing (field)

import Models.Resources.AboutInfo as AboutInfo
import Models.Resources.Template as Template
import Models.Resources.Instance as Instance
import Models.Resources.InstanceCreationSuccess as InstanceCreationSuccess
import Models.Resources.InstanceCreationFailure as InstanceCreationFailure
import Models.Resources.InstanceDeletionSuccess as InstanceDeletionSuccess
import Models.Resources.InstanceDeletionFailure as InstanceDeletionFailure
import Models.Resources.InstanceUpdateSuccess as InstanceUpdateSuccess
import Models.Resources.InstanceUpdateFailure as InstanceUpdateFailure

import Utils.MaybeUtils as MaybeUtils

import Updates.Messages exposing (..)
import Messages exposing (..)

import Array

import Set

import Dict

import Websocket

import Json.Encode as Encode

import Utils.CmdUtils as CmdUtils

payloadFieldName = "payload"

wsRelativePath = "/ws"

-- TODO return also a Cmd
update msg model =
  let msgType =
    Decode.decodeString typeDecoder msg
  in
    case msgType of
      Ok SetAboutInfoMsgType ->
        let aboutInfoResult =
          Decode.decodeString (field payloadFieldName AboutInfo.decoder) msg
        in
          case aboutInfoResult of
            Ok aboutInfo ->
              ( { model | aboutInfo = Just aboutInfo }
              , Cmd.none
              )
            Err error ->
              ( { model | aboutInfo = Nothing }
              , showError "Failed to decode about info: " error
              )
      Ok ListTemplatesMsgType ->
        let templatesResult =
          Decode.decodeString (field payloadFieldName (Decode.array Template.decoder)) msg
        in
          case templatesResult of
            Ok templates ->
              let templatesDict =
                templates
                |> Array.toList
                |> List.map (\t -> (t.id, t))
                |> Dict.fromList
              in
                ( { model | templates = templatesDict }
                , Cmd.none -- TODO send success message similar to error (enough to just send it for now)
                )
            Err error ->
              ( { model | templates = Dict.empty }
              , showError "Failed to decode templates: " error
              )
      Ok ListInstancesMsgType ->
        let instancesResult =
          Decode.decodeString (field payloadFieldName (Decode.array Instance.decoder)) msg
        in
          case instancesResult of
            Ok instances ->
              let instanceDict =
                instances
                |> Array.toList
                |> List.map (\i -> (i.id, i))
                |> Dict.fromList
              in
                ( { model | instances = instanceDict }
                , Cmd.none
                )
            Err error ->
              ( { model | instances = Dict.empty }
              , showError "Failed to decode instances: " error
              )
      Ok InstanceCreationSuccessMsgType ->
        let instanceCreationSuccessResult =
          Decode.decodeString (field payloadFieldName InstanceCreationSuccess.decoder) msg
        in
          case instanceCreationSuccessResult of
            Ok instanceCreationSuccess ->
              ( model
              , CmdUtils.sendMsg
                ( UpdateBodyViewMsg
                  ( ExpandNewInstanceForm False instanceCreationSuccess.instanceCreation.templateId )
                )
              )
            Err error ->
              ( model
              , showError "Failed to decode the instance creation result: " error
              )
      Ok InstanceCreationFailureMsgType ->
        let instanceCreationFailureResult =
          Decode.decodeString (field payloadFieldName InstanceCreationFailure.decoder) msg
        in
          case instanceCreationFailureResult of
            Ok instanceCreationFailure ->
              ( model
              , showError "Failed to create instance: " (toString instanceCreationFailure.reason)
              )
            Err error ->
              ( model
              , showError "Failed to decode the instance creation result: " error
              )
      Ok InstanceDeletionSuccessMsgType ->
        let instanceDeletionSuccessResult =
          Decode.decodeString (field payloadFieldName InstanceDeletionSuccess.decoder) msg
        in
          case instanceDeletionSuccessResult of
            Ok instanceDeletionSuccess ->
              let bodyUiModel = model.bodyUiModel
              in
                let newBodyUiModel =
                  { bodyUiModel | selectedInstances = (Set.remove instanceDeletionSuccess.instanceId model.bodyUiModel.selectedInstances) }
                in
                  ( { model | bodyUiModel = newBodyUiModel }
                  , Cmd.none
                  )
            Err error ->
              ( model
              , showError "Failed to decode the instance deletion result: " error
              )
      Ok InstanceDeletionFailureMsgType ->
        let instanceDeletionFailureResult =
          Decode.decodeString (field payloadFieldName InstanceDeletionFailure.decoder) msg
        in
          case instanceDeletionFailureResult of
            Ok instanceDeletionFailure ->
              ( model
              , showError "Failed to delete instance: " (toString instanceDeletionFailure.reason)
              )
            Err error ->
              ( model
              , showError "Failed to decode the instance deletion result: " error
              )
      Ok InstanceUpdateSuccessMsgType ->
        let instanceUpdateSuccessResult =
          Decode.decodeString (field payloadFieldName InstanceUpdateSuccess.decoder) msg
        in
          case instanceUpdateSuccessResult of
            Ok instanceUpdateSuccess ->
              ( model
              , if ( MaybeUtils.isDefined instanceUpdateSuccess.instanceUpdate.selectedTemplate
                   || MaybeUtils.isDefined instanceUpdateSuccess.instanceUpdate.parameterValues
                   ) then
                  CmdUtils.sendMsg (UpdateBodyViewMsg (DiscardParameterValueChanges instanceUpdateSuccess.instance.id))
                else
                  Cmd.none
              )
            Err error ->
              ( model
              , showError "Failed to decode the instance update result: " error
              )
      Ok InstanceUpdateFailureMsgType ->
        let instanceUpdateFailureResult =
          Decode.decodeString (field payloadFieldName InstanceUpdateFailure.decoder) msg
        in
          case instanceUpdateFailureResult of
            Ok instanceUpdateFailure ->
              ( model
              , showError "Failed to update instance: " (toString instanceUpdateFailure.reason)
              )
            Err error ->
              ( model
              , showError "Failed to decode the instance update result: " error
              )
      Ok ErrorMsgType ->
        let errorResult =
          Decode.decodeString (field payloadFieldName (Decode.string)) msg
        in
          case errorResult of
            Ok error ->
              ( model
              , showError "An error occured: " error
              )
            Err error ->
              ( model
              , showError "Failed to decode an error message: " error
              )
      Err error ->
        ( model
        , showError "Failed to decode web socket message: " error
        )
      Ok (UnknownMsgType unknown) ->
        ( model
        , showError "Unknown message type: " unknown
        )

typeDecoder =
  field "messageType" typeDecoderDecoder

typeDecoderDecoder =
  Decode.andThen
    (\typeString -> Decode.succeed (stringToIncomingType typeString))
    Decode.string

stringToIncomingType s =
  case s of
    "aboutInfo" -> SetAboutInfoMsgType
    "listTemplates" -> ListTemplatesMsgType
    "listInstances" -> ListInstancesMsgType
    "error" -> ErrorMsgType
    "addInstanceSuccess" -> InstanceCreationSuccessMsgType
    "addInstanceError" -> InstanceCreationFailureMsgType
    "deleteInstanceSuccess" -> InstanceDeletionSuccessMsgType
    "deleteInstanceError" -> InstanceDeletionFailureMsgType
    "updateInstanceSuccess" -> InstanceUpdateSuccessMsgType
    "updateInstanceError" -> InstanceUpdateFailureMsgType
    anything -> UnknownMsgType anything

showError prefix error =
  CmdUtils.sendMsg
    ( UpdateErrorsMsg
      ( AddError
        ( String.concat [prefix, error]
        )
      )
    )

connect location =
  Websocket.connect WsConnectError WsConnect (locationToWsUrl location)

listen location =
  -- WebSocket.listen "ws://localhost:9000/ws" ProcessWsMsg -- TODO relative URL
  Websocket.listen WsListenError WsMessage WsConnectionLost (locationToWsUrl location)

send location object messageType =
  let wrappedMessage =
    Encode.object [ ("messageType", Encode.string (outgoingTypeToString messageType)), ("payload", object) ] -- putting a line break here fucks up the compiler???
  in
    Websocket.send
      WsSendError
      WsSent
      (locationToWsUrl location)
      ( Encode.encode 0 wrappedMessage )

outgoingTypeToString t =
  case t of
    CreateInstanceMsgType -> "addInstance"
    DeleteInstanceMsgType -> "deleteInstance"
    UpdateInstanceMsgType -> "updateInstance"

locationToWsUrl location =
  let wsProtocol = if (String.contains "https" location.protocol) then "wss" else "ws"
  in
    String.concat
      [ wsProtocol
      , "://"
      , location.host
      , wsRelativePath
      ]
