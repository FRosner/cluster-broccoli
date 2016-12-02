module Updates.UpdateTemplates exposing (updateTemplates)

import Commands.FetchTemplates exposing (Msg)
import Models.Resources.Template exposing (Template)

type Msg
  = NoOp

-- TODO send error to error channel
updateTemplates : Commands.FetchTemplates.Msg -> List Template -> (List Template, Cmd Msg)
updateTemplates message oldTemplates =
  case message of
    FetchTemplates (Ok newTemplates) ->
      ( newTemplates
      , Cmd.none
      )
    FetchTemplates (Err error) ->
      ( []
      , Cmd.none
      )
    ShowNewInstanceForm template ->
      ( Set.insert template.id expandedNewInstanceForms
      , Cmd.none
      )
    HideNewInstanceForm template ->
      ( Set.remove template.id expandedNewInstanceForms
      , Cmd.none
      )
