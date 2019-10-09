module Messages exposing (..)

import Array exposing (Array)
import Model exposing (TabState)
import Models.Resources.AboutInfo exposing (AboutInfo)
import Models.Resources.Instance exposing (Instance, InstanceId)
import Models.Resources.InstanceCreated exposing (InstanceCreated)
import Models.Resources.InstanceCreation exposing (InstanceCreation)
import Models.Resources.InstanceDeleted exposing (InstanceDeleted)
import Models.Resources.InstanceError exposing (InstanceError)
import Models.Resources.InstanceTasks exposing (InstanceTasks)
import Models.Resources.InstanceUpdate exposing (InstanceUpdate)
import Models.Resources.InstanceUpdated exposing (InstanceUpdated)
import Models.Resources.NodeResources exposing (NodeResources)
import Models.Resources.Template exposing (Template)
import Navigation exposing (Location)
import Updates.Messages exposing (..)
import Websocket exposing (..)


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
    | TabMsg TabState
    | OnLocationChange Location
    | TemplateFilter String
    | InstanceFilter String
    | NodeFilter String
    | NoOp


{-| Incoming web socket messages, send from the server to the client.
-}
type IncomingWsMessage
    = SetAboutInfoMessage AboutInfo
    | ListTemplatesMessage (Array Template)
    | ListInstancesMessage (Array Instance)
    | ListResourcesMessage (List NodeResources)
    | AddInstanceSuccessMessage InstanceCreated
    | AddInstanceErrorMessage InstanceError
    | DeleteInstanceSuccessMessage InstanceDeleted
    | DeleteInstanceErrorMessage InstanceError
    | UpdateInstanceSuccessMessage InstanceUpdated
    | UpdateInstanceErrorMessage InstanceError
    | GetInstanceTasksSuccessMessage InstanceTasks
    | GetInstanceTasksErrorMessage InstanceId InstanceError
    | ErrorMessage String


{-| The type of an outgoing websocket message.
-}
type OutgoingWsMessage
    = AddInstanceMessage InstanceCreation
    | DeleteInstanceMessage InstanceId
    | UpdateInstanceMessage InstanceUpdate
    | GetInstanceTasks InstanceId
    | GetResources
