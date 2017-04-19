module Updates.UpdateAboutInfo exposing (updateAboutInfo)

import Updates.Messages exposing (UpdateAboutInfoMsg(..))
import Models.Resources.AboutInfo exposing (AboutInfo)
import Messages exposing (AnyMsg(..))
import Http exposing (Error(..))

updateAboutInfo : UpdateAboutInfoMsg -> Maybe AboutInfo -> (Maybe AboutInfo, Cmd AnyMsg)
updateAboutInfo message oldAboutInfo =
  case message of
    SetAbout about ->
      ( Just about
      , Cmd.none
      )
