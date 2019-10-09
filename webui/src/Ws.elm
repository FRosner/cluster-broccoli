module Ws exposing (attemptReconnect, connect, disconnect, listen, send, update)

import Array exposing (Array)
import Dict
import Json.Decode as Decode exposing (Decoder, andThen, field)
import Json.Encode as Encode
import Maybe.Extra exposing (isJust)
import Messages exposing (..)
import Model exposing (Model)
import Models.Resources.AboutInfo as AboutInfo
import Models.Resources.Instance as Instance
import Models.Resources.InstanceCreated as InstanceCreated
import Models.Resources.InstanceCreation as InstanceCreation
import Models.Resources.InstanceDeleted as InstanceDeleted
import Models.Resources.InstanceError as InstanceError
import Models.Resources.InstanceTasks as InstanceTasks
import Models.Resources.InstanceUpdate as InstanceUpdate
import Models.Resources.InstanceUpdated as InstanceUpdated
import Models.Resources.NodeResources as NodeResources
import Models.Resources.Template as Template
import Navigation exposing (Location)
import Set
import Time
import Updates.Messages exposing (..)
import Utils.CmdUtils as CmdUtils
import Websocket


wsRelativePath : String
wsRelativePath =
    "/ws"


{-| Decode an incoming websocket message from JSON.
-}
incomingWsMessageDecoder : Decoder IncomingWsMessage
incomingWsMessageDecoder =
    field "messageType" Decode.string
        |> andThen (field "payload" << payloadDecoder)


{-| Decode the payload of a websocket message from JSON.

Decode according to the message type.

-}
payloadDecoder : String -> Decoder IncomingWsMessage
payloadDecoder t =
    case t of
        "aboutInfo" ->
            Decode.map SetAboutInfoMessage AboutInfo.decoder

        "listTemplates" ->
            Decode.map ListTemplatesMessage <| Decode.array Template.decoder

        "listInstances" ->
            Decode.map ListInstancesMessage <| Decode.array Instance.decoder

        "listNodeResources" ->
            Decode.map ListResourcesMessage <| Decode.list NodeResources.decoder

        "error" ->
            Decode.map ErrorMessage Decode.string

        "addInstanceSuccess" ->
            Decode.map AddInstanceSuccessMessage InstanceCreated.decoder

        "addInstanceError" ->
            Decode.map AddInstanceErrorMessage InstanceError.decoder

        "deleteInstanceSuccess" ->
            Decode.map DeleteInstanceSuccessMessage InstanceDeleted.decoder

        "deleteInstanceError" ->
            Decode.map DeleteInstanceErrorMessage InstanceError.decoder

        "updateInstanceSuccess" ->
            Decode.map UpdateInstanceSuccessMessage InstanceUpdated.decoder

        "updateInstanceError" ->
            Decode.map UpdateInstanceErrorMessage InstanceError.decoder

        "getInstanceTasksSuccess" ->
            Decode.map GetInstanceTasksSuccessMessage InstanceTasks.decoder

        "getInstanceTasksError" ->
            Decode.map2 GetInstanceTasksErrorMessage (field "instanceId" Decode.string) (field "error" InstanceError.decoder)

        s ->
            Decode.fail <| "Unknown message type: " ++ s


{-| Update the given model from a raw incoming websocket message.

Decode the incoming websocket message from the given JSON string and update the
given model according to the message.

Return the new model and a subsequent command to run.

-}
update : String -> Model -> ( Model, Cmd AnyMsg )
update msg model =
    case Decode.decodeString incomingWsMessageDecoder msg of
        Ok message ->
            updateFromMessage model message

        Err error ->
            ( model
            , showError "Failed to decode web socket message: " error
            )


