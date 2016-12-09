module Updates.UpdateBodyView exposing (updateBodyView)

import Updates.Messages exposing (UpdateBodyViewMsg(..))
import Set exposing (Set)
import Models.Resources.Template exposing (TemplateId)
import Messages exposing (AnyMsg)

updateBodyView : UpdateBodyViewMsg -> Set TemplateId -> (Set TemplateId, Cmd AnyMsg)
updateBodyView message oldExpandedTemplates =
  case message of
    ToggleTemplate templateId ->
      ( if (Set.member templateId oldExpandedTemplates) then
          Set.remove templateId oldExpandedTemplates
        else
          Set.insert templateId oldExpandedTemplates
      , Cmd.none
      )
