module Models.Ui.InstanceParameterForm exposing (..)

import Dict exposing (Dict)

type alias InstanceParameterForm =
  { parameterValues : Dict String String
  }

isBeingEdited : Maybe InstanceParameterForm -> Bool
isBeingEdited maybeInstanceParameterForm =
  maybeInstanceParameterForm
    |> Maybe.map (\f -> not (Dict.isEmpty f.parameterValues))
    |> Maybe.withDefault False