updateFromMessage : Model -> IncomingWsMessage -> ( Model, Cmd AnyMsg )
updateFromMessage model message =
    case message of
        SetAboutInfoMessage info ->
            ( { model | aboutInfo = Just info }
            , Cmd.none
            )

        ListTemplatesMessage templates ->
            ( { model
                | templates =
                    templates
                        |> Array.toList
                        |> List.map (\t -> ( t.id, t ))
                        |> Dict.fromList
              }
            , Cmd.none
            )

        ListInstancesMessage instances ->
            ( { model
                | instances =
                    instances
                        |> Array.toList
                        |> List.map (\i -> ( i.id, i ))
                        |> Dict.fromList
              }
            , model.bodyUiModel.expandedInstances
                |> Set.toList
                |> List.map (GetInstanceTasks >> SendWsMsg >> CmdUtils.sendMsg)
                |> Cmd.batch
            )

        ListResourcesMessage nodesResources ->
            ( { model | nodesResources = nodesResources }
            , Cmd.none
            )

        AddInstanceSuccessMessage result ->
            ( { model | instances = Dict.insert result.instance.id result.instance model.instances }
            , CmdUtils.sendMsg
                (UpdateBodyViewMsg
                    (ExpandNewInstanceForm False result.instanceCreation.templateId)
                )
            )

        AddInstanceErrorMessage error ->
            ( model
            , showError "Failed to create instance: " (toString error.reason)
            )

        DeleteInstanceSuccessMessage result ->
            let
                bodyUiModel =
                    model.bodyUiModel
            in
            ( { model
                | bodyUiModel = { bodyUiModel | selectedInstances = Set.remove result.instanceId model.bodyUiModel.selectedInstances }
                , instances = Dict.remove result.instance.id model.instances
              }
            , Cmd.none
            )

        DeleteInstanceErrorMessage error ->
            ( model
            , showError "Failed to delete instance: " (toString error.reason)
            )

        UpdateInstanceSuccessMessage result ->
            ( { model
                | instances = Dict.insert result.instance.id result.instance model.instances
              }
            , if
                isJust result.instanceUpdate.selectedTemplate
                    || isJust result.instanceUpdate.parameterValues
              then
                CmdUtils.sendMsg (UpdateBodyViewMsg (DiscardParameterValueChanges result.instance.id))

              else
                Cmd.none
            )

        UpdateInstanceErrorMessage error ->
            ( model
            , showError "Failed to update instance: " (toString error.reason)
            )

        GetInstanceTasksSuccessMessage result ->
            ( { model | tasks = Dict.insert result.instanceId result model.tasks }, Cmd.none )

        GetInstanceTasksErrorMessage instanceId error ->
            -- When we failed to get tasks of an instance clear the tasks of the instance, and swallow the error since
            -- we'll retry soon anyway.
            ( { model | tasks = Dict.insert instanceId (InstanceTasks.empty instanceId) model.tasks }, Cmd.none )

        ErrorMessage error ->
            ( model
            , showError "An error occured: " error
            )


showError : String -> String -> Cmd AnyMsg
showError prefix error =
    CmdUtils.sendMsg
        (UpdateErrorsMsg
            (AddError
                (String.concat [ prefix, error ])
            )
        )


connect : Location -> Cmd AnyMsg
connect location =
    Websocket.connect WsConnectError WsConnect (locationToWsUrl location)


disconnect : Location -> Cmd AnyMsg
disconnect location =
    Websocket.disconnect WsErrorDisconnect WsSuccessDisconnect (locationToWsUrl location)


listen : Location -> Sub AnyMsg
listen location =
    Websocket.listen WsListenError WsMessage WsConnectionLost (locationToWsUrl location)


{-| Encode an outgoing websocket message to JSON.
-}
encodeOutgoingWsMessage : OutgoingWsMessage -> Encode.Value
encodeOutgoingWsMessage message =
    let
        ( messageType, payload ) =
            case message of
                AddInstanceMessage creation ->
                    ( "addInstance", InstanceCreation.encoder creation )

                DeleteInstanceMessage id ->
                    ( "deleteInstance", Encode.string id )

                UpdateInstanceMessage update ->
                    ( "updateInstance", InstanceUpdate.encoder update )

                GetInstanceTasks id ->
                    ( "getInstanceTasks", Encode.string id )

                GetResources ->
                    ( "getResources", Encode.string "" )
    in
    Encode.object
        [ ( "messageType", Encode.string messageType )
        , ( "payload", payload )
        ]


send : Location -> OutgoingWsMessage -> Cmd AnyMsg
send location message =
    Websocket.send
        WsSendError
        WsSent
        (locationToWsUrl location)
        (encodeOutgoingWsMessage message |> Encode.encode 0)


locationToWsUrl : Location -> String
locationToWsUrl location =
    let
        wsProtocol =
            if String.contains "https" location.protocol then
                "wss"

            else
                "ws"
    in
    String.concat
        [ wsProtocol
        , "://"
        , location.host
        , wsRelativePath
        ]


attemptReconnect : Cmd AnyMsg
attemptReconnect =
    CmdUtils.delayMsg (5 * Time.second) AttemptReconnect
