module Update exposing (..)

import Messages exposing (Msg(..))
import Models exposing (Model)
import Players.Update
import About.Update
import Templates.Update


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
  case msg of
    PlayersMsg subMsg ->
      let
        ( updatedPlayers, cmd ) =
          Players.Update.update subMsg model.players
      in
        ( { model | players = updatedPlayers }, Cmd.map PlayersMsg cmd )
    AboutMsg subMsg ->
      let
        ( updatedAboutInfo, cmd ) =
          About.Update.update subMsg model.aboutInfo
      in
        ( { model | aboutInfo = updatedAboutInfo }, Cmd.map AboutMsg cmd )
    TemplatesMsg subMsg ->
      let
        ( updatedTemplates, cmd ) =
          Templates.Update.update subMsg model.templates
      in
        ( { model | templates = updatedTemplates }, Cmd.map TemplatesMsg cmd )
