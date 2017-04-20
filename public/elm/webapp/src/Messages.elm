module Messages exposing (..)

import Updates.Messages exposing (..)

type AnyMsg
  = UpdateAboutInfoMsg Updates.Messages.UpdateAboutInfoMsg
  | UpdateErrorsMsg Updates.Messages.UpdateErrorsMsg
  | ProcessWsMsg String
  | SendWsMsg String
  | UpdateLoginFormMsg Updates.Messages.UpdateLoginFormMsg
  | UpdateLoginStatusMsg Updates.Messages.UpdateLoginStatusMsg
  | UpdateBodyViewMsg Updates.Messages.UpdateBodyViewMsg
  | UpdateTemplatesMsg Updates.Messages.UpdateTemplatesMsg
  | NoOp
  -- | FetchTemplatesMsg Commands.FetchTemplates.Msg
  -- | ViewsBodyMsg Views.Body.Msg
  -- | ViewsNewInstanceFormMsg Views.NewInstanceForm.Msg
