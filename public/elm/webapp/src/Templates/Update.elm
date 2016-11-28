module Templates.Update exposing (..)

import Templates.Messages exposing (Msg(..))
import Templates.Models exposing (Templates)

update : Msg -> Templates -> (Templates, Cmd Msg)
update message oldTemplates =
    case message of
        FetchTemplates (Ok newTemplates) ->
            (newTemplates, Cmd.none)

        FetchTemplates (Err error) ->
            ([], Cmd.none)
