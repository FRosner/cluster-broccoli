module Messages exposing (..)

import Updates.Messages exposing (..)
import Array exposing (Array)
import Navigation exposing (Location)
import Websocket exposing (..)
import Models.Resources.Instance exposing (Instance, InstanceId)
import Models.Resources.InstanceError exposing (InstanceError)
import Models.Resources.InstanceCreation exposing (InstanceCreation)
import Models.Resources.InstanceUpdate exposing (InstanceUpdate)
import Models.Resources.AboutInfo exposing (AboutInfo)
import Models.Resources.Template exposing (Template)
import Models.Resources.InstanceCreationSuccess exposing (InstanceCreationSuccess)
import Models.Resources.InstanceCreationFailure exposing (InstanceCreationFailure)
import Models.Resources.InstanceDeleted exposing (InstanceDeleted)
import Models.Resources.InstanceUpdateSuccess exposing (InstanceUpdateSuccess)
import Models.Resources.InstanceUpdateFailure exposing (InstanceUpdateFailure)


type AnyMsg
    = UpdateErrorsMsg Updates.Messages.UpdateErrorsMsg
    | SendWsMsg OutgoingWsMessage
    | WsMessage ( Url, Message )
    | WsListenError ( Url, ErrorMessage )
    | WsConnectionLost Url
    | WsConnectError ( Url, ErrorMessage )
    | WsConnect Url
    | WsSendError ( Url, Message, ErrorMessage )
    | WsSent ( Url, Message )
    | WsSuccessDisconnect Url
    | WsErrorDisconnect ( Url, ErrorMessage )
    | AttemptReconnect
    | UpdateLoginFormMsg Updates.Messages.UpdateLoginFormMsg
    | UpdateLoginStatusMsg Updates.Messages.UpdateLoginStatusMsg
    | UpdateBodyViewMsg Updates.Messages.UpdateBodyViewMsg
    | OnLocationChange Location
    | TemplateFilter String
    | InstanceFilter String
    | NoOp


{-| Incoming web socket messages, send from the server to the client.
-}
type IncomingWsMessage
    = SetAboutInfoMessage AboutInfo
    | ListTemplatesMessage (Array Template)
    | ListInstancesMessage (Array Instance)
    | AddInstanceSuccessMessage InstanceCreationSuccess
    | AddInstanceErrorMessage InstanceCreationFailure
    | DeleteInstanceSuccessMessage InstanceDeleted
    | DeleteInstanceErrorMessage InstanceError
    | UpdateInstanceSuccessMessage InstanceUpdateSuccess
    | UpdateInstanceErrorMessage InstanceUpdateFailure
    | ErrorMessage String


{-| The type of an outgoing websocket message.
-}
type OutgoingWsMessage
    = AddInstanceMessage InstanceCreation
    | DeleteInstanceMessage InstanceId
    | UpdateInstanceMessage InstanceUpdate
