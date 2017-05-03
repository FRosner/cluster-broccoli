module Models.Ui.InstanceParameterForm exposing (..)

import Models.Resources.Template exposing (Template)

import Dict exposing (Dict)

import Maybe exposing (Maybe)

type alias InstanceParameterForm =
  { originalParameterValues : Dict String String
  , changedParameterValues : Dict String String
  , selectedTemplate : Maybe Template
  }

empty =
  InstanceParameterForm
    Dict.empty
    Dict.empty
    Nothing
