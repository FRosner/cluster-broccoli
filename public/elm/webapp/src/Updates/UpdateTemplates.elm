module Updates.UpdateTemplates exposing (updateTemplates)

import Updates.Messages exposing (UpdateTemplatesMsg(..))
import Messages exposing (AnyMsg)
import Models.Resources.Template exposing (Template)

updateTemplates : UpdateTemplatesMsg -> List Template -> (List Template, Cmd AnyMsg)
updateTemplates message oldTemplates =
  case message of
    ListTemplates templates ->
      ( templates
      , Cmd.none
      )
