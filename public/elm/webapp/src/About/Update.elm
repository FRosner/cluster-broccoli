module About.Update exposing (..)

import About.Messages exposing (Msg(..))
import About.Models exposing (AboutInfo)
import Maybe exposing (Maybe(..))

update : Msg -> Maybe AboutInfo -> (Maybe AboutInfo, Cmd Msg)
update message oldAboutInfo =
    case message of
        FetchAbout (Ok newAboutInfo) ->
            (Just newAboutInfo, Cmd.none)

        FetchAbout (Err error) ->
            (Nothing, Cmd.none)
