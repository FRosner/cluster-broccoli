module Messages exposing (..)

import Updates.Messages exposing (..)
import Json.Decode
import Navigation exposing (Location)
import Websocket exposing (..)


type AnyMsg
    = UpdateErrorsMsg Updates.Messages.UpdateErrorsMsg
    | SendWsMsg Json.Decode.Value OutgoingWsMsgType
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


type IncomingWsMsgType
    = SetAboutInfoMsgType
    | ListTemplatesMsgType
    | ListInstancesMsgType
    | ErrorMsgType
    | InstanceCreationSuccessMsgType
    | InstanceCreationFailureMsgType
    | InstanceDeletionSuccessMsgType
    | InstanceDeletionFailureMsgType
    | InstanceUpdateSuccessMsgType
    | InstanceUpdateFailureMsgType
    | UnknownMsgType String


type OutgoingWsMsgType
    = CreateInstanceMsgType
    | DeleteInstanceMsgType
    | UpdateInstanceMsgType
