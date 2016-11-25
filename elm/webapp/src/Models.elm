module Models exposing (..)

import Players.Models exposing (Player)
import About.Models exposing (AboutInfo)
import Maybe exposing (Maybe(..))

type alias Model =
    { players : List Player
    , aboutInfo: Maybe AboutInfo
    }


initialModel : Model
initialModel =
    { players = [ Player 1 "Sam" 1 ]
    , aboutInfo = Nothing
    }
