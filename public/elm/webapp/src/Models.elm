module Models exposing (..)

import Players.Models exposing (Player)
import About.Models exposing (AboutInfo)
import Templates.Models exposing (..)
import Maybe exposing (Maybe(..))

type alias Model =
    { players : List Player
    , aboutInfo: Maybe AboutInfo
    , templatesModel: Templates.Models.Model
    }


initialModel : Model
initialModel =
    { players = [ Player 1 "Sam" 1 ]
    , aboutInfo = Nothing
    , templatesModel = Templates.Models.initialModel
    }
