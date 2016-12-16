module Models.Ui.InstanceParameterForm exposing (..)

import Dict exposing (Dict)

type alias InstanceParameterForm =
  { originalParameterValues : Dict String String
  , changedParameterValues : Dict String String
  }
