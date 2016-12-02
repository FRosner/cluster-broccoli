module Updates.UpdateErrors exposing (updateErrors)

import Updates.Messages exposing (UpdateErrorsMsg(..))
import Messages exposing (AnyMsg)
import Models.Ui.Notifications exposing (Errors)

updateErrors : UpdateErrorsMsg -> Errors -> (Errors, Cmd AnyMsg)
updateErrors message oldErrors =
  case message of
    AddError error ->
      (error :: oldErrors, Cmd.none)
