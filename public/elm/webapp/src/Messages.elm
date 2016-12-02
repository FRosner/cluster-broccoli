module Messages exposing (..)

import Updates.Messages exposing (..)

type AnyMsg
  = UpdateAboutInfoMsg Updates.Messages.UpdateAboutInfoMsg
  | UpdateErrorsMsg Updates.Messages.UpdateErrorsMsg
  | UpdateLoginFormMsg Updates.Messages.UpdateLoginFormMsg
  | NoOp
  -- | FetchTemplatesMsg Commands.FetchTemplates.Msg
  -- | ViewsBodyMsg Views.Body.Msg
  -- | ViewsNewInstanceFormMsg Views.NewInstanceForm.Msg
