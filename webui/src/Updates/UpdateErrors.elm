module Updates.UpdateErrors exposing (updateErrors)

import Messages exposing (AnyMsg)
import Models.Ui.Notifications exposing (Errors)
import Updates.Messages exposing (UpdateErrorsMsg(..))
import Utils.ListUtils as ListUtils


updateErrors : UpdateErrorsMsg -> Errors -> ( Errors, Cmd AnyMsg )
updateErrors message oldErrors =
    case message of
        AddError error ->
            ( error :: oldErrors, Cmd.none )

        CloseError index ->
            ( ListUtils.remove index oldErrors
            , Cmd.none
            )
