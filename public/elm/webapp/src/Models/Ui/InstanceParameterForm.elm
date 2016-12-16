module Models.Ui.InstanceParameterForm exposing (..)

import Dict exposing (Dict)

type alias InstanceParameterForm =
  { changedParameterValues : Dict String String
  }
