module Messages exposing (..)

import Updates.Messages exposing (..)

import Json.Decode

type AnyMsg
  = UpdateAboutInfoMsg Updates.Messages.UpdateAboutInfoMsg
  | UpdateErrorsMsg Updates.Messages.UpdateErrorsMsg
  | ProcessWsMsg String
  | SendWsMsg Json.Decode.Value OutgoingWsMsgType
  | UpdateLoginFormMsg Updates.Messages.UpdateLoginFormMsg
  | UpdateLoginStatusMsg Updates.Messages.UpdateLoginStatusMsg
  | UpdateBodyViewMsg Updates.Messages.UpdateBodyViewMsg
  | UpdateTemplatesMsg Updates.Messages.UpdateTemplatesMsg
  | NoOp
  -- | FetchTemplatesMsg Commands.FetchTemplates.Msg
  -- | ViewsBodyMsg Views.Body.Msg
  -- | ViewsNewInstanceFormMsg Views.NewInstanceForm.Msg

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
