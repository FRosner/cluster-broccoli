module Models exposing (..)

import Players.Models exposing (Player)
import About.Models exposing (AboutInfo)
import Templates.Models exposing (Templates, Template)
import Maybe exposing (Maybe(..))

type alias Model =
    { players : List Player
    , aboutInfo: Maybe AboutInfo
    , templates: List Template
    }


initialModel : Model
initialModel =
    { players = [ Player 1 "Sam" 1 ]
    , aboutInfo = Nothing
    , templates = []
    }
